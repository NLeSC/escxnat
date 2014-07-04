/*
 * Copyright 2012-2014 Netherlands eScience Center.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at the following location:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * For the full license, see: LICENSE.txt (located in the root folder of this distribution).
 * ---
 */
// source:

package nl.esciencecenter.medim.dicom.types;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;
import nl.esciencecenter.ptk.util.ResourceLoader;
import nl.esciencecenter.ptk.util.StringUtil;

import org.dcm4che2.data.VR;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Custom Dicom Tags DB.
 */
public class DicomTags
{
    // "Group", "Element", "VR", "Name"
    public static final String CSV_GROUP = "Group";

    public static final String CSV_ELEMENT = "Element";

    public static final String CSV_VR = "VR";

    public static final String CSV_NAME = "Name";

    @JsonPropertyOrder({
            "group", "element", "VR", "name", "keep", "options"
    })
    public static class CsvTagLine
    {
        // Read CSV line as Strings. Parse contents later.
        public String group;

        public String element;

        public String VR;

        public String name;

        public String keep;

        public String options;

        public CsvTagLine()
        {
        }
    }

    public static enum TagProcessingOption
    {
        DELETE("Delete Tag"),
        KEEP("Keep"),
        HASH("Hash"),
        HASH_UID("Hash UID"),
        ENCRYPT("Encrypt"),
        ENCRYPT_HASH("Encrypt and Hash"),
        ENCRYPT_HASH_UID("Encrypt and Hash UID"),
        CLEAR("Clear or nullify"),
        SET_DATE_TO_01JAN("Set date to 01-Jan (Keep year)"),
        SET_DATE_TO_01JAN1900("Set date to 01-Jan 1900"),
        SET_TIME_TO_0000HOURS("Set time to 00:00(am)");

        String comment = null;

        private TagProcessingOption(String comment)
        {
            this.comment = comment;
        }

        public String getComment()
        {
            return comment;
        }

        public static TagProcessingOption valueOfOrNull(String optionString, boolean ignoreCase)
        {
            try
            {
                if (ignoreCase)
                    optionString = optionString.toUpperCase();
                return TagProcessingOption.valueOf(optionString);
            }
            catch (Throwable e)
            {

            }
            return null;
        }
    }

    public static class TagDirective
    {
        int tagNr = 0;

        VR vr; // VR type is here as extra type check

        String name;

        TagProcessingOption option;

        public String toString()
        {
            return "{" + tagNr + ":" + vr + "'" + name + "'=" + option + "}";
        }

    }

    // ========================================================================
    // DicomTags Factory methods
    // ========================================================================

    private static DicomTags instance = null;

    public static DicomTags createFromFile(String filePath) throws IOException
    {
        DicomTags tags = new DicomTags();
        tags.readFromFile(filePath);
        return tags;
    }

    public static DicomTags createFromText(String txt) throws IOException
    {
        DicomTags tags = new DicomTags();
        tags.readFromText(txt);
        return tags;
    }

    public static DicomTags getDefault() throws IOException
    {

        if (instance == null)
        {
            instance = DicomTags.createFromFile("dicom/dicom_tags.xcsv");
        }

        return instance;

    }

    public static DicomTags createEmpty()
    {
        return new DicomTags();
    }

    // ==================
    // DicomTags instance
    // ==================

    protected Map<Integer, TagDirective> dicomTags = new Hashtable<Integer, TagDirective>();

    protected DicomTags()
    {
        init();
    }

    private void init()
    {
    }

    public int size()
    {
        if (this.dicomTags == null)
            return 0;
        return this.dicomTags.size();
    }

    protected void readFromFile(String filePath) throws IOException
    {
        ResourceLoader loader = ResourceLoader.getDefault();
        String txt = loader.readText(loader.resolveUrl(filePath));
        readFromText(txt);
    }

    protected void readFromText(String txt) throws IOException
    {
        // Pass I: remove comments including the ending newline!
        Pattern pat = Pattern.compile("^#.*\n", Pattern.MULTILINE);
        String newTxt = pat.matcher(txt).replaceAll("");
        // Not needed: Pass II: remove empty lines as a result of the
        // pat=Pattern.compile("\n\n",Pattern.MULTILINE);
        // newTxt=pat.matcher(newTxt).replaceAll("");

        // ObjectMapper mapper=new ObjectMapper();
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(CsvTagLine.class); // create object mapping from CsvLine.class

        // CsvSchema schema = CsvSchema.builder()
        // .addColumn(CSV_GROUP)
        // .addColumn(CSV_ELEMENT)
        // .addColumn(CSV_VR)
        // .addColumn(CSV_NAME)
        // .build();

        MappingIterator<CsvTagLine> mi = mapper.reader(CsvTagLine.class).with(schema).readValues(newTxt);

        List<TagDirective> tags = new ArrayList<TagDirective>();

        // skip first:
        CsvTagLine header = mi.nextValue();

        // check header values.
        while (mi.hasNextValue())
        {
            CsvTagLine line = mi.nextValue();
            TagDirective tag = new TagDirective();
            // do something?
            tag.tagNr = StringUtil.parseHexidecimal(line.group) * 0x10000 + StringUtil.parseHexidecimal(line.element);
            tag.name = line.name;
            line.keep = StringUtil.stripWhiteSpace(line.keep);
            line.options = StringUtil.stripWhiteSpace(line.options);

            // Support OX
            if (StringUtil.equalsIgnoreCase(line.VR, "OX"))
                line.VR = "OB"; // treat as bytes;

            VRType vrType = VRType.valueOf(line.VR);
            tag.vr = vrType.vr();

            boolean keep = false;

            if (StringUtil.isWhiteSpace(line.keep) == false)
                keep = (Integer.parseInt(line.keep) > 0);

            if (keep == false)
            {
                tag.option = TagProcessingOption.DELETE;
            }
            else
            {
                // check option:
                // System.err.printf("- %s | %s | %s | %s\n",line.group,line.element,line.keep,line.options);
                if (StringUtil.isWhiteSpace(line.options) == false)
                {
                    tag.option = TagProcessingOption.valueOfOrNull(line.options, true);
                    // error parsing option:
                    if (tag.option == null)
                    {
                        throw new IOException("Parse Error: could not parse Tag Option:" + line.options);
                    }
                }
                else
                {
                    tag.option = TagProcessingOption.KEEP; // no option -> keep.
                }
            }

            tags.add(tag);
        }

        // POST: check tags:

        for (int i = 0; i < tags.size(); i++)
        {
            TagDirective tag = tags.get(i);
            // logger.debugPritnf("TagOption: 0x%8x '%s' : %s\n",tag.tagNr,tag.name,tag.option);
            this.dicomTags.put(tag.tagNr, tag); // register
        }
    }

    public TagProcessingOption getOption(int tagNr)
    {
        TagDirective el = this.dicomTags.get(tagNr);
        if (el == null)
            return null;
        return el.option;
    }

    public TagProcessingOption getOption(int tagNr, TagProcessingOption defaultOption)
    {
        TagDirective el = this.dicomTags.get(tagNr);

        if (el == null)
            return defaultOption;
        return el.option;
    }

    /**
     * Whether to Keep the hash tag. If the tag needs to be cleared this method will return true as well.
     */
    public boolean keepTag(int tagNr)
    {
        TagDirective tag = this.dicomTags.get(tagNr);

        if (tag == null)
            return false;

        switch (tag.option)
        {
            case DELETE:
                return false;
            case KEEP:
            case HASH:
                return true;
                //
            default:
                return true;
        }
    }

    /** Really delete from Dicom File */
    public boolean deleteTag(int tagNr)
    {
        return (keepTag(tagNr) == false);
    }

    public String getTagDescription(int tagNr)
    {
        TagDirective el = this.dicomTags.get(tagNr);
        if (el == null)
            return null;
        return el.name;
    }

    public VR getVRofTag(int tagnr)
    {
        TagDirective tag = this.dicomTags.get(tagnr);
        if (tag == null)
            return null;

        return tag.vr;
    }

}

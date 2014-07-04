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

package nl.esciencecenter.medim.dicom;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import nl.esciencecenter.ptk.GlobalProperties;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.util.StringUtil;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.VR;
import org.dcm4che2.util.TagUtils;

/**
 * Dicom Dumper util. Prints out the DICOM metadata to CSV format.
 */
public class DicomDumper implements DicomObject.Visitor
{
    public static enum TagIdformat
    {
        HEXADECIMAL, SINGLE_INTEGER, INTEGER_PAIR
    };

    private PrintStream output;

    private String identStr;

    private String infixStr;

    private String eolStr;

    private TagIdformat tagIdformat = TagIdformat.INTEGER_PAIR;

    private boolean quoteAllFields = false;

    private String fieldSeperator = ";";

    private boolean noMagic;

    public DicomDumper(PrintStream output)
    {
        this.output = output;
    }

    public void setTagIdFormat(TagIdformat format)
    {
        this.tagIdformat = format;
    }

    public void dumpFile(String filename) throws IOException
    {
        java.io.File file = new java.io.File(filename);

        boolean isDicom = false;

        if (this.noMagic == false)
        {
            isDicom = DicomUtil.hasDicomMagic(file);
        }
        else
        {
            isDicom = true;
        }

        if (isDicom == false)
        {
            throw new FileNotFoundException("Wrong Magic: File is NOT Dicom file:" + file);
        }

        DicomObject obj = DicomUtil.readDicom(file);
        dumpDicomObject("", "", "\n", obj);
    }

    public void dumpDicomObject(DicomObject obj)
    {
        this.identStr = "";
        this.infixStr = "";
        this.eolStr = "\n";

        obj.accept(this);
    }

    public void dumpDicomObject(String indentStr, String infixStr, String eolStr, DicomObject obj)
    {
        this.identStr = "";
        this.infixStr = "";
        this.eolStr = "\n";

        obj.accept(this);
    }

    @Override
    public boolean visit(DicomElement dicomEl)
    {
        printElement(identStr, infixStr, eolStr, dicomEl);
        return true;
    }

    public void printElement(String prefix, String infix, String eol, DicomElement el)
    {
        // use StringBuffer
        StringBuffer sb = new StringBuffer();

        switch (tagIdformat)
        {
            case HEXADECIMAL:
            {
                addField(sb, StringUtil.toHexString("0x", el.tag(), true, 8), true, fieldSeperator);
                break;
            }
            case SINGLE_INTEGER:
            {
                addField(sb, "" + el.tag(), false, fieldSeperator);
                break;
            }
            default:
            {
                addField(sb, tagIdString(el.tag()), true, fieldSeperator);
            }
        }

        if (el.vr() == VR.SQ)
        {
            // outPrintf(prefix+"(%s,%s, #%d}\n",tagString(el.tag()),el.vr(),el.countItems());
            addField(sb, "SQ");
            addField(sb, "#" + el.countItems());
        }
        else
        {
            VR vr = el.vr();
            int len = el.length();
            int numValues = el.vm(null);

            addField(sb, "" + vr);
            addField(sb, getTagName(el.tag(), 34, true));
            // addField(sb,padd(""+len,' ',4,true));
            // addField(sb,padd(""+numValues,' ',4,true));

            if (vr == VR.OB)
            {
                addField(sb, "OtherBytes:#" + len);
            }
            else
            {
                this.addField(sb, getValueAsString(el, null, numValues, true), false, fieldSeperator);
            }

        }

        outPrintf(prefix + "%s\n", prefix + sb.toString());
    }

    private StringBuffer addField(StringBuffer sb, String value)
    {
        return addField(sb, value, quoteAllFields, fieldSeperator);
    }

    private StringBuffer addField(StringBuffer sb, String value, boolean quote, String fieldSeperator)
    {
        if (quote)
        {
            sb.append('"');
        }
        sb.append(value);
        if (quote)
        {
            sb.append('"');
        }
        if (fieldSeperator != null)
            sb.append(fieldSeperator);
        return sb;
    }

    private static String getTagName(int tag, int fixedLength, boolean alignLeft)
    {
        String name = DicomUtil.getTagName(tag);
        return padd(name, ' ', fixedLength, alignLeft);
    }

    public static String padd(String string, char fillChar, int fixedLength, boolean alignLeft)
    {
        if (string == null)
        {
            string = "";
        }

        int n = string.length();
        int diff = fixedLength - n;
        if (diff <= 0)
            return string;

        byte bytes[] = new byte[diff];
        for (int i = 0; i < diff; bytes[i] = (byte) fillChar, i++)
            ;
        if (alignLeft)
            return string + new String(bytes);
        else
            return new String(bytes) + string;

    }

    // charset may be null
    public static String getValueAsString(DicomElement el, SpecificCharacterSet charSet, int numValues, boolean quote)
    {
        String str = "";

        if (numValues <= 0)
        {
            str += "<null>";
        }
        else if (numValues == 1)
        {
            str += el.vr().toString(el.getBytes(), el.bigEndian(), charSet);
        }
        if (numValues <= 1)
        {
            if (quote)
                str = "\"" + str + "\"";
            return str;
        }

        String strs[] = el.getStrings(charSet, true);// el.toString(el.getBytes(),el.bigEndian(),charSet);
        String quoteStr = "";
        if (quote)
            quoteStr = "\"";

        str += "[" + new StringList(strs).toString(quoteStr, ",") + "]";

        return str;
    }

    public void outPrintf(String format, Object... args)
    {
        this.output.printf(format, args);
    }

    public static String tagIdString(int tag)
    {
        StringBuffer sb = new StringBuffer();
        TagUtils.toStringBuffer(tag, sb);
        return sb.toString();
    }

    public static void main(String args[])
    {
        String cwd = GlobalProperties.getGlobalUserDir();

        DicomDumper dcmDumpert = new DicomDumper(System.out);
        String fileName = null;

        boolean printUsage = false;

        for (String arg : args)
        {
            if (arg.startsWith("-fs=") || arg.startsWith("-fieldseperator=") || arg.startsWith("-fieldSeperator="))
            {
                int index = arg.indexOf('=');
                String subStr = arg.substring(index + 1, arg.length());
                // quoted char
                if (subStr.startsWith("'") && subStr.endsWith("'"))
                {
                    subStr.substring(1, subStr.length() - 1);
                }
                dcmDumpert.fieldSeperator = subStr;
            }
            else if (arg.equals("-ht") || arg.equals("-hextag") || arg.equals("-hexTag"))
            {
                dcmDumpert.setTagIdFormat(TagIdformat.HEXADECIMAL);
            }
            else if (arg.equals("-it") || arg.equals("-inttag") || arg.equals("-intTag"))
            {
                dcmDumpert.setTagIdFormat(TagIdformat.SINGLE_INTEGER);
            }
            else if (arg.equals("-pt") || arg.equals("-pairtag") || arg.equals("-pairTag"))
            {
                dcmDumpert.setTagIdFormat(TagIdformat.INTEGER_PAIR);
            }
            else if (arg.equals("-aq") || arg.equals("-allquotes") || arg.equals("-allQuotes"))
            {
                dcmDumpert.quoteAllFields = true;
            }
            else if (arg.equals("-nm") || arg.equals("-nomagic") || arg.equals("-noMagic"))
            {
                dcmDumpert.noMagic = true;
            }

            else if (arg.equals("-h") || arg.equals("-help"))
            {
                printUsage = true;
            }
            else
            {
                fileName = arg;
            }
        }

        if ((printUsage) || (StringUtil.isEmpty(fileName)))
        {
            printUsage();
            System.exit(-1);
        }

        try
        {
            dcmDumpert.dumpFile(fileName);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }

    public static void printUsage()
    {
        System.err.println("Usage: [options] <dicomFile> \n"
                + " Where options are:\n"
                + "   -ht, -hextag     print tag numbers as hexadecimal value. \n"
                + "   -it, -inttag     print tag numbers as single integer value.\n"
                + "   -pt, -pairtag    print tag numbers as integer word pair (is default).\n"
                );
    }

}

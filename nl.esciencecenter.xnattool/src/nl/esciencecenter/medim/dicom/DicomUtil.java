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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import nl.esciencecenter.medim.dicom.types.DicomTags;
import nl.esciencecenter.medim.dicom.types.DicomTypes;
import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.IOUtil;
import nl.esciencecenter.ptk.util.StringUtil;

import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.io.StopTagInputHandler;

/**
 * Dicom File manager. Todo: Check DicomDir utils.
 */
public class DicomUtil
{
    static
    {
        initTagNames();
    }

    static protected Map<Integer, String> tagNames = null;

    static protected void initTagNames()
    {
        tagNames = new Hashtable<Integer, String>();
        addTags(Tag.class, tagNames);
    }

    static private void addTags(Class clazz, Map<Integer, String> tagMap)
    {
        Field fields[] = clazz.getFields();

        for (int i = 0; i < fields.length; i++)
        {
            String tagName = fields[i].getName();
            if (tagName != null)
            {
                int nr = -1;

                try
                {
                    if (clazz == Tag.class)
                    {
                        nr = Tag.forName(tagName);
                    }

                    tagNames.put(nr, fields[i].getName());
                }
                catch (IllegalArgumentException e)
                {
                    ; // skip
                }
            }
        }

    }

    // === Tag Stuff ===
    public static boolean isTagField(String name)
    {
        try
        {
            Tag.forName(name);
            return true;
        }
        catch (IllegalArgumentException e)
        {
            return false;
        }
    }

    public static int getTagField(String name)
    {
        // use Tag:
        int tag = Tag.forName(name);
        return tag;
    }

    public static String getTagName(int tag)
    {
        if (tagNames == null)
            initTagNames();

        return tagNames.get(new Integer(tag));
    }

    // ========================================================================
    // Factory methods
    // ========================================================================

    public static DicomObject createNewDicom(boolean initDefault)
    {
        DicomObject dicom;

        if (initDefault)
            dicom = new BasicDicomObject(createDefaultDicom());
        else
            dicom = new BasicDicomObject();

        // defaults ?
        return dicom;
    }

    public static DicomObject createDefaultDicom()
    {
        DicomObject def = new BasicDicomObject();
        // smallest possible dicom object ?
        def.putString(Tag.TransferSyntaxUID, VR.UI, DicomTypes.EXPLICIT_VR_LITTLE_ENDIAN);

        return def;
    }

    // ========================================================================
    // Dicom Readers:
    // ========================================================================

    /** Reads dicom object from InputStream. Does not close stream */
    public static DicomObject readDicom(InputStream in, int maxTag) throws IOException
    {
        BufferedInputStream bin = new BufferedInputStream(in);
        DicomInputStream din = new DicomInputStream(bin);

        if (maxTag > 0)
            din.setHandler(new StopTagInputHandler(maxTag + 1));

        DicomObject dcm = din.readDicomObject();

        return dcm;
    }

    public static DicomObject readDicom(byte[] bytes) throws IOException
    {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);

        try
        {
            return readDicom(bis, -1);
        }
        finally
        {
            try
            {
                bis.close();
            }
            catch (IOException ignore)
            {
                ;
            }
        }
    }

    public static DicomObject readDicom(InputStream in) throws IOException
    {
        return readDicom(in, -1);
    }

    public static DicomObject readDicom(File file, int maxTag) throws IOException
    {
        InputStream fin = new FileInputStream(file);

        DicomObject dicomobject = null;

        try
        {
            if (file.getName().endsWith(".gz"))
                fin = new GZIPInputStream(fin);
            else if (file.getName().endsWith(".zip"))
                fin = new ZipInputStream(fin);

            dicomobject = readDicom(fin, maxTag);
        }
        catch (IOException e)
        {
            throw new IOException("Could readn't read file:" + file + "\n" + e.getMessage(), e);
        }
        finally
        {
            if (fin != null)
            {
                try
                {
                    fin.close();
                }
                catch (IOException ignore)
                {
                    ;
                }
            }
        }

        return dicomobject;

    }

    public static DicomObject readDicom(String filename) throws IOException
    {
        return readDicom(new java.io.File(filename), -1);
    }

    public static DicomObject readDicom(File file) throws IOException
    {
        return readDicom(file, -1);
    }

    public static boolean hasDicomMagic(FSPath node)
    {
        return hasDicomMagic(node.toJavaFile());
    }

    public static boolean hasDicomMagic(java.io.File javaFile)
    {
        FileInputStream fips = null;

        try
        {
            fips = new FileInputStream(javaFile);
            return hasDicomMagic(fips);
        }
        catch (FileNotFoundException e)
        {
            return false;
        }
        finally
        {
            if (fips != null)
            {
                try
                {
                    fips.close();
                }
                catch (IOException ignore)
                {
                    ;
                }
            }
        }
    }

    /**
     * Checks whether at position 128 (and further) the MAGIC bytes "DICM" are there.
     */
    public static boolean hasDicomMagic(InputStream inps)
    {
        int index = 128;
        int len = index + 4;
        byte buf[] = new byte[len];

        try
        {
            IOUtil.syncReadBytes(inps, 0, buf, 0, len, false);
            return hasDicomMagic(buf);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Checks whether bytes at index 128 and further contains magic bytes 'DICM'
     */
    public static boolean hasDicomMagic(byte[] buf)
    {
        int index = 128;
        if (buf.length < 128 + 4)
            return false;

        byte DICMbytes[] = new byte[]
        {
                'D', 'I', 'C', 'M'
        };

        for (int i = 0; i < DICMbytes.length; i++)
        {
            if (DICMbytes[i] != buf[index + i])
            {
                return false;
            }
        }
        return true;
    }

    // ========================================================================
    // Dicom Writers
    // ========================================================================

    public static File writeDicom(DicomObject dic, String fileName) throws IOException
    {
        File file = new java.io.File(fileName);
        FileOutputStream fos;

        try
        {
            fos = new FileOutputStream(file);
        }
        catch (FileNotFoundException e)
        {
            throw e;
        }

        try
        {
            writeDicom(dic, fos);
        }
        catch (IOException e)
        {
            throw new IOException("Failed to write to:" + file + "\n" + e.getMessage(), e);
        }
        finally
        {
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException ignore)
                {
                    ;
                }
            }
        }

        return file;
    }

    public static void writeDicom(DicomObject dicom, OutputStream outStream) throws IOException
    {
        BufferedOutputStream bos = new BufferedOutputStream(outStream);
        DicomOutputStream dos = new DicomOutputStream(bos);

        try
        {
            dos.writeDicomFile(dicom);
        }
        finally
        {
            try
            {
                dos.close();
            }
            catch (IOException ignore)
            {
                ;
            }
            try
            {
                bos.close();
            }
            catch (IOException ignore)
            {
                ;
            }
        }

        return;
    }

    public static byte[] getBytes(DicomObject dicom) throws IOException
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DicomOutputStream dos = new DicomOutputStream(bos);

        try
        {
            dos.writeDicomFile(dicom);
            return bos.toByteArray();
        }
        finally
        {
            try
            {
                dos.close();
            }
            catch (IOException ignore)
            {
                ;
            }
            try
            {
                bos.close();
            }
            catch (IOException ignore)
            {
                ;
            }
        }
    }

    // ========================================================================
    // UID methods
    // ========================================================================

    /**
     * Create dotted decimal UID using rootPrefix ("1.2.34. etc") as prefix. Created UID wil not be longer then
     * maxLength but may be shorter.
     */
    public static String createRandomUID(String rootPrefix, int maxLength) throws Exception
    {
        if (StringUtil.isWhiteSpace(rootPrefix))
        {
            throw new Exception("RootPrefix can't be empty");
        }

        UUID uuid = UUID.randomUUID();

        // use *BIG* Integers:
        BigInteger msb64 = new BigInteger("" + uuid.getMostSignificantBits());
        BigInteger lsb64 = new BigInteger("" + uuid.getLeastSignificantBits());

        if ((rootPrefix.endsWith(".") == false))
            rootPrefix = rootPrefix + ".";

        if (msb64.compareTo(BigInteger.ZERO) < 0)
            msb64 = msb64.negate();

        if (lsb64.compareTo(BigInteger.ZERO) < 0)
            lsb64 = lsb64.negate();

        String uid = rootPrefix + "" + msb64 + "." + lsb64;
        if (uid.length() >= maxLength)
            return uid.substring(0, maxLength);
        return uid;
    }

    public static VRType getVRType(VR vr)
    {
        return VRType.valueOf(vr);
    }

    public static VR getVRofTag(int tagnr) throws Exception
    {
        DicomTags tags = DicomTags.getDefault();

        if (tags == null)
            throw new Exception("DicomTag database not initialized!");

        return tags.getVRofTag(tagnr);
    }

}

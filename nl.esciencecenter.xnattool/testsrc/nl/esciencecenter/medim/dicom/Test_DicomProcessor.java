/*
 * Copyright 2012-2013 Netherlands eScience Center.
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

import java.io.IOException;

import nl.esciencecenter.medim.dicom.DicomProcessor;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.medim.dicom.types.DicomTags;
import nl.esciencecenter.medim.dicom.types.DicomTags.TagProcessingOption;
import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;
import nl.esciencecenter.ptk.util.StringUtil;

import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.junit.Assert;
import org.junit.Test;

public class Test_DicomProcessor
{
    @Test
    public void TestHashUID()
    {
        int maxB = 24;

        byte bytes[] = new byte[maxB];

        for (int i = 0; i < maxB; i++)
            bytes[i] = 0;

        // testHashUID("0.",bytes,"0.00000000000000000000000000000000000000000000000000000000");
        testHashUID("0.", bytes, "0.0");

        for (int i = 0; i < maxB; i++)
            bytes[i] = (byte) 255;
        // as checked by google: 24 bytes
        testHashUID("0.", bytes, "0.6277101735386680763835789423207666416102355444464034512895");
    }

    protected void testHashUID(String uidPrefix, byte[] hashBytes, String expectedUid)
    {
        String hashStr = DicomProcessor.createHashedUid(uidPrefix, hashBytes, 24);
        Assert.assertEquals("Expected hash doesn't match actual", expectedUid, hashStr);
    }

    // The cyclic exor is used to shorten hashes.
    @Test
    public void testExorBytes()
    {
        // trivial:
        byte bytes[] = new byte[2];
        bytes[0] = (byte) Integer.parseInt("10101010", 2);
        bytes[1] = (byte) Integer.parseInt("01010101", 2);
        byte result[] = DicomProcessor.exorBytes(bytes, 1);
        byte expected[] = new byte[] {
                (byte) 0x0ff
        };

        Assert.assertEquals("Exored result doesnt match expected", result[0], expected[0]);

        testExorBytes(new byte[] {}, new byte[] {}, 0);
        testExorBytes(new byte[] {
                0
        }, new byte[] {
                0
        }, 1);
        testExorBytes(new byte[] {
                (byte) 0xaa, (byte) 0x55
        }, new byte[] {
                (byte) 0xff
        }, 1);
        byte pattern1[] = new byte[] {
                (byte) 0xaa, 0x55, (byte) 0x55, (byte) 0xaa
        };
        testExorBytes(pattern1, new byte[] {
                (byte) 0x0
        }, 1);
        testExorBytes(pattern1, new byte[] {
                (byte) 0xff, (byte) 0xff
        }, 2);

        // Insert Fault:
        // testExorBytes(new byte[]{0},new byte[]{1},1);

    }

    private void testExorBytes(byte[] source, byte[] expectedResult, int maxLen)
    {
        byte result[] = DicomProcessor.exorBytes(source, maxLen);

        for (int i = 0; i < expectedResult.length; i++)
            Assert.assertEquals("Exored result doesnt match expected at #" + i, expectedResult[i], result[i]);
    }

    @Test
    public void testCreateDicom() throws Exception
    {
        // Test empty dicom
        DicomObject dicom = DicomUtil.createNewDicom(true);

        DicomWrapper wrap = new DicomWrapper(dicom, null);
        wrap.setIsModifyable(true);

        String uid = "1.0";
        wrap.setTag(Tag.StudyInstanceUID, "1.0");
        Assert.assertEquals(wrap.getStudyInstanceUID(), uid);
    }

    @Test
    public void testCreateDicomBytes() throws Exception
    {
        // Test empty dicom
        DicomObject dicom = DicomUtil.createNewDicom(true);

        DicomWrapper wrap = new DicomWrapper(dicom, null);
        wrap.setIsModifyable(true);

        String uid = "1.0";
        wrap.setTag(Tag.StudyInstanceUID, uid);
        Assert.assertEquals(wrap.getStudyInstanceUID(), uid);

        byte bytes[] = wrap.getBytes();
        Assert.assertTrue("bytes created from empty dicom object doesnt contain 'DICM' magic", DicomUtil.hasDicomMagic(bytes));

        DicomObject dicom2 = DicomUtil.readDicom(bytes);
        DicomWrapper wrap2 = new DicomWrapper(dicom2, null);
        Assert.assertEquals("Re-created Dicom Object must contain similar StudyInstanceUID", uid, wrap2.getStudyInstanceUID());

    }

    @Test
    public void testBasicTags() throws IOException
    {
        DicomObject dicom = DicomUtil.createNewDicom(false);
        testDicomSetGetField(dicom, Tag.StudyID, VR.SH, "id1");
    }

    protected void testDicomSetGetField(DicomObject dicom, int tagNr, VR vr, String value) throws IOException
    {
        dicom.putString(tagNr, vr, value);
        DicomElement el = dicom.get(tagNr);

        // use DicomWrapper
        String tagWrapValue = DicomWrapper.element2String(el, null);
        // use DicomElement
        String tagDirectValue = dicom.getString(tagNr);

        String tagFieldStr = ("#" + tagNr + "'" + DicomUtil.getTagName(tagNr) + "',VR=" + vr);
        Assert.assertEquals("Field value (DicomWrapper) doesn't match:" + tagFieldStr, value, tagWrapValue);
        Assert.assertEquals("Field value (DicomElement) doesn't match:" + tagFieldStr, value, tagDirectValue);

        // Check VR Type against database:
        VR configType = DicomTags.getDefault().getVRofTag(tagNr);

        // Config meta type 'OX' can match against 'OB' and 'OW'. Dicom Tags themself never have 'OX'.
        if (configType == VRType.OX.vr())
        {
            if ((vr == VRType.OB.vr()) || (vr == VRType.OW.vr()))
            {
                ; // ok
            }
            else
            {
                Assert.assertEquals("VR type 'OX' (from tag:" + tagFieldStr + ") configuration doesn't either 'OB' or 'OW'.", vr,
                        configType);
            }
        }
        else
        {
            Assert.assertEquals("VR type (from tag:" + tagFieldStr + ") configuration doesn't match used type.", vr, configType);
        }
    }

    @Test
    public void testBase64Encoding() throws IOException
    {
        // example taken from wiki:
        String val = "Man"; // 3 characters= 3x8 = 24 bits
        String val64 = "TWFu";// 3 bas64 characters = 4x 6 = 24 bits.
        testBase64Encoding(val, val64);
    }

    public void testBase64Encoding(String value, String base64) throws IOException
    {

        Assert.assertEquals("Base64 encoded string:'" + value + "' doesn't match expected.", base64,
                StringUtil.base64Encode(value.getBytes()));
        Assert.assertEquals("Base64 decoded string:'" + base64 + "' doesn't match expected.", value,
                new String(StringUtil.base64Decode(base64)));
    }

    @Test
    public void TestDicomTagsConfig() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0010,0x0010,PN,\"Patient Name\",1,HASH\n"
                + "0x0010,0x0020,LO,\"Patient ID\",1,HASH\n";

        DicomTags tags = DicomTags.createFromText(dicomConfig);

        TagProcessingOption opt = tags.getOption(0x00100010);
        // opt=null;
        Assert.assertEquals("Tag Option of PatientName must be 'HASH'", TagProcessingOption.HASH, opt);

        opt = tags.getOption(0x00100020);
        Assert.assertEquals("Tag Option of PatientId must be 'HASH'", TagProcessingOption.HASH, opt);

        Assert.assertEquals("VR Type of PatientName must be PN", VR.PN, tags.getVRofTag(0x00100010));
        Assert.assertEquals("VR Type of PatientID must be LO", VR.LO, tags.getVRofTag(0x00100020));

    }

    @Test
    public void TestHashStringTags() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0010,0x0010,PN,\"Patient Name\",1,HASH\n"
                + "0x0010,0x0020,LO,\"Patient ID\",1,HASH\n";

        DicomTags tags = DicomTags.createFromText(dicomConfig);
        DicomProcessor defProc = new DicomProcessor(tags, DicomTestUtil.createDefaultProcOpts("testid", "12345"));

        // ID and single component of a PatientName may not exceed 64 bytes!
        // 32 byte hash = 44 character base64 encoded String.

        testProcessStringTag(defProc, "patientId", Tag.PatientID, "OQ7j3Pylj4ePLwafb62s15Iw05sjNlwXU7lWooz/5Ic=", 44);
        testProcessStringTag(defProc, "patientName", Tag.PatientName, "+mdCPEZtV/tIzprOh3gBRU8n1kGfUxpcp2BXkEwwI18=", 44);
    }

    protected void testProcessStringTag(DicomProcessor proc, String value, int tagNr, String expected, int expectedSize) throws Exception
    {
        DicomWrapper dummy = DicomTestUtil.createDummyDicom();

        dummy.setIsModifyable(true);
        dummy.setTag(tagNr, value);
        String result = dummy.getStringValue(tagNr);

        // test actual 'set' method.
        Assert.assertEquals("Test (dicom) object doesn't have required value! (setTag failed)", value, result);

        // process:
        dummy = proc.process(dummy);

        // result:
        result = dummy.getStringValue(tagNr);
        Assert.assertEquals("Hashed value doesnt match for Tag:" + DicomUtil.getTagName(tagNr) + ".", expected, result);
        int len = result.length();
        // Insert Fault:
        // len=1000;
        Assert.assertEquals("Character length of base64 encoded/encrypted String doesn't match expected:" + len + "!=" + expectedSize,
                expectedSize, len);
        byte bytes[] = result.getBytes();
        len = bytes.length;

        Assert.assertEquals(
                "Actual byte length of Java String doesn't match the number of characters. This can happen when using non ASCII characters!",
                expectedSize, len);
        System.out.printf("Length of Hash String=%d\n", result.length());

    }

    @Test
    public void TestHashUIDStringTags() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0020,0x000D,UI,\"Study Instance UID\",1,HASH_UID\n"
                + "0x0020,0x000E,UI,\"Series Instance UID\",1,HASH_UID\n";

        DicomTags tags = DicomTags.createFromText(dicomConfig);
        DicomProcessor defProc = new DicomProcessor(tags, DicomTestUtil.createDefaultProcOpts("testid", "12345"));

        // ID and single component of a PatientName may not exceed 64 bytes!
        // 32 byte hash = 44 character base64 encoded String.
        testProcessStringTag(defProc, "1.2.3.4.5", Tag.StudyInstanceUID, "99.4204629107754314596487061406589837754705824752771508063909",
                61);
        testProcessStringTag(defProc, "1.2.3.4.5.6", Tag.SeriesInstanceUID,
                "99.4546193923923468677598443730882976073381443147326160939328", 61);
    }

    @Test
    public void testEncryptStringTags() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0010,0x0010,PN,\"Patient Name\",1,ENCRYPT\n"
                + "0x0010,0x0020,LO,\"Patient ID\",1,ENCRYPT\n";

        String sourceId = "testid";
        String password = "12345";

        DicomTags tags = DicomTags.createFromText(dicomConfig);
        DicomProcessor defProc = new DicomProcessor(tags, DicomTestUtil.createDefaultProcOpts(sourceId, password));

        // prefix id to used password for extra salt (not used anymore)
        // echo -n patientIdValue | openssl enc -des-ede3 -nosalt -pass pass:testid12345 -base64 -md sha256 -p
        // key=19362A9D53D6A85E1D82DE046CE3C42D648EDC4A8BA3B1D9
        // eyPW8jrHLUZHrk4v66OfDQ==

        // echo -n patientIdValue | openssl enc -des-ede3 -nosalt -pass pass:12345 -base64 -md sha256 -p
        // key=5994471ABB01112AFCC18159F6CC74B4F511B99806DA59B3
        // XP6jSIrgIQJfCdRLTgelpw==

        testProcessStringTag(defProc, "patientIdValue", Tag.PatientID, "XP6jSIrgIQJfCdRLTgelpw==", 24);

        // echo -n patientIdValue | openssl enc -des-ede3 -nosalt -pass pass:testid12345 -base64 -md sha256 -p
        // key=19362A9D53D6A85E1D82DE046CE3C42D648EDC4A8BA3B1D9
        // eyPW8jrHLUZHrk4v66OfDQ==

        // echo -n patientNameValue | openssl enc -des-ede3 -nosalt -pass pass:12345 -base64 -md sha256 -p
        // key=5994471ABB01112AFCC18159F6CC74B4F511B99806DA59B3
        // Ibr09TaiR4rbwFa3LgpncBmtruIG+dPX

        testProcessStringTag(defProc, "patientNameValue", Tag.PatientName, "Ibr09TaiR4rbwFa3LgpncBmtruIG+dPX", 32);
    }

    @Test
    public void TestEncryptHashStringTags() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0010,0x0010,PN,\"Patient Name\",1,ENCRYPT_HASH\n"
                + "0x0010,0x0020,LO,\"Patient ID\",1,ENCRYPT_HASH\n";

        DicomTags tags = DicomTags.createFromText(dicomConfig);
        DicomProcessor defProc = new DicomProcessor(tags, DicomTestUtil.createDefaultProcOpts("testid", "12345"));

        // ID and single component of a PatientName may not exceed 64 bytes!
        // 32 byte hash = 44 character base64 encoded String.
        testProcessStringTag(defProc, "patientId", Tag.PatientID, "srW4c6dtlalKhcQjM3UOFTa9ZwklOqn6CBLe7angDTQ=", 44);
        testProcessStringTag(defProc, "patientName", Tag.PatientName, "r90x56PiYEZUUGY0YpA6x6Cji3tEgvKOCKahHKaMkGs=", 44);
    }

    @Test
    public void TestEncryptHashUIDStringTags() throws Exception
    {
        String dicomConfig = "Group,Element,VR,Name,Keep,Option\n"
                + "0x0020,0x000D,UI,\"Study Instance UID\",1,ENCRYPT_HASH_UID\n"
                + "0x0020,0x000E,UI,\"Series Instance UID\",1,ENCRYPT_HASH_UID\n";

        DicomTags tags = DicomTags.createFromText(dicomConfig);
        DicomProcessor defProc = new DicomProcessor(tags, DicomTestUtil.createDefaultProcOpts("testid", "12345"));

        // ID and single component of a PatientName may not exceed 64 bytes!
        // 32 byte hash = 44 character base64 encoded String.
        testProcessStringTag(defProc, "1.2.3.4.5", Tag.StudyInstanceUID, "99.4172487386883469284013742100485763316631486852937852953349",
                61);
        testProcessStringTag(defProc, "1.2.3.4.5.6", Tag.SeriesInstanceUID,
                "99.5865990744155969053175599961344212888999959344684303397440", 61);

    }

}

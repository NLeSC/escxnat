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

import nl.esciencecenter.medim.dicom.DicomProcessingProfile;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.SubjectKeyType;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.medim.dicom.types.DicomTags;
import nl.esciencecenter.ptk.crypt.CryptScheme;
import nl.esciencecenter.ptk.crypt.StringHasher;

import org.dcm4che2.data.DicomObject;

public class DicomTestUtil
{
    public static DicomWrapper createDummyDicom()
    {
        // Test empty dicom
        DicomObject dicom = DicomUtil.createNewDicom(true);

        DicomWrapper wrap = new DicomWrapper(dicom, null);
        wrap.setIsModifyable(true);

        return wrap;
    }

    // read configuration file.
    public static DicomTags createConfiguredDicomTagOptions() throws IOException
    {
        DicomTags tagOpts = DicomTags.createFromFile("dicom/dicom_tags.xcsv");
        return tagOpts;
    }

    public static DicomProcessingProfile createDefaultProcOpts(String sourceId, String password) throws Exception
    {

        // default is to use the sourceId as salt source.
        byte saltBytes[] = sourceId.getBytes("UTF-8");

        DicomProcessingProfile opts = new DicomProcessingProfile(
                CryptScheme.DESEDE_ECB_PKCS5,
                24,
                "99.",
                SubjectKeyType.CRYPTHASH_PATIENT_ID,
                saltBytes,
                false);

        StringHasher hasher = new StringHasher(StringHasher.SHA_256);

        // Default use SHA-256 of passphrase to convert to bytes
        byte key[] = hasher.hash(password, true, null, false);

        opts.setEncryptionKey(key);

        return opts;
    }

}

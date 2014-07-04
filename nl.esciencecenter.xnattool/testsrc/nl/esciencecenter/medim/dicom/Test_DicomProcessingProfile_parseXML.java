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

import nl.esciencecenter.medim.dicom.DicomProcessingProfile;
import nl.esciencecenter.ptk.xml.XmlUtil;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

public class Test_DicomProcessingProfile_parseXML
{

    @Test
    public void testCreateProfile() throws Exception
    {
        DicomProcessingProfile procOpts = DicomTestUtil.createDefaultProcOpts("testid", "12345");

        String xml = procOpts.toXML();

        System.out.println(XmlUtil.prettyFormat(xml, 3));

        DicomProcessingProfile procOpts2 = DicomProcessingProfile.parseXML(xml);

        compare(procOpts, procOpts2);

    }

    private void compare(DicomProcessingProfile procOpts1, DicomProcessingProfile procOpts2) throws JsonProcessingException
    {

        Assert.assertEquals("Fields 'profileName' do not match.", procOpts1.getProfileName(), procOpts2.getProfileName());
        Assert.assertEquals("Fields 'maxHashUIDByteLength' do not match.", procOpts1.getMaxHashedUIDByteLength(),
                procOpts2.getMaxHashedUIDByteLength());

        Assert.assertEquals("Fields 'defaultUIDOption' do not match.", procOpts1.getDefaultUIDOption(), procOpts2.getDefaultUIDOption());
        Assert.assertEquals("Fields 'uidPrefix' do not match.", procOpts1.getUIDPrefix(), procOpts2.getUIDPrefix());
        Assert.assertEquals("Fields 'processDicom' do not match.", procOpts1.getDoProcessDicom(), procOpts2.getDoProcessDicom());

        Assert.assertEquals("XML strings must match.", procOpts1.toXML(), procOpts2.toXML());

    }

}

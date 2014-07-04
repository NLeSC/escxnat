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
package tests.other;

import java.io.IOException;
import java.util.List;

import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;

import org.dcm4che2.data.DicomObject;
import org.junit.Test;

public class Test_GetTagNames
{
    @Test
    public void testGetTagNames()
    {
        java.net.URL dcmUrl = this.getClass().getClassLoader().getResource("tests/data/test_dicom1.dcm");
        testRead(dcmUrl.getPath());
    }

    protected void testRead(String filename)
    {
        java.io.File file = new java.io.File(filename);

        try
        {
            DicomObject obj = DicomUtil.readDicom(file);
            DicomWrapper wrap = new DicomWrapper(obj, file.toURI());
            List<String> names = wrap.getTagNames(true);

            for (String name : names)
            {
                outPrintf(" - %s\n", name);
            }

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    static void outPrintf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

}

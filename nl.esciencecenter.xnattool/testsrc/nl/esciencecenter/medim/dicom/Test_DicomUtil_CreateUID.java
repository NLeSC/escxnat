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

import org.junit.Assert;
import org.junit.Test;

import nl.esciencecenter.medim.dicom.DicomUtil;

public class Test_DicomUtil_CreateUID
{
    @Test
    public void testConstructors() throws Exception
    {
        testCreateRandomUid("9.99", 64);
        testCreateRandomUid("9.99.", 64);
        testCreateRandomUid("0", 64);
        testCreateRandomUid("0", 32);
        testCreateRandomUid("0", 7);
        // invalid:
        testCreateUidException("", 64);
    }

    void testCreateRandomUid(String prefix, int maxLen) throws Exception
    {
        //
        int minlen = prefix.length() + 32;

        boolean endsWithDot = prefix.endsWith(".");

        String uuid = DicomUtil.createRandomUID(prefix, maxLen);
        infoPrintf("UUID('%s',%d)=%s\n", prefix, maxLen, DicomUtil.createRandomUID(prefix, maxLen));

        Assert.assertNotNull("Created UUID may not be null", uuid);
        Assert.assertTrue("Actual size must be less or equal than desired length", (uuid.length() <= maxLen));

        String uuidPrefix = uuid.substring(0, prefix.length());
        Assert.assertEquals("UUID must start with actual prefix string\n", uuidPrefix, prefix);

        if (endsWithDot == false)
        {
            char dotC = uuid.charAt(prefix.length());
            Assert.assertEquals("Prefix without dot must have dot between prefix and UUID part:" + uuid, dotC, '.');
        }
        else
        {
            ;// ok
        }
    }

    public void infoPrintf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

    public void testCreateUidException(String prefix, int maxLen)
    {
        try
        {
            String uuid = DicomUtil.createRandomUID(prefix, maxLen);
            infoPrintf("UUID('%s',%d)=%s\n", prefix, maxLen, DicomUtil.createRandomUID(prefix, maxLen));
            Assert.fail("Constructor must throw Exception");
        }
        catch (Exception e)
        {
            infoPrintf("Invalid constructor call, throws expected exception:%s\n", e.getClass());
        }
    }

}

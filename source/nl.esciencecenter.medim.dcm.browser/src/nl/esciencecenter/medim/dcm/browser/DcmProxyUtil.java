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
package nl.esciencecenter.medim.dcm.browser;

import java.io.IOException;
import java.util.List;

import org.dcm4che2.data.Tag;

import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSNode;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;

public class DcmProxyUtil
{
    private static int[] defaultTagNumbers; 
    private static String[] defaultTagNames; 
    
    static
    {
        initDefaultTags();  
    }
    
    private static void initDefaultTags()
    {
        defaultTagNumbers=new int[]{
            Tag.PatientID,
            Tag.PatientName,
            Tag.SeriesInstanceUID,
            Tag.SeriesNumber,
            Tag.StudyInstanceUID,
            Tag.StudyID, 
            Tag.SeriesDescription,
            // Tag.SeriesDescriptionCodeSequence,
            Tag.SeriesDate,
            Tag.SeriesTime, 
            Tag.StudyDescription,
            Tag.StudyDate,
            Tag.StudyTime
        };
        
        StringList tagList=new StringList(); 
        
        for (int i=0;i<defaultTagNumbers.length;i++)
        {
            tagList.add(DicomUtil.getTagName(defaultTagNumbers[i])); 
        }
        
        defaultTagNames=tagList.toArray(); 
    }
    
    public static List<String> getDicomTagNames(FSNode fsNode, boolean sort) throws IOException
    {
        DicomWrapper wrap;
        wrap = getDicom(fsNode);
        return wrap.getTagNames(sort); 
    }

    public static DicomWrapper getDicom(FSNode fsNode) throws IOException
    {
        return DicomWrapper.readFrom(fsNode.getURI()); 
    }

    public static String[] getDefaultDicomAttributeNames()
    {
        return defaultTagNames;
    }

    public static int getTagNumber(String name)
    {
        return DicomUtil.getTagField(name); 
    }

    public static boolean isTagName(String name)
    {
        // TODO Auto-generated method stub
        return false;
    }    
    
}

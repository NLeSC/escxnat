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

package nl.esciencecenter.medim;

import java.util.Date;

/**
 * Study or Session info is a Descriptor for a collection of ScanSets (Series).
 * One Study or Session can have multiple ScanSets or Series.
 * Typically these Scan Set are made during the same session or Patient Visit and differ only in modality and used Scan Parameters.  
 *  
 * @author Piter T. de Boer
 */
public class StudyInfo extends DataInfo
{

    protected StudyInfo()
    {
    }

    public StudyInfo(String studyUid, String studyLabel)
    {
        this.setStudyInstanceUID(studyUid);
        this.setStudyId(studyLabel);
    }

    /**
     * Study Instance UID groups a set of ScanSet (Series)
     */
    protected String studyInstanceUID;

    /**
     * Custom studyId. Is not unique and not an UID.
     */
    protected String studyId;

    protected String studyDescription;

    protected Date studyDate;

    public String getStudyDescription()
    {
        return studyDescription;
    }

    protected void setStudyInstanceUID(String studyUid)
    {
        this.studyInstanceUID = studyUid;
    }

    public String getStudyInstanceUID()
    {
        return studyInstanceUID;
    }

    public String getStudyId()
    {
        return studyId;
    }

    protected void setStudyId(String studyId)
    {
        this.studyId = studyId;
    }

    public Date getStudyDate()
    {
        return studyDate;
    }

    protected void setStudyDate(Date studyDate)
    {
        this.studyDate = studyDate;
    }

    @Override
    public String getDataType()
    {
        return "StudyInfo";
    }

    @Override
    public String getDataUID()
    {
        return this.studyInstanceUID;
    }

}
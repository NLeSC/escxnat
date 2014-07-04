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
 * SubjectInfo contains relevant meta-data about the subject (patient) as parsed from the DICOM (meta) data fields. <br>
 */
public class SubjectInfo extends DataInfo
{
    /**
     * Default use PatientID as ID field, but sometimes the PatientName is used as ID.
     */
    private boolean usePatientNameAsID = false;

    private String dcmPatientName;

    private String dcmPatientID;

    // M/F/U/B/?
    private String dcmPatientGender;

    /**
     * Dicom patient Age String is a 4 char String ::= 'xxxW' | 'xxxM' | 'xxxY'
     */
    private String dcmPatientAgeString;

    private Date dcmPatientBirthDate;

    public SubjectInfo()
    {
    }
    
    @Override
    public String getDataType()
    {
        return "SubjectInfo";
    }

    @Override
    public String getDataUID()
    {
        return getSubjectID(); 
    }
    
    public String getPatientID()
    {
        return dcmPatientID;
    }

    public String getPatientName()
    {
        return dcmPatientName;
    }

    public String getSubjectID()
    {
        if (usePatientNameAsID)
            return this.dcmPatientName;
        else
            return this.dcmPatientID;
    }

    public String getPatientGender()
    {
        return dcmPatientGender;
    }

    public void setPatientName(String patientName)
    {
        this.dcmPatientName = patientName;
    }

    public void setPatientID(String patientID)
    {
        this.dcmPatientID = patientID;
    }

    /**
     * @return Patient Age String as specified in DICOM (xxxW,xxxM,xxxY)
     */
    public String getPatientAgeString()
    {
        return this.dcmPatientAgeString;
    }

    public Date getPatientBirthDate()
    {
        return this.dcmPatientBirthDate;
    }

    public void setPatientAgeString(String ageString)
    {
        this.dcmPatientAgeString = ageString;
    }

    public void setPatientGender(String genderString)
    {
        this.dcmPatientGender = genderString;
    }

    public void setPatientBirthDate(Date date)
    {
        this.dcmPatientBirthDate = date;
    }


}
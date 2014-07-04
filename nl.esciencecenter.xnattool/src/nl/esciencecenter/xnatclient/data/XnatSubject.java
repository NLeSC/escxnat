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

package nl.esciencecenter.xnatclient.data;

import java.util.List;
import java.util.Map;

import nl.esciencecenter.xnatclient.data.XnatTypes.Gender;
import nl.esciencecenter.xnatclient.data.XnatTypes.Handedness;

public class XnatSubject extends XnatObject
{
    /**
     * Create new XnatSubject Object without additional meta data. Must specify matching subject ID and subject Label.
     * 
     * @param subjectId
     * @param subjectLabel
     * @param subjectLabel2
     * @return New XnatSubject Object, but without extra (meta) data fields.
     */
    public static XnatSubject createXnatSubject(String projectId, String subjectLabel)
    {
        XnatSubject subj = new XnatSubject();
        subj.setProjectID(projectId);
        subj.setLabel(subjectLabel);
        return subj;
    }

    protected XnatSubject()
    {
        super(XnatObjectType.XNAT_SUBJECT);
    }

    public XnatSubject(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_SUBJECT, fields);
    }

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getSubjectFieldNames(true).toArray(new String[] {});
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return XnatConst.getSessionFieldNames(false);
    }

    /**
     * Return XNAT Subject ID. Only set when Subject has been registered. XDAT: Out Parameter.
     */
    public String getID()
    {
        return get(XnatConst.FIELD_ID);
    }

    /**
     * Subject label as specified when registering this subject. XDAT: In/Out parameter.
     * 
     * @return
     */
    public String getLabel()
    {
        return get(XnatConst.FIELD_LABEL);
    }

    protected void setLabel(String label)
    {
        set(XnatConst.FIELD_LABEL, label);
    }

    protected void setID(String id)
    {
        set(XnatConst.FIELD_ID, id);
    }

    protected void setProjectID(String projectId)
    {
        set(XnatConst.FIELD_PROJECT_ID, projectId);
    }

    public String getProjectID()
    {
        return get(XnatConst.FIELD_PROJECT_ID);
    }

    /**
     * Date when record has been inserted in Xnat DataBase. XDAT: Out parameter.
     * 
     * @return
     */
    public String getInsertDate()
    {
        return get(XnatConst.FIELD_INSERT_DATE);
    }

    /**
     * User which has inserted record into Xnat Database. XDAT: Out parameter.
     * 
     * @return
     */
    public String getInsertUser()
    {
        return get(XnatConst.FIELD_INSERT_USER);
    }

    /**
     * XDAT: Out parameter.
     */
    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

    /**
     * Get Age in years. XDAT: In/Out parameter
     * 
     * @return
     */
    public String getAge()
    {
        return get(XnatConst.FIELD_SUBJECT_AGE);
    }

    /**
     * Set Age in years. XDAT: In/Out parameter
     * 
     * @return
     */
    public void setAge(int age)
    {
        set(XnatConst.FIELD_SUBJECT_AGE, "" + age);
    }

    /**
     * Set Gender: Male/Female/Unknown. XDAT: In/Out parameter.
     * 
     * @return
     */
    public String getGender()
    {
        return get(XnatConst.FIELD_SUBJECT_GENDER);
    }

    public String getHandedness()
    {
        return get(XnatConst.FIELD_SUBJECT_HANDEDNESS);
    }

    public void setHandedness(Handedness hand)
    {
        set(XnatConst.FIELD_SUBJECT_HANDEDNESS, "" + hand);
    }

    public void setGender(Gender gender)
    {
        set(XnatConst.FIELD_SUBJECT_GENDER, "" + gender);
    }

    public boolean isStandardField(String fieldName, boolean ignoreCase)
    {
        return XnatConst.isSubjectField(fieldName, ignoreCase);
    }
}

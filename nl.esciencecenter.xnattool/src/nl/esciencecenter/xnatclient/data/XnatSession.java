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

import java.util.Date;
import java.util.List;
import java.util.Map;

import nl.esciencecenter.ptk.presentation.Presentation;

public class XnatSession extends XnatObject
{
    /**
     * Create new XnatSession Object. (Meta)Data fields are empty.
     * 
     * @return
     */
    public static XnatSession createXnatSession(String projectId, String subjectLabel, String sessionLabel)
    {
        XnatSession session = new XnatSession(sessionLabel);
        session.setProjectId(projectId);
        session.setSubjectLabel(subjectLabel);
        return session;
    }

    private String subjectLabel;

    protected XnatSession()
    {
        super(XnatObjectType.XNAT_SESSION);
    }

    protected XnatSession(String label)
    {
        super(XnatObjectType.XNAT_SESSION);
        this.setLabel(label);
    }

    public XnatSession(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_SESSION, fields);
    }

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getSessionFieldNames(true).toArray(new String[] {});
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return XnatConst.getSessionFieldNames(false);
    }

    public void setSubjectLabel(String subjectLabel)
    {
        // not part of XDAT object: Store here:
        this.subjectLabel = subjectLabel;
    }

    public String getSubjectLabel()
    {
        // not part of XDAT object: Store here:
        return this.subjectLabel;
    }

    public String getSubjectAssessorDataID()
    {
        return get(XnatConst.FIELD_SUBJECTASSESSORDATA_ID);
    }

    public void setProjectId(String projectId)
    {
        set(XnatConst.FIELD_PROJECT_ID, projectId);
    }

    public String getProjectID()
    {
        return get(XnatConst.FIELD_PROJECT_ID);
    }

    public String getLabel()
    {
        return get(XnatConst.FIELD_LABEL);
    }

    protected void setLabel(String label)
    {
        set(XnatConst.FIELD_LABEL, label);
    }

    public String getSessionDateString()
    {
        return get(XnatConst.FIELD_DATE);
    }

    public Date getSessionDate()
    {
        String dateStr = getSessionDateString();
        if (dateStr == null)
            return null;

        return Presentation.createDateFromNormalizedDateTimeString(dateStr);
    }

    public String getXsiType()
    {
        return get(XnatConst.FIELD_XSITYPE);
    }

    public String getInsertDate()
    {
        return get(XnatConst.FIELD_INSERT_DATE);
    }

    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

    public void setSessionDate(Date date)
    {
        set(XnatConst.FIELD_DATE, Presentation.createNormalizedDateTimeString(date));
    }

    public String getAgeString()
    {
        return get(XnatConst.FIELD_SESSION_AGE);
    }

    public void setAge(String ageString)
    {
        set(XnatConst.FIELD_SESSION_AGE, ageString);
    }

    public boolean isStandardField(String fieldName, boolean ignoreCase)
    {
        return XnatConst.isSessionField(fieldName, ignoreCase);
    }

}

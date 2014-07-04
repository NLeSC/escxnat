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

public class XnatReconstruction extends XnatObject
{

    public XnatReconstruction()
    {
        super(XnatObjectType.XNAT_RECONSTRUCTION);
    }

    public XnatReconstruction(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_RECONSTRUCTION, fields);
    }

    @Override
    public void set(String name, String value)
    {
        super.set(name, value);
    }

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getReconstructionFieldNames();
    }

    public String getID()
    {
        return get(XnatConst.FIELD_ID);
    }

    public String getReconstructionType()
    {
        return get(XnatConst.FIELD_TYPE);
    }

    public String getXsiType()
    {
        return get(XnatConst.FIELD_XSITYPE);
    }

    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return null;
    }

    public void setIDMapping(String projectid, String subjectLabel, String sessionLabel)
    {
        // Project/Subject/Session IDs+labels are not yet stored in Scan objects...
        // setProjectId(projectid);
        // setSubjectLabel(subjectLabel);
        // setSessionLabel(sessionLabel);
    }

}

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

public class XnatProject extends XnatObject
{
    protected static XnatProject createXnatProject()
    {
        XnatProject newProj = new XnatProject();
        return newProj;
    }

    protected XnatProject()
    {
        super(XnatObjectType.XNAT_PROJECT);
    }

    public XnatProject(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_PROJECT, fields);
    }

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getProjectFieldNames();
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return null;
    }

    @Override
    public String getID()
    {
        return get(XnatConst.FIELD_ID);
    }

    protected void setID(String id)
    {
        this.set(XnatConst.FIELD_ID, id);
    }

    public String getProjectID()
    {
        return get(XnatConst.FIELD_PROJECT_ID);
    }

    private void setProjectID(String id)
    {
        set(XnatConst.FIELD_PROJECT_ID, id);
    }

    public String getSecondaryID()
    {
        return get(XnatConst.FIELD_SECONDARY_ID);
    }

    public String getName()
    {
        return get(XnatConst.FIELD_NAME);
    }

    public String getDescription()
    {
        return get(XnatConst.FIELD_DESCRIPTION);
    }

    public String getPIFirstname()
    {
        return get(XnatConst.FIELD_PI_FIRSTNAME);
    }

    public String getPILastname()
    {
        return get(XnatConst.FIELD_PI_LASTNAME);
    }

    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

}

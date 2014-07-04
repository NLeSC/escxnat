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

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import nl.esciencecenter.ptk.data.StringList;

/**
 * Name,Value Store for XnatData. Fields are data field as return by the REST client. Attributes are extra (meta) data
 * attributes which are optional.
 * 
 * @author Piter T. de Boer
 * 
 */
public abstract class XnatObject
{
    public static enum XnatObjectType
    {
        XNAT_PROJECT, XNAT_SUBJECT, XNAT_SESSION, XNAT_SCAN, XNAT_RECONSTRUCTION, XNAT_FILE
    }

    /**
     * Factory method for XnatObjects.
     */
    public static XnatObject create(XnatObjectType newType)
    {
        switch (newType)
        {
            case XNAT_PROJECT:
                return new XnatProject();
            case XNAT_SUBJECT:
                return new XnatSubject();
            case XNAT_SESSION:
                return new XnatSession();
            case XNAT_SCAN:
                return new XnatScan();
            case XNAT_FILE:
                return new XnatFile();
            case XNAT_RECONSTRUCTION:
                return new XnatReconstruction();
            default:
                throw new Error("Invalid XnatObjectType:" + newType);

        }
        // return null;
    }

    // === instance ===

    protected XnatObjectType xnatType;

    protected Map<String, String> fieldMap = new Hashtable<String, String>();

    protected StringList customDataFieldNames = new StringList();

    public XnatObject(XnatObjectType type, Map<String, String> fields)
    {
        this.xnatType = type;
        init(fields);
    }

    protected void init(Map<String, String> fields)
    {
        String[] names = this.getFieldNames();
        for (int i = 0; i < names.length; i++)
        {
            String value = fields.get(names[i]);
            if (value != null)
                fieldMap.put(names[i], value);
        }
    }

    protected XnatObject(XnatObjectType type)
    {
        this.xnatType = type;
    }

    public XnatObjectType getObjectType()
    {
        return xnatType;
    }

    public String getID()
    {
        return get(XnatConst.FIELD_ID);
    }

    public void updateId(String idStr)
    {
        String oldIdstr = get(XnatConst.FIELD_ID);

        if ((oldIdstr != null) && (oldIdstr.equals(idStr) == false))
        {
            throw new RuntimeException("Cannot set ID of Object which already an other ID. Old Id=" + oldIdstr + ";New ID=" + idStr);
        }

        set(XnatConst.FIELD_ID, idStr);
    }

    public boolean hasField(String fieldName)
    {
        return (this.get(fieldName) != null);
    }

    public void set(String name, String value)
    {
        // check if name in field name map ?
        this.fieldMap.put(name, value);
    }

    public String get(String name)
    {
        return this.fieldMap.get(name);
    }

    public String get(int index)
    {
        return this.fieldMap.get(getFieldName(index));
    }

    public String getFieldName(int index)
    {
        String names[] = this.getFieldNames();

        if ((index < 0) || (index > names.length))
            return null;

        return names[index];
    }

    public String toString()
    {
        return toString("", "", ",", "");
    }

    public String toString(String prefix, String indentStr, String fieldSepStr, String eolStr)
    {
        String str = prefix + "[" + getObjectType() + ":" + eolStr;

        String names[] = this.getFieldNames();
        for (int i = 0; i < names.length; i++)
        {
            str += prefix + indentStr + names[i] + "=" + get(names[i]);
            if (i + 1 < names.length)
            {
                str += fieldSepStr;
            }
        }
        str += eolStr;
        str += prefix + "]";

        return str;
    }

    // --------------------------
    // custom (meta) data field
    // --------------------------

    /**
     * Custom meta data field. Must be created first in the DNAT database.
     * 
     * @param name
     * @param value
     */
    public void setCustomField(String name, String value)
    {
        this.customDataFieldNames.addUnique(name);
        this.fieldMap.put(name, value);
    }

    public String getCustomField(String name)
    {
        if (name == null)
            return null;
        return this.fieldMap.get(name);
    }

    public List<String> getCustomFieldNames()
    {
        return this.customDataFieldNames;
    }

    public List<String> getMetaDataFieldNames(boolean includeCustomFieldNames)
    {

        StringList list = new StringList(getMetaDataFieldNames());

        if (includeCustomFieldNames)
        {
            list.merge(new StringList(customDataFieldNames));
        }
        return list;
    }

    // === interface ===

    abstract public String[] getFieldNames();

    abstract public List<String> getMetaDataFieldNames();

}

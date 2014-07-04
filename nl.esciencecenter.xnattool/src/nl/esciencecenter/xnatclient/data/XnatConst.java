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

import nl.esciencecenter.ptk.data.StringList;

public class XnatConst
{
    public static final String FIELD_ID = "ID";

    public static final String FIELD_SECONDARY_ID = "secondary_ID";

    public static final String FIELD_NAME = "Name";

    public static final String FIELD_DESCRIPTION = "description";

    public static final String FIELD_PI_FIRSTNAME = "pi_firstname";

    public static final String FIELD_PI_LASTNAME = "pi_lastname";

    /** not full URI but relative path part of URI */
    public static final String FIELD_URI_PATH = "URI";

    /** Label is used in REST uris! */
    public static final String FIELD_LABEL = "label";

    public static final String FIELD_PROJECT_ID = "project";

    // Object hierarchy.
    public static final String FIELD_SUBJECT_ID = "subjectId";

    public static final String FIELD_SESSION_ID = "sessionId";

    public static final String FIELD_SUBJECT_LABEL = "subjectLabel";

    public static final String FIELD_SESSION_LABEL = "sessionLabel";

    // xnat db meta-data
    public static final String FIELD_INSERT_DATE = "insert_date";

    public static final String FIELD_INSERT_USER = "insert_user";

    public static final String FIELD_DATE = "date";

    public static final String FIELD_XSITYPE = "xsiType";

    public static final String FIELD_SUBJECTASSESSORDATA_ID = "xnat:subjectassessordata/id";

    public static final String FIELD_XNAT_IMAGESCANDATA_ID = "xnat_imagescandata_id";

    public static final String FIELD_TYPE = "type";

    public static final String FIELD_SCAN_QUALITY = "quality";

    public static final String FIELD_NOTE = "note";

    public static final String FIELD_SCAN_SERIES_DESCRIPTION = "series_description";

    public static final String FIELD_SIZE = "Size";

    public static final String FIELD_COLLECTION = "collection";

    public static final String FIELD_FILE_TAGS = "file_tags";

    public static final String FIELD_FILE_FORMAT = "file_format";

    public static final String FIELD_FILE_CONTENT = "file_content";

    public static final String FIELD_CAT_ID = "cat_ID";

    // Subject Meta Data
    public static final String FIELD_SUBJECT_AGE = "age";

    public static final String FIELD_SUBJECT_DOB = "DOB";

    public static final String FIELD_SUBJECT_YOB = "YOB";

    public static final String FIELD_SUBJECT_GENDER = "gender";

    public static final String FIELD_SUBJECT_HANDEDNESS = "handedness";

    // Session Meta Data
    public static final String FIELD_SESSION_AGE = "age";

    // Type and formats

    // === Fields ===

    public static final String project_fields[] =
    {
            FIELD_ID,
            FIELD_SECONDARY_ID,
            FIELD_NAME,
            FIELD_DESCRIPTION,
            FIELD_PI_FIRSTNAME,
            FIELD_PI_LASTNAME,
            FIELD_URI_PATH
    };

    public static final String subject_fields[] =
    {
            FIELD_ID,
            FIELD_LABEL,
            FIELD_PROJECT_ID,
            FIELD_INSERT_DATE,
            FIELD_INSERT_USER,
            FIELD_URI_PATH,
    };

    public static final String subject_fields_metadata[] =
    {
            // meta data:
            FIELD_SUBJECT_AGE,
            FIELD_SUBJECT_YOB,
            FIELD_SUBJECT_DOB,
            FIELD_SUBJECT_GENDER,
            FIELD_SUBJECT_HANDEDNESS
    };

    public static final String session_fields[] =
    {
            FIELD_ID,
            FIELD_LABEL,
            FIELD_PROJECT_ID,
            FIELD_XSITYPE,
            FIELD_SUBJECTASSESSORDATA_ID,
            FIELD_INSERT_DATE,
            FIELD_URI_PATH,
    };

    public static final String session_fields_metadata[] =
    {
            FIELD_SESSION_AGE,
            FIELD_DATE
    };

    // "xnat_imagescandata_id","ID","type","quality","xsiType","note","series_description","URI"
    public static final String scan_fields[] =
    {
            FIELD_ID,
            FIELD_XNAT_IMAGESCANDATA_ID,
            FIELD_TYPE,
            FIELD_XSITYPE,
            FIELD_SCAN_QUALITY,
            FIELD_NOTE,
            FIELD_SCAN_SERIES_DESCRIPTION,
            FIELD_URI_PATH
    };

    // "xnat_reconstructedimagedata_id","ID","type","URI" ("xsiType"?).
    public static final String reconstruction_fields[] =
    {
            FIELD_ID,
            FIELD_TYPE,
            FIELD_XSITYPE,
            FIELD_URI_PATH
    };

    // "Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID"
    public static final String file_fields[] =
    {
            FIELD_NAME,
            FIELD_SIZE,
            FIELD_URI_PATH,
            FIELD_COLLECTION,
            FIELD_FILE_TAGS,
            FIELD_FILE_FORMAT,
            FIELD_FILE_CONTENT,
            FIELD_CAT_ID
    };

    public static String[] getProjectFieldNames()
    {
        return project_fields;
    }

    public static List<String> getSubjectFieldNames(boolean all)
    {
        StringList list = new StringList(subject_fields_metadata);

        if (all)
            list.merge(subject_fields);

        return list;
    }

    public static List<String> getSessionFieldNames(boolean all)
    {
        StringList list = new StringList(session_fields_metadata);

        if (all)
        {
            list.merge(session_fields);
        }

        return list;
    }

    public static String[] getScanFieldNames()
    {
        return scan_fields;
    }

    public static String[] getReconstructionFieldNames()
    {
        return reconstruction_fields;
    }

    public static String[] getFileFieldNames()
    {
        return file_fields;
    }

    public static boolean isSessionField(String fieldName, boolean ignoreCase)
    {
        if (fieldName == null)
            return false;

        for (String name : getSessionFieldNames(true)) // linear search
        {
            if (fieldName.equals(name))
                return true;
        }

        return false;
    }

    public static boolean isSubjectField(String fieldName, boolean ignoreCase)
    {
        if (fieldName == null)
        {
            return false;
        }

        for (String name : subject_fields) // linear search
        {
            if (ignoreCase)
            {
                if (fieldName.equalsIgnoreCase(name))
                    return true;
            }
            else
            {
                if (fieldName.equals(name))
                    return true;
            }
        }

        return false;
    }

}

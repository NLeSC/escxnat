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

public class XnatScan extends XnatObject
{

    public XnatScan()
    {
        super(XnatObjectType.XNAT_SCAN);
    }

    public XnatScan(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_SCAN, fields);
    }

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getScanFieldNames();
    }

    /**
     * Scans only have an ID, not a label.
     */
    public String getID()
    {
        return get(XnatConst.FIELD_ID);
    }

    public String getImageScanDataID()
    {
        return get(XnatConst.FIELD_XNAT_IMAGESCANDATA_ID);
    }

    public String getScanType()
    {
        return get(XnatConst.FIELD_TYPE);
    }

    public String getQuality()
    {
        return get(XnatConst.FIELD_SCAN_QUALITY);
    }

    public String getXsiType()
    {
        return get(XnatConst.FIELD_XSITYPE);
    }

    public String getNote()
    {
        return get(XnatConst.FIELD_NOTE);
    }

    public String getDescription()
    {
        return get(XnatConst.FIELD_SCAN_SERIES_DESCRIPTION);
    }

    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

    public void setDescription(String description)
    {
        this.set(XnatConst.FIELD_SCAN_SERIES_DESCRIPTION, description);
    }

    public void setIDMapping(String projectid, String subjectLabel, String sessionLabel)
    {
        // Project/Subject/Session IDs+labels are not yet stored in Scan objects...
        // setProjectId(projectid);
        // setSubjectLabel(subjectLabel);
        // setSessionLabel(sessionLabel);
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return null;
    }

    public static NewScanInfo createNewScanInfoFrom(XnatScan scan)
    {
        NewScanInfo info = new NewScanInfo(scan.getID());
        info.note = scan.getNote();
        info.quality = scan.getQuality();
        info.series_description = scan.getDescription();
        return info;
    }

}

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

import nl.esciencecenter.ptk.net.URIFactory;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageContentType;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;

public class XnatFile extends XnatObject
{

    public XnatFile()
    {
        super(XnatObjectType.XNAT_FILE);
    }

    public XnatFile(Map<String, String> fields)
    {
        super(XnatObjectType.XNAT_FILE, fields);
    }

    // "Name","Size","URI","collection","file_tags","file_format","file_content","cat_ID"

    @Override
    public String[] getFieldNames()
    {
        return XnatConst.getFileFieldNames();
    }

    @Override
    public List<String> getMetaDataFieldNames()
    {
        return null;
    }

    public String getID()
    {
        return getName();// name is ID?
    }

    public String getName()
    {
        return get(XnatConst.FIELD_NAME);
    }

    public String getFileName()
    {
        return get(XnatConst.FIELD_NAME);
    }

    public String getBasename()
    {
        return URIFactory.basename(getFileName());
    }

    public String getFileSizeString()
    {
        return get(XnatConst.FIELD_SIZE);
    }

    /**
     * Returns file size of remote file. -1 for unkown.
     * 
     * @return
     */
    public long getFileSize()
    {
        String str = this.getFileSizeString();
        if (str == null)
            return -1;

        return Long.parseLong(str);
    }

    public String getCollection()
    {
        return get(XnatConst.FIELD_COLLECTION);
    }

    public void setCollection(String collection)
    {
        this.set(XnatConst.FIELD_COLLECTION, collection);
    }

    public String getFileTags()
    {
        return get(XnatConst.FIELD_FILE_TAGS);
    }

    public String getFormatTypeString()
    {
        return get(XnatConst.FIELD_FILE_FORMAT);
    }

    public String getContentTypeString()
    {
        return get(XnatConst.FIELD_FILE_CONTENT);
    }

    public ImageFormatType getImageFormatType()
    {
        return XnatTypes.parseImageFormatType(getFormatTypeString());
    }

    // public ImageContentType getImageContentType()
    // {
    // return XnatTypes.parseImageContentType(getContentTypeString());
    // }

    public String getCatID()
    {
        return get(XnatConst.FIELD_CAT_ID);
    }

    public String getURIPath()
    {
        return get(XnatConst.FIELD_URI_PATH);
    }

    public ImageFileInfo getImageFileInfo()
    {
        ImageFileInfo info = new ImageFileInfo(getFormatTypeString(), getContentTypeString());
        info.setDestinationFilename(getFileName());
        info.setFileSize(this.getFileSize());
        return info;
    }

}

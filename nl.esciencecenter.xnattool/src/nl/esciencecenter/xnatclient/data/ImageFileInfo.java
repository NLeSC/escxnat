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

import nl.esciencecenter.xnatclient.data.XnatTypes.ImageContentType;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;

/**
 * Info used when uploading a file.
 */
public class ImageFileInfo
{
    public String formatType;

    public String imageContentType;

    public String destinationFileBasename;

    private long fileSize;

    public ImageFileInfo()
    {
    }

    public ImageFileInfo duplicate()
    {
        ImageFileInfo newInfo = new ImageFileInfo();
        newInfo.copyFrom(this);
        return newInfo;
    }

    protected void copyFrom(ImageFileInfo other)
    {
        this.formatType = other.formatType;
        this.imageContentType = other.imageContentType;
        this.destinationFileBasename = other.destinationFileBasename;
        this.fileSize = other.fileSize;
    }

    public ImageFileInfo(ImageFormatType format, ImageContentType contentType)
    {
        this.formatType = format.toString();
        this.imageContentType = contentType.toString();
    }

    public ImageFileInfo(String formatType, String contentType)
    {
        this.formatType = formatType;
        this.imageContentType = contentType;
    }

    public String getContentType()
    {
        return this.imageContentType;
    }

    public String getImageFormatType()
    {
        return this.formatType;
    }

    /**
     * Basename of Destination file
     * 
     * @return
     */
    public String getDestinationFilename()
    {
        return destinationFileBasename;
    }

    /**
     * Basename of destination file
     * 
     * @param newName
     */
    public void setDestinationFilename(String newName)
    {
        this.destinationFileBasename = newName;
    }

    public void setFileSize(long length)
    {
        this.fileSize = length;
    }

    public long getFileSize()
    {
        return this.fileSize;
    }

    // generated methods.

    @Override
    public String toString()
    {
        return "ImageFileInfo [formatType=" + formatType + ", contentType=" + imageContentType
                + ", destinationFileBasename=" + destinationFileBasename + ", fileSize=" + fileSize + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((destinationFileBasename == null) ? 0 : destinationFileBasename.hashCode());
        result = prime * result + (int) (fileSize ^ (fileSize >>> 32));
        result = prime * result + ((formatType == null) ? 0 : formatType.hashCode());
        result = prime * result + ((imageContentType == null) ? 0 : imageContentType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageFileInfo other = (ImageFileInfo) obj;
        if (destinationFileBasename == null)
        {
            if (other.destinationFileBasename != null)
                return false;
        }
        else if (!destinationFileBasename.equals(other.destinationFileBasename))
            return false;
        if (fileSize != other.fileSize)
            return false;
        if (formatType != other.formatType)
            return false;
        if (imageContentType == null)
        {
            if (other.imageContentType != null)
                return false;
        }
        else if (!imageContentType.equals(other.imageContentType))
            return false;
        return true;
    }

}

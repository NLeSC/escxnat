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

package nl.esciencecenter.xnattool;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.net.URIFactory;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.web.WebConfig;
import nl.esciencecenter.ptk.web.WebConfig.AuthenticationType;
import nl.esciencecenter.ptk.xml.XmlUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonPropertyOrder({
        "xnatUri", "xnatUser", "defaultOwnerId", "dataSetsConfigDir", "imageCacheDir"
})
public class XnatToolConfig
{

    public static XnatToolConfig parseXML(String xml) throws JsonParseException, JsonMappingException, IOException
    {
        ObjectMapper xmlMapper = new XmlMapper();
        XnatToolConfig value = xmlMapper.readValue(xml, XnatToolConfig.class);
        // check ?
        return value;
    }

    public static XnatToolConfig loadConfig(String path) throws Exception
    {
        String txt = FSUtil.getDefault().readText(path);
        return parseXML(txt);
    }

    public static void saveConfig(XnatToolConfig config, String path) throws Exception
    {
        // update version befor saving:
        config.setConfigVersion(XnatToolMain.getVersion());
        String xml = config.toXML();
        xml = XmlUtil.prettyFormat(xml, 3);
        FSUtil.getDefault().writeText(path, xml);
    }

    // ========
    // Instance
    // ========

    /** Configuration version. Update when saving a Configuration */
    protected String version = null;

    /**
     * URI of XNAT web service.
     */
    protected URI xnatUri = null;

    /**
     * Current User name.
     */
    protected String xnatUser = null;

    // public char xnatPasswd[]=null;// not stored.

    /**
     * Contains (meta) data and uploaded profiles of the DataSets.
     */
    protected URI dataSetsConfigDir;

    /**
     * Cache dir for processed dicom files.
     */
    protected URI imageCacheDir;

    /**
     * Default Source Id for the DataSetConfig objects.
     */
    protected String defaultSourceId = null;

    /**
     * Whether to clear the image cache dir when exiting the application.
     */
    protected boolean clearImageCacheDirAfterExit = false;

    protected boolean firstRun = true;

    protected boolean keepProcessedDicomFile = false;

    protected boolean autoCreateMappingsFile = false;

    protected boolean autoExtractMetaData = false;

    protected boolean getAutoResumeAndVerifyUpload = true;

    protected Secret xnatPassword = null;

    public XnatToolConfig()
    {
    }

    public XnatToolConfig(URI uri)
    {
        this.updateURI(uri);
    }

    @JacksonXmlProperty(localName = "xnatUser")
    public String getXnatUser()
    {
        return xnatUser;
    }

    @JsonIgnore
    public URI getXnatURI()
    {
        return xnatUri;
    }

    @JacksonXmlProperty(localName = "xnatUri")
    public String getXnatURIString()
    {
        return xnatUri.toString();
    }

    @JsonIgnore
    public void setXnatURI(URI uri)
    {
        updateURI(uri);
    }

    @JacksonXmlProperty(localName = "xnatUri")
    public void setXnatURI(String vriStr) throws Exception
    {
        updateURI(new URI(vriStr));
    }

    @JsonIgnore
    public void updateUser(String user) throws URISyntaxException
    {
        xnatUser = user;
        // Copy username into URI if already defined.
        // This is only intended as user feedback.
        // Field xnatUser is actual login name.

        if (xnatUri != null)
        {
            xnatUri = new URIFactory(xnatUri).setUserInfo(user).toURI();
        }
    }

    @JsonIgnore
    public void updateURI(URI uri)
    {
        xnatUri = uri;
        if (uri == null)
        {
            xnatUser = null;
        }
        else
        {
            String userInf = uri.getUserInfo();
            // copy username from uri to xnatUser field.
            if (StringUtil.isWhiteSpace(userInf) == false)
            {
                xnatUser = userInf;
            }
        }
    }

    @JacksonXmlProperty(localName = "configVersion")
    public void setConfigVersion(String configVersion)
    {
        this.version = configVersion;
    }

    @JacksonXmlProperty(localName = "configVersion")
    public String getConfigVersion()
    {
        return version;
    }

    @JsonIgnore
    public URI getDataSetsConfigDir()
    {
        return dataSetsConfigDir;
    }

    @JacksonXmlProperty(localName = "dataSetsConfigDir")
    public String getDataSetsConfigDirString()
    {
        return dataSetsConfigDir.toString();
    }

    @JsonIgnore
    public void setDataSetsConfigDir(URI uri)
    {
        dataSetsConfigDir = uri;
    }

    @JacksonXmlProperty(localName = "dataSetsConfigDir")
    public void setDataSetConfigDir(String uri) throws URISyntaxException
    {
        dataSetsConfigDir = new URI(uri);
    }

    @JsonIgnore
    public URI getImageCacheDir()
    {
        return this.imageCacheDir;
    }

    @JacksonXmlProperty(localName = "imageCacheDir")
    public String getImageCacheDirString()
    {
        return this.imageCacheDir.toString();
    }

    @JsonIgnore
    public void setImageCacheDir(URI uri)
    {
        imageCacheDir = uri;
    }

    @JacksonXmlProperty(localName = "imageCacheDir")
    public void setImageCacheDir(String uri) throws URISyntaxException
    {
        imageCacheDir = new URI(uri);
    }

    @JacksonXmlProperty(localName = "defaultSourceId")
    public void setDefaultSourceId(String id)
    {
        defaultSourceId = id;
    }

    @JacksonXmlProperty(localName = "defaultSourceId")
    public String getDefaultSourceId()
    {
        return defaultSourceId;
    }

    public String toXML() throws JsonProcessingException
    {
        XmlMapper xmlMapper = new XmlMapper();
        String xml = xmlMapper.writeValueAsString(this);
        return xml;
    }

    @JacksonXmlProperty(localName = "clearImageCacheDirAfterExit")
    public boolean getClearImageCacheDirAfterExit()
    {
        return clearImageCacheDirAfterExit;
    }

    @JacksonXmlProperty(localName = "clearImageCacheDirAfterExit")
    public void setClearImageCacheDirAfterExit(boolean value)
    {
        clearImageCacheDirAfterExit = value;
    }

    @JacksonXmlProperty(localName = "firstRun")
    public boolean getFirstRun()
    {
        return firstRun;
    }

    @JacksonXmlProperty(localName = "firstRun")
    public void setFirstRun(boolean value)
    {
        firstRun = value;
    }

    @JacksonXmlProperty(localName = "keepProcessedDicomFile")
    public boolean getKeepProcessedDicomFile()
    {
        return keepProcessedDicomFile;
    }

    @JacksonXmlProperty(localName = "keepProcessedDicomFile")
    public void setKeepProcessedDicomFile(boolean val)
    {
        keepProcessedDicomFile = val;
    }

    @JacksonXmlProperty(localName = "autoCreateMappingsFile")
    public boolean getAutoCreateMappingsFile()
    {
        return autoCreateMappingsFile;
    }

    @JacksonXmlProperty(localName = "autoCreateMappingsFile")
    public void setAutoCreateMappingsFile(boolean val)
    {
        autoCreateMappingsFile = val;
    }

    @JacksonXmlProperty(localName = "autoExtractMetaData")
    public void setAutoExtractMetaData(boolean val)
    {
        autoExtractMetaData = val;
    }

    @JacksonXmlProperty(localName = "autoExtractMetaData")
    public boolean getAutoExtractMetaData()
    {
        return autoExtractMetaData;
    }

    @JacksonXmlProperty(localName = "autoResumeAndVerifyUpload")
    public boolean getAutoResumeAndVerifyUpload()
    {
        return getAutoResumeAndVerifyUpload;
    }

    @JacksonXmlProperty(localName = "autoResumeAndVerifyUpload")
    public void setAutoResumeAndVerifyUpload(boolean value)
    {
        getAutoResumeAndVerifyUpload = value;
    }

    @JsonIgnore
    public WebConfig getWebConfig()
    {
        WebConfig conf = new WebConfig(this.getXnatURI(), AuthenticationType.BASIC, false);
        conf.setCredentials(getXnatUser(), null);

        return conf;
    }
}

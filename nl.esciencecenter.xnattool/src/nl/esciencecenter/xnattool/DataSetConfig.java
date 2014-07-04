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
import java.util.Properties;

import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.ScanKeyType;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.SessionKeyType;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.SubjectKeyType;
import nl.esciencecenter.ptk.io.FSUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * DataSet Configuration. Jackson Compatible object.
 */
public class DataSetConfig
{
    public static DataSetConfig createDefault()
    {
        DicomProcessingProfile opts = DicomProcessingProfile.createDefault();
        DataSetConfig conf = new DataSetConfig(opts);
        conf.initDefaults();
        return conf;
    }

    public static DataSetConfig loadFrom(String path) throws Exception
    {
        String xml = FSUtil.getDefault().readText(path);
        DataSetConfig opts = DataSetConfig.parseXML(xml);
        opts.postReadUpdate();
        return opts;
    }

    public static DataSetConfig parseXML(String xml) throws JsonParseException, JsonMappingException,
            IOException
    {
        ObjectMapper xmlMapper = new XmlMapper();
        DataSetConfig value = xmlMapper.readValue(xml, DataSetConfig.class);
        value.postReadUpdate();
        return value;
    }

    // ========================================================================
    //
    // ========================================================================

    // @JacksonXmlProperty(isAttribute = true, localName = "sourceId")
    protected String sourceId = null;

    // @JacksonXmlProperty(isAttribute = true, localName = "dataSetName")
    protected String dataSetName = null;

    protected String xnatProjectID = null;

    protected java.net.URI imageSourceDir = null;

    protected DicomProcessingProfile dicomProcessingProfile = null;

    protected ImageTypes.DataSetType dataSetType = null;

    protected ImageTypes.ScanSubType scanSubType = ImageTypes.ScanSubType.RAW_SCAN;

    /**
     * Whether to use PatientID or PatientName as SubjectKey.
     */
    protected SubjectKeyType subjectKeyType = null;

    /**
     * Which field to use as SessionKey and how to use it.
     */
    protected SessionKeyType sessionKeyType = null;

    /**
     * This field contains sourceId encrypted with encryptionKey to check whether ID+EncryptionKey combination match The
     * actual key isn't stored. This field is stored and is used has to authenticate the sourceID by providing the
     * sourceId together with the passphrase.
     */
    protected byte encryptedSourceId[] = null;

    /** This is the encryption key encrypted with the actual used password */
    protected byte encryptedKey[] = null;

    protected ScanKeyType scanKeyType;

    protected Properties guiProperties = new Properties();

    protected DataSetConfig()
    {
    }

    protected DataSetConfig(DicomProcessingProfile profile)
    {
        this.dicomProcessingProfile = profile;
        initDefaults();
    }

    private void initDefaults()
    {
        sourceId = "anonymous";
        dataSetName = "default";
        subjectKeyType = SubjectKeyType.CRYPTHASH_PATIENT_ID;
        sessionKeyType = SessionKeyType.CRYPTHASH_STUDY_UID;
        scanKeyType = ScanKeyType.CRYPTHASH_SCAN_UID;
        dataSetType = ImageTypes.DataSetType.DICOM_SCANSET;
        scanSubType = ImageTypes.ScanSubType.RAW_SCAN;
        imageSourceDir = null;
    }

    /**
     * Update settings after reading from file. This method auto-updates missing settings or fixes bugs in previous
     * versions.
     */
    @JsonIgnore
    protected void postReadUpdate()
    {
        DataSetConfig def = createDefault();

        if (this.dataSetType == null)
            this.dataSetType = def.dataSetType;

        if (this.subjectKeyType == null)
            subjectKeyType = def.subjectKeyType;

        if (this.sessionKeyType == null)
            sessionKeyType = def.sessionKeyType;

        if (this.scanKeyType == null)
            scanKeyType = def.scanKeyType;

        if (this.scanSubType == null)
            scanSubType = def.scanSubType;
    }

    // ===============
    // Getters/Setters
    // ===============

    @JacksonXmlProperty(localName = "dicomProcessingProfile")
    public void setDicomProcessingProfile(DicomProcessingProfile profile)
    {
        dicomProcessingProfile = profile;
    }

    @JacksonXmlProperty(localName = "dicomProcessingProfile")
    public DicomProcessingProfile getDicomProcessingProfile()
    {
        return dicomProcessingProfile;
    }

    @JacksonXmlProperty(localName = "dataSetName")
    public String getDataSetName()
    {
        return this.dataSetName;
    }

    @JacksonXmlProperty(localName = "dataSetName")
    public void setDataSetName(String newName)
    {
        this.dataSetName = newName;
    }

    @JacksonXmlProperty(localName = "dataSetType")
    public ImageTypes.DataSetType getDataSetType()
    {
        return this.dataSetType;
    }

    @JacksonXmlProperty(localName = "dataSetType")
    public void setDataSetType(ImageTypes.DataSetType newType)
    {
        this.dataSetType = newType;
    }

    @JacksonXmlProperty(localName = "sourceId")
    public String getSourceId()
    {
        return this.sourceId;
    }

    @JacksonXmlProperty(localName = "sourceId")
    public void setSourceId(String value)
    {
        this.sourceId = value;
    }

    @JacksonXmlProperty(localName = "imageSourceDir")
    public void setImageSourceDir(java.net.URI path)
    {
        this.imageSourceDir = path;
    }

    @JacksonXmlProperty(localName = "imageSourceDir")
    public java.net.URI getImageSourceDir()
    {
        return imageSourceDir;
    }

    /**
     * This returns the password <em>encrypted</em> version of the encryption key
     */
    @JacksonXmlProperty(localName = "encryptedKey")
    public byte[] getEncryptedKey()
    {
        return encryptedKey;
    }

    /**
     * This set the password <em>encrypted</em> version of the encryption key.
     */
    @JacksonXmlProperty(localName = "encryptedKey")
    public void setEncryptedKey(byte value[])
    {
        this.encryptedKey = value;
    }

    /**
     * This return the password <em>encrypted</em> version of the SourceId. This field can be used to check whether the
     * suplied password matches this profile.
     */
    @JacksonXmlProperty(localName = "encryptedSourceId")
    public byte[] getEncryptedSourceID()
    {
        return encryptedSourceId;
    }

    /**
     * This sets the password <em>encrypted</em> version of the SourceId. This field can be used to check whether the
     * supplied password matches this profile.
     */
    @JacksonXmlProperty(localName = "encryptedSourceId")
    public void setEncryptedSourceID(byte bytes[])
    {
        this.encryptedSourceId = bytes;
    }

    @JacksonXmlProperty(localName = "subjectKeyType")
    public void setSubjectKeyType(SubjectKeyType newType)
    {
        this.subjectKeyType = newType;
    }

    @JacksonXmlProperty(localName = "subjectKeyType")
    public SubjectKeyType getSubjectKeyType()
    {
        return subjectKeyType;
    }

    @JacksonXmlProperty(localName = "sessionKeyType")
    public void setSessionKeyType(SessionKeyType keyType)
    {
        this.sessionKeyType = keyType;
    }

    @JacksonXmlProperty(localName = "sessionKeyType")
    public SessionKeyType getSessionKeyType()
    {
        return sessionKeyType;
    }

    @JacksonXmlProperty(localName = "scanKeyType")
    public void setScanKeyType(ScanKeyType keyType)
    {
        this.scanKeyType = keyType;
    }

    @JacksonXmlProperty(localName = "scanKeyType")
    public ScanKeyType getScanKeyType()
    {
        return this.scanKeyType;
    }

    @JsonIgnore
    public String toXML() throws JsonProcessingException
    {
        XmlMapper xmlMapper = new XmlMapper();
        String xml = xmlMapper.writeValueAsString(this);
        return xml;
    }

    @JsonIgnore
    public void setEncryptedCredentials(byte[] encryptedSourceId, byte[] encryptedKey)
    {
        this.encryptedSourceId = encryptedSourceId;
        this.encryptedKey = encryptedKey;
    }

    @JsonIgnore
    public void setCredentials(String newSourceId, byte[] encryptionKey)
    {
        sourceId = newSourceId;
        // this.dicomProcessingProfile.setSaltBytes(newSourceId.getBytes("UTF-8"));
        dicomProcessingProfile.setEncryptionKey(encryptionKey);
    }

    @JsonIgnore
    // import or else the decrypted key will show up in the XML.
    public byte[] getEncryptionKey()
    {
        return this.dicomProcessingProfile.getEncryptionKey();
    }

    @JsonIgnore
    public void setEncryptionKey(byte[] key)
    {
        dicomProcessingProfile.setEncryptionKey(key);
        return;
    }

    @JsonIgnore
    // import or else the decrypted key will show up in the XML.
    public boolean hasValidEncryptionKey()
    {
        return (getEncryptedKey() != null);
    }

    /**
     * Set GUI specific Properties. This way GUI specific settings per DataSet can be restored when the GUI starts.
     */
    @JacksonXmlProperty(localName = "guiProperties")
    public void setGuiProperties(Properties properties)
    {
        guiProperties = properties;
    }

    /**
     * Get GUI specific Properties. This way GUI specific settings per DataSet can be restored when the GUI starts.
     */
    @JacksonXmlProperty(localName = "guiProperties")
    public Properties getGuiProperties()
    {
        return guiProperties;
    }

    @JsonIgnore
    public void setGuiProperty(String name, String value)
    {
        // When rereading XML files with missing guiProperties, the field is null.
        if (guiProperties == null)
            guiProperties = new Properties();
        guiProperties.setProperty(name, value);
    }

    @JsonIgnore
    public String getGuiProperty(String name)
    {
        return guiProperties.getProperty(name);
    }

    @JacksonXmlProperty(localName = "scanSubType")
    public void setScanSubType(ImageTypes.ScanSubType subType)
    {
        this.scanSubType = subType;
    }

    @JacksonXmlProperty(localName = "scanSubType")
    public ImageTypes.ScanSubType getScanSubType()
    {
        return scanSubType;
    }

}

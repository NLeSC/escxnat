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

package nl.esciencecenter.medim.dicom;

import java.io.IOException;

import nl.esciencecenter.ptk.crypt.CryptScheme;
import nl.esciencecenter.ptk.io.FSUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Dicom Processing options. Also contains options for the Xnat subject/session ID mappings.<br>
 * This object is persistent and can be serialized to Json or XML using Jackson FasterXML. It is not Serializable by
 * itself.
 */
@JsonPropertyOrder(
{
        "profileName",
        "maxHashUIDByteLength",
        "cryptScheme",
        "uidPrefix",
        "defaultUIDOption",
        "hashSalt",
        "prefixHashSalt",
        "processDicom"
})
public class DicomProcessingProfile
{
    /**
     * Whether to use PatientID or PatientName as main Subject Key. Using Patient Id is the default.
     */
    public static enum SubjectKeyType
    {
        /** Crypto Hash the (Dicom) Patient ID Field. */
        CRYPTHASH_PATIENT_ID(true),
        /** Crypto-Hash the (Dicom) Patient Name Field. */
        CRYPTHASH_PATIENT_NAME(true),
        /** Use plain Patient ID field. */
        PLAIN_PATIENT_ID(false),
        /** Use plain Patient Name field. */
        PLAIN_PATIENT_NAME(false);

        private boolean doCryptHash;

        private SubjectKeyType(boolean doCryptHash)
        {
            this.doCryptHash = doCryptHash;
        }

        public boolean getDoCryptHash()
        {
            return doCryptHash;
        }

        public static String[] stringValues()
        {
            SubjectKeyType[] values = SubjectKeyType.values();
            String stringValues[] = new String[values.length];

            for (int i = 0; i < values.length; i++)
            {
                stringValues[i] = values[i].toString();
            }
            return stringValues;
        }
    };

    /**
     * What field to use as Session Key(Xnat Session Label) and how to create the Session Label.
     */
    public static enum SessionKeyType
    {
        /** CryptoHash the (Dicom) Study UID. This is the Default. */
        CRYPTHASH_STUDY_UID(true),

        /** Use plain (Dicom) Study UID as Session Key */
        PLAIN_STUDY_UID(false),

        /** Use year of Study Date. */
        STUDY_DATE_YEAR(false),

        /** Use Year+Month of Study Date. */
        STUDY_DATE_YEAR_MONTH(false),

        /** Use patient age in whole years. */
        PATIENT_AGE_YEAR(false),

        /** Use Patient age in &lt;YEARS&gt;&lt;MONTHS&gt; */
        PATIENT_AGE_YEAR_MONTHS(false);

        private boolean doCryptHash;

        private SessionKeyType(boolean doCryptHash)
        {
            this.doCryptHash = doCryptHash;
        }

        /**
         * @return true - if the selected field needs to be encrypted. Only applicable to Study UID.
         */
        public boolean getDoCryptHash()
        {
            return doCryptHash;
        }

        public static String[] toStringArray()
        {
            SubjectKeyType[] values = SubjectKeyType.values();
            String stringValues[] = new String[values.length];

            for (int i = 0; i < values.length; i++)
            {
                stringValues[i] = values[i].toString();
            }
            return stringValues;
        }
    };

    public static enum ScanKeyType
    {
        /** CryptoHash the Series UID. This is the Default */
        CRYPTHASH_SCAN_UID(true),
        /** Keep Series UID as is. */
        PLAIN_SCAN_UID(false);

        private boolean doCryptHash;

        /**
         * @return true - if the selected field needs to be encrypted. Only applicable to Series(Scan) UID.
         */
        private ScanKeyType(boolean doCryptHash)
        {
            this.doCryptHash = doCryptHash;
        }

        public boolean getDoCryptHash()
        {
            return doCryptHash;
        }
    }

    /**
     * Option to use for UID type Dicom tags.
     */
    public static enum UIDOption
    {
        KEEP_UID,
        HASH_UID,
        ENCRYPT_UID,
        ENCRYPT_HASH_UID,
        RANDOMIZE_UID
    };

    // Factory method.
    public static DicomProcessingProfile createDefault()
    {
        DicomProcessingProfile prof = new DicomProcessingProfile();
        prof.initDefault();
        return prof;
    }

    public static DicomProcessingProfile parseXML(String xml) throws JsonParseException, JsonMappingException,
            IOException
    {
        ObjectMapper xmlMapper = new XmlMapper();
        DicomProcessingProfile value = xmlMapper.readValue(xml, DicomProcessingProfile.class);
        // check ?
        return value;
    }

    public static DicomProcessingProfile loadFrom(String path) throws Exception
    {
        String xml = FSUtil.getDefault().readText(path);
        DicomProcessingProfile opts = DicomProcessingProfile.parseXML(xml);
        return opts;
    }

    // ========================================================================
    // Instance
    // ========================================================================

    @JacksonXmlProperty(isAttribute = true, localName = "profileName")
    protected String profileName;

    protected CryptScheme cryptScheme;

    /**
     * Prefix or UIDs, for example '99.'
     */
    protected String uidPrefix;

    /**
     * Default processing options for UID tags
     */
    protected UIDOption defaultUidOption;

    /**
     * Maximum length of UID hashes. Currently limited to 24 bytes (or 192 bits) for UID hashes to fit in 64 character
     * sized dotted decimal Strings. 24 bytes => (x 2.56) => 62 decimals.
     */
    protected int maxHashUIDByteLen = -1;

    /** Whether to process the dicom fields or not */
    protected boolean processDicom = true;

    /**
     * Whether to apply the salt bytes befor the hashing or after.
     */
    protected boolean prefixHashSalt = false;

    /**
     * Optional Salt Bytes to used in Hashing and Encryption.
     */
    protected byte hashSalt[] = null;

    // ===
    //
    // ===

    /**
     * Actual encryption key to use. Do not store inside XML, must be supplied.
     */
    @JsonIgnore
    private byte _encryptionKey[] = null;

    protected DicomProcessingProfile()
    {
        ; // empty by default.
    }

    protected void initDefault()
    {
        _encryptionKey = null;
        cryptScheme = CryptScheme.DESEDE_ECB_PKCS5;
        uidPrefix = "99.";
        maxHashUIDByteLen = 24;
        defaultUidOption = UIDOption.ENCRYPT_HASH_UID;
        prefixHashSalt = false;
        hashSalt = null;
        processDicom = true;
    }

    public DicomProcessingProfile(
            CryptScheme cryptScheme,
            int maxHashUid,
            String _uidPrefix,
            SubjectKeyType _subjectIdType,
            byte hashSaltBytes[],
            boolean prefixHash)
    {
        this.cryptScheme = cryptScheme;
        this._encryptionKey = null;
        this.hashSalt = hashSaltBytes;
        this.maxHashUIDByteLen = maxHashUid;
        this.uidPrefix = _uidPrefix;
        this.prefixHashSalt = prefixHash;
    }

    /**
     * Specify sourceId which is used as ID and as salt for the hashing and the actual encryptionKey or fields which
     * need to be encrypted.<br>
     * JSON note: Ignore setting/getting Password/EncryptionKeys!
     */
    @JsonIgnore
    public void setEncryptionKey(byte encryptionKey[])
    {
        this._encryptionKey = encryptionKey;
    }

    /**
     * Return actual raw encryption key.<br>
     * JSON note: Ignore setting/getting Password/EncryptionKeys!
     */
    @JsonIgnore
    public byte[] getEncryptionKey()
    {
        return _encryptionKey;
    }

    // ===========================
    // Json "POJO" Attributes: Wrap POJO fields with get/set methods.
    // ===========================

    @JacksonXmlProperty(localName = "profileName")
    public void setProfileName(String name)
    {
        this.profileName = name;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "profileName")
    public String getProfileName()
    {
        return profileName;
    }

    @JacksonXmlProperty(localName = "cryptScheme")
    public CryptScheme getCryptScheme()
    {
        return this.cryptScheme;
    }

    @JacksonXmlProperty(localName = "cryptScheme")
    public void setCryptScheme(CryptScheme value)
    {
        this.cryptScheme = value;
    }

    @JacksonXmlProperty(localName = "uidPrefix")
    public String getUIDPrefix()
    {
        return this.uidPrefix;
    }

    @JacksonXmlProperty(localName = "uidPrefix")
    public void setUIDPrefix(String value)
    {
        this.uidPrefix = value;
    }

    @JacksonXmlProperty(localName = "maxHashedUIDByteLength")
    /** Maximum length of Hashed UID in bytes */
    public int getMaxHashedUIDByteLength()
    {
        return this.maxHashUIDByteLen;
    }

    @JacksonXmlProperty(localName = "maxHashedUIDByteLength")
    /** Maximum length of Hashed UID in bytes */
    public void setMaxHashedUIDByteLength(int len)
    {
        this.maxHashUIDByteLen = len;
    }

    @JacksonXmlProperty(localName = "defaultUIDOption")
    public UIDOption getDefaultUIDOption()
    {
        return this.defaultUidOption;
    }

    @JacksonXmlProperty(localName = "defaultUIDOption")
    public void setDefaultUIDOption(UIDOption uidOpt)
    {
        this.defaultUidOption = uidOpt;
    }

    @JacksonXmlProperty(localName = "processDicom")
    public boolean getDoProcessDicom()
    {
        return this.processDicom;
    }

    @JacksonXmlProperty(localName = "processDicom")
    public void setDoProcessDicom(boolean value)
    {
        this.processDicom = value;
    }

    @JacksonXmlProperty(localName = "prefixHashSalt")
    public boolean getPrefixSalt()
    {
        return this.prefixHashSalt;
    }

    @JacksonXmlProperty(localName = "prefixHashSalt")
    public void setPrefixHash(boolean value)
    {
        this.prefixHashSalt = value;
    }

    @JacksonXmlProperty(localName = "hashSalt")
    public byte[] getHashSalt()
    {
        return this.hashSalt;
    }

    /**
     * Specify sourceId or "Hash Salt" in bytes which is used as ID and as salt for the hashing and as salt for the
     * actual encryptionKey.<br>
     */
    @JacksonXmlProperty(localName = "hashSalt")
    public void setHashSalt(byte bytes[])
    {
        this.hashSalt = bytes;
    }

    public String toXML() throws JsonProcessingException
    {
        XmlMapper xmlMapper = new XmlMapper();
        String xml = xmlMapper.writeValueAsString(this);
        return xml;
    }

}

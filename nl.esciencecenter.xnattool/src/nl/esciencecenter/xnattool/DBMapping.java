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

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import nl.esciencecenter.ptk.crypt.StringCrypter;
import nl.esciencecenter.ptk.crypt.StringCrypter.EncryptionException;
import nl.esciencecenter.ptk.crypt.StringHasher;
import nl.esciencecenter.ptk.csv.CSVData;
import nl.esciencecenter.ptk.data.HashMapList;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.util.StringUtil;

/**
 * Mappings between ScanSets and remote Xnat DB.
 * 
 * Current implementation encrypts the ID and then hashed the encrypted result.
 * 
 */
public class DBMapping
{
    public static final String CSV_OWNERID = "dataset.ownerid";

    public static final String CSV_DATASETNAME = "dataset.name";

    public static final String CSV_ENCRYPTED_OWNERID = "dataset.encryptedOwnerID";

    public static final String CSV_SUBJECTKEYTYPE = "subject.keyType";

    public static final String CSV_SUBJECTID = "subject.id";

    public static final String CSV_ENCRYPTED_SUBJECTKEY = "subject.encryptedKey";

    public static final String CSV_SUBJECTLABEL = "subject.label";

    public static final String CSV_SESSIONID = "session.id";

    public static final String CSV_SESSIONLABEL = "session.label";

    public static final String CSV_SCANID = "scan.id";

    public static final String CSV_SCANLABEL = "scan.label";

    /**
     * ID Mapping Element.
     */
    public static class IDMapping
    {
        /** Encrypted ID */
        protected byte cryptedID[];

        /** SHA-256 hash of encrypted ID */
        protected byte hashedID[];

        /**
         * Resulting label string which contains hexadecimal representation of the hash. Not prefixed by "0X" but by,
         * for example, "sub_","scn_", etc.
         */
        protected String labelString;

        public IDMapping(byte[] cryptedIDBytes, byte[] hashedIDBytes, String labelString)
        {
            this.cryptedID = cryptedIDBytes;
            this.hashedID = hashedIDBytes;
            this.labelString = labelString;
        }
    }

    public class IDMapper extends HashMapList<String, IDMapping>
    {
        private static final long serialVersionUID = -7903650726916939179L;

        public IDMapper()
        {
        }

        public IDMapping get(String key)
        {
            return super.get(key);
        }

        public byte[] getCryptedID(String key)
        {
            IDMapping map = this.get(key);
            if (map == null)
                return null;
            return map.cryptedID;
        }

        public byte[] getHashedID(String key)
        {
            IDMapping map = this.get(key);
            if (map == null)
                return null;
            return map.hashedID;
        }

        /**
         * Actual label which contains hashed ID
         */
        public String getHashedLabelString(String key)
        {
            IDMapping map = this.get(key);
            if (map == null)
                return null;

            return map.labelString;
        }

        public IDMapping getCreateLabel(String id, boolean doCryptHash, boolean autoCreate, String labelPrefix) throws EncryptionException
        {
            IDMapping newid = this.get(id);

            if (newid == null)
            {
                if (autoCreate == false)
                    return null;

                if (doCryptHash)
                {
                    newid = cryptHash(labelPrefix, id);
                }
                else
                {
                    byte[] cryptBytes = null;
                    byte[] hashBytes = null;
                    // use plain id.
                    newid = new IDMapping(cryptBytes, hashBytes, labelPrefix + id);
                }
                put(id, newid);
            }

            return newid;
        }

        protected synchronized IDMapping cryptHash(String labelPrefix, String id) throws EncryptionException
        {
            // first encrypt
            byte cryptBytes[] = encryptID(id);
            // hash result
            byte hashBytes[] = hashBytes(cryptBytes, getHashSaltBytes(), cryptHashSettings.maxHashLength);
            String hashStr = labelPrefix + StringUtil.toHexString(hashBytes, true);

            IDMapping mapping = new IDMapping(cryptBytes, hashBytes, hashStr);
            return mapping;
        }

        protected byte[] getHashSaltBytes()
        {
            String id = DBMapping.this.cryptHashSettings.getSourceID();
            // use char encofing of hasher:
            return id.getBytes(DBMapping.this.hasher.getEncoding());
        }

        public String[] getKeyArray()
        {
            return super.getKeyArray(new String[0]);
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            return toString("", sb, "\n").toString();
        }

        public StringBuilder toString(String prefix, StringBuilder sb, String postfix)
        {
            String keys[] = this.getKeyArray();

            for (String key : keys)
            {
                IDMapping mapping = this.get(key);
                String cryptStr = StringUtil.toHexString(mapping.cryptedID, true);
                String hashStr = mapping.labelString;
                sb.append(prefix + '"' + key + "\",\"" + cryptStr + "\",\"" + hashStr + '"' + postfix);
            }

            return sb;
        }

        public void clear()
        {
            super.clear();
            // this.instanceCount=offsetStart;
        }
    }

    /**
     * Groups a set of IDMappers.
     * 
     */
    public class GroupMapper extends HashMapList<String, IDMapper>
    {
        private static final long serialVersionUID = -1313650726916939179L;

        public GroupMapper()
        {
        }

        public String[] getKeyArray()
        {
            return super.getKeyArray(new String[0]);
        }

        /**
         * Return flattend array of all subgroup keys. These keys might contains doubles if the subgroup keys are not
         * unique between subgroups.
         * 
         * @return
         */
        public List<String> getAllSubGroupKeys()
        {
            String groupKeys[] = this.getKeyArray();

            StringList allKeys = new StringList();

            for (String key : groupKeys)
            {
                String[] subKeys = this.getKeyArray(key);
                allKeys.add(subKeys);
            }

            return allKeys;
        }

        public String[] getKeyArray(String subGroupKey)
        {
            IDMapper mapper = get(subGroupKey);
            if (mapper == null)
                return null;
            return mapper.getKeyArray();
        }

        public byte[] getCryptedID(String groupKey, String subKey)
        {
            IDMapper mapper = this.get(groupKey);
            if (mapper == null)
                return null;
            return mapper.getCryptedID(subKey);
        }

        public byte[] getHashedID(String groupKey, String subKey)
        {
            IDMapper mapper = this.get(groupKey);
            if (mapper == null)
                return null;

            return mapper.getHashedID(subKey);
        }

        public String getHashedLabelString(String groupKey, String subKey)
        {
            IDMapper mapper = this.get(groupKey);
            if (mapper == null)
                return null;

            return mapper.getHashedLabelString(subKey);
        }

        public String getCreate(String groupKey, String subKey, boolean doCryptHash, String labelPrefix, boolean autoCreate)
                throws EncryptionException
        {
            IDMapper mapper = this.get(groupKey);

            if (mapper == null)
            {
                if (autoCreate == false)
                    return null;

                mapper = new IDMapper();
                this.put(groupKey, mapper);
            }

            IDMapping mapping = mapper.getCreateLabel(subKey, doCryptHash, autoCreate, labelPrefix);

            return mapping.labelString;
        }

        public boolean delete(String groupKey, String subKey)
        {
            IDMapper mapper = this.get(groupKey);

            if (mapper == null)
                return false;

            return (mapper.remove(subKey) != null);
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            return toString("", sb, "\n").toString();
        }

        public StringBuilder toString(String prefix, StringBuilder sb, String postfix)
        {
            String keys[] = this.getKeyArray();

            for (String key : keys)
            {
                IDMapper mapper = this.get(key);
                String subprefix = prefix + '"' + key + "\",";
                mapper.toString(subprefix, sb, postfix);
            }

            return sb;
        }

    }

    public static String[] toArray(Set<String> set)
    {
        String keyArr[] = new String[set.size()];
        keyArr = set.toArray(keyArr);
        return keyArr;
    }

    // ========================================================================
    // XNat DB Mappings
    // ========================================================================

    protected CryptHashSettings cryptHashSettings = null;

    protected IDMapper subjectMappings = null;

    protected GroupMapper sessionMappings = null;

    protected GroupMapper scanMappings = null;

    private StringHasher hasher = null;

    private StringCrypter crypter;

    private String subjectPrefix = "sub_";

    private String sessionInfix = "_ses_";

    private String scanPrefix = "scn_";

    public DBMapping(CryptHashSettings cryptHashSettings)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, EncryptionException
    {
        init(cryptHashSettings);
    }

    private void init(CryptHashSettings cryptHashSettings)
            throws NoSuchAlgorithmException, UnsupportedEncodingException, EncryptionException
    {
        this.cryptHashSettings = cryptHashSettings;
        this.hasher = new StringHasher(cryptHashSettings.hashAlgorithm, StringCrypter.CHARSET_UTF8);

        // reuse hasher. Is safe in single threaded invocations!
        subjectMappings = new IDMapper();

        this.crypter = new StringCrypter(cryptHashSettings.encryptionKey,
                cryptHashSettings.cryptScheme,
                cryptHashSettings.hashAlgorithm,
                StringCrypter.CHARSET_UTF8);

        sessionMappings = new GroupMapper();
        scanMappings = new GroupMapper();
    }

    /**
     * Encrypt (UTF-8) String using current settings.
     */
    public byte[] encryptID(String id) throws EncryptionException
    {
        return crypter.encrypt(id);
    }

    /**
     * Decrypt bytes and return as (UTF-8) String.
     */
    public String decryptID(byte[] encryptedID) throws EncryptionException
    {
        return new String(crypter.decrypt(encryptedID), crypter.getCharacterEncoding());
    }

    public String decryptBase64EncodedID(String base64EncodedID) throws EncryptionException
    {
        return crypter.decryptString(base64EncodedID);
    }

    public String decryptHexencodedID(String hexencodedID) throws EncryptionException
    {
        return crypter.decryptHexEncodedString(hexencodedID);
    }

    public byte[] hashBytes(byte[] sourceBytes, byte[] saltBytes, int maxHashLen)
    {
        byte digest[] = hasher.hash(sourceBytes, true, saltBytes, cryptHashSettings.prefixSalt);
        digest = StringHasher.truncate(digest, maxHashLen);
        return digest;
    }

    public String getXnatSubjectLabel(String subjectKey)
    {
        return this.subjectMappings.getHashedLabelString(subjectKey);
    }

    public String getXnatSubjectEncryptedID(String subjectKey)
    {
        return this.subjectMappings.getHashedLabelString(subjectKey);
    }

    /**
     * The subjectKey can be for example: patientID or patientName.
     */
    public String getCreateXnatSubjectLabel(String subjectKey, boolean doCryptHash, boolean autoCreate) throws EncryptionException
    {
        // optionally check use subject:
        IDMapping mapping = subjectMappings.getCreateLabel(subjectKey, doCryptHash, autoCreate, subjectPrefix);

        if (mapping == null)
            return null;

        return mapping.labelString;
    }

    public boolean deleteSubjectLabel(String subjKey)
    {
        return (subjectMappings.remove(subjKey) != null);
    }

    public String getXnatSessionLabel(String subjectKey, String studyInstanceUID)
    {
        return sessionMappings.getHashedLabelString(subjectKey, studyInstanceUID);
    }

    public boolean deleteSessionLabel(String subjKey, String studyKey)
    {
        return sessionMappings.delete(subjKey, studyKey);
    }

    /**
     * The subjectKey can be for example: patientID or patientName.
     */
    public String getCreateXnatSessionLabel(String subjectKey, boolean doCryptHashSubject, String sessionKey, boolean doCryptHashSession,
            boolean autoCreate)
            throws EncryptionException
    {
        // ======
        // Patch
        // XNAT does not allow similar session Labels within the same project.
        // Prefix with subject label to distinct similar session labels for
        // different subjects:
        // =======

        String subjectLabel = this.getCreateXnatSubjectLabel(subjectKey, doCryptHashSubject, false);
        if (subjectLabel == null)
            throw new Error("SubjectLabel not found for (subjecKey)patientId:" + subjectKey);

        //
        return sessionMappings.getCreate(subjectKey, sessionKey, doCryptHashSession, subjectLabel + sessionInfix, autoCreate);
    }

    public String getXnatScanLabel(String sessionKey, String scanSetKey)
    {
        return this.scanMappings.getHashedLabelString(sessionKey, scanSetKey);
    }

    public String getCreateXnatScanLabel(String sessionKey, String scanSetKey, boolean doCryptHashScan, String labelPrefix,
            boolean autoCreate)
            throws EncryptionException
    {
        return scanMappings.getCreate(sessionKey, scanSetKey, doCryptHashScan, labelPrefix, autoCreate);
    }

    public boolean deleteScanLabel(String sessionKey, String scanSetKey)
    {
        return scanMappings.delete(sessionKey, scanSetKey);
    }

    /**
     * If PatientIDs are used as SubjectKey, this method return the list of subjects/patients.
     */
    public String[] getSubjectKeys()
    {
        return this.subjectMappings.getKeyArray();
    }

    /**
     * If StudyUIDs are used as SessionKey, this method returns the StudyUIDs
     */
    public String[] getSessionKeys(String subjectKey)
    {
        return this.sessionMappings.getKeyArray(subjectKey);
    }

    /** Collect all session keys (from all subjects) */
    public String[] getSessionKeys()
    {
        String keys[] = this.getSubjectKeys();
        if (keys == null)
            return null;

        StringList allKeys = new StringList();

        for (String key : keys)
        {
            // String subjLabel=this.subjectMappings.get(key);
            allKeys.add(getSessionKeys(key));
        }

        return allKeys.toArray();
    }

    public String[] getScanKeys(String sessionKey)
    {
        return this.scanMappings.getKeyArray(sessionKey);
    }

    public int getNumScanSets()
    {
        return this.getAllScanSetKeys().size();
    }

    public List<String> getAllScanSetKeys()
    {
        return this.scanMappings.getAllSubGroupKeys();
    }

    /**
     * Reverse lookup of Session key given the ScanSet Key.
     * 
     * @param Session
     *            Key of ScanSet key
     * @return
     */
    public String getSessionKeyOfScanSetKey(String scanSetKey)
    {
        // linear search:
        for (String sessionKey : this.scanMappings.getKeyArray())
        {
            IDMapping mapping = scanMappings.get(sessionKey).get(scanSetKey);
            if (mapping != null)
                return sessionKey;
        }
        return null;
    }

    /**
     * Reverse lookup of SubjectKey given the Session Key.
     * 
     * @param sessionKey
     * @return Subject Key of Session Key
     */
    public String getSubjectKeyOfSessionKey(String sessionKey)
    {
        // linear search:
        for (String subjectKey : this.sessionMappings.getKeyArray())
        {
            IDMapping mapping = sessionMappings.get(subjectKey).get(sessionKey);
            if (mapping != null)
                return subjectKey;
        }
        return null;
    }

    /**
     * Create CSV Mapping text. This contians the DBMapping between Dicom fields and XNAT Labels.
     */
    public void toCSV(String dataSetName, StringBuilder sb)
    {
        CSVData csvData = new CSVData(true);

        csvData.setFieldSeparator(";");

        String headers[] = new String[] {
                CSV_OWNERID, CSV_DATASETNAME,
                CSV_SUBJECTID, CSV_SUBJECTLABEL,
                CSV_SESSIONID, CSV_SESSIONLABEL,
                CSV_SCANID, CSV_SCANLABEL
        };

        csvData.setHeaders(headers);

        String sourceId = this.cryptHashSettings.getSourceID();

        String subjectKeys[] = this.getSubjectKeys();
        if (subjectKeys == null)
            return;

        for (String subjKey : subjectKeys)
        {
            IDMapping subjMapping = subjectMappings.get(subjKey);
            String subjectLabel = this.getXnatSubjectLabel(subjKey);

            String sessionKeys[] = this.getSessionKeys(subjKey);
            if (sessionKeys == null)
                continue;

            for (String sessKey : sessionKeys)
            {
                String sessionLabel = this.getXnatSessionLabel(subjKey, sessKey);

                String scanKeys[] = this.getScanKeys(sessKey);
                if (scanKeys == null)
                    continue;

                String row[] = new String[8];

                for (String scanKey : scanKeys)
                {
                    String scanLabel = this.getXnatScanLabel(sessKey, scanKey);

                    row[0] = sourceId;
                    row[1] = dataSetName;
                    row[2] = subjKey;
                    row[3] = subjectLabel;
                    row[4] = sessKey;
                    row[5] = sessionLabel;
                    row[6] = scanKey;
                    row[7] = scanLabel;

                    csvData.addRow(row);
                }
            }
        }

        csvData.toString(sb, ";", "\n");
    }

    // =====================
    // Life cycle management
    // =====================

    /**
     * Clear all DBMapings. After a clear() the Object may be reused.
     */
    public void clear()
    {
        this.scanMappings.clear();
        this.sessionMappings.clear();
        this.subjectMappings.clear();
    }

    /**
     * Dispose object. After a dispose() the object must not be used anymore.
     */
    public void dispose()
    {
        clear();
    }

}

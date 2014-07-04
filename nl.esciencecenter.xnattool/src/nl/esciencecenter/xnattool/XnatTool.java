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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.esciencecenter.medim.ImageDirEvent;
import nl.esciencecenter.medim.ImageDirScanner;
import nl.esciencecenter.medim.ImageDirScanner.FileFilterOptions;
import nl.esciencecenter.medim.ImageDirScannerListener;
import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.ScanSetInfo;
import nl.esciencecenter.medim.ScanSetInfo.FileDescriptor;
import nl.esciencecenter.medim.ScanSetInfo.ScanTypeParameters;
import nl.esciencecenter.medim.SeriesInfo;
import nl.esciencecenter.medim.StudyInfo;
import nl.esciencecenter.medim.SubjectInfo;
import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile;
import nl.esciencecenter.medim.dicom.DicomProcessor;
import nl.esciencecenter.medim.dicom.DicomTagFilters;
import nl.esciencecenter.medim.dicom.DicomTagFilters.DicomTagFilter;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.medim.dicom.types.DicomTags;
import nl.esciencecenter.medim.nifti.NiftiDirScanner;
import nl.esciencecenter.ptk.GlobalProperties;
import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.crypt.StringCrypter;
import nl.esciencecenter.ptk.crypt.StringCrypter.DecryptionFailedException;
import nl.esciencecenter.ptk.crypt.StringCrypter.EncryptionException;
import nl.esciencecenter.ptk.csv.CSVData;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.io.exceptions.FileURISyntaxException;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.net.URIUtil;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.ssl.CertificateStore;
import nl.esciencecenter.ptk.ssl.CertificateStoreException;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.PutMonitor;
import nl.esciencecenter.ptk.web.WebException;
import nl.esciencecenter.ptk.web.WebException.Reason;
import nl.esciencecenter.ptk.xml.XmlUtil;
import nl.esciencecenter.xnatclient.XnatClient;
import nl.esciencecenter.xnatclient.XnatClient.FilesCollection;
import nl.esciencecenter.xnatclient.data.ImageFileInfo;
import nl.esciencecenter.xnatclient.data.NewScanInfo;
import nl.esciencecenter.xnatclient.data.XnatFile;
import nl.esciencecenter.xnatclient.data.XnatProject;
import nl.esciencecenter.xnatclient.data.XnatScan;
import nl.esciencecenter.xnatclient.data.XnatSession;
import nl.esciencecenter.xnatclient.data.XnatSubject;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageContentType;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;
import nl.esciencecenter.xnatclient.exceptions.XnatAuthenticationException;
import nl.esciencecenter.xnatclient.exceptions.ParameterException;
import nl.esciencecenter.xnatclient.exceptions.XnatClientException;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 * Stateful XnatTool for Dicom Uploading and Identity Mapping to XNAT. Manages a set of DataSet Configurations.
 * 
 * @author Piter T. de Boer
 */
public class XnatTool implements ImageDirScannerListener
{
    private static ClassLogger logger = ClassLogger.getLogger(XnatTool.class);

    static
    {
        // logger.setLevelToDebug();
    }

    protected static ClassLogger getLogger()
    {
        return logger;
    }

    public static void assertEqual(String message, String expected, String actual) throws XnatToolException
    {
        if (expected == null)
        {
            throw new XnatToolException(message + "\nActual Value is null.");
        }
        else if (expected.equals(actual) == false)
        {
            throw new XnatToolException(message + "\nExpected='" + expected + "', actual='" + actual + "'");
        }
    }

    public static void assertNotNull(String message, Object value) throws XnatToolException
    {
        if (value == null)
            throw new XnatToolException(message);
    }

    // ========================================================================
    //
    // ========================================================================

    private XnatToolConfig config = null;

    private XnatClient xnatClient = null;

    // Project
    private String currentProjectID;

    private List<XnatProject> cachedXnatProjects;

    // filtering
    protected DicomDirScanner.FileFilterOptions filterOptions = new DicomDirScanner.FileFilterOptions();

    private String currentDataSetConfigName = null;

    private DataSetConfigList dataSetConfigs = null;

    // encryption and hash settings
    private CryptHashSettings cryptSettings = null;

    // Dicom processing
    private DicomProcessor dicomProcessor = null;

    // Data Mapping
    private DBMapping dbMapping = null;

    private ImageDirScanner imageDirScanner;

    private Set<String> scanSetUids;

    private CertificateStore certStore;

    private boolean hasPersistentConfig = true;

    // -------------------------------
    // backwards compatibility option.
    // -------------------------------

    protected boolean option_putAtlasUnderReconstructions = false;

    public XnatTool(XnatToolConfig newConfig, boolean hasPersistantConfig) throws Exception
    {
        this.hasPersistentConfig = hasPersistantConfig;
        init(newConfig);
    }

    public void updateConfig(XnatToolConfig newConfig) throws Exception
    {
        init(newConfig);
    }

    private void init(XnatToolConfig newConfig) throws Exception
    {
        this.config = newConfig;

        persistantUpdateXnatLocation(config.getXnatURI());

        // Default Filter Options:
        this.filterOptions.setExtensions(FileFilterOptions.default_dicom_extensions);
        this.filterOptions.checkFileSize = true;
        this.filterOptions.maxSliceFileSize = 10 * 1024 * 1024; //
        this.filterOptions.checkFileMagic = true;

        this.cryptSettings = new CryptHashSettings();

        reloadDataSetConfigs();

        if (dataSetConfigs == null)
        {
            // reload failed:
            dataSetConfigs = new DataSetConfigList();
        }

        DicomProcessingProfile dicomOpts = null;

        DataSetConfig dataSetConfig = this.getCurrentDataSetConfig();

        if (dataSetConfig != null)
        {
            dicomOpts = dataSetConfig.dicomProcessingProfile;
        }
        else
        {
            dicomOpts = DicomProcessingProfile.createDefault();
        }

        initDicomProcessor(dicomOpts);
    }

    public void setJSessionID(String jsession)
    {
        xnatClient.setJSessionID(jsession);
    }

    public void setXnatCredentials(String user, Secret password)
    {
        this.xnatClient.setCredentials(user, password);
    }

    public boolean authenticateXnat() throws WebException
    {
        xnatClient.connect();
        return xnatClient.isAuthenticated();
    }

    public boolean isXnatAuthenticated()
    {
        return xnatClient.isAuthenticated();
    }

    public java.net.URI getXnatURI()
    {
        return this.xnatClient.getServiceURI();
    }

    public XnatToolConfig getToolConfig()
    {
        return config;
    }

    public XnatClient getXnatClient()
    {
        return xnatClient;
    }

    /**
     * Set alternative CertificateStore
     */
    public void setCertificateStore(CertificateStore certStore) throws CertificateStoreException
    {
        this.xnatClient.setCertificateStore(certStore);
        this.certStore = certStore; // keep
    }

    public void createCachedir() throws IOException
    {
        FSPath cacheDirNode = getCacheDir();

        if (cacheDirNode.exists() == false)
        {
            cacheDirNode.mkdirs();
            logger.debugPrintf("Creating new CacheDir:%s\n", cacheDirNode);
        }
        else
        {
            logger.debugPrintf("Using existing CacheDir:%s\n", cacheDirNode);
        }
    }

    public FSPath getCacheDir() throws IOException
    {
        if (config.getImageCacheDir() == null)
            throw new NullPointerException("CacheDir not specified. Please specify Cache Dir.");

        FSPath cacheDirNode = this.getFSUtil().newFSPath(config.getImageCacheDir());
        return cacheDirNode;
    }

    /**
     * Change DataSetsConfig Directory and reload DataSetConfiguration
     */
    public boolean reloadDataSetsConfigDir(URI loc, boolean autoCreate) throws IOException
    {
        this.config.setDataSetsConfigDir(loc);
        // update now:
        this.saveToolConfig();

        deleteEncryptionKeys();

        // block/reset:
        this.currentDataSetConfigName = null;
        this.dataSetConfigs = null;
        this.dbMapping = null;

        boolean exists = FSUtil.getDefault().existsDir(loc.getPath());

        if (exists == false)
        {
            if (autoCreate)
            {
                exists = createDataSetsConfigDir();
                return exists;
            }
            else
            {
                return false; // UI checks this and sets 'Create' button.
            }
        }
        else
        {
            // Try to load default data set config:
            try
            {
                boolean reloaded = this.reloadDataSetConfigs();
                if (reloaded)
                {
                    logger.infoPrintf("Switched to existing DataSets configuration dir:%s\n", loc);
                }
            }
            catch (Exception e)
            {
                logger.warnPrintf("No default DataSet configuration(s) found at location:%s", loc);
            }
            return true; // directory exists, but no configuration found.
        }
    }

    // === Persistant Settings === //

    public void persistantUpdateXnatLocation(URI uri) throws XnatToolException, URISyntaxException, WebException, CertificateStoreException
    {
        this.config.updateURI(uri);
        if (this.xnatClient != null)
        {
            try
            {
                xnatClient.disconnect();
            }
            catch (Exception e)
            {
                logger.warnPrintf("Exeption when disconnecting from xnatClient:%s\n", xnatClient);
            }
        }

        // get User From Config:
        String user = this.config.getXnatUser();

        if (StringUtil.isEmpty(user))
        {
            // update user
            user = config.getXnatURI().getUserInfo();

            if (StringUtil.isEmpty(user))
                user = GlobalProperties.getGlobalUserName();
        }
        // auto update user
        config.updateUser(user);

        this.xnatClient = new XnatClient(config.getXnatURI(), user, null);

        if (this.certStore != null)
        {
            this.xnatClient.setCertificateStore(certStore);
        }
        this.saveToolConfig();
    }

    public void persistantSetCacheDir(URI loc, boolean autoCreate) throws IOException
    {
        this.config.setImageCacheDir(loc);
        if (autoCreate)
        {
            this.createCachedir();
        }
        saveToolConfig();
    }

    public void persistantSetDefaultSourceID(String name)
    {
        config.defaultSourceId = name;
        saveToolConfig();
    }

    // === Load/Save Tool Config.

    protected boolean saveToolConfig()
    {
        URI confPath;

        if (this.hasPersistentConfig == false)
        {
            return false;
        }

        try
        {
            confPath = XnatToolMain.getSettingsFileURI();
        }
        catch (URISyntaxException e1)
        {
            handleError(e1, "Invalid configuration location URI:%s\n", e1.getInput());
            return false;
        }

        try
        {
            XnatToolConfig.saveConfig(config, confPath.getPath());
            return true;
        }
        catch (Exception e2)
        {
            handleError(e2, "Couldn't save tool configuration to:%s\n", confPath);
        }

        return false;
    }

    private void handleError(Throwable ex, String message, Object... args)
    {
        // Call back to UI !
        logger.logException(ClassLogger.ERROR, ex, "Could not save configuration file to %s\n", args);
    }

    // ========================================================================
    // XNat Interface:
    // ========================================================================

    /**
     * Returns authorized project names.
     */
    public String[] getProjectIDs(boolean update) throws Exception
    {
        List<XnatProject> projs = getProjects(update);

        if ((projs == null) || (projs.size() <= 0))
            return null;

        int n = projs.size();

        String names[] = new String[n];
        for (int i = 0; i < n; i++)
            names[i] = projs.get(i).getID(); // Use Ids!;

        return names;
    }

    /**
     * Returns authorized project names. Project name might not be defined.
     */
    public String[] getProjectNames(boolean update) throws Exception
    {
        List<XnatProject> projs = getProjects(update);

        if ((projs == null) || (projs.size() <= 0))
            return null;

        int n = projs.size();

        String names[] = new String[n];
        for (int i = 0; i < n; i++)
            names[i] = projs.get(i).getName();

        return names;
    }

    public List<XnatProject> getProjects(boolean update) throws Exception
    {
        if ((update) || (this.cachedXnatProjects == null))
            this.cachedXnatProjects = xnatClient.listProjects();

        return this.cachedXnatProjects;
    }

    /**
     * Returns current cached project ID.
     */
    public String getCurrentProjectID()
    {
        return this.currentProjectID;
    }

    public String getXnatUsername()
    {
        return this.xnatClient.getUsername();
    }

    // ========================================================================
    // DBMapping and credentials
    // ========================================================================

    public String getDefaultSourceId()
    {
        String id = this.config.getDefaultSourceId();

        if ((id == null) || (id == ""))
        {
            // auto copy xnat user name:
            id = getXnatUsername();

            if (id != null)
            {
                id = id.toUpperCase();
            }
            config.setDefaultSourceId(id);
        }

        return id;
    }

    protected String getDataSetSourceID()
    {
        DataSetConfig setConfig = this.getCurrentDataSetConfig();

        if (setConfig == null)
        {
            return config.defaultSourceId;
        }

        return setConfig.sourceId;
    }

    public boolean hasCurrentDataSetconfig()
    {
        return (getCurrentDataSetConfig() != null);
    }

    public boolean isDataSetEncryptionKeyInitialized()
    {
        DataSetConfig setConfig = this.getCurrentDataSetConfig();

        if (setConfig == null)
        {
            return false;
        }

        return setConfig.hasValidEncryptionKey();
    }

    /**
     * Authenticate SourceID and Password with (stored) encrypted Source ID and encrypted Encryption Key. The SourceID
     * and Encryption Key are encrypted with an Password when written to the DataSet configuration file. This method
     * will decrypt the stored (Encrypted) Source ID and (encrypted) Encryption Key and match the decrypted Source ID
     * with the sourceId given as argument.
     * <p>
     * This will initialize the DBMapping and these credentials are also used by the DicomProcessor.
     * <p>
     * 
     * @param sourceId
     *            the current data set sourceId. Must match with the encrypted Source ID.
     * @param passPhrase
     *            - passprhase used to encrypt the Source ID ans the Encryption Key
     * @param autoCreateEncryptionKey
     *            - if no encryption key has been created, generate a key from the passPhrase.
     * 
     */
    public void authenticateEncryptionKeys(String sourceId, Secret passPhrase, boolean autoCreateEncryptionKey) throws Exception
    {
        DataSetConfig setConfig = this.getCurrentDataSetConfig();

        if (setConfig == null)
        {
            throw new XnatToolException("No Configuration loaded or created yet. Can't authenticate keys");
        }

        if (sourceId == null || sourceId == "")
        {
            throw new NullPointerException("Argument sourceId is empty or null!");
        }

        StringCrypter crypter = createEncrypter(passPhrase);

        byte[] encryptedSourceID = setConfig.getEncryptedSourceID();

        if (encryptedSourceID == null)
        {
            if (autoCreateEncryptionKey)
            {
                initializeDataSetEncryptionKeys(setConfig, crypter, sourceId, passPhrase);
                encryptedSourceID = setConfig.getEncryptedSourceID();
                if (encryptedSourceID == null)
                {
                    throw new XnatAuthenticationException("Failed to initailize a new Encryption Key. Returned Encrypted SourceID is null!");
                }
            }
            else
            {
                throw new XnatAuthenticationException("Encryption Keys not initialized. Auto creation of Encryption Key disabled.");
            }
        }

        byte[] encryptedKey = setConfig.getEncryptedKey();

        if (encryptedKey == null)
        {
            throw new XnatAuthenticationException("Encryption Keys not initialized. Created or decrypted Encryption Key is null!");
        }

        try
        {
            // To check whether the supplied key matches the DataSet the
            // sourceID is encrypted with that same key.
            // If the encrypted SourceID can be decrypted the keys match this
            // data set.

            String decryptedSourceId = new String(crypter.decrypt(encryptedSourceID), "UTF-8");

            if (!sourceId.equals(decryptedSourceId))
            {
                throw new XnatAuthenticationException("Authentication Failed: Passphrase and Owner ID combination do not match.");
            }
        }
        catch (DecryptionFailedException e)
        {
            throw new XnatAuthenticationException("Authentication Failed: Passphrase is incorrect.", e);
        }

        byte decryptedKey[] = crypter.decrypt(encryptedKey);

        DicomProcessingProfile procOpts = dicomProcessor.getProcessingOptions();
        procOpts.setEncryptionKey(decryptedKey);
        setConfig.setEncryptionKey(decryptedKey);

        // re-initialized DicomProcessor !
        dicomProcessor.updateProcessingOptions(procOpts);

        // Update ID and Keys for XNAT CryptHashing ...
        cryptSettings.setCredentials(sourceId, decryptedKey);

        dbMapping = new DBMapping(cryptSettings);

        logger.infoPrintf(" - sourceId          =%s\n", sourceId);
        logger.infoPrintf(" - encryptedKey      =%s\n", StringUtil.toHexString(encryptedKey));
        logger.infoPrintf(" - encryptedSourceId =%s\n", StringUtil.toHexString(encryptedSourceID));
        logger.infoPrintf(" - decryptedKey      =%s\n", StringUtil.toHexString(decryptedKey));
    }

    /**
     * Create Stateful Encrypter using current encryption settings.
     */
    private StringCrypter createEncrypter(Secret passPhrase) throws NoSuchAlgorithmException, UnsupportedEncodingException,
            EncryptionException
    {
        return new StringCrypter(
                passPhrase,
                cryptSettings.cryptScheme,
                cryptSettings.hashAlgorithm,
                StringCrypter.CHARSET_UTF8);
    }

    public void initializeEncryptionKeyFromSourceText(String sourceId, Secret keySourceText, Secret keyPassword) throws Exception
    {
        StringCrypter crypter = this.createEncrypter(keyPassword);
        DataSetConfig setConfig = this.getCurrentDataSetConfig();
        initializeDataSetEncryptionKeys(setConfig, crypter, sourceId, keySourceText);
    }

    protected DataSetConfig initializeDataSetEncryptionKeys(
            DataSetConfig config,
            StringCrypter crypter,
            String sourceId,
            Secret keyDigestSourceText) throws Exception
    {
        // generate (sha-256 hash) digest from key source text.
        byte digest[] = crypter.createKeyDigest(keyDigestSourceText);
        byte encryptedKey[] = crypter.encrypt(digest);
        byte encryptedSourceId[] = crypter.encrypt(sourceId);

        config.setCredentials(sourceId, digest);
        config.setEncryptedCredentials(encryptedSourceId, encryptedKey);

        this.saveDataSetConfigs(); // persistant update!
        return config;
    }

    public void deleteEncryptionKeys()
    {
        DataSetConfig setConfig = this.getCurrentDataSetConfig();
        if (setConfig == null)
            return; // no config loaded yet!

        setConfig.setCredentials(null, null);
        setConfig.setEncryptedCredentials(null, null);

        DicomProcessingProfile opts = this.dicomProcessor.getProcessingOptions();

        if (opts != null)
        {
            // remove encryptionKey;
            opts.setEncryptionKey(null);
        }

        if (this.dbMapping != null)
        {
            this.dbMapping.cryptHashSettings.setCredentials(null, null);
        }

        // clear DB mapping here ?
        this.dbMapping = null;
    }

    protected void saveMappings(String name, DBMapping mapping)
    {
        URI loc = getToolConfig().getDataSetsConfigDir();
        try
        {
            loc = URIUtil.appendPath(loc, name + "_mappings.csv");

            StringBuilder sb = new StringBuilder();
            mapping.toCSV(name, sb);
            String csvText = sb.toString();

            logger.debugPrintf("Saving new ID Mapping File: %s\n>>>-----------\n%s>>>-----------\n",
                    loc, csvText);

            FSUtil.getDefault().writeText(loc.getPath(), csvText);

        }
        catch (Exception e)
        {
            logger.logException(ClassLogger.ERROR, e, "Failed to write ID Mappings to CSV File:%sn", loc);
        }
    }

    protected void saveMetaData(String name, ImageDirScanner dicomSource, DBMapping mappings)
    {
        URI loc = getToolConfig().getDataSetsConfigDir();
        try
        {
            loc = URIUtil.appendPath(loc, name + "_metadata.csv");
            StringBuilder sb = new StringBuilder();

            new MetaDataWriter(dicomSource, mappings).toCSV(sb);

            String csvText = sb.toString();

            logger.debugPrintf("Saving new ID Mapping File: %s\n>>>-----------\n%s>>>-----------\n",
                    loc, csvText);

            FSUtil.getDefault().writeText(loc.getPath(), csvText);

        }
        catch (Exception e)
        {
            logger.logException(ClassLogger.ERROR, e, "Failed to write ID Mappings to CSV File:%sn", loc);
        }
    }

    public CSVData getMetaData() throws Exception
    {
        if (dbMapping == null)
        {
            throw new Exception("No (Meta) Data, please scan first.");
        }

        return new MetaDataWriter(imageDirScanner, dbMapping).toCSV();
    }

    protected void printMappings(DBMapping mapping, ImageDirScannerListener optListener)
    {
        if (optListener != null)
        {
            optListener.notifyImageDirScannerEvent(ImageDirEvent.newMessageEvent("=== Current XNAT DB Mappings ==="));
        }

        String subjs[] = mapping.getSubjectKeys();

        if (subjs != null)
        {
            for (int i = 0; i < subjs.length; i++)
            {
                String subjkey = subjs[i]; // is patientID;
                String xnatSubjectLabel = mapping.getXnatSubjectLabel(subjkey);
                logger.infoPrintf(">PatientID %s=>%s\n", subjkey, xnatSubjectLabel);
                if (optListener != null)
                {
                    optListener.notifyImageDirScannerEvent(ImageDirEvent.newMessageEvent("- Subject Mapping:" + subjkey + " -> "
                            + xnatSubjectLabel));
                }

                String sessionKeys[] = mapping.getSessionKeys(subjkey);

                if (sessionKeys == null)
                    continue;

                for (int j = 0; j < sessionKeys.length; j++)
                {
                    String sessKey = sessionKeys[j]; // is studyUID
                    String xnatSessionLabel = mapping.getXnatSessionLabel(subjkey, sessKey);
                    logger.infoPrintf(" - StudyUID: %s=>%s\n", sessKey, xnatSessionLabel);
                    if (optListener != null)
                    {
                        optListener.notifyImageDirScannerEvent(ImageDirEvent.newMessageEvent("- - Session Mapping:" + sessKey + " -> "
                                + xnatSessionLabel));
                    }

                    String scanKeys[] = mapping.getScanKeys(sessKey);
                    if (scanKeys == null)
                        continue;

                    for (int k = 0; k < scanKeys.length; k++)
                    {
                        String scanKey = scanKeys[k];
                        String xnatScanLabel = mapping.getXnatScanLabel(sessKey, scanKey);
                        logger.infoPrintf(" - - SeriesUID: %s=>%s\n", scanKey, xnatScanLabel);
                        if (optListener != null)
                        {
                            optListener.notifyImageDirScannerEvent(ImageDirEvent.newMessageEvent("- - -ScanSet Mapping:" + scanKey + " -> "
                                    + xnatScanLabel));
                        }

                    }
                }
            }
        }
    }

    // ========================================================================
    // Dicom Processing
    // ========================================================================

    protected void initDicomProcessor(DicomProcessingProfile options) throws Exception
    {
        // Load optional dicom tags configuration here:
        String dicomTagConfigurationFile = "dicom/dicom_tags.xcsv";

        DicomTags tagOpts;

        try
        {
            tagOpts = DicomTags.createFromFile(dicomTagConfigurationFile);
        }
        catch (Exception e)
        {
            throw new XnatToolException("Failed to read Dicom Tag Processing configuration file from:" + dicomTagConfigurationFile, e);
        }

        dicomProcessor = new DicomProcessor(tagOpts, options);
    }

    protected DicomProcessingProfile getProcessingOptions()
    {
        return dicomProcessor.getProcessingOptions();
    }

    // ========================================================================
    // Xnat Mappings
    // ========================================================================

    public XnatSubject getCreateSubject(String projectId, String subjectLabel, boolean autoCreate) throws Exception
    {
        if (StringUtil.isEmpty(projectId))
            throw new XnatToolException("ProjecId can't be empty)");

        if (StringUtil.isEmpty(subjectLabel))
            throw new XnatToolException("SubjectLabel can't be empty)");

        XnatSubject subject = null;

        try
        {
            subject = xnatClient.getSubjectByLabel(projectId, subjectLabel);
        }
        catch (WebException e)
        {
            if ((autoCreate == false) && (e.getReason() != Reason.RESOURCE_NOT_FOUND))
            {
                throw e;
            }
        }

        if (subject == null)
        {
            if (autoCreate == false)
                return null;

            // Create at XNAT DB
            subject = xnatClient.createSubject(projectId, subjectLabel);
            //
            logger.infoPrintf("Created new subject: %s:%s\n", subject.getID(), subject.getLabel());
            subject = xnatClient.getSubjectByLabel(projectId, subjectLabel);
        }

        // use subject create by current user!
        logger.infoPrintf("Got Subject:%s:%s\n", subject.getID(), subject.getLabel());
        return subject;
    }

    protected XnatSession getCreateSession(XnatSubject subject, String sessionLabel, boolean autoCreate) throws Exception
    {
        String projectId = subject.getProjectID();
        String subjectLabel = subject.getLabel();

        XnatSession session = xnatClient.getSession(projectId, subjectLabel, sessionLabel);

        if (session == null)
        {
            if (autoCreate == false)
                return null;

            long current = System.currentTimeMillis();
            GregorianCalendar now = new GregorianCalendar();
            now.setTimeInMillis(current);
            Date date = now.getTime();

            // New Object!
            session = XnatSession.createXnatSession(projectId, subjectLabel, sessionLabel);
            session.setSessionDate(date);
            //
            session = xnatClient.createSession(session);
            //
            logger.infoPrintf("Created new Session: %s:%s\n", session.getID(), session.getLabel());
            session = xnatClient.getSessionOfSubject(subject, sessionLabel);
        }

        logger.infoPrintf("Got Session:%s\n", session);
        return session;
    }

    protected XnatScan getCreateMrScan(XnatSubject subject, XnatSession session, String newScanLabel, ScanSetInfo scanSet,
            boolean autoCreate) throws WebException, XnatClientException
    {
        XnatScan scan = xnatClient.getScanByLabel(session, newScanLabel);

        if (scan == null)
        {
            if (autoCreate == false)
                return null;

            NewScanInfo info = new NewScanInfo(newScanLabel);
            // info.scanID=newScanID;

            info.note = "No Notes.";
            info.series_description = scanSet.getSeriesDescription();

            if (StringUtil.isEmpty(info.series_description))
            {
                info.series_description = "No Description.";
            }

            // info.scanID=newScanID;
            // info.mrScanDataType=mrScanDataType;
            info.quality = "usable";

            String scanid = xnatClient.createMrScan(session, info);
            logger.infoPrintf("Created new Scan:%s\n", scanid);
            scan = xnatClient.getScanByLabel(session, newScanLabel);
        }

        logger.infoPrintf(">>> Got Scan:%s\n", scan);
        return scan;
    }

    protected XnatSubject createNewSubject(String projectId, String subjectLabel) throws Exception
    {
        return this.getCreateSubject(projectId, subjectLabel, true);
    }

    protected XnatSession createNewSession(String projectId, String subjectLabel, String newSession) throws Exception
    {
        return this.getCreateSession(this.getCreateSubject(projectId, subjectLabel, false), newSession, true);
    }

    /**
     * 
     * Get Either Patient Name or Patient ID form the subject depending on set DataSetConfiguration.
     * 
     * @param subject
     *            - Subject.
     * @return
     */
    protected String getSubjectKey(SubjectInfo subject) throws XnatClientException
    {
        switch (getCurrentDataSetConfig().subjectKeyType)
        {
            case CRYPTHASH_PATIENT_NAME:
            case PLAIN_PATIENT_NAME:
            {
                return subject.getPatientName();
            }
            case CRYPTHASH_PATIENT_ID:
            case PLAIN_PATIENT_ID:
            {
                return subject.getPatientID();
            }
            default:
            {
                throw new ParameterException("Cannot determine Subject Key type:" + getCurrentDataSetConfig().subjectKeyType);
            }
        }
    }

    /**
     * Whether to crypt/hash the Session Key.
     * 
     * @return
     */
    protected boolean getDoCryptHashSessionKey()
    {
        return getCurrentDataSetConfig().getSessionKeyType().getDoCryptHash();
    }

    /**
     * Check whether to crypt+hash the Scan Key (Scan Id)
     * 
     * @return true if the ScanIDs need to be crypto-hashed.
     */
    protected boolean getDoCryptHashScanKey()
    {
        DataSetConfig conf = getCurrentDataSetConfig();
        boolean doCryptScanKey = conf.getScanKeyType().getDoCryptHash();

        // Atlas files have no ScanID, ScanId is use as Atlas (Reconstruction)
        // Label.

        switch (getCurrentDataSetConfig().dataSetType)
        {
            case NIFTI_ATLASSET:
            {
                return false;
            }
            default:
            {
                return doCryptScanKey;
            }
        }
    }

    protected String getScanSetLabelPrefix()
    {
        return "scn_";
    }

    /**
     * Whether to crypt/hash the Subject Key.
     * 
     * @return
     */
    protected boolean getDoCryptHashSubjectKey()
    {
        return getCurrentDataSetConfig().subjectKeyType.getDoCryptHash();
    }

    /**
     * Get Either Patient Name or Patient ID from the subject depending on the DataSet Configuration.
     */
    protected String getSessionKey(StudyInfo session) throws XnatClientException
    {
        switch (getCurrentDataSetConfig().sessionKeyType)
        {
            case CRYPTHASH_STUDY_UID:
            {
                return session.getStudyInstanceUID();
            }
            case PLAIN_STUDY_UID:
            {
                return session.getStudyInstanceUID();
            }
            case STUDY_DATE_YEAR:
            {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(session.getStudyDate());
                return "" + cal.get(Calendar.YEAR);
            }
            case STUDY_DATE_YEAR_MONTH:
            {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(session.getStudyDate());
                // january is month '0'!
                return "" + cal.get(Calendar.YEAR) + "_" + Presentation.to2decimals(1 + cal.get(Calendar.MONTH));
            }
            case PATIENT_AGE_YEAR:
            case PATIENT_AGE_YEAR_MONTHS:
            default:
            {
                throw new ParameterException("Unsupported Session Key type:" + getCurrentDataSetConfig().sessionKeyType);
            }
        }
    }

    // ========================================================================
    // Filters
    // ========================================================================

    /**
     * Add Repeat/Echo time filter. Times must match (TRMin <= TR <=TRMax) AND (TEMin <= TE <= TEMax)
     */
    public void addTRTEFilter(int minTR, int maxTR, int minTE, int maxTE)
    {
        DicomTagFilter fil = new DicomTagFilters.MinMaxFilter(Tag.RepetitionTime, minTR, maxTR);
        filterOptions.dicomTagFilters.add(fil);
        fil = new DicomTagFilters.MinMaxFilter(Tag.EchoTime, minTE, maxTE);
        filterOptions.dicomTagFilters.add(fil);
    }

    public ImageDirScanner.FileFilterOptions getFilterOptions()
    {
        return this.filterOptions;
    }

    // ========================================================================
    // Scan Image Directory
    // ========================================================================

    public void enableDicom() throws IOException
    {
        this.getCurrentDataSetConfig().setDataSetType(ImageTypes.DataSetType.DICOM_SCANSET);
        saveDataSetConfigs();
    }

    public void enableNifti() throws IOException
    {
        this.getCurrentDataSetConfig().setDataSetType(ImageTypes.DataSetType.NIFTI_SCANSET);
        saveDataSetConfigs();
    }

    public void enableAtlas() throws IOException
    {
        this.getCurrentDataSetConfig().setDataSetType(ImageTypes.DataSetType.NIFTI_ATLASSET);
        saveDataSetConfigs();
    }

    private void initImageDirScanner(ImageDirScannerListener optListener) throws XnatToolException
    {
        DataSetConfig conf = this.getCurrentDataSetConfig();

        if (conf == null)
        {
            throw new XnatToolException("No DataSet Configured. Please Initialize!");
        }

        ImageTypes.DataSetType dataType = conf.getDataSetType();

        if (dataType == ImageTypes.DataSetType.NIFTI_SCANSET)
        {
            NiftiDirScanner niftiScanner = new NiftiDirScanner(ImageTypes.DataSetType.NIFTI_SCANSET);
            niftiScanner.setScanSubType(conf.getScanSubType());
            imageDirScanner = niftiScanner;
        }
        else if (dataType == ImageTypes.DataSetType.NIFTI_ATLASSET)
        {
            NiftiDirScanner niftiScanner = new NiftiDirScanner(ImageTypes.DataSetType.NIFTI_ATLASSET);
            niftiScanner.setScanSubType(conf.getScanSubType());
            imageDirScanner = niftiScanner;
        }
        else if (dataType == ImageTypes.DataSetType.DICOM_SCANSET)
        {
            imageDirScanner = new DicomDirScanner();
        }
        else
        {
            throw new XnatToolException("Unrecognized DataType:" + dataType);
        }

        imageDirScanner.addDicomDirListener(this);

        if (optListener != null)
        {
            imageDirScanner.addDicomDirListener(optListener);
        }

    }

    /**
     * Scan on the image source directory.
     * 
     * @param URI
     *            The directory to be scanned.
     * @param optListener
     *            - Optional DicomDirScannerLister which receives updates during the scan.
     * @return - true if images were found.
     * @throws Exception
     */
    public boolean doScanImageSourceDir(URI imageSourceDir, ImageDirScannerListener optListener, DicomDirScanner.ScanMonitor optScanMonitor)
            throws Exception
    {
        // Update settings and save before performing actual scan.
        this.saveDataSetConfigs();

        if (this.dbMapping == null)
        {
            throw new XnatToolException("InitializationError: DBMapping NOT initialized!");
        }

        // this re-initialized DBMapping!
        // ProcessingOptions options = this.getProcessingOptions();
        // this.updateOwnerIDAndPassword(options.getSourceId(),options.getPassword());

        initImageDirScanner(optListener);

        imageDirScanner.clear();
        // update filters:
        imageDirScanner.setFilterOption(filterOptions);
        // perform scan:
        imageDirScanner.scanDirectory(imageSourceDir, true, optScanMonitor);
        // update:
        logger.infoPrintf("Got %d files and %d ScanSets\n", imageDirScanner.getNumFiles(), imageDirScanner.getNumScanSets());
        // process mappings:
        addDBMappings(optListener);

        return true;
    }

    @Override
    public void notifyImageDirScannerEvent(ImageDirEvent e)
    {
        logger.debugPrintf("<DicomDirEvent>:%s\n", e);
    }

    public int getLocalNumScanSets()
    {
        return imageDirScanner.getNumScanSets();
    }

    public ImageDirScanner getImageDirScanner()
    {
        return this.imageDirScanner;
    }

    // ========================================================================
    // DBMappings
    // ========================================================================

    public int getLocalNumSubjects()
    {
        String[] keys = dbMapping.getSubjectKeys();
        if (keys == null)
            return 0;
        return keys.length;
    }

    public int getLocalNumSessions()
    {
        String[] keys = dbMapping.getSessionKeys();
        if (keys == null)
            return 0;
        return keys.length;
    }

    protected void addDBMappings(ImageDirScannerListener optListener) throws Exception
    {
        if (this.dbMapping == null)
            throw new XnatToolException("DBMapping NOT initialized!");

        dbMapping.clear();

        scanSetUids = imageDirScanner.getScanSetUIDs();

        Iterator<String> it = scanSetUids.iterator();
        int index = 0;

        while (it.hasNext())
        {
            String uid = it.next();

            ScanSetInfo scanSet = imageDirScanner.getScanSet(uid);

            try
            {
                addScanMapping(scanSet);
            }
            catch (Exception e)
            {
                // init failed!
                logger.logException(ClassLogger.ERROR, e, "Failed to add/create DBMapping for scanSet:%s\n", scanSet);
                // keep scanset for now.
            }

            logger.infoPrintf("> SetId[%d]='%s'\n", index++, uid);

            SubjectInfo subj = scanSet.getSubjectInfo();
            ScanTypeParameters scanType = scanSet.getScanTypeParameters();
            StudyInfo studyInf = scanSet.getStudyInfo();
            SeriesInfo seriesInf = scanSet.getSeriesInfo();

            // do not let null pointer here cause havoc.
            try
            {
                logger.infoPrintf(" - StudyInstanceUID  = %s\n", scanSet.getStudyInstanceUID());
                logger.infoPrintf(" - StudyID           = %s\n", studyInf.getStudyId());
                logger.infoPrintf(" - xnatSessionLabel  = %s\n",
                        dbMapping.getXnatSessionLabel(getSubjectKey(subj), getSessionKey(studyInf)));

                logger.infoPrintf(" - SeriesInstanceUID = %s\n", scanSet.getSeriesInstanceUID());
                logger.infoPrintf(" - SeriesNr          = %d\n", seriesInf.getSeriesNr());
                logger.infoPrintf(" - SeriesDescription = %s\n", seriesInf.getSeriesDescription());
                logger.infoPrintf(" - xnatScanLabel     = %s\n",
                        dbMapping.getXnatScanLabel(scanSet.getStudyInstanceUID(), scanSet.getSeriesInstanceUID()));

                logger.infoPrintf(" - SeriesDate        = %s\n", seriesInf.getSeriesDate());
                logger.infoPrintf(" - StudyDate         = %s\n", studyInf.getStudyDate());
                logger.infoPrintf("   [ScanType]        \n");
                logger.infoPrintf(" - Modality          = %s\n", scanType.modality);
                logger.infoPrintf(" - ScanningSequence  = %s\n", scanType.scanningSequence);
                logger.infoPrintf(" - TR/TE (TI)        = %f/%f (%f)\n", scanType.repeatTime, scanType.echoTime, scanType.inverseTime);
                logger.infoPrintf(" - Flip Angle        = %f\n", scanType.flipAngle);

                logger.infoPrintf("   [Subject]\n");
                logger.infoPrintf(" - PatientName       = %s\n", subj.getPatientName());
                logger.infoPrintf(" - PatientId         = %s\n", subj.getPatientID());
                logger.infoPrintf(" - xnatSubjectLabel  = %s\n",
                        dbMapping.getCreateXnatSubjectLabel(getSubjectKey(subj), getDoCryptHashSubjectKey(), false));

                logger.infoPrintf(" - PatientAge        = %s\n", subj.getPatientAgeString());
                logger.infoPrintf(" - PatientGender     = %s\n", subj.getPatientGender());
                logger.infoPrintf(" - PatientBirthDate  = %s\n", subj.getPatientBirthDate());

                logger.infoPrintf("   [FileSet] \n");
                // logger.infoPrintf(" - fileSetID         = %s\n",scanSet.fileSetId);
                logger.infoPrintf(" - Number of files   = %d\n", scanSet.getNumFSNodes());
                logger.infoPrintf(" - first file        = %s\n", scanSet.getFirstFile());

                logger.infoPrintf(" ---------------------\n");
            }
            catch (Throwable e)
            {
                logger.logException(ClassLogger.WARN, e, "Logging exception:%s\n", e);
            }
        }

        printMappings(dbMapping, optListener);

        if (this.getToolConfig().getAutoCreateMappingsFile())
        {
            saveMappings(this.currentDataSetConfigName, dbMapping);
        }

        if (this.getToolConfig().getAutoExtractMetaData())
        {
            saveMetaData(this.currentDataSetConfigName, this.imageDirScanner, dbMapping);
        }
    }

    protected void addScanMapping(ScanSetInfo scanSet) throws XnatClientException, EncryptionException
    {
        if (dbMapping == null)
        {
            throw new Error("DBMapping not yet initialized:dbMapping==null");
        }

        // Map patientID,patientName => SubjectID
        SubjectInfo subjectInfo = scanSet.getSubjectInfo();
        String subjectKey = getSubjectKey(subjectInfo);
        if (StringUtil.isEmpty(subjectKey))
        {
            throw new NullPointerException("SubjectKey may not be Null or Empty!");
        }

        String subjectLabel = dbMapping.getCreateXnatSubjectLabel(subjectKey, getDoCryptHashSubjectKey(), true);

        // Map subjectLabel (patientID?)+StudyInstanceUID => sessionLabel
        StudyInfo studyInfo = scanSet.getStudyInfo();
        // String studyUID=scanSet.getStudyInstanceUID();
        String sessionKey = getSessionKey(studyInfo);

        if (StringUtil.isEmpty(sessionKey))
        {
            throw new NullPointerException("sessionKey may not be Null or Empty!");
        }

        String sessionLabel = dbMapping.getCreateXnatSessionLabel(subjectKey, getDoCryptHashSubjectKey(), sessionKey,
                getDoCryptHashSessionKey(), true);

        // Map studyUID+SeriesInstanceUID => scanLabel
        String seriesUID = scanSet.getSeriesInstanceUID();
        String scanLabel = dbMapping.getCreateXnatScanLabel(sessionKey, seriesUID, getDoCryptHashScanKey(), getScanSetLabelPrefix(), true);

        logger.infoPrintf("Added mapping PatientID/PatientName:%s/%s\n", subjectInfo.getPatientID(), subjectInfo.getPatientName());
        logger.infoPrintf(" - patient -> subject: '%s' (keyType=%s) -> '%s'\n", subjectKey, subjectLabel,
                this.getCurrentDataSetConfig().subjectKeyType);
        logger.infoPrintf(" - study   -> session: '%s' (keyType=%s) -> '%s'\n", sessionKey, sessionLabel,
                this.getCurrentDataSetConfig().sessionKeyType);
        logger.infoPrintf(" - series  -> scanSet: '%s' -> '%s'\n", seriesUID, scanLabel);
    }

    // ========================================================================
    // Uploading
    // ========================================================================

    /**
     * Upload Scan files
     * 
     * @throws Exception
     * 
     */
    public void doUploadTo(String projectId) throws Exception
    {
        createCachedir();
        this.doUploadTo(projectId, true, null);
    }

    public void doUploadTo(String projectId, boolean autoCreate, UploadMonitorListener monitor) throws Exception
    {
        if (monitor.isCancelled())
        {
            throw new XnatToolException("Upload cancelled.");
        }

        // Project must exist!

        List<String> scanIds = imageDirScanner.getScanSetUIDList();

        int numScans = scanIds.size();

        if ((this.xnatClient.isAuthenticated() == false) && (XnatToolMain.demoUpload == false))
        {
            throw new XnatToolException("User not authenticated. Please login first for user:" + this.getXnatUsername());
        }

        String sourceId = this.dbMapping.cryptHashSettings.getSourceID();

        // ScanSet file stats for monitor:
        int scanSetNumFiles[] = new int[numScans];

        for (int i = 0; i < numScans; i++)
        {
            ScanSetInfo scanSet = imageDirScanner.getScanSet(scanIds.get(i));
            scanSetNumFiles[i] = scanSet.getNumFSNodes();
        }

        String taskName = "Uploading ScanSets";
        monitor.notifyStartUpload(taskName, scanSetNumFiles);

        DataSetConfig dataSetConf = this.getCurrentDataSetConfig();
        ImageTypes.DataSetType dataSetType = dataSetConf.getDataSetType();
        // nifti sub type:
        ImageTypes.ScanSubType scanSubType = dataSetConf.getScanSubType();

        for (int scanNr = 0; scanNr < numScans; scanNr++)
        {
            String scanId = scanIds.get(scanNr);
            ScanSetInfo scanSet = imageDirScanner.getScanSet(scanIds.get(scanNr));

            // one scan per subject/session
            logger.infoPrintf(">>> Uploading ScanSet[#%d]:%s\n", scanNr, scanId);

            // Map patientID,patientName => SubjectID
            String subjectKey = getSubjectKey(scanSet.getSubjectInfo());
            String subjectLabel;
            subjectLabel = dbMapping.getCreateXnatSubjectLabel(subjectKey, getDoCryptHashSubjectKey(), false);

            if (subjectLabel == null)
            {
                // initialization error: DBMapping not correct.
                throw new XnatToolException("DBMapping Error: Subject Label does not exists for SubjectKey:" + subjectKey);
            }

            // Map subjectLabel (patientID?)+StudyInstanceUID => sessionLabel
            String sessionKey = getSessionKey(scanSet.getStudyInfo());
            String sessionLabel = dbMapping.getCreateXnatSessionLabel(subjectKey, getDoCryptHashSubjectKey(), sessionKey,
                    getDoCryptHashSessionKey(), false);
            if (sessionLabel == null)
            {
                // initialization error: DBMapping not correct.
                throw new XnatToolException("DBMapping Error: Session Label does not exists for SessionKey:" + sessionKey);
            }

            // Map studyUID+SeriesInstanceUID => scanLabel
            String scanUID = scanSet.getScanUID();
            String scanLabel;

            switch (dataSetType)
            {
                case DICOM_SCANSET:
                {
                    scanLabel = dbMapping.getCreateXnatScanLabel(sessionKey, scanUID, getDoCryptHashScanKey(), getScanSetLabelPrefix(),
                            autoCreate);
                    break;
                }
                case NIFTI_SCANSET:
                {
                    scanLabel = dbMapping.getCreateXnatScanLabel(sessionKey, scanUID, getDoCryptHashScanKey(), getScanSetLabelPrefix(),
                            autoCreate);
                    break;
                }
                case NIFTI_ATLASSET:
                {
                    scanLabel = ImageTypes.getScanSubTypeFileLabel(scanSubType);
                    break;
                }
                default:
                    throw new Error("getScanSetLabelPrefix():Invalid DataSetType:" + getCurrentDataSetConfig().dataSetType);
            }

            if (scanLabel == null)
            {
                // initialization error: DBMapping not correct.
                throw new XnatToolException("DBMapping Error: ScanSet Label does not exists for ScanUID(SeriesUID):" + scanUID);
            }

            XnatSubject subject = null;
            XnatSession session = null;
            XnatScan scan = null;

            scanSet.setSeriesDescription("Scan Set Type:" + dataSetType + ", scanSubType=" + scanSubType + ",ownerId=" + sourceId);

            if (XnatToolMain.demoUpload == false)
            {
                subject = getCreateSubject(projectId, subjectLabel, autoCreate);
                assertNotNull("No such subject or creation failed for project/subject:" + projectId + "/" + subjectLabel, subject);
                assertEqual("Subject Label in Subject Object must match actual subjectLabel", subjectLabel, subject.getLabel());

                session = getCreateSession(subject, sessionLabel, autoCreate);
                assertNotNull("No such session or creation failed for subject/session:" + subjectLabel + "/" + sessionLabel, session);
                assertEqual("Session Label in Session Object must match actual sessionLabel", sessionLabel, session.getLabel());
                // ================
                // Create Scan:
                // ================

                scan = getCreateMrScan(subject, session, scanLabel, scanSet, true);

                logger.infoPrintf(">>> Got Scan:%s\n", scan);
            }

            if (monitor != null)
            {
                monitor.notifyCollectionStart(scanNr, "ScanSet:#" + scanNr + ".");
            }

            uploadScanSetFiles(dataSetType, subject, session, scan, scanSet, scanNr, monitor);
            scanSet.setUploadFinishedDate(Presentation.now());

            if (monitor != null)
            {
                monitor.notifyCollectionDone(scanNr);
            }
        }
    }

    protected void uploadScanSetFiles(ImageTypes.DataSetType dataSetType, XnatSubject subject, XnatSession session, XnatScan xnatScan,
            ScanSetInfo scanSet, int scanNr, UploadMonitorListener monitor) throws Exception
    {

        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();
        String scanLabel = xnatScan.getID();

        // bridge between PutMonitor and UploadMonitorListener:
        PutMonitorAdaptor putMonitor = new PutMonitorAdaptor(monitor);

        // query existing files.
        FilesCollection fileCollections = this.xnatClient.listScanFiles(session, scanLabel);

        List<FileDescriptor> files = scanSet.getFileDescriptors();

        if ((files == null) || (files.size() <= 0))
        {
            throw new XnatToolException("No Files to upload!");
        }

        if ((dataSetType == ImageTypes.DataSetType.NIFTI_SCANSET) && (files.size() != 1))
        {
            throw new XnatToolException("When uploading Nifti ScanSets, each ScanSet may only contain one file! Number of files="
                    + files.size());
        }

        // if ( (dataSetType==DataSetType.NIFTI_ATLAS) && (files.size()!=2) )
        // {
        // throw new
        // XnatToolException("When uploading Nifti Atlases, each ScanSet must contain two files! Number of files="+files.size());
        // }

        boolean isAtlas = (dataSetType == ImageTypes.DataSetType.NIFTI_ATLASSET);

        for (int fileNr = 0; fileNr < files.size(); fileNr++)
        {
            if (monitor.isCancelled())
            {
                throw new XnatToolException("Upload cancelled!");
            }

            FileDescriptor fileDescr = files.get(fileNr);
            if ((fileDescr == null) || (fileDescr.fsNode == null))
            {
                // possible since files are ordered using scan nr. and some may
                // be missing.
                continue;
            }

            FSPath uploadFile = fileDescr.fsNode;
            String fileLabel = fileDescr.fileLabel;

            FSPath orgFile = uploadFile;
            FSPath sourceFile = orgFile;
            FSPath processedFile = null;

            if (dataSetType == ImageTypes.DataSetType.DICOM_SCANSET)
            {
                String destFilename = createXnatDicomTargetFileName(subjectLabel, sessionLabel, scanLabel, fileNr);

                boolean doProcessDicom = this.getProcessingOptions().getDoProcessDicom();
                if (doProcessDicom)
                {
                    if (monitor != null)
                    {
                        monitor.logPrintf("Processing DICOM File:\n - %s\n", sourceFile);
                    }

                    processedFile = processFile(this.getCacheDir(), sourceFile, destFilename);
                    uploadFile = processedFile; // upload new file.
                }

                long fileSize = uploadFile.getFileSize();

                if (monitor != null)
                {
                    monitor.notifyFileStart(scanNr, fileNr, fileSize, "File:" + uploadFile.getPathname());
                }

                // =======================================
                // Check and Verify existing Remote File !
                // =========================================

                boolean exists = checkAndVerifyExisting(fileCollections, ImageFormatType.DICOM, orgFile, uploadFile, destFilename,
                        getToolConfig().getAutoResumeAndVerifyUpload());

                if (exists)
                {
                    logger.infoPrintf("Remote DICOM file already exists: skip existing:%s\n", orgFile);

                    if (monitor != null)
                    {
                        monitor.logPrintf("Verified: Remote DICOM file already exists and file sizes match for:\n - %s\n", orgFile);
                    }
                }
                else
                {
                    if (XnatToolMain.demoUpload)
                    {
                        logger.infoPrintf("DEMO:putDicomFile:%s -> %s\n\n", uploadFile, destFilename);
                    }
                    else
                    {
                        logger.infoPrintf("putDicomFile:%s -> %s\n\n", uploadFile, destFilename);
                        putDicomFile(session, xnatScan, uploadFile, fileNr, destFilename, putMonitor);
                    }
                }
            }
            else if (dataSetType == ImageTypes.DataSetType.NIFTI_SCANSET)
            {
                StringHolder basenameH = new StringHolder();
                StringHolder extensionH = new StringHolder();
                imageDirScanner.splitBasenameAndExtension(uploadFile, basenameH, extensionH);

                String ext = extensionH.value.toLowerCase();

                String destFilename = createXnatNiftiTargetFileName(subjectLabel, sessionLabel, scanLabel, ext);

                boolean exists = checkAndVerifyExisting(fileCollections, ImageFormatType.NIFTI, orgFile, uploadFile, destFilename,
                        getToolConfig().getAutoResumeAndVerifyUpload());

                if (exists)
                {
                    logger.infoPrintf("Remote NIFTI file already exists: skip existing:%s\n", orgFile);

                    if (monitor != null)
                    {
                        monitor.logPrintf("Verified: Remote NIFTI file already exists and file sizes match for:\n - %s\n", orgFile);
                    }
                }
                else
                {
                    long fileSize = uploadFile.getFileSize();

                    if (monitor != null)
                    {
                        monitor.notifyFileStart(scanNr, fileNr, fileSize, uploadFile.toString());
                    }

                    if (XnatToolMain.demoUpload)
                    {
                        logger.infoPrintf("DEMO: putNiftiFile (atlas=%s): %s -> %s\n\n", isAtlas, uploadFile, destFilename);
                    }
                    else
                    {
                        logger.infoPrintf("putNiftiFile (atlas=%s): %s -> %s\n\n", isAtlas, uploadFile, destFilename);
                        putNiftiScanSetFile(session, xnatScan, uploadFile, destFilename, putMonitor);
                    }
                }
            }
            else if (dataSetType == ImageTypes.DataSetType.NIFTI_ATLASSET)
            {
                StringHolder basenameH = new StringHolder();
                StringHolder extensionH = new StringHolder();
                imageDirScanner.splitBasenameAndExtension(uploadFile, basenameH, extensionH);
                String ext = extensionH.value.toLowerCase();

                String destFilename;
                boolean isAnnotation;
                String reconId = "";
                String atlasScanLabel = null;

                if (fileNr == 0)
                {
                    destFilename = createXnatNiftiAtlasFileName(subjectLabel, sessionLabel, scanLabel, ext);
                    isAnnotation = false;
                }
                else
                {
                    reconId = session.getLabel() + "_" + fileLabel;
                    String atlasLabel = "atlas." + fileLabel; // Actual Atlas,
                                                              // put in
                                                              // reconstructions.
                    atlasScanLabel = "atlas_" + fileLabel;
                    destFilename = createXnatNiftiAtlasFileName(subjectLabel, sessionLabel, atlasLabel, ext);
                    isAnnotation = true;
                }

                long fileSize = uploadFile.getFileSize();

                if (monitor != null)
                {
                    monitor.notifyFileStart(scanNr, fileNr, fileSize, uploadFile.toString());
                }

                if (isAnnotation)
                {
                    // put annotations under reconstructions of this session:
                    if (XnatToolMain.demoUpload == false)
                    {
                        if (option_putAtlasUnderReconstructions)
                        {
                            // create reconstruction;
                            this.putNiftiReconstructionFile(session, reconId, uploadFile, destFilename, putMonitor);
                        }
                        else
                        {
                            // create scan with atlas label.
                            XnatScan atlasScan = getCreateMrScan(subject, session, atlasScanLabel, scanSet, true);
                            putNiftiScanSetFile(session, atlasScan, uploadFile, destFilename, putMonitor);
                        }
                    }
                    else
                    {
                        logger.infoPrintf("putNiftiReconstructionFile reconId= %s (isAnnotation=%s): %s -> %s\n\n", reconId, ""
                                + isAnnotation, uploadFile, destFilename);
                    }
                }
                else
                {
                    if (XnatToolMain.demoUpload == false)
                    {
                        putNiftiScanSetFile(session, xnatScan, uploadFile, destFilename, putMonitor);
                    }
                    else
                    {
                        logger.infoPrintf("putNiftiFile (isAnnotation=%s): %s -> %s\n\n", "" + isAnnotation, uploadFile, destFilename);
                    }
                }
            }

            if (monitor != null)
            {
                monitor.notifyFileDone(scanNr, fileNr);
            }

            if ((processedFile != null) && (config.getKeepProcessedDicomFile() == false))
            {
                try
                {
                    processedFile.delete();
                }
                catch (Exception e)
                {
                    logger.logException(ClassLogger.ERROR, e, "Failed to delete image file:" + processedFile);
                }
            }
        } // for files
    }

    private boolean checkAndVerifyExisting(FilesCollection filesCollection, ImageFormatType formatType, FSPath orgFile, FSPath uploadFile,
            String destFilename, boolean autoResumeAndVerifyUpload) throws XnatToolException, IOException
    {
        String resourceLabel = "";

        if (formatType != null)
        {
            resourceLabel = formatType.toString();
        }

        XnatFile remoteFile = filesCollection.getFile(resourceLabel, destFilename);

        if (remoteFile == null)
        {
            return false;
        }

        if (this.getToolConfig().getAutoResumeAndVerifyUpload() == false)
        {
            throw new XnatToolException("Remote file already exists:" + remoteFile);
        }

        // check size
        if (remoteFile.getFileSize() != uploadFile.getFileSize())
        {
            throw new XnatToolException("Verify Failed: Remote file exists already but has different file size!\n"
                    + "Size=" + orgFile.getFileSize() + "; for Source File:" + orgFile + "\n"
                    + "Size=" + uploadFile.getFileSize() + "; for Processed File:" + uploadFile + "\n"
                    + "Size=" + remoteFile.getFileSize() + "; for Remote File=" + remoteFile.getFileName() + "\n");
        }

        return true;
    }

    protected void putDicomFile(XnatSession session, XnatScan scan, FSPath file, int fileNum, String targetFilename, PutMonitor putMonitor)
            throws Exception
    {
        // current support DICOM/T1 only!
        ImageFileInfo info = new ImageFileInfo(ImageFormatType.DICOM, ImageContentType.T1_RAW);
        info.setDestinationFilename(targetFilename);

        if (XnatToolMain.demoUpload)
        {
            logger.infoPrintf(">>> Dummy Run for slice/file: #%d/%s\n", fileNum, file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());
            try
            {
                Thread.sleep(10);
            }
            catch (Throwable t)
            {
                ;
            }
        }
        else
        {
            logger.infoPrintf(">>> Uploading set['%s']#%d=%s\n", scan.getID(), fileNum, file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());
            String resultId = this.xnatClient.putDicomFile(session, scan, file.getPathname(), info, putMonitor);
            logger.debugPrintf(">>> result=%s\n", resultId);
        }
    }

    protected void putNiftiScanSetFile(XnatSession session, XnatScan scan, FSPath file, String targetFilename, PutMonitor putMonitor)
            throws Exception
    {
        ImageFileInfo info = new ImageFileInfo(ImageFormatType.NIFTI, ImageContentType.T1_RECON);
        info.setDestinationFilename(targetFilename);
        String scanId = scan.getID();

        if (XnatToolMain.demoUpload)
        {
            logger.infoPrintf(">>> Dummy Run for nifti: %s\n", file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());
            try
            {
                Thread.sleep(10);
            }
            catch (Throwable t)
            {
                ;
            }
        }
        else
        {
            logger.infoPrintf(">>> Uploading Nifti ScanSet: %s\n", scan.getID(), file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());

            String resultId = this.xnatClient.putNiftiScanFile(session, scanId, file.getPathname(), info, putMonitor);

            logger.debugPrintf(">>> result=%s\n", resultId);
        }
    }

    protected void putNiftiReconstructionFile(XnatSession session, String reconId, FSPath file, String targetFilename, PutMonitor putMonitor)
            throws Exception
    {
        ImageFileInfo info = new ImageFileInfo(ImageFormatType.NIFTI, ImageContentType.T1_RECON);
        info.setDestinationFilename(targetFilename);

        if (XnatToolMain.demoUpload)
        {
            logger.infoPrintf(">>> Dummy Run for nifti: %s\n", file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());
            try
            {
                Thread.sleep(10);
            }
            catch (Throwable t)
            {
                ;
            }
        }
        else
        {
            logger.infoPrintf(">>> putReconstructionNiftiFile session/reconId:([%s,%s]: %s\n", session.getLabel(), reconId, file);
            logger.infoPrintf(" - > target filename=%s\n", info.getDestinationFilename());
            String resultId;

            // create reconstruction first:
            String status = xnatClient.createReconstruction(session, reconId, info.getContentType());
            // put reconstruction:
            resultId = this.xnatClient.putReconstructionFile(session, reconId, file.getPathname(), info, putMonitor);

            logger.debugPrintf(">>> result=%s\n", resultId);
        }
    }

    protected static String createXnatDicomTargetFileName(String subjectLabel, String sessionLabel, String scanLabel, int fileNum)
    {
        // padd with zeros:
        String fileNumStr = Presentation.to3decimals(fileNum);
        String fileName = subjectLabel + "." + sessionLabel + "." + scanLabel + ".file_" + fileNumStr + ".dcm";

        return fileName;
    }

    protected static String createXnatNiftiTargetFileName(String subjectLabel, String sessionLabel, String scanLabel, String ext)
    {
        String fileName = subjectLabel + "." + sessionLabel + "." + scanLabel + "." + ext;
        return fileName;
    }

    protected static String createXnatNiftiAtlasFileName(String subjectLabel, String sessionLabel, String atlasLabel, String ext)
    {
        String fileName = subjectLabel + "." + sessionLabel;

        if (atlasLabel != null)
        {
            fileName += "." + atlasLabel;
        }

        fileName += "." + ext;

        return fileName;
    }

    protected FSPath processFile(FSPath tmpDir, FSPath sourceFile, String destFilename) throws Exception
    {
        logger.debugPrintf("Processing dicom file:%s to:%s\n", sourceFile, destFilename);

        DicomWrapper wrap = DicomWrapper.readFrom(sourceFile.getURI());

        wrap.setIsModifyable(true);
        wrap = dicomProcessor.process(wrap);

        wrap.performChecks(true);

        DicomObject dic = wrap.getDicomObject();
        FSPath destFile = tmpDir.resolvePath(destFilename);
        DicomUtil.writeDicom(dic, destFile.getPathname());

        return destFile;
    }

    // ========================================================================
    // Utils
    // ========================================================================

    public FSUtil getFSUtil()
    {
        return FSUtil.getDefault();
    }

    // ========================================================================
    // DataSetConfig Management
    // ========================================================================

    protected URI createDataSetsConfigLocation() throws URISyntaxException
    {
        URI loc = getToolConfig().getDataSetsConfigDir();
        if (loc == null)
            return null;
        loc = URIUtil.appendPath(loc, "datasets_config.xcfg");
        return loc;
    }

    protected void saveDataSetConfigs() throws IOException
    {
        try
        {
            URI loc = createDataSetsConfigLocation();
            DataSetConfigList.saveTo(this.dataSetConfigs, loc.getPath());
        }
        catch (URISyntaxException e)
        {
            throw new IOException("Couldn't create target file URI:" + e.getReason(), e);
        }

    }

    public String getDataSetConfigName()
    {
        return currentDataSetConfigName;
    }

    protected boolean reloadDataSetConfigs() throws IOException, URISyntaxException
    {
        URI loc = createDataSetsConfigLocation();
        if (loc == null)
            return false;

        try
        {
            this.dataSetConfigs = DataSetConfigList.loadFrom(loc.getPath());
        }
        catch (Exception e)
        {
            this.dataSetConfigs = new DataSetConfigList();
            logger.warnPrintf("Could NOT (re)load DataSetConfigurations from :%s\n", loc);
            return false;
        }

        // update with first name from list:
        if ((dataSetConfigs != null) && (dataSetConfigs.size() > 0))
        {
            logger.infoPrintf("reloadDataSetConfigs() Loaded: %d DataSet configurations from:%s\n", dataSetConfigs.size(), loc);
            this.currentDataSetConfigName = dataSetConfigs.getDataSetConfig(0).getDataSetName();
            logger.infoPrintf("reloadDataSetConfigs() Setting current dataset name=%s\n", currentDataSetConfigName);
        }
        else
        {
            logger.infoPrintf("reloadDataSetConfigs() No DataSet configurations from:%s\n", loc);
            this.currentDataSetConfigName = null; // should block processing!
        }

        return true;
    }

    public DataSetConfig switchToDataSetConfig(String name) throws Exception
    {
        if (name == null)
            throw new NullPointerException("Name is NULL!");

        if (dataSetConfigs == null)
            return null;

        DataSetConfig config = getDataSetConfigs(true).getDataSetConfig(name);

        if (config != null)
        {
            this.currentDataSetConfigName = config.getDataSetName();
            updateCurrentDataSetConfig(config);
            return config;
        }

        return null;
    }

    protected void updateCurrentDataSetConfig(DataSetConfig dataSetConfig) throws Exception
    {
        currentDataSetConfigName = dataSetConfig.dataSetName;

        // invalid config:
        if (dataSetConfig.getDicomProcessingProfile() == null)
        {
            throw new XnatToolException("DataSetConfiguration doesn't contains dicomProcessingProfile!\n"
                    + XmlUtil.prettyFormat(config.toXML(), 3));
        }

        initDicomProcessor(dataSetConfig.dicomProcessingProfile);

        // clear DB Mapping!
        this.dbMapping = null;
    }

    public List<String> getDataSetNames()
    {
        return this.getDataSetConfigs(true).getDataSetConfigNames();
    }

    public boolean createDataSetsConfigDir() throws IOException
    {
        URI uri = this.getToolConfig().getDataSetsConfigDir();

        FSUtil fs = getFSUtil();
        FSPath dir = fs.newLocalDir(uri);

        if (dir.exists())
        {
            return true;
        }
        else
        {
            // auto create parent as well.
            if (dir.getParent().exists() == false)
            {
                dir.getParent().mkdir();
            }
        }

        dir.mkdir();
        logger.infoPrintf("Created new DataSet config directory:%s\n", uri);
        return true;
    }

    protected DataSetConfigList getDataSetConfigs(boolean autoInit)
    {
        if ((dataSetConfigs == null) && (autoInit))
            dataSetConfigs = new DataSetConfigList();
        return dataSetConfigs;
    }

    public DataSetConfig getCurrentDataSetConfig()
    {
        return getDataSetConfigs(true).getDataSetConfig(this.currentDataSetConfigName);
    }

    public DataSetConfig createNewDataSetConfig(String sourceId, String newName) throws Exception
    {
        // defaults:
        DicomProcessingProfile dicomOpts = DicomProcessingProfile.createDefault();
        DataSetConfig newConfig = new DataSetConfig(dicomOpts);
        newConfig.setSourceId(sourceId);
        newConfig.setDataSetName(newName);
        newConfig.setImageSourceDir(URIUtil.appendPath(FSUtil.getDefault().getUserHome(), "dicom"));

        Exception e1 = null;

        try
        {
            getDataSetConfigs(true).add(newConfig);
            this.saveDataSetConfigs();
        }
        catch (Exception e)
        {
            e1 = e;
        }

        // update
        switchToDataSetConfig(newName);

        if (e1 != null)
            throw e1;

        return newConfig;
    }

    public void setDataSetImageSourceDir(String loc) throws XnatToolException, IOException
    {

        DataSetConfig setConf = getCurrentDataSetConfig();

        if (setConf == null)
        {
            throw new XnatToolException("No Current DataSet created!");
        }

        setConf.setImageSourceDir(resolveURI(loc));
        saveDataSetConfigs();
    }

    public void setDataSetScanSubType(ImageTypes.ScanSubType subType) throws XnatToolException, IOException
    {
        DataSetConfig setConf = getCurrentDataSetConfig();

        if (setConf == null)
        {
            throw new XnatToolException("No Current DataSet created!");
        }

        setConf.setScanSubType(subType);
        saveDataSetConfigs();
    }

    private URI resolveURI(String loc) throws FileURISyntaxException
    {
        return getFSUtil().resolvePathURI(loc);
    }

    public DataSetConfig getDataSetConfig(String name)
    {
        return this.getDataSetConfigs(true).getDataSetConfig(name);
    }

    public void clearImageCacheDir() throws IOException
    {
        URI cacheDir = this.getToolConfig().getImageCacheDir();
        FSPath dir = this.getFSUtil().newLocalDir(cacheDir);

        FSPath nodes[] = dir.listNodes();
        for (FSPath node : nodes)
        {
            logger.debugPrintf("Deleting cache file:%s\n", node);
            node.delete();
        }
    }

    /**
     * Perform graceful exit and cleanup/dispose held resources.
     */
    public void dispose()
    {

        if (getToolConfig().getClearImageCacheDirAfterExit())
        {
            try
            {
                this.clearImageCacheDir();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        if (imageDirScanner != null)
        {
            imageDirScanner.dispose();
            imageDirScanner = null;
        }

        if (this.dbMapping != null)
        {
            this.dbMapping.dispose();
            this.dbMapping = null;
        }

        if (this.dicomProcessor != null)
        {
            this.dicomProcessor.dispose();
            this.dicomProcessor = null;
        }

    }

    public void doUploadCSV(String projectId, CSVData csvData, boolean autoCreateSubjects, boolean autoCreateSessions,
            UploadMonitor uploadMonitor) throws WebException, XnatClientException
    {
        xnatClient.putMetaData(projectId, csvData, autoCreateSubjects, autoCreateSessions, uploadMonitor);
    }

    // ==================
    // XnatCMD Interface
    // ==================

}

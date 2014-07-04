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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import nl.esciencecenter.ptk.GlobalProperties;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.net.URIFactory;
import nl.esciencecenter.ptk.net.URIUtil;
import nl.esciencecenter.ptk.ssl.CertificateStore;
import nl.esciencecenter.ptk.ssl.CertificateStoreException;
import nl.esciencecenter.ptk.util.ResourceLoader;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.xml.XmlUtil;
import nl.esciencecenter.xnattool.ui.UIUtil;
import nl.esciencecenter.xnattool.ui.XnatToolFrame;
import nl.esciencecenter.xnattool.ui.XnatToolMainPanel;
import nl.esciencecenter.xnattool.ui.XnatToolPanelController;

/**
 * Globals for XnatTool + main starter for the XnatTool.
 */
public class XnatToolMain
{
    public static final String ESCXNAT_CONFIGSUBDIR = ".escxnat";

    public static final String ESCXNAT_CONFIGFILE = "settings.xcfg";

    public static final String ESCXNAT_DEFAULT_PROFILE = "profile.xcfg";

    public static final String ESCXNAT_PROPERTIES_FILE = "escxnat_properties.prop";

    public static final String PROP_ESCXNAT_VERSION = "escxnat.version";

    // === static private ===

    private static ClassLogger logger = ClassLogger.getLogger(XnatToolMain.class);

    static
    {

        if (GlobalProperties.isWindows())
        {
            // looks slightly better in Windows 7:
            // NATIVE = WINDOWS
            UIUtil.switchLookAndFeel(UIUtil.LookAndFeelType.NATIVE);
        }
        else if (GlobalProperties.isMac())
        {
            // Mac has nice native LAF also
            UIUtil.switchLookAndFeel(UIUtil.LookAndFeelType.NATIVE);
        }
        else
        {
            // UIUtil.switchLookAndFeel(UIUtil.LookAndFeelType.NIMBUS);
            UIUtil.switchLookAndFeel(UIUtil.LookAndFeelType.PLASTIC_3D);
        }

        // ClassLogger.getLogger(XnatToolMain.class).setLevelToDebug();
        // ClassLogger.getLogger(XnatTool.class).setLevelToDebug();

        // ClassLogger.getLogger(WebClient.class).setLevelToDebug();

    }

    // === static public ===

    public static boolean demoUpload = false;

    /**
     * Returns ~/.escxnat/
     * 
     * @throws URISyntaxException
     */
    public static URI getConfigDirURI() throws URISyntaxException
    {
        URI homeLoc = FSUtil.getDefault().getUserHomeURI();
        return new URIFactory(homeLoc).appendPath(ESCXNAT_CONFIGSUBDIR).toURI();
    }

    /**
     * Returns ~/.escxnat/settings.xcfg
     * 
     * @throws URISyntaxException
     */
    public static URI getSettingsFileURI() throws URISyntaxException
    {
        return new URIFactory(getConfigDirURI()).appendPath(ESCXNAT_CONFIGFILE).toURI();
    }

    public static URI getCaCertsLocation() throws URISyntaxException
    {
        return new URIFactory(getConfigDirURI()).appendPath("cacerts").toURI();
    }

    public static URI getDefaultXnatURI()
    {
        try
        {
            return new URI("https", null, "bioboost-virt.science.ru.nl", 443, "/escXnat", null, null);
        }
        catch (URISyntaxException e)
        {
            logger.logException(ClassLogger.ERROR, e, "Severe: Could not create Default XNAT Uri!" + e.getMessage());
            return null;
        }
    }

    public static Properties getInstallationProperties()
    {
        try
        {
            ResourceLoader rl = ResourceLoader.getDefault();
            return rl.loadProperties(rl.resolveUrl(ESCXNAT_PROPERTIES_FILE));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public static String getVersion()
    {
        Properties props = getInstallationProperties();
        if (props == null)
            return "?";

        String value = props.getProperty(PROP_ESCXNAT_VERSION);
        if (value != null)
            return value;
        return "?";
    }

    // ==========================================
    // instance
    // ==========================================

    private XnatToolFrame uploaderFrame;

    private XnatToolPanelController uploaderController;

    private XnatToolMainPanel uploaderPanel;

    public XnatToolMain(String[] args)
    {
        parseArgument(args);
    }

    public void start()
    {
        try
        {
            logger.infoPrintf("Starting XnatToolMain version:%s\n", getVersion());
            Properties props = getInstallationProperties();

            initGUI();
            initConfig();
            XnatToolConfig config = uploaderController.getToolConfig();

            if (config.getFirstRun())
            {
                config.setFirstRun(false);
                doFirstRun();
            }
        }
        catch (Exception e)
        {
            logger.fatal("FATAL: initGUI(): Caught Exception during startup:" + e);
            e.printStackTrace();
        }
    }

    private void doFirstRun()
    {
        this.uploaderController.doConfigDialog(true);
    }

    public void parseArgument(String[] args)
    {
        for (String arg : args)
        {
            if (arg.endsWith("-debug"))
            {
                ClassLogger.getRootLogger().setLevelToDebug();
            }
            else if (arg.equals("-info"))
            {
                ClassLogger.getRootLogger().setLevelToInfo();
            }
        }
    }

    protected void initGUI() throws Exception
    {
        // GUI Startup Sequence:
        uploaderFrame = new XnatToolFrame();
        // show:
        uploaderFrame.setVisible(true);
        // update:
        uploaderPanel = uploaderFrame.getUploaderPanel();
        uploaderController = uploaderPanel.getController();
    }

    /**
     * Load configuration and initialize the XnatTool.
     */
    protected void initConfig() throws URISyntaxException
    {
        XnatToolConfig conf = loadDefaultXnatConfig(true);

        if (conf != null)
        {
            uploaderController.updateToolConfig(conf);
        }
        else
        {
            logger.fatal("Could not load or create initial configuration file\n");
        }

        // Check custom certificates and update tool:
        try
        {
            // CertificateStore
            // certStore=CertificateStore.loadCertificateStore("/home/ptdeboer/.vletrc/cacerts",CertificateStore.DEFAULT_PASSPHRASE,true,true);
            // Load default certificate store from classpath
            CertificateStore certStore = CertificateStore.getDefault(false);

            if (certStore == null)
            {
                logger.infoPrintf("No default Certificate Store found.\n");
            }
            else
            {
                logger.infoPrintf("Using default CertificateStore:" + certStore.getKeyStoreLocation());
                uploaderController.getXnatTool().setCertificateStore(certStore);
            }
        }
        catch (CertificateStoreException e)
        {
            logger.errorPrintf("Failed to load CertificateStore:%s\n", e);
            logger.logException(ClassLogger.ERROR, e, "Exception:%s", e);
        }
    }

    // ========================================================================
    // Main Starter
    // ========================================================================

    /**
     * Load XnatTool Configuration or create new one if it doesn't exists and autoCreate==true.
     */
    public static XnatToolConfig loadDefaultXnatConfig(boolean autoCreate) throws URISyntaxException
    {
        URI homeLoc = FSUtil.getDefault().getUserHomeURI();

        FSUtil fsutil = FSUtil.getDefault();
        XnatToolConfig conf = null;

        URI confDir = XnatToolMain.getConfigDirURI();
        URI confPath = XnatToolMain.getSettingsFileURI();

        try
        {
            // load from: ~/.escxnat/settings.xcfg
            if ((fsutil.existsDir(confDir.getPath()) == false) && (autoCreate))
            {
                logger.infoPrintf("Creating new directory:%s\n", confDir);
                fsutil.mkdir(confDir.getPath());
            }

            if (fsutil.existsFile(confPath.getPath(), true))
            {
                conf = XnatToolConfig.loadConfig(confPath.getPath());
                logger.infoPrintf("Loaded ToolConfig from:%s\n>>>---\n%s\n>>>>---\n", confPath,
                        XmlUtil.prettyFormat(conf.toXML(), 4));
            }
            else
            {
                logger.infoPrintf("No config found, creating new config at:%s\n", confPath);
            }

        }
        catch (Exception e)
        {
            logger.logException(ClassLogger.WARN, e, "Failed to read config file from:%s\n", confPath);
        }

        if (conf != null)
        {
            return conf;
        }

        // ---------
        // Create New Configuration.
        // ---------

        conf = new XnatToolConfig();
        conf.xnatUser = GlobalProperties.getGlobalUserName();
        conf.setXnatURI(getDefaultXnatURI());
        // conf.setXnatURI(new URI("https", null,"xnatws.esciencetest.nl", 443,
        // "/escXnat", null, null));
        conf.setImageCacheDir(URIUtil.appendPath(homeLoc, "escxnat/cache"));
        conf.setDataSetsConfigDir(URIUtil.appendPath(homeLoc, "escxnat"));

        if (confPath != null)
        {
            try
            {
                XnatToolConfig.saveConfig(conf, confPath.getPath());
                logger.infoPrintf("Created new configuration to:%s\n", confPath);
            }
            catch (Exception e)
            {
                logger.warnPrintf("Couldn't save default configuration to:%s\n", confPath);
            }
        }

        return conf;
    }

    public static void main(String args[])
    {
        XnatToolMain upl = new XnatToolMain(args);
        upl.start();
    }

}

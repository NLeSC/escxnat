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

package nl.esciencecenter.xnattool.ui;

import java.awt.AWTEvent;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import nl.esciencecenter.medim.ImageDirEvent;
import nl.esciencecenter.medim.ImageDirScanner.FileFilterOptions;
import nl.esciencecenter.medim.ImageDirScanner.ScanMonitor;
import nl.esciencecenter.medim.ImageDirScannerListener;
import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.csv.CSVData;
import nl.esciencecenter.ptk.csv.CSVFrame;
import nl.esciencecenter.ptk.data.SecretHolder;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.jfx.util.FXFileChooser;
import nl.esciencecenter.ptk.jfx.util.FXFileChooser.ChooserType;
import nl.esciencecenter.ptk.jfx.util.FXWebJFrame;
import nl.esciencecenter.ptk.net.URIUtil;
import nl.esciencecenter.ptk.task.ActionTask;
import nl.esciencecenter.ptk.task.ITaskMonitor;
import nl.esciencecenter.ptk.ui.dialogs.ExceptionDialog;
import nl.esciencecenter.ptk.ui.panels.monitoring.TaskMonitorDialog;
import nl.esciencecenter.ptk.ui.widgets.NavigationBar.NavigationAction;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.WebException;
import nl.esciencecenter.ptk.web.WebException.Reason;
import nl.esciencecenter.xnattool.DataSetConfig;
import nl.esciencecenter.xnattool.UploadMonitor;
import nl.esciencecenter.xnattool.XnatTool;
import nl.esciencecenter.xnattool.XnatToolConfig;
import nl.esciencecenter.xnattool.XnatToolMain;

public class XnatToolPanelController implements ActionListener, ImageDirScannerListener, FocusListener
{
    static ClassLogger logger = ClassLogger.getLogger(XnatToolPanelController.class);

    static
    {
        // logger.setLevelToDebug();
    }

    public class ComboBoxListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            comboBoxAction(e);
        }
    }

    private XnatToolMainPanel mainPanel = null;

    private XnatTool xnatTool = null;

    private ComboBoxListener comboBoxListener;

    // test/mockup mode
    private boolean isMockup = false;

    // private UploadMonitor uploadMonitor;
    private ActionTask uploadTask;

    // private UploadMonitor uploadMonitor;
    private ActionTask scanTask;

    public XnatToolPanelController(XnatToolMainPanel xnatUploadPanel)
    {
        this.mainPanel = xnatUploadPanel;
        this.comboBoxListener = new ComboBoxListener();
    }

    public XnatTool getXnatTool()
    {
        return this.xnatTool;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        handleEvent(e);
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        handleEvent(e);
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        handleEvent(e);
    }

    public void handleFrameEvent(ActionEvent e)
    {
        handleEvent(e);
    }

    public void handleEvent(AWTEvent e)
    {
        debugPrintf("Event:%s\n", e);

        Object source = e.getSource();
        String actionCmd = null;
        boolean modCtrl = true;
        ActionEvent actionEvent = null;

        if (e instanceof ActionEvent)
        {
            actionEvent = (ActionEvent) e;
            actionCmd = actionEvent.getActionCommand();

            if ((actionEvent.getModifiers() & ActionEvent.CTRL_MASK) > 0)
            {
                modCtrl = true;
            }
        }

        NavigationAction navBarAction = NavigationAction.valueOfOrNull(actionCmd);

        try
        {
            if (navBarAction != null)
            {
                switch (navBarAction)
                {
                    case LOCATION_CHANGED:
                    {
                        String locationTxt = this.getLocationBarText();
                        updateLocation(locationTxt, true, true);
                        break;
                    }
                    default:
                        break;
                }
            }

            // -------------
            // Focus Lost Events!
            // -------------

            if (source.equals(mainPanel.getUserTF()))
            {
                updateUser(mainPanel.getUserText());
                return;
            }
            else if (source.equals(mainPanel.getDefaultOwnerIdTF()))
            {
                updateDefaultOwnerID(mainPanel.getDefaultOwnerIdTF().getText());
                return;
            }

            // ---
            // check focus change event combined with Action Commands:
            // ---

            UIAction xcmd = null;

            try
            {
                xcmd = UIAction.valueOf(actionCmd);
            }
            catch (Throwable ex)
            {
                ; //
            }

            // check actual actions commands:
            if (xcmd != null)
            {
                switch (xcmd)
                {
                    case CB_PROJECT_CHANGED:
                    {
                        String proj = this.mainPanel.getProjectSelBox().getSelectedItemString();
                        updateProject(proj);
                        break;
                    }
                    case ACTION_LOGIN:
                    {
                        doLogin(true);
                        break;
                    }
                    case ACTION_UPLOAD:
                    {
                        doUpload();
                        break;
                    }
                    case ACTION_SCAN:
                    {
                        doScan();
                        break;
                    }
                    case ACTION_VIEW_SCANSETINF:
                    {
                        doViewScanInfo();
                        break;
                    }
                    case ACTION_EXIT:
                    {
                        doExit();
                        break;
                    }
                    case MAIN_MENU_HELP:
                    {
                        doHelp();
                        break;
                    }
                    case BUT_NEW_DATASET:
                    {
                        doCreateNewDataSetConfig();
                        break;
                    }
                    case CB_DATASET_CHANGED:
                    {
                        String name = this.mainPanel.getDataSetSelectionCB().getSelectedItemString();
                        if ((name != null) && (!name.equals(xnatTool.getDataSetConfigName())))
                        {
                            doSwitchDataSetConfig(name);
                            logger.infoPrintf("Switched to DataSet:%s\n", name);
                        }

                        break;
                    }
                    case BUT_CREATE_INIT_KEY:
                    {
                        doCreateNewDatasetKey(modCtrl);

                        break;
                    }
                    case BUT_AUTHENTICATE_KEY:
                    {
                        doAuthenticateDataSetKey(true, true);
                    }
                    case DATASET_TYPE_CHANGED:
                    {
                        switchDataSetTypeFromUI();
                        break;
                    }
                    case DATASET_FILTER_CHANGED:
                    case DATASET_FILE_FILTER_CHANGED:
                    case DATASET_MAGIC_CB_CHANGED:
                    {
                        updateDataSetFilterOptionsFromUI();
                        break;
                    }
                    case SCANSUBTYPE_OPTIONS_CHANGED:
                    {
                        updateScanSubTypeOptionFromUI();
                        break;
                    }
                    case FIELD_SOURCEDIR_CHANGED:
                    {
                        this.updateImageSourceDirToTool();
                        break;
                    }
                    // case CREATE_CONFIGSDIR:
                    // {
                    // // re-update
                    // updateDataSetsConfigsDirToTool(true);
                    //
                    // break;

                    case FIELD_PASSWORD_CHANGED:
                    {
                        doAuthenticateDataSetKey(false, true);
                        break;
                    }
                    case FIELD_OWNERID_CHANGED:
                    {
                        // doCheckOwnerID();
                        break;
                    }
                    case UPLOAD_CSV_TO_PROJECT:
                    {
                        doUploadCSV();
                        break;
                    }
                    case MAIN_CONFIGURATION:
                    {
                        doConfigDialog(false);
                        break;
                    }
                    case MAIN_DEBUGLEVEL_TO_DEBUG:
                    {
                        ClassLogger.getRootLogger().setLevelToDebug();
                        break;
                    }
                    case MAIN_DEBUGLEVEL_TO_INFO:
                    {
                        ClassLogger.getRootLogger().setLevelToInfo();
                        break;
                    }
                    case MAIN_DEBUGLEVEL_TO_ERROR:
                    {
                        ClassLogger.getRootLogger().setLevelToError();
                    }
                    case MAIN_LOCATION_SET_DEFAULT:
                    {
                        this.doChangeLocationToDefault();
                    }
                    default:
                    {
                        logger.warnPrintf("Unsupported Panel Action:%s\n", xcmd);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            this.handle(ex.getMessage(), ex);
        }
    }

    public void doHelp()
    {
        FXWebJFrame.launch("https://github.com/NLeSC/escxnat/wiki/escxnat-help");
    }

    protected void doViewScanInfo()
    {
        try
        {
            CSVData data = this.xnatTool.getMetaData();

            CSVFrame jframe = new CSVFrame(data);

            jframe.setSize(new Dimension(800, 600));
            jframe.setVisible(true);
        }
        catch (Exception e)
        {
            handle("Couldn't get MetaData", e);
        }
    }

    protected void updateScanSubTypeOptionFromUI()
    {
        ImageTypes.ScanSubType subType = mainPanel.getScanSubTypeOption();

        logger.infoPrintf("updateScanSubTypeOption:%s\n", subType);
        // only set NUC Scan explicitly, default to RAW for other options.
        try
        {
            switch (subType)
            {
                case NUC_SCAN:
                {
                    xnatTool.setDataSetScanSubType(subType);
                    break;
                }
                case RAW_SCAN:
                {
                    xnatTool.setDataSetScanSubType(subType);
                    break;
                }
                default:
                {
                    xnatTool.setDataSetScanSubType(ImageTypes.ScanSubType.RAW_SCAN);
                    break;
                }
            }
        }
        catch (Exception e)
        {
            this.handle("Failed to update ScanSubType to:" + subType, e);
        }
    }

    protected void switchDataSetTypeFromUI() throws IOException
    {
        ImageTypes.DataSetType uiDataSetType = this.mainPanel.getDataSetType();
        switchDataSetFilterFields(uiDataSetType);
    }

    protected void switchDataSetFilterFields(ImageTypes.DataSetType newDataSetType) throws IOException
    {
        boolean checkMagick = true;

        this.mainPanel.setDataSetType(newDataSetType);

        switch (newDataSetType)
        {
            case DICOM_SCANSET:
            {
                this.mainPanel.fileFilterSB.setValues(XnatToolMainPanel.dicomFileFilterOptions);
                this.mainPanel.filterModalitySB.setEnabled(true);
                this.xnatTool.getFilterOptions().setExtensions(FileFilterOptions.default_dicom_extensions);
                this.xnatTool.enableDicom();
                checkMagick = true;
                this.mainPanel.checkMagicChBx.setSelected(checkMagick);
                this.mainPanel.checkMagicChBx.setEnabled(true);
                this.mainPanel.scanSubTypeSB.setEnabled(false);
                break;
            }
            case NIFTI_SCANSET:
            {
                this.mainPanel.fileFilterSB.setValues(XnatToolMainPanel.niftiFileFilterOptions);
                this.mainPanel.filterModalitySB.setEnabled(false);
                this.xnatTool.getFilterOptions().setExtensions(FileFilterOptions.default_nifti_extensions);
                this.xnatTool.enableNifti();
                checkMagick = false;
                this.mainPanel.checkMagicChBx.setSelected(checkMagick);
                this.mainPanel.checkMagicChBx.setEnabled(false);
                this.mainPanel.scanSubTypeSB.setEnabled(false);
                break;
            }
            case NIFTI_ATLASSET:
            {
                this.mainPanel.fileFilterSB.setValues(XnatToolMainPanel.niftiFileFilterOptions);
                this.mainPanel.filterModalitySB.setEnabled(false);
                this.xnatTool.getFilterOptions().setExtensions(FileFilterOptions.default_nifti_extensions);
                this.xnatTool.enableAtlas();
                checkMagick = false;
                this.mainPanel.checkMagicChBx.setSelected(checkMagick);
                this.mainPanel.checkMagicChBx.setEnabled(false);
                this.mainPanel.scanSubTypeSB.setEnabled(true);
                break;
            }
            default:
            {
                logger.errorPrintf("updateDataSetFileFilters(): NOT recognized:%s\n", newDataSetType);
                // disable all file filters and set to defaults(!)
                this.xnatTool.getFilterOptions().setCheckFileMagic(false);
                this.xnatTool.getFilterOptions().setExtensions(null);
                this.mainPanel.filterModalitySB.setEnabled(false);
                this.xnatTool.enableDicom();
                checkMagick = true;
                this.mainPanel.checkMagicChBx.setSelected(checkMagick);
                this.mainPanel.checkMagicChBx.setEnabled(true);
                checkMagick = false;
                break;
            }
        }
    }

    protected void updateDataSetFilterOptionsFromUI() throws IOException
    {
        ImageTypes.DataSetType uiDataSetType = this.mainPanel.getDataSetType();
        ImageTypes.DataSetType confDataSetType = xnatTool.getCurrentDataSetConfig().getDataSetType();

        String fileFilter = this.mainPanel.fileFilterSB.getSelectedItemString();
        String filterModality = this.mainPanel.getFilterModalityType();
        boolean checkMagick = this.mainPanel.checkMagicChBx.isSelected();

        logger.debugPrintf("[DataSet Type and Filters]\n");
        logger.debugPrintf(" - (new)dataSetType = %s\n", uiDataSetType);
        logger.debugPrintf(" - fileFilter       = %s\n", fileFilter);
        logger.debugPrintf(" - filterModatily   = %s\n", filterModality);
        logger.debugPrintf(" - checkMagick      = %s\n", checkMagick);

        switch (uiDataSetType)
        {
            case DICOM_SCANSET:
            {
                break;
            }
            case NIFTI_SCANSET:
            {
                break;
            }
            case NIFTI_ATLASSET:
            {
                break;
            }
            default:
            {
                logger.errorPrintf("DataSetType NOT recognized:" + uiDataSetType);
                break;
            }
        }

        // ---------------------
        // Update Filter Options
        // ---------------------

        if (StringUtil.compare(fileFilter, XnatToolMainPanel.OPT_FILE_ALL_EXTS) == 0)
        {
            this.xnatTool.getFilterOptions().setExtensions(null);
        }
        else if (StringUtil.compare(fileFilter, XnatToolMainPanel.OPT_DICOM_EXTS) == 0)
        {
            this.xnatTool.getFilterOptions().setExtensions(FileFilterOptions.default_dicom_extensions);
        }
        else if (StringUtil.compare(fileFilter, XnatToolMainPanel.OPT_NIFTI_EXTS) == 0)
        {
            this.xnatTool.getFilterOptions().setExtensions(FileFilterOptions.default_nifti_extensions);
        }

        if (StringUtil.compare(filterModality, XnatToolMainPanel.OPT_DICOM_MR) == 0)
        {
            // Explicit Dicom MR
            this.xnatTool.getFilterOptions().setModalityFilter("MR");
        }
        else
        {
            // Explicit delete Dicom MR
            this.xnatTool.getFilterOptions().deleteModalityFilter();
        }

        // update check magick:
        this.xnatTool.getFilterOptions().setCheckFileMagic(checkMagick);

    }

    protected boolean doUploadCSV() throws IOException
    {
        DataSetConfig setConf = this.getXnatTool().getCurrentDataSetConfig();
        // setConf.getMetaDataFile();

        if (doLogin(false) == false)
        {
            return false;
        }

        if (doAuthenticateDataSetKey(true, false) == false)
        {
            return false;
        }

        URI file = FXFileChooser.staticStartFileChooser(ChooserType.OPEN_FILE, this.getXnatTool().getToolConfig().getDataSetsConfigDir()
                .getPath());

        if (file == null)
        {
            return false;
        }

        final String projectId = this.getSelectedProjectID();

        if (StringUtil.isEmpty(projectId))
        {
            this.showError("No Project Selected. Please select project first.");
            return false;
        }

        try
        {
            final CSVData reader = new CSVData();
            reader.setFieldSeparators(new String[]
            {
                    ",", ";"
            });
            reader.readFile(file);

            final UploadMonitor uploadMonitor = new UploadMonitor(this);

            this.uploadTask = new ActionTask(null, "Uploading CSV File to:" + this.getLocation() + " {" + projectId + "} ",
                    uploadMonitor)
            {
                @Override
                protected void doTask() throws Exception
                {
                    try
                    {
                        xnatTool.doUploadCSV(projectId, reader, true, true, uploadMonitor);
                    }
                    catch (Throwable t)
                    {
                        handle(t.getMessage(), t);
                    }
                    finally
                    {
                        exitBusy();
                    }
                }

                @Override
                protected void stopTask() throws Exception
                {

                }

            };

            this.uploadTask.startTask();

            // show monitor. Dialog also control the task!
            UploadMonitorDialog.showUploadMonitorDialog(getMainPanelJFrame(), uploadTask, 0);
        }
        catch (Exception e)
        {
            this.handle("Failed to upload CSV File:" + file, e);
        }

        return true;
    }

    protected boolean doUploadReconstruction(String reconTypeName) throws IOException
    {
        DataSetConfig setConf = this.getXnatTool().getCurrentDataSetConfig();
        // setConf.getMetaDataFile();

        if (doLogin(false) == false)
        {
            return false;
        }

        if (doAuthenticateDataSetKey(true, false) == false)
        {
            return false;
        }

        URI file = FXFileChooser.staticStartFileChooser(ChooserType.OPEN_FILE, "file:///");

        if (file == null)
        {
            return false;
        }

        final String projectId = this.getSelectedProjectID();

        if (StringUtil.isEmpty(projectId))
        {
            this.showError("No Project Selected. Please select project first.");
            return false;
        }

        return false;
    }

    public void updateSourceIdFields(String sourceId)
    {
        this.mainPanel.setDataSetSourceID(sourceId);
        this.mainPanel.getDatasetConfigPanel().setSourceId(sourceId);
    }

    protected boolean doAuthenticateDataSetKey(boolean askInteractive, boolean authenticateIfalreadyAuthenticated)
    {
        if (getDataSetConfig() == null)
        {
            this.showError("No Valid DataSet created or selected!");
            return false;
        }

        String sourceId = this.getDataSetConfig().getSourceId();

        String dataSetName = this.getDataSetConfig().getDataSetName();

        Secret passPhrase;

        if (this.xnatTool.isDataSetEncryptionKeyInitialized())
        {
            if (authenticateIfalreadyAuthenticated == false)
            {
                return true;
            }
            // re-authenticate;
        }

        if (askInteractive)
        {
            SecretHolder secretH = new SecretHolder();
            uiAskField("Enter Dataset Passphrase.", "Enter DataSet Passphrase for DataSet:" + dataSetName,
                    secretH);

            if (secretH.value == null || secretH.value.isEmpty())
            {
                logger.info("doAuthenticateDataSetKey(): Cancelled");
                return false;
            }

            passPhrase = secretH.value;
            this.updatePassphrase(passPhrase);
        }
        else
        {
            passPhrase = this.mainPanel.getPassphrase();
        }

        return doAuthenticateDataSetKeys(sourceId, passPhrase);
    }

    protected boolean doAuthenticateDataSetKeys(String sourceId, Secret passPhrase)
    {
        if (this.xnatTool.hasCurrentDataSetconfig() == false)
        {
            this.showError("No DataSet loaded or configured yet.\nPlease create a DataSet");
            return false;
        }

        boolean authenticated = false;

        try
        {
            xnatTool.authenticateEncryptionKeys(sourceId, passPhrase, true);
            authenticated = true;

        }
        catch (Exception e)
        {
            updateKeyStatusText("Error!");
            logger.infoPrintf("AuthenticationError:%s\n", e);
            this.handle("Failed to authenticate DataSet Keys", e);
        }

        updateKeyStatusFieldsFromConfigFromTool();
        updateEnableScanFields(authenticated);

        return authenticated;
    }

    public void updateEnableScanFields(boolean authenticated)
    {
        this.mainPanel.setEnableScanButton(authenticated);
        this.mainPanel.setEnableImageSourceTF(authenticated);
    }

    public void doDeleteDatasetKeys(boolean updateFields) throws Exception
    {
        try
        {
            xnatTool.deleteEncryptionKeys();
            if (updateFields)
            {
                updateKeyStatusFieldsFromConfigFromTool();
            }
        }
        catch (Exception e)
        {
            updateKeyStatusText("Error!");
            logger.infoPrintf("AuthenticationError:%s\n", e);
            throw e;
        }
    }

    protected boolean doCreateNewDataSetConfig()
    {
        URI dataSetDir = xnatTool.getToolConfig().getDataSetsConfigDir();

        if (FSUtil.getDefault().existsDir(dataSetDir.getPath()) == false)
        {
            showError("DataSet Configuration directory doesn't exists.\n"
                    + "Please create directory. See Menu:Settings-> Tool Configuration.");
            return false;
        }

        try
        {
            StringHolder valueH = new StringHolder("DataSet1");
            uiAskField("Create New Data Set.", "Enter name of New DataSet configuration", valueH);

            if (valueH.value == null || valueH.value.equals(""))
            {
                logger.info("Cancelled creation of new DataSetConfig. No DataSet name given.\n");
                return false;
            }

            String newName = valueH.value;

            if (xnatTool.getDataSetConfig(newName) != null)
            {
                this.showError("DataSet Configuration already exists:" + valueH.value);
                return false;
            }

            // String sourceId=mainPanel.getDefaultSourceID();
            String sourceId = xnatTool.getDefaultSourceId();
            if (sourceId == null || sourceId.equals(""))
            {
                valueH.value = null;

                valueH = new StringHolder(xnatTool.getXnatUsername());
                uiAskField("Enter new Owner ID", "Enter Owner ID to be associated with this DataSet", valueH);

                if (valueH.value == null || valueH.value.equals(""))
                {
                    logger.info("Cancelled creation of new DataSetConfig. No OwnerId given.\n");
                    return false;
                }
                sourceId = valueH.value;
                xnatTool.persistantSetDefaultSourceID(sourceId);
            }

            DataSetConfig config = this.xnatTool.createNewDataSetConfig(sourceId, newName);
            doSwitchDataSetConfig(newName);

            return true;

        }
        catch (Exception e)
        {
            this.handle("Couldn't create new DataSet Configuration.", e);
        }

        return false;
    }

    protected boolean doSwitchDataSetConfig(String dataSetName)
    {
        if (dataSetName == null)
        {
            mainPanel.setSourceDirText("");
            setDataSetOwnerID("");
            // mainPanel.setDataSetNames(new ArrayList<String>(), "");
            this.updateKeyStatusText("<No Data Set>");
            return false;
        }

        DataSetConfig setConfig = null;

        try
        {
            setConfig = xnatTool.switchToDataSetConfig(dataSetName);
        }
        catch (Exception e)
        {
            this.handle("Couldn't load/switch to DataSet configuration:" + dataSetName, e);
        }

        // Update DataSet Names to current DataSet:
        mainPanel.setDataSetNames(xnatTool.getDataSetNames(), dataSetName);

        // Clear possible cached decrypted encryptionkey:
        setConfig.setEncryptionKey(null);

        // Update Key Status
        updateKeyStatusFieldsFromConfigFromTool();
        // New DataSet, disable previous scan.
        updateEnableScanFields(false);

        // Update Actual DataSetConfig fields
        updateDataSetConfigToUI(setConfig);

        return true;
    }

    protected void updateDataSetConfigToUI(DataSetConfig setConfig)
    {
        setDataSetOwnerID(setConfig.getSourceId());
        String dataSetName = setConfig.getDataSetName();

        // Main Panel:
        mainPanel.setSourceDirText(setConfig.getImageSourceDir().getPath());
        mainPanel.setDataSetSourceID(setConfig.getSourceId());
        mainPanel.setDataSetName(dataSetName, true);
        mainPanel.setEnableNewDataSetConfig(true); // have valid DataSet Config.
        mainPanel.setEnablePassphraseTF(true);
        // Clear Passphrase!
        mainPanel.setPassphrase(null);

        // Filters and Scan Options:
        try
        {
            this.switchDataSetFilterFields(setConfig.getDataSetType());
        }
        catch (IOException e)
        {
            this.handle("Couldn't load/switch to DataSet configuration:" + dataSetName, e);
        }

        // mainPanel.checkMagicChBx.setEnabled(setConfig.getCheckMagick());
        mainPanel.setScanSubType(setConfig.getScanSubType());

        // DataSet SettingPanel (other tab)
        updateDataSetSettingsPanel(setConfig);
    }

    protected void updateDataSetSettingsPanel(DataSetConfig setConfig)
    {
        DatasetSettingsPanel settingsPanel = mainPanel.getDatasetConfigPanel();
        settingsPanel.setDataSetName(setConfig.getDataSetName());
        settingsPanel.setSubjectKeyType(setConfig.getSubjectKeyType());
        settingsPanel.setSessionKeyType(setConfig.getSessionKeyType());
        settingsPanel.setScanKeyType(setConfig.getScanKeyType());
        settingsPanel.sourceIdTF.setText(setConfig.getSourceId());

        // settingsPanel.ownerIdHashTF.setText(StringUtil.toHexString(setConfig.getEncryptedSourceID()));
        // settingsPanel.keyHashTF.setText(StringUtil.toHexString(setConfig.getEncryptedKey()));

        if (getAdvancedMode())
        {
            settingsPanel.createKeyBut.setEnabled(true);
        }
        else
        {
            settingsPanel.createKeyBut.setEnabled(setConfig.getEncryptedKey() == null);
        }
    }

    protected int uiAskField(String title, String message, StringHolder value)
    {
        JTextField tf = new JTextField(value.value);
        Object[] inputFields =
        {
                message, tf
        };
        int result = this.askInput(title, inputFields, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
        {
            value.value = tf.getText();
        }
        return result;
    }

    protected int uiAskField(String title, String message, SecretHolder value)
    {
        JPasswordField tf = new JPasswordField();
        Object[] inputFields =
        {
                message, tf
        };
        int result = this.askInput(title, inputFields, JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION)
        {
            value.value = new Secret(tf.getPassword());
        }
        return result;
    }

    public int askInput(String title, Object[] inputFields, int optionPaneOption)
    {
        return JOptionPane.showConfirmDialog(mainPanel, inputFields, title, optionPaneOption);
    }

    public boolean askOkCancel(String title, String message)
    {
        int result = JOptionPane.showConfirmDialog(mainPanel, message, title, JOptionPane.OK_CANCEL_OPTION);
        return (result == JOptionPane.OK_OPTION);
    }

    protected boolean doUpload()
    {
        // checks

        if (this.uploadTask != null)
        {
            if (this.uploadTask.isAlive())
            {
                this.showError("Uploading task still active!:" + this.uploadTask.getTaskName());
                return false;
            }
        }

        if ((XnatToolMain.demoUpload == false) && (this.xnatTool.isXnatAuthenticated() == false))
        {
            this.showError("Not logged in!\nPlease supply your XNAT credentials first, before uploading to XNAT");
            return false;
        }

        enterBusy();

        try
        {
            updateDataSetConfigOptionsToTool();
        }
        catch (Exception e)
        {
            this.handle("Incorrect Settings", e);
            return false;
        }

        final String projectId = this.getSelectedProjectID();
        // final String subjectLabel=this.getSelectedSubjectID();
        // final String sessionLabel=this.getSelectedSessionID();
        final UploadMonitor uploadMonitor = new UploadMonitor(this);

        this.uploadTask = new ActionTask(null, "Uploading to:" + this.getLocation() + " {" + projectId + "} ",
                uploadMonitor)
        {
            @Override
            protected void doTask() throws Exception
            {
                try
                {
                    // this.uploadMonitor=new UploadMonitor(this);
                    xnatTool.doUploadTo(projectId, true, uploadMonitor);
                }
                catch (Throwable t)
                {
                    handle("Upload failed to project:" + projectId, t);
                }
                finally
                {
                    exitBusy();
                }
            }

            @Override
            protected void stopTask() throws Exception
            {

            }
        };

        this.uploadTask.startTask();

        // show monitor. Dialog also control the task!
        UploadMonitorDialog.showUploadMonitorDialog(getMainPanelJFrame(), uploadTask, 0);

        return true;
    }

    protected void updateDataSetConfigOptionsToTool() throws Exception
    {
        // DicomProcessingProfile options =
        // this.uploaderPanel.getSettingsPanel().getProcessingOptions();
        // this.xnatTool.setProcessingOptions(options);
    }

    public ITaskMonitor getUploadTaskMonitor()
    {
        if (this.uploadTask != null)
            return this.uploadTask.getTaskMonitor();

        return null;
    }

    protected JFrame getMainPanelJFrame()
    {
        return this.mainPanel.getJFrame();
    }

    public String getLocationBarText()
    {
        return mainPanel.getLocationBar().getLocationText();
    }

    public boolean doLogin(boolean loginIfAlreadyAuthenticated)
    {
        if (xnatTool.isXnatAuthenticated())
        {
            if (loginIfAlreadyAuthenticated == false)
            {
                return true;
            }
            // re-login anyway;
        }

        this.mainPanel.updateMouseBusy(true);

        // micro sleep:
        try
        {
            Thread.sleep(1);
        }
        catch (InterruptedException e1)
        {
            //
        }

        boolean authenticated = false;

        try
        {
            // Copy user info into LocationBar+ used URI.
            updateUser(mainPanel.getUserText());

            URI loc = this.getLocation();
            // update tool with actual location:
            xnatTool.persistantUpdateXnatLocation(loc);
            authenticated = xnatTool.authenticateXnat();

            if (authenticated)
            {
                postLogin();
            }

        }
        catch (Exception e)
        {
            // provide descriptive feedback:
            if (e instanceof WebException)
            {
                Reason reason = ((WebException) e).getReason();

                switch (reason)
                {
                    case UNAUTHORIZED:
                    case FORBIDDEN:
                    {
                        showError("Invalid password or user is not authenticated.");
                        break;
                    }
                    case RESOURCE_NOT_FOUND:
                    case UNKNOWN_HOST:
                    case CONNECTION_EXCEPTION:
                    case CONNECTION_TIME_OUT:
                    case IOEXCEPTION:
                    {
                        showError("" + reason + ": Invalid url or remote server is down.");
                        break;
                    }
                    case URI_EXCEPTION:
                    {
                        showError("Syntax Error. Invalid or mispelled URI.\n" + e.getMessage());
                        break;
                    }
                    case HTTP_ERROR:
                    default:
                    {
                        showError("Unkown web exception occurred.\nReason=" + reason + "\n" + e.getMessage());
                        break;
                    }
                }
            }
            else
            {
                this.handle("Couldn't login to:" + this.getLocation(), e);
            }
        }

        this.mainPanel.updateMouseBusy(false);
        return authenticated;
    }

    private void checkUsername(String userText) throws Exception
    {
        if (StringUtil.hasWhiteSpace(userText))
        {
            throw new Exception("Username contains spaces:'" + userText + "'");
        }
    }

    public void doExit()
    {
        JFrame frame = this.mainPanel.getJFrame();

        disposeAll();

        if (frame != null)
        {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    protected void disposeAll()
    {
        if (this.xnatTool != null)
        {
            xnatTool.dispose();
            xnatTool = null;
        }
    }

    public void comboBoxAction(ActionEvent e)
    {
        String cmd = e.getActionCommand();
        actionPerformed(e);
    }

    public void updateUser(String userText) throws Exception
    {
        this.checkUsername(userText);
        URI vri = getLocation();
        URI newVri = URIUtil.replaceUserinfo(vri, userText);
        this.updateLocation(newVri.toString(), true, false);
    }

    protected void updateDefaultOwnerID(String ownerId)
    {
        boolean ok = false;

        if (StringUtil.hasWhiteSpace(ownerId))
        {
            this.showError("Owner ID may not contain spaces:'" + ownerId + "'");
            ok = false;
        }
        else
        {
            ok = true;
        }

        if (ok == false)
        {
            // revert:
            String prevId = this.xnatTool.getToolConfig().getDefaultSourceId();
            // filter previous whitespace(!)
            if (prevId != null)
            {
                prevId = prevId.replaceAll("[ \t\n]", "");
            }

            this.mainPanel.getDefaultOwnerIdTF().setText(prevId);
            return; // abort action.
        }
        else
        {
            xnatTool.getToolConfig().setDefaultSourceId(ownerId);
        }
    }

    public URI getLocation()
    {
        try
        {
            return new URI(this.getLocationBarText());
        }
        catch (Exception e)
        {
            handle(e.getMessage(), e);
        }

        return null;
    }

    public void updateLocationText(String locationTxt, boolean addToHistory)
    {
        this.mainPanel.getLocationBar().updateLocation(locationTxt, addToHistory);
    }

    protected void doChangeLocationToDefault()
    {
        URI loc = XnatToolMain.getDefaultXnatURI();
        if (this.xnatTool.isXnatAuthenticated())
        {
            // logout/de-authenticate ?
        }

        this.updateLocation(loc.toString(), false, false);
    }

    public void updateLocation(String locationTxt, boolean addToHistory, boolean loginAndUpdate)
    {
        try
        {
            this.updateLocationText(locationTxt, addToHistory);

            URI uri = new URI(locationTxt);
            xnatTool.persistantUpdateXnatLocation(uri);
            this.mainPanel.setUser(xnatTool.getXnatUsername());

            if (loginAndUpdate)
            {
                boolean authenticated = xnatTool.authenticateXnat();

                if (authenticated)
                {
                    postLogin();
                }
            }
        }
        catch (WebException e)
        {
            switch (e.getReason())
            {
                case RESOURCE_NOT_FOUND:
                {
                    this.showError("Invalid URI or Server is down:" + this.getLocationBarText() + "\n --- Message ---\n" + e.getMessage());
                }
                default:
                {
                    handle(e.getMessage(), e);
                }
            }
        }
        catch (URISyntaxException e)
        {
            handle("Syntax Error: Invalid location:" + locationTxt, e);
        }
        catch (Exception e)
        {
            handle("Initialization Error updating location:" + locationTxt, e);
        }
    }

    protected boolean initXnatTool(XnatToolConfig xconfig, boolean authenticate)
    {
        try
        {
            if (this.xnatTool == null)
            {
                xnatTool = new XnatTool(xconfig, true);
            }
            else
            {
                xnatTool.updateConfig(xconfig);
            }

            if (authenticate)
                return xnatTool.authenticateXnat();
        }
        catch (Exception e)
        {
            handle("Can't connect to new location:" + xconfig.getXnatURI(), e);
        }

        addDefaultMRFiltersTo(xnatTool);

        return false;
    }

    protected void addDefaultMRFiltersTo(XnatTool xnatTool2)
    {
        // Spin Echo:
        // Long TR 1500+ ms
        // Short TR 250-700 ms
        // Long TE 60+ ms
        // Short TE 10-25 ms

        // Weighting TR TE flip
        // T1 short short 90°
        // T2 long long 90°

        // proton-density (PD) long short 90°

        // xnatTool.addTRTEFilter(200, 700,10,25);

        // Only MR is used !
        xnatTool.getFilterOptions().setModalityFilter("MR");
        xnatTool.getFilterOptions().setCheckFileMagic(true);
        // Need to specify this depending on the SequenceSeries Type!
        // currently allow large range to filter out error values.
        xnatTool.addTRTEFilter(0, 20000, 0, 20000);
    }

    protected void handle(String errorTxt, Throwable e)
    {
        errorPrintf("***Error:%s\n", errorTxt);
        errorPrintf("-----------\n%s\n----------------\n", e.getMessage());
        e.printStackTrace();

        ExceptionDialog.show(mainPanel, errorTxt, e, false);

    }

    private void debugPrintf(String format, Object... args)
    {
        logger.debugPrintf(format, args);
    }

    private void errorPrintf(String format, Object... args)
    {
        logger.errorPrintf(format, args);
    }

    public ActionListener getComboBoxListener()
    {
        return this.comboBoxListener;
    }

    public void updateProjects()
    {
        String[] names = null;

        try
        {
            names = this.xnatTool.getProjectIDs(true);
        }
        catch (Exception e)
        {
            if (isMockup)
            {
                names = new String[]
                {
                        "Project1", "Project2", "Project3"
                };
            }

            handle("Couldn't get ProjectNames for service:" + this.getLocation(), e);
        }

        if (names != null)
            this.mainPanel.getProjectSelBox().setValues(names);
    }

    public void postLogin()
    {
        // refresh projects selection list.
        this.updateProjects();
        // at least enable the 'New' button, DataSets might not yet be created.
        this.mainPanel.setEnableNewDataSetConfig(true);
    }

    public String getSelectedProjectID()
    {
        return this.mainPanel.getProjectSelBox().getSelectedItemString();
    }

    public void updateProject(String projectId)
    {
        //
    }

    public void setImageSourceLoc(URI loc)
    {
        this.mainPanel.setSourceDirText(loc.getPath());
    }

    public void setDataSetOwnerID(String id)
    {
        this.mainPanel.setDataSetSourceID(id);
    }

    public void updateToolConfig(XnatToolConfig conf)
    {
        try
        {
            this.initXnatTool(conf, false);
        }
        catch (Exception e)
        {
            handle("Failed to update configuration:\n" + conf, e);
        }

        updateToolConfigFields(conf);

        // Save DataSet Config ?
        DataSetConfig dataSetConf = this.xnatTool.getCurrentDataSetConfig();

        if (dataSetConf == null)
        {
            logger.warnPrintf("No Default DataSetConfig loaded from config dir:%s!\n", conf.getDataSetsConfigDir());
            this.doSwitchDataSetConfig(null); // clear fields.
        }
        else
        {
            // updater is (or should be) null proof: null will clear fields.
            this.doSwitchDataSetConfig(dataSetConf.getDataSetName());
        }
    }

    public XnatToolConfig getToolConfig()
    {
        if (xnatTool == null)
            return null;

        return this.xnatTool.getToolConfig();
    }

    protected void updateToolConfigFields(XnatToolConfig conf)
    {
        URI dataSetsConfDir = conf.getDataSetsConfigDir();

        // setDataSetConfigDirectory(dataSetsConfDir);
        // this.setImageCacheLoc(conf.getImageCacheDir());
        this.updateLocation(conf.getXnatURIString(), true, false);
        this.updateDefaultOwnerID(conf.getDefaultSourceId());

        boolean dataSetDirExists = FSUtil.getDefault().existsDir(dataSetsConfDir.getPath());

        this.mainPanel.getDefaultOwnerIdTF().setText(conf.getDefaultSourceId());

        // if (dataSetDirExists==false)
        // {
        // this.showError("Warning: Current DataSet Configuration directory doesn't exist:\n"+dataSetsConfDir);
        // }
        // this.mainPanel.dataDirCreateBut.setEnabled(dataSetDirExists==false);
    }

    private void updateKeyStatusText(String keyStatus)
    {
        mainPanel.keyStatusTF.setText(keyStatus);
        mainPanel.getDatasetConfigPanel().keyStatusTF.setText(keyStatus);
    }

    /**
     * Update Panel fields and buttons after Key Initialization/Creation.
     */
    protected void updateKeyStatusFieldsFromConfigFromTool()
    {
        DataSetConfig datasetConfig = xnatTool.getCurrentDataSetConfig();

        String keyStatus = "<Unknown>";
        boolean hasExistingKey = false;
        boolean hasAuthenticatedKey = false;

        if (datasetConfig == null)
        {
            keyStatus = "<No DataSet Ceated yet>";
            hasExistingKey = false;
            hasAuthenticatedKey = false;
        }
        else
        {
            byte encryptedKey[] = datasetConfig.getEncryptedKey();
            if ((encryptedKey == null) || (encryptedKey.length <= 0))
            {
                keyStatus = "No Key Created Yet!";
                hasExistingKey = false;
                hasAuthenticatedKey = false;
            }
            else
            {
                hasExistingKey = true;
                // key status:
                byte bytes[] = datasetConfig.getEncryptionKey();

                if ((bytes == null) || (bytes.length <= 0))
                {
                    keyStatus = "Not Authenticated!";
                    hasAuthenticatedKey = false;
                }
                else
                {
                    keyStatus = "Authenticated";
                    hasAuthenticatedKey = true;
                }
            }
        }

        updateKeyStatusText(keyStatus);

        // update main panel fields+buttons;
        mainPanel.setEnableCreateKeyKeyButton(hasExistingKey == false);
        mainPanel.setEnableAuthenticateKeyButton((hasExistingKey) && (hasAuthenticatedKey == false));
        mainPanel.setEnablePassphraseTF(hasExistingKey);
        // [DataSet Settings Panel]
        updateDataSetSettingsPanel(datasetConfig);

    }

    public boolean getAdvancedMode()
    {
        return true;
    }

    public boolean doScan()
    {
        if (this.xnatTool == null)
        {
            showError("Session not initialized");
            return false;
        }

        if (this.scanTask != null)
        {
            if (this.scanTask.isAlive())
            {
                this.showError("Scan Task still active!:" + this.scanTask.getTaskName());
                return false;
            }
        }

        String dir = this.mainPanel.getSourceDirText();
        final java.net.URI sourceDirUri;

        try
        {
            // resolve!
            sourceDirUri = FSUtil.getDefault().resolvePathURI(dir);
        }
        catch (IOException e1)
        {
            handle("Invalid location:" + dir, e1);
            return false;
        }

        try
        {
            // updateDataSetConfigOptionsToTool();
            this.updateImageSourceDirToTool();
        }
        catch (Exception e)
        {
            this.handle("Incorrect Settings", e);
            return false;
        }

        // updateOutputDirToTool();

        if (xnatTool.isDataSetEncryptionKeyInitialized() == false)
        {
            this.showError("Encryption Key not yet initialized. Create one or supply your password for:"
                    + xnatTool.getDataSetConfigName());
            return false;
        }

        enterBusy();

        final ScanMonitor scanMonitor = new ScanMonitor();
        final ScanMonitorWatcher scanWatcher = new ScanMonitorWatcher(this, scanMonitor);

        this.scanTask = new ActionTask(null, "Scanning Directory::" + sourceDirUri, scanWatcher)
        {
            @Override
            protected void doTask() throws Exception
            {
                try
                {
                    // this.uploadMonitor=new UploadMonitor(this);
                    xnatTool.doScanImageSourceDir(sourceDirUri, scanWatcher, scanMonitor);
                    postDoScanDirectory();
                }
                catch (Throwable t)
                {
                    XnatToolPanelController.this.handle(t.getMessage(), t);
                }
                finally
                {
                    XnatToolPanelController.this.exitBusy();
                }
            }

            @Override
            protected void stopTask() throws Exception
            {
                // get thread of task, not calling thread!
                // not needed: this.getThread().interrupt();
            }
        };

        this.scanTask.startTask();

        TaskMonitorDialog dailog = TaskMonitorDialog.showTaskMonitorDialog(getMainPanelJFrame(), scanTask, 0);

        return true;
    }

    protected void updateImageSourceDirToTool()
    {
        String loc = this.mainPanel.getSourceDirText();

        if (this.xnatTool == null)
        {
            return;// spurious event: can happen during initGui().
        }

        try
        {
            this.getXnatTool().setDataSetImageSourceDir(loc);
        }
        catch (Exception e)
        {
            this.handle("Couldn't set Cache Dir location to:" + loc, e);
        }
    }

    protected void exitBusy()
    {
        this.mainPanel.updateMouseBusy(false);
    }

    protected void enterBusy()
    {
        this.mainPanel.updateMouseBusy(true);
    }

    public void showError(String text)
    {
        String buts[] =
        {
            "OK"
        };
        MessageDialog.showMessage(mainPanel.getJFrame(), "Error!", text, buts);
    }

    /**
     * Called after doScanDir has finished!
     */
    protected void postDoScanDirectory()
    {
        int numSubjects = xnatTool.getLocalNumSubjects();
        int numSessions = xnatTool.getLocalNumSessions();
        int numScanSets = xnatTool.getLocalNumScanSets();
        int numFiles = xnatTool.getImageDirScanner().getNumFiles();
        // int numRawFiles = xnatTool.getDicomDirScanner().getNumRawFiles();

        this.mainPanel.setScanSetsInfo("Subjects:" + numSubjects, "Sessions:" + numSessions, "ScanSets:" + numScanSets);
        this.mainPanel.setEnableUploadButton(true);
        this.mainPanel.setEnableViewScanInfBut(true);
    }

    public XnatToolMainPanel getUploadPanel()
    {
        return this.mainPanel;
    }

    public DataSetConfig getDataSetConfig()
    {
        return this.xnatTool.getCurrentDataSetConfig();
    }

    protected boolean doCreateNewDatasetKey(boolean modCtrl)
    {
        boolean created = false;
        created = doCreateNewKeyDailog(modCtrl);

        updateEnableScanFields(created);

        if (created)
        {
            this.updateKeyStatusFieldsFromConfigFromTool();
            return true;
        }
        else
        {
            return false;
        }
    }

    protected boolean doCreateNewKeyDailog(boolean modCtrl)
    {
        String ownerId = this.mainPanel.getDataSetSourceID();
        // Secret passPhrase = this.mainPanel.getPassword();

        if (this.xnatTool.hasCurrentDataSetconfig() == false)
        {
            this.showError("No DataSet loaded or configured yet\nPlease create a DataSet First.");
            return false;
        }

        if (this.xnatTool.getCurrentDataSetConfig().hasValidEncryptionKey())
        {
            if (askOkCancel("Create new Key?", "Current DataSet already has an Encryption Key. Create new one ?") == false)
                return false;
        }

        KeyCreateDialog dialog = KeyCreateDialog.showDialog(this, "Create a New Key for DataSet:"
                + this.xnatTool.getDataSetConfigName(), true, true);

        Secret secret = dialog.getPassphraseSecret();
        this.updatePassphrase(secret);

        if (secret != null)
        {
            return true;
        }
        else
        {
            // cancelled or no key given
            return false;
        }
    }

    protected void updatePassphrase(Secret secret)
    {
        this.mainPanel.setPassphrase(secret);
        mainPanel.getDatasetConfigPanel().setPassphrase(secret);
    }

    @Override
    public void notifyImageDirScannerEvent(ImageDirEvent e)
    {
        logger.debugPrintf("<DicomDirEvent>:%s\n", e);
    }

    public String getDataSetSourceID()
    {
        return this.mainPanel.getDataSetSourceID();
    }

    public void doConfigDialog(boolean firstRun)
    {
        ConfigDialog dialog = ConfigDialog.createConfigDialog(this.getMainPanelJFrame(), this, firstRun);
        dialog.getController().updateSettingsFromTool();
        // pack,modality,location relative to frame, then setVisible();
        dialog.setModal(true);
        dialog.setLocationRelativeTo(this.getMainPanelJFrame());
        // when modal==true setVisible(true) will only return when dialog is
        // closed.
        dialog.setVisible(true);

        // post update:
        String confDir = dialog.getController().getNewDatasetConfigDir();
        if (confDir != null)
        {
            updateDataSetsConfigsDir(confDir, true);
        }

        String imageCacheDir = dialog.getController().getNewImageCacheDir();
        FSUtil fsUtil = FSUtil.getDefault();

        if (imageCacheDir != null)
        {
            try
            {
                this.getXnatTool().getToolConfig().setImageCacheDir(fsUtil.resolvePathURI(imageCacheDir));
            }
            catch (IOException e)
            {
                this.handle("Syntax Error: Invalid Image Cache Dir location.", e);
            }
        }

    }

    protected void updateDataSetsConfigsDir(String datasetsConfigDir, boolean autoCreate)
    {
        FSUtil fsUtil = FSUtil.getDefault();

        try
        {
            // use button 'create'.
            boolean exists = xnatTool.reloadDataSetsConfigDir(fsUtil.resolvePathURI(datasetsConfigDir), autoCreate);

            // list of DataSetConfigurations:
            List<String> names = xnatTool.getDataSetNames();
            if ((names != null) && (names.size() > 0))
            {
                mainPanel.setDataSetNames(names, names.get(0));
                this.doSwitchDataSetConfig(names.get(0));
            }
            else
            {
                mainPanel.setDataSetNames(null, null); // clear
            }

        }
        catch (Exception e)
        {
            this.handle("Couldn't create new directory:" + datasetsConfigDir, e);
        }
        // catch (URISyntaxException e)
        // {
        // this.handle("Invalid location:" + datasetsConfigDir, e);
        // }
    }

    public Secret getPassphrase()
    {
        return mainPanel.getPassphrase();
    }

}

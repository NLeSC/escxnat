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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.ImageTypes.DataSetType;
import nl.esciencecenter.medim.ImageTypes.ScanSubType;
import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField;
import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField.LocationType;
import nl.esciencecenter.ptk.ui.widgets.NavigationBar;
import nl.esciencecenter.ptk.ui.widgets.StringSelectionComboBox;
import nl.esciencecenter.xnattool.ui.UIUtil.UISettings;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * UploadPanel
 */
public class XnatToolMainPanel extends javax.swing.JPanel
{
    Cursor defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);

    Cursor busyCursor = new Cursor(Cursor.WAIT_CURSOR);

    // public static final String OPT_FILTETYPE_DICOM_scans = "Dicom Scans";
    //
    // public static final String OPT_FILTETYPE_NIFTI_scans = "Nifti Scans";
    //
    // public static final String OPT_FILTETYPE_ATLAS_niftis = "Atlas (nifti)";

    public static final String OPT_DICOM_MR = "Dicom MR";

    public static final String OPT_DICOM_ALL = "Dicom All";

    public static final String OPT_NIFTI_ALL = "Nifti";

    public static final String OPT_DICOM_EXTS = "*.dcm";

    public static final String OPT_FILE_ALL_EXTS = "*.*";

    public static final String OPT_NIFTI_EXTS = "*.nii[.gz]";

    protected static String dicomModalityOptions[] =
    {
            OPT_DICOM_MR, OPT_DICOM_ALL
    };

    protected static String dicomFileFilterOptions[] =
    {
            OPT_DICOM_EXTS, OPT_FILE_ALL_EXTS
    };

    protected static String niftiFileFilterOptions[] =
    {
        OPT_NIFTI_EXTS
    };

    protected static String niftiFilterOptions[] =
    {
        OPT_NIFTI_ALL
    };

    protected static ImageTypes.ScanSubType scanSubTypeOptions[] =
    {
            ImageTypes.ScanSubType.RAW_SCAN, ImageTypes.ScanSubType.NUC_SCAN, ImageTypes.ScanSubType.NONE
    };

    private static final long serialVersionUID = -6382874421099827120L;

    // ===

    private NavigationBar locationBar;

    private JPanel mainPanel;

    private NavigationBar uiNavigationBar;

    private JLabel dirLabel;

    private JLabel infoLabel;

    private JLabel fileTypeLbl;

    private DatasetSettingsPanel datasetConfigPanel;

    private JPanel settingsMainPnl;

    private JTabbedPane tabPanel;

    private JLabel authLabel;

    private JLabel XnatProject;

    private JLabel userLabel;

    private JLabel ownerLabel;

    private JPanel infoPanel;

    private XnatToolPanelController controller;

    private JFrame frame;

    private boolean isBusy;

    private int busyCount;

    private JScrollPane dicomOptionsSP;

    private JLabel flowLoginDownLbl;

    private JLabel flowScanSetLbl;

    private JLabel flowDataSetLbl;

    private JLabel flowAccountLabel;

    private JLabel passphraseLbl;

    private JLabel keyStatusLbl;

    private JLabel dataSetConfigSeperator;

    private JLabel dataSetCBLabel;

    // key fields + buttons
    protected JButton authenticateKeyBut;

    protected JButton createKeyBut;

    protected JPasswordField passPhrasePF;

    protected JTextField ownerIdTF;

    protected JTextField keyStatusTF;

    // Combo Boxes and buttons:
    protected StringSelectionComboBox dataSetSelectionCB;

    protected JButton loginBut;

    protected JButton uploadBut;

    protected JButton scanBut;

    protected JButton newDataSetBut;

    // Changable Fields:
    protected LocationSelectionField sourceDirTF;

    protected JTextField xnatUserTF;

    protected JTextField defaultOwnerIdTF;

    protected StringSelectionComboBox projectSelBox;

    protected StringSelectionComboBox dataSetTypeSB;

    protected StringSelectionComboBox filterModalitySB;

    protected StringSelectionComboBox scanSubTypeSB;

    private JButton changeOwnerIDBut;

    private JLabel defaultOwnerIDLbl;

    private JLabel scanSetsInfoLbl;

    private JTextField scanSetsInfoTF;

    private JButton viewScanSetBut;

    private JLabel lblFileFilter;

    protected StringSelectionComboBox fileFilterSB;

    protected JCheckBox checkMagicChBx;

    private JLabel lblScanType;

    // scan set statistics:

    /**
     * Auto-generated main method to display this JPanel inside a new JFrame.
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new XnatToolMainPanel());
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public XnatToolMainPanel(JFrame parent)
    {
        super();
        this.frame = parent;
        init();
    }

    public XnatToolMainPanel()
    {
        super();
        init();
    }

    /**
     * Set containing frame.
     */
    protected void setJFrame(JFrame frame)
    {
        this.frame = frame;
    }

    /**
     * Return parent JFrame, if it has one
     */
    protected JFrame getJFrame()
    {
        return this.frame;
    }

    private void init()
    {
        this.controller = new XnatToolPanelController(this);
        initGUI();
    }

    private void initGUI()
    {
        UISettings uiSettings = UIUtil.getUISettings();

        try
        {
            // this.setPreferredSize(new java.awt.Dimension(710, 792));
            this.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            {
                this.uiNavigationBar = getLocationBar();
                this.uiNavigationBar.setEnableNagivationButtons(true);
            }

            setLayout(new BorderLayout(0, 0));
            {
                infoPanel = new JPanel();
                add(infoPanel, BorderLayout.NORTH);
                infoPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));
                // infoPanel.setPreferredSize(new java.awt.Dimension(653, 30));

                {
                    this.uiNavigationBar = getLocationBar();
                    infoPanel.add(uiNavigationBar);
                    this.uiNavigationBar.setEnableNagivationButtons(true);
                }
            }

            tabPanel = new JTabbedPane();
            add(tabPanel);

            mainPanel = new JPanel();
            FormLayout configPanelLayout = new FormLayout(new ColumnSpec[]
            {
                    ColumnSpec.decode("max(5dlu;pref)"),
                    ColumnSpec.decode("19dlu"),
                    ColumnSpec.decode("5dlu"),
                    ColumnSpec.decode("33dlu"),
                    ColumnSpec.decode("max(32dlu;pref)"),
                    ColumnSpec.decode("max(32dlu;pref):grow"),
                    ColumnSpec.decode("5dlu"),
                    ColumnSpec.decode("max(32dlu;pref):grow"),
                    ColumnSpec.decode("5dlu"),
                    ColumnSpec.decode("54dlu"),
                    ColumnSpec.decode("max(18dlu;default)"),
                    FormFactory.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("max(17dlu;pref):grow(10)"),
                    ColumnSpec.decode("5dlu"),
                    ColumnSpec.decode("max(51dlu;pref)"),
                    ColumnSpec.decode("5dlu"),
            },
                    new RowSpec[]
                    {
                            FormFactory.UNRELATED_GAP_ROWSPEC,
                            RowSpec.decode("18dlu"),
                            RowSpec.decode("6dlu"),
                            RowSpec.decode("16dlu"),
                            RowSpec.decode("16dlu"),
                            RowSpec.decode("16dlu"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(19dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("16dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            FormFactory.RELATED_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.RELATED_GAP_ROWSPEC,
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            FormFactory.DEFAULT_ROWSPEC,
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("5dlu:grow"),
                    });

            tabPanel.addTab("Main", null, mainPanel, null);

            mainPanel.setLayout(configPanelLayout);
            mainPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            // mainPanel.setPreferredSize(new java.awt.Dimension(656,
            // 645));
            // mainPanel.setPreferredSize(new
            // java.awt.Dimension(392, 226));

            {
                userLabel = new JLabel();
                mainPanel.add(userLabel, new CellConstraints("4, 4, 2, 1, default, default"));
                userLabel.setText("XNAT User:");
            }
            {
                authLabel = new JLabel();
                mainPanel.add(authLabel, "2, 2, 14, 1");
                authLabel.setText("I) XNAT Account Information");
                authLabel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                loginBut = new JButton();
                mainPanel.add(loginBut, "10, 4");
                loginBut.setText("Login");
                loginBut.setPreferredSize(new java.awt.Dimension(101, 23));
                loginBut.addActionListener(this.controller);
                loginBut.setActionCommand(UIAction.ACTION_LOGIN.toString());
            }
            {
                defaultOwnerIDLbl = new JLabel("Owner ID:");
                mainPanel.add(defaultOwnerIDLbl, "4, 5,2,1,fill,default");
            }
            {
                defaultOwnerIdTF = new JTextField();
                defaultOwnerIdTF.setText("<Owner ID>");
                mainPanel.add(defaultOwnerIdTF, "6, 5, 3, 1, fill, default");
                defaultOwnerIdTF.setColumns(10);
                defaultOwnerIdTF.addActionListener(this.controller);
                defaultOwnerIdTF.addFocusListener(this.controller);
            }
            {
                XnatProject = new JLabel();
                mainPanel.add(XnatProject, "4, 6, 2, 1");
                XnatProject.setText("XNAT Project:");
            }
            {
                projectSelBox = new StringSelectionComboBox(new String[]
                {
                    "?"
                });
                projectSelBox.addActionListener(controller.getComboBoxListener());
                projectSelBox.setActionCommand(UIAction.CB_PROJECT_CHANGED.toString());
                mainPanel.add(projectSelBox, "6, 6, 3, 1");
            }
            {
                xnatUserTF = new JTextField();
                mainPanel.add(xnatUserTF, "6, 4, 3, 1");
                xnatUserTF.setText("<User>");
                xnatUserTF.addActionListener(this.controller);
            }
            {
                ownerIdTF = new JTextField();
                mainPanel.add(ownerIdTF, "6, 11, 3, 1");
                ownerIdTF.setText("<OwnerID>");
                ownerIdTF.addActionListener(this.controller);
                ownerIdTF.setActionCommand(UIAction.FIELD_OWNERID_CHANGED.toString());
                ownerIdTF.setEditable(false);
                uiSettings.applySetting(ownerIdTF, UISettings.UIType.INFO_TEXTFIELD);
            }
            {
                changeOwnerIDBut = new JButton("Change");
                mainPanel.add(changeOwnerIDBut, "10, 11");
                changeOwnerIDBut.setEnabled(false);
                changeOwnerIDBut.addActionListener(this.controller);
                changeOwnerIDBut.setActionCommand(UIAction.BUT_CHANGE_OWNERID.toString());
            }
            {
                dataSetSelectionCB = new StringSelectionComboBox(new String[]
                {
                    "<None>"
                });
                dataSetSelectionCB.setEnabled(false);
                dataSetSelectionCB.setEditable(false);
                dataSetSelectionCB.setActionCommand(UIAction.CB_DATASET_CHANGED.toString());
                dataSetSelectionCB.addActionListener(controller.getComboBoxListener());
                mainPanel.add(dataSetSelectionCB, "6, 10, 3, 1");
            }
            {
                createKeyBut = new JButton();
                mainPanel.add(createKeyBut, "10, 12");
                createKeyBut.setText("Create");
                createKeyBut.setActionCommand(UIAction.BUT_CREATE_INIT_KEY.toString());
                createKeyBut.addActionListener(controller);
                createKeyBut.setEnabled(false); // start disabled.
            }
            {
                authenticateKeyBut = new JButton();
                mainPanel.add(authenticateKeyBut, "15, 13");
                authenticateKeyBut.setText("Authenticate");
                authenticateKeyBut.addActionListener(this.controller);
                authenticateKeyBut.setActionCommand(UIAction.BUT_AUTHENTICATE_KEY.toString());
                authenticateKeyBut.setEnabled(false); // start disabled.
            }
            {
                dirLabel = new JLabel();
                mainPanel.add(dirLabel, "4, 15, 2, 1");
                dirLabel.setText("Source Directory:");
            }
            {
                sourceDirTF = new LocationSelectionField(LocationType.DirType);
                sourceDirTF.setLocationText("<Image Source Directory>");
                sourceDirTF.setLocationActionCommand(UIAction.FIELD_SOURCEDIR_CHANGED.toString());
                // Focus!
                sourceDirTF.addFocusListener(new FocusListener()
                {
                    public void focusGained(FocusEvent e)
                    {
                    }

                    public void focusLost(FocusEvent e)
                    {
                        getController().updateImageSourceDirToTool();
                    }

                });
                sourceDirTF.setFocusable(true);
                sourceDirTF.addLocationActionListener(controller);
                sourceDirTF.setLocationEnabled(false);
            }
            mainPanel.add(sourceDirTF, "6, 15, 8, 1");
            {
                scanBut = new JButton();
                mainPanel.add(scanBut, "15, 15");
                scanBut.setText("Scan Files");
                scanBut.addActionListener(this.controller);
                scanBut.setActionCommand(UIAction.ACTION_SCAN.toString());
                scanBut.setEnabled(false);
            }
            {
                JLabel lblFileType = new JLabel();
                lblFileType.setText("Data Type");
                mainPanel.add(lblFileType, "4, 16, 2, 1");
            }
            {
                dataSetTypeSB = new StringSelectionComboBox(new String[]
                {
                        ImageTypes.DataSetType.DICOM_SCANSET.toString(),
                        ImageTypes.DataSetType.NIFTI_SCANSET.toString(),
                        ImageTypes.DataSetType.NIFTI_ATLASSET.toString()
                });
                dataSetTypeSB.addActionListener(controller.getComboBoxListener());
                dataSetTypeSB.setActionCommand(UIAction.DATASET_TYPE_CHANGED.toString());
                mainPanel.add(dataSetTypeSB, "6, 16");
            }
            {
                lblFileFilter = new JLabel("File Filter");
                mainPanel.add(lblFileFilter, "8, 16");
            }
            {
                checkMagicChBx = new JCheckBox("check magic type");
                checkMagicChBx.setActionCommand(UIAction.DATASET_MAGIC_CB_CHANGED.toString());
                checkMagicChBx.addActionListener(controller);
                fileFilterSB = new StringSelectionComboBox(dicomFileFilterOptions);
                fileFilterSB.addActionListener(controller.getComboBoxListener());
                fileFilterSB.setActionCommand(UIAction.DATASET_FILE_FILTER_CHANGED.toString());
                mainPanel.add(fileFilterSB, "10, 16, fill, default");

                {
                    fileTypeLbl = new JLabel();
                    fileTypeLbl.setText("Filter Options");
                    mainPanel.add(fileTypeLbl, "8, 17");
                }

                {
                    filterModalitySB = new StringSelectionComboBox(dicomModalityOptions);
                    filterModalitySB.addActionListener(controller.getComboBoxListener());
                    filterModalitySB.setActionCommand(UIAction.DATASET_FILTER_CHANGED.toString());
                    mainPanel.add(filterModalitySB, "10, 17");
                }

                {
                    lblScanType = new JLabel("Scan Type");
                    mainPanel.add(lblScanType, "8, 18");
                }

                {
                    String vals[] = new String[scanSubTypeOptions.length];
                    for (int i = 0; i < scanSubTypeOptions.length; i++)
                    {
                        vals[i] = scanSubTypeOptions[i].toString();
                    }
                    scanSubTypeSB = new StringSelectionComboBox(vals);
                    scanSubTypeSB.addActionListener(controller.getComboBoxListener());
                    scanSubTypeSB.setActionCommand(UIAction.SCANSUBTYPE_OPTIONS_CHANGED.toString());
                    mainPanel.add(scanSubTypeSB, "10, 18");
                }

                mainPanel.add(checkMagicChBx, "8, 20, 6, 1");
            }

            {
                infoLabel = new JLabel();
                infoLabel.setText("III) Scan set information");
                infoLabel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                mainPanel.add(infoLabel, "2, 23, 14, 1");
            }
            {
                newDataSetBut = new JButton();
                newDataSetBut.setText("New");
                newDataSetBut.setPreferredSize(new java.awt.Dimension(101, 23));
                newDataSetBut.setActionCommand(UIAction.BUT_NEW_DATASET.toString());
                newDataSetBut.addActionListener(controller);
                newDataSetBut.setEnabled(false);
                mainPanel.add(newDataSetBut, "10, 10");
            }
            {
                ownerLabel = new JLabel();
                mainPanel.add(ownerLabel, "4, 11, 2, 1");
                ownerLabel.setText("Owner ID:");
            }

            {
                dataSetCBLabel = new JLabel();
                mainPanel.add(dataSetCBLabel, "4, 10, 2, 1");
                dataSetCBLabel.setText("DataSet");
            }
            {
                dataSetConfigSeperator = new JLabel();
                mainPanel.add(dataSetConfigSeperator, "2, 8, 14, 1");
                dataSetConfigSeperator.setText("II) Data Set Configuration");
                dataSetConfigSeperator.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                keyStatusLbl = new JLabel();
                mainPanel.add(keyStatusLbl, "4, 12, 2, 1");
                keyStatusLbl.setText("Key Status");
            }
            {
                keyStatusTF = new JTextField();
                mainPanel.add(keyStatusTF, "6, 12, 3, 1");
                keyStatusTF.setText("<Key Status>");
                keyStatusTF.setEditable(false);
                uiSettings.applySetting(keyStatusTF, UISettings.UIType.INFO_TEXTFIELD);
            }
            {
                passphraseLbl = new JLabel();
                mainPanel.add(passphraseLbl, "4, 13, 2, 1");
                passphraseLbl.setText("Passphrase");
            }
            {
                passPhrasePF = new JPasswordField();
                mainPanel.add(passPhrasePF, "6, 13, 8, 1");
                passPhrasePF.setText("");
                passPhrasePF.addActionListener(this.controller);
                passPhrasePF.setActionCommand(UIAction.FIELD_PASSWORD_CHANGED.toString());
                passPhrasePF.setEnabled(false);
            }
            {
                flowAccountLabel = new JLabel();
                mainPanel.add(flowAccountLabel, "2, 4, 1, 4");
                flowAccountLabel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                // flowAccountLabel.setIcon(loadIcon("flow/flow_diamond_llblue.png"));
            }
            {
                flowDataSetLbl = new JLabel();
                mainPanel.add(flowDataSetLbl, "2, 10, 1, 8");
                flowDataSetLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                flowScanSetLbl = new JLabel();
                mainPanel.add(flowScanSetLbl, "2, 25, 1, 2");
                flowScanSetLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                flowLoginDownLbl = new JLabel();
                // flowLoginDownLbl.setIcon(this.loadIcon("flow/flow_down_llblue.png"));
                mainPanel.add(flowLoginDownLbl, "15, 6");

            }

            {
                scanSetsInfoLbl = new JLabel("Scan Sets:");
                mainPanel.add(scanSetsInfoLbl, "4, 25, 2, 1");
            }
            {
                scanSetsInfoTF = new JTextField();
                scanSetsInfoTF.setText("<Sessions>");
                mainPanel.add(scanSetsInfoTF, "6, 25, 5, 1, fill, default");
                scanSetsInfoTF.setColumns(10);
            }
            {
                uploadBut = new JButton();
                mainPanel.add(uploadBut, "15, 25");
                uploadBut.setText("Upload");
                uploadBut.addActionListener(this.controller);
                uploadBut.setActionCommand(UIAction.ACTION_UPLOAD.toString());
                uploadBut.setEnabled(false);
            }
            {
                viewScanSetBut = new JButton("View");
                viewScanSetBut.addActionListener(this.controller);
                viewScanSetBut.setActionCommand(UIAction.ACTION_VIEW_SCANSETINF.toString());
                viewScanSetBut.setEnabled(false);

                mainPanel.add(viewScanSetBut, "6, 26");
            }

            // ---
            // DatasetSettingsPanel
            // ---

            {
                settingsMainPnl = new JPanel();
                tabPanel.addTab("DataSet Profile", null, settingsMainPnl, null);
                BorderLayout DcmOptionsPanelLayout = new BorderLayout();
                settingsMainPnl.setLayout(DcmOptionsPanelLayout);
                dicomOptionsSP = new JScrollPane();
                settingsMainPnl.add(dicomOptionsSP, BorderLayout.CENTER);

                {
                    datasetConfigPanel = new DatasetSettingsPanel(this.controller);
                    settingsMainPnl.add(datasetConfigPanel);
                }

            }
            {

            }

            this.validate();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public NavigationBar getLocationBar()
    {
        if (this.locationBar == null)
        {
            this.locationBar = new nl.esciencecenter.ptk.ui.widgets.NavigationBar(NavigationBar.LOCATION_ONLY);
            this.locationBar.setEnableNagivationButtons(false);
            this.locationBar.addTextFieldListener(this.controller);
            this.locationBar.clearLocationHistory();
        }

        return this.locationBar;
    }

    public StringSelectionComboBox getProjectSelBox()
    {
        return projectSelBox;
    }

    public XnatToolPanelController getController()
    {
        return this.controller;
    }

    public void setUser(String username)
    {
        xnatUserTF.setText(username);
    }

    public String getUserText()
    {
        return this.xnatUserTF.getText();
    }

    public JTextField getUserTF()
    {
        return this.xnatUserTF;
    }

    public JTextField getDefaultOwnerIdTF()
    {
        return this.defaultOwnerIdTF;
    }

    public String getDefaultOwnerId()
    {
        return this.defaultOwnerIdTF.getText();
    }

    public String getSourceDirText()
    {
        return this.sourceDirTF.getLocationText();
    }

    public void setSourceDirText(String txt)
    {
        sourceDirTF.setLocationText(txt);
    }

    public void setBusy(boolean val)
    {
        if (val)
            this.busyCount++;
        else
            this.busyCount--;

        this.isBusy = val;
    }

    public boolean isBusy()
    {
        return this.isBusy;
    }

    public void updateMouseBusy(boolean isBusy)
    {
        setBusy(isBusy);

        if (isBusy())
            setCursor(busyCursor);
        else
            setCursor(defaultCursor);
    }

    public DataSetType getDataSetType()
    {
        String strVal = dataSetTypeSB.getSelectedItemString();
        if (strVal == null)
            return null;

        return ImageTypes.DataSetType.valueOfByLabel(strVal);
    }

    public void setDataSetType(DataSetType type)
    {
        dataSetTypeSB.setSelectedItem(type.toString());
    }

    public String getFilterModalityType()
    {
        return filterModalitySB.getSelectedItemString();
    }

    public DatasetSettingsPanel getDatasetConfigPanel()
    {
        return datasetConfigPanel;
    }

    public String getDataSetSourceID()
    {
        return ownerIdTF.getText();
    }

    public void setDataSetSourceID(String id)
    {
        ownerIdTF.setText(id);
    }

    public Secret getPassphrase()
    {
        return new Secret(passPhrasePF.getPassword());
    }

    public void setPassphrase(Secret value)
    {
        if (value == null)
        {
            passPhrasePF.setText(null);
        }
        else
        {
            passPhrasePF.setText(new String(value.getChars()));
        }
    }

    public void setDataSetNames(List<String> dataSetNames, String selected)
    {
        boolean empty = false;
        String names[];

        if (dataSetNames == null)
        {
            names = new String[]
            {
                "<NONE>"
            };
            empty = true;
        }
        else
        {
            names = new String[dataSetNames.size()];
            names = dataSetNames.toArray(names);
            empty = false;
        }

        this.dataSetSelectionCB.setValues(names);
        this.dataSetSelectionCB.setEnabled(empty == false);
        this.dataSetSelectionCB.setEditable(false);
        this.dataSetSelectionCB.setSelectedItem(selected);
    }

    public StringSelectionComboBox getDataSetSelectionCB()
    {
        return this.dataSetSelectionCB;
    }

    public void setDataSetName(String name, boolean autoAddToSelection)
    {
        if (dataSetSelectionCB.hasValue(name) == false)
        {
            if (autoAddToSelection)
            {
                if (dataSetSelectionCB.isEnabled() == false)
                {
                    // new first DataSet.
                    dataSetSelectionCB.setValues(new String[]
                    {
                        name
                    });
                    dataSetSelectionCB.setEnabled(true);
                    return;
                }
                else
                {
                    dataSetSelectionCB.addValue(name);
                }
            }
            else
            {
                return;
            }
        }

        String val = dataSetSelectionCB.getSelectedItemString();
        if ((val != null) && (val.equals(name)))
        {
            return;
        }

        this.dataSetSelectionCB.setSelectedItem(name);
        this.dataSetSelectionCB.setEnabled(true);
    }

    public void setEnableCreateKeyKeyButton(boolean value)
    {
        this.createKeyBut.setEnabled(value);
    }

    public void setEnableAuthenticateKeyButton(boolean value)
    {
        this.authenticateKeyBut.setEnabled(value);
    }

    public void setScanSetsInfo(String subjectInfo, String sessionInfo, String scanSetsInfo)
    {
        // this.subjectsInfoTf.setText(subjectInfo);
        // this.sessionInfoTF.setText(sessionInfo);
        this.scanSetsInfoTF.setText(subjectInfo + "/" + sessionInfo + "/" + scanSetsInfo);
    }

    public void setEnableDataSelectionCB(boolean value)
    {
        dataSetSelectionCB.setEditable(value);
    }

    public void setEnablePassphraseTF(boolean value)
    {
        passPhrasePF.setEnabled(value);
    }

    public void setEnableNewDataSetConfig(boolean value)
    {
        newDataSetBut.setEnabled(value);
    }

    public void setEnableChangeOwnerIDBut(boolean value)
    {
        changeOwnerIDBut.setEnabled(value);
    }

    public void setEnableImageSourceTF(boolean value)
    {
        sourceDirTF.setLocationEnabled(value);
        sourceDirTF.setLocationEditable(value);
    }

    public void setEnableUploadButton(boolean value)
    {
        this.uploadBut.setEnabled(value);
    }

    public void setEnableScanButton(boolean value)
    {
        this.scanBut.setEnabled(value);
    }

    public void setEnableViewScanInfBut(boolean value)
    {
        this.viewScanSetBut.setEnabled(value);
    }

    public ImageTypes.ScanSubType getScanSubTypeOption()
    {
        String str = scanSubTypeSB.getSelectedItemString();
        if (str == null)
        {
            return ScanSubType.NONE;
        }

        return ImageTypes.ScanSubType.valueOf(str);
    }

    public void setScanSubType(ScanSubType subType)
    {
        if (subType == null)
        {
            subType = ScanSubType.NONE;
        }

        this.scanSubTypeSB.setSelectedItem(subType.toString());
    }

}

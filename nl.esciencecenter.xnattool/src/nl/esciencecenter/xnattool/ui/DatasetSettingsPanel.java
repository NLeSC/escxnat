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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.medim.dicom.DicomProcessingProfile.*;
import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.ui.widgets.StringSelectionComboBox;
import nl.esciencecenter.xnattool.ui.UIUtil.UISettings;
import nl.esciencecenter.xnattool.ui.UIUtil.UISettings.UIType;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class DatasetSettingsPanel extends javax.swing.JPanel
{
    private static final long serialVersionUID = -1L;

    public static final String NEW_PROFILE = "newProfile";

    DatasetSettingsPanelController controller;

    private JLabel profileLbl;

    private JLabel subjectIdLbl;

    private JLabel settingsLbl;

    private JLabel passwordLbl;

    private JLabel sourceIDLbl;

    private JLabel dicomSettingsLbl;

    private JLabel headerLbl;

    private JLabel keyStatusLabl;

    // Fields:
    protected StringSelectionComboBox profileCB;

    protected JButton newProfileBut;

    protected StringSelectionComboBox subjectIdCB;

    protected JButton createKeyBut;

    protected JButton saveBut;

    protected JTextField keyStatusTF;

    protected StringSelectionComboBox scanUidCB;

    // private
    protected JTextField sourceIdTF;

    protected JPasswordField passwordTF;

    private XnatToolPanelController masterController;

    private JLabel sessionKeyLbl;

    StringSelectionComboBox sessionKeyCB;

    private JLabel datasetNameLbl;

    private JTextField dataSetNameTF;

    private JLabel lblScanKeyType;

    /**
     * Auto-generated main method to display this JPanel inside a new JFrame.
     */
    public static void main(String[] args)
    {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new DatasetSettingsPanel(null));
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public DatasetSettingsPanel()
    {
        super();
        initGUI();
    }

    public DatasetSettingsPanel(XnatToolPanelController panelController)
    {
        super();
        this.controller = new DatasetSettingsPanelController(this);
        this.masterController = panelController;
        initGUI();
    }

    protected XnatToolPanelController getMasterController()
    {
        return this.masterController;
    }

    private void initGUI()
    {
        UISettings uiSettings = UIUtil.getUISettings();

        try
        {
            FormLayout thisLayout = new FormLayout(new ColumnSpec[] {
                    ColumnSpec.decode("max(5dlu;pref)"),
                    ColumnSpec.decode("80dlu"),
                    ColumnSpec.decode("max(5dlu;pref)"),
                    ColumnSpec.decode("84dlu:grow"),
                    ColumnSpec.decode("8dlu"),
                    ColumnSpec.decode("30dlu"),
                    ColumnSpec.decode("8dlu"),
                    ColumnSpec.decode("43dlu"),
                    ColumnSpec.decode("5dlu"),
                    ColumnSpec.decode("default:grow"),
                    ColumnSpec.decode("5dlu"),
            },
                    new RowSpec[] {
                            RowSpec.decode("max(5dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;default)"),
                            FormFactory.RELATED_GAP_ROWSPEC,
                            RowSpec.decode("15dlu"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("15dlu"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("15dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("5dlu"),
                            RowSpec.decode("max(15dlu;pref)"),
                            RowSpec.decode("max(15dlu;pref)"),
                            FormFactory.PREF_ROWSPEC,
                            FormFactory.RELATED_GAP_ROWSPEC,
                            RowSpec.decode("5dlu"),
                    });
            this.setLayout(thisLayout);
            {
                datasetNameLbl = new JLabel("DataSet Name:");
                add(datasetNameLbl, "2, 4");
            }
            {
                dataSetNameTF = new JTextField();
                add(dataSetNameTF, "4, 4, fill, default");
                // dataSetNameTF.setColumns(10);
                dataSetNameTF.setEditable(false);
                uiSettings.applySetting(dataSetNameTF, UIType.INFO_TEXTFIELD);
            }

            {
                profileLbl = new JLabel();
                this.add(profileLbl, "2, 6");
                profileLbl.setText("Profile:");
            }
            {
                newProfileBut = new JButton();
                this.add(newProfileBut, "8, 6");
                newProfileBut.setText("New!");
                newProfileBut.setEnabled(false);
                newProfileBut.setActionCommand(NEW_PROFILE);
                newProfileBut.addActionListener(controller);
            }
            {
                profileCB = new StringSelectionComboBox(new String[] {
                    "Default"
                });
                this.add(profileCB, "4, 6");
                profileCB.addActionListener(controller);
                profileCB.setEnabled(false);
            }

            {
                settingsLbl = new JLabel();
                this.add(settingsLbl, "2, 8, 9, 1");
                settingsLbl.setText("Identity and Encryption ");
                settingsLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                sourceIDLbl = new JLabel();
                this.add(sourceIDLbl, "2, 10");
                sourceIDLbl.setText("OwnerID");
            }
            {
                sourceIdTF = new JTextField();
                this.add(sourceIdTF, "4, 10");
                sourceIdTF.setText("OwnerID");
                sourceIdTF.addActionListener(controller);
                sourceIdTF.addFocusListener(controller);
            }
            {
                keyStatusLabl = new JLabel();
                this.add(keyStatusLabl, "2, 11");
                keyStatusLabl.setText("KeyStatus");
            }
            {
                keyStatusTF = new JTextField();
                this.add(keyStatusTF, "4, 11, 3, 1");
                keyStatusTF.setText("<No Key Initialized>");
                keyStatusTF.setEditable(false);
                uiSettings.applySetting(keyStatusTF, UISettings.UIType.INFO_TEXTFIELD);
            }
            {
                createKeyBut = new JButton();
                this.add(createKeyBut, "8, 11");
                createKeyBut.setText("Create");
                createKeyBut.addActionListener(this.controller);
            }
            {
                passwordLbl = new JLabel();
                this.add(passwordLbl, "2, 12");
                passwordLbl.setText("Passphrase");
            }
            {
                passwordTF = new JPasswordField();
                this.add(passwordTF, "4, 12, 3, 1");
                passwordTF.setText("");
                passwordTF.addActionListener(controller);
                // passwordTF.setEnabled(false);
            }
            {
                dicomSettingsLbl = new JLabel();
                this.add(dicomSettingsLbl, "2, 14, 9, 1");
                dicomSettingsLbl.setText("XNAT ID-Mapping");
                dicomSettingsLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }

            {
                headerLbl = new JLabel();
                this.add(headerLbl, "2, 2, 9, 1");
                headerLbl.setText("DataSet Profile");
                headerLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }

            // SubjectKey type.
            {
                subjectIdLbl = new JLabel();
                this.add(subjectIdLbl, "2, 16");
                subjectIdLbl.setText("Subject Key Type:");
            }

            {

                subjectIdCB = new StringSelectionComboBox(SubjectKeyType.stringValues());
                this.add(subjectIdCB, "4, 16, 3, 1");
                subjectIdCB.addActionListener(controller);
            }
            // SessionKey type.
            {
                sessionKeyLbl = new JLabel();
                this.add(sessionKeyLbl, "2, 17");
                sessionKeyLbl.setText("Session Key Type:");
            }

            {
                String values[] = new String[] {
                        "" + SessionKeyType.CRYPTHASH_STUDY_UID,
                        "" + SessionKeyType.PLAIN_STUDY_UID,
                        "" + SessionKeyType.STUDY_DATE_YEAR,
                        "" + SessionKeyType.STUDY_DATE_YEAR_MONTH
                };
                sessionKeyCB = new StringSelectionComboBox(values);

                this.add(sessionKeyCB, "4, 17, 3, 1");
                sessionKeyCB.addActionListener(controller);
            }
            // / ScanKey type
            {
                lblScanKeyType = new JLabel("Scan Key Type");
                add(lblScanKeyType, "2, 18");
            }
            {
                scanUidCB = new StringSelectionComboBox(new String[] {
                        "" + ScanKeyType.CRYPTHASH_SCAN_UID, "" + ScanKeyType.PLAIN_SCAN_UID
                });
                this.add(scanUidCB, "4, 18, 3, 1");
                scanUidCB.addActionListener(controller);
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setDataSetName(String name)
    {
        // Avoid event loop (or double event trigger) by checking whether value has already been set.
        if (dataSetNameTF.getText().equals(name))
            return;

        dataSetNameTF.setText(name);
    }

    // public void setProfileNames(String names[])
    // {
    // DefaultComboBoxModel model = new DefaultComboBoxModel(names);
    // profileCB.setModel(model);
    // }

    public void setSubjectKeyType(SubjectKeyType subjectIDType)
    {
        if (subjectIDType == null)
        {
            this.subjectIdCB.setEnabled(false);
            return;
        }
        else
        {
            this.subjectIdCB.setEnabled(true);
        }

        this.subjectIdCB.setSelectedItem(subjectIDType);
    }

    public void setScanKeyType(ScanKeyType scanKeyType)
    {
        if (scanKeyType == null)
        {
            this.scanUidCB.setEnabled(false);
            return;
        }
        else
        {
            this.scanUidCB.setEnabled(true);
        }

        this.scanUidCB.setSelectedItem(scanKeyType);

    }

    public void setSessionKeyType(SessionKeyType sessionKeyType)
    {
        if (sessionKeyType == null)
        {
            this.sessionKeyCB.setEnabled(false);
            return;
        }
        else
        {
            this.sessionKeyCB.setEnabled(true);
        }

        this.sessionKeyCB.setSelectedItem(sessionKeyType);
    }

    public String getSourceID()
    {
        return this.sourceIdTF.getText();
    }

    public void setSourceId(String newSourceId)
    {
        // Avoid event loop (or double event trigger) by checking whether value has already been set.
        if (sourceIdTF.getText().equals(newSourceId))
            return;

        sourceIdTF.setText(newSourceId);
    }

    public Secret getPasswordSecret()
    {
        return new Secret(passwordTF.getPassword());
    }

    public void setPassphrase(Secret secret)
    {
        this.passwordTF.setText(new String(secret.getChars()));
    }

}

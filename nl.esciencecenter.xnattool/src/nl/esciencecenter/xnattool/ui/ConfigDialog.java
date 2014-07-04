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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField;
import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField.LocationType;
import nl.esciencecenter.xnattool.ui.UIUtil.UISettings;
import nl.esciencecenter.xnattool.ui.UIUtil.UISettings.UIType;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ConfigDialog extends javax.swing.JDialog
{
    private static final long serialVersionUID = -5558298493342571756L;

    public static ConfigDialog createConfigDialog(JFrame frame, XnatToolPanelController mainController, boolean firstRun)
    {
        ConfigDialog dialog = new ConfigDialog(frame, mainController, firstRun);

        // dialog.pack();
        // dialog.setLocationRelativeTo(frame);
        // dialog.setVisible(true);
        return dialog;
    }

    // ===
    public String firstRunText = "Important: Before you start the tool please check the settings below.\n\n";

    public String configDialogText =
            "Specify DataSet Configuration directory and Image Cache Dir.\n"
                    + "The DataSet Configuration directory stores your DataSet profiles and identity mappings.\n"
                    + "The Image Cache directory holds temporary processed dicom images, which after uploading can be deleted.\n"
                    + "If the directories below do not exist yet, please create them by pressing the 'Create' button.\n";

    // ===

    private JPanel MainPanel;

    private JPanel topPanel;

    private JLabel dataSetConfigLbl;

    protected JButton createImageCacheDirBut;

    protected JButton createDatasetsConfigDirBut;

    private JButton okBut;

    protected JButton clearCacheBut;

    private LocationSelectionField imageCacheDirTF;

    private JLabel imageCacheDirectoryLbl;

    private JButton cancelBut;

    private JPanel buttonPanel;

    private LocationSelectionField datasetConfigDirTF;

    private JTextArea helpTextArea;

    private ConfigDialogController controller;

    private XnatToolPanelController mainController;

    private boolean firstRun = false;

    protected JCheckBox keepProcessedDicomCB;

    protected JCheckBox autoCreateIdMappingsCB;

    protected JCheckBox autoExtractMetaDataCB;

    /**
     * Auto-generated main method to display this JDialog
     */
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                JFrame frame = new JFrame();
                ConfigDialog inst = new ConfigDialog(frame);
                inst.setVisible(true);
            }
        });
    }

    /**
     * @wbp.parser.constructor
     */
    public ConfigDialog(JFrame frame)
    {
        super(frame);
        initGUI();
    }

    public ConfigDialog(JFrame frame, XnatToolPanelController mainController, boolean firstRun)
    {
        super(frame);
        this.mainController = mainController;
        this.firstRun = firstRun;
        initGUI();
    }

    public XnatToolPanelController getMasterController()
    {
        return mainController;
    }

    private void initGUI()
    {
        this.controller = new ConfigDialogController(this);
        UISettings uiSettings = UIUtil.getUISettings();

        try
        {
            {
                topPanel = new JPanel();

                getContentPane().add(topPanel, BorderLayout.NORTH);
                topPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
                {
                    JLabel topPanelLbl = new JLabel("Tool Configuration Options.");
                    topPanelLbl.setFont(new Font("DejaVu Sans Mono", Font.BOLD, 14));
                    topPanel.add(topPanelLbl);
                }
            }
            {
                MainPanel = new JPanel();
                getContentPane().add(MainPanel, BorderLayout.CENTER);
                FormLayout MainPanelLayout = new FormLayout(new ColumnSpec[] {
                        ColumnSpec.decode("8dlu"),
                        ColumnSpec.decode("12dlu"),
                        ColumnSpec.decode("42dlu"),
                        ColumnSpec.decode("17dlu:grow"),
                        ColumnSpec.decode("74dlu"),
                        ColumnSpec.decode("17dlu:grow"),
                        FormFactory.UNRELATED_GAP_COLSPEC,
                        ColumnSpec.decode("55dlu"),
                        ColumnSpec.decode("8dlu"),
                },
                        new RowSpec[] {
                                RowSpec.decode("5dlu"),
                                RowSpec.decode("6dlu"),
                                RowSpec.decode("max(32dlu;default):grow"),
                                FormFactory.RELATED_GAP_ROWSPEC,
                                RowSpec.decode("16dlu"),
                                RowSpec.decode("5dlu"),
                                RowSpec.decode("max(15dlu;pref)"),
                                RowSpec.decode("5dlu"),
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                RowSpec.decode("16dlu"),
                                RowSpec.decode("5dlu"),
                                RowSpec.decode("14dlu"),
                                FormFactory.RELATED_GAP_ROWSPEC,
                                FormFactory.DEFAULT_ROWSPEC,
                                FormFactory.RELATED_GAP_ROWSPEC,
                                RowSpec.decode("13dlu"),
                                RowSpec.decode("16dlu"),
                                RowSpec.decode("5dlu:grow"),
                        });
                MainPanel.setLayout(MainPanelLayout);
                MainPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                // topSubBorderPnl.setPreferredSize(new java.awt.Dimension(504, 77));
                {
                    helpTextArea = new JTextArea();
                    MainPanel.add(helpTextArea, "2, 3, 7, 1");
                    helpTextArea.setText(getHelpText(this.firstRun));
                    // helpTextArea.setPreferredSize(new java.awt.Dimension(244, 100));
                    helpTextArea.setEditable(false);
                    helpTextArea.setLineWrap(true);
                    helpTextArea.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                    uiSettings.applySetting(helpTextArea, UIType.INFO_TEXTFIELD);
                }
                // MainPanel.setPreferredSize(new java.awt.Dimension(518, 205));
                {
                    dataSetConfigLbl = new JLabel();
                    MainPanel.add(dataSetConfigLbl, "2, 5, 7, 1");
                    dataSetConfigLbl.setText("Dataset Configuration Directory and Options:");
                    dataSetConfigLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                }
                {
                    datasetConfigDirTF = new LocationSelectionField(LocationType.DirType);
                    MainPanel.add(datasetConfigDirTF, "3, 7, 4, 1");
                    datasetConfigDirTF.setLocationText("<Dataset Config Directory>");
                    datasetConfigDirTF.addLocationActionListener(controller);
                    datasetConfigDirTF.addFocusListener(new FocusListener() {
                        public void focusGained(FocusEvent e)
                        {
                        }

                        public void focusLost(FocusEvent e)
                        {
                            getController().updateDatasetConfigDirChanged();
                        }
                    });
                    datasetConfigDirTF.setLocationActionCommand("" + UIAction.FIELD_CONFIGDIR_CHANGED);
                }
                {
                    autoCreateIdMappingsCB = new JCheckBox("Auto create ID Mappings file.");
                    MainPanel.add(autoCreateIdMappingsCB, "3, 9, 3, 1");
                    autoCreateIdMappingsCB.setActionCommand("" + UIAction.OPTION_AUTO_CREATE_ID_MAPPINGS);
                    autoCreateIdMappingsCB.addActionListener(controller);
                }
                {
                    autoExtractMetaDataCB = new JCheckBox("Auto extract meta-data from dicom files.");
                    MainPanel.add(autoExtractMetaDataCB, "3, 11, 3, 1");
                    autoExtractMetaDataCB.setActionCommand("" + UIAction.OPTION_AUTO_EXTRACT_META_DATA);
                    autoExtractMetaDataCB.addActionListener(controller);
                }
                {
                    imageCacheDirectoryLbl = new JLabel();
                    MainPanel.add(imageCacheDirectoryLbl, "2, 13, 7, 1");
                    imageCacheDirectoryLbl.setText("Image Cache Directory and Caching Options");
                    imageCacheDirectoryLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                }
                {
                    imageCacheDirTF = new LocationSelectionField(LocationType.DirType);
                    MainPanel.add(imageCacheDirTF, "3, 15, 4, 1");
                    imageCacheDirTF.setLocationText("<Image Cache Directory>");
                    imageCacheDirTF.addLocationActionListener(controller);
                    imageCacheDirTF.addFocusListener(new FocusListener() {
                        public void focusGained(FocusEvent e)
                        {
                        }

                        public void focusLost(FocusEvent e)
                        {
                            getController().updateImageCacheDirChanged();
                        }
                    });

                    imageCacheDirTF.setLocationActionCommand("" + UIAction.FIELD_IMAGECACHEDIR_CHANGED);
                }
                {
                    keepProcessedDicomCB = new JCheckBox("Keep Processed Dicom files.");
                    MainPanel.add(keepProcessedDicomCB, "3, 17, 3, 1");
                    keepProcessedDicomCB.setActionCommand("" + UIAction.OPTION_CACHE_KEEP_PROCESSED_DICOM);
                    keepProcessedDicomCB.addActionListener(controller);
                }
                {
                    clearCacheBut = new JButton();
                    MainPanel.add(clearCacheBut, "5, 20");
                    clearCacheBut.setText("Clear Cache");
                    clearCacheBut.setActionCommand("" + UIAction.CLEAR_IMAGECACHEDIR);
                    clearCacheBut.addActionListener(controller);
                }
                {
                    createDatasetsConfigDirBut = new JButton();
                    MainPanel.add(createDatasetsConfigDirBut, "8, 7");
                    createDatasetsConfigDirBut.setText("Create");
                    createDatasetsConfigDirBut.setActionCommand("" + UIAction.CONFIG_CREATE_CONFIGSDIR);
                    createDatasetsConfigDirBut.addActionListener(controller);
                }
                {
                    createImageCacheDirBut = new JButton();
                    MainPanel.add(createImageCacheDirBut, "8, 15");
                    createImageCacheDirBut.setText("Create");
                    createImageCacheDirBut.setActionCommand("" + UIAction.CONFIG_CREATE_IMAGECACHEDIR);
                    createImageCacheDirBut.addActionListener(controller);
                }
            }
            {
                buttonPanel = new JPanel();
                getContentPane().add(buttonPanel, BorderLayout.SOUTH);
                buttonPanel.setEnabled(false);
                buttonPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                {
                    cancelBut = new JButton();
                    buttonPanel.add(cancelBut);
                    cancelBut.setText("Cancel");
                    cancelBut.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            controller.doCancel();
                        }
                    });
                }
                {
                    okBut = new JButton();
                    buttonPanel.add(okBut);
                    okBut.setText("Ok");
                    okBut.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e)
                        {
                            controller.doOk();
                        }
                    });
                }
            }

            this.validate();
            Dimension size = this.getPreferredSize();
            size.width = 800;
            size.height += 64;
            this.setSize(size);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String getHelpText(boolean firstRun)
    {
        String txt = "";
        if (firstRun)
        {
            txt += txt + firstRunText;

        }
        txt += this.configDialogText;
        return txt;
    }

    public String getImageCacheDirText()
    {
        return this.imageCacheDirTF.getLocationText();
    }

    public String getDataSetConfigDirText()
    {
        return datasetConfigDirTF.getLocationText();
    }

    public ConfigDialogController getController()
    {
        return this.controller;
    }

    public void setDatasetsConfigDir(String location)
    {
        datasetConfigDirTF.setLocationText(location);
    }

    public void setImageCacheDir(String location)
    {
        this.imageCacheDirTF.setLocationText(location);
    }

    public boolean getAutoCreateDirs()
    {
        return true;
    }

}

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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.xnattool.DataSetConfig;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class KeyCreateDialog extends javax.swing.JDialog
{
    private static final long serialVersionUID = -1;

    private JPanel mainPanel;

    private JPanel midKeyInfoPanel;

    private JPanel lowPanel;

    private JLabel dataSetLbl;

    private JLabel ownerIdLBL;

    private JLabel keySourceTextLabl;

    private JLabel actualKeyLbl;

    private JScrollPane mainTestSP;

    private JTextField ownerIDTF;

    private JTextField dataSetTF;

    private JTextArea topTextArea;

    private JPanel topPanel;

    private KeyCreateDialogController controller;

    // ---
    protected JTextField rawKeyStatusTF;

    JPasswordField passphraseTF;

    protected JCheckBox showPPCB;

    protected JCheckBox showRawKeyCB;

    private JButton createKeyBut;

    // ---

    protected JButton cancelBut;

    protected JButton okBut;

    private byte[] rawKey;

    private boolean createNewKey = false;

    private JPanel createButPanel;

    private XnatToolPanelController masterController;

    private boolean exitOk = false;

    public KeyCreateDialog(XnatToolPanelController masterController, JFrame frame, boolean createNewKey)
    {
        super(frame);
        this.masterController = masterController;
        this.createNewKey = createNewKey;
        initGUI();
    }

    public KeyCreateDialog(JFrame frame)
    {
        super(frame);
        this.createNewKey = false;
        initGUI();
    }

    private void initGUI()
    {
        this.controller = new KeyCreateDialogController(this);

        try
        {
            {
                mainPanel = new JPanel();
                BorderLayout mainPanelLayout = new BorderLayout();
                mainPanel.setLayout(mainPanelLayout);
                getContentPane().add(mainPanel, BorderLayout.CENTER);
                mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                mainPanel.setPreferredSize(new java.awt.Dimension(720, 289));
                {
                    topPanel = new JPanel();
                    BoxLayout topPanelLayout = new BoxLayout(topPanel, javax.swing.BoxLayout.X_AXIS);
                    mainPanel.add(topPanel, BorderLayout.NORTH);
                    topPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                    topPanel.setLayout(topPanelLayout);
                    {
                        topTextArea = new JTextArea();
                        topPanel.add(topTextArea);
                        topTextArea.setText("Create (New) DataSet Key");
                        topTextArea.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                        topTextArea.setEditable(false);
                    }
                }
                {
                    lowPanel = new JPanel();
                    FlowLayout lowPanelLayout = new FlowLayout();
                    mainPanel.add(lowPanel, BorderLayout.SOUTH);
                    lowPanel.setLayout(lowPanelLayout);
                    {
                        okBut = new JButton();
                        lowPanel.add(okBut);
                        okBut.setText("OK");
                        okBut.setEnabled(false);
                        okBut.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e)
                            {
                                exitOk(true);
                            }
                        });
                    }
                    {
                        cancelBut = new JButton();
                        lowPanel.add(cancelBut);
                        cancelBut.setText("Cancel");
                        cancelBut.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e)
                            {
                                rawKey = null;
                                exitOk(false);
                            }
                        });
                    }
                }
                {
                    midKeyInfoPanel = new JPanel();
                    mainPanel.add(midKeyInfoPanel, BorderLayout.CENTER);
                    FormLayout midKeyInfoPanelLayout = new FormLayout(
                            "5dlu, 64dlu, 5dlu, d, 128dlu, 32dlu:grow, 5dlu",
                            "d, d, d, d, d, d, 5dlu, max(p;15dlu), max(p;15dlu), d, max(p;15dlu), 5dlu, d, d, max(p;15dlu), max(p;15dlu), d");
                    midKeyInfoPanel.setLayout(midKeyInfoPanelLayout);
                    midKeyInfoPanel.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
                    midKeyInfoPanel.setPreferredSize(new java.awt.Dimension(570, 218));
                    {
                        mainTestSP = new JScrollPane();
                        midKeyInfoPanel.add(mainTestSP, new CellConstraints("2, 4, 3, 2, default, default"));
                    }
                    {
                        keySourceTextLabl = new JLabel();
                        midKeyInfoPanel.add(keySourceTextLabl, new CellConstraints("2, 10, 1, 1, default, default"));
                        keySourceTextLabl.setText("Passphrase:");
                        keySourceTextLabl.setEnabled(true);
                    }
                    {
                        actualKeyLbl = new JLabel();
                        midKeyInfoPanel.add(actualKeyLbl, new CellConstraints("2, 13, 1, 1, default, default"));
                        actualKeyLbl.setText("Key Status:");
                    }
                    {
                        rawKeyStatusTF = new JTextField();
                        midKeyInfoPanel.add(rawKeyStatusTF, new CellConstraints("5, 13, 2, 1, default, default"));
                        rawKeyStatusTF.setText("<Key Status>");
                        rawKeyStatusTF.setEditable(false);
                        rawKeyStatusTF.addActionListener(this.controller);
                    }
                    {
                        passphraseTF = new JPasswordField();
                        midKeyInfoPanel.add(passphraseTF, new CellConstraints("5, 10, 2, 1, default, default"));
                        passphraseTF.addActionListener(this.controller);
                    }
                    {
                        showPPCB = new JCheckBox();
                        midKeyInfoPanel.add(showPPCB, new CellConstraints("5, 11, 1, 1, default, default"));
                        showPPCB.setText("Show Passphrase");
                        showPPCB.addActionListener(this.controller);
                    }
                    {
                        ownerIdLBL = new JLabel();
                        midKeyInfoPanel.add(ownerIdLBL, new CellConstraints("2, 9, 1, 1, default, default"));
                        ownerIdLBL.setText("Owner ID:");
                    }
                    {
                        dataSetLbl = new JLabel();
                        midKeyInfoPanel.add(dataSetLbl, new CellConstraints("2, 8, 1, 1, default, default"));
                        dataSetLbl.setText("DataSet:");
                    }
                    {
                        showRawKeyCB = new JCheckBox();
                        midKeyInfoPanel.add(showRawKeyCB, new CellConstraints("5, 15, 1, 1, default, default"));
                        showRawKeyCB.setText("Show Raw Key");
                        showRawKeyCB.addActionListener(this.controller);
                    }
                    {
                        dataSetTF = new JTextField();
                        midKeyInfoPanel.add(dataSetTF, new CellConstraints("5, 8, 1, 1, default, default"));
                        dataSetTF.setText("<DataSet>");
                        dataSetTF.setEditable(false);
                    }
                    {
                        ownerIDTF = new JTextField();
                        midKeyInfoPanel.add(ownerIDTF, new CellConstraints("5, 9, 1, 1, default, default"));
                        midKeyInfoPanel.add(getCreateButPanel(), new CellConstraints("2, 16, 6, 1, default, default"));
                        ownerIDTF.setText("<Owner ID>");
                        ownerIDTF.setEditable(false);
                    }
                }
            }
            this.setSize(714, 323);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void setTitle(String title)
    {
        super.setTitle(title);
        this.topTextArea.setText(title);
    }

    protected JPasswordField getPassphraseField()
    {
        return passphraseTF;
    }

    protected void togglePasswordField(boolean show)
    {
        // toggle:
        char c = getPassphraseField().getEchoChar();

        if (show)
        {
            getPassphraseField().setEchoChar((char) 0);
        }
        else
        {
            getPassphraseField().setEchoChar('*');
        }
    }

    public void setKey(byte bytes[], boolean show)
    {
        this.rawKey = bytes;
        showKey(show);
    }

    protected void showKey(boolean show)
    {
        if (rawKey == null)
        {
            this.rawKeyStatusTF.setText("No Key!");
            return;
        }

        if (rawKey.length < 8)
        {
            this.rawKeyStatusTF.setText("*** Error Key is to Short");
            return;
        }

        if (show)
        {
            this.rawKeyStatusTF.setText(StringUtil.toHexString(rawKey, true));
        }
        else
        {
            this.rawKeyStatusTF.setText("Key Created!");
        }
    }

    protected byte[] getRawKey()
    {
        return this.rawKey;
    }

    public void exitOk(boolean ok)
    {
        this.setVisible(false);
        this.exitOk = ok;
        if (!ok)
        {
            this.passphraseTF.setText(null); // clear;
        }
    }

    public boolean getExitOK()
    {
        return exitOk;
    }

    public XnatToolPanelController getMasterController()
    {
        return this.masterController;
    }

    public Secret getPassphraseSecret()
    {
        return new Secret(this.passphraseTF.getPassword());
    }

    // ===
    // Static
    // ===

    public static KeyCreateDialog showDialog(XnatToolPanelController masterController, String title, boolean createNewKey, boolean isModal)
    {
        JFrame frame = masterController.getMainPanelJFrame();

        KeyCreateDialog inst = new KeyCreateDialog(masterController, frame, createNewKey);
        inst.setTitle(title);

        if (masterController != null)
        {
            inst.updateDataSetFieldsFromMasterController();
        }

        inst.setLocationRelativeTo(frame);
        inst.setModal(isModal);
        // starts dialog
        inst.setVisible(true);

        // after exit() or dialog isn't modal:
        if (isModal)
        {
            ; //
        }

        return inst;
    }

    private void updateDataSetFieldsFromMasterController()
    {
        DataSetConfig config = this.masterController.getDataSetConfig();

        if (config == null)
        {
            dataSetTF.setText("<No Data Set Selected!>");
            ownerIDTF.setText("<?>");
            return;
        }

        this.dataSetTF.setText(config.getDataSetName());
        this.ownerIDTF.setText(config.getSourceId());
        this.setKey(config.getEncryptionKey(), false);

        Secret secret = this.masterController.getPassphrase();
        this.passphraseTF.setText(new String(secret.getChars()));
    }

    /**
     * Auto-generated main method to display this JDialog
     */
    public static void main(String[] args)
    {
        KeyCreateDialog dialog = showDialog(null, "Create new key", true, true);
        byte[] key = dialog.getRawKey();
        System.err.println("keys=" + StringUtil.toHexString(key));

    }

    JButton getCreateKeyBut()
    {
        if (createKeyBut == null)
        {
            createKeyBut = new JButton();
            createKeyBut.setText("Create New Key");
            createKeyBut.addActionListener(this.controller);
        }
        return createKeyBut;
    }

    private JPanel getCreateButPanel()
    {
        if (createButPanel == null)
        {
            createButPanel = new JPanel();
            createButPanel.add(getCreateKeyBut());
        }
        return createButPanel;
    }

    public void setPassphrase(Secret secret)
    {
        this.passphraseTF.setText(new String(secret.getChars()));
    }

}

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
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField;
import nl.esciencecenter.ptk.ui.widgets.LocationSelectionField.LocationType;
import nl.esciencecenter.ptk.ui.widgets.StringSelectionComboBox;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class UploadReconDialog extends javax.swing.JDialog
{
    private static final long serialVersionUID = 7538461485025833359L;

    private JTextField uploadInfoTF;

    private JLabel reconstrucionOptionsLbl;

    private JLabel dataSetLbl;

    private JLabel typeLbl;

    private JLabel xnatMappingFields;

    private JTextField setIdLbl;

    private JTextField jTextField2;

    private JTextField jTextField1;

    private JTextField dataSetTF;

    private JLabel fileIdLbl;

    private JComboBox reconTypeCB;

    private JLabel fileLbl;

    private JButton cancelBut;

    private JButton uploadBut;

    private JPanel buttonsPnl;

    private JLabel sessionIdLbl;

    private JLabel subjectIdLbl;

    private JLabel ProjectLbl;

    private LocationSelectionField fileLocationFF;

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
                UploadReconDialog inst = new UploadReconDialog(frame);
                inst.setVisible(true);
            }
        });
    }

    public UploadReconDialog(JFrame frame)
    {
        super(frame);
        initGUI();
    }

    private void initGUI()
    {
        try
        {
            FormLayout thisLayout = new FormLayout(
                    "max(p;5dlu), 69dlu, max(p;5dlu), 64dlu, 5dlu, 65dlu, 5dlu, 95dlu:grow, 5dlu",
                    "max(p;5dlu), 17dlu, max(p;5dlu), 16dlu, 5dlu, max(p;15dlu), max(p;15dlu), 5dlu, max(p;15dlu), 5dlu, max(p;15dlu), max(p;15dlu), max(p;15dlu), 17dlu, max(p;15dlu), 7dlu, max(p;15dlu)");
            getContentPane().setLayout(thisLayout);
            {
                uploadInfoTF = new JTextField();
                getContentPane().add(uploadInfoTF, new CellConstraints("2, 2, 7, 1, default, default"));
                uploadInfoTF.setText("Upload Reconstruction/Atlas");
            }
            {
                reconstrucionOptionsLbl = new JLabel();
                getContentPane().add(reconstrucionOptionsLbl, new CellConstraints("2, 4, 7, 1, default, default"));
                reconstrucionOptionsLbl.setText("Reconstruction: ");
                reconstrucionOptionsLbl.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            {
                ProjectLbl = new JLabel();
                getContentPane().add(ProjectLbl, new CellConstraints("2, 11, 1, 1, default, default"));
                ProjectLbl.setText("Project:");
            }
            {
                dataSetLbl = new JLabel();
                getContentPane().add(dataSetLbl, new CellConstraints("2, 12, 1, 1, default, default"));
                dataSetLbl.setText("DataSet:");
            }
            {
                subjectIdLbl = new JLabel();
                getContentPane().add(subjectIdLbl, new CellConstraints("2, 14, 1, 1, default, default"));
                subjectIdLbl.setText("Subject ID:");
            }
            {
                sessionIdLbl = new JLabel();
                getContentPane().add(sessionIdLbl, new CellConstraints("2, 13, 1, 1, default, default"));
                sessionIdLbl.setText("Session ID: ");
            }
            {
                buttonsPnl = new JPanel();
                getContentPane().add(buttonsPnl, new CellConstraints("2, 17, 7, 1, default, default"));
                {
                    uploadBut = new JButton();
                    buttonsPnl.add(uploadBut);
                    uploadBut.setText("Upload");
                }
                {
                    cancelBut = new JButton();
                    buttonsPnl.add(cancelBut);
                    cancelBut.setText("Cancel");
                }
            }
            {
                fileLbl = new JLabel();
                getContentPane().add(fileLbl, new CellConstraints("2, 6, 1, 1, default, default"));
                fileLbl.setText("File:");
            }
            {
                fileLocationFF = new LocationSelectionField(LocationType.FileType);
                getContentPane().add(fileLocationFF, new CellConstraints("4, 6, 5, 1, default, default"));
            }
            {
                typeLbl = new JLabel();
                getContentPane().add(typeLbl, new CellConstraints("2, 7, 1, 1, default, default"));
                typeLbl.setText("Type;");
            }
            {
                reconTypeCB = new StringSelectionComboBox(new String[] {
                        "Atlas", "Reconstruction"
                });
                getContentPane().add(reconTypeCB, new CellConstraints("4, 7, 1, 1, default, default"));
            }
            {
                fileIdLbl = new JLabel();
                getContentPane().add(fileIdLbl, new CellConstraints("2, 15, 1, 1, default, default"));
                fileIdLbl.setText("File ID:");
            }
            {
                dataSetTF = new JTextField();
                getContentPane().add(dataSetTF, new CellConstraints("4, 12, 3, 1, default, default"));
                dataSetTF.setText("<DataSet>");
            }
            {
                jTextField1 = new JTextField();
                getContentPane().add(jTextField1, new CellConstraints("4, 13, 3, 1, default, default"));
                jTextField1.setText("<Session ID> ");
            }
            {
                jTextField2 = new JTextField();
                getContentPane().add(jTextField2, new CellConstraints("4, 14, 3, 1, default, default"));
                jTextField2.setText("<Subject ID>");
            }
            {
                setIdLbl = new JTextField();
                getContentPane().add(setIdLbl, new CellConstraints("4, 15, 3, 1, default, default"));
                setIdLbl.setText("<Set ID>");
            }
            {
                xnatMappingFields = new JLabel();
                getContentPane().add(xnatMappingFields, new CellConstraints("2, 9, 7, 1, default, default"));
                xnatMappingFields.setText("XNAT Mapping");
                xnatMappingFields.setBorder(BorderFactory.createEtchedBorder(BevelBorder.LOWERED));
            }
            this.setSize(725, 374);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}

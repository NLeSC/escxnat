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

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.WindowConstants;

public class XnatToolFrame extends JFrame
{
    // === //

    private static final long serialVersionUID = -8637429660885223824L;

    private XnatToolMainPanel uploadPanel;

    private JMenuBar menuBar;

    private JMenuItem helpMI;

    private JMenuItem configurationMI;

    private JSeparator helpSep1;

    private JMenu debugMenu;

    private JMenuItem aboutMI;

    private JMenu helpMenu;

    private JMenu dataMenu;

    private JMenu m;

    private JMenu locationMenu;

    private XnatToolFrameController frameController;

    private JMenuItem dataMI;

    private JMenu locationsMenu;

    private JMenuItem defaultLocationMI;

    public XnatToolFrame()
    {
        super();
        initGui();
    }

    protected void initGui()
    {
        this.uploadPanel = new XnatToolMainPanel(this);
        getContentPane().add(uploadPanel);

        this.frameController = new XnatToolFrameController(this);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        {
            this.menuBar = this.createMenuBar();
            setJMenuBar(menuBar);
        }

        pack();
        Dimension size = this.getPreferredSize();
        size.width += 32;
        size.height += 32;
        this.setSize(size);

    }

    protected JMenuBar createMenuBar()
    {
        if (menuBar != null)
        {
            return menuBar;
        }

        menuBar = new JMenuBar();

        {
            locationMenu = new JMenu();
            menuBar.add(locationMenu);
            locationMenu.setText("Location");
            {
                locationsMenu = new JMenu();
                locationMenu.add(locationsMenu);
                locationsMenu.setText("Locations");
                {
                    defaultLocationMI = new JMenuItem();
                    locationsMenu.add(defaultLocationMI);
                    defaultLocationMI.setText("<Default>");
                    defaultLocationMI.setActionCommand("" + UIAction.MAIN_LOCATION_SET_DEFAULT);
                    defaultLocationMI.addActionListener(frameController);
                }
            }

            locationMenu.add(new JSeparator());

            {
                JMenuItem mi = new JMenuItem("Exit");
                mi.setActionCommand("" + UIAction.ACTION_EXIT);
                locationMenu.add(mi);
                mi.addActionListener(frameController);
            }
        }
        {
            m = new JMenu();
            menuBar.add(m);
            m.setText("Settings");
            {
                configurationMI = new JMenuItem();
                m.add(configurationMI);
                configurationMI.setText("Tool Configuration");
                configurationMI.setActionCommand("" + UIAction.MAIN_CONFIGURATION);
                configurationMI.addActionListener(frameController);
            }
        }
        {
            dataMenu = new JMenu();
            menuBar.add(dataMenu);
            dataMenu.setText("Data");
            {

                {
                    dataMI = new JMenuItem();
                    dataMenu.add(dataMI);
                    dataMI.setText("DataSet");
                    dataMI.addActionListener(frameController);
                    dataMI.setEnabled(false);
                }

                // Meta Data -> upload CSV
                {
                    JMenu metaDataMenu = new JMenu();
                    dataMenu.add(metaDataMenu);
                    metaDataMenu.setText("Meta Data");
                    metaDataMenu.addActionListener(frameController);
                    metaDataMenu.setEnabled(true);
                    {
                        JMenuItem uploadCsvMI = new JMenuItem();
                        metaDataMenu.add(uploadCsvMI);
                        uploadCsvMI.setText("Upload CSV to Poject");
                        uploadCsvMI.setActionCommand("" + UIAction.UPLOAD_CSV_TO_PROJECT);
                        uploadCsvMI.addActionListener(frameController);
                        uploadCsvMI.setEnabled(true);
                    }
                }

            }

        }
        {
            helpMenu = new JMenu();
            menuBar.add(helpMenu);
            helpMenu.setText("Help");
            {
                helpMI = new JMenuItem();
                helpMenu.add(helpMI);
                helpMI.setText("Help");
                helpMI.setActionCommand("" + UIAction.MAIN_MENU_HELP);
                helpMI.addActionListener(frameController);
            }
            {
                aboutMI = new JMenuItem();
                helpMenu.add(aboutMI);
                aboutMI.setText("About");
                aboutMI.setActionCommand("" + UIAction.MAIN_MENU_ABOUT);
                aboutMI.addActionListener(frameController);
            }
            {
                helpSep1 = new JSeparator();
                helpMenu.add(helpSep1);
            }
            {
                debugMenu = new JMenu();
                helpMenu.add(debugMenu);
                debugMenu.setText("Debug");
                {
                    JMenuItem menuItem = new JMenuItem();
                    debugMenu.add(menuItem);
                    menuItem.setText("On");
                    menuItem.setActionCommand("" + UIAction.MAIN_DEBUGLEVEL_TO_DEBUG);
                    menuItem.addActionListener(frameController);
                }
                {
                    JMenuItem menuItem = new JMenuItem();
                    debugMenu.add(menuItem);
                    menuItem.setText("Info");
                    menuItem.setActionCommand("" + UIAction.MAIN_DEBUGLEVEL_TO_INFO);
                    menuItem.addActionListener(frameController);
                }

                {
                    JMenuItem menuItem = new JMenuItem();
                    debugMenu.add(menuItem);
                    menuItem.setText("Off");
                    menuItem.setActionCommand("" + UIAction.MAIN_DEBUGLEVEL_TO_ERROR);
                    menuItem.addActionListener(frameController);
                }
            }
        }

        return menuBar;

    }

    public static void main(String args[])
    {
        XnatToolFrame frame = new XnatToolFrame();
        frame.setVisible(true);
    }

    public XnatToolPanelController getUploaderController()
    {
        return this.uploadPanel.getController();
    }

    public XnatToolMainPanel getUploaderPanel()
    {
        return this.uploadPanel;
    }

    public XnatToolPanelController getMasterController()
    {
        return uploadPanel.getController();
    }
}

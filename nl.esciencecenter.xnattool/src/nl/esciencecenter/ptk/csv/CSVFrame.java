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

package nl.esciencecenter.ptk.csv;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class CSVFrame extends JFrame
{
    private static final long serialVersionUID = -5815939015184884637L;

    // --- instance --- //

    private CSVData csVdata;

    private JPanel mainPanel;

    private JScrollPane scrollPane;

    public CSVFrame(CSVData csvData)
    {
        this.csVdata = csvData;
        initGui();
        updateData(csvData);
    }

    protected void initGui()
    {
        {
            mainPanel = new JPanel();
            this.add(mainPanel);
            mainPanel.setLayout(new BorderLayout());
            {
                scrollPane = new JScrollPane();
                mainPanel.add(scrollPane, BorderLayout.CENTER);
            }
        }

        // this.addWindowListener(this);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public void updateData(CSVData data)
    {
        CSVTable table = new CSVTable(data);
        scrollPane.setViewportView(table);
    }

    public static void viewData(CSVData data)
    {
        CSVFrame jframe = new CSVFrame(data);
        jframe.setSize(new Dimension(800, 600));
        jframe.setVisible(true);
    }

}

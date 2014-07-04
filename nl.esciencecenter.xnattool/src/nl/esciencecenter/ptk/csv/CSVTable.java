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

import java.util.List;

import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class CSVTable extends JTable
{
    private static final long serialVersionUID = 7035919101558931780L;

    public CSVTable(CSVData data)
    {
        // super(new CSVDataModel(null));
        initData(data);
    }

    protected void initData(CSVData data)
    {
        this.setAutoCreateColumnsFromModel(false);
        this.setModel(new CSVDataModel(data));

        if (this.getAutoCreateColumnsFromModel() == false)
        {
            initColumns();
        }

        initHeaders();
    }

    private void initColumns()
    {
        TableColumnModel colModel = new DefaultTableColumnModel();
        CSVDataModel model = getCSVModel();

        if (model == null)
            return;

        for (int c = 0; c < this.getCSVModel().getColumnCount(); c++)
        {
            TableColumn column = new TableColumn(c);
            colModel.addColumn(column);
        }

        this.setColumnModel(colModel);
    }

    public CSVDataModel getCSVModel()
    {
        return (CSVDataModel) super.getModel();
    }

    private void initHeaders()
    {
        TableColumnModel colModel = this.getColumnModel();
        List<String> headers = this.getCSVModel().getHeaderNames();

        for (int h = 0; h < headers.size(); h++)
        {
            colModel.getColumn(h).setHeaderValue(headers.get(h));
        }
    }

}

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

import javax.swing.table.AbstractTableModel;

public class CSVDataModel extends AbstractTableModel
{
    private static final long serialVersionUID = 2344960829430541557L;

    private CSVData data;

    public CSVDataModel(CSVData data)
    {
        this.data = data;
    }

    @Override
    public int getRowCount()
    {
        if (data == null)
            return 0;
        return data.getNrOfRows();
    }

    @Override
    public int getColumnCount()
    {
        if (data == null)
            return 0;
        return data.getNrOfColumns();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        if (data == null)
            return null;

        String[] row = data.getRow(rowIndex);

        if (row == null)
            return null;

        return row[columnIndex];

    }

    public String getColumnName(int colNr)
    {
        if (data == null)
            super.getColumnName(colNr);

        return data.getHeaderName(colNr);
    }

    public List<String> getHeaderNames()
    {
        if (data == null)
            return null;
        return data.getHeaders();
    }

}

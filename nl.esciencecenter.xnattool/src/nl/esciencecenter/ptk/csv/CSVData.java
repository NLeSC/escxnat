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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.util.ResourceLoader;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;

/**
 * CSVData Reader/Writer utility.
 */
public class CSVData
{
    private static ClassLogger logger = ClassLogger.getLogger(CSVData.class);

    // ===

    protected String fieldSeparators[] = null;

    protected List<String> headerList = null;

    protected ArrayList<String[]> data = null;

    protected boolean isEditable = false;

    public CSVData()
    {
    }

    public CSVData(List<String> headerList, List<String[]> data)
    {
        this.headerList = headerList;
        this.data = new ArrayList<String[]>(data.size());
        // copy data:
        for (int i = 0; i < data.size(); i++)
        {
            addRow(data.get(i));
        }
    }

    public CSVData(boolean isEditable)
    {
        this.isEditable = isEditable;
    }

    /**
     * Add new row to data. This data structure must be editable.
     */
    protected int addNewRow()
    {
        assertEditable();

        if (data == null)
        {
            // auto init;
            data = new ArrayList<String[]>();
        }

        synchronized (data)
        {
            int rowNr = data.size();
            data.add(new String[headerList.size()]);
            return rowNr;
        }

    }

    public void addRow(String[] row)
    {
        this.assertEditable();

        int rowNr = this.addNewRow();
        this.setRow(rowNr, row);

    }

    public void setRow(int rowNr, String[] sourceRow)
    {
        this.assertEditable();

        if (data == null)
        {
            throw new Error("No Data. Create Row first!");
        }

        if (this.data.size() < rowNr)
        {
            throw new Error("Row number to high:" + rowNr + ">" + data.size());
        }

        synchronized (data)
        {
            String newRow[] = new String[sourceRow.length];
            System.arraycopy(sourceRow, 0, newRow, 0, sourceRow.length);
            data.set(rowNr, newRow);
        }

    }

    public void setHeaders(String fieldNames[])
    {
        this.assertEditable();
        this.headerList = new StringList(fieldNames);
    }

    public void setField(int rowNr, String fieldName, String fieldValue)
    {
        this.assertEditable();
        setField(rowNr, this.getFieldNr(fieldName), fieldValue);
    }

    public void setField(int rowNr, int fieldNr, String fieldValue)
    {
        String row[] = data.get(rowNr);
        if (row == null)
            throw new Error("No such row:" + rowNr);

        row[fieldNr] = fieldValue;
    }

    /**
     * Set field separator if not a comma ','.
     */
    public void setFieldSeparator(String str)
    {
        this.fieldSeparators = new String[] {
                str
        };
    }

    /**
     * Set field Seperators. First seperator in the array is the default one.
     * 
     * @param seperators
     *            Array of seperator Strings.
     */
    public void setFieldSeparators(String[] seperators)
    {
        this.fieldSeparators = seperators;
    }

    public void readFile(URI file) throws MalformedURLException, IOException
    {
        ResourceLoader loader = ResourceLoader.getDefault();
        String txt = loader.readText(file.toURL());
        this.parseText(txt);
    }

    public void readFile(String filename) throws IOException
    {
        ResourceLoader loader = ResourceLoader.getDefault();
        String txt = loader.readText(loader.resolveUrl(filename));
        this.parseText(txt);
    }

    public void parseText(String csvText) throws IOException
    {
        // Extended CSV !
        // Pass I: remove comments including the ending newline!
        Pattern pat = Pattern.compile("^#.*\n", Pattern.MULTILINE);
        csvText = pat.matcher(csvText).replaceAll("");

        // todo: check how jackson can parse alternative field separators;
        if (fieldSeparators != null)
        {
            // csvText=csvText.replaceAll(",","_");

            for (String sep : fieldSeparators)
            {
                // lazy replace
                csvText = csvText.replaceAll(sep, ",");
            }
        }

        // Not needed: Pass II: remove empty lines as a result of the
        // pat=Pattern.compile("\n\n",Pattern.MULTILINE);
        // newTxt=pat.matcher(newTxt).replaceAll("");

        // ObjectMapper mapper=new ObjectMapper();
        CsvMapper mapper = new CsvMapper();
        mapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        MappingIterator<Object[]> it = mapper.reader(Object[].class).readValues(csvText);

        if (it.hasNext() == false)
        {
            throw new IOException("Empty text or csv text contains no headers!");
        }

        // read header:
        Object headers[] = it.next();

        StringList list = new StringList();
        for (int i = 0; i < headers.length; i++)
        {
            list.add(headers[i].toString());
        }

        logger.debugPrintf("Headers=%s\n", list.toString("<>"));
        headerList = list;

        data = new ArrayList<String[]>();

        // check header values.
        while (it.hasNext())
        {
            Object line[] = it.next();
            String row[] = new String[line.length];

            for (int j = 0; j < line.length; j++)
            {
                Object value = line[j];
                if (value != null)
                {
                    row[j] = value.toString();
                }
            }
            data.add(row);
        }

        logger.debugPrintf("Read %d number of rows\n", data.size());
    }

    public int getNrOfRows()
    {
        if (this.data == null)
            return 0;
        return data.size();
    }

    public int getNrOfColumns()
    {
        if ((this.headerList == null) || (headerList.size() <= 0))
            return 0;

        return this.headerList.size();
    }

    public List<String[]> getData()
    {
        return data;
    }

    public int getFieldNr(String fieldName)
    {
        if ((this.headerList == null) || (this.headerList.size() <= 0))
            return -1;

        return headerList.indexOf(fieldName);
    }

    public String[] getRow(int row)
    {
        return data.get(row);
    }

    public String get(int rowNr, int columnNr)
    {
        String row[] = data.get(rowNr);
        return row[columnNr];
    }

    public String get(int rowNr, String fieldName)
    {
        String row[] = data.get(rowNr);
        int field = getFieldNr(fieldName);
        if ((field < 0) || (field >= row.length))
            return null;

        return row[field];
    }

    /**
     * Returns field value as string. Whitespace is stripped. If the value contains only whitespace a null value is
     * returned.
     */
    public String getField(int row, String fieldName, boolean noWhiteSpace)
    {
        String value = get(row, fieldName);
        if (value == null)
            return null;

        if (noWhiteSpace == false)
            return value;

        if (StringUtil.isWhiteSpace(value))
            return null;

        return StringUtil.stripWhiteSpace(value);
    }

    public List<String> getHeaders()
    {
        return this.headerList;
    }

    public int getNrColumns(int row)
    {
        if (this.data == null)
            return -1;

        if (row >= data.size())
            return -1;

        String[] rowData = data.get(row);
        if (rowData == null)
            return -1;

        return rowData.length;
    }

    /**
     * Return row data as single string
     */
    public String getRowAsString(int row)
    {
        if (this.data == null)
            return null;

        if ((row < 0) || (row >= data.size()))
            return null;

        String[] rowData = data.get(row);

        if ((rowData == null) || (rowData.length <= 0))
            return null;

        String str = "";
        for (int i = 0; i < rowData.length; i++)
        {
            // do not quote empty Strings.
            if ((rowData[i] != null) && rowData[i] != "")
            {
                str = str + '"' + rowData[i] + '"';
            }

            if (i + 1 < rowData.length)
            {
                str += ";";
            }
        }
        return str;
    }

    public String toString()
    {
        return toString(",", "\n");
    }

    public String toString(String fieldSeperator, String lineSeperator)
    {
        StringBuilder sb = new StringBuilder();
        toString(sb, fieldSeperator, lineSeperator);
        return sb.toString();
    }

    public void toString(StringBuilder sb, String fieldSeperator, String lineSeperator)
    {
        CSVWriter writer = new CSVWriter(sb);
        writer.setFieldSeperator(fieldSeperator);
        writer.setLineSeperator(lineSeperator);

        String[] headers = this.headerList.toArray(new String[] {});
        writer.writeHeaders(headers);
        for (int row = 0; row < this.getNrOfRows(); row++)
        {
            writer.writeRow(this.getRow(row));
        }
    }

    protected void assertEditable()
    {
        if ((this.headerList == null) || (this.data == null))
        {
            // not data -> editable by default.
            // Only After data, the data can be read-only
            return;
        }

        if (this.isEditable == false)
        {
            throw new Error("Not editable Data!");
        }
    }

    public String toCSVDateString(Date date)
    {
        String dateStr = "";
        if (date != null)
            dateStr = Presentation.createNormalizedDateString(date);
        return dateStr;
    }

    public String getHeaderName(int colNr)
    {
        if (this.headerList == null)
            return null;
        return this.headerList.get(colNr);
    }

    public int getHeaderNr(String name)
    {
        if (this.headerList == null)
            return -1;

        for (int i = 0; i < headerList.size(); i++)
        {
            if (headerList.get(i).equals(name))
                return i;
        }
        return -1;
    }

}

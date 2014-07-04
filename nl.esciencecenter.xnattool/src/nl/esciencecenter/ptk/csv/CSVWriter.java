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

import java.util.Date;

import nl.esciencecenter.ptk.presentation.Presentation;

/**
 * CSV File create util. Todo: use Jackson API.
 */
class CSVWriter
{
    protected StringBuilder stringBuilder = null;

    protected String fieldSeperator = ";";

    protected String lineSeperator = "\n";

    protected boolean quoteAll = true;

    public CSVWriter(StringBuilder sb)
    {
        this.stringBuilder = sb;
    }

    public CSVWriter writeHeaders(String[] fieldNames)
    {
        for (int i = 0; i < fieldNames.length; i++)
        {
            stringBuilder.append(fieldNames[i]);
            if (i + 1 < fieldNames.length)
                stringBuilder.append(fieldSeperator);
        }
        endLine();
        return this;
    }

    public CSVWriter writeDateField(Date date)
    {
        String dateStr = "";
        if (date != null)
            dateStr = Presentation.createNormalizedDateString(date);

        return writeField(dateStr);
    }

    /**
     * Write complete row of fields. Ends line with endline character.
     * 
     * @param values
     *            String array of values.
     */
    public CSVWriter writeRow(String values[])
    {
        writeFields(values);
        endLine();
        return this;
    }

    /**
     * Write array of fields, does not write endline character.
     */
    public CSVWriter writeFields(String values[])
    {
        if (values == null)
            return this;

        for (String value : values)
        {
            writeField(value);
        }
        return this;
    }

    public CSVWriter writeField(String value)
    {
        if (value == null)
            value = "";

        if (quoteAll)
        {
            stringBuilder.append('"' + value + '"');
        }
        else
        {
            stringBuilder.append(value);
        }

        stringBuilder.append(fieldSeperator);

        return this;
    }

    public void endLine()
    {
        stringBuilder.append(lineSeperator);
    }

    public StringBuilder getStringBuilder()
    {
        return stringBuilder;
    }

    public void setFieldSeperator(String fsString)
    {
        this.fieldSeperator = fsString;
    }

    public void setLineSeperator(String lsString)
    {
        this.lineSeperator = lsString;
    }

    public void setQuoteAll(boolean value)
    {
        this.quoteAll = value;
    }

}

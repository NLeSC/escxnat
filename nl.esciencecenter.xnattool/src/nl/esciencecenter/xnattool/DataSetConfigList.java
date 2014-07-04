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

package nl.esciencecenter.xnattool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.xml.XmlUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * List of DataSetConfigurations.
 * 
 * Separate class so that Jackson can properly load and save it to XML.
 * 
 */
public class DataSetConfigList
{

    public static DataSetConfigList loadFrom(String path) throws IOException
    {
        String xml = FSUtil.getDefault().readText(path);
        DataSetConfigList list = DataSetConfigList.parseXML(xml);
        list.postReadUpdate();
        return list;
    }

    public static DataSetConfigList parseXML(String xml) throws JsonParseException, JsonMappingException,
            IOException
    {
        ObjectMapper xmlMapper = new XmlMapper();
        DataSetConfigList value = xmlMapper.readValue(xml, DataSetConfigList.class);
        // check ?
        return value;
    }

    public static void saveTo(DataSetConfigList configs, String path) throws IOException
    {
        String xml = configs.toXML();
        xml = XmlUtil.prettyFormat(xml, 4);
        FSUtil.getDefault().writeText(path, xml);
    }

    // ========
    // Instance
    // ========

    @JacksonXmlProperty(localName = "dataSetConfigs")
    protected ArrayList<DataSetConfig> dataSetConfigs;

    public DataSetConfigList()
    {
        dataSetConfigs = new ArrayList<DataSetConfig>();
    }

    /**
     * @return actual list of DataSetConfigurations.
     */
    public List<DataSetConfig> list()
    {
        return dataSetConfigs;
    }

    // @JacksonXmlProperty(localName = "dataSetConfigs")
    public ArrayList<DataSetConfig> getDataSetConfigs()
    {
        return dataSetConfigs;
    }

    // @JacksonXmlProperty(localName = "dataSetConfigs")
    public void setDataSetConfigs(ArrayList<DataSetConfig> newConfigs)
    {
        dataSetConfigs = newConfigs;
    }

    @JsonIgnore
    public int add(DataSetConfig config)
    {
        dataSetConfigs.add(config);
        return dataSetConfigs.size();
    }

    @JsonIgnore
    public boolean hasDataSetConfig(String name)
    {
        return (getDataSetConfig(name) != null);
    }

    @JsonIgnore
    public DataSetConfig getDataSetConfig(String name)
    {
        for (int i = 0; i < dataSetConfigs.size(); i++)
        {
            if (StringUtil.compare(dataSetConfigs.get(i).getDataSetName(), name) == 0)
                return dataSetConfigs.get(i);
        }

        return null;
    }

    @JsonIgnore
    public List<String> getDataSetConfigNames()
    {
        StringList nameList = new StringList();

        for (int i = 0; i < dataSetConfigs.size(); i++)
        {
            nameList.add(dataSetConfigs.get(i).getDataSetName());
        }

        return nameList;
    }

    @JsonIgnore
    public String toXML() throws JsonProcessingException
    {
        XmlMapper xmlMapper = new XmlMapper();
        String xml = xmlMapper.writeValueAsString(this);
        return xml;
    }

    @JsonIgnore
    public int size()
    {
        return this.dataSetConfigs.size();
    }

    @JsonIgnore
    public DataSetConfig getDataSetConfig(int index)
    {
        return this.dataSetConfigs.get(index);
    }

    @JsonIgnore
    protected void postReadUpdate()
    {
        for (DataSetConfig conf : dataSetConfigs)
        {
            conf.postReadUpdate();
        }

    }

}

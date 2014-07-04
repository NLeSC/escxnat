package nl.esciencecenter.xnattool;

import nl.esciencecenter.ptk.xml.XmlUtil;
import nl.esciencecenter.xnattool.DataSetConfig;
import nl.esciencecenter.xnattool.DataSetConfigList;

import org.junit.Assert;
import org.junit.Test;

public class Test_DataSetConfig_ToXml
{
    @Test
    public void testDataSetsConfigToXml() throws Exception
    {
        DataSetConfig conf = Test_DataSetConfig.createDefault();
        testConfigToXML(conf);
    }

    protected void testConfigToXML(DataSetConfig conf) throws Exception
    {
        String xml = conf.toXML();
        DataSetConfig conf2 = DataSetConfig.parseXML(xml);

        Test_DataSetConfig.configAssertEqual("Parsed XML from original DataSetConfig doesn't match.", conf, conf2);

    }

    @Test
    public void testDataSetsConfigsToXml() throws Exception
    {
        DataSetConfigList confs = new DataSetConfigList();

        DataSetConfig conf1 = Test_DataSetConfig.createDefault();

        confs.add(conf1);
        DataSetConfig conf2 = Test_DataSetConfig.createDefault();

        confs.add(conf2);

        String xml = confs.toXML();
        System.out.println(">>>\n" + XmlUtil.prettyFormat(xml, 4));

        DataSetConfigList parsedConfs = DataSetConfigList.parseXML(xml);

        Assert.assertEquals("Parsed DataSetConfigList must contain two configurations", parsedConfs.size(), 2);

        for (int i = 0; i < parsedConfs.size(); i++)
        {
            Test_DataSetConfig.configAssertEqual("Parsed DataSetConfig #" + i + " mismatches original.", confs.getDataSetConfig(i),
                    parsedConfs.getDataSetConfig(i));
        }

    }
}

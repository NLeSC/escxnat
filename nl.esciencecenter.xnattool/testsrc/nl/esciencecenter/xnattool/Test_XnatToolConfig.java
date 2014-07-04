package nl.esciencecenter.xnattool;

import java.net.URI;

import org.junit.Assert;
import org.junit.Test;

import nl.esciencecenter.ptk.GlobalProperties;
import nl.esciencecenter.ptk.net.URIUtil;
import nl.esciencecenter.ptk.xml.XmlUtil;
import nl.esciencecenter.xnattool.XnatToolConfig;

public class Test_XnatToolConfig
{

    public XnatToolConfig createDefault() throws Exception
    {
        URI vri = new URI("http", null, "xnatws.esciencetest.nl", 80, "/escXnat", null, null);

        // defaults:
        XnatToolConfig conf = new XnatToolConfig(vri);

        conf.updateUser(GlobalProperties.getGlobalUserName());

        URI homeLoc = new URI("file://" + GlobalProperties.getGlobalUserHome());

        conf.setImageCacheDir(URIUtil.appendPath(homeLoc, "escxnat/cache"));
        conf.setDataSetsConfigDir(URIUtil.appendPath(homeLoc, "escxnat"));

        return conf;
    }

    @Test
    public void testConfigToXML() throws Exception
    {
        XnatToolConfig conf1 = createDefault();

        String xmlStr = conf1.toXML();

        System.out.println(XmlUtil.prettyFormat(xmlStr, 3));

        XnatToolConfig conf2 = XnatToolConfig.parseXML(xmlStr);

        // code insertion: conf2.updateUser("bogo");
        compare(conf1, conf2);

    }

    private void compare(XnatToolConfig conf1, XnatToolConfig conf2)
    {
        Assert.assertEquals("XNAT URIs don't match!", conf1.getXnatURI(), conf2.getXnatURI());
        Assert.assertEquals("XNAT User names don't match!", conf1.getXnatUser(), conf2.getXnatUser());

        Assert.assertEquals("XNAT Meta data dirs don't match!", conf1.getDataSetsConfigDir(), conf2.getDataSetsConfigDir());
        // Assert.assertEquals("XNAT Image source dirs don't match!",conf1.getImageSourceDir(),
        // conf2.getImageSourceDir());
        Assert.assertEquals("XNAT User Image Cache dirs don't match!", conf1.getImageCacheDir(), conf2.getImageCacheDir());

    }

}

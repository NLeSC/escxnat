package nl.esciencecenter.medim.dicom;

import org.junit.Assert;
import org.junit.Test;

import nl.esciencecenter.medim.dicom.types.DicomTags;

public class Test_DicomTags_readConfig
{

    @Test
    public void testReadDicomTagsXCSV() throws Exception
    {
        DicomTags tagdb = DicomTags.createFromFile("dicom/dicom_tags.xcsv");
        Assert.assertNotNull("DicomTags is null", tagdb);
    }

}

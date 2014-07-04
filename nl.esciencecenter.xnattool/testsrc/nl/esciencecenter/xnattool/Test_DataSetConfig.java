package nl.esciencecenter.xnattool;

import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.ScanKeyType;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.SessionKeyType;
import nl.esciencecenter.medim.dicom.DicomProcessingProfile.SubjectKeyType;
import nl.esciencecenter.ptk.data.StringHolder;

import org.junit.Assert;
import org.junit.Test;

public class Test_DataSetConfig
{
    public static final String SOURCEID = "sourceid";

    public static final String DATASETNAME = "default";

    public static final SubjectKeyType SUBJECTKEYTYPE = SubjectKeyType.CRYPTHASH_PATIENT_ID;

    public static final SessionKeyType SESSIONKEYTYPE = SessionKeyType.CRYPTHASH_STUDY_UID;

    public static final ScanKeyType SCANKEYTYPE = ScanKeyType.CRYPTHASH_SCAN_UID;

    public static final ImageTypes.DataSetType DATASETTYPE = ImageTypes.DataSetType.DICOM_SCANSET;

    public static final java.net.URI IMAGESOURCEDIR = null;

    public static DataSetConfig createDefault()
    {
        DataSetConfig config = new DataSetConfig();

        config.sourceId = SOURCEID;
        config.dataSetName = DATASETNAME;
        config.subjectKeyType = SUBJECTKEYTYPE;
        config.sessionKeyType = SESSIONKEYTYPE;
        config.scanKeyType = SCANKEYTYPE;
        config.dataSetType = DATASETTYPE;
        config.imageSourceDir = IMAGESOURCEDIR;

        return config;
    }

    public static void configAssertEqual(String mainMessage, DataSetConfig conf, DataSetConfig other) throws Exception
    {
        Assert.assertEquals(mainMessage + ":field DataSetName mismatches", conf.getDataSetName(), other.getDataSetName());
        Assert.assertEquals(mainMessage + ":field SourceID", conf.getSourceId(), other.getSourceId());
        Assert.assertEquals(mainMessage + ":field DataSetType", conf.getDataSetType(), other.getDataSetType());

        Assert.assertEquals(mainMessage + ":field ScanKeyType", conf.getScanKeyType(), other.getScanKeyType());
        Assert.assertEquals(mainMessage + ":field DataSetType", conf.getSessionKeyType(), other.getSessionKeyType());
        Assert.assertEquals(mainMessage + ":field DataSetType", conf.getSubjectKeyType(), other.getSubjectKeyType());
    }

    // =======
    // Testers
    // =======

    @Test
    public void testCreateDefault()
    {
        DataSetConfig config = createDefault();

        // defaults:
        Assert.assertEquals("Wrong default DataSetName", config.getDataSetName(), DATASETNAME);
        Assert.assertFalse("Default DataSetConfig may not have valid encryption key", config.hasValidEncryptionKey());

    }

    @Test
    public void testCompare() throws Exception
    {
        DataSetConfig conf1 = createDefault();
        DataSetConfig conf2 = createDefault();

        // test compare method itself:
        StringHolder reasonH = new StringHolder();
        configAssertEqual("Comparing datasets failed", conf1, conf2);

        // not matching:
        // conf2.setDataSetName("NoTheSame");
        // configAssertEqual("DataSetConfig compare failed. Configurations shouldn't match",conf1,conf2);

    }

}

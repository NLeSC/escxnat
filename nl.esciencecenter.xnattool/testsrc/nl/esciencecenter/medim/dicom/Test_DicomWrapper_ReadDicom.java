package nl.esciencecenter.medim.dicom;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class Test_DicomWrapper_ReadDicom
{

    @Test
    public void test_read_dicom1() throws IOException
    {
        java.net.URL dcmUrl = this.getClass().getClassLoader().getResource("tests/data/test_dicom1.dcm");
        testRead(dcmUrl.getPath());
    }

    protected void testRead(String filename) throws IOException
    {
        java.io.File file = new java.io.File(filename);

        boolean isDicom = DicomUtil.hasDicomMagic(file);
        Assert.assertTrue("Test file must have DICOM magic", isDicom);

        DicomWrapper wrap = DicomWrapper.readFrom(file.toURI());
        // DicomObject obj = wrap.getDicomObject();

        int expectedIDs[] = new int[] {
                1, 2, 1, 8
        };

        int ids[] = new int[4];
        wrap.getSetIdentificationNrs(ids);

        for (int i = 0; i < expectedIDs.length; i++)
        {
            Assert.assertEquals("Identifier #" + i + " from dicom test file doesn't match", expectedIDs[i], ids[i]);
        }

        // check default values:

        Assert.assertEquals("Field name 'Patient ID'   doesn't match", wrap.getPatientName(), "SD VC-003M");
        Assert.assertEquals("Field name 'Patient Name' doesn't match", wrap.getPatientID(), "SD VC-003M");
        Assert.assertEquals("Field name 'Study UID'    doesn't match", wrap.getStudyInstanceUID(), "1.3.6.1.4.1.9328.50.6.554");
        Assert.assertEquals("Field name 'Series UID'   doesn't match", wrap.getSeriesInstanceUID(), "1.3.6.1.4.1.9328.50.6.1008");

        dicomInfo(wrap);

    }

    protected void dicomInfo(DicomWrapper wrappedDcm)
    {
        //
        // DicomDumper printer = new DicomDumper(System.out);
        // printer.dumpDicomObject(obj);
        // outPrintf(" [Dump]\n");

        double orientations[] = wrappedDcm.getImageOrientationPatient();
        double positions[] = wrappedDcm.getImagePositionPatient();

        int ids[] = new int[4];
        wrappedDcm.getSetIdentificationNrs(ids);

        outPrintf(" - Patient ID/Name = %s / %s\n", wrappedDcm.getPatientID(), wrappedDcm.getPatientName());
        outPrintf(" - Study  UID      = %s \n", wrappedDcm.getStudyInstanceUID());
        outPrintf(" - Series UID      = %s \n", wrappedDcm.getSeriesInstanceUID());
        outPrintf(" - Set Id nrs      = %d.%d.%d.%d\n", ids[0], ids[1], ids[2], ids[3]);
        outPrintf(" - Image Position    = [%f,%f,%f]\n", positions[0], positions[1], positions[2]);
        outPrintf(" - Image Orientation = [%f,%f,%f],[%f,%f,%f]\n", orientations[0], orientations[1], orientations[2], orientations[3],
                orientations[4], orientations[5]);

        outPrintf(" - Study Date      = %s\n", wrappedDcm.getStudyDateTime());
        outPrintf(" - Special - \n");

    }

    static void outPrintf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

}

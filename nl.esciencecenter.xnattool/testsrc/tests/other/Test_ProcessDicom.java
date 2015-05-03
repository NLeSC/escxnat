package tests.other;

import nl.esciencecenter.medim.dicom.DicomProcessingProfile;
import nl.esciencecenter.medim.dicom.DicomProcessor;
import nl.esciencecenter.medim.dicom.DicomTestUtil;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.medim.dicom.types.DicomTags;

import org.dcm4che2.data.DicomObject;
import org.junit.Test;

public class Test_ProcessDicom
{

    public void testProcessDicom() throws Exception
    {
        java.net.URL dcmUrl = this.getClass().getResource("/tests/data/test_dicom1.dcm");
        testProcess(dcmUrl.getPath());
    }

    public void testProcess(String filename) throws Exception
    {
        java.io.File file = new java.io.File(filename);

        boolean isDicom = DicomUtil.hasDicomMagic(file);

        if (isDicom == false)
        {
            // throw new Exception("File is NOT dicom:"+file);
            System.err.printf("Magic 'DICM' NOT found in:%s\n", file);
        }

        DicomWrapper wrap = DicomWrapper.readFrom(file.toURI());
        DicomObject org = wrap.getDicomObject();

        int ids[] = new int[4];
        wrap.getSetIdentificationNrs(ids);

        outPrintf("> File:%s\n", file);

        outPrintf(" [Dicom] Before\n");

        outPrintf(" - Patient ID/Name   = %s / %s\n", wrap.getPatientID(), wrap.getPatientName());
        outPrintf(" -  - Birth DateTime = %s\n", wrap.getPatientBirthDateTime());
        outPrintf(" - Study  UID        = %s \n", wrap.getStudyInstanceUID());
        outPrintf(" - Series UID        = %s \n", wrap.getSeriesInstanceUID());
        outPrintf(" - Set Id nrs        = %d.%d.%d.%d\n", ids[0], ids[1], ids[2], ids[3]);
        outPrintf(" - Study Date        = %s\n", wrap.getStudyDateTime());
        byte bytes[] = DicomUtil.getBytes(org);
        outPrintf(" - Size of bytes    = %d\n", bytes.length);

        // ---
        wrap.setIsModifyable(true);

        DicomProcessingProfile procOpts = DicomTestUtil.createDefaultProcOpts("testid", "12345");
        // procOpts.setCredentials("testid","12345");
        DicomTags tagOpts = DicomTestUtil.createConfiguredDicomTagOptions();
        DicomProcessor dcmProc = new DicomProcessor(tagOpts, procOpts);

        // dcmProc.setTagOptions(tagOpts);
        dcmProc.process(wrap);

        outPrintf(" [Dicom] After\n");

        outPrintf(" - Patient ID/Name   = %s / %s\n", wrap.getPatientID(), wrap.getPatientName());
        outPrintf(" -  - Birth DateTime = %s\n", wrap.getPatientBirthDateTime());
        outPrintf(" - Study  UID        = %s \n", wrap.getStudyInstanceUID());
        outPrintf(" - Series UID        = %s \n", wrap.getSeriesInstanceUID());
        outPrintf(" - Set Id nrs        = %d.%d.%d.%d\n", ids[0], ids[1], ids[2], ids[3]);
        outPrintf(" - Study Date        = %s\n", wrap.getStudyDateTime());

        DicomObject dicom = dcmProc.getDicomObject();
        // test get bytes;
        bytes = DicomUtil.getBytes(dicom);
        outPrintf(" - Size of bytes    = %d\n", bytes.length);
    }

    static void outPrintf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

}

package tests.other;

import java.io.IOException;

import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.ptk.GlobalProperties;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;

public class Test_DicomUtil_ReadWrite
{

    public static void main(String args[])
    {
        try
        {
            String home = GlobalProperties.getGlobalUserHome();
            String file = home + "/dicom/subset1/1.MR.head_DHead.4.1.20061214.091206.156000.1632817982.dcm";
            String fileOut = home + "/dicom/out/new1.MR.head_DHead.4.1.20061214.091206.156000.1632817982.dcm";

            DicomObject obj = DicomUtil.readDicom(file);
            changeFields(obj);
            DicomUtil.writeDicom(obj, fileOut);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static DicomObject readDicom(String filename) throws Exception
    {
        java.io.File file = new java.io.File(filename);

        try
        {
            DicomObject obj = DicomUtil.readDicom(file);
            // DicomPrinter printer=new DicomPrinter(obj,System.out);
            // printer.printAll();

            return obj;
        }
        catch (IOException e)
        {
            throw new Exception("IOException: couldn't read:" + filename + "\n" + e.getMessage(), e);
        }
    }

    static void changeFields(DicomObject dcm)
    {
        System.out.printf("Changing tag:%s\n", Integer.toHexString(Tag.PatientName));

        dcm.putString(Tag.PatientName, VR.PN, "newName");
        dcm.putString(Tag.PatientID, VR.LO, "newID");

    }

}

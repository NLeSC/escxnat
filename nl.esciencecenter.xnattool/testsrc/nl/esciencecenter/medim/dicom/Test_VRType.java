package nl.esciencecenter.medim.dicom;

import org.dcm4che2.data.VR;
import org.junit.Assert;
import org.junit.Test;

import nl.esciencecenter.medim.dicom.types.DicomTypes;
import nl.esciencecenter.medim.dicom.types.DicomTypes.VRType;

public class Test_VRType
{

    @Test
    public void testVRTypes()
    {
        VRType vrTypes[] = DicomTypes.VRType.values();

        for (VRType vr : vrTypes)
        {
            testVRType(vr);
        }

    }

    private void testVRType(VRType vrType)
    {
        // enum type to string results in actual enum type;
        String vrName = vrType.toString();

        VRType valueOfVr = VRType.valueOf(vrName);
        Assert.assertEquals("valueOf(vrName) must result back in actual VR Enum type:" + vrType, vrType, valueOfVr);

        VR vr = vrType.vr();
        VRType fromVr = VRType.valueOf(vr);
        if (((vrType == VRType.DL) || (vrType == VRType.OX)) && ((fromVr == VRType.OB) || (fromVr == VRType.OW)))
        {
            // OX May match against OB or OW
            // DL May match against OB or OW
        }
        else
        {
            Assert.assertEquals("fromVR(vr) must result back in actual VR Enum type:" + vrType, vrType, fromVr);
        }
    }

}

package nl.esciencecenter.medim;

import nl.esciencecenter.ptk.util.StringUtil;

public class ImageTypes
{
    public static final String DEFAULT_SCAN_FILE_LABEL = "scan";

    public static final String DEFAULT_NUC_SCAN_FILE_LABEL = "nuc_scan";

    // =============
    // Custom Types
    // =============

    public static enum DataSetType
    {
        DICOM_SCANSET("Dicom Scans"), NIFTI_SCANSET("Nifti Scans"), NIFTI_ATLASSET("Atlas (Nifti)");

        private String label;

        private DataSetType(String menuLabel)
        {
            label = menuLabel;
        }

        public String getLabel()
        {
            return label;
        }

        public static DataSetType valueOfByLabel(String strVal)
        {
            for (DataSetType type : values())
            {
                if (StringUtil.equals(strVal, type.toString(), type.label))
                {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * SubType option for non-uniform scan types. Applicable for nifti scans. Dicoms are always "Raw".
     */
    public static enum ScanSubType
    {
        RAW_SCAN("Raw Scan"), NUC_SCAN("NUC Scan"), NONE("None");

        private String label;

        private ScanSubType(String menuLabel)
        {
            label = menuLabel;
        }

        public String getLabel()
        {
            return label;
        }
    }

    public static String getScanSubTypeFileLabel(ScanSubType subType)
    {
        switch (subType)
        {
            case NUC_SCAN:
            {
                return DEFAULT_NUC_SCAN_FILE_LABEL;
            }
            case RAW_SCAN:
            {
                return DEFAULT_SCAN_FILE_LABEL;
            }
            default:
            {
                return DEFAULT_SCAN_FILE_LABEL;
            }
        }
    }

}

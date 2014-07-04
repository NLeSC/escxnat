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

package nl.esciencecenter.xnatclient.data;

public class XnatTypes
{
    // ===========
    // Enum Types
    // ===========

    public enum ImageContentType
    {
        T1_RAW, T2_RAW, T1_RECON, ATLAS_LABEL
    };

    public enum ImageFormatType
    {
        DICOM, NIFTI
    };

    public enum ImageMrType
    {
        T1
    };

    /**
     * Resource Type and Sub Path in resource URL paths: Scan: .../scans/... or reconstruction: .../reconstructions/...
     * <ul>
     * <li>SCAN : ../experiment/EXPID/scans/SCANID/resources/LABEL/files/...
     * <li>RECONSTRUCTION : ../experiments/ExpID/reconstructions/RECONID/files/...
     * <li>EXPERIMENT : ../experiments/EXPID/files/...
     * </ul>
     */
    public enum XnatResourceType
    {
        SCAN,
        RECONSTRUCTION,
        EXPERIMENT
    };

    public enum Handedness
    {
        UNKNOWN("unknown"),
        LEFTHANDED("left"),
        RIGHTHANDED("right"),
        AMBIDEXTROUS("ambidextrous");

        // === Instance ===

        private String text;

        private Handedness(String text)
        {
            this.text = text;
        }

        public String toString()
        {
            return text;
        }

        // === Static ==

        static public String getFieldName()
        {
            return XnatConst.FIELD_SUBJECT_HANDEDNESS;
        }

        /**
         * Case insensitive fromString()
         */
        public static Handedness fromString(String str)
        {
            return valueOf(str.toUpperCase());
        }
    }

    public enum Gender
    {
        UNKNOWN("unknown"),
        MALE("male"),
        FEMALE("female");

        // === Instance ===

        private String text;

        private Gender(String text)
        {
            this.text = text;
        }

        public String toString()
        {
            return text;
        }

        // === Static ==

        static public String getFieldName()
        {
            return XnatConst.FIELD_SUBJECT_GENDER;
        }

        /**
         * Case insensitive fromString()
         */
        public static Gender fromString(String str)
        {
            return valueOf(str.toUpperCase());
        }
    }

    public static ImageFormatType parseImageFormatType(String value)
    {
        if (value == null || value.equals(""))
            return null;

        return ImageFormatType.valueOf(value);
    }

    public static ImageContentType parseImageContentType(String value)
    {
        if (value == null || value.equals(""))
            return null;
        try
        {
            return ImageContentType.valueOf(value);
        }
        catch (Throwable t)
        {
            return null;
        }
    }

}

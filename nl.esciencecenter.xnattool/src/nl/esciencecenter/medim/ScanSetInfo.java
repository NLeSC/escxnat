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

package nl.esciencecenter.medim;

import java.util.ArrayList;
import java.util.Date;

import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.medim.exceptions.ScanSetException;
import nl.esciencecenter.ptk.io.FSPath;

import org.dcm4che2.data.Tag;

/**
 * ScanSetInfo object which contains relevant fields about a Patient/Study/Series.<br>
 * Patient or Subject maps to PatientInfo object. Study or Session maps to StudyInfo object. Series or ScanSet maps to
 * SeriesInfo object.
 */
public class ScanSetInfo extends DataInfo
{
    public static final int ATLAS_SCANSET_FILE_INDEX = 0;

    /**
     *  Relevant Scan Parameter used for Protocol Filtering.
     */ 
    public static class ScanTypeParameters
    {
        /**
         * TE or Echo Time
         */
        public double echoTime;

        /**
         * TR or Response TIme
         */
        public double repeatTime;

        /**
         * TI or Inverse Time. Value can be null, use negative value <0 to indicate missing value.
         */
        public double inverseTime;

        /**
         * FA or Flip Angle.
         */
        public double flipAngle;

        /**
         * Scanning Sequence Type.
         */
        public String scanningSequence;

        /**
         * Modality Type.
         */
        public String modality;
    }

    /**
     * Container class combining actual File with logical file label.
     */
    public static class FileDescriptor
    {
        public String fileLabel;

        public FSPath fsNode;

        public FileDescriptor(String label, FSPath node)
        {
            this.fileLabel = label;
            this.fsNode = node;
        }
    }

    protected ImageTypes.DataSetType datasetType;

    protected String scanLabel;

    protected SubjectInfo subjectInfo = new SubjectInfo();

    protected StudyInfo studyInfo = new StudyInfo();

    protected SeriesInfo seriesInfo = new SeriesInfo();

    protected ArrayList<FileDescriptor> fsNodes;

    protected ScanTypeParameters scanTypeParameters = new ScanTypeParameters();

    // protected StringList tagNames;

    protected Date uploadFinished;

    /**
     * New DataSetType
     * 
     * @param setType
     *            - DICOM ScanSet , NIFTI ScanSet , NIFTI ATLAS
     * @param scanUid
     *            - unique scan UID
     * @param scanLabelOrId
     *            - non unique Scan ID human readable label name.
     */
    public ScanSetInfo(ImageTypes.DataSetType setType, String scanUid, String scanLabelOrId)
    {
        this.datasetType = setType;
        this.getSeriesInfo().setSeriesInstanceUID(scanUid);
        this.scanLabel = scanLabelOrId;
        this.fsNodes = new ArrayList<FileDescriptor>();
    }

    public ImageTypes.DataSetType getDataSetType()
    {
        return datasetType;
    }

    public String getStudyInstanceUID()
    {
        return getStudyInfo().studyInstanceUID;
    }

    public String getSeriesInstanceUID()
    {
        if (getSeriesInfo() == null)
            return null;

        return getSeriesInfo().getSeriesInstanceUID();
    }

    public SubjectInfo getSubjectInfo()
    {
        return this.subjectInfo;
    }

    public SeriesInfo getSeriesInfo()
    {
        return this.seriesInfo;
    }

    public StudyInfo getStudyInfo()
    {
        return this.studyInfo;
    }

    /**
     * Returns ScanSet label or logical id. This label might not be unique when scanning multiple ScanSets from
     * different Subjects. Use getScanUID() for unique ScanSet IDs. For dicom this could be he Series ID (not UID) or
     * some other human readable scanset ID.
     * 
     * @return
     */
    public String getScanLabel()
    {
        return this.scanLabel;
    }

    /**
     * Return the field which is used to uniquely identify this ScanSet. <br>
     * For dicom for example this is currently the SeriesInstanceUID.
     * 
     * @return Unique ScanSetUID.
     */
    public String getScanUID()
    {
        if (getSeriesInfo() == null)
            return null;

        return this.getSeriesInfo().getSeriesInstanceUID();
    }

    public void setAtlasScanSetFile(String fileLabel, FSPath node)
    {
        this.setFile(ATLAS_SCANSET_FILE_INDEX, fileLabel, node);
    }

    public void addAtlasAnnotationFile(String fileLabel, FSPath node)
    {
        // First file is reserved for the actual scan set.
        if (this.fsNodes.size() <= 0)
        {
            fsNodes.add(new FileDescriptor(null, null));
        }

        this.fsNodes.add(new FileDescriptor(fileLabel, node));
    }

    public FSPath getAtlasScanSetFile()
    {
        return getFile(ATLAS_SCANSET_FILE_INDEX);
    }

    /**
     * Add (Slice) File to correct position in node list. Auto increases the File Array. Use nr=-1 for unknown. File
     * will be added at the end of the list.
     * 
     * @param fileNr
     *            - slice number or other accession number. Use -1 for unkown
     * @param label
     *            - optional file label to use for this scan or slice.
     * @param node
     *            - actual file node.
     */
    public void setFile(int nr, String label, FSPath node)
    {
        // linear search, optionally use instance nr to sort file:
        if (this.fsNodes.contains(node))
        {
            return;
        }

        fsNodes.ensureCapacity(nr + 1); // auto expand to desired length.

        if (fsNodes.size() <= nr)
        {
            // fill with empty entries.
            for (int i = fsNodes.size(); i <= nr; i++)
            {
                fsNodes.add(new FileDescriptor(null, null));
            }
        }

        //
        // SPI (Philips) Patch:
        // If (slice) instance number is known, set at specified location, if
        // not, just add here:
        //

        if (nr >= 0)
        {
            fsNodes.set(nr, new FileDescriptor(label, node));
        }
        else
        {
            fsNodes.add(new FileDescriptor(label, node));
        }
    }

    public FSPath getFile(int nr)
    {
        if ((this.fsNodes == null) || (fsNodes.size() <= nr))
        {
            return null;
        }

        FileDescriptor fileDes = fsNodes.get(nr);
        if (fileDes == null)
        {
            return null;
        }

        return fileDes.fsNode;
    }

    /**
     * Returns actual number of valid (non null) files
     */
    public int getNumFSNodes()
    {
        // Count NON null files.
        // The array might contain null entries as some slices might start from
        // an offset. The index represent the slice nr or position.
        //

        int index = 0;

        for (int i = 0; i < this.fsNodes.size(); i++)
        {
            if ((this.fsNodes.get(i) != null) && (fsNodes.get(i).fsNode != null))
            {
                index++;
            }
        }

        return index;
    }

    /**
     * @return Return first non null file from file list.
     */
    public FSPath getFirstFile()
    {
        if ((fsNodes == null) || (fsNodes.size() <= 0))
        {
            return null;
        }

        for (int i = 0; i < this.fsNodes.size(); i++)
        {
            if (this.fsNodes.get(i) != null)
            {
                if (fsNodes.get(i).fsNode != null)
                {
                    return fsNodes.get(i).fsNode;
                }
            }
        }

        return null;
    }

    public ArrayList<FileDescriptor> getFileDescriptors()
    {
        return this.fsNodes;
    }

    /**
     * Initialize new or check existing subject information.
     */
    public void initCheck(DicomWrapper wrap, boolean isNew) throws ScanSetException
    {
        // Set Identifiers per ScanSet MUST be the same.
        assertNullOrEqual(getSeriesInfo().getSeriesInstanceUID(), wrap.getSeriesInstanceUID(), Tag.SeriesInstanceUID);
        getSeriesInfo().setSeriesInstanceUID(wrap.getSeriesInstanceUID());

        assertNullOrEqual(getStudyInfo().getStudyInstanceUID(), wrap.getStudyInstanceUID(), Tag.StudyInstanceUID);
        getStudyInfo().setStudyInstanceUID(wrap.getStudyInstanceUID());

        assertNullOrEqual(getSubjectInfo().getPatientName(), wrap.getPatientName(), Tag.PatientName);
        getSubjectInfo().setPatientName(wrap.getPatientName());

        assertNullOrEqual(getSubjectInfo().getPatientID(), wrap.getPatientID(), Tag.PatientID);
        getSubjectInfo().setPatientID(wrap.getPatientID());

        // TBI:
        // this.acquisitionNr = wrap.getAcquisitionNumber();

        // Extra subject info
        // xxxW or xxxM or xxxY (weeks, month,years);
        getSubjectInfo().setPatientAgeString(wrap.getStringValue(Tag.PatientAge));
        getSubjectInfo().setPatientGender(wrap.getStringValue(Tag.PatientSex));
        getSubjectInfo().setPatientBirthDate(wrap.getDateValue(Tag.PatientBirthDate));

        // extra info
        getStudyInfo().setStudyDate(wrap.getDateValue(Tag.StudyDate));
        // extra NON UID identifiers (should be the same).
        getStudyInfo().setStudyId(wrap.getStudyID());
        getStudyInfo().studyDescription = wrap.getStringValue(Tag.StudyDescription);

        getSeriesInfo().setSeriesNumber(wrap.getSeriesNumber());
        getSeriesInfo().setSeriesDescription(wrap.getStringValue(Tag.SeriesDescription));
        getSeriesInfo().setSeriesDate(wrap.getDateValue(Tag.SeriesDate));

        scanTypeParameters.scanningSequence = wrap.getScanningSequenceTypeString();
        scanTypeParameters.modality = wrap.getModalityType();
        scanTypeParameters.repeatTime = wrap.getRepetitionTimeDouble();
        scanTypeParameters.echoTime = wrap.getEchoTimeDouble();
        // optional, mandatory for IR.
        scanTypeParameters.inverseTime = wrap.getInversionTimeDouble();
        scanTypeParameters.flipAngle = wrap.getFlipAngleDouble();

        // this.fileSetId=wrap.getFileSetID(); // if applicable
    }

    /** Assert that previous value is either null or matched newVal. */
    private boolean assertNullOrEqual(String prevVal, String newVal, int tagNr) throws ScanSetException
    {
        if (prevVal == null)
            return true;

        boolean eq = prevVal.equals(newVal);

        if (eq == false)
        {
            throw new ScanSetException("Image file from same ScanSet has mismatching attribute:"
                    + DicomUtil.getTagName(tagNr) + ". '" + prevVal + "!='" + newVal + "'");
        }

        return true;
    }

    public void setSeriesDescription(String message)
    {
        this.getSeriesInfo().setSeriesDescription(message);
    }

    public String getSeriesDescription()
    {
        if (this.getSeriesInfo() == null)
            return null;

        return getSeriesInfo().getSeriesDescription();
    }

    public void setSubjectInfo(SubjectInfo info)
    {
        this.subjectInfo = info;
    }

    public void setStudyInfo(StudyInfo info)
    {
        this.studyInfo = info;
    }

    public void setSingleScanSetFile(FSPath file)
    {
        this.fsNodes = new ArrayList<FileDescriptor>();
        this.fsNodes.add(new FileDescriptor(null, file));
    }

    public ScanTypeParameters getScanTypeParameters()
    {
        return this.scanTypeParameters;
    }

    // public void mergeTagNames(List<String> names)
    // {
    // if (this.tagNames == null)
    // {
    // tagNames = new StringList(names);
    // }
    // else
    // {
    // tagNames.add(names, true);
    // }
    // // tagNames.sort();
    // tagNames.unique(false);
    // }

    // public List<String> getTagNames()
    // {
    // return tagNames;
    // }

    public void setUploadFinishedDate(Date date)
    {
        this.uploadFinished = date;
    }

    public Date getUploadFinishedDate()
    {
        return uploadFinished;
    }

    @Override
    public String getDataType()
    {
        return "ScanSet";
    }

    @Override
    public String getDataUID()
    {
        return this.getSeriesInfo().getSeriesInstanceUID();
    }

    protected void setSeriesInfo(SeriesInfo seriesInfo)
    {
        this.seriesInfo = seriesInfo;
    }

}

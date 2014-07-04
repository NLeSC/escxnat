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

package nl.esciencecenter.medim.dicom;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.esciencecenter.medim.ImageDirEventType;
import nl.esciencecenter.medim.ImageDirScanner;
import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.ImageTypes.DataSetType;
import nl.esciencecenter.medim.ScanSetInfo;
import nl.esciencecenter.medim.dicom.DicomTagFilters.DicomTagFilter;
import nl.esciencecenter.medim.exceptions.ScanSetException;
import nl.esciencecenter.ptk.data.HashMapList;
import nl.esciencecenter.ptk.data.HashSetList;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.io.exceptions.FileURISyntaxException;
import nl.esciencecenter.ptk.net.URIFactory;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import org.dcm4che2.data.DicomObject;

/**
 * DICOM Directory scanner, performs an (optional) recursive scan on a DICOM
 * directory and filters and sorts the DICOM files into ScanSets. <br>
 * Zip files not supported.
 * 
 * @author Piter T. de Boer
 */
public class DicomDirScanner extends ImageDirScanner
{
    private static ClassLogger logger = ClassLogger.getLogger(DicomDirScanner.class);

    static
    {
        // logger.setLevelToDebug();
    }

    // ========================================================================
    //
    // ========================================================================

    /**
     * List of all dicom files. Before filtering this contains all files, after
     * filtering this contains the filtered files.
     */
    protected List<FSPath> imageFiles;

    /**
     * Mapping of the Subject ID, which can be for example the DICOM PatientID
     * or PatientName, to a Set of Study IDs.
     */
    protected Map<String, HashSetList<String>> subjectToStudies;

    /**
     * Mapping of the Study ID, for example the DICOM Study UID, to a set of
     * ScanSet (Series) UIDs
     */
    protected Map<String, HashSetList<String>> studyToScanSets;

    protected List<FileError> fileErrors;

    // ===
    // state
    // ===

    private ScanMonitor scanMonitor;

    public DicomDirScanner()
    {
        super(ImageTypes.DataSetType.DICOM_SCANSET);
        // defaults:
        initFilters();
    }

    // ========================================================================
    // Scanning
    // ========================================================================

    protected boolean matchFilters(DicomWrapper wrap)
    {
        List<DicomTagFilter> filters = this.getTagFilters();
        if (filters == null)
            return true; // no filters => accept all;

        return wrap.matches(filters);
    }

    /**
     * Quick scans directory. Only checking file by extension and size. Sorting/analyzing
     * is done later.
     * 
     * @throws InterruptedException
     * @throws ScanSetException
     */
    public boolean scanDirectory(URI imageDirUri, boolean recursive, ScanMonitor optScanMonitor) throws IOException, InterruptedException,
            ScanSetException
    {
        if (optScanMonitor == null)
        {
            this.scanMonitor = new ScanMonitor();
        }
        else
        {
            this.scanMonitor = optScanMonitor;
        }

        this.fireNewEvent(ImageDirEventType.STARTING, "Scanning directory:" + imageDirUri.getPath());
        this.imageFiles = null;

        // First add all files
        logger.infoPrintf("Scanning directory:%s\n", imageDirUri);

        URIFactory uriFac = new URIFactory(imageDirUri);

        String localPath = uriFac.getPath();

        FSUtil fs = FSUtil.getDefault();
        FSPath dir;
        dir = fs.newLocalDir(fs.resolvePathURI(localPath));
        
        if (dir.exists() == false)
        {
            throw new IOException("Location is not a valid directory:" + imageDirUri.getPath());
        }

        List<FSPath> heap = new ArrayList<FSPath>();
        List<FSPath> dirsToScan = new ArrayList<FSPath>();
        // root:
        dirsToScan.add(dir);

        // heap scan:
        while (dirsToScan.size() > 0)
        {
            // pop dir;
            FSPath current = dirsToScan.get(0);
            dirsToScan.remove(0);
            enterDirectory(current, true);
            int numFiles = 0;

            if (Thread.currentThread().isInterrupted() == true)
            {
                this.setIsScanning(false);
                throw new InterruptedException("Thread is interrupted. Stopped scanning!");
            }

            // scan:

            FSPath[] files = current.listNodes();
            if (files == null)
            {
                logger.infoPrintf(" - No files in directory:%s\n", imageDirUri);
                exitDirectory(current, true);
                continue; // no files/directories
            }

            for (int i = 0; i < files.length; i++)
            {
                FSPath file = files[i];

                if (files[i].isFile())
                {
                    // check extensions:
                    if ((filterOptions.checkExtensions() == false) || (hasValidExtension(file.getExtension())))
                    {
                        // check file size here:
                        if (this.filterOptions.checkFileSize && (file.getFileSize() > this.getMaxSliceFileLength()))
                        {
                            logger.infoPrintf(" - Skipping: File to big (size %d > max). Skipping: %s\n", file.getFileSize(), file);
                        }
                        else
                        {
                            heap.add(file);
                            logger.infoPrintf(" - Adding file:%s\n", file);
                            numFiles++;
                        }

                    }
                    else
                    {
                        logger.infoPrintf(" - file doesn't match file filter:%s\n", files[i]);
                    }
                }
                else if (files[i].isDirectory())
                {
                    // add to heap :
                    dirsToScan.add(files[i]);
                    logger.infoPrintf(" - adding directory:%s\n", files[i]);
                }
            }
            fireNewEvent(ImageDirEventType.MESSAGE, " - Number of (DICOM) files found:" + numFiles);
            exitDirectory(current, true);
        }

        this.imageFiles = heap;
        this.scanMonitor.totalFiles = heap.size();

        this.fireNewEvent(ImageDirEventType.MESSAGE, ">>> Processing " + imageFiles.size() + " dicom files.");

        // Now filter files
        doFilterFiles();

        this.fireNewEvent(ImageDirEventType.IS_DONE, "Done scanning directory:" + imageDirUri.getPath());

        return true;
    }

    /**
     * Filter files after quick scanning. 
     * This method inspects the actual Dicom File. 
     */
    protected boolean doFilterFiles() throws IOException, InterruptedException, ScanSetException
    {
        // all files.
        List<FSPath> files = this.getFiles();
        List<FSPath> keepFiles = new ArrayList<FSPath>(); // file

        if (files == null)
            return false;

        int numFiles = files.size();

        this.subjectToStudies = new HashMapList<String, HashSetList<String>>();
        this.studyToScanSets = new HashMapList<String, HashSetList<String>>();

        this.fileErrors = new ArrayList<FileError>();

        for (int i = 0; i < numFiles; i++)
        {
            DicomWrapper wrap = null;

            scanMonitor.currentFile = i;

            if (Thread.currentThread().isInterrupted() == true)
            {
                this.setIsScanning(false);
                throw new InterruptedException("Thread is interrupted. Stopping scanning!");
            }

            FSPath node = files.get(i);

            logger.infoPrintf("> Analying file:%s\n", node);

            if (this.filterOptions.checkFileSize && (node.getFileSize() > this.getMaxSliceFileLength()))
            {
                logger.infoPrintf(" - Skipping: File to big (size %d > max). Skipping: %s\n", node.getFileSize(), node);
                continue;
            }
            else if (this.filterOptions.checkFileMagic && (node instanceof FSPath)
                    && (DicomUtil.hasDicomMagic((FSPath) node) == false))
            {
                logger.infoPrintf(" - Skipping: Wrong Magic. File is not Dicom: %s\n", node);
                continue;
            }

            // identifiers:
            String studyUid = null; // A study *may* contain multiply series
            Integer seriesNr = null; // Relative number of the series within a
                                     // study
            String seriesUid = null; // Used as Actual ScanSet ID.
            Integer instanceNr = null;// Scan slice number within a ScanSet.

            // Optional aqcuisition number:
            Integer acquNr = null;

            Exception fileEx = null;

            try
            {
                // ===============================================================
                // Fetch info, any exceptions throw here will disqualify the
                // file!
                // =================================================================

                DicomObject dicomObj = DicomUtil.readDicom(node.getURI().getPath());
                wrap = new DicomWrapper(dicomObj, node.getURI());

                int ids[] = new int[4];

                wrap.getSetIdentificationNrs(ids);
                String studyId = wrap.getStudyID();

                studyUid = wrap.getStudyInstanceUID();
                seriesUid = wrap.getSeriesInstanceUID(); // Series UID =>
                                                         // ScanSet UID !
                instanceNr = wrap.getInstanceNumber();
                seriesNr = wrap.getSeriesNumber();
                acquNr = wrap.getAcquisitionNumber();

                double[] positions = wrap.getImagePositionPatient();

                logger.debugPrintf(" - Patient ID/Name      = %s / %s\n", wrap.getPatientID(), wrap.getPatientName());
                logger.debugPrintf(" - Study/Series UIDs    = %s / %s\n", studyUid, seriesUid);
                logger.debugPrintf("   Acquisition/Instance = %d/%d\n", acquNr, instanceNr);
                logger.debugPrintf(" - Set Id nrs           = ('%s')%d.%d.%d.%d\n", studyId, ids[0], ids[1], ids[2], ids[3]);
                if ((positions != null) && (positions.length >= 3))
                {
                    logger.debugPrintf(" - Image Position   = [%f,%f,%f]\n", positions[0], positions[1], positions[2]);
                }
                else
                {
                    logger.debugPrintf(" - Image Position   = <?>\n");
                }
                // Valid DICOM add here before filter (which might be changed
                // after scan)
                keepFiles.add(node);
            }
            catch (Exception e)
            {
                // Read Error: Not valid dicom.
                fileEx = e;
                // IO Exception.
                logger.logException(ClassLogger.WARN, e, "- failed to read file:%s.", node);
                // skip file.
                seriesUid = null;
                // wrap=null;
            }

            // happens with SPI files:
            if (instanceNr == null)
            {
                instanceNr = -1;
            }

            // file must contain valid seriesUid and instanceNr
            if ((seriesUid != null) && (wrap != null) && (instanceNr != null))
            {
                double TR = wrap.getRepetitionTimeDouble();
                double TE = wrap.getEchoTimeDouble();
                double TI = wrap.getInversionTimeDouble();
                double FA = wrap.getFlipAngleDouble();

                logger.infoPrintf(" - TR/TE/TI/FA       = %f/%f/%f/%f\n", TR, TE, TI, FA);
                logger.infoPrintf(" - Modility/Sequence = %s/%s\n", wrap.getModalityType(),
                        new StringList(wrap.getScanningSequenceTypes()).toString(","));

                if (matchFilters(wrap))
                {
                    logger.infoPrintf(" - >>> Matches Filters!\n");
                    logger.infoPrintf(" - Adding valid DICOM file (%3d,%3d):%s\n", seriesNr, instanceNr, node);

                    // create list if doesn't exist:
                    ScanSetInfo scanSet = this.getScanSet(seriesUid);
                    boolean isNew = false;
                    if (scanSet == null)
                    {
                        scanSet = new ScanSetInfo(ImageTypes.DataSetType.DICOM_SCANSET, seriesUid, seriesUid); // scanset
                                                                                                    // ID.
                        isNew = true;
                    }

                    try
                    {
                        scanSet.initCheck(wrap, isNew);
                        // slice
                        scanSet.setFile(instanceNr, "DICOM#" + instanceNr, node);
                        // put update
                        registerScanSet(seriesUid, scanSet, isNew);

                        //List<String> names = wrap.getTagNames(false);
                        //scanSet.mergeTagNames(names);
                    }
                    catch (ScanSetException e)
                    {
                        // init failed!
                        logger.logException(ClassLogger.ERROR, e,
                                " Invalid ScanSet attributes. Values might conflict for (slice)file:%s\n", node);
                        this.fireErrorMessage("Error processing file:\n - " + node + "\n - Error = " + e.getMessage() + "\n");

                        fileErrors.add(new FileError(node, e.getMessage()));

                        // do not ignore, should be valid Dicom.
                        String errorText = e.getMessage();
                        throw new ScanSetException("*** Error while processing file:" + wrap.getFilename() + "\n" + errorText);

                    }
                }
                else
                {
                    logger.infoPrintf(" - File does NOT match Filters! Skipping file:%s\n", node);
                    this.fireMessage("File does not match filters. Skipping:\n - " + node);
                }
            }
            else
            {
                logger.infoPrintf(" - Wrong file or read error for:%s\n", node);
            }

            // cleanup ?
            if (wrap != null)
            {
                wrap.dispose();
            }

            if (i % 10 == 0)
            {
                this.fireNewEvent(ImageDirEventType.UPDATE_STATS, node.toString());
            }

        }// for (files)

        this.imageFiles = keepFiles;
        this.scanMonitor.totalFilteredFiles = keepFiles.size();

        return true;
    }

    protected void registerScanSet(String scanSetuid, ScanSetInfo scanSet, boolean isNewScanSet) throws ScanSetException
    {
        if (scanSetuid == null)
            throw new NullPointerException("Cannot add null seriesUid");

        if (scanSetuid.equals(scanSet.getSeriesInfo().getSeriesInstanceUID()) == false)
            throw new ScanSetException("Internal Error: ScanSet seriesUid mismatch");

        this.addScanSet(scanSetuid, scanSet);

        String subjectId = scanSet.getSubjectInfo().getPatientID();
        String studyUid = scanSet.getStudyInfo().getStudyInstanceUID();

        boolean isNewStudy = false;
        boolean isNewSubject = false;

        // synchronized(this.studyToScanSets)
        {
            HashSetList<String> list = this.studyToScanSets.get(studyUid);
            if (list == null)
            {
                list = new HashSetList<String>();
                isNewStudy = true;
            }
            list.add(scanSetuid);
            this.studyToScanSets.put(scanSet.getStudyInfo().getStudyInstanceUID(), list);
        }

        // synchronized(this.subjectToStudies)
        {
            HashSetList<String> list = this.subjectToStudies.get(subjectId);
            if (list == null)
            {
                list = new HashSetList<String>();
                isNewSubject = true;
            }
            list.add(scanSet.getStudyInfo().getStudyInstanceUID());
            this.subjectToStudies.put(subjectId, list);
        }

        if (isNewSubject)
        {
            fireNewEvent(ImageDirEventType.NEW_SUBJECT, subjectId);
        }

        if (isNewStudy)
        {
            fireNewEvent(ImageDirEventType.NEW_STUDY, studyUid);
        }

        if (isNewScanSet)
        {
            fireNewEvent(ImageDirEventType.NEW_SCANSET, scanSetuid);
        }
    }

    @Override
    public List<FSPath> getFiles()
    {
        return this.imageFiles;
    }

    // number of file in directory, not parse/scanned!
    public int getNumRawFiles()
    {
        if (this.imageFiles == null)
            return 0;

        return this.imageFiles.size();
    }

    /**
     * Returns actual nr of evaluated files.
     */
    public int getNumFiles()
    {
        if (this.getScanSets() == null)
            return 0;

        int total = 0;

        Set<String> keys = this.getScanSets().keySet();
        Iterator<String> it = keys.iterator();

        while (it.hasNext())
        {
            String val = it.next();
            ScanSetInfo set = this.getScanSets().get(val);

            if (set != null)
            {
                int setNodes = set.getNumFSNodes();
                total = total + set.getNumFSNodes();
            }
        }
        return total;
    }

    public Set<String> getSubjectIDs()
    {
        if (subjectToStudies == null)
            return null;

        Set<String> keys = this.subjectToStudies.keySet();
        return keys;
    }

    public Set<String> getStudyUids(String subjectId)
    {
        if (subjectToStudies == null)
            return null;

        return this.subjectToStudies.get(subjectId);
    }

    public Set<String> getScanSetUids(String identifier)
    {
        if (studyToScanSets == null)
            return null;

        return this.studyToScanSets.get(identifier);
    }

    // =====================
    // Life cycle management
    // =====================

    /**
     * Dispose object. After a dispose() the object must not be used anymore.
     */
    public void dispose()
    {
        clear();
    }

    /**
     * Clear ScanSets and other information. After a clear() the Object may be
     * reused.
     */
    public void clear()
    {
        super.clear();

        if (imageFiles != null)
            imageFiles.clear();

        if (subjectToStudies != null)
            subjectToStudies.clear();

        if (studyToScanSets != null)
            studyToScanSets.clear();

        if (fileErrors != null)
            fileErrors.clear();

    }

//    public List<String> getScanSetFieldNames(String scanSetUid)
//    {
//        ScanSetInfo scanSet = this.getScanSet(scanSetUid);
//        return scanSet.getTagNames();
//    }

}

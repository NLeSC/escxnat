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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.esciencecenter.medim.dicom.DicomTagFilters;
import nl.esciencecenter.medim.dicom.DicomTagFilters.DicomTagFilter;
import nl.esciencecenter.medim.exceptions.ScanSetException;
import nl.esciencecenter.ptk.data.HashMapList;
import nl.esciencecenter.ptk.data.HashSetList;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

import org.dcm4che2.data.Tag;

/**
 * Abtract class which can scan a directory contain MRI images. <br>
 * Super class for DicomDirScanner and NiftiDirScanner
 * 
 * @author Piter T. de Boer
 */
public abstract class ImageDirScanner
{
    /**
     * File Error Holder class. Combines File node with errorText into holder class.
     */
    public static class FileError
    {
        FSPath fsNode;

        String errorText;

        public FileError(FSPath node, String message)
        {
            this.fsNode = node;
            this.errorText = message;
        }
    }

    /**
     * Monitor object which can be watched during the scanning and processing. Implementation will update this Monitor
     * Object.
     */
    public static class ScanMonitor
    {
        public int totalFiles = 0;

        public int totalFilteredFiles = 0;

        public int currentFile = 0;
    }

    /**
     * DICOM/Nifti Filter Settings.
     */
    public static class FileFilterOptions
    {
        public static StringList default_dicom_extensions = new StringList(new String[] {
                "dcm", "DCM"
        });

        public static StringList default_nifti_extensions = new StringList(new String[] {
                "nii", "NII", "nii.gz", "NII.GZ"
        });

        public static StringList default_atlas_extensions = new StringList(new String[] {
                "nii", "NII", "nii.gz", "NII.GZ", "txt", "TXT", "csv", "CSV", "xml", "XML"
        });

        // === Instance ===

        /**
         * List of valid extensions without "." or "*." prefixed (no wildcard or regular expressions) !<br>
         * For example: { "dcm", DCM" }
         */
        public StringList validExtensions = default_dicom_extensions;

        public List<DicomTagFilter> dicomTagFilters = new ArrayList<DicomTagFilter>();

        public boolean checkFileMagic = true;

        /**
         * Whether to check the file size. A typical slice file is less then 1 MB.
         */
        public boolean checkFileSize = true;

        /**
         * Slice file size limit during scanning. Note this limit is only applicable to DICOM slice files.
         */
        public long maxSliceFileSize = 10 * 1024 * 1024;

        // public long maxFileSize;
        /**
         * Whether to recurse into sub-directories.
         */
        public boolean recursiveScan = true;

        public void setCheckFileMagic(boolean val)
        {
            checkFileMagic = val;
        }

        /**
         * Set list of extension with prefixed dot, for example <code>String[]{"dcm","DCM" }</code>
         * 
         * @param extensions
         *            List of allowed extensions.
         */
        public void setExtensions(List<String> extensions)
        {
            if (extensions == null)
            {
                this.validExtensions = null;
            }
            else
            {
                this.validExtensions = new StringList(extensions);
            }
        }

        public boolean checkExtensions()
        {
            if ((this.validExtensions == null) || (this.validExtensions.size() <= 0))
            {
                return false;
            }

            return true;
        }

        /**
         * Set Modality Filter for example "MR" or "CT". Set to null to accept all.
         * 
         * @param modality
         *            The DICOM modility
         */
        public void setModalityFilter(String modality)
        {
            // remove previous;
            this.deleteModalityFilter();

            DicomTagFilter fil = new DicomTagFilters.StringMatchFilter(Tag.Modality, modality, false);
            dicomTagFilters.add(fil);
        }

        public void deleteModalityFilter()
        {
            for (DicomTagFilter fil : dicomTagFilters)
            {
                if (fil.getTagNr() == Tag.Modality)
                {
                    dicomTagFilters.remove(fil);
                    return; // stop.
                }
            }
        }
    }

    // =======================================
    // Instance
    // =======================================

    // State of scanner.
    private boolean _isScanning = false;

    /**
     * Registered listeners. Typically this is the GUI.
     */
    protected List<ImageDirScannerListener> listeners = new ArrayList<ImageDirScannerListener>();

    protected FileFilterOptions filterOptions;

    protected ImageTypes.DataSetType dataSetType;

    protected ImageTypes.ScanSubType scanSubType;

    /**
     * Maps ScanSet UID (= Series Instance UID) to ScanSetInfo object.
     */
    protected Map<String, ScanSetInfo> _scanSets; // series

    /**
     * Sorted set of the ScanSet UIDs (Series UID).
     */
    protected HashSetList<String> _scanSetUids;

    // ========================================================================
    // Listeners/Events
    // ========================================================================

    public ImageDirScanner(ImageTypes.DataSetType dataSetType)
    {
        init(dataSetType);
    }

    public ImageTypes.DataSetType getDataSetType()
    {
        return dataSetType;
    }

    private void init(ImageTypes.DataSetType dataSetType)
    {
        this._scanSets = new HashMapList<String, ScanSetInfo>();
        this._scanSetUids = new HashSetList<String>();
        this.dataSetType = dataSetType;
    }

    public void addDicomDirListener(ImageDirScannerListener listener)
    {
        this.listeners.add(listener);
    }

    public void removeDicomDirListener(ImageDirScannerListener listener)
    {
        this.listeners.remove(listener);
    }

    protected void fireNewEvent(ImageDirEventType eventType, String idOrMessage, Object... args)
    {
        ImageDirEvent event = new ImageDirEvent(this, eventType, idOrMessage, args);
        fireEvent(event);
    }

    protected void fireEvent(ImageDirEvent event)
    {
        // synchronous update!
        for (ImageDirScannerListener l : listeners)
        {
            l.notifyImageDirScannerEvent(event);
        }

        // concurrency bug, introduce nanosleep between event to avoid race
        // conditions and (rare) thread lockups
        try
        {
            Thread.sleep(0, 1);
        }
        catch (InterruptedException e)
        {
            // logger.logException(ClassLogger.WARN,e,"Nanosleep got interrrupted:%s",e);
            // keep flag, it is cleared after InterruptedException is thrown.
            Thread.currentThread().interrupt();
        }
    }

    protected void fireMessage(String message, Object... args)
    {
        ImageDirEvent event = new ImageDirEvent(this, ImageDirEventType.MESSAGE, message, args);
        fireEvent(event);
    }

    protected void fireErrorMessage(String message, Object... args)
    {
        ImageDirEvent event = new ImageDirEvent(this, ImageDirEventType.ERROR_MESSAGE, message, args);
        fireEvent(event);
    }

    protected void exitDirectory(FSPath dir, boolean fireEvent)
    {
        // System.err.printf("Leaving:%s\n",dir);
        fireNewEvent(ImageDirEventType.EXIT_DIRECTORY, dir.getPathname());
    }

    protected void enterDirectory(FSPath dir, boolean fireEvent)
    {
        // System.err.printf("Entering:%s\n,",dir);
        fireNewEvent(ImageDirEventType.ENTER_DIRECTORY, dir.getPathname());
    }

    public boolean isScanning()
    {
        return this._isScanning;
    }

    protected void setIsScanning(boolean value)
    {
        this._isScanning = value;
    }

    public Map<String, ScanSetInfo> getScanSets()
    {
        return this._scanSets;
    }

    public HashSetList<String> getScanSetUIDs()
    {
        return this._scanSetUids;
    }

    public SubjectInfo getSubjectInfo(String subjectId)
    {
        if (this._scanSets == null)
            return null;

        Map<String, ScanSetInfo> scanSets = this.getScanSets();

        for (String key : scanSets.keySet())
        {
            ScanSetInfo set = scanSets.get(key);
            if (set != null)
            {
                SubjectInfo subjInf = set.getSubjectInfo();
                if ((subjInf != null) && (subjInf.getSubjectID().equals(subjectId)))
                    return subjInf;
            }
        }

        return null;
    }

    public StudyInfo getStudyInfo(String studyUid)
    {
        Map<String, ScanSetInfo> scanSets = this.getScanSets();

        if (scanSets == null)
            return null;

        for (String key : scanSets.keySet())
        {
            ScanSetInfo set = scanSets.get(key);
            if (set != null)
            {
                StudyInfo studyInf = set.getStudyInfo();
                if ((studyInf != null) && (studyInf.getStudyInstanceUID().equals(studyUid)))
                    return studyInf;
            }
        }

        return null;
    }

    public SeriesInfo getSeriesInfo(String seriesUid)
    {
        Map<String, ScanSetInfo> scanSets = this.getScanSets();

        if (scanSets == null)
            return null;

        for (String key : scanSets.keySet())
        {
            ScanSetInfo set = scanSets.get(key);
            if (set != null)
            {
                SeriesInfo seriesInf = set.getSeriesInfo();
                if ((seriesInf != null) && (seriesInf.getSeriesInstanceUID().equals(seriesUid)))
                {
                    return seriesInf;
                }
            }
        }

        return null;
    }

    // ========================================================================
    // Filtering
    // ========================================================================

    protected void initFilters()
    {
        this.filterOptions = new FileFilterOptions();
    }

    protected void setTagFilters(List<DicomTagFilter> filters)
    {
        this.filterOptions.dicomTagFilters = filters;
    }

    protected List<DicomTagFilter> getTagFilters()
    {
        return this.filterOptions.dicomTagFilters;
    }

    public void setFilterOption(FileFilterOptions filterOptions)
    {
        this.filterOptions = filterOptions;
    }

    /**
     * Return maximum length a file may have. Slices aren't that big.
     */
    protected long getMaxSliceFileLength()
    {
        return filterOptions.maxSliceFileSize;
    }

    protected long getMaxNiftiLength()
    {
        return 2 * 1024 * 1024 * 1024;
    }

    /**
     * Split filename into basename without extension and matched extension.
     * 
     */
    public boolean splitBasenameAndExtension(FSPath file, StringHolder basenameH, StringHolder extensionH)
    {
        String shortest = null;
        String matchedExt = null;

        for (String ext : this.filterOptions.validExtensions)
        {
            String baseName = file.getBasename();
            if (baseName.endsWith("." + ext))
            {
                // strip valid extension,including dot:
                String name = baseName.substring(0, baseName.length() - ext.length() - 1);
                // find longest matching extension (.nii vs .nii.gz)
                if (shortest == null)
                {
                    shortest = name;
                    matchedExt = ext;
                }
                else if (name.length() < shortest.length())
                {
                    shortest = name;
                    matchedExt = ext;
                }
            }
        }

        if (shortest != null)
        {
            if (basenameH != null)
                basenameH.value = shortest;

            if (extensionH != null)
                extensionH.value = matchedExt;

            return true;
        }

        return false;
    }

    protected boolean hasValidExtension(String filename)
    {

        if (this.filterOptions.checkExtensions() == false)
        {
            // skip filtering: allow all.
            return true;
        }

        if (this.filterOptions.validExtensions == null)
        {
            // error:
            ClassLogger.getLogger(this.getClass()).warnPrintf("checkExtensions() is true, but no valid are extensions defined!");
            return false;
        }

        for (String ext : filterOptions.validExtensions)
        {
            if (filename.endsWith(ext))
                return true;
        }

        return false;
    }

    // =================
    // Files/Image Sets
    // =================

    public int getNumFiles()
    {
        List<FSPath> files = this.getFiles();

        if (files == null)
            return 0;

        return files.size();
    }

    public int getNumScanSets()
    {
        Set<String> set = this.getScanSetUIDs();

        if (set == null)
        {
            return 0;
        }

        return set.size();
    }

    public List<String> getScanSetUIDList()
    {
        Set<String> setUids = getScanSetUIDs();

        if (setUids == null)
        {
            return new StringList();
        }

        synchronized (setUids)
        {
            int n = setUids.size();
            StringList idList = new StringList(n);

            Iterator<String> it = setUids.iterator();

            while (it.hasNext())
            {
                String uid = it.next();
                idList.add(uid);
            }
            return idList;
        }
    }

    public ScanSetInfo getScanSet(String scanSetUid)
    {
        if (this._scanSets == null)
            return null;

        return this._scanSets.get(scanSetUid);
    }

    protected void addScanSet(String scanSetUid, ScanSetInfo scanSet)
    {
        // Add scanSet and update mappings:
        synchronized (_scanSetUids)
        {
            this._scanSetUids.add(scanSetUid);
        }

        synchronized (_scanSets)
        {
            this._scanSets.put(scanSetUid, scanSet);
        }
    }

    // ==
    public void clear()
    {
        if (this._scanSets != null)
            this._scanSets.clear();

        if (this._scanSetUids != null)
            this._scanSetUids.clear();

    }

    public void dispose()
    {

    }

    // ===
    // Abstract Interface
    // ===

    /**
     * Scan Directory
     * 
     * @param imageDirUri
     * @param recursive
     * @param optScanMonitor
     * @return
     * @throws ScanSetException
     * @throws IOException
     * @throws InterruptedException
     */
    abstract public boolean scanDirectory(URI imageDirUri, boolean recursive, ScanMonitor optScanMonitor) throws ScanSetException,
            IOException, InterruptedException;

    /**
     * Return scanned files.
     */
    abstract public List<FSPath> getFiles();

}

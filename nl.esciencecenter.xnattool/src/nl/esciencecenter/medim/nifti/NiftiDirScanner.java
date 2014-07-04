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

package nl.esciencecenter.medim.nifti;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import nl.esciencecenter.medim.ImageDirEventType;
import nl.esciencecenter.medim.ImageDirScanner;
import nl.esciencecenter.medim.ImageTypes;
import nl.esciencecenter.medim.ScanSetInfo;
import nl.esciencecenter.medim.StudyInfo;
import nl.esciencecenter.medim.SubjectInfo;
import nl.esciencecenter.medim.exceptions.ScanSetException;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;

public class NiftiDirScanner extends ImageDirScanner
{
    private static final ClassLogger logger = ClassLogger.getLogger(NiftiDirScanner.class);

    static
    {
        // logger.setLevelToDebug();
    }

    // === Instance === //

    private ScanMonitor scanMonitor;

    private List<FSPath> imageFiles;

    /**
     * Filename field separator string. Can be anything String.split() accepts.
     */
    private String fieldSeperator = "-";

    public NiftiDirScanner(ImageTypes.DataSetType type)
    {
        super(type);
        logger.debugPrintf("NiftiDirScanner():NEW!");
    }

    @Override
    public boolean scanDirectory(URI imageDirUri, boolean recursive, ScanMonitor optScanMonitor)
            throws ScanSetException, IOException, InterruptedException
    {
        if (optScanMonitor == null)
        {
            this.scanMonitor = new ScanMonitor();
        }
        else
        {
            this.scanMonitor = optScanMonitor;
        }

        this.fireNewEvent(ImageDirEventType.STARTING, "Scanning (NIFTI) directory:" + imageDirUri.getPath());

        this.imageFiles = null;

        // First add all files
        logger.infoPrintf("Scanning NIFTI directory:%s\n", imageDirUri);

        FSUtil fs = FSUtil.getDefault();
        FSPath dir = fs.newLocalDir(imageDirUri);
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
                    if ((filterOptions.checkExtensions() == false) || (hasValidExtension(file.getBasename())))
                    {
                        // disabled for NIFTI.

                        // // check file size here:
                        // if (this.filterOptions.checkFileSize && (file.getFileSize()>this.getMaxNiftiFileLength()) )
                        // {
                        // logger.infoPrintf(" - Skipping: File to big (size %d > max). Skipping: %s\n",file.getFileSize(),file);
                        // }
                        // else
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
            fireNewEvent(ImageDirEventType.MESSAGE, " - Number of (Nifti) files found:" + numFiles);
            exitDirectory(current, true);
        }

        this.imageFiles = heap;
        this.scanMonitor.totalFiles = heap.size();

        this.fireNewEvent(ImageDirEventType.MESSAGE, ">>> Processing " + imageFiles.size() + " nifti files.");

        // Now filter files
        doFilterFiles();

        this.fireNewEvent(ImageDirEventType.IS_DONE, "Done scanning directory:" + imageDirUri.getPath());

        return true;
    }

    protected void doFilterFiles()
    {
        boolean isAtlasSet = (this.getDataSetType() == ImageTypes.DataSetType.NIFTI_ATLASSET);

        List<FSPath> orgFiles = imageFiles;
        ArrayList<FSPath> newFiles = new ArrayList<FSPath>();

        for (FSPath file : orgFiles)
        {
            logger.infoPrintf(" Checking file:%s\n", file);

            // NIFTI ScanSets:
            // Parse: <SubjectId>-<SessionId>.nii[.gz]
            // Parse: <SubjectId>-<SessionId>-<ScanSetId>.nii[.gz]
            // ScanID is copied from SessionID
            //
            // NIFTI ATLAS:
            // Parse: <SubjectID>-<AtlasId>.nii[.gz]
            // Parse: <SubjectId>-<AtlasId>-<AtlasLabel>.nii[.gz]
            // ScanID="atlas"
            //
            // Parse: <SubjectID>-<AtlasId>.txt
            // Parse: <SubjectID>-<AtlasId>.xml
            // Parse: <SubjectID>-<AtlasId>.csv
            //

            // strip one (.dcm) or optionally two (.nii.gz) extensions!
            StringHolder basenameH = new StringHolder();
            StringHolder extensionH = new StringHolder();
            boolean matched = this.splitBasenameAndExtension(file, basenameH, extensionH);

            if (matched == false)
            {
                logger.warnPrintf("File NOT recognized:" + file);
                continue;
            }

            String baseName = basenameH.value;

            String strs[] = baseName.split(getFieldSeperator());

            String subjId;
            String sessId;
            String scanId;
            String atlasLabel = null;
            boolean isBinaryAnnotation = false;

            if (strs.length < 2)
            {
                logger.errorPrintf("Error: Couldn't parse fileName:%s\n", file);
                continue;
            }
            else if (strs.length == 2)
            {
                //
                subjId = strs[0];
                sessId = strs[1];
                scanId = strs[1];
                if (isAtlasSet)
                {
                    isBinaryAnnotation = false; // is source Scan Set for Atlas
                    atlasLabel = null;
                    scanId = "1";
                }
            }
            else
            // filename has at least 3 parts.
            {
                subjId = strs[0];
                sessId = strs[1];
                scanId = strs[2];
                if (isAtlasSet)
                {
                    isBinaryAnnotation = true; // binary image or actual atlas, label must be strs[2]
                    atlasLabel = strs[2];
                    scanId = "1"; // must match scanId above
                }
            }

            // create pseudo unique ids.Assume filename are unique within a project context.
            String patientId = subjId;
            String sessionUid = subjId + "_" + sessId;
            String scanUid = sessionUid + "_" + scanId;

            logger.infoPrintf(" [IDs] \n");
            logger.infoPrintf(" - subjectId    : %s\n", subjId);
            logger.infoPrintf(" - sessionId    : %s\n", sessId);
            logger.infoPrintf(" - scanSetId    : %s\n", scanId);
            logger.infoPrintf(" - atlasLabel   : %s\n", atlasLabel);
            logger.infoPrintf(" - isAnnotation : %s\n", "" + isBinaryAnnotation);
            logger.infoPrintf(" [UIDs] \n");
            logger.infoPrintf(" - sessionUid   : %s\n", sessionUid);
            logger.infoPrintf(" - scanUid      : %s\n", scanUid);

            // create ScanSet if doesn't exist:
            ScanSetInfo scanSet = this.getScanSet(scanUid);
            boolean isNew = false;
            if (scanSet == null)
            {
                if (isAtlasSet)
                {
                    scanSet = new ScanSetInfo(ImageTypes.DataSetType.NIFTI_ATLASSET, scanUid, scanId); // scanset ID.
                }
                else
                {
                    scanSet = new ScanSetInfo(ImageTypes.DataSetType.NIFTI_SCANSET, scanUid, scanId); // scanset ID.
                }

                isNew = true;
            }

            // patient/subject info:
            SubjectInfo subjInfo = scanSet.getSubjectInfo();
            subjInfo.setPatientID(patientId);
            subjInfo.setPatientName(patientId);
            scanSet.setSubjectInfo(subjInfo);

            // study/sesssion
            StudyInfo studyInfo = new StudyInfo(sessionUid, sessId);
            scanSet.setStudyInfo(studyInfo);

            // files
            if (isAtlasSet)
            {
                scanSet.setSeriesDescription("Atlas Collection");

                if (isBinaryAnnotation)
                {
                    scanSet.addAtlasAnnotationFile(atlasLabel, file);
                }
                else
                {
                    // use SubType label as Atlas label: either "scan" or "nuc_scan"
                    scanSet.setAtlasScanSetFile(ImageTypes.getScanSubTypeFileLabel(scanSubType), file);
                }
            }
            else
            {
                scanSet.setSingleScanSetFile(file);
                scanSet.setSeriesDescription("Nifti ScanSet File.");
            }

            // register:
            this.addScanSet(scanUid, scanSet);
        }

        // keep filtered files
        // this.imageFiles=imageFiles;
    }

    /**
     * Return field seperator in filename. Default to '-' (dash).
     * 
     * @return
     */
    public String getFieldSeperator()
    {
        return fieldSeperator;
    }

    public void setFieldSeperator(String fieldSeperatorStr)
    {
        this.fieldSeperator = fieldSeperatorStr;
    }

    @Override
    public List<FSPath> getFiles()
    {
        return imageFiles;
    }

    /**
     * Specify Nifti Scan Sub Type: RAW or NUC.
     */
    public void setScanSubType(ImageTypes.ScanSubType subType)
    {
        this.scanSubType = subType;
    }

    /**
     * @return Nifti Scan Sub Type.
     */
    public ImageTypes.ScanSubType getScanSubType(ImageTypes.ScanSubType subType)
    {
        return scanSubType;
    }

}

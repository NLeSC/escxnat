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

package nl.esciencecenter.xnatclient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.util.ResourceLoader;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.PutMonitor;
import nl.esciencecenter.ptk.web.ResponseInputStream;
import nl.esciencecenter.ptk.web.WebConfig;
import nl.esciencecenter.ptk.web.WebException;
import nl.esciencecenter.xnatclient.XnatClient.FilesCollection;
import nl.esciencecenter.xnatclient.data.ImageFileInfo;
import nl.esciencecenter.xnatclient.data.NewScanInfo;
import nl.esciencecenter.xnatclient.data.XnatFile;
import nl.esciencecenter.xnatclient.data.XnatProject;
import nl.esciencecenter.xnatclient.data.XnatReconstruction;
import nl.esciencecenter.xnatclient.data.XnatScan;
import nl.esciencecenter.xnatclient.data.XnatSession;
import nl.esciencecenter.xnatclient.data.XnatSubject;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;
import nl.esciencecenter.xnatclient.data.XnatTypes.XnatResourceType;
import nl.esciencecenter.xnatclient.exceptions.XnatClientException;

public class XnatCopy implements PutMonitor
{
    public static class FileStats
    {
        public String fileName = null;

        public long fileSize = -1;

        public long bytesTransferred = 0;

        public boolean done = false;

        public boolean started = false;

        public void start(String filename, long filesize)
        {
            this.fileName = filename;
            this.fileSize = filesize;
            this.bytesTransferred = 0;
            this.started = true;
            this.done = false;
        }

        public void done(String filename, long filesize)
        {
            this.fileName = filename;
            this.fileSize = filesize;
            this.bytesTransferred = filesize;
            this.started = true;
            this.done = true;
        }
    }

    public static class DummyMonitor implements XnatCopyMonitor
    {
        @Override
        public void infoPrintf(String format, Object... args)
        {
            logger.infoPrintf(format, args);
        }
    }

    private static ClassLogger logger = ClassLogger.getLogger(XnatCopy.class);

    protected XnatClient sourceClient;

    protected XnatClient destClient;

    protected StringList sourceCollections;

    private ResourceLoader resourceLoader;

    private ImageFileInfo currentFile;

    private FileStats currentFileStats = new FileStats();

    private boolean optAutoLabelEmptyResourceLabels = true;

    private boolean optAutoRemoveReconName = true;

    private XnatCopyMonitor monitor = new DummyMonitor();

    /**
     * Optional List of Subjects, if null copy all subjects.
     */
    private Set<String> subjectNames = null;

    /**
     * Subject names are labels.
     */
    private boolean subjectNameIsLabel = true;

    protected XnatCopy()
    {
    }

    public XnatCopy(WebConfig sourceConfig, WebConfig destConfig) throws WebException
    {
        initSource(sourceConfig);
        initDest(destConfig);
        resourceLoader = new ResourceLoader(); // private copy
    }

    public void setSubjects(Set<String> subjects, boolean useLabel)
    {
        this.subjectNames = subjects;
        subjectNameIsLabel = useLabel;
    }

    public void setMonitor(XnatCopyMonitor monitor)
    {
        this.monitor = monitor;
    }

    public FileStats getCurrentFileStats()
    {
        return currentFileStats;
    }

    private void initDest(WebConfig config) throws WebException
    {
        destClient = new XnatClient(config);
    }

    private void initSource(WebConfig config) throws WebException
    {
        sourceClient = new XnatClient(config);
    }

    public void authenticate(boolean source, boolean dest) throws WebException
    {
        if (source)
        {
            sourceClient.connect();
        }

        if (dest)
        {
            destClient.connect();
        }

    }

    public void copyProjectResourceFiles(String sourceProject, String destProject, Set<XnatResourceType> resourceTypes)
            throws WebException, XnatClientException
    {
        XnatProject sourceP = sourceClient.getProject(sourceProject);
        XnatProject destP = destClient.getProject(destProject);

        monitor.infoPrintf("XNAT Source     : %s/%s\n", sourceClient, sourceP.getID());
        monitor.infoPrintf("XNAT Destination: %s/%s\n", destClient, destP.getID());

        notifyStartProjectCopy(sourceP.getID(), destP.getID(), false);

        List<XnatSubject> sourceSubs = sourceClient.listSubjects(sourceProject);

        for (int numSub = 0; numSub < sourceSubs.size(); numSub++)
        {
            XnatSubject sourceSub = sourceSubs.get(numSub);

            if (copySubject(sourceSub) == false)
            {
                logger.debugPrintf(" - skipping Subject:%s/%s\n", sourceSub.getID(), sourceSub.getLabel());
                continue;
            }

            notifySubjectCopy(sourceSub.getLabel(), false);
            XnatSubject destSub = destClient.createSubject(destProject, sourceSub.getLabel());

            List<XnatSession> sourceSessions = sourceClient.listSessions(sourceSub);
            for (int numSess = 0; numSess < sourceSessions.size(); numSess++)
            {
                XnatSession sourceSess = sourceSessions.get(numSess);
                notifyStartSessionCopy(sourceSess.getLabel(), false);

                XnatSession destSess = destClient.createSession(destSub, sourceSess.getLabel());

                if (resourceTypes.contains(XnatResourceType.SCAN))
                {
                    List<XnatScan> sourceScans = sourceClient.listScans(sourceSess);
                    for (int numScan = 0; numScan < sourceScans.size(); numScan++)
                    {
                        XnatScan sourceScan = sourceScans.get(numScan);
                        notifyStartResourceCopy(XnatResourceType.SCAN, sourceScan.getScanType(), sourceScan.getID(), false);

                        NewScanInfo destInfo = XnatScan.createNewScanInfoFrom(sourceScan);
                        String destScanLbl = destClient.createMrScan(destSess, destInfo);
                        copyResourceFiles(sourceSess, XnatResourceType.SCAN, sourceScan.getID(), destSess, destScanLbl);

                        notifyStartResourceCopy(XnatResourceType.SCAN, sourceScan.getScanType(), sourceScan.getID(), true);
                    }
                }

                if (resourceTypes.contains(XnatResourceType.RECONSTRUCTION))
                {
                    List<XnatReconstruction> sourceRecons = sourceClient.listReconstructions(sourceSess);
                    for (int numScan = 0; numScan < sourceRecons.size(); numScan++)
                    {
                        XnatReconstruction sourceRecon = sourceRecons.get(numScan);
                        notifyStartResourceCopy(XnatResourceType.RECONSTRUCTION, sourceRecon.getReconstructionType(), sourceRecon.getID(),
                                false);

                        String destScanLbl = destClient.createReconstruction(destSess, sourceRecon.getID(),
                                sourceRecon.getReconstructionType());
                        copyResourceFiles(sourceSess, XnatResourceType.RECONSTRUCTION, sourceRecon.getID(), destSess, destScanLbl);

                        notifyStartResourceCopy(XnatResourceType.RECONSTRUCTION, sourceRecon.getReconstructionType(), sourceRecon.getID(),
                                true);
                    }
                }

                notifyStartSessionCopy(sourceSess.getLabel(), true);
            }
            notifySubjectCopy(sourceSub.getLabel(), false);
        }

        notifyStartProjectCopy(sourceP.getID(), destP.getID(), true);
    }

    private boolean copySubject(XnatSubject sourceSub)
    {
        // no subjects means all:
        if (this.subjectNames == null)
        {
            return true;
        }

        String name;

        if (this.subjectNameIsLabel)
            name = sourceSub.getLabel();
        else
            name = sourceSub.getID();

        return (subjectNames.contains(name));
    }

    private void copyResourceFiles(XnatSession sourceSess,
            XnatResourceType resourceType,
            String sourceResourceID,
            XnatSession destSess,
            String destResourceID) throws WebException, XnatClientException
    {

        FilesCollection fileCollections;

        if (resourceType == XnatResourceType.SCAN)
        {
            fileCollections = sourceClient.listScanFiles(sourceSess, destResourceID);
        }
        else if (resourceType == XnatResourceType.RECONSTRUCTION)
        {
            fileCollections = sourceClient.listReconstructionFiles(sourceSess, destResourceID);
        }
        else
        {
            throw new XnatClientException("Invalid XnatResourceType:" + resourceType);
        }

        // check collections:
        String collections[] = fileCollections.keySet().toArray(new String[0]);

        for (String colName : collections)
        {
            List<XnatFile> files = fileCollections.getFileList(colName);
            monitor.infoPrintf(">>>>>Collection '%s'  number of files:%d\n", colName, files.size());

            for (XnatFile file : files)
            {
                ImageFileInfo fileInfo;
                byte fileBytes[];
                fileInfo = file.getImageFileInfo();

                try
                {
                    ResponseInputStream inps = sourceClient.getResourceFileInputStream(sourceSess, resourceType, sourceResourceID, colName,
                            file.getBasename());
                    fileBytes = resourceLoader.readBytes(inps);
                    try
                    {
                        inps.close();
                    }
                    catch (IOException e)
                    {
                        ;
                    }
                    ;

                    String destCollectionLabel = renameResourceLabel(fileInfo, colName);
                    monitor.infoPrintf(">>>>>File [%s->%s]/%s num bytes=%d\n", colName, destCollectionLabel, file.getBasename(),
                            fileBytes.length);

                    ImageFileInfo destInfo = fileInfo.duplicate();
                    renameFile(destInfo);
                    this.startFile(destInfo);
                    String resultStr = destClient.putResourceFile(destSess, resourceType, destResourceID, destCollectionLabel, fileBytes,
                            destInfo, this);

                    this.fileDone(fileInfo);

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    protected boolean renameFile(ImageFileInfo info)
    {
        if (info.destinationFileBasename == null)
        {
            return false;
        }

        if (this.optAutoRemoveReconName)
        {
            // old niftis are called "recon". Remove them.
            String extStr = ".recon.nii.gz";
            String repStr = ".nii.gz";

            if (info.destinationFileBasename.endsWith(extStr))
            {
                String fileName = info.destinationFileBasename;
                fileName = fileName.substring(0, fileName.length() - extStr.length());
                fileName += repStr;
                logger.infoPrintf("Renaming:%s ->%s\n", info.destinationFileBasename, fileName);
                info.destinationFileBasename = fileName;
                return true;
            }
        }

        return false;
    }

    private String renameResourceLabel(ImageFileInfo sourceFileInfo, String colName)
    {
        String formatType = sourceFileInfo.getImageFormatType();

        // auto map empty labels to Image type: "DICOM"|"NIFTI".

        if (this.optAutoLabelEmptyResourceLabels)
        {
            if (StringUtil.isEmpty(colName))
            {
                colName = formatType.toString().toUpperCase();
            }
        }

        return colName;
    }

    private void startFile(ImageFileInfo fileInfo)
    {
        this.currentFile = fileInfo;
        this.currentFileStats.start(fileInfo.getDestinationFilename(), fileInfo.getFileSize());
    }

    private void fileDone(ImageFileInfo fileInfo)
    {
        this.currentFile = fileInfo;
        this.currentFileStats.done(fileInfo.getDestinationFilename(), fileInfo.getFileSize());
    }

    @Override
    public void bytesWritten(long numBytes)
    {
        logger.debugPrintf("Update:bytesWritten=%d\n", numBytes);
        this.currentFileStats.bytesTransferred = numBytes;

    }

    @Override
    public void putDone()
    {
        this.currentFileStats.done = true;
    }

    private void notifyStartProjectCopy(String sourceProjectID, String destProjectID, boolean done)
    {
        monitor.infoPrintf(">Copying Project:%s -> %s (%s)\n", sourceProjectID, destProjectID, done ? "DONE" : "START");
    }

    private void notifySubjectCopy(String label, boolean done)
    {
        monitor.infoPrintf(">>Copying Subject:%s (%s)\n", label, done ? "DONE" : "START");
    }

    private void notifyStartSessionCopy(String label, boolean done)
    {
        monitor.infoPrintf(">>>Copying Session:%s (%s)\n", label, done ? "DONE" : "START");
    }

    private void notifyStartResourceCopy(XnatResourceType scan, String subType, String id, boolean done)
    {
        monitor.infoPrintf(">>>>Copying Resource <%s>:(%s)%s (%s)\n", scan, subType, id, done ? "DONE" : "START");
    }

    public XnatClient getSourceXnatClient()
    {
        return sourceClient;
    }

    public XnatClient getDestXnatClient()
    {
        return destClient;
    }

}

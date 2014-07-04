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

package nl.esciencecenter.xnattool;

import nl.esciencecenter.ptk.task.ITaskMonitor;
import nl.esciencecenter.ptk.task.TaskMonitorAdaptor;
import nl.esciencecenter.xnattool.ui.XnatToolPanelController;

/**
 * Monitor Adaptor
 */
public class UploadMonitor extends TaskMonitorAdaptor implements UploadMonitorListener
{
    String currentTask;

    String currentCollectionName;

    String currentFile;

    // stats:
    int collectionSizesTodo[];

    int collectionSizesDone[];

    int totalFilesTodo = 0;

    int totalFilesDone = 0;

    int currentCollectionId = -1;

    ITaskMonitor monitor;

    public UploadMonitor(XnatToolPanelController uploaderController)
    {
        // uploader=uploaderController;
        this.monitor = this;
    }

    @Override
    public void logPrintf(String format, Object... args)
    {
        super.logPrintf(format, args);
    }

    @Override
    public void notifyStartUpload(String taskName, int filesSizes[])
    {
        this.currentTask = taskName;
        this.collectionSizesTodo = filesSizes;
        this.collectionSizesDone = new int[filesSizes.length];

        for (int i = 0; i < collectionSizesTodo.length; i++)
        {
            this.totalFilesTodo += collectionSizesTodo[i];
        }

        monitor.startTask(taskName, totalFilesTodo);
    }

    @Override
    public void notifyCollectionStart(int collectionId, String collectionName)
    {
        this.currentCollectionName = collectionName;
        this.currentCollectionId = collectionId;

        this.logPrintf("Starting Collection #%d:%s\n", collectionId, collectionName);

        monitor.startSubTask(collectionName, collectionSizesTodo[collectionId]);
    }

    @Override
    public void notifyCollectionDone(int collectionId)
    {
        // totalFilesDone+=this.collectionSizesTodo[collectionId];
        monitor.updateTaskDone(totalFilesDone);
        monitor.endSubTask(this.currentCollectionName);

        if (collectionId == this.collectionSizesTodo.length - 1)
        {
            monitor.endTask(currentTask);
        }
    }

    @Override
    public void notifyFileStart(int collectionId, int fileNumber, long numBytesTodo, String filename)
    {
        this.currentFile = filename;
        logPrintf("Uploading Collection/File:%d/%d:\n", (collectionId + 1), (fileNumber + 1));
        logPrintf(" - %s\n", filename);
    }

    @Override
    public void notifyFileDone(int collectionId, int fileNr)
    {
        logPrintf(" - File Done.\n");

        if (collectionSizesDone != null)
        {
            collectionSizesDone[collectionId]++; // use increment and count actual nr. of files uploaded.
        }
        this.totalFilesDone++;

        monitor.updateSubTaskDone(this.currentCollectionName, collectionId);
        // running total:
        monitor.updateTaskDone(totalFilesDone);
    }

    @Override
    public void updateFileBytesUploaded(int scanNr, int fileNr, long numBytes)
    {
        // file upload is sub sub task;
    }

    public int getCurrentCollectionId()
    {
        return this.currentCollectionId;
    }

    public int getTotalFilesTodo()
    {
        return this.totalFilesTodo;
    }

    public int getTotalFilesDone()
    {
        return this.totalFilesDone;
    }

    public int getCurrentFilesDone()
    {
        if ((currentCollectionId < 0) || (collectionSizesDone == null))
        {
            return -1;
        }

        if ((currentCollectionId < 0) || (currentCollectionId >= collectionSizesDone.length))
        {
            return -1;
        }

        return collectionSizesDone[currentCollectionId];
    }

    public int getNumCollectionsTodo()
    {
        if ((collectionSizesTodo == null) || (collectionSizesTodo.length <= 0))
        {
            return -1;
        }
        return this.collectionSizesTodo.length;
    }

}

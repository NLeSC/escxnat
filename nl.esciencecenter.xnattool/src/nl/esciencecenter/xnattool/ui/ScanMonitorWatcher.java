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

package nl.esciencecenter.xnattool.ui;
import nl.esciencecenter.medim.ImageDirEvent;
import nl.esciencecenter.medim.ImageDirEventType;
import nl.esciencecenter.medim.ImageDirScannerListener;
import nl.esciencecenter.medim.ImageDirScanner.ScanMonitor;
import nl.esciencecenter.ptk.task.TaskMonitorAdaptor;

public class ScanMonitorWatcher extends TaskMonitorAdaptor implements ImageDirScannerListener
{
	protected XnatToolPanelController uploadController; 
	protected ScanMonitor scanMonitor=null;
	
	public ScanMonitorWatcher(XnatToolPanelController uploaderController, ScanMonitor scanMonitor)
	{
		this.uploadController=uploaderController; 
		this.startTask("Scanning Directory", 1000); 
		logPrintf("Starting scan...\n");
		this.scanMonitor=scanMonitor; 
	}

	protected void updateFilesDone(boolean isDone)
	{
	    // Change file stats, start with a small offset to trigger >0% as progress. 
	    // Also when zero files have scanned set total todo to 1000 to avoid 100% as current progress :-/... 
	    if (scanMonitor.totalFiles>0)
	    {
	        taskStats.name="Processing Files:"+(scanMonitor.currentFile+1)+" out of:"+scanMonitor.totalFiles; 
	        taskStats.todo = 10+scanMonitor.totalFiles; 
	    }
	    else
	    {
	        taskStats.todo = 1000; //  
	    }
	    
        this.updateTaskDone(10+scanMonitor.currentFile);
	}
	
	@Override
	public void notifyImageDirScannerEvent(ImageDirEvent e)
	{
	    updateFilesDone((e.eventType==ImageDirEventType.IS_DONE)); 
	        
	    switch(e.eventType)
	    {
	        case MESSAGE:
	            logPrintf("%s\n",e.identifierOrMessage);
	            break;
	        case ENTER_DIRECTORY:
	            logPrintf("Entering (sub) directory:%s\n",e.identifierOrMessage);
	            break;
	        case EXIT_DIRECTORY:
	            logPrintf("Finished (sub) directory:%s\n",e.identifierOrMessage);
	            break;
	        case NEW_SCANSET: 
	            logPrintf("Registering new ScanSet:%s\n",e.identifierOrMessage);
	            break;
	        case NEW_STUDY:
	            logPrintf("Registering new Study  :%s\n",e.identifierOrMessage);
	            break;
	        case NEW_SUBJECT: 
	            logPrintf("Registering new Subject:%s\n",e.identifierOrMessage);
	            break;
	        case UPDATE_STATS:
	            // updateFilesDone((e.eventType==ImageDirEventType.IS_DONE)); 
	            break; 
	        default: 
	            this.logPrintf("%s:%s\n",e.eventType,e.identifierOrMessage);
	            break; 
	    }
		
	}
	
}

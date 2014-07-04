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

package nl.esciencecenter.xnattool.util;

import java.util.HashSet;
import java.util.Set;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.ui.UI;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.WebConfig;
import nl.esciencecenter.ptk.web.WebException;
import nl.esciencecenter.xnatclient.XnatCopy;
import nl.esciencecenter.xnatclient.XnatCopy.FileStats;
import nl.esciencecenter.xnatclient.XnatCopyMonitor;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;
import nl.esciencecenter.xnatclient.data.XnatTypes.XnatResourceType;
import nl.esciencecenter.xnattool.XnatToolConfig;

/**
 * Performs an XnatCopy and monitors the result.
 * 
 * @author Piter T. de Boer.
 */
public class XnatCopyCMD
{
    public class XnatCopyMonitorAdaptor implements XnatCopyMonitor, Runnable
    {
        protected XnatCopy xnatCopy;

        protected boolean stop = false;

        public XnatCopyMonitorAdaptor(XnatCopy xnatCopy)
        {
            this.xnatCopy = xnatCopy;
        }

        @Override
        public void infoPrintf(String format, Object... args)
        {
            System.out.printf("XnatCopyMonitor:");
            System.out.printf(format, args);
        }

        @Override
        public void run()
        {
            doLoop();
        }

        public void stop()
        {
            stop = true;
        }

        protected void doLoop()
        {

            while (stop == false)
            {
                updateStats();

                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {
                    stop = true;
                }

                if (Thread.currentThread().isInterrupted())
                {
                    stop = true;
                }

            }

            infoPrintf("Stopped\n");
        }

        protected void updateStats()
        {
            FileStats stats = xnatCopy.getCurrentFileStats();
            if (stats.started == true)
            {
                infoPrintf(" - %s: Copied %s out of %s\n", stats.fileName, stats.bytesTransferred, stats.fileSize);
            }
        }
    }

    protected XnatToolConfig sourceConf;

    protected XnatToolConfig destConf;

    protected String sourceProject;

    protected String destProject;

    protected XnatCopy xnatCopy = null;

    private WebConfig sourceWebConfig;

    private WebConfig destWebConfig;

    Set<XnatResourceType> resourceTypeSet = null;

    private HashSet<String> imageFormatTypeSet;

    private XnatCopyMonitorAdaptor monitor;

    private Thread thread;

    private boolean verbose;

    public XnatCopyCMD(XnatToolConfig sourceConf_, XnatToolConfig destConf_) throws WebException
    {
        sourceConf = sourceConf_;
        destConf = destConf_;
        init();
    }

    private void init() throws WebException
    {
        sourceWebConfig = sourceConf.getWebConfig();
        sourceWebConfig.useAuthentication();
        destWebConfig = destConf.getWebConfig();
        destWebConfig.useAuthentication();

        xnatCopy = new XnatCopy(sourceWebConfig, destWebConfig);

        resourceTypeSet = new HashSet<XnatResourceType>();
        resourceTypeSet.add(XnatResourceType.SCAN);
        resourceTypeSet.add(XnatResourceType.RECONSTRUCTION);

        // more//all/wildcards ?
        imageFormatTypeSet = new HashSet<String>();
        imageFormatTypeSet.add("" + ImageFormatType.DICOM);
        imageFormatTypeSet.add("" + ImageFormatType.NIFTI);

        this.monitor = new XnatCopyMonitorAdaptor(xnatCopy);
        xnatCopy.setMonitor(monitor);

    }

    /**
     * @return Return actual XnatCopy object.
     */
    public XnatCopy getXnatCopy()
    {
        return xnatCopy;
    }

    public void setProjectIDs(String sourceProjectID, String destProjectID)
    {
        this.sourceProject = sourceProjectID;
        this.destProject = destProjectID;
    }

    public void setSourceCredentials(String user, Secret password)
    {
        sourceWebConfig.setCredentials(user, password);
    }

    public void setDestCredentials(String user, Secret password)
    {
        destWebConfig.setCredentials(user, password);
    }

    public void setUI(UI ui)
    {
        xnatCopy.getSourceXnatClient().setUI(ui);
        xnatCopy.getDestXnatClient().setUI(ui);
    }

    public void authenticate() throws WebException
    {
        xnatCopy.authenticate(true, true);
    }

    public void setSubjectLabels(Set<String> subjects)
    {
        xnatCopy.setSubjects(subjects, true);
    }

    public void doCopy() throws Exception
    {
        ClassLogger.getLogger(XnatCopy.class).setLevelToInfo();

        try
        {
            // Set<XnatResourceType> set=new SetList<XnatResourceType>();

            Set<XnatResourceType> set = new HashSet<XnatResourceType>();
            set.add(XnatResourceType.SCAN);
            set.add(XnatResourceType.RECONSTRUCTION);

            startMonitor();

            xnatCopy.copyProjectResourceFiles(sourceProject, destProject, set);
        }
        catch (Exception e)
        {
            throw e;
        }

    }

    protected void startMonitor()
    {
        this.thread = new Thread(monitor);
        this.thread.start();
    }

    protected void stoptMonitor()
    {
        this.monitor.stop();
    }

    public void setVerbose(boolean verbose)
    {
        this.verbose = verbose;
    }

}

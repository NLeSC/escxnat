/*
 * Copyright 2012-2013 Netherlands eScience Center.
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
package tests.other.ui.panels;

import nl.esciencecenter.ptk.task.ActionTask;
import nl.esciencecenter.ptk.task.ITaskMonitor;
import nl.esciencecenter.xnattool.ui.UploadMonitorDialog;

public class TestUploadMonitorDialog
{

    /**
     * Auto-generated main method to display this JPanel inside a new JFrame.
     */
    public static void main(String[] args)
    {

        ActionTask task = new ActionTask(null, "TaskMonitorDailog tester")
        {
            public void doTask()
            {

                ITaskMonitor monitor = this.getMonitor();

                int N = 100;
                int M = 10;
                int sleep = 100;

                monitor.startTask("DailogTest", N);

                for (int i = 0; i < N; i++)
                {
                    String subTask = "Subtask:" + i;

                    monitor.startSubTask(subTask, M);
                    monitor.logPrintf("New subtask:%s\n", subTask);

                    for (int j = 0; j < M; j++)
                    {

                        try
                        {
                            Thread.sleep(sleep);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }

                        monitor.updateSubTaskDone(subTask, j);

                        if (this.isCancelled())
                        {
                            monitor.logPrintf("***Cancelled***\n", subTask);
                            return;
                        }
                    }

                    monitor.endSubTask(subTask);
                    monitor.updateTaskDone(i);
                }

                monitor.endTask(null);

            }

            @Override
            public void stopTask()
            {
            }
        };

        task.startTask();

        UploadMonitorDialog.showUploadMonitorDialog(null, task, 0);

        //
        // frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        // frame.pack();
        // frame.setVisible(true);

    }
}

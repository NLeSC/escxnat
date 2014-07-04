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

import java.awt.AWTEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import nl.esciencecenter.medim.dicom.DicomProcessingProfile.*;
import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.xnattool.DataSetConfig;

public class DatasetSettingsPanelController implements ActionListener, FocusListener
{
    private DatasetSettingsPanel settingsPanel;

    public DatasetSettingsPanelController(DatasetSettingsPanel settingsPanel)
    {
        this.settingsPanel = settingsPanel;
    }

    protected XnatToolPanelController getMasterController()
    {
        return settingsPanel.getMasterController();
    }

    protected DataSetConfig getDataSetConfig()
    {
        XnatToolPanelController mctrl = this.settingsPanel.getMasterController();
        if (mctrl == null)
            return null;

        return mctrl.getDataSetConfig();
    }

    @Override
    public void focusGained(FocusEvent e)
    {
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        handleEvent(e);
    }

    @Override
    public void actionPerformed(ActionEvent event)
    {
        handleEvent(event);
    }

    protected void handleEvent(AWTEvent event)
    {
        Object source = event.getSource();
        String actionStr;
        boolean modCtrl = false;
        boolean temporyFocusEvent = false;
        boolean lostFocus = false;
        boolean isFocusEvent = false;

        if (event instanceof FocusEvent)
        {
            isFocusEvent = true;
            FocusEvent focusEvent = (FocusEvent) event;
            temporyFocusEvent = focusEvent.isTemporary();
            lostFocus = (focusEvent.getID() == FocusEvent.FOCUS_LOST);
        }
        else if (event instanceof ActionEvent)
        {
            actionStr = ((ActionEvent) event).getActionCommand();
            modCtrl = ((((ActionEvent) event).getModifiers() & ActionEvent.CTRL_MASK) > 0);
        }

        DataSetConfig config = getDataSetConfig();

        // focus sensitive fields!
        // filter for permanent focus lost event only.

        if ((isFocusEvent == false) || ((lostFocus) && (temporyFocusEvent == false)))
        {
            if (source == settingsPanel.sourceIdTF)
            {
                String newSourceId = settingsPanel.getSourceID();
                String oldSourceId = this.getMasterController().getDataSetSourceID();

                if (StringUtil.compare(oldSourceId, newSourceId) != 0)
                {
                    doUpdateSourceId(oldSourceId, newSourceId);
                }
            }
        }

        // focus ignorant fields:
        if (isFocusEvent == false)
        {
            if (source == settingsPanel.passwordTF)
            {
                doAuthenticateKey(settingsPanel.getPasswordSecret());
            }
            else if (source == settingsPanel.subjectIdCB)
            {
                String valStr = settingsPanel.subjectIdCB.getSelectedItem().toString();
                if (config == null)
                    getMasterController().showError("No DataSet Configuration. Please create one");
                else
                    config.setSubjectKeyType(SubjectKeyType.valueOf(valStr));
            }
            else if (source == settingsPanel.sessionKeyCB)
            {
                String valStr = settingsPanel.sessionKeyCB.getSelectedItem().toString();
                if (config == null)
                    getMasterController().showError("No DataSet Configuration. Please create one");
                else
                    config.setSessionKeyType(SessionKeyType.valueOf(valStr));
            }
            else if (source == settingsPanel.scanUidCB)
            {
                String valStr = settingsPanel.scanUidCB.getSelectedItem().toString();
                if (config == null)
                    getMasterController().showError("No DataSet Configuration. Please create one");
                else
                    config.setScanKeyType(ScanKeyType.valueOf(valStr));
            }
            else if (source == settingsPanel.createKeyBut)
            {
                doCreateNewKey(modCtrl);
            }
        }
    }

    protected boolean doCreateNewKey(boolean modCtrl)
    {
        return this.getMasterController().doCreateNewDatasetKey(modCtrl);
    }

    protected void doAuthenticateKey(Secret passPhrase)
    {
        XnatToolPanelController masterCtrl = this.getMasterController();

        String sourceId = settingsPanel.getSourceID();

        // copy source If from Settings Panel back !
        masterCtrl.updateSourceIdFields(sourceId);

        try
        {
            masterCtrl.doAuthenticateDataSetKeys(sourceId, passPhrase);
        }
        catch (Exception e)
        {
            masterCtrl.handle("Error authenticating DataSet.", e);
        }

        DataSetConfig conf = getDataSetConfig();
        if (conf == null)
            return; // false
    }

    protected boolean doUpdateSourceId(String oldSourceId, String newSourceId)
    {
        XnatToolPanelController ctrl = this.getMasterController();

        boolean okResult = ctrl.askOkCancel(
                "Change Owner ID?",
                "Do you want to change the Owner ID ?\n"
                        + "If you change the Owner ID you have to create a new Key and Authenticate it with a new Passphrase.\n"
                        + "Continuing will invalidate your old key and DataSet!");

        if (okResult == false)
        {
            settingsPanel.setSourceId(oldSourceId);
            return false;
        }

        try
        {
            // update SourceID -> Delete Keys, will also clear KeyFields!
            ctrl.doDeleteDatasetKeys(true);
        }
        catch (Exception e)
        {
            ctrl.handle("Couldn't delete Encryption Keys", e);
            return false;
        }

        updateSourceIdFields(newSourceId);
        return true;
    }

    protected void updateSourceIdFields(String newId)
    {
        // redirect to master controller to update both panel fields!
        getMasterController().updateSourceIdFields(newId);
    }

}

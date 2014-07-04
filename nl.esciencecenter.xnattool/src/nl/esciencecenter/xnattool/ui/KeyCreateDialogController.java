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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.data.SecretHolder;
import nl.esciencecenter.xnattool.DataSetConfig;
import nl.esciencecenter.xnattool.XnatTool;

public class KeyCreateDialogController implements ActionListener, FocusListener
{

    private KeyCreateDialog createDialog;

    public KeyCreateDialogController(KeyCreateDialog keyCreateDialog)
    {
        this.createDialog = keyCreateDialog;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Object source = e.getSource();

        // String cmd=e.getActionCommand();
        try
        {
            if (source == createDialog.passphraseTF)
            {
                updatePassphrase();
            }
            else if (source == createDialog.getCreateKeyBut())
            {
                updatePassphrase();
            }
            else if (source == createDialog.showPPCB)
            {
                createDialog.togglePasswordField(createDialog.showPPCB.isSelected());
            }
            else if (source == createDialog.showRawKeyCB)
            {
                createDialog.showKey(createDialog.showRawKeyCB.isSelected());
            }

        }
        catch (Exception ex)
        {
            handle("Error", ex);
        }
    }

    @Override
    public void focusGained(FocusEvent e)
    {
        // Object source=e.getSource();
    }

    @Override
    public void focusLost(FocusEvent e)
    {
        Object source = e.getSource();
        try
        {
            if (source == createDialog.passphraseTF)
            {
                updatePassphrase();
            }
        }
        catch (Exception ex)
        {
            handle("Failed to create key from text\n", ex);
        }
    }

    private void updatePassphrase() throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        char chars[] = createDialog.getPassphraseField().getPassword();
        if (chars != null)
        {
            boolean result = createDataSetKey();
            this.createDialog.okBut.setEnabled(result);
        }
    }

    private void handle(String action, Exception ex)
    {
        if (createDialog.getMasterController() != null)
        {
            createDialog.getMasterController().handle(action, ex);
        }
        else
        {
            // stand alone:
            System.err.printf("Error:%s\n", action);
            System.err.printf("Exception:%s\n", ex);
            ex.printStackTrace();
        }
    }

    protected boolean createDataSetKey()
    {
        XnatToolPanelController masterController = createDialog.getMasterController();

        String sourceId = masterController.getDataSetSourceID();
        DataSetConfig dataSetConfig = masterController.getDataSetConfig();
        if (dataSetConfig == null)
        {
            masterController.showError("No DataSet Configuration Created. Create one first!");
            return false;
        }

        String dataSetName = dataSetConfig.getDataSetName();

        Secret passP1 = createDialog.getPassphraseSecret();

        if ((passP1.getChars() == null) || (passP1.getChars().length < 8))
        {
            masterController.showError("Passphrase is to short!");
            return false;
        }

        SecretHolder valueH = new SecretHolder();
        masterController.uiAskField("Verify passphrase.", "Verify your passphrase Please for DataSet:" + dataSetName, valueH);

        if (valueH.value == null || valueH.value.isEmpty())
        {
            masterController.logger.debugPrintf("KeyCreateDialogController:doCreateNewKey(): Cancelled\n");
            return false;
        }

        Secret secret2 = valueH.value;
        if (!passP1.equals(secret2))
        {
            masterController.showError("Passphrases do not match!");
            return false;
        }

        try
        {
            // Use passphrase both as key digest source and encryption password
            // for now.

            Secret keySourceText = secret2;
            Secret keyPassphrase = secret2;
            XnatTool xnatTool = masterController.getXnatTool();
            xnatTool.initializeEncryptionKeyFromSourceText(sourceId, keySourceText, keyPassphrase);
            // authenticate and initalize encryption:
            xnatTool.authenticateEncryptionKeys(sourceId, keyPassphrase, false);
            byte rawKey[] = xnatTool.getCurrentDataSetConfig().getEncryptionKey();
            this.createDialog.setKey(rawKey, this.createDialog.showRawKeyCB.isSelected());
            this.createDialog.setPassphrase(secret2);
            // / update main panel as well:
            this.createDialog.getMasterController().updatePassphrase(keyPassphrase);

            return true;
        }
        catch (Exception e)
        {
            this.handle("Could not initialize new Encryption Key.", e);
        }

        return false;
    }

}

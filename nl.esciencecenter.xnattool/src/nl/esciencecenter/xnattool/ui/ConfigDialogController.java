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
import java.io.IOException;

import nl.esciencecenter.xnattool.XnatTool;
import nl.esciencecenter.xnattool.XnatToolConfig;

public class ConfigDialogController implements ActionListener
{
    private ConfigDialog configDialog;

    private String orgConfigConfigDir;

    private String orgImageCacheDir;

    private String newConfigConfigDir;

    private String newImageCacheDir;

    ConfigDialogController(ConfigDialog dialog)
    {
        this.configDialog = dialog;
    }

    protected XnatToolPanelController getMasterController()
    {
        return configDialog.getMasterController();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        handleEvent(e);
    }

    protected void handleEvent(ActionEvent e)
    {
        String cmdString = ((ActionEvent) e).getActionCommand();

        if (cmdString == null)
            return;

        UIAction cmd = UIAction.valueOf(cmdString);

        switch (cmd)
        {
            case CONFIG_CREATE_CONFIGSDIR:
            {
                createConfigDir();
                break;
            }
            case CONFIG_CREATE_IMAGECACHEDIR:
            {
                createImageCacheDir();
                break;
            }
            case CLEAR_IMAGECACHEDIR:
            {
                clearImageCacheDir();
                break;
            }
            case FIELD_CONFIGDIR_CHANGED:
            {
                this.updateDatasetConfigDirChanged();
                break;
            }
            case FIELD_IMAGECACHEDIR_CHANGED:
            {
                this.updateImageCacheDirChanged();
                break;
            }
            // Options
            case OPTION_CACHE_KEEP_PROCESSED_DICOM:
            {
                boolean val = this.configDialog.keepProcessedDicomCB.isSelected();
                this.getXnatTool().getToolConfig().setKeepProcessedDicomFile(val);
            }
            case OPTION_AUTO_CREATE_ID_MAPPINGS:
            {
                boolean val = this.configDialog.autoCreateIdMappingsCB.isSelected();
                this.getXnatTool().getToolConfig().setAutoCreateMappingsFile(val);
            }
            case OPTION_AUTO_EXTRACT_META_DATA:
            {
                boolean val = this.configDialog.autoExtractMetaDataCB.isSelected();
                this.getXnatTool().getToolConfig().setAutoExtractMetaData(val);
            }
            default:
            {

            }
        } // switch
    }

    protected void clearImageCacheDir()
    {
        // check:
        java.net.URI cacheUri = getXnatTool().getToolConfig().getImageCacheDir();
        if (cacheUri == null)
        {
            handle("No CacheDirectory configurated. cacheUri==null!", new NullPointerException(
                    "Error, NO CacheDirectory configurated! (cacheUri==null!)"));
        }

        try
        {
            this.getXnatTool().clearImageCacheDir();
        }
        catch (IOException e)
        {
            handle("Failed to clear cache directory:" + cacheUri.getPath(), e);
        }
    }

    private boolean createImageCacheDir()
    {
        String loc = configDialog.getImageCacheDirText();

        try
        {
            java.net.URI uri = this.getXnatTool().getFSUtil().resolvePathURI(loc);
            getXnatTool().getFSUtil().mkdirs(uri.getPath());
            updateImageCacheDirChanged();
            return true;
        }
        catch (IOException e)
        {
            handle("Failed to create Image Cache directory:" + loc, e);
        }

        return false;
    }

    private boolean createConfigDir()
    {
        String loc = configDialog.getDataSetConfigDirText();

        try
        {
            java.net.URI uri = this.getXnatTool().getFSUtil().resolvePathURI(loc);
            getXnatTool().getFSUtil().mkdirs(uri.getPath());
            updateDatasetConfigDirChanged();
            return true;
        }
        catch (IOException e)
        {
            handle("Failed to create Dataset Configuration Directory:" + loc, e);
        }

        return false;
    }

    private void handle(String errorTxt, Exception e)
    {
        if (getMasterController() != null)
        {
            getMasterController().handle(errorTxt, e);
        }
        else
        {
            // Mocking:
            System.err.println("Error:" + errorTxt);
            e.printStackTrace();
        }
    }

    private XnatTool getXnatTool()
    {
        return getMasterController().getXnatTool();
    }

    public void updateSettingsFromTool()
    {
        XnatTool tool = this.getXnatTool();

        if (tool == null)
            return; // gui mocking mode;

        XnatToolConfig config = tool.getToolConfig();

        this.orgConfigConfigDir = config.getDataSetsConfigDir().getPath();
        this.orgImageCacheDir = config.getImageCacheDir().getPath();

        this.configDialog.setDatasetsConfigDir(orgConfigConfigDir);
        this.configDialog.setImageCacheDir(orgImageCacheDir);
        updateImageCacheDirChanged();
        updateDatasetConfigDirChanged();

        this.newConfigConfigDir = null;
        this.newImageCacheDir = null;

        this.configDialog.keepProcessedDicomCB.setSelected(config.getKeepProcessedDicomFile());
        this.configDialog.autoCreateIdMappingsCB.setSelected(config.getAutoCreateMappingsFile());
        this.configDialog.autoExtractMetaDataCB.setSelected(config.getAutoExtractMetaData());

    }

    protected void doCancel()
    {
        this.newConfigConfigDir = null;
        this.newImageCacheDir = null;

        this.configDialog.setVisible(false);
    }

    protected void doOk()
    {
        // update configuration:
        this.newConfigConfigDir = configDialog.getDataSetConfigDirText();
        this.newImageCacheDir = configDialog.getImageCacheDirText();
        //
        boolean result = this.createImageCacheDir();
        if (result == false)
        {
            result = getMasterController().askOkCancel("Error", "Could not create Image Cache Dir. Exit anyway?");
            if (result)
            {
                this.dispose();
                return;
            }
            else
            {
                return;
            }
        }

        result = this.createConfigDir();
        if (result == false)
        {
            result = getMasterController().askOkCancel("Error", "Could not DataSet configuration directory. Exit anyway?");
            if (result)
            {
                this.dispose();
                return;
            }
            else
            {
                return;
            }
        }

        this.dispose();
    }

    public void dispose()
    {
        this.configDialog.setVisible(false);
    }

    public String getNewDatasetConfigDir()
    {
        return this.newConfigConfigDir;
    }

    public String getNewImageCacheDir()
    {
        return this.newImageCacheDir;
    }

    public void updateImageCacheDirChanged()
    {
        String loc = configDialog.getImageCacheDirText();

        try
        {
            java.net.URI uri = this.getXnatTool().getFSUtil().resolvePathURI(loc);
            boolean exists = this.getXnatTool().getFSUtil().existsDir(uri.getPath());
            this.configDialog.createImageCacheDirBut.setEnabled(exists == false);
        }
        catch (IOException e)
        {
            handle("Syntax Error: Invalid Image Cache directory Location:" + loc, e);
        }

    }

    public void updateDatasetConfigDirChanged()
    {
        String loc = configDialog.getDataSetConfigDirText();

        try
        {
            java.net.URI uri = this.getXnatTool().getFSUtil().resolvePathURI(loc);
            boolean exists = this.getXnatTool().getFSUtil().existsDir(uri.getPath());
            this.configDialog.createDatasetsConfigDirBut.setEnabled(exists == false);
        }
        catch (IOException e)
        {
            handle("Syntax Error: Invalid Dataset Configuration Location:" + loc, e);
        }
    }
}

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

public enum UIAction
{
    ACTION_LOGIN,
    ACTION_SCAN,
    ACTION_UPLOAD,
    ACTION_EXIT,
    CB_PROJECT_CHANGED,
    FIELD_OWNERID_CHANGED,
    FIELD_PASSWORD_CHANGED,
    // filter
    DATASET_TYPE_CHANGED,
    DATASET_FILE_FILTER_CHANGED,
    DATASET_FILTER_CHANGED,
    DATASET_MAGIC_CB_CHANGED,
    SCANSUBTYPE_OPTIONS_CHANGED,
    FIELD_SOURCEDIR_CHANGED,
    // config:
    CONFIG_CREATE_CONFIGSDIR,
    CONFIG_CREATE_IMAGECACHEDIR,
    FIELD_IMAGECACHEDIR_CHANGED,
    FIELD_CONFIGDIR_CHANGED,
    CLEAR_IMAGECACHEDIR,
    // data sets
    CB_DATASET_CHANGED,
    BUT_NEW_DATASET,
    BUT_CREATE_INIT_KEY,
    BUT_AUTHENTICATE_KEY,
    BUT_CHANGE_OWNERID,
    ACTION_VIEW_SCANSETINF,
    // Meta Data
    UPLOAD_CSV_TO_PROJECT,
    // Main Menu
    MAIN_MENU_HELP,
    MAIN_MENU_ABOUT,
    MAIN_LOCATION_SET_DEFAULT,
    MAIN_DEBUGLEVEL_TO_DEBUG,
    MAIN_DEBUGLEVEL_TO_INFO,
    MAIN_DEBUGLEVEL_TO_ERROR,
    MAIN_CONFIGURATION,
    // Settings
    OPTION_CACHE_KEEP_PROCESSED_DICOM,
    OPTION_AUTO_CREATE_ID_MAPPINGS,
    OPTION_AUTO_EXTRACT_META_DATA,

    ;
}
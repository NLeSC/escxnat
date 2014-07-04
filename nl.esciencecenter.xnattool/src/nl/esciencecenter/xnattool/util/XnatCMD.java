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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.data.Pair;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.ui.ConsoleUI;
import nl.esciencecenter.ptk.ui.SimpelUI;
import nl.esciencecenter.ptk.util.ResourceLoader;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.ResponseInputStream;
import nl.esciencecenter.ptk.xml.XmlUtil;
import nl.esciencecenter.xnatclient.XnatClient;
import nl.esciencecenter.xnatclient.XnatClient.FilesCollection;
import nl.esciencecenter.xnatclient.data.XnatFile;
import nl.esciencecenter.xnatclient.data.XnatProject;
import nl.esciencecenter.xnatclient.data.XnatReconstruction;
import nl.esciencecenter.xnatclient.data.XnatScan;
import nl.esciencecenter.xnatclient.data.XnatSession;
import nl.esciencecenter.xnatclient.data.XnatSubject;
import nl.esciencecenter.xnattool.XnatTool;
import nl.esciencecenter.xnattool.XnatToolConfig;

public class XnatCMD
{
    private static final ClassLogger logger = ClassLogger.getLogger(XnatCMD.class);

    static String xnatUser = null;

    static Secret xnatPasswd = null;

    static String jsessionId = null;

    static String sourceFile = null;

    static String projectId = null;

    static String subjectLbl = null;

    static String sessionLbl = null;

    static String scanLbl = null;

    static boolean recursive = false;

    // global options
    static boolean verbose = false;

    static boolean printSids = false;

    static boolean useUI = false;

    // == destination arguments ==

    static String destFile = null;

    static String destDir = null;

    static String destXnatUser = null;

    static Secret destXnatPasswd = null;

    static String destJsessionId = null;

    static java.net.URI destXnatURI = null;

    static String destProjectId = null;

    // copy options
    static StringList subjectLabels;

    static StringList scanIds;

    public static void printUsage()
    {
        System.out.println("usage: [global options] <command> [command options] <xnatUri> [extra options]\n"
                + "    <comand>                           ; command, see below\n"
                + "    <xnatUri>                          ; URI of XNAT webservice for example: 'https://[user]@www.xnat.org[:80]/xnat'\n"
                + "                                       ; if argument is ambigous use '-uri <uri>' or '-duri <uri>' to specify URI\n"
                + "\n"
                + " Where [global options] can be:        \n"
                + "    -v     | -verbose                  ; be more verbose \n"
                + "    -ui    | -useUI                    ; enable ui, shows pop-up windows\n"
                + "    -nui   | -noUI                     ; disable ui (default)\n"
                + "    -debug | -info                     ; specify debug level\n"
                + "\n"
                + " Where [commandOptions] can be:        \n"
                + "    -u     | -user <xnat user>         ; XNAT user if not specified in URI (overrides user@<uri> part)\n"
                + "    -uri   | -sourceUri <XNAT URI>     ; explicit option for (source) URI\n"
                + "    -pw    | -password <xnat password> ; XNAT Password. If no password is given it will be prompted \n"
                + "    -prj   | -project <ProjectID>      ; XNAT Project ID \n"
                + "    -r     | -recursive                ; perform command recursively if applicable\n"
                + "    -jsid  | -jsessionID <JESSIONID>   ; (re)-use already authenticated jsession by specifying the JSESSION_ID\n"
                + "    -pjsid | -printSessionID           ; print out JSESSION_ID after authentication\n"
                + "\n"
                + " Commands:                             \n"
                + "    login                              ; authenticate and printout JSESSIONID, implies -pjsid\n"
                + "    listProject                        ; list Project, use -prj <ProjectID> \n"
                + "    listProjects                       ; list Projects IDs\n"
                + "    listSubjects [ -prj <project> ]    ; list subjects of all projects, or specified project\n"
                + "    listFiles    [ -prj <project> ]    ; list files of all projects, or specified project\n"
                + "\n"
                + " Data Transfer Commands:\n"
                + "    copyProject -prj <project> -dprj <dest project> [copy options]\n"
                + "                                       ; Copy all data from one project to other project\n"
                + "    copySubjects -prj <project> -subs <Subject1>,<Subject2>,... [copy options]\n"
                + "                                       ; Copy subjects from one project to other project\n"
                + "                                       ; Specify comma separated subject list using -subs <list>\n"
                + "\n"
                + " Download Commands:\n"
                + "    getScan -prj <project> -scan <scanId> [ -dest <destFile> ]\n"
                + "                                       ; Get ScanSet and download as zip. \n"
                // + "    getSubject -prj <project> -sub <subjectId> [ -dest <destFile> ]\n"
                // + "                                       ; Get Subject and download as zip. \n"
                // + "    getSubjects -prj <project> -subs <Sub1>,<Sub2>,... [ -dir destDirectory ] \n"
                // + "                                       ; Get Subjects and download as zip. \n"
                + "\n"
                + "\n"
                + " Where [copy options] can be:              \n"
                + "    -duri  | -destUri  <uri>               ; destination URI of (remote) XNAT Web Service.\n"
                + "    -du    | -destUser <user>              ; destination user account of (remote) XNAT Web Service.\n"
                + "    -dpw   | -destPassword <passwd>        ; desintation password or (remote) XNAT web Service.\n"
                + "    -dprj  | -destProject <project>        ; destination project \n"
                + "    -djsid | -destJSessionID <jsession id> ; destination JSESSIONID.\n"
                + "    -subs  | -subjectLabels  <subjects>    ; list of subject labels to copy.\n"
                //

                );

    }

    public static void main(String args[])
    {
        int status = doCommand(args);
        System.exit(status);
    }

    public static int doCommand(String args[])
    {
        int status = 1;

        int index = 0;

        // optional read from default Xnat Config !
        XnatToolConfig conf = new XnatToolConfig();
        XnatToolConfig destConf = null;// new XnatToolConfig();

        // Destination only for copy/transfer options.
        boolean hasDest = false;

        String commandStr = null;

        for (index = 0; index < args.length; index++)
        {
            String arg1 = args[index];
            String arg2 = null;

            if (index + 1 < args.length)
            {
                arg2 = args[index + 1];
            }

            // single arguments
            if (arg1.equals("-h") || arg1.equals("-help") || arg1.equals("--help"))
            {
                printUsage();
                return -2;
            }
            else if (StringUtil.equals(arg1, "-ui", "-useUI"))
            {
                useUI = true;
                continue;
            }
            else if (StringUtil.equals(arg1, "-nui", "-noUI"))
            {
                useUI = false;
                continue;
            }
            else if (arg1.equals("-r") || arg1.equals("-recursive"))
            {
                recursive = true;
                continue;
            }
            else if (arg1.equals("-r") || arg1.equals("-recursive"))
            {
                recursive = true;
                continue;
            }
            else if (arg1.equals("-debug"))
            {
                ClassLogger.getRootLogger().setLevelToDebug();
                continue;
            }
            else if (arg1.equals("-info"))
            {
                ClassLogger.getRootLogger().setLevelToInfo();
                continue;
            }
            else if (arg1.equals("-warn"))
            {
                ClassLogger.getRootLogger().setLevelToWarn();
                continue;
            }
            else if (StringUtil.equals(arg1, "-v", "-verbose"))
            {
                verbose = true;
                continue;
            }
            else if (StringUtil.equals(arg1, "-pjsid", "-printSessionID"))
            {
                printSids = true;
                continue;
            }

            // double argument options:

            if (arg1.startsWith("-") && (arg2 != null) && (arg2.startsWith("-") == false))
            {
                if (arg1.equals("-config"))
                {
                    String path = arg2;
                    try
                    {
                        conf = XnatToolConfig.loadConfig(path);
                        logger.debugPrintf("--- using config from:" + path
                                + " -----\n%s--------------------------------\n", XmlUtil.prettyFormat(conf.toXML(), 3));
                        index++;
                        continue;
                    }
                    catch (Exception e)
                    {
                        errPrintf("***Error: couldn't load config from:%s\n", path);
                        e.printStackTrace();
                        return -3;
                    }
                }

                if ((arg1.equals("-uri")) || (arg1.equals("-sourceUri")))
                {
                    try
                    {
                        conf.setXnatURI(new java.net.URI(arg2));
                        index++;
                        continue;
                    }
                    catch (URISyntaxException e)
                    {
                        errPrintf("Could not parse uri argument:%s\n", arg1);
                        e.printStackTrace();
                        return -4;
                    }
                }
                else if ((arg1.equals("-u")) || (arg1.equals("-user")))
                {
                    xnatUser = arg2;
                    index++;
                    continue;
                }
                else if (StringUtil.equals(arg1, "-pw", "-password"))
                {
                    xnatPasswd = new Secret(arg2.toCharArray());
                    index++;
                    continue;
                }
                else if (StringUtil.equals(arg1, "-prj", "-project"))
                {
                    projectId = arg2;
                    index++;
                    continue;
                }
                else if (arg1.equals("-jsid") || arg1.equals("-jsessionID"))
                {
                    jsessionId = arg2;
                    index++;
                    continue;
                }
                else if ((arg1.equals("-du")) || (arg1.equals("-destUser")))
                {
                    hasDest = true;
                    destXnatUser = arg2;
                    index++;
                    continue;
                }
                else if (StringUtil.equals(arg1, "-sub", "-subs", "-subjectLabels"))
                {
                    // subject arguments
                    subjectLabels = StringList.createFrom(arg2, ",");
                    index++;
                    continue;
                }
                else if (StringUtil.equals(arg1, "-scan", "-scans"))
                {
                    // scan arguments
                    scanIds = StringList.createFrom(arg2, ",");
                    index++;
                    continue;
                }
                else if (StringUtil.equals(arg1, "-dpw", "-destPW", "-destPassword"))
                {
                    hasDest = true;
                    destXnatPasswd = new Secret(arg2.toCharArray());
                    index++;
                    continue;
                }
                else if (arg1.equals("-djsid") || arg1.equals("-destSessionID"))
                {
                    hasDest = true;
                    destJsessionId = arg2;
                    index++;
                    continue;
                }
                else if (arg1.equals("-dprj") || arg1.equals("-destProject"))
                {
                    hasDest = true;
                    destProjectId = arg2;
                    index++;
                    continue;
                }
                else if (arg1.equals("-dest") || arg1.equals("-destFile"))
                {
                    destFile = arg2;
                    index++;
                    continue;
                }
                else if (arg1.equals("-dir") || arg1.equals("-destDir"))
                {
                    destDir = arg2;
                    index++;
                    continue;
                }
                // allow "destURI" "destUri" (and "desturi")
                else if (arg1.equals("-duri") || arg1.toLowerCase().equals("-destUri"))
                {
                    hasDest = true;
                    try
                    {
                        destXnatURI = new java.net.URI(arg2);
                    }
                    catch (URISyntaxException e)
                    {
                        errPrintf("Could not parse uri argument:%s\n", arg1);
                        e.printStackTrace();
                        return -5;
                    }

                    index++;
                    continue;
                }
                else
                {
                    errPrintf("Unknown option:%s\n", arg1);
                    printUsage();
                    exit(-6);
                }

            }
            else
            {
                // either <command> or <uri>
                // first non option argument is command, second must be uri:
                if (commandStr == null)
                {
                    commandStr = arg1;
                }
                else
                {
                    if (conf.getXnatURI() == null)
                    {
                        try
                        {
                            conf.setXnatURI(new java.net.URI(arg1));
                        }
                        catch (URISyntaxException e)
                        {
                            errPrintf("Could parse uri argument:%s\n", arg1);
                            e.printStackTrace();
                            return -7;
                        }
                    }
                    else
                    {

                        errPrintf("Invalid argument:%s\n", arg1);
                        printUsage();
                        return -8;
                    }
                }

            }
        }

        // --------------------
        // Post parse arguments
        // --------------------

        try
        {

            // pre config post options:
            if (StringUtil.isEmpty(xnatUser) == false)
            {
                infoPrintf("Using user from argument:%s\n", xnatUser);
                // -du|-destUser option overrides URI
                conf.updateUser(xnatUser);
            }
            else
            {
                infoPrintf("Using user from URI:%s\n", conf.getXnatUser());
            }

            infoPrintf("Using uri: %s\n", conf.getXnatURI());

            if (xnatPasswd != null)
            {
                infoPrintf("Using destination password from argument.\n");
                // check has password !
                ;
            }

            if (hasDest)
            {
                destConf = new XnatToolConfig();

                if (destXnatURI != null)
                {
                    destConf.updateURI(destXnatURI);
                    infoPrintf("Using destination URI:%s\n", destXnatURI);
                }
                else
                {
                    errPrintf("Remote destination argument given, but no destination (-destURI <uri> ) URI configured!");
                    return -9;
                }

                // pre config post options:
                if (StringUtil.isEmpty(destXnatUser) == false)
                {
                    infoPrintf("Using destination user from argument:%s\n", destXnatUser);
                    // -du|-destUser option overrides URI
                    destConf.updateUser(destXnatUser);
                }
                else
                {
                    infoPrintf("Using destination user from URI:%s\n", destConf.getXnatUser());
                }

                if (destXnatPasswd != null)
                {
                    infoPrintf("Using destination password from argument.\n");
                    // check has password !
                    ;
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return -10;
        }

        // ----------
        // Do Command
        // ----------

        if (commandStr == null)
        {
            errPrintf("No command given, use '-h' for help. Aborting\n");
            return -11;
        }

        if (conf.getXnatURI() == null)
        {
            errPrintf("No XNAT Uri given, use '-h' for help. Aborting\n");
            return -12;
        }

        try
        {
            status = doCommand(conf, destConf, commandStr);
        }
        catch (Exception e)
        {
            errPrintf("Exception performing command:%s\n", commandStr);
            e.printStackTrace();
            return -13;
        }

        return status;
    }

    private static void exit(int status)
    {
        System.exit(status);
    }

    static protected int doCommand(XnatToolConfig conf, XnatToolConfig optDestConf, String commandStr) throws Exception
    {
        if (commandStr.equals("login"))
        {
            printSids = true;
            XnatClient client, destClient;

            client = authenticate(conf, xnatPasswd, jsessionId);

            if (optDestConf != null)
            {
                destClient = authenticate(optDestConf, destXnatPasswd, destJsessionId);
            }
            return 0;
        }
        else if (StringUtil.equals(commandStr, "lp", "listProject", "listProjects"))
        {
            return listProjects(conf, false, recursive, false);
        }
        else if (StringUtil.equals(commandStr, "lsub", "listSubject", "listSubjects"))
        {
            return listProjects(conf, true, recursive, false);
        }
        else if (StringUtil.equals(commandStr, "lf", "listFiles"))
        {
            // listFiles implies recursive.
            return listProjects(conf, true, true, true);
        }
        else if ((commandStr.equals("getScan")) || (commandStr.equals("getScans")))
        {
            return getScansAsZip(conf);
        }
        else if ((commandStr.equals("copyProject")) || (commandStr.equals("copySubjects")))
        {
            if (optDestConf == null)
            {
                errPrintf("Need XNAT destination configuration, specify destination server using: -duri or -destUri.\n");
                return -16;
            }

            if (projectId == null)
            {
                errPrintf("Source Project not defined, use option: -prj <projectId> or -sourceProject <projectId>\n");
                return -17;
            }

            if (destProjectId == null)
            {
                errPrintf("Destination project not defined, use option: -dprj <projectId> or -destProject <projectId>\n");
                return -18;
            }

            Set<String> subjectsSet = null;
            if (commandStr.equals("copySubjects"))
            {
                if (subjectLabels == null)
                {
                    errPrintf("copySubjects command given, but no subject list provided. Use -subs <subject list>\n");
                    return -19;

                }
                subjectsSet = subjectLabels.toSet();
            }

            return copyProject(conf, optDestConf, projectId, destProjectId, subjectsSet);
        }
        else if (commandStr.equals("statProject"))
        {
            if (projectId == null)
            {
                errPrintf("Source Project not defined, use option: -prj <projectId> or -sourceProject <projectId>\n");
                return -17;
            }

            return statProject(conf, projectId);
        }
        else
        {
            errPrintf("Unkown command:%s. Use -h for help.\n", commandStr);
            return -20;
        }
    }

    static XnatClient authenticate(XnatToolConfig conf, Secret pwd, String jsid) throws Exception
    {
        XnatTool xnatTool;
        xnatTool = new XnatTool(conf, false);

        String xnatUser = conf.getXnatUser();
        xnatTool.setXnatCredentials(xnatUser, pwd);

        if (jsid != null)
        {
            xnatTool.setJSessionID(jsid);
        }

        XnatClient client = xnatTool.getXnatClient();

        if (useUI)
        {
            client.setUI(new SimpelUI());
        }
        else
        {
            client.setUI(new ConsoleUI());
        }

        xnatTool.authenticateXnat();

        if (printSids)
        {
            outPrintf("URI=%s\n", client.getServiceURI());
            outPrintf("JSESSIONID=%s\n", client.getJSessionID());
        }

        return client;
    }

    static int listProjects(XnatToolConfig conf, boolean subjects, boolean recurse, boolean listFiles) throws Exception
    {
        // init XnatTool but do not auto save configuration!
        XnatClient client = authenticate(conf, xnatPasswd, jsessionId);

        List<String> projects;

        if (projectId != null)
        {
            // list specified project:
            projects = new ArrayList<String>();
            projects.add(projectId);
        }
        else
        {
            projects = client.listProjectIDs();
        }

        if ((recurse == false) && (subjects == false))
        {
            for (String projId : projects)
            {
                outPrintf("%s\n", projId);
            }
        }
        else
        {
            for (String projId : projects)
            {
                outPrintf("%s/\n", projId);

                if ((subjects == false) && (recurse == false))
                {
                    continue;
                }

                // continue with subjects:
                List<XnatSubject> subs = client.listSubjects(projId);

                for (XnatSubject sub : subs)
                {
                    String subLabel = sub.getLabel();
                    // check subjects:
                    if ((subjectLabels != null) && (subjectLabels.size() > 0))
                    {
                        if (subjectLabels.contains(subLabel) == false)
                        {
                            continue; // skip
                        }
                    }

                    outPrintf("%s/subject/%s/\n", projId, subLabel);
                    List<XnatSession> sess = client.listSessions(sub);

                    if (recurse == true)
                    {
                        // continue with rest:
                        for (XnatSession ses : sess)
                        {
                            String sesLabel = ses.getLabel();
                            outPrintf("%s/subject/%s/session/%s\n", projId, subLabel, sesLabel);
                            List<XnatScan> scans = client.listScans(ses);

                            for (XnatScan scan : scans)
                            {
                                String scanId = scan.getID();
                                outPrintf("%s/subject/%s/session/%s/scans/%s\n", projId, subLabel, sesLabel, scanId);

                                if (listFiles)
                                {
                                    FilesCollection collection = client.listScanFiles(ses, scanId);
                                    for (String label : collection.keySet())
                                    {
                                        outPrintf("%s/subject/%s/session/%s/scans/%s/files/%s\n", projId, subLabel, sesLabel, scanId, label);

                                        for (XnatFile file : collection.getFileList(label))
                                        {
                                            printFile("   ", file);
                                        }
                                    }
                                }

                            }

                            List<XnatReconstruction> recons = client.listReconstructions(ses);

                            for (XnatReconstruction recon : recons)
                            {
                                String reconId = recon.getID();
                                recon.getID();
                                outPrintf("%s/subject/%s/session/%s/reconstructions/%s\n", projId, subLabel, sesLabel, reconId);

                                if (listFiles)
                                {
                                    FilesCollection collection = client.listReconstructionFiles(ses, reconId);

                                    for (String label : collection.keySet())
                                    {
                                        outPrintf("%s/subject/%s/session/%s/reconstructions/%s/files/%s\n", projId, subLabel, sesLabel,
                                                reconId, label);

                                        for (XnatFile file : collection.getFileList(label))
                                        {
                                            printFile("   ", file);
                                        }
                                    }
                                }

                            }

                        }
                    }// if (recurse)
                }
            }
        }

        return 0;
    }

    public static void printFile(String prefix, XnatFile file)
    {
        String sizeStr = Presentation.createSizeString(file.getFileSize(), true, 2, 2);
        String typeStr = StringUtil.paddString(file.getContentTypeString(), 8, ' ', false);
        String attrStr = prefix + StringUtil.paddString(sizeStr, 8, ' ', true) + " " + typeStr;

        outPrintf("%s %s\n", attrStr, file.getBasename());
    }

    public static int copyProject(XnatToolConfig conf, XnatToolConfig optDestConf, String sourcePrj, String destPrj, Set<String> subjects)
    {

        try
        {
            // delegate to XnatCopyCMD class:
            XnatCopyCMD xnatCopy = new XnatCopyCMD(conf, optDestConf);
            xnatCopy.setVerbose(verbose);

            xnatCopy.setProjectIDs(sourcePrj, destPrj);
            xnatCopy.setSourceCredentials(conf.getXnatUser(), xnatPasswd);
            xnatCopy.setDestCredentials(optDestConf.getXnatUser(), destXnatPasswd);
            xnatCopy.setSubjectLabels(subjects);

            if (useUI)
            {
                xnatCopy.setUI(new SimpelUI());
            }
            else
            {
                xnatCopy.setUI(new ConsoleUI());
            }

            if (jsessionId != null)
            {
                infoPrintf("Using source JSESSIONID:%s\n", jsessionId);
                xnatCopy.getXnatCopy().getSourceXnatClient().setJSessionID(jsessionId);
            }

            if (destJsessionId != null)
            {
                infoPrintf("Using destination JSESSIONID:%s\n", destJsessionId);
                xnatCopy.getXnatCopy().getDestXnatClient().setJSessionID(destJsessionId);
            }

            // start/
            xnatCopy.authenticate();

            if (printSids)
            {
                outPrintf("SOURCE JSESSIONID=%s\n", xnatCopy.getXnatCopy().getSourceXnatClient().getJSessionID());
                outPrintf("DEST JSESSIONID=%s\n", xnatCopy.getXnatCopy().getDestXnatClient().getJSessionID());
            }
            else
            {
                infoPrintf("SOURCE JSESSIONID=%s\n", xnatCopy.getXnatCopy().getSourceXnatClient().getJSessionID());
                infoPrintf("DEST JSESSIONID=%s\n", xnatCopy.getXnatCopy().getDestXnatClient().getJSessionID());
            }

            xnatCopy.doCopy();
            // done
            return 0;
        }
        catch (Exception e)
        {
            errPrintf("Failed to copy Project:'%s' to project:'%s'.\nReason=%s\n", sourcePrj, destPrj, e.getMessage());
            e.printStackTrace();
            return -22;
        }
    }

    public static int getScansAsZip(XnatToolConfig conf) throws Exception
    {
        // init XnatTool but do not auto save configuration!
        XnatClient client = authenticate(conf, xnatPasswd, jsessionId);
        if (scanIds == null)
        {
            errPrintf("No scan IDs given. Use -scan <ID> or -scans <ID1>,<ID2>, ...\n");
            return -23;

        }

        for (String scan : scanIds)
        {
            String zipTargetFile = destFile;

            if (zipTargetFile == null)
            {
                zipTargetFile = scan + ".zip";
            }

            if (zipTargetFile.toLowerCase().endsWith(".zip") == false)
            {
                zipTargetFile = zipTargetFile + ".zip";
            }

            getScanAsZipFile(client, projectId, scan, zipTargetFile);
        }

        return 0;
    }

    public static int getScanAsZipFile(XnatClient client, String projectId, String scanLbl, String targetFile) throws Exception
    {
        XnatProject xnatProj = client.getProject(projectId);

        List<Pair<XnatSession, XnatScan>> scanPairs = client.findScanByLabel(projectId, scanLbl, true);

        if (scanPairs.size() > 0)
        {
            for (Pair<XnatSession, XnatScan> pair : scanPairs)
            {
                XnatSession ses = scanPairs.get(0).one();
                XnatScan scan = scanPairs.get(0).two();

                infoPrintf(" - found session/scan (%s/%s):%s\n", ses.getID(), scan.getID(), scan);

                // get scan
                getScanAsZipFile(client, ses, scan, targetFile);
            }
            return 0;
        }
        else
        {
            errPrintf("no such scan:%s\n", scanLbl);
            return 404;
        }

    }

    public static int getScanAsZipFile(XnatClient client, XnatSession sess, XnatScan scan, String targetFile) throws Exception
    {
        ResponseInputStream zipStream = client.getScanSetZipInputStream(sess, scan.getID(), null);

        FSUtil fsUtil = new FSUtil();

        // resolve destination directory and use that as CWD:
        if (destDir != null)
        {
            URI destDirURI = fsUtil.resolvePathURI(destDir);
            if (fsUtil.existsDir(destDirURI.getPath()) == false)
            {
                errPrintf("No such directory:%s (=>%s)\n", destDir, destDirURI);
                return 404;
            }

            fsUtil.setWorkingDir(destDirURI);
        }

        URI targetPath = fsUtil.resolvePathURI(targetFile);

        ResourceLoader loader = new ResourceLoader(fsUtil, null);

        byte[] bytes = loader.readBytes(zipStream);

        zipStream.close();

        infoPrintf(" - number of ZIP bytes read=%d\n", bytes.length);

        // write bytes:
        loader.writeBytesTo(targetPath, bytes);
        FSPath file = fsUtil.newFSPath(targetFile);

        outPrintf("%s %d\n", file.getPath(), file.getFileSize());

        return 0;
    }

    static int statProject(XnatToolConfig conf, String id) throws Exception
    {
        // init XnatTool but do not auto save configuration!
        XnatClient client = authenticate(conf, xnatPasswd, jsessionId);

        XnatProject xnatProj = client.getProject(projectId);

        outPrintf("--- ProjectId=%s ---\n", projectId);
        outPrintf("%s\n", xnatProj.toString("", " - ", "\n", "\n"));

        return 0;
    }

    public static void errPrintf(String format, Object... args)
    {
        System.err.printf(format, args);
    }

    public static void outPrintf(String format, Object... args)
    {
        System.out.printf(format, args);
    }

    public static void infoPrintf(String format, Object... args)
    {
        if (verbose)
        {
            System.out.printf(format, args);
        }
    }
}

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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import nl.esciencecenter.ptk.crypt.Secret;
import nl.esciencecenter.ptk.csv.CSVData;
import nl.esciencecenter.ptk.data.HashMapList;
import nl.esciencecenter.ptk.data.Pair;
import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.io.FSUtil;
import nl.esciencecenter.ptk.io.exceptions.FileURISyntaxException;
import nl.esciencecenter.ptk.io.FSPath;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.ssl.CertificateStore;
import nl.esciencecenter.ptk.ssl.CertificateStoreException;
import nl.esciencecenter.ptk.ui.UI;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.web.PutMonitor;
import nl.esciencecenter.ptk.web.ResponseInputStream;
import nl.esciencecenter.ptk.web.URIQueryParameters;
import nl.esciencecenter.ptk.web.WebClient;
import nl.esciencecenter.ptk.web.WebConfig;
import nl.esciencecenter.ptk.web.WebConfig.AuthenticationType;
import nl.esciencecenter.ptk.web.WebConst;
import nl.esciencecenter.ptk.web.WebException;
import nl.esciencecenter.ptk.web.WebException.Reason;
import nl.esciencecenter.xnatclient.data.ImageFileInfo;
import nl.esciencecenter.xnatclient.data.NewScanInfo;
import nl.esciencecenter.xnatclient.data.XnatConst;
import nl.esciencecenter.xnatclient.data.XnatFile;
import nl.esciencecenter.xnatclient.data.XnatObject;
import nl.esciencecenter.xnatclient.data.XnatObject.XnatObjectType;
import nl.esciencecenter.xnatclient.data.XnatParser;
import nl.esciencecenter.xnatclient.data.XnatProject;
import nl.esciencecenter.xnatclient.data.XnatReconstruction;
import nl.esciencecenter.xnatclient.data.XnatScan;
import nl.esciencecenter.xnatclient.data.XnatSession;
import nl.esciencecenter.xnatclient.data.XnatSubject;
import nl.esciencecenter.xnatclient.data.XnatTypes.ImageFormatType;
import nl.esciencecenter.xnatclient.data.XnatTypes.XnatResourceType;
import nl.esciencecenter.xnatclient.exceptions.XnatClientException;
import nl.esciencecenter.xnatclient.exceptions.XnatParseException;
import nl.esciencecenter.xnattool.UploadMonitor;

/**
 * XNAT Rest interface to an Xnat Web Service. Supports basic functions to browse and create Projects, Sessions and
 * ScanSets.
 * 
 * @author Piter T. de Boer
 */
public class XnatClient
{
    private static ClassLogger logger = ClassLogger.getLogger(XnatClient.class);

    static
    {
        // logger.setLevelToDebug();
    }

    public static class FilesCollection
    {
        protected Map<String, List<XnatFile>> fileCollection;

        public FilesCollection(Map<String, List<XnatFile>> files)
        {
            this.fileCollection = files;
        }

        public Map<String, List<XnatFile>> getFiles()
        {
            return fileCollection;
        }

        public List<XnatFile> getFileList(String resourceLabel)
        {
            return fileCollection.get(resourceLabel);
        }

        public Set<String> keySet()
        {
            return fileCollection.keySet();
        }

        public XnatFile getFile(String collectionLabel, String filename)
        {
            List<XnatFile> list = this.getFileList(collectionLabel);

            if ((list == null) || (list.size() <= 0))
            {
                return null;
            }

            for (XnatFile file : list)
            {
                if (file.getFileName().equals(filename))
                {
                    return file;
                }
            }

            return null;
        }
    }

    // ========================================================================
    //
    // ========================================================================

    /**
     * Rest/Web interface Client.
     */
    protected WebClient webClient;

    public XnatClient(java.net.URI serviceUri, String username, Secret password) throws WebException
    {
        logger.infoPrintf("New XnatClient for:%s\n", serviceUri);
        WebConfig config = new WebConfig(serviceUri, AuthenticationType.BASIC, false);
        config.setCredentials(username, password);
        init(config);
    }

    public XnatClient(WebConfig config) throws WebException
    {
        logger.infoPrintf("New XnatClient for:%s\n", config.getServiceURIString());
        init(config);
    }

    protected void init(WebConfig config) throws WebException
    {
        webClient = new WebClient(config);
    }

    public java.net.URI getServiceURI()
    {
        return webClient.getServiceURI();
    }

    public String getUsername()
    {
        return webClient.getUsername();
    }

    public void setCredentials(String user, Secret passwd)
    {
        webClient.setCredentials(user, passwd);
    }

    public void connect() throws WebException
    {
        webClient.connect();
    }

    public void disconnect() throws WebException
    {
        webClient.disconnect();
    }

    public boolean isAuthenticated()
    {
        return webClient.isAuthenticated();
    }

    public void setJSessionID(String jsession)
    {
        webClient.setJSessionID(jsession);
    }

    public void setUI(UI ui)
    {
        webClient.setUI(ui);
    }

    // ========================================================================
    // Put/Get methods
    // ========================================================================

    public String doJSonQuery(String queryStr) throws WebException
    {
        logger.debugPrintf("doJSonQuery: '%s'\n", queryStr);

        StringHolder resultH = new StringHolder();
        StringHolder contentTypeH = new StringHolder();
        int httpStatus = webClient.doGet(queryStr, resultH, contentTypeH);

        if (contentTypeH.value != null)
        {
            if (contentTypeH.value.startsWith("text/html"))
            {
                // some error in HTML
                throw new WebException(Reason.INVALID_RESPONSE,
                        httpStatus,
                        "Invalid (HTML) response from query (expected JSON):"
                                + queryStr + "\n"
                                + "--- response ---\n" + resultH.value,
                        contentTypeH.value,
                        resultH.value);
            }

            // Must be JSON!
            if (contentTypeH.value.startsWith("application/json") == false)
            {
                // some error in HTML
                throw new WebException(Reason.INVALID_RESPONSE,
                        httpStatus,
                        "Invalid contenType response:'" + contentTypeH.value
                                + "' from query (expected JSON):" + queryStr + "\n"
                                + "--- response ---\n" + resultH.value,
                        contentTypeH.value,
                        resultH.value);
            }
        }

        logger.debugPrintf("doJSonQuery(): Return stat = %s\n", WebConst.getHTTPStatusString(httpStatus));
        logger.debugPrintf("doJSonQuery(): Encoding = %s\n", contentTypeH.value);
        logger.debugPrintf("doJSonQuery(): Result:\n>>>-------------\n%s\n>>>-------------\n", resultH.value);

        return resultH.value;
    }

    public String doPutQueryString(String queryStr) throws WebException
    {
        logger.debugPrintf("doPutQueryString: '%s'\n", queryStr);

        StringHolder resultH = new StringHolder();
        StringHolder contentTypeH = new StringHolder();
        // doPut handles Http Errors
        int httpStatus = webClient.doPut(queryStr, resultH, contentTypeH);

        logger.debugPrintf("doJSonQuery(): httpStatus= %d\n", httpStatus);
        logger.debugPrintf("doJSonQuery(): Encoding  = %s\n", contentTypeH.value);
        logger.debugPrintf("doJSonQuery(): Result:\n>>>-------------\n%s\n>>>-------------\n", resultH.value);

        return resultH.value;
    }

    public String doDeleteQueryString(String queryStr) throws WebException
    {
        logger.debugPrintf("doDeleteQueryString: '%s'\n", queryStr);

        StringHolder resultH = new StringHolder();
        StringHolder contentTypeH = new StringHolder();
        // doPut handles Http Errors
        int httpStatus = webClient.doDelete(queryStr, resultH, contentTypeH);

        logger.debugPrintf("doJSonQuery(): httpStatus= %d\n", httpStatus);
        logger.debugPrintf("doJSonQuery(): Encoding  = %s\n", contentTypeH.value);
        logger.debugPrintf("doJSonQuery(): Result:\n>>>-------------\n%s\n>>>-------------\n", resultH.value);

        return resultH.value;
    }

    public List<XnatProject> listProjects() throws WebException, XnatClientException
    {
        String resultStr = doJSonQuery("data/projects?format=json");
        List<XnatProject> projects = new ArrayList<XnatProject>();
        XnatParser.parseJsonResult(XnatObjectType.XNAT_PROJECT, resultStr, projects);

        return projects;
    }

    public List<String> listProjectIDs() throws WebException, XnatClientException
    {
        StringList list = new StringList();
        List<XnatProject> projs = listProjects();

        for (XnatProject proj : projs)
        {
            list.add(proj.getID());
        }
        return list;
    }

    public XnatProject getProject(String projectid) throws WebException, XnatClientException
    {
        if (projectid == null)
            return null;

        try
        {
            String resultStr = doJSonQuery("REST/projects/" + projectid + "?format=json");

            List<XnatProject> projects = new ArrayList<XnatProject>();
            XnatParser.parseJsonQueryResult(XnatObjectType.XNAT_PROJECT, resultStr, (List<? extends XnatObject>) projects);

            if (projects.size() < 1)
                return null;

            return projects.get(0);
        }
        catch (WebException e)
        {
            if (e.getReason() == Reason.RESOURCE_NOT_FOUND)
            {
                throw new XnatClientException("Project '" + projectid + "' not found at:" + this.getServiceURI(), e);
            }
            throw e;
        }
    }

    public List<XnatSubject> listSubjects(String projectid) throws WebException, XnatClientException
    {
        if (projectid == null)
            return null;

        // String servicePath=webClient.getServicePath();
        String resultstr = doJSonQuery("data/projects/" + projectid + "/subjects?format=json");

        List<XnatSubject> subjects = new ArrayList<XnatSubject>();
        XnatParser.parseJsonResult(XnatObjectType.XNAT_SUBJECT, resultstr, subjects);
        return subjects;
    }

    public List<String> listSubjectLabels(String projectId) throws WebException, XnatClientException
    {
        StringList list = new StringList();
        List<XnatSubject> subs = listSubjects(projectId);

        for (XnatSubject sub : subs)
        {
            list.add(sub.getLabel());
        }
        return list;
    }

    public List<XnatSession> listSessions(XnatSubject xnatSubject) throws WebException, XnatClientException
    {
        if (xnatSubject == null)
        {
            return null;
        }

        return listSessions(xnatSubject.getProjectID(), xnatSubject.getLabel());
    }

    public List<XnatSession> listSessions(String projectId, String subjectLabel) throws WebException, XnatClientException
    {
        String resultstr = doJSonQuery("data/projects/" + projectId + "/subjects/" + subjectLabel
                + "/experiments/?format=json");
        List<XnatSession> sessions = new ArrayList<XnatSession>();
        XnatParser.parseJsonResult(XnatObjectType.XNAT_SESSION, resultstr, sessions);

        for (XnatSession sess : sessions)
        {
            sess.setSubjectLabel(subjectLabel); // is not part of returned meta
                                                // data.
        }
        return sessions;
    }

    public List<String> listSessionLabels(String projectId, String subjectId) throws WebException, XnatClientException
    {
        StringList list = new StringList();
        List<XnatSession> sess = listSessions(projectId, subjectId);

        for (XnatSession ses : sess)
        {
            list.add(ses.getLabel());
        }
        return list;
    }

    public List<XnatScan> listScans(XnatSession xnatSession) throws WebException, XnatClientException
    {
        // String servicePath=webClient.getServicePath();
        String projectid = xnatSession.getProjectID();
        String subjectLabel = xnatSession.getSubjectLabel();
        String sessionLabel = xnatSession.getLabel();

        String resultstr = doJSonQuery("data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/"
                + sessionLabel + "/scans?format=json");
        List<XnatScan> scans = new ArrayList<XnatScan>();

        logger.debugPrintf("listScans(), resulstr=%s\n", resultstr);

        XnatParser.parseJsonResult(XnatObjectType.XNAT_SCAN, resultstr, (List) scans);

        for (XnatScan scan : scans)
        {
            scan.setIDMapping(projectid, subjectLabel, sessionLabel);
        }

        return scans;
    }

    public List<XnatReconstruction> listReconstructions(XnatSession xnatSession) throws WebException, XnatClientException
    {
        // String servicePath=webClient.getServicePath();
        String projectid = xnatSession.getProjectID();
        String subjectLabel = xnatSession.getSubjectLabel();
        String sessionLabel = xnatSession.getLabel();

        String resultstr = doJSonQuery("data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/"
                + sessionLabel + "/reconstructions?format=json");

        List<XnatReconstruction> recons = new ArrayList<XnatReconstruction>();

        logger.debugPrintf("listScans(), resulstr=%s\n", resultstr);

        XnatParser.parseJsonResult(XnatObjectType.XNAT_RECONSTRUCTION, resultstr, (List) recons);

        for (XnatReconstruction scan : recons)
        {
            scan.setIDMapping(projectid, subjectLabel, sessionLabel);
        }

        return recons;
    }

    // public Map<String, List<XnatFile>> listScanFiles(XnatScan scan) throws
    // WebException, XnatParseException
    // {
    // String scanUri = scan.getURIPath();
    // logger.debugPrintf("listScanFiles:%s\n", scanUri);
    //
    // // use scanset URI from actual XnatScan Object:
    // String resultStr = doJSonQuery(scanUri + "/files?format=json");
    //
    // return parseScanFilesResults(resultStr);
    // }

    public FilesCollection listScanFiles(XnatSession xnatSession, String scanLabel)
            throws WebException, XnatParseException
    {
        // String servicePath=webClient.getServicePath();
        String projectId = xnatSession.getProjectID();
        String subjectLabel = xnatSession.getSubjectLabel();
        String sessionLabel = xnatSession.getLabel();

        return listResourceFiles(projectId, subjectLabel, sessionLabel, "scans", scanLabel);
    }

    public FilesCollection listReconstructionFiles(XnatSession xnatSession, String reconId)
            throws WebException, XnatParseException
    {
        // String servicePath=webClient.getServicePath();
        String projectId = xnatSession.getProjectID();
        String subjectLabel = xnatSession.getSubjectLabel();
        String sessionLabel = xnatSession.getLabel();

        return listResourceFiles(projectId, subjectLabel, sessionLabel, "reconstructions", reconId);
    }

    protected FilesCollection listResourceFiles(String projectId, String subjectLabel, String sessionLabel,
            String resourceType, String resourceId) throws WebException, XnatParseException
    {
        String resultstr = doJSonQuery("data/projects/" + projectId + "/subjects/" + subjectLabel + "/experiments/"
                + sessionLabel + "/" + resourceType + "/" + resourceId + "/files?format=json");

        Map<String, List<XnatFile>> result = parseScanFilesResults(resultstr);
        return new FilesCollection(result);
    }

    protected Map<String, List<XnatFile>> parseScanFilesResults(String jsonStr) throws XnatParseException
    {
        logger.debugPrintf("parseScanFilesResults()=%s\n", jsonStr);

        List<XnatFile> files = new ArrayList<XnatFile>();
        XnatParser.parseJsonResult(XnatObjectType.XNAT_FILE, jsonStr, (List) files);

        // sort all files into collections:
        Map<String, List<XnatFile>> collections = new HashMapList<String, List<XnatFile>>();

        for (XnatFile file : files)
        {
            String collection = file.getCollection();
            if (collection == null)
            {
                collection = "";
            }
            List<XnatFile> subList = collections.get(collection);
            if (subList == null)
            {
                subList = new ArrayList<XnatFile>();
            }
            subList.add(file);
            collections.put(collection, subList); // create/update
        }

        return collections;
    }

    /**
     * Find subject using the unique Subject ID. No ProjectID is needed as the Subject ID is unique.
     * 
     * @param subjectId
     *            - The SubjectID
     * @return
     * @throws WebException
     * @throws XnatParseException
     */
    public XnatSubject getSubjectById(String subjectId) throws WebException, XnatParseException
    {
        logger.debugPrintf("getSubjectById:%s\n", subjectId);
        String resultStr = doJSonQuery("REST/subjects/" + subjectId + "?format=json");
        List<XnatSubject> subjects = new ArrayList<XnatSubject>();

        XnatParser.parseJsonQueryResult(XnatObjectType.XNAT_SUBJECT, resultStr, (List) subjects);

        if (subjects.size() < 1)
        {
            return null;
        }

        return subjects.get(0);
    }

    /**
     * Get XnatSubject using the subject label. This method needs the Project ID as well as Subject Labels are not
     * unique within XNAT.
     * 
     * @param projectId
     * @param subjectLabel
     * @return
     * @throws WebException
     * @throws XnatParseException
     */
    public XnatSubject getSubjectByLabel(String projectId, String subjectLabel) throws WebException, XnatParseException
    {
        logger.debugPrintf("getSubject:%s\n", subjectLabel);
        String resultStr = doJSonQuery("data/archive/projects/" + projectId + "/subjects/" + subjectLabel + "?format=json");
        List<XnatSubject> subjects = new ArrayList<XnatSubject>();

        XnatParser.parseJsonQueryResult(XnatObjectType.XNAT_SUBJECT, resultStr, (List) subjects);

        if (subjects.size() < 1)
        {
            return null;
        }

        return subjects.get(0);
    }

    public XnatSession getSessionOfSubject(XnatSubject subject, String sessionLabel) throws WebException, XnatClientException
    {
        return getSession(subject.getProjectID(), subject.getLabel(), sessionLabel);
    }

    /**
     * Use either SessionID or SessionLabel. The SessionLabel is part of the (rest) URI.
     * 
     * @throws XnatClientException
     */
    public XnatSession getSession(String projectId, String subjectLabel, String sessionLabel) throws WebException,
            XnatClientException
    {
        List<XnatSession> sessions = this.listSessions(projectId, subjectLabel);

        if ((sessions == null) || (sessions.size() <= 0))
        {
            return null;
        }

        for (int i = 0; i < sessions.size(); i++)
        {
            if ((sessionLabel != null) && (sessions.get(i).getLabel().equals(sessionLabel)))
            {
                return sessions.get(i);
            }
        }

        return null;
    }

    /**
     * Find Session or Experiment using the Session ID. No ProjectID is needed as the Session Id is unique.
     * 
     * @param subjectId
     *            - The SubjectID
     * @return
     * @throws WebException
     * @throws XnatParseException
     */
    public XnatSession getSessionById(String sessionId) throws WebException, XnatParseException
    {
        logger.debugPrintf("getSessionById:%s\n", sessionId);
        String resultStr = doJSonQuery("REST/experiments/" + sessionId + "?format=json");
        List<XnatSession> sessions = new ArrayList<XnatSession>();

        XnatParser.parseJsonQueryResult(XnatObjectType.XNAT_SESSION, resultStr, (List) sessions);

        if (sessions.size() < 1)
        {
            return null;
        }

        return sessions.get(0);
    }

    /**
     * Scan has no Label, use ScanID to access ScanSet.
     * 
     * @throws XnatClientException
     */
    public XnatScan getScanByLabel(XnatSession session, String scanLabel) throws WebException,
            XnatClientException
    {
        List<XnatScan> scans = this.listScans(session);

        for (int i = 0; i < scans.size(); i++)
        {
            if ((scanLabel != null) && (scans.get(i).getID().equals(scanLabel)))
            {
                return scans.get(i);
            }
        }
        return null;
    }

    // ========================================================================
    // Create Methods for Project/Subject/Session/Scan
    // ========================================================================

    /**
     * Create new (empty) Project. Doesn't return anything if project Creation is successful!
     */
    public void createProject(String projectId, String projectName) throws WebException
    {
        if (projectName == null)
        {
            projectName = projectId;
        }
        //
        String putstr = "data/projects/" + projectId + "?name=" + projectName;
        String resultstr = doPutQueryString(putstr);

        // empty -> null;
        if (StringUtil.isEmpty(resultstr))
        {
            logger.warnPrintf("Create Project (put) didn't return any info. Assuming project was created ok\n");
            return; // null;
        }

        return; // null;
    }

    /**
     * Create new (empty) Project. Doesn't return anything if project Creation is successful!
     */
    public String deleteProject(String projectId) throws WebException
    {
        if (projectId == null)
        {
            throw new NullPointerException("ProjectID can not be NULL!");
        }
        //
        String delStr = "data/projects/" + projectId;
        String resultstr = doDeleteQueryString(delStr);

        // empty -> null;
        if (StringUtil.isEmpty(resultstr))
        {
            logger.warnPrintf("Create Project (put) didn't return any info. Assuming project was created ok\n");
        }

        return resultstr;
    }

    public XnatSubject createSubject(String projectId, String subjectLabel) throws WebException, XnatClientException
    {
        // Object
        XnatSubject subject = XnatSubject.createXnatSubject(projectId, subjectLabel);
        // actual create:
        return this.putSubject(projectId, subject, true);
    }

    /**
     * Create new Subject. Returns Subject ID. If the SubjectLabel already exists, the id of the existing subject will
     * be returned.
     * 
     * @return the Subject ID as register at the Xnat Database.
     * @throws WebException
     *             , XnatException
     */
    public XnatSubject createSubject(String projectId, XnatSubject subject) throws WebException, XnatClientException
    {
        return putSubject(projectId, subject, true);
    }

    public XnatSubject updateSubject(String projectId, XnatSubject subject) throws WebException, XnatClientException
    {
        return putSubject(projectId, subject, false);
    }

    protected XnatSubject putSubject(String projectId, XnatSubject subject, boolean newSubject) throws WebException,
            XnatClientException
    {
        String subjectLabel = subject.getLabel();

        if (subjectLabel == null)
        {
            throw new NullPointerException("XnatSubject label is NULL!");
        }

        checkValidLabel("SubjectLabel", subjectLabel);

        String putstr = "data/projects/" + projectId + "/subjects/" + subjectLabel;

        URIQueryParameters pars = new URIQueryParameters();

        // short names allowed for following default parameters:
        String parNames[] = new String[]
        {
                XnatConst.FIELD_SUBJECT_AGE,
                XnatConst.FIELD_SUBJECT_DOB,
                XnatConst.FIELD_SUBJECT_YOB,
                XnatConst.FIELD_SUBJECT_GENDER,
                XnatConst.FIELD_SUBJECT_HANDEDNESS
        };

        for (String name : parNames)
        {
            if (subject.hasField(name))
            {
                // user lower case names:
                pars.put(name.toLowerCase(), subject.get(name));
            }
        }

        // iterate over custom fields:
        List<String> customFields = subject.getCustomFieldNames();
        for (String field : customFields)
        {
            String parName = "xnat:subjectData/fields/field[name=" + field.toLowerCase() + "]/field";
            String parValue = subject.getCustomField(field);
            if (parValue != null)
            {
                pars.put(parName, parValue);
                logger.debugPrintf("putSubject(): URI Parameter '%s=%s' =>  %s\n", parName, parValue,
                        pars.get(pars.size() - 1));
            }
            else
            {
                logger.warnPrintf("Value of custom data field is null:%s\n", field);
            }
        }

        try
        {
            if (pars.size() > 0)
            {
                putstr += "?" + pars.toQueryString();
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WebException(Reason.URI_EXCEPTION, "Encoding error:" + e.getMessage(), e);
        }

        String resultstr = doPutQueryString(putstr);
        subject.updateId(resultstr);

        return subject;
    }

    public XnatSession createSession(XnatSubject subject, String sessionLabel) throws WebException, XnatClientException
    {
        XnatSession session = XnatSession.createXnatSession(subject.getProjectID(), subject.getLabel(), sessionLabel);
        return this.createSession(session);
    }

    /**
     * Create new Session. Returns session ID.
     * 
     * If sessionLabel already exists, the id of the existing session will be returned.
     * 
     * @throws XnatClientException
     */
    public XnatSession createSession(String projectId, String subjectLabel, XnatSession session) throws WebException,
            XnatClientException
    {
        session.setProjectId(projectId);
        session.setSubjectLabel(subjectLabel);
        return putSession(session, true);
    }

    public XnatSession createSession(XnatSession session) throws WebException, XnatClientException
    {
        return putSession(session, true);
    }

    public XnatSession updateSession(XnatSession session) throws WebException, XnatClientException
    {
        return putSession(session, false);
    }

    protected XnatSession putSession(XnatSession session, boolean newSession) throws WebException, XnatClientException
    {
        // XNATRestClient $rest_params -m PUT -remote
        // "/data/archive/projects/YOURTEST/subjects/subject1/experiments/session1?xnat:mrSessionData/date=01/02/07"
        checkValidLabel("XnatSession::Label", session.getLabel());

        String usDateString = null;
        String projectId = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();

        URIQueryParameters pars = new URIQueryParameters();

        Date sessionDate = session.getSessionDate();

        if ((sessionDate == null) && (newSession))
        {
            sessionDate = Presentation.createDate(System.currentTimeMillis());
        }

        if (sessionDate != null)
        {
            GregorianCalendar time = new GregorianCalendar();
            time.setTime(sessionDate);
            time.setTimeZone(TimeZone.getTimeZone("GMT"));

            usDateString = time.get(Calendar.YEAR) + "-" + (1 + time.get(Calendar.MONTH)) + "-"
                    + time.get(Calendar.DAY_OF_MONTH);
            pars.put("xnat:mrSessionData/date", usDateString);

            // // 24 hours scale:
            // String
            // timeString=time.get(Calendar.HOUR_OF_DAY)+":"+time.get(Calendar.MINUTE)+":"+time.get(Calendar.SECOND);
            // pars.add("xnat:mrSessionData/time="+timeString);
        }

        // short names allowed for following default parameters:
        String parNames[] = new String[]
        {
            XnatConst.FIELD_SESSION_AGE
        };

        for (String name : parNames)
        {
            if (session.hasField(name))
            {
                // here a 'xnat:...' type prefix must be used. Also must use
                // lower case names for parameter names.
                pars.put("xnat:mrSessionData/" + name.toLowerCase(), session.get(name));
            }
        }

        // iterate over custom fields:
        List<String> customFields = session.getCustomFieldNames();
        for (String field : customFields)
        {
            String parName = "xnat:mrSessionData/fields/field[name=" + field.toLowerCase() + "]/field";
            String parValue = session.getCustomField(field);
            if (parValue != null)
            {
                pars.put(parName, parValue);
                logger.debugPrintf("putSession(): adding custom  Parameter %s:%s\n\n", parName, parValue);
            }
            else
            {
                logger.warnPrintf("Value of custom data field is null:%s\n", field);
            }
        }

        String putStr = "data/projects/" + projectId + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel;

        try
        {
            if (pars.size() > 0)
            {
                putStr += "?" + pars.toQueryString();
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WebException(Reason.URI_EXCEPTION, "URI Encoding error:" + e.getMessage(), e);
        }

        String resultStr = doPutQueryString(putStr);
        session.updateId(resultStr);

        if (newSession)
        {
            logger.infoPrintf("Created new Session %s:%s\n", session.getID(), session.getLabel());
        }
        else
        {
            logger.infoPrintf("Updated Session: %s:%s\n", session.getID(), session.getLabel());
        }

        return session;
    }

    /**
     * Create MR Scan. Using XNAT Type: xnat:mrScanData Returns new scanID.
     * 
     * If subjectLabel already exists, the id of the existing subject will be returned.
     */
    public String createMrScan(XnatSession session, NewScanInfo info) throws WebException
    {
        // String newScanID,String mrScanDataType

        // XNATRestClient $rest_params -m PUT -remote
        // "/data/archive/projects/YOURTEST/subjects/subject1/experiments/session1/scans/SCAN1?xnat:mrScanData/type=T1"

        String projectid = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();

        String putStr = "data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel
                + "/scans/" + info.getScanID();

        // If the session is a mrSession the scan type should default to
        // mrScanDataType
        // XNAT 1.6.2 patch: Must use explicit xsiType (not xsi:type).
        //
        URIQueryParameters pars = new URIQueryParameters();
        pars.put("xsiType", "xnat:mrScanData");

        // Extra Scan information:
        if (info.note != null)
        {
            pars.put(XnatConst.FIELD_NOTE, info.note);
        }
        if (info.quality != null)
        {
            pars.put(XnatConst.FIELD_SCAN_QUALITY, info.quality);
        }
        if (info.series_description != null)
        {
            pars.put(XnatConst.FIELD_SCAN_SERIES_DESCRIPTION, info.series_description);
        }

        try
        {
            if (pars.size() > 0)
            {
                putStr += "?" + pars.toQueryString();
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WebException(Reason.URI_EXCEPTION, "URI Encoding error:" + e.getMessage(), e);
        }

        String resultstr = doPutQueryString(putStr);

        if (StringUtil.isEmpty(resultstr))
        {
            logger.warnPrintf("createMrScan(): Put didn't return any data. Assuming scan was created:%s\n", info.getScanID());
            resultstr = info.getScanID();
        }

        return resultstr;
    }

    /**
     * Create Reconstruction Returns new scanID.
     * 
     * If subjectLabel already exists, the id of the existing subject will be returned.
     */
    public String createReconstruction(XnatSession session,
            String reconId,
            String reconType) throws WebException, XnatClientException
    {
        // "/data/prearchive/projects/project1/subjects/subject1/experiments/session1/reconstructions/session1_recon_343?xnat:reconstructedImageData/type=T1_RECON

        String projectid = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();

        // if (reconType != ImageContentType.T1_RECON) // || T2_RECON
        // {
        // throw new Exception("Type muse be RECON type:" + reconType);
        // }

        String putStr = "data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel
                + "/reconstructions/" + reconId;

        putStr += "?xnat:reconstructedImageData/type=" + reconType;

        // Create Reconstruction:
        String resultstr = doPutQueryString(putStr);

        if (StringUtil.isEmpty(resultstr))
        {
            logger.warnPrintf("createReconstruction(): Put didn't return any data. Assuming scan was created:%s\n",
                    reconId);
            resultstr = reconId;
        }

        return resultstr;
    }

    // ========================================================================
    // File Putters
    // ========================================================================

    public String putDicomFile(XnatSession session,
            XnatScan scan,
            String fullFilepath,
            ImageFileInfo imageInfo,
            PutMonitor optPutMonitor) throws WebException, XnatClientException
    {
        if (StringUtil.equals(imageInfo.getImageFormatType(), ImageFormatType.DICOM) == false)
        {
            throw new XnatClientException("putDicomFile: Image type must be DICOM, is:" + imageInfo.getImageFormatType());
        }

        String resourceLabel = "" + imageInfo.getImageFormatType();
        return putResourceFile(session, XnatResourceType.SCAN, scan.getID(), resourceLabel, fullFilepath, imageInfo, optPutMonitor);
    }

    public String putNiftiScanFile(XnatSession session,
            String scanLabel,
            String fullFilepath,
            ImageFileInfo imageInfo,
            PutMonitor optPutMonitor) throws WebException, XnatClientException
    {
        if (!StringUtil.equals(imageInfo.getImageFormatType(), ImageFormatType.NIFTI))
        {
            throw new XnatClientException("putNiftiFile: Image type must be NIFTI, is:" + imageInfo.getImageFormatType());
        }

        String resourceLabel = "" + imageInfo.getImageFormatType();
        return putResourceFile(session, XnatResourceType.SCAN, scanLabel, resourceLabel, fullFilepath, imageInfo, optPutMonitor);
    }

    /**
     * Upload 3D (Nifti) Reconstruction or Atlas
     * 
     * @param session
     *            - XnatSession to upload to
     * @param reconId
     *            - reconstruction ID used after .../reconstructions/ part in URL
     * @param fullFilepath
     *            - absolute path of local file to upload.
     * @param imageInfo
     *            - Image Information whether this is DICOM or NIFTI, etc.
     * @param optPutMonitor
     *            - optional Put Monitor to monitor the upload. May be null.
     * @return
     * @throws Exception
     */
    public String putReconstructionFile(XnatSession session,
            String reconId,
            String fullFilepath,
            ImageFileInfo imageInfo,
            PutMonitor optPutMonitor) throws WebException, XnatClientException
    {
        String resourceLabel = imageInfo.getImageFormatType();
        return putResourceFile(session, XnatResourceType.RECONSTRUCTION, reconId, resourceLabel, fullFilepath, imageInfo, optPutMonitor);
    }

    /**
     * Upload Scan or Reconstruction file
     * 
     * @param session
     *            - XnatSession to upload to
     * @param resourceId
     *            - Scan ID or Reconstruction ID used after scans/... or reconstruction/... part.
     * @param fullFilepath
     *            - absolute path of local file to upload.
     * @param imageInfo
     *            - Image Information whether this is DICOM or NIFTI, etc.
     * @param optPutMonitor
     *            - optional Put Monitor to monitor the upload. May be null.
     * @return
     * @throws Exception
     */
    public String putResourceFile(XnatSession session,
            XnatResourceType resourceType,
            String resourceID,
            String optResourceLabel,
            String fullFilepath,
            ImageFileInfo imageInfo,
            PutMonitor optPutMonitor) throws WebException, XnatClientException
    {

        logger.infoPrintf(">putResourceFile():(<Project>/<Sesssion>/<%s>/<Resource>)%s/%s/%s/%s <= %s\n",
                resourceType,
                session.getProjectID(),
                session.getLabel(),
                resourceID,
                optResourceLabel,
                fullFilepath);

        if (optResourceLabel == null)
        {
            // image (resource) type, for example: "DICOM" or "NIFTI".
            optResourceLabel = imageInfo.getImageFormatType();
        }

        String destFilename = imageInfo.getDestinationFilename();
        String putStr = createResourceFilesURL(session, resourceType, resourceID, imageInfo, optResourceLabel);

        try
        {
            logger.debugPrintf(" - putStr=%s\n", putStr);

            // XNATRestClient $rest_params -m PUT -remote
            // "/data/archive/projects/YOURTEST/subjects/SUBJECTID/experiments/SESSIONID/scans/SCANID/files/1232132.dcm?format=DICOM&content=T1_RAW"
            // -local /data/subject1/session1/RAW/SCAN1/1232132.dcm
            // putFile(projectid,subjectLabel,sessionLabel,scanID,fullFilepath,info)

            StringHolder resultH = new StringHolder();

            FSUtil fs = getFSUtil();
            FSPath file = fs.newFSPath(fullFilepath);

            if (StringUtil.isEmpty(destFilename))
            {
                destFilename = file.getBasename();
            }

            logger.infoPrintf("putScanFile():%s=>%s\n", fullFilepath, putStr);

            // put file:
            String filePath = file.getPathname();
            int status = webClient.doPutFile(putStr, filePath, resultH, optPutMonitor);

            return resultH.value;
        }
        catch (WebException e)
        {
            throw handlePutFileException(e, putStr, imageInfo);
        }
        catch (IOException e)
        {
            throw new XnatClientException("Couldn't locate source file to upload:" + fullFilepath, e);
        }
    }

    public String putResourceFile(XnatSession session,
            XnatResourceType resourceType,
            String resourceId,
            String optResourceLabel,
            byte[] fileBytes,
            ImageFileInfo imageInfo,
            PutMonitor optPutMonitor) throws XnatClientException, WebException
    {
        logger.debugPrintf(">putScanFile(): <Project>/<Sesssion>/<%s>/<Resource>): %s/%s/%s/%s (#bytes=%d)\n",
                "" + resourceType,
                session.getProjectID(),
                session.getLabel(),
                resourceId,
                optResourceLabel,
                fileBytes.length);

        String putStr = createResourceFilesURL(session, resourceType, resourceId, imageInfo, optResourceLabel);

        try
        {
            logger.infoPrintf(" - putScanFile():%s (#bytes=%d) =>%s\n", imageInfo.getDestinationFilename(), fileBytes.length, putStr);

            StringHolder resultH = new StringHolder();

            // Use Http Put with bytes as body:
            int status = webClient.doPutBytes(putStr, fileBytes, "application/octetstream", resultH, optPutMonitor);

            return resultH.value;
        }
        catch (WebException e)
        {
            throw handlePutFileException(e, putStr, imageInfo);
        }
    }

    private WebException handlePutFileException(WebException e, String putStr, ImageFileInfo imageInfo)
    {
        String errorStr = "File already exists";
        String txt = "" + e.getMessage();
        if (txt.contains(errorStr))
        {
            return new WebException(WebException.Reason.RESOURCE_ALREADY_EXISTS, "Remote file already exists.\n"
                    + "- PUT URL=" + putStr + "\n"
                    + "- destinationFile=" + imageInfo.getDestinationFilename() + "\n", e);

        }
        else
        {
            return e;
        }
    }

    protected String createResourceFilesURL(XnatSession session, XnatResourceType resourceType, String resourceId, ImageFileInfo fileInfo,
            String optResourceLabel) throws XnatClientException
    {
        String projectid = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();
        String destFilename = fileInfo.getDestinationFilename();

        String putStr = "data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel;

        String resourceFilesPath = createResourceFilesPath(resourceType, resourceId, optResourceLabel);

        putStr += "/" + resourceFilesPath + "/" + destFilename;
        URIQueryParameters pars = new URIQueryParameters();
        try
        {
            // encode parameters:

            pars.put("format", fileInfo.getImageFormatType());
            pars.put("content", fileInfo.getContentType());
            putStr += "?" + pars.toQueryString();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new XnatClientException("Failed to encode parameters:" + pars.toString(), e);
        }

        return putStr;
    }

    /**
     * Switch whether to upload files to scans/reconstructions and with optional Resource Label as follows:
     * <ul>
     * <li>"scans/{ScanID}/files?"
     * <li>"scans/{ScanID}/resources/{LABEL}/files"
     * <li>"reconstructions/{ReconID}/files
     * <li>"reconstructions/{ReconID}/resources/{LABEL}/files"
     * </ul>
     * 
     * @param resourceType
     *            - SCAN or RECONSTRUCTION
     * @param resourceId
     *            - ScanID or ReconID
     * @param optResourceLabel
     *            - optional resource label
     * @return URL sub path of resource files destination:
     * @throws XnatClientException
     */
    private String createResourceFilesPath(XnatResourceType resourceType, String resourceId, String optResourceLabel)
            throws XnatClientException
    {
        String pathStr;

        if (resourceType == XnatResourceType.SCAN)
        {
            pathStr = "scans/" + resourceId;
        }
        else if (resourceType == XnatResourceType.RECONSTRUCTION)
        {
            pathStr = "reconstructions/" + resourceId;
        }
        else
        {
            throw new XnatClientException("Unsupported XnatResourceType:" + resourceType);
        }

        if ((optResourceLabel == null) || (optResourceLabel == ""))
        {
            pathStr += "/files";
        }
        else
        {
            pathStr += "/resources/" + optResourceLabel + "/files";
        }

        return pathStr;
    }

    // ========================================================================
    // File Getters
    // ========================================================================

    public ResponseInputStream getScanFileInputStream(XnatSession session, String scanLbl, String optResourceLabel, String remoteFileName)
            throws Exception
    {
        return getResourceFileInputStream(session, XnatResourceType.SCAN, scanLbl, optResourceLabel, remoteFileName);
    }

    public ResponseInputStream getReconstructionFileInputStream(XnatSession session, String reconId, String optResourceLabel,
            String remoteFileName)
            throws Exception
    {
        return getResourceFileInputStream(session, XnatResourceType.RECONSTRUCTION, reconId, optResourceLabel, remoteFileName);
    }

    public ResponseInputStream getResourceFileInputStream(XnatSession session, XnatResourceType resourceType, String resourceId,
            String optResourceLabel, String remoteFileName)
            throws Exception
    {
        String projectid = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();
        String queryStr = null;

        try
        {
            // XNATRestClient $rest_params -m PUT -remote
            // "/data/archive/projects/YOURTEST/subjects/subject1/experiments/session1/scans/SCAN1/files/1232132.dcm?format=DICOM&content=T1_RAW"
            // -local /data/subject1/session1/RAW/SCAN1/1232132.dcm
            // putFile(projectid,subjectLabel,sessionLabel,scanID,fullFilepath,info)

            String resourceFilesPath = createResourceFilesPath(resourceType, resourceId, optResourceLabel);

            queryStr = "data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel
                    + "/" + resourceFilesPath + "/" + remoteFileName;

            logger.infoPrintf("queryFile():%s=>%s\n", remoteFileName, queryStr);

            return webClient.doGetInputStream(queryStr);
        }
        catch (WebException e)
        {
            throw handleGetFileException(e, queryStr);
        }
    }

    public ResponseInputStream getSessionScansZipInputStream(XnatSession session) throws Exception
    {
        // /data/archive/projects/TEST/subjects/1/experiments/MR1/scans/ALL/files?format=zip
        // return .../scans/ALL/

        return getScanSetZipInputStream(session, "ALL", null);
    }

    /**
     * Create ZipFileStream to complete ScanSet for downloading purposes.
     * 
     * @param session
     *            XnatSession
     * @param scan
     *            XnatScan
     * @param resourceLabel
     *            Resource Label, for example "DICOM" of "NIFTI" or null
     * @return
     * @throws Exception
     */
    public ResponseInputStream getScanSetZipInputStream(XnatSession session, String scanId, String optResourceLabel) throws Exception
    {
        String projectid = session.getProjectID();
        String subjectLabel = session.getSubjectLabel();
        String sessionLabel = session.getLabel();
        String queryStr = null;

        try
        {
            // XNATRestClient $rest_params -m PUT -remote
            // "/data/archive/projects/YOURTEST/subjects/subject1/experiments/session1/scans/SCAN1/files/1232132.dcm?format=DICOM&content=T1_RAW"
            // -local /data/subject1/session1/RAW/SCAN1/1232132.dcm
            // putFile(projectid,subjectLabel,sessionLabel,scanID,fullFilepath,info)

            String resourceFilesPath = createResourceFilesPath(XnatResourceType.SCAN, scanId, optResourceLabel);

            queryStr = "data/projects/" + projectid + "/subjects/" + subjectLabel + "/experiments/" + sessionLabel
                    + "/" + resourceFilesPath + "?format=zip"; // +"&structure=legacy";

            logger.infoPrintf("getScanSetZipInputStream(): %s => %s\n", resourceFilesPath, queryStr);

            return webClient.doGetInputStream(queryStr);
        }
        catch (WebException e)
        {
            handleGetFileException(e, queryStr);
            return null;
        }
    }

    /**
     * Create ZipFileStream from complete Subject. <br>
     * This zip contains all the files under scans and other resources.
     * 
     * @param subjet
     *            XnatSubject
     * @param resourceLabel
     *            Resource Label to filter, for example "DICOM" of "NIFTI" or null
     * @return
     * @throws Exception
     */
    public ResponseInputStream getSubjectZipInputStream(XnatSubject subject, String optResourceLabel) throws Exception
    {
        String projectid = subject.getProjectID();
        String subjectLabel = subject.getLabel();

        String queryStr = null;

        try
        {
            // XNATRestClient $rest_params -m PUT -remote
            // "/data/archive/projects/YOURTEST/subjects/subject1/experiments/session1/scans/SCAN1/files/1232132.dcm?format=DICOM&content=T1_RAW"
            // -local /data/subject1/session1/RAW/SCAN1/1232132.dcm
            // putFile(projectid,subjectLabel,sessionLabel,scanID,fullFilepath,info)

            queryStr = "data/projects/" + projectid + "/subjects/" + subjectLabel
                    + "?format=zip"; // +"&structure=legacy";

            logger.infoPrintf("getScanSetZipInputStream(): subject:%s => %s\n", subjectLabel, queryStr);

            return webClient.doGetInputStream(queryStr);
        }
        catch (WebException e)
        {
            handleGetFileException(e, queryStr);
            return null;
        }
    }

    private WebException handleGetFileException(WebException e, String getStr) throws WebException
    {
        String errorStr = "File not found";
        String txt = "" + e.getMessage();
        if (txt.contains(errorStr))
        {
            return new WebException(WebException.Reason.RESOURCE_NOT_FOUND, "Remote file or resource not found.\n"
                    + " GET URL=" + getStr + "\n", e);
        }
        else
        {
            return e;
        }
    }

    /**
     * Upload single Scan Set directly from directory. Directory must contain exactly one scan set.
     */
    public String uploadDicomScanFiles(XnatSession session,
            XnatScan scan,
            String scanSetDirectory,
            ImageFileInfo imageInfo) throws Exception
    {
        FSUtil fs = getFSUtil();
        FSPath scanDir = fs.newFSPath(scanSetDirectory);

        FSPath[] files = scanDir.listNodes();

        logger.infoPrintf("uploadScanFiles():%s=>%s/%s/%s/%s\n",
                scanSetDirectory, session.getProjectID(),
                session.getSubjectLabel(), session.getLabel(), scan.getID());

        String resultstr = "";

        for (int i = 0; i < files.length; i++)
        {
            String res = this.putDicomFile(session, scan, files[i].getPathname(), imageInfo, null);
            if (res != null)
            {
                resultstr += res;
            }
        }

        return resultstr;
    }

    // ========================================================================
    // CSV Meta Data Upload:
    // ========================================================================

    /**
     * Upload CSV style meta data.
     * 
     * Supports both subject and session meta data. First line of CSV file must have the headers defined. Subject field
     * must be prefixed with "subject." and session fields must be prefixed with "session.".<br>
     * For example:
     * 
     * <pre>
     * subject.id,subject.gender,session.id,session.age
     * "patient1","Male","session01","50" 
     * "patient1","Male","session02","55" 
     * "patient2","Female","session01","60" 
     * "patient2","Female","session02","65"
     * </pre>
     * 
     * @param projectId
     * @param csvText
     * @param createSubjects
     * @param createSessions
     * @throws Exception
     */
    public void putMetaData(String projectId, String csvText, boolean createSubjects, boolean createSessions)
            throws WebException, XnatClientException
    {

        try
        {
            CSVData reader = new CSVData();
            reader.setFieldSeparators(new String[]
            {
                    ";", ","
            });
            reader.parseText(csvText);

            putMetaData(projectId, reader, createSubjects, createSessions, null);
        }
        catch (IOException e)
        {
            throw new XnatClientException("Failed to parse text.\n" + e.getMessage(), e);
        }
        // catch (WebException | XnatException e)
    }

    public void putMetaData(String projectId, CSVData csvData, boolean createSubjects, boolean createSessions, UploadMonitor monitor)
            throws WebException, XnatClientException
    {
        boolean prefixSessionWithSubject = false;

        List<String> headers = csvData.getHeaders();

        StringList sessionFields = new StringList();
        for (String header : headers)
        {
            if (header.startsWith("session.") && ((header.equals("session.label") == false) || (header.equals("session.id") == false)))
            {
                sessionFields.add(header.substring("session.".length()));
            }
        }

        StringList subjectFields = new StringList();
        for (String header : headers)
        {
            if (header.startsWith("subject.") && ((header.equals("subject.label") == false) || (header.equals("subject.id") == false)))
            {
                subjectFields.add(header.substring("subject.".length()));
            }
        }

        int numRows = csvData.getNrOfRows();

        if (monitor != null)
        {
            monitor.notifyStartUpload("Uploading #" + numRows + " rows of CSV Meta-Data", new int[]
            {
                numRows
            });
        }

        for (int row = 0; row < numRows; row++)
        {
            if ((csvData.getNrColumns(row) <= 0) || StringUtil.isEmpty(csvData.getRowAsString(row)))
            {
                logger.debugPrintf(" - skipping empty row:#%s\n", row);
                continue;
            }

            if (monitor != null)
            {
                monitor.notifyFileStart(0, row, 1, null);
            }

            // Subject ID field from CVS is actually the Xnat Subject Label:
            String subjectLbl = csvData.get(row, "subject.label");
            String sessionLbl;
            String sessionId;

            // =====================
            // Create/Update Subject
            // =====================

            if (StringUtil.isEmpty(subjectLbl))
            {
                throw new XnatClientException("No 'subject.label' field in (row,column):" + row + ","
                        + csvData.getFieldNr("subject.label") + "\nRow=" + csvData.getRowAsString(row));
            }

            // label only subject:
            XnatSubject subject = XnatSubject.createXnatSubject(projectId, subjectLbl);

            // Check/update subject fields if specified
            for (String field : subjectFields)
            {
                String fieldValue = csvData.getField(row, "subject." + field, true);
                if ((fieldValue == null) || (fieldValue == ""))
                {
                    logger.debugPrintf(" - skipping field:%s\n", field);
                    continue;
                }

                if (subject.isStandardField(field, false))
                {
                    subject.set(field, fieldValue);
                    logger.debugPrintf(" - setting standard subject field: %s=%s\n", field, fieldValue);
                }
                else
                {
                    subject.setCustomField(field, fieldValue);
                    logger.debugPrintf(" - setting *custom* subject field: %s=%s\n", field, fieldValue);
                }
            }

            if (monitor != null)
            {
                monitor.logPrintf("Updating Subject:%s\n", subjectLbl);

                for (String name : subject.getMetaDataFieldNames(true))
                {
                    String value = subject.get(name);
                    if (value != null)
                        monitor.logPrintf(" - %s:%s\n", name, value);
                }
            }

            if (createSubjects == false)
            {
                XnatSubject remoteSubject = null;
                try
                {
                    remoteSubject = this.getSubjectByLabel(projectId, subjectLbl);
                }
                catch (WebException e)
                {
                    if (e.getReason() != Reason.RESOURCE_NOT_FOUND)
                    {
                        throw new XnatClientException("Failed to query Subject:" + subjectLbl, e);
                    }
                }

                if (remoteSubject == null)
                {
                    throw new XnatClientException("Subject doesn't exist: project/subject=" + projectId + "/" + subjectLbl);
                }

                this.updateSubject(projectId, subject);
            }
            else
            {
                // create or update:
                this.createSubject(projectId, subject);
            }

            // =====================
            // Create/Update Session
            // =====================

            sessionLbl = csvData.get(row, "session.label");

            if (StringUtil.isEmpty(sessionLbl))
            {
                logger.infoPrintf(" - no session.label specified: skipping session data for row/subject:%s/%s\n", row,
                        subjectLbl);
                // throw new
                // Exception("No 'session.id' field in (row,column):"+row+","+reader.getFieldNr("session.id"));

                // Extra check: if a session.id is not specified, but there is
                // session meta data
                // throw exception to indicate inconsistency in the meta-data
                for (String field : sessionFields)
                {
                    String fieldValue = csvData.getField(row, "session." + field, true);
                    if ((fieldValue != null) && (StringUtil.isWhiteSpace(fieldValue) == false))
                    {
                        throw new XnatClientException(
                                "No 'session.id' was specified, but there is session meta-data for field: session."
                                        + field + "=" + fieldValue);
                    }
                }
                // ok
                continue;
            }

            // ***
            // Important: within a project no similar session/experiment labels
            // may be used for different subjects.
            // Prefix session label with subject label.
            //
            if (prefixSessionWithSubject)
            {
                sessionLbl = subjectLbl + "_" + sessionLbl;
            }
            else
            {
                // sessionLbl = sessionLbl;
            }

            XnatSession session = XnatSession.createXnatSession(projectId, subjectLbl, sessionLbl);

            // Assume one session per row. Previous values are overwritten
            for (String field : sessionFields)
            {
                String fieldValue = csvData.getField(row, "session." + field, true);
                if (session.isStandardField(field, false))
                {
                    session.set(field, fieldValue);
                    logger.debugPrintf(" - setting standard session field: %s=%s\n", field, fieldValue);
                }
                else
                {
                    session.setCustomField(field, fieldValue);
                    logger.debugPrintf(" - setting *custom* session field: %s=%s\n", field, fieldValue);
                }
            }

            if (monitor != null)
            {
                monitor.logPrintf("Updating session:%s\n", sessionLbl);

                String rowstr = "";

                for (String name : session.getMetaDataFieldNames(true))
                {
                    String value = session.get(name);
                    if (value != null)
                        monitor.logPrintf(" - %s:%s\n", name, value);
                }
            }

            if (createSessions == false)
            {
                XnatSession remoteSession = null;

                try
                {
                    remoteSession = this.getSession(projectId, subjectLbl, sessionLbl);
                }
                catch (WebException e)
                {
                    if (e.getReason() != Reason.RESOURCE_NOT_FOUND)
                    {
                        throw new XnatClientException("Failed to query Subject:" + subjectLbl, e);
                    }
                }

                if (remoteSession == null)
                {
                    throw new XnatClientException("Session doesn't exist: project/subject/session=" + projectId + "/"
                            + subjectLbl + "/" + sessionLbl);
                }

                this.updateSession(session);
            }
            else
            {
                // create/update session
                this.putSession(session, createSessions);
            }

            if (monitor != null)
            {
                monitor.notifyFileDone(0, row);
            }
        }

        if (monitor != null)
        {
            monitor.logPrintf("Finished Updating Meta-Data\n");
        }
    }

    // ========================================================================
    // Miscellaneous
    // ========================================================================

    // Not tested.
    public String setCustomQualityLabels(String labels[]) throws WebException
    {
        // curl -X PUT -u admin
        // https://my.xnat.org/REST/config/scan-quality/labels?inbody=true
        // --data-binary @scan-quality-labels.txt

        StringList labelList = new StringList(labels);
        String labelstr = labelList.toString(",");
        logger.debugPrintf("New labels='%s'\n", labelstr);

        StringHolder holderText = new StringHolder();

        int status = webClient.doPutString("REST/config/scan-quality/labels", labelstr, holderText, true);
        return holderText.value;
    }

    /**
     * Label String can not contain spaces or other special characters, only the set: [a-zA-Z0-9_]. Label Names must be
     * URI proof.
     * 
     * @param labelName
     *            - Label Name
     * @param labelString
     *            - Actual Label String to check.
     * @return
     * @throws XnatClientException
     */
    public boolean checkValidLabel(String labelName, String labelString) throws XnatClientException
    {
        if (StringUtil.isEmpty(labelString))
        {
            throw new XnatClientException("Label:" + labelName + " can not be empty!");
        }

        if (labelString.contains(" "))
        {
            throw new XnatClientException("Label:" + labelName + " can not contain spaces or other special characters:'" + labelString
                    + "'");
        }

        // test REs:
        if (labelString.matches("[a-zA-Z0-9_]*") == false)
        {
            throw new XnatClientException("Label:" + labelName + " can not contain special characters:'" + labelString + "'");
        }

        return true;
    }

    public void setCertificateStore(CertificateStore certStore) throws CertificateStoreException
    {
        this.webClient.setCertificateStore(certStore);
    }

    public FSUtil getFSUtil()
    {
        return FSUtil.getDefault();
    }

    // === Misc. ===

    public String toString()
    {
        return "XnatClient:[uri:" + this.webClient.getServiceURI() + ", isAuthenticated:" + this.isAuthenticated() + "]";
    }

    public String getJSessionID()
    {
        if (this.webClient == null)
            return null;
        return this.webClient.getJSessionID();
    }

    /**
     * Find Scan within a Project.
     * 
     * @param projectId
     *            - Project ID.
     * @param scanLbl
     *            - Scan only have a label which is also the scan ID.
     * @return List of <Session,Scan> pairs. The Session is needed to uniquely identify the Scan.
     * @throws XnatClientException
     * @throws WebException
     */
    public List<Pair<XnatSession, XnatScan>> findScanByLabel(String projectId, String scanLbl, boolean ignoreCase) throws WebException,
            XnatClientException
    {
        logger.infoPrintf("findScanByLabel(): projectId=%s,scanLbl=%s\n", projectId, scanLbl);

        // todo: use Xnat rest query engine.
        ArrayList<Pair<XnatSession, XnatScan>> filteredScans = new ArrayList<Pair<XnatSession, XnatScan>>();

        List<XnatSubject> subjs = listSubjects(projectId);

        for (XnatSubject sub : subjs)
        {
            List<XnatSession> sess = listSessions(sub);
            for (XnatSession ses : sess)
            {
                List<XnatScan> scans = this.listScans(ses);
                for (XnatScan scan : scans)
                {
                    if (StringUtil.equals(ignoreCase, scan.getID(), scanLbl))
                    {
                        logger.infoPrintf("- found matching scan:%s\n", scan);
                        Pair<XnatSession, XnatScan> pair = new Pair<XnatSession, XnatScan>(ses, scan);
                        filteredScans.add(pair);
                    }
                }
            }
        }
        logger.infoPrintf("findScanByLabel(): returning #%d scans\n", filteredScans.size());

        return filteredScans;
    }

}

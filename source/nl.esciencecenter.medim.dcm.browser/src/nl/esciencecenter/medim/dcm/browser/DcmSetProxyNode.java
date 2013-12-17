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
package nl.esciencecenter.medim.dcm.browser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.esciencecenter.medim.SeriesInfo;
import nl.esciencecenter.medim.StudyInfo;
import nl.esciencecenter.medim.SubjectInfo;
import nl.esciencecenter.medim.dcm.browser.DcmProxyFactory.DicomSetType;
import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.ptk.data.LongHolder;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.net.URIFactory;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyException;
import nl.esciencecenter.vbrowser.vrs.data.Attribute;
import nl.esciencecenter.vbrowser.vrs.exceptions.VRLSyntaxException;
import nl.esciencecenter.vbrowser.vrs.vrl.VRL;
import nl.esciencecenter.xnatclient.exceptions.ScanSetException;

public class DcmSetProxyNode extends DcmProxyNode
{
    protected DicomDirScanner dicomDirScanner;

    private ArrayList<DcmProxyNode> subNodes;

    private String identifier;

    private SubjectInfo subjectInfo;

    private StudyInfo studyInfo;

    private SeriesInfo seriesInfo;

    protected DcmSetProxyNode(DcmProxyFactory factory, DcmSetProxyNode parent, VRL proxyLocation)
    {
        super(factory, parent, proxyLocation);
        setType = DicomSetType.DicomDirectory;
    }

    @Override
    protected String doGetName() throws ProxyException
    {
        switch (setType)
        {
            case DicomDirectory:
            {
                return getVRL().getBasename();
            }
            case DicomSubject:
            {
                if (subjectInfo != null)
                    return subjectInfo.getPatientName();
                return getVRL().getQuery();
            }
            case DicomStudy:
            {
                if (studyInfo != null)
                    return studyInfo.getStudyDescription();
                return getVRL().getQuery();
            }
            case DicomScanSet:
            {
                if (seriesInfo != null)
                    return seriesInfo.getSeriesDescription();
                return getVRL().getQuery();
            }
            default:
                return getVRL().getQuery();
        }
    }

    @Override
    protected String doGetResourceType() throws ProxyException
    {
        return setType.toString();
    }

    @Override
    protected String doGetResourceStatus() throws ProxyException
    {
        return "ok";
    }

    @Override
    protected String doGetMimeType() throws ProxyException
    {
        return null;
    }

    @Override
    protected boolean doGetIsComposite() throws ProxyException
    {
        return true;
    }

    @Override
    protected List<DcmProxyNode> doGetChilds(int offset, int range, LongHolder numChildsLeft) throws ProxyException
    {
        if (setType == DicomSetType.DicomDirectory)
        {
            // auto init DicomDir:
            if (dicomDirScanner == null)
            {
                dicomDirScanner = new DicomDirScanner();

                try
                {
                    dicomDirScanner.scanDirectory(this.locator.toURI(), true,null);
                }
                catch (IOException | URISyntaxException | InterruptedException | ScanSetException e)
                {
                    throw new ProxyException("Failed to scan direector:" + this.locator, e);
                }
            }
        }

        return getSubNodes();
    }

    private List<DcmProxyNode> getSubNodes() throws ProxyException
    {
        if (this.subNodes != null)
            return subNodes;

        if (setType == DicomSetType.DicomDirectory)
        {
            Set<String> ids = getDicomDirScanner().getSubjectIDs();
            return createSubjectNodes(ids);
        }
        else if (setType == DicomSetType.DicomSubject)
        {
            Set<String> ids = getDicomDirScanner().getStudyUids(identifier);
            return createStudyNodes(ids);
        }
        else if (setType == DicomSetType.DicomStudy)
        {
            Set<String> ids = getDicomDirScanner().getScanSetUids(identifier);
            return createScanSetNodes(ids);
        }
        else if (setType == DicomSetType.DicomScanSet)
        {
            ArrayList<DcmProxyNode> nodes=new ArrayList<DcmProxyNode>(); 
            nodes.add(createDcmFilesNode(identifier));
            return nodes; 
        }

        return null;
    }

    protected DicomDirScanner getDicomDirScanner()
    {
        DcmSetProxyNode current = this;

        while (current != null)
        {
            if (current.dicomDirScanner != null)
                return current.dicomDirScanner;
            current = current.parent;
        }

        return null;
    }

    private List<DcmProxyNode> createSubjectNodes(Set<String> ids) throws ProxyException
    {
        this.subNodes = new ArrayList<DcmProxyNode>();

        for (String id : ids)
        {
            subNodes.add(createSubjectNode(id));
        }
        return subNodes;
    }

    private DcmProxyNode createSubjectNode(String subjectId) throws ProxyException
    {
        VRL loc = createSubjectVRL(subjectId);

        DcmSetProxyNode node = new DcmSetProxyNode(this.getProxyFactory(), this, loc);
        node.setType = DicomSetType.DicomSubject;
        node.identifier = subjectId;

        node.subjectInfo = this.getDicomDirScanner().getSubjectInfo(subjectId);

        getProxyFactory().addNodeToHeap(node);

        return node;
    }

 
    
    private VRL createSubjectVRL(String id) throws ProxyException
    {

        try
        {
            return new VRL(getBaseVRL().toString() + "?subject=" + URIFactory.encodeQuery(id));
        }
        catch (VRLSyntaxException e)
        {
            throw new ProxyException("Invalid VRL:" + id, e);
        }
    }

    private VRL createStudyVRL(String id) throws ProxyException
    {

        try
        {
            return new VRL(getBaseVRL().toString() + "?study=" + URIFactory.encodeQuery(id));
        }
        catch (VRLSyntaxException e)
        {
            throw new ProxyException("Invalid VRL:" + id, e);
        }
    }

    private VRL createScanSetVRL(String id) throws ProxyException
    {

        try
        {
            return new VRL(getBaseVRL().toString() + "?series=" + URIFactory.encodeQuery(id));
        }
        catch (VRLSyntaxException e)
        {
            throw new ProxyException("Invalid VRL:" + id, e);
        }
    }

    public VRL createScanSetFilesVRL(String id) throws ProxyException
    {

        try
        {
            return new VRL(getBaseVRL().toString() + "?filesForSeries=" + URIFactory.encodeQuery(id));
        }
        catch (VRLSyntaxException e)
        {
            throw new ProxyException("Invalid VRL:" + id, e);
        }
    }
    
    private List<DcmProxyNode> createStudyNodes(Set<String> ids) throws ProxyException
    {
        this.subNodes = new ArrayList<DcmProxyNode>();

        for (String id : ids)
        {
            //System.err.printf("New studyNode=%s\n", id);
            subNodes.add(createStudyNode(id));
        }

        return subNodes;

    }

    private DcmProxyNode createStudyNode(String id) throws ProxyException
    {
        VRL loc = createStudyVRL(id);

        DcmSetProxyNode node = new DcmSetProxyNode(this.getProxyFactory(), this, loc);
        node.setType = DicomSetType.DicomStudy;
        node.identifier = id;
        node.studyInfo = this.getDicomDirScanner().getStudyInfo(id);
        getProxyFactory().addNodeToHeap(node);

        return node;
    }

    private List<DcmProxyNode> createScanSetNodes(Set<String> ids) throws ProxyException
    {
        this.subNodes = new ArrayList<DcmProxyNode>();

        for (String id : ids)
        {
            // System.err.printf("New scanSetNode=%s\n", id);
            subNodes.add(createScanSetNode(id));
        }

        return subNodes;

    }

    private DcmProxyNode createScanSetNode(String scanSetUid) throws ProxyException
    {
        VRL loc = createScanSetVRL(scanSetUid);

        DcmSetProxyNode node = new DcmSetProxyNode(this.getProxyFactory(), this, loc);
        node.setType = DicomSetType.DicomScanSet;
        node.identifier = scanSetUid;
        node.seriesInfo = this.getDicomDirScanner().getSeriesInfo(scanSetUid);
        getProxyFactory().addNodeToHeap(node);

        return node;
    }

    private DcmProxyNode createDcmFilesNode(String scanSetUid) throws ProxyException
    { 
        VRL loc = createScanSetFilesVRL(scanSetUid);

        DcmFilesNode node = new DcmFilesNode(this, loc,scanSetUid);
        getProxyFactory().addNodeToHeap(node);

        return node;
        
    }
    
    @Override
    protected List<String> doGetChildTypes() throws ProxyException
    {
        return null;
    }

    @Override
    protected List<String> doGetAttributeNames() throws ProxyException
    {
        // String[] names = super.getDefaultProxyAttributesNames();
        StringList list = new StringList() ; // names);
        list.add("name");
        list.add("identifier");
        list.add("resourceType");
        list.add("description");
        
        //list.remove("icon");
        //list.add(DcmProxyUtil.getDefaultDicomAttributeNames());
        return list;
    }

    @Override
    protected List<Attribute> doGetAttributes(List<String> names) throws ProxyException
    {
        List<Attribute> attrs = super.getDefaultProxyAttributes(names);

        for (int i = 0; i < names.size(); i++)
        {
            attrs.add(getAttribute(names.get(i)));
        }

        return attrs;
    }

    public Attribute getAttribute(String name)
    {
        if (name.equals("name"))
        {
            return new Attribute(name, getName());
        }
        else if (name.equals("identifier"))
        {
            return new Attribute(name,identifier); 
        }
        else if (name.equals("resourceType"))
        {
            return new Attribute(name,getResourceType()); 
        }
        else if (name.equals("description"))
        {
            return new Attribute(name, getDescription());
        }
        else if (DcmProxyUtil.isTagName(name))
        {
            return new Attribute(name,getTagStringValue(name)); 
        }
        
        return null; 
    }

    public String getDescription()
    {
        String value = this.getVRL().getQuery();

        switch (setType)
        {
            case DicomDirectory:
            {
                value = getVRL().getBasename();
                break;
            }
            case DicomSubject:
            {
                if (this.subjectInfo != null)
                    value = subjectInfo.getPatientName();
                break;
            }
            case DicomScanSet:
            {
                if (this.seriesInfo != null)
                    value = seriesInfo.getSeriesDescription();
                break;
            }
            case DicomStudy:
            {
                if (this.studyInfo != null)
                {
                    value = studyInfo.getStudyDescription();
                }
                break;
            }
            default:
            {
                value = getVRL().getQuery();
            }
        }
        
        return value; 

    }

    public String getTagStringValue(String name)
    {
        int tag=DcmProxyUtil.getTagNumber(name); 

        switch (setType)
        {
            case DicomDirectory:
                return getDicomDirTag(identifier,name);
            case DicomSubject:
                return getSubjectTag(identifier,name);
            case DicomStudy: 
                return getStudyTag(identifier,name); 
            case DicomScanSet:
                return getScanSetTag(identifier,name); 
            default:
                return null;
        }
    }

    private String getDicomDirTag(String dirName, String name)
    {
        return null;
    }


    private String getSubjectTag(String subjectId, String name)
    {
        return null;
    }
    
    private String getStudyTag(String studyUid, String name)
    {
        return null;
    }
    
    private String getScanSetTag(String identifier2, String name)
    {
        return null;
    }

}

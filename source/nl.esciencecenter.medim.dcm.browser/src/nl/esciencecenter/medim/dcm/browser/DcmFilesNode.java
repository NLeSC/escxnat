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

import java.util.ArrayList;
import java.util.List;

import nl.esciencecenter.medim.ScanSetInfo.FileDescriptor;
import nl.esciencecenter.medim.dcm.browser.DcmProxyFactory.DicomSetType;
import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.ptk.data.LongHolder;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSNode;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyException;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyNode;
import nl.esciencecenter.vbrowser.vrs.data.Attribute;
import nl.esciencecenter.vbrowser.vrs.exceptions.VRLSyntaxException;
import nl.esciencecenter.vbrowser.vrs.vrl.VRL;

import org.dcm4che2.data.Tag;

/** 
 * Dicom FileSet node. 
 */
public class DcmFilesNode extends DcmProxyNode
{
    private Presentation dcmPresentation;
    
    protected String scanSetUid; 
    
    protected FSNode directory=null; 
    
    protected List<DcmFileNode> files=null; //new ArrayList<FSNode>(); 

    private DcmSetProxyNode dcmSetNode;
    
    public DcmFilesNode(DcmSetProxyNode parent,VRL locator, String scanSetUid) throws ProxyException
    {
        super(parent.getProxyFactory(),parent,locator);
        this.scanSetUid=scanSetUid;
        this.dcmSetNode=parent; 
    }

    @Override
    protected List<? extends ProxyNode> doGetChilds(int offset, int range,LongHolder numChildsLeft) throws ProxyException
    {
        if (files==null)
            initFiles(); 
        return files;
    }

    protected void initFiles() throws ProxyException
    {
        List<FileDescriptor> fsNodes = this.dcmSetNode.getDicomDirScanner().getScanSet(this.scanSetUid).getFileDescriptors(); 
        List<DcmFileNode> nodes=new ArrayList<DcmFileNode>();
        
        for (FileDescriptor node:fsNodes)
        {
            if ((node==null) || (node.fsNode==null))
            {
                continue; // first files might be missing
            }
            
            DcmFileNode fileNode = creatFileNode(this,node.fsNode,node.fsNode.getBasename()); 
            nodes.add(fileNode);
            getProxyFactory().addNodeToHeap(fileNode);
        }

        this.files=nodes; 
    }
    
    private DcmFileNode creatFileNode(DcmFilesNode dcmFilesNode, FSNode node,String sliceName) throws ProxyException
    {
        this.getBaseVRL(); 
        DcmFileNode fileNode=new DcmFileNode(dcmSetNode, node, createSingleFileVRL(node.getPathname())); 
        fileNode.setName(sliceName);
        return fileNode; 
    }

    public VRL createSingleFileVRL(String filePath) throws ProxyException
    {

        try
        {
            return new VRL(getBaseVRL().toString() + "?files="+filePath);
        }
        catch (VRLSyntaxException e)
        {
            throw new ProxyException("Invalid VRL:" + id, e);
        }
    }
    
    protected DcmFilesNode resolve() throws ProxyException
    {
    	return this; 
    }
    
    
    @Override
    public String getIconURL(String status,int size) throws ProxyException
    {
        return null; 
    }
	
	protected boolean isResourceLink()
    {
	    return false; 
    }
	
	protected void debug(String msg)
	{
		System.err.println("VRSProxyNode:"+msg); 
	}
	
	public String toString()
	{
		return "<ProxyNode>"+locator.toString(); 
	}

	@Override
	protected String doGetMimeType() throws ProxyException
	{
	    return "application/dicom"; 
	}

	@Override
	protected boolean doGetIsComposite() throws ProxyException 
	{
	    return true; 
	}

    @Override
    protected List<String> doGetChildTypes() throws ProxyException
    {
        return new StringList("DCMFileNode");  
    }

	// ========================================================================
	// Misc 
	// ========================================================================
	
    @Override
    protected String doGetName()
    {   
        return "files"; 
    }
    
    @Override
    protected String doGetResourceType()
    {   
        return  ""+DicomSetType.DicomFilesNode; 
    }

    @Override
    protected String doGetResourceStatus() throws ProxyException
    {
        return "virtual"; 
    }

    @Override
    protected List<String> doGetAttributeNames() throws ProxyException
    {
        return super.doGetAttributeNames(); 
    }   

    @Override
    protected List<Attribute> doGetAttributes(List<String> names) throws ProxyException
    {
        return super.doGetAttributes(names); 
   }
    
    @Override
    protected Presentation doGetPresentation()
    {
        if (dcmPresentation==null)
        {
            dcmPresentation=new Presentation();
            StringList list=new StringList(); 

            list.add(DicomUtil.getTagName(Tag.InstanceNumber)); 
            list.add(DicomUtil.getTagName(Tag.PatientName)); 
            list.add(DicomUtil.getTagName(Tag.PatientID)); 
            list.add(DicomUtil.getTagName(Tag.PatientAge)); 
            // uid
            list.add(DicomUtil.getTagName(Tag.SeriesInstanceUID)); 
            list.add(DicomUtil.getTagName(Tag.SeriesNumber)); 
            
            dcmPresentation.setChildAttributeNames(list); 
        }
        
        return dcmPresentation; 
        
    }

    @Override
    protected DicomDirScanner getDicomDirScanner()
    {
        return this.parent.getDicomDirScanner(); 
    }

}
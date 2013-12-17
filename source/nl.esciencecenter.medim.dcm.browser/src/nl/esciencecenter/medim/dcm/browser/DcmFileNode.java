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

import org.dcm4che2.data.Tag;

import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.medim.dicom.DicomUtil;
import nl.esciencecenter.medim.dicom.DicomWrapper;
import nl.esciencecenter.ptk.data.LongHolder;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.io.FSNode;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyException;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyNode;
import nl.esciencecenter.vbrowser.vrs.data.Attribute;
import nl.esciencecenter.vbrowser.vrs.data.AttributeType;
import nl.esciencecenter.vbrowser.vrs.vrl.VRL;


/** 
 * Single Dicom File Node. 
 */
public class DcmFileNode extends DcmProxyNode
{
    private Presentation dcmPresentation;
    private String name; 
    private FSNode fsNode; 
    
    public DcmFileNode(DcmSetProxyNode parent,FSNode node,VRL vrl) throws ProxyException
    {
        super(parent.getProxyFactory(),parent,vrl);  
        this.fsNode=node; 
    }

    @Override
    protected List<ProxyNode> doGetChilds(int offset, int range,LongHolder numChildsLeft) throws ProxyException
    {
         return null;
    }
    
    protected DcmFileNode resolve() throws ProxyException
    {
    	return this; 
    }
    
    protected DcmFileNode[] createNodes(FSNode[] nodes) throws ProxyException
    {
    	if (nodes==null)
    		return null; 
    	
    	return null; 
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
	    return false; 
	}

    @Override
    protected List<String> doGetChildTypes() throws ProxyException
    {
        return new StringList("DCMNode"); 
    }

	// ========================================================================
	// Misc 
	// ========================================================================
	
    @Override
    protected String doGetResourceType()
    {   
        return "DicomFile"; 
    }

    @Override
    protected String doGetResourceStatus() throws ProxyException
    {
        return null; 
    }

    @Override
    protected List<String> doGetAttributeNames() throws ProxyException
    {
        
        try
        {
            List<String> list = DcmProxyUtil.getDicomTagNames(fsNode,true);
            if (list==null)
                return null;
            return list;
        }
        catch (Exception e)
        {
            throw new ProxyException("Error getting tag names from proxy node from:"+fsNode,e);  
        }
    }   


    @Override
    protected List<Attribute> doGetAttributes(List<String> names) throws ProxyException
    {
	        
	    try
        {
	        ArrayList<Attribute> attrs=new ArrayList<Attribute>(names.size()); 
	        DicomWrapper wrap = DicomWrapper.readFrom(fsNode.getURI()); 
	        
	        for (int i=0;i<names.size();i++)
	        {
	            if (DicomUtil.isTagField(names.get(i))==false)
	            {
                    attrs.add(new Attribute(AttributeType.STRING,names.get(i),"?")); 
	            }
	            else
	            {
	                String value=wrap.getValueAsString(DicomUtil.getTagField(names.get(i))); 
	                attrs.add(new Attribute(AttributeType.STRING,names.get(i),value));
	            }
	        }
	        
	        return attrs; 
        }
        catch (Exception e)
        {
            throw new ProxyException("Couldn't get attributes\n",e); 
        } 
   }
    
    @Override
    protected Presentation doGetPresentation()
    {
        if (dcmPresentation==null)
        {
            dcmPresentation=new Presentation();
            StringList list=new StringList(); 
            
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

    protected void setName(String sliceName)
    {
        name=sliceName;
    }
    
    public String getName()
    {
        return name;
    }

}
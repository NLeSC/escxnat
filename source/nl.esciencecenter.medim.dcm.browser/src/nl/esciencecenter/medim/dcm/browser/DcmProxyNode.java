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

import java.util.List;

import nl.esciencecenter.medim.dcm.browser.DcmProxyFactory.DicomSetType;
import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.ptk.data.StringList;
import nl.esciencecenter.ptk.presentation.Presentation;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyException;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyNode;
import nl.esciencecenter.vbrowser.vrs.data.Attribute;
import nl.esciencecenter.vbrowser.vrs.data.AttributeNames;
import nl.esciencecenter.vbrowser.vrs.vrl.VRL;

abstract public class DcmProxyNode extends ProxyNode
{
    protected DcmSetProxyNode parent; 
    
    protected DicomSetType setType;
    
    public DcmProxyNode(DcmProxyFactory factory, DcmSetProxyNode parent, VRL locator)
    {
        super(factory,locator);
        this.parent=parent; 
    }

    @Override
    protected void doPrefetchAttributes() throws ProxyException
    {
        super.doPrefetchAttributes(); 
    }
    
    public DicomSetType getDicomSetType()
    {
        return this.setType;
    }
    
    @Override
    public DcmProxyFactory getProxyFactory()
    {
        return (DcmProxyFactory)super.getProxyFactory(); 
    }
    
    @Override
    protected String doGetName() throws ProxyException
    {
        return this.getVRL().getBasename();
    }
    
    protected DcmProxyNode doGetParent()
    {
        return parent;  
    }

    @Override
    protected List<String> doGetAttributeNames() throws ProxyException 
    {
        return new StringList(AttributeNames.ATTR_NAME,AttributeNames.ATTR_RESOURCE_TYPE);
    }

    @Override
    protected List<Attribute> doGetAttributes(List<String> names) throws ProxyException 
    {
        return super.getDefaultProxyAttributes(names); 
    }

    @Override
    protected Presentation doGetPresentation() 
    {
        Presentation pres=Presentation.getPresentation(setType.toString(),true);
        try
        {
            // copy all: 
            pres.setChildAttributeNames(getAttributeNames());
            Presentation.storePresentation(setType.toString(), pres);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        
        return pres;
    }
    
    public VRL getBaseVRL()
    {
        VRL vri=this.getVRL(); 
        VRL baseVri = new VRL(vri.getScheme(),vri.getUserinfo(),vri.getHostname(),vri.getPort(),vri.getPath(),null,null); 
        // System.err.println(">>> VRL="+vri); 
        return baseVri;
    }

    // ==================
    // Abstract Interface 
    // ==================
    
    abstract protected DicomDirScanner getDicomDirScanner();
    
}


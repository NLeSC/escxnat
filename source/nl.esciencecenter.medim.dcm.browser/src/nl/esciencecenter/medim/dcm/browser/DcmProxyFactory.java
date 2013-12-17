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

import java.util.Hashtable;
import java.util.Map;

import nl.esciencecenter.ptk.data.StringHolder;
import nl.esciencecenter.ptk.util.StringUtil;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyException;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyFactory;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyNode;
import nl.esciencecenter.vbrowser.vrs.vrl.VRL;


/** 
 * VRS Proxy Factory for VRSProxyNodes.  
 */
public class DcmProxyFactory extends ProxyFactory
{
    private static ClassLogger logger; 
    
    static
    {
    	logger=ClassLogger.getLogger(DcmProxyFactory.class);
    	logger.setLevelToDebug();
    }
    
	private static DcmProxyFactory instance; 
    
    public static DcmProxyFactory getDefault() 
    {
        if (instance==null)
            instance=new DcmProxyFactory();
              
        return instance; 
   }
    
    public static enum DicomSetType
    {
        DicomDirectory,
        DicomSubject,
        DicomStudy,
        DicomScanSet, // ScanSet is series. Use this name to avoid study/series confusion.
        DicomFilesNode // Virtual node 
    };
    
    // ========================================================================
    // 
    // ========================================================================

    protected DcmProxyFactory()
    {
        super(); 
    }
    
//	public DCMProxyNode openLocation(VRI vrl) throws ProxyException
//	{
//		try 
//		{
//			return (DCMProxyNode)openLocation(new VRI(vrl.toURI()));
//		}
//		catch (Exception e) 
//		{
//			throw new ProxyException("Failed to open location:"+vrl+"\n"+e.getMessage(),e); 
//		} 
//	}
	
	// actual open location: 
	
    Map<String,DcmProxyNode> dcmNodes =new Hashtable<String,DcmProxyNode>(); 
	
    public DcmProxyNode doOpenLocation(VRL locator) throws ProxyException
    {
    	logger.infoPrintf(">>> doOpenLocation():%s <<<\n",locator);
    	
        try
        {
            DcmProxyNode node=dcmNodes.get(locator.toNormalizedString());
            
            if (node==null)
            {   
                node=new DcmSetProxyNode(this,null,locator);
                dcmNodes.put(locator.toNormalizedString(), node); 
            }
            
            return node; 
        }
        catch (Exception e)
        {
            throw new ProxyException("Failed to open location:"+locator+"\n"+e.getMessage(),e); 
        }
    }
    
    protected void addNodeToHeap(DcmProxyNode node)
    {
        this.dcmNodes.put(node.getVRL().toNormalizedString(),node); 
    }
    
    @Override
    public boolean canOpen(VRL locator,StringHolder reason) 
    {
		// internal scheme!
		if (StringUtil.equals("file",locator.getScheme())) 
			return true; 

		  // internal scheme!
        if (StringUtil.equals("dicom",locator.getScheme())) 
            return true; 

		return false;
	}


}

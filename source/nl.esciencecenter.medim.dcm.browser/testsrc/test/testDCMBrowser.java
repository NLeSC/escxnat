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
package test;

import nl.esciencecenter.medim.dcm.browser.DcmProxyFactory;
import nl.esciencecenter.medim.dicom.DicomDirScanner;
import nl.esciencecenter.ptk.GlobalProperties;
import nl.esciencecenter.ptk.util.logging.ClassLogger;
import nl.esciencecenter.ptk.vbrowser.ui.browser.BrowserPlatform;
import nl.esciencecenter.ptk.vbrowser.ui.browser.ProxyBrowser;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.ProxyNode;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.VirtualRootNode;
import nl.esciencecenter.ptk.vbrowser.ui.proxy.VirtualRootProxyFactory;

public class testDCMBrowser 
{
	public static void main(String args[])
	{
        
        ClassLogger.getLogger(DicomDirScanner.class).setLevelToDebug(); 

        ClassLogger.getRootLogger().setLevelToDebug(); 
        
		try 
		{
			BrowserPlatform platform=BrowserPlatform.getInstance(); 
		    
			DcmProxyFactory fac = DcmProxyFactory.getDefault();  
		    platform.registerProxyFactory(fac);  

		    VirtualRootProxyFactory rootFactory = new VirtualRootProxyFactory(); 
		    platform.registerProxyFactory(rootFactory);  

		    ProxyBrowser frame=(ProxyBrowser)platform.createBrowser(); 
		    
    		ProxyNode node = fac.openLocation("file:///"+GlobalProperties.getGlobalUserHome()+"/dicom/data"); 
    		
    		VirtualRootNode root =rootFactory.getRoot(); 
    		
    		root.addChild(node); 
    		
			frame.setRoot(root,true,false); 
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		
	}
}

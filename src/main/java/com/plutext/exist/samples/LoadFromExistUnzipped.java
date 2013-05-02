/*
 *  Copyright 2012, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */

package com.plutext.exist.samples;


import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.io3.Load3;
import org.docx4j.openpackaging.io3.Save;
import org.docx4j.openpackaging.io3.stores.PartStore;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.samples.AbstractSample;


/**
 * Example of loading an unzipped file from the file system.
 * 
 * @author jharrop
 * @since 3.0
 *
 */
public class LoadFromExistUnzipped extends AbstractSample {
	
	public static void main(String[] args) throws Exception {

		try {
			getInputFilePath(args);
		} catch (IllegalArgumentException e) {
	    	inputfilepath = System.getProperty("user.dir") + "/OUT";
		}		
		
		// Load the docx
		PartStore partLoader 
//			= new com.plutext.exist.xmldb.ExistUnzippedPartStore("/db/logdocx/t8", "admin", "");
			= new com.plutext.exist.webdav.ExistUnzippedPartStore("/db/logdocx/t8", "admin", "");
		final Load3 loader = new Load3(partLoader);
		OpcPackage opc = loader.get();
		
		System.out.println(
			XmlUtils.marshaltoString(
					((WordprocessingMLPackage)opc).getMainDocumentPart().getJaxbElement(),
					true, true)
				);
		
//		// Save it
//		ExistUnzippedPartStore zps = new ExistUnzippedPartStore("/db/OUT/sample-docx4", "admin", "");
//		zps.setSourcePartStore(opc.getPartStore());
//		
//		Save saver = new Save(opc, zps);
//		saver.save(null);
		
	}
		

}

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


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io3.Load3;
import org.docx4j.openpackaging.io3.Save;
import org.docx4j.openpackaging.io3.stores.PartStore;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.samples.AbstractSample;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;


/**
 * Example of saving a package to eXist zipped up as a binary blob,
 * using WebDAV
 * 
 * @author jharrop
 * @since 3.0
 *
 */
public class SaveToExistZippedViaWebDAV extends AbstractSample {
	
	public static void main(String[] args) throws Exception {

		// Load the docx from somewhere
		try {
			getInputFilePath(args);
		} catch (IllegalArgumentException e) {
	    	inputfilepath = System.getProperty("user.dir") + "/";
		}		
		
		OpcPackage opc = OpcPackage.load(new File(inputfilepath + "sample.docx")) ;
		
//		// Load the docx
//		PartStore partLoader 
////			= new com.plutext.exist.xmldb.ExistUnzippedPartStore("/db/zip1/report3", "admin", "admin");
//			= new com.plutext.exist.webdav.ExistUnzippedPartStore("/db/zip1/report3", "admin", "admin");
//		final Load3 loader = new Load3(partLoader);
//		OpcPackage opc = loader.get();
		
		// Now we have an OpcPackage...
		
		// Save it
		String url = "http://localhost:8080/exist/webdav/db/logdocx/tf.docx";
		long start = System.currentTimeMillis();

		
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 		
		Save saver = new Save(opc); 
		saver.save(baos);
		
		Sardine sardine = SardineFactory.begin();
		sardine.setCredentials("admin", "");		
		sardine.put(url, baos.toByteArray());
		sardine.shutdown();
				
		long elapsed = System.currentTimeMillis() - start;
		System.out.println(elapsed);
		
		
	}
		

}

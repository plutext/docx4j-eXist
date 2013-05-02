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


import java.io.InputStream;

import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.samples.AbstractSample;

import com.github.sardine.impl.SardineImpl;


/**
 * Example of loading a package from eXist stored zipped up as a binary blob,
 * using WebDAV
 * 
 * @author jharrop
 * @since 3.0
 *
 */
public class LoadFromExistZippedViaREST extends AbstractSample {
	
	public static void main(String[] args) throws Exception {

		// Load the docx from URL
		String url = "http://localhost:8080/exist/rest/db/logdocx/tf.docx";
		
		long start = System.currentTimeMillis();
				
		// SardineImpl gives us a convenient way to use
		// org.apache.http.client.methods.HttpGet
		
		SardineImpl sardine = new SardineImpl();
		sardine.setCredentials("admin", "");		
		InputStream is = sardine.get(url);
		
		OpcPackage opc = OpcPackage.load(is) ;
		System.out.println(opc.getClass().getName());
		
		sardine.shutdown();
				
		long elapsed = System.currentTimeMillis() - start;
		System.out.println(elapsed);
		
		
	}
		

}

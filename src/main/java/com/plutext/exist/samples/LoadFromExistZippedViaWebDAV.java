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


import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.samples.AbstractSample;
import org.exist.xmldb.EXistResource;
import org.exist.xmldb.RemoteBinaryResource;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;


/**
 * Example of loading a package from eXist stored zipped up as a binary blob,
 * using XML:DB
 * 
 * @author jharrop
 * @since 3.0
 *
 */
public class LoadFromExistZippedViaWebDAV extends AbstractSample {
	
	public static void main(String[] args) throws Exception {

		// Load the docx from URL
		String colStr = "xmldb:exist://localhost:8080/exist/xmlrpc/db/logdocx";
		String resource = "th.docx";
		
		long start = System.currentTimeMillis();
				
		String user = "admin";
		String password = "";

		final String driver = "org.exist.xmldb.DatabaseImpl"; 
		
		try { 
			// initialize database driver
			Class cl = Class.forName(driver);
			Database database = (Database) cl.newInstance();
			database.setProperty("create-database", "true");
			DatabaseManager.registerDatabase(database);
		} catch (Exception e) {
			throw new Docx4JException(e.getMessage(), e);
		} 
		
		// For simplicity, here we assume the collection exists
		Collection col = DatabaseManager.getCollection(colStr, user, password);
		Resource res = col.getResource(resource);
		// .. and that the resource really is a RemoteBinaryResource
		OpcPackage opc = OpcPackage.load(
				((RemoteBinaryResource)res).getStreamContent()) ;

		System.out.println(opc.getClass().getName());
		
		// Cleanup
		((EXistResource) res).freeResources();
		col.close();
				
		long elapsed = System.currentTimeMillis() - start;
		System.out.println(elapsed);
		
		
	}
		

}

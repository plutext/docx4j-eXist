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
package com.plutext.exist;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io3.Load3;
import org.docx4j.openpackaging.io3.stores.PartStore;
import org.docx4j.openpackaging.io3.stores.ZipPartStore.ByteArray;
import org.docx4j.openpackaging.parts.CustomXmlDataStoragePart;
import org.docx4j.openpackaging.parts.JaxbXmlPart;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.XmlPart;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart;
import org.w3c.dom.Document;
import org.w3c.dom.Node;


import org.xmldb.api.base.*; 
import org.xmldb.api.modules.*; 
import org.xmldb.api.*; 
import javax.xml.transform.OutputKeys; 
import org.exist.xmldb.EXistResource; 
import org.exist.xmldb.RemoteBinaryResource;

/**
 * Load an unzipped package from the file system;
 * save it to some output stream.
 * 
 * TODO convert path sep in part name to suit
 * underlying file system.
 * 
 * @author jharrop
 * @since 3.0
 */
public class ExistUnzippedPartStore implements PartStore {
	
	private static Logger log = Logger.getLogger(ExistUnzippedPartStore.class);
	
	private static String URI = "xmldb:exist://localhost:8080/exist/xmlrpc";
		
	String docxColl;  // eg "/db/docx4/apple"
	
	String user;
	String password;
	
	public ExistUnzippedPartStore(String docxColl, String user, String password) throws Docx4JException {
		
		this.docxColl = docxColl;
		this.user = user;
		this.password = password;

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

	}

	private PartStore sourcePartStore;	

	/**
	 * Set this if its different to the target part store
	 * (ie this object)
	 */
	public void setSourcePartStore(PartStore partStore) {
		this.sourcePartStore = partStore;
	}
	
	/////// Load methods

	public boolean partExists(String partName) {
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = partName;
		}
		System.out.println(URI + docxColl + partPrefix);
		//String urlEncoded = URLEncoder.encode(URI + docxColl + partPrefix);
		System.out.println(resource);
				
		Collection col = null;
		Resource res = null;
		try { // get the collection
			col = DatabaseManager.getCollection(URI + docxColl + partPrefix, user, password);
			
			if (col==null) {
				log.debug("No col: " + URI + docxColl + partPrefix);
				return false;
			}
			
			col.setProperty(OutputKeys.INDENT, "no");
			
			res = col.getResource(resource);
			return (res != null);
		} catch (XMLDBException e) {
			// TODO Auto-generated catch block
			System.out.println("error getCollection:" + URI + docxColl + partPrefix);
			e.printStackTrace();
			return false;
		} finally { // dont forget to clean up!
			if (res != null) {
				try {
					((EXistResource) res).freeResources();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
			if (col != null) {
				try {
					col.close();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
		}
	}
	
	public InputStream loadPart(String partName) throws IOException {
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = URLEncoder.encode(partName);
		}
		System.out.println(URI + docxColl + partPrefix);
		System.out.println(resource);
				
		Collection col = null;
		Resource res = null;
		try { // get the collection
			col = DatabaseManager.getCollection(URI + docxColl + partPrefix, user, password);
			col.setProperty(OutputKeys.INDENT, "no");
			
			res = col.getResource(resource);
			
			// The XML DB API doesn't give you a way 
			// to stream a resource?!
			
			if (res==null) {

				log.error(resource + " doesn't exist!" );
				return null;
			
			} else if (res instanceof XMLResource) {
				Node n = ((XMLResource)res).getContentAsDOM();
				DOMSource source = new DOMSource(n);  
				StringWriter xmlAsWriter = new StringWriter();  
				  
				StreamResult result = new StreamResult(xmlAsWriter);  
				  
				TransformerFactory.newInstance().newTransformer().transform(source, result);  
				  
				// write changes  
				return new ByteArrayInputStream(xmlAsWriter.toString().getBytes("UTF-8")); 
			} else if (res instanceof RemoteBinaryResource) {
				return ((RemoteBinaryResource)res).getStreamContent();
			}else {
				log.error("Handle " + res.getClass().getName() );
				return null;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} finally { // dont forget to clean up!
			if (res != null) {
				try {
					((EXistResource) res).freeResources();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
			if (col != null) {
				try {
					col.close();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
		}
		
		
	}

	///// Save methods
	
	/**
	 * Does nothing 
	 */
	public void setOutputStream(OutputStream os) {
		// Nothing to do
	}

	File dir;
	
	public void saveContentTypes(ContentTypeManager ctm) throws Docx4JException {
		
		try {
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	        ctm.marshal(baos);
	        
	        setContents(docxColl, 
	        		URLEncoder.encode("[Content_Types].xml"), 
	        		"XMLResource", 
	        		baos.toString() );	        
	        
		} catch (Exception e) {
			throw new Docx4JException("Error marshalling Content_Types ", e);
		}
	
	}
	
	public void saveJaxbXmlPart(JaxbXmlPart part) throws Docx4JException {

		String partName;
		if (part.getPartName().getName().equals("_rels/.rels")) {
			partName = part.getPartName().getName();			
		} else {
			partName = part.getPartName().getName().substring(1);						
		}
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = partName;
		}
		System.out.println(docxColl + partPrefix);
		System.out.println(resource);
		
		try {


			if (part.isUnmarshalled() ) {
			
				ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		        part.marshal(baos);

		        setContents(docxColl + partPrefix, 
		        		URLEncoder.encode(resource), 
		        		"XMLResource", 
		        		baos.toString() );	        
		        
	        } else {

	        	if (!partExists(partName)
	        			&& this.sourcePartStore==null) {
	        		throw new Docx4JException("part store has changed, and sourcePartStore not set");
	        	} else {
	        		InputStream is = sourcePartStore.loadPart(partName);
	        		
					ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	        		
	        		int read = 0;
	        		byte[] bytes = new byte[1024];
	        	 
	        		while ((read = is.read(bytes)) != -1) {
	        			baos.write(bytes, 0, read);
	        		}
	        	 
	        		is.close();
	        		
			        setContents(docxColl + partPrefix, 
			        		URLEncoder.encode(resource), 
			        		"XMLResource", 
			        		baos.toString() );	        
	        	 	        		
	        	}
	        	
	        }
	        	        
		} catch (Exception e) {
			throw new Docx4JException("Error marshalling JaxbXmlPart " + part.getPartName(), e);
		}
	}
	
	public void saveCustomXmlDataStoragePart(CustomXmlDataStoragePart part) throws Docx4JException {
		
		String partName = part.getPartName().getName().substring(1);
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = partName;
		}
		System.out.println(docxColl + partPrefix);
		System.out.println(resource);
		
		try {
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
	        part.getData().writeDocument( baos );

	        setContents(docxColl + partPrefix, 
	        		URLEncoder.encode(resource), 
	        		"XMLResource", 
	        		baos.toString() );	        
		        
	        	        
		} catch (Exception e) {
			throw new Docx4JException("Error marshalling JaxbXmlPart " + part.getPartName(), e);
		}
	}
	
	public void saveXmlPart(XmlPart part) throws Docx4JException {
	
		String partName = part.getPartName().getName().substring(1);
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = partName;
		}
		System.out.println(docxColl + partPrefix);
		System.out.println(resource);
		
		try {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Document doc = part.getDocument();

			/*
			 * With Crimson, this gives:
			 * 
				Exception in thread "main" java.lang.AbstractMethodError: org.apache.crimson.tree.XmlDocument.getXmlStandalone()Z
					at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.setDocumentInfo(DOM2TO.java:373)
					at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.parse(DOM2TO.java:127)
					at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.parse(DOM2TO.java:94)
					at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:662)
					at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:708)
					at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transform(TransformerImpl.java:313)
					at org.docx4j.model.datastorage.CustomXmlDataStorageImpl.writeDocument(CustomXmlDataStorageImpl.java:174)
			 * 
			 */
			DOMSource source = new DOMSource(doc);
			XmlUtils.getTransformerFactory().newTransformer()
					.transform(source, new StreamResult(baos));

			setContents(docxColl + partPrefix,
					URLEncoder.encode(resource), "XMLResource", baos.toString());

		} catch (Exception e) {
			throw new Docx4JException("Error marshalling JaxbXmlPart "
					+ part.getPartName(), e);
		}
	}
	
	public void saveBinaryPart(Part part) throws Docx4JException {
		
		// Drop the leading '/'
		String partName = part.getPartName().getName().substring(1);
		
		String partPrefix="";
		String resource; 
		int pos = partName.lastIndexOf("/");
		if (pos>-1) {
			partPrefix = "/" + partName.substring(0, pos);
			resource = partName.substring(pos+1);
		} else {
			resource = partName;
		}
		System.out.println(docxColl + partPrefix);
		System.out.println(resource);
		

		try {
	        	        
	        if (((BinaryPart)part).isLoaded() ) {
			
	            java.nio.ByteBuffer bb = ((BinaryPart)part).getBuffer();
	            byte[] bytes = null;
	            bytes = new byte[bb.limit()];
	            bb.get(bytes);	        
		        
		        setContents(docxColl + partPrefix, 
		        		URLEncoder.encode(resource), 
		        		"BinaryResource", 
		        		bytes, part.getContentType() );	        
		        
	        } else {
		        
	        	boolean partExists = partExists(partName);
	        	if (!partExists
	        			&& this.sourcePartStore==null) {
	        		
	        		throw new Docx4JException("part store has changed, and sourcePartStore not set");
	        		
	        	} else if (partExists
	        			&& this.sourcePartStore==this) {
	        		
		        	// No need to save

	        	} else {
	    			ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        		
	        		InputStream is = sourcePartStore.loadPart(part.getPartName().getName().substring(1));
	        		int read = 0;
	        		byte[] bytes = new byte[1024];
	        		
	        		while ((read = is.read(bytes)) != -1) {
	        			baos.write(bytes, 0, read);
	        		}
	        		is.close();
	        		
			        setContents(docxColl + partPrefix, 
			        		URLEncoder.encode(resource), 
			        		"BinaryResource", 
			        		baos.toByteArray(), part.getContentType() );	        

	        	}
	        }

			
		} catch (Exception e ) {
			throw new Docx4JException("Failed to put binary part", e);			
		}
		
		log.info( "success writing part: " + partName);		
		
	}
	
	/**
	 * Does nothing
	 */
	public void finishSave() throws Docx4JException {
		// Nothing to do
	}

	public void setContents(String colString, String id, String type, Object contents) throws XMLDBException {
		setContents( colString,  id,  type,  contents, null);
	}
	
	public void setContents(String colString, String id, String type, Object contents, String mimetype) throws XMLDBException {
		
		// type eg "XMLResource"
		
		Collection col = null;
		Resource res = null;
		try {
			col = getOrCreateCollection(colString);

			//res = (XMLResource) col.createResource(id, type);
			res = getOrCreateResource( col,  id,  type);
			res.setContent(contents);
			if (type.equals("BinaryResource")) {
				((RemoteBinaryResource)res).setMimeType(mimetype);
			}
			col.storeResource(res);
			
		} finally { // dont forget to cleanup
			if (res != null) {
				try {
					((EXistResource) res).freeResources();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
			if (col != null) {
				try {
					col.close();
				} catch (XMLDBException xe) {
					xe.printStackTrace();
				}
			}
		}
	}

	private  Resource getOrCreateResource(Collection col, String id, String type)
			throws XMLDBException {
		
		Resource res = null;
		try { // get the collection
			res = col.getResource(id);
			
			if (res != null) {
				return res;
			} else {
				return col.createResource(id, type);
			}
		} catch (XMLDBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} 
	}
	
	private  Collection getOrCreateCollection(String collectionUri)
			throws XMLDBException {
		return getOrCreateCollection(collectionUri, 0);
	}

	private  Collection getOrCreateCollection(String collectionUri,
			int pathSegmentOffset) throws XMLDBException {
		Collection col = DatabaseManager.getCollection(URI + collectionUri, user, password);
		if (col == null) {
			if (collectionUri.startsWith("/")) {
				collectionUri = collectionUri.substring(1);
			}
			String pathSegments[] = collectionUri.split("/");
			if (pathSegments.length > 0) {
				StringBuilder path = new StringBuilder();
				for (int i = 0; i <= pathSegmentOffset; i++) {
					path.append("/" + pathSegments[i]);
				}
				Collection start = DatabaseManager.getCollection(URI + path);
				if (start == null) {
					// collection does not exist, so create
					String parentPath = path
							.substring(0, path.lastIndexOf("/"));
					Collection parent = DatabaseManager.getCollection(URI
							+ parentPath, user, password);
					CollectionManagementService mgt = (CollectionManagementService) parent
							.getService("CollectionManagementService", "1.0");
					System.out.println("creating " + pathSegments[pathSegmentOffset]);
					col = mgt.createCollection(pathSegments[pathSegmentOffset]);
					
					/*
						2013-01-10 18:44:06,056 [eXistThread-41] ERROR (NativeBroker.java [getOrCreateCollection]:751) - Permission denied to create collection '/db/docxOUT' 
						2013-01-10 18:44:06,057 [eXistThread-41] DEBUG (RpcConnection.java [handleException]:123) - Account 'guest' not allowed to write to collection '/db' 
						org.exist.security.PermissionDeniedException: Account 'guest' not allowed to write to collection '/db'
							at org.exist.storage.NativeBroker.getOrCreateCollection(NativeBroker.java:752)					 
							*/
					
					
					col.close();
					parent.close();
				} else {
					start.close();
				}
			}
			return getOrCreateCollection(collectionUri, ++pathSegmentOffset);
		} else {
			return col;
		}
	}
}

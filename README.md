docx4j-eXist
============

docx4j integration with eXist 2.0 proof of concept

This demonstrates saving and loading a docx (or pptx or xlsx):

- in exploded (unzipped) form (so that eXist's XML power can be brought to 
bear on it)

- as a binary blob


APPROACHES

Saving/Loading the unzipped docx is demonstrated via the following APIs:

- XML:DB

- WebDAV (using Sardine client, since that's much nicer than the Milton client)

- CMIS: It would be nice to demonstrate via CMIS, but eXist does not support that.

- REST: looks like this could be done.  See http://exist-db.org/exist/apps/doc/devguide_rest.xml


STORING DOCUMENTS UNZIPPED IN EXIST

How do you get to have docx stored in eXist in unzipped format?

You can either use docx4j to create a docx or load a docx from somewhere else (eg file system),
and then use this project to save it unzipped to eXist.

That's the easiest. But you can also use Joe Wiz's friendly Unzip XQuery module. That's
handy if some other process is storing the files to eXist, or if this project seems buggy.

1. Follow the instructions at https://github.com/joewiz/unzip to install that into your
eXist 2.0

   Tips 1: Install functx using package name 'http://www.functx.com'
   Tips 2: Unzip 0.1.3 xar can be found pre-built, in source/main/exist/

2. Add the trigger from source/main/exist/collection.xconf for a collection of your choice

3. Upload a docx into that collection; if successful you should see it unzipped in eXist 


STATUS

WebDAV
- Load and Save both work
- but note http://exist.2174344.n4.nabble.com/RC2-s-WebDAV-404-s-on-character-td4657976.html

XML:DB
- Load and Save both work
- xmldb/ExistUnzippedPartStore.java writes %5BContent_Types%5D.xml which is what we want...






 
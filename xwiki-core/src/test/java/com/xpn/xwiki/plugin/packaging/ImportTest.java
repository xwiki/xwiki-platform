/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.xpn.xwiki.plugin.packaging;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.stub.VoidStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.store.XWikiHibernateRecycleBinStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

public class ImportTest extends AbstractBridgedXWikiComponentTestCase
{
    private Package pack;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockRecycleBinStore;

    private Mock mockXWikiVersioningStore;

    private Mock mockRightService;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.pack = new Package();
        this.xwiki = new XWiki();
        getContext().setWiki(this.xwiki);
        this.xwiki.setConfig(new XWikiConfig());
        this.xwiki.setNotificationManager(new XWikiNotificationManager());

        // mock a store that would also handle translations
        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);
                    String documentKey = shallowDoc.getFullName();
                    if (!shallowDoc.getLanguage().equals("")) {
                        documentKey += "." + shallowDoc.getLanguage();
                    }
                    if (docs.containsKey(documentKey)) {
                        return docs.get(documentKey);
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    // if this is a translated document, append a language prefix
                    String documentKey = document.getFullName();
                    if (!document.getLanguage().equals("")) {
                        documentKey += "." + document.getLanguage();
                    }
                    docs.put(documentKey, document);
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("deleteXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.deleteXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    // delete the document from the map
                    String documentKey = document.getFullName();
                    if (!document.getLanguage().equals("")) {
                        documentKey += "." + document.getLanguage();
                    }
                    docs.remove(documentKey);
                    return null;
                }
            });
        this.mockXWikiStore.stubs().method("getTranslationList").will(
            new CustomStub("Implements XWikiStoreInterface.getTranslationList")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    // search for this document in the map and return it's translations
                    List translationList = new ArrayList();
                    for (Iterator pairsIt = docs.entrySet().iterator(); pairsIt.hasNext();) {
                        Map.Entry currentEntry = (Map.Entry) pairsIt.next();
                        if (((String) currentEntry.getKey()).startsWith(document.getFullName())
                            && !((XWikiDocument) currentEntry.getValue()).getLanguage().equals("")) {
                            // yeeey, it's a translation
                            translationList.add(((XWikiDocument) currentEntry.getValue()).getLanguage());
                        }
                    }
                    return translationList;
                }
            });
        this.mockXWikiStore.stubs().method("injectCustomMapping").will(returnValue(false));

        this.mockRecycleBinStore =
            mock(XWikiHibernateRecycleBinStore.class, new Class[] {XWikiContext.class}, new Object[] {getContext()});
        this.mockRecycleBinStore.stubs().method("saveToRecycleBin").will(VoidStub.INSTANCE);

        this.mockXWikiVersioningStore =
            mock(XWikiHibernateVersioningStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {
            this.xwiki, getContext()});
        this.mockXWikiVersioningStore.stubs().method("getXWikiDocumentArchive").will(returnValue(null));
        this.mockXWikiVersioningStore.stubs().method("resetRCSArchive").will(returnValue(null));

        this.xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        this.xwiki.setRecycleBinStore((XWikiHibernateRecycleBinStore) this.mockRecycleBinStore.proxy());
        this.xwiki.setVersioningStore((XWikiVersioningStoreInterface) mockXWikiVersioningStore.proxy());

        // mock the right service
        this.mockRightService = mock(XWikiRightService.class);
        this.mockRightService.stubs().method("checkAccess").will(returnValue(true));
        this.mockRightService.stubs().method("hasAdminRights").will(returnValue(true));
        this.mockRightService.stubs().method("hasProgrammingRights").will(returnValue(true));
        this.xwiki.setRightService((XWikiRightService) this.mockRightService.proxy());
    }

    /**
     * Test the regular document import.
     * 
     * @throws Exception
     */
    public void testImportDocument() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument("Test", "DocImport");
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] {doc1}, new String[] {"ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument = this.xwiki.getDocument("Test.DocImport", getContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki.getDocument("Test.DocImportNonexisting", getContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument("Test", "DocImport");
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("en");
        this.xwiki.saveDocument(doc1Translation, getContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the import with document overwrite.
     * 
     * @throws Exception
     */
    public void testImportOverwriteDocument() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument("Test", "DocImportOverwrite");
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] {doc1}, new String[] {"ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument = this.xwiki.getDocument("Test.DocImportOverwrite", getContext());
        assertFalse(foundDocument.isNew());

        // create the overwriting document
        String newContent = "This is new content";
        XWikiDocument overwritingDoc = new XWikiDocument("Test", "DocImportOverwrite");
        overwritingDoc.setContent(newContent);

        zipFile = this.createZipFile(new XWikiDocument[] {overwritingDoc}, new String[] {"ISO-8859-1"});

        // use a new packager because we need to clean-up import data (files list, doucument data)
        this.pack = new Package();
        // import and install
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if the document is there
        XWikiDocument foundOverwritingDoc = this.xwiki.getDocument("Test.DocImportOverwrite", getContext());
        assertFalse(foundOverwritingDoc.isNew());
        assertEquals(foundOverwritingDoc.getContent(), newContent);
    }

    /**
     * Test the import of translation files, with overwrite.
     * 
     * @throws Exception
     */
    public void testImportTranslationsOverwrite() throws Exception
    {
        XWikiDocument original = new XWikiDocument("Test", "DocTranslation");
        original.setDefaultLanguage("en");
        original.setTranslation(0);
        XWikiDocument translation = new XWikiDocument("Test", "DocTranslation");
        translation.setLanguage("fr");
        translation.setDefaultLanguage("en");
        translation.setTranslation(1);
        translation.setOriginalDocument(original);

        // import and install those twice with tests
        byte[] zipFile =
            this.createZipFile(new XWikiDocument[] {original, translation}, new String[] {"ISO-8859-1", "ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());
        XWikiDocument foundDocument = this.xwiki.getDocument("Test.DocTranslation", getContext());
        assertFalse(foundDocument.isNew());
        // get the translation
        XWikiDocument translationDoc = foundDocument.getTranslatedDocument("fr", getContext());
        assertFalse(translationDoc.isNew());

        // use a new packager because we need to clean-up import data (files list, doucument data)
        this.pack = new Package();
        // import again and do the same tests
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());
        foundDocument = this.xwiki.getDocument("Test.DocTranslation", getContext());
        // might not be the best method to test the document is in the store though...
        assertFalse(foundDocument.isNew());
        // get the translation
        translationDoc = foundDocument.getTranslatedDocument("fr", getContext());
        assertFalse(translationDoc.isNew());
    }

    private String getPackageXML(XWikiDocument docs[])
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
        sb.append("<package>\n").append("<infos>\n").append("<name>Backup</name>\n");
        sb.append("<description>on Mon Jan 01 01:44:32 CET 2007 by XWiki.Admin</description>\n");
        sb.append("<licence></licence>\n");
        sb.append("<author>XWiki.Admin</author>\n");
        sb.append("<version></version>\n");
        sb.append("<backupPack>true</backupPack>\n");
        sb.append("</infos>\n");
        sb.append("<files>\n");
        for (int i = 0; i < docs.length; i++) {

            sb.append("<file defaultAction=\"0\" language=\"" + docs[i].getLanguage() + "\">" + docs[i].getFullName()
                + "</file>\n");
        }
        sb.append("</files></package>\n");
        return sb.toString();
    }

    private byte[] getEncodedByteArray(String content, String charset) throws IOException
    {
        StringReader rdr = new StringReader(content);
        BufferedReader bfr = new BufferedReader(rdr);
        ByteArrayOutputStream ostr = new ByteArrayOutputStream();
        OutputStreamWriter os = new OutputStreamWriter(ostr, charset);

        // Voluntarily ignore the first line... as it's the xml declaration
        String line = bfr.readLine();
        os.append("<?xml version=\"1.0\" encoding=\"" + charset + "\"?>\n");

        line = bfr.readLine();
        while (null != line) {
            os.append(line);
            os.append("\n");
            line = bfr.readLine();
        }
        os.flush();
        os.close();
        return ostr.toByteArray();
    }

    private byte[] createZipFile(XWikiDocument docs[], String[] encodings) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry zipp = new ZipEntry("package.xml");
        zos.putNextEntry(zipp);
        zos.write(getEncodedByteArray(getPackageXML(docs), "ISO-8859-1"));
        for (int i = 0; i < docs.length; i++) {
            String zipEntryName = docs[i].getSpace() + "/" + docs[i].getName();
            if (docs[i].getTranslation() != 0) {
                zipEntryName += "." + docs[i].getLanguage();
            }
            ZipEntry zipe = new ZipEntry(zipEntryName);
            zos.putNextEntry(zipe);
            String xmlCode = docs[i].toXML(false, false, false, false, getContext());
            zos.write(getEncodedByteArray(xmlCode, encodings[i]));
        }
        zos.closeEntry();
        return baos.toByteArray();
    }
}

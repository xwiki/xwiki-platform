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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.stub.VoidStub;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateRecycleBinStore;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateVersioningStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;

public class ImportTest extends AbstractPackageTest
{
    private Package pack;

    private XWiki xwiki;

    private Mock mockXWikiStore;

    private Mock mockRecycleBinStore;

    private Mock mockXWikiVersioningStore;

    private Mock mockRightService;

    private Map<String, XWikiDocument> docs = new HashMap<String, XWikiDocument>();

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.pack = new Package();
        this.xwiki = new XWiki();
        getContext().setWiki(this.xwiki);
        this.xwiki.setConfig(new XWikiConfig());

        // mock a store that would also handle translations
        this.mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class}, new Object[] {this.xwiki,
            getContext()});
        this.mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                @Override
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
                @Override
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
                @Override
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
                @Override
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
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("Test", "Test", "DocImport"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] {doc1}, new String[] {"ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImport"), getContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportNonexisting"), getContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(new DocumentReference("Test", "Test", "DocImport"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("en");
        this.xwiki.saveDocument(doc1Translation, getContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the regular document import with non-ascii document title.
     * 
     * @throws Exception
     */
    public void testImportDocumentNonAsciiTitle() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1.setDefaultLanguage("zh");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] {doc1}, new String[] {"UTF-8"}, "UTF-8");

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"), getContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportNonexisting"), getContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("zh");
        this.xwiki.saveDocument(doc1Translation, getContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }


    /**
     * Test the regular document import with non-ascii document title and non-utf8 platform encoding.
     * 
     * @throws Exception
     */
    public void testImportDocumentNonAsciiTitleNonUtf8PlatformEncoding() throws Exception
    {
        String oldEncoding = System.getProperty("file.encoding");
        System.setProperty("file.encoding", "ISO-8859-1");

        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1.setDefaultLanguage("zh");

        byte[] zipFile = this.createZipFileUsingCommonsCompress(new XWikiDocument[] {doc1}, new String[] {"UTF-8"}, "UTF-8");

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();

        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        System.setProperty("file.encoding", oldEncoding);

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"), getContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportNonexisting"), getContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(new DocumentReference("Test", "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("zh");
        this.xwiki.saveDocument(doc1Translation, getContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the regular document import.  Test XAR file built with commons compress.
     * 
     * @throws Exception
     */
    public void testImportDocumentXarCreatedByCommonsCompress() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("Test", "Test", "DocImport"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFileUsingCommonsCompress(new XWikiDocument[] {doc1}, new String[] {"ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImport"), getContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportNonexisting"), getContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument = foundDocument.getTranslatedDocument("fr", getContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(new DocumentReference("Test", "Test", "DocImport"));
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
        XWikiDocument doc1 = new XWikiDocument(new DocumentReference("Test", "Test", "DocImportOverwrite"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] {doc1}, new String[] {"ISO-8859-1"});

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportOverwrite"), getContext());
        assertFalse(foundDocument.isNew());

        // create the overwriting document
        String newContent = "This is new content";
        XWikiDocument overwritingDoc = new XWikiDocument(new DocumentReference("Test", "Test", "DocImportOverwrite"));
        overwritingDoc.setContent(newContent);

        zipFile = this.createZipFile(new XWikiDocument[] {overwritingDoc}, new String[] {"ISO-8859-1"});

        // use a new packager because we need to clean-up import data (files list, doucument data)
        this.pack = new Package();
        // import and install
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());

        // check if the document is there
        XWikiDocument foundOverwritingDoc =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocImportOverwrite"), getContext());
        assertFalse(foundOverwritingDoc.isNew());
        assertEquals(foundOverwritingDoc.getContent(), newContent);
        assertEquals(foundDocument, foundOverwritingDoc.getOriginalDocument());
    }

    /**
     * Test the import of translation files, with overwrite.
     * 
     * @throws Exception
     */
    public void testImportTranslationsOverwrite() throws Exception
    {
        XWikiDocument original = new XWikiDocument(new DocumentReference("Test", "Test", "DocTranslation"));
        original.setDefaultLanguage("en");
        original.setTranslation(0);
        XWikiDocument translation = new XWikiDocument(new DocumentReference("Test", "Test", "DocTranslation"));
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
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocTranslation"), getContext());
        assertFalse(foundDocument.isNew());
        // get the translation
        XWikiDocument translationDoc = foundDocument.getTranslatedDocument("fr", getContext());
        assertFalse(translationDoc.isNew());

        // use a new packager because we need to clean-up import data (files list, doucument data)
        this.pack = new Package();
        // import again and do the same tests
        this.pack.Import(zipFile, getContext());
        this.pack.install(getContext());
        foundDocument = this.xwiki.getDocument(new DocumentReference("Test", "Test", "DocTranslation"), getContext());
        // might not be the best method to test the document is in the store though...
        assertFalse(foundDocument.isNew());
        // get the translation
        translationDoc = foundDocument.getTranslatedDocument("fr", getContext());
        assertFalse(translationDoc.isNew());
    }

}

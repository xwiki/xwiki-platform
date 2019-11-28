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

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.event.ExtensionInstalledEvent;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.internal.local.DefaultLocalExtension;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ImportTest extends AbstractPackageTest
{
    LocalizationContext localizationContext;

    private Package pack;

    private XWiki xwiki;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.xwiki = this.oldcore.getSpyXWiki();

        this.localizationContext = this.oldcore.getMocker().registerMockComponent(LocalizationContext.class);

        this.pack = new Package();

        when(localizationContext.getCurrentLocale()).thenReturn(Locale.ROOT);

        // mock the right service
        doReturn(true).when(this.oldcore.getMockRightService()).checkAccess(any(), any(), any());
        doReturn(true).when(this.oldcore.getMockRightService()).hasWikiAdminRights(any());
        doReturn(true).when(this.oldcore.getMockRightService()).hasProgrammingRights(any());
    }

    /**
     * Test the regular document import.
     * 
     * @throws Exception
     */
    @Test
    public void testImportDocument() throws Exception
    {
        XWikiDocument doc1 =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] { doc1 }, new String[] { "ISO-8859-1" }, null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new LocalDocumentReference("Test", "DocImport"), this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportNonexisting"), this.oldcore.getXWikiContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument =
            foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("en");
        this.xwiki.saveDocument(doc1Translation, this.oldcore.getXWikiContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the regular document import when the XAR is tagged as extension.
     * 
     * @throws Exception
     */
    @Test
    public void testImportExtension() throws Exception
    {
        ExtensionId extensionId = new ExtensionId("test", "1.0");

        XWikiDocument doc1 =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] { doc1 }, new String[] { "ISO-8859-1" }, extensionId);

        // Store the extension in the local repository
        DefaultLocalExtension localExtension = new DefaultLocalExtension(null, extensionId, "xar");
        File file = File.createTempFile("temp", ".xar");
        FileUtils.writeByteArrayToFile(file, zipFile);
        localExtension.setFile(file);
        LocalExtensionRepository localeRepository =
            this.oldcore.getMocker().getInstance(LocalExtensionRepository.class);
        localeRepository.storeExtension(localExtension);

        // Listen to extension installed event
        EventListener extensionListener = mock(EventListener.class);
        when(extensionListener.getEvents()).thenReturn(Arrays.asList(new ExtensionInstalledEvent()));
        when(extensionListener.getName()).thenReturn("extension installed listener");
        ObservationManager observationManager = this.oldcore.getMocker().getInstance(ObservationManager.class);
        observationManager.addListener(extensionListener);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new LocalDocumentReference("Test", "DocImport"), this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportNonexisting"), this.oldcore.getXWikiContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument =
            foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("en");
        this.xwiki.saveDocument(doc1Translation, this.oldcore.getXWikiContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertNotSame(foundDocument, foundTranslationDocument);

        // Check that the extension has been registered
        InstalledExtensionRepository installedExtensionRepository =
            this.oldcore.getMocker().getInstance(InstalledExtensionRepository.class);
        assertNotNull(installedExtensionRepository.getInstalledExtension(extensionId));
        assertNotNull(installedExtensionRepository.getInstalledExtension(extensionId.getId(),
            "wiki:" + this.oldcore.getXWikiContext().getWikiId()));

        verify(extensionListener).onEvent(any(), any(), any());
    }

    /**
     * Test the regular document import with non-ascii document title.
     * 
     * @throws Exception
     */
    @Test
    public void testImportDocumentNonAsciiTitle() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1.setDefaultLanguage("zh");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] { doc1 }, new String[] { "UTF-8" }, "UTF-8", null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if it is there
        XWikiDocument foundDocument = this.xwiki.getDocument(
            new LocalDocumentReference("Test", "\u60A8\u597D\u4E16\u754C"), this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportNonexisting"), this.oldcore.getXWikiContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument =
            foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("zh");
        this.xwiki.saveDocument(doc1Translation, this.oldcore.getXWikiContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the regular document import with non-ascii document title and non-utf8 platform encoding.
     * 
     * @throws Exception
     */
    @Test
    public void testImportDocumentNonAsciiTitleNonUtf8PlatformEncoding() throws Exception
    {
        String oldEncoding = System.getProperty("file.encoding");
        System.setProperty("file.encoding", "ISO-8859-1");

        XWikiDocument doc1 = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1.setDefaultLanguage("zh");

        byte[] zipFile = this.createZipFileUsingCommonsCompress(new XWikiDocument[] { doc1 }, new String[] { "UTF-8" },
            "UTF-8", null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();

        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        System.setProperty("file.encoding", oldEncoding);

        // check if it is there
        XWikiDocument foundDocument = this.xwiki.getDocument(
            new LocalDocumentReference("Test", "\u60A8\u597D\u4E16\u754C"), this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportNonexisting"), this.oldcore.getXWikiContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument =
            foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "\u60A8\u597D\u4E16\u754C"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("zh");
        this.xwiki.saveDocument(doc1Translation, this.oldcore.getXWikiContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the regular document import. Test XAR file built with commons compress.
     * 
     * @throws Exception
     */
    @Test
    public void testImportDocumentXarCreatedByCommonsCompress() throws Exception
    {
        XWikiDocument doc1 =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile =
            this.createZipFileUsingCommonsCompress(new XWikiDocument[] { doc1 }, new String[] { "ISO-8859-1" }, null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if it is there
        XWikiDocument foundDocument =
            this.xwiki.getDocument(new LocalDocumentReference("Test", "DocImport"), this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        XWikiDocument nonExistingDocument = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportNonexisting"), this.oldcore.getXWikiContext());
        assertTrue(nonExistingDocument.isNew());

        XWikiDocument foundTranslationDocument =
            foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertSame(foundDocument, foundTranslationDocument);

        XWikiDocument doc1Translation =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImport"));
        doc1Translation.setLanguage("fr");
        doc1Translation.setDefaultLanguage("en");
        this.xwiki.saveDocument(doc1Translation, this.oldcore.getXWikiContext());
        foundTranslationDocument = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertNotSame(foundDocument, foundTranslationDocument);
    }

    /**
     * Test the import with document overwrite.
     * 
     * @throws Exception
     */
    @Test
    public void testImportOverwriteDocument() throws Exception
    {
        XWikiDocument doc1 = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImportOverwrite"));
        doc1.setDefaultLanguage("en");

        byte[] zipFile = this.createZipFile(new XWikiDocument[] { doc1 }, new String[] { "ISO-8859-1" }, null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        // import and install this document
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if it is there
        XWikiDocument foundDocument = this.xwiki.getDocument(new LocalDocumentReference("Test", "DocImportOverwrite"),
            this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());

        // create the overwriting document
        String newContent = "This is new content";
        XWikiDocument overwritingDoc = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocImportOverwrite"));
        overwritingDoc.setContent(newContent);

        zipFile = this.createZipFile(new XWikiDocument[] { overwritingDoc }, new String[] { "ISO-8859-1" }, null);

        // use a new packager because we need to clean-up import data (files list, document data)
        this.pack = new Package();
        // import and install
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());

        // check if the document is there
        XWikiDocument foundOverwritingDoc = this.xwiki
            .getDocument(new LocalDocumentReference("Test", "DocImportOverwrite"), this.oldcore.getXWikiContext());
        assertFalse(foundOverwritingDoc.isNew());
        assertNotSame(foundDocument, foundOverwritingDoc.getVersion());
        assertEquals(foundOverwritingDoc.getContent(), newContent);
        // Make sure the previous version is set as original document by the packager
        // This is cheating a bit, should be tested using a listener instead this hack
        assertSame(foundDocument, foundOverwritingDoc.getOriginalDocument().getOriginalDocument());
    }

    /**
     * Test the import of translation files, with overwrite.
     * 
     * @throws Exception
     */
    @Test
    public void testImportTranslationsOverwrite() throws Exception
    {
        XWikiDocument original = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocTranslation"));
        original.setDefaultLanguage("en");
        original.setTranslation(0);
        XWikiDocument translation = new XWikiDocument(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "Test", "DocTranslation"));
        translation.setLanguage("fr");
        translation.setDefaultLanguage("en");
        translation.setTranslation(1);
        translation.setOriginalDocument(original);

        // import and install those twice with tests
        byte[] zipFile = this.createZipFile(new XWikiDocument[] { original, translation },
            new String[] { "ISO-8859-1", "ISO-8859-1" }, null);

        // make sure no data is in the packager from the other tests run
        this.pack = new Package();
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());
        XWikiDocument foundDocument = this.xwiki.getDocument(new LocalDocumentReference("Test", "DocTranslation"),
            this.oldcore.getXWikiContext());
        assertFalse(foundDocument.isNew());
        // get the translation
        XWikiDocument translationDoc = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertFalse(translationDoc.isNew());

        // use a new packager because we need to clean-up import data (files list, doucument data)
        this.pack = new Package();
        // import again and do the same tests
        this.pack.Import(zipFile, this.oldcore.getXWikiContext());
        this.pack.install(this.oldcore.getXWikiContext());
        foundDocument = this.xwiki.getDocument(new LocalDocumentReference("Test", "DocTranslation"),
            this.oldcore.getXWikiContext());
        // might not be the best method to test the document is in the store though...
        assertFalse(foundDocument.isNew());
        // get the translation
        translationDoc = foundDocument.getTranslatedDocument("fr", this.oldcore.getXWikiContext());
        assertFalse(translationDoc.isNew());
    }
}

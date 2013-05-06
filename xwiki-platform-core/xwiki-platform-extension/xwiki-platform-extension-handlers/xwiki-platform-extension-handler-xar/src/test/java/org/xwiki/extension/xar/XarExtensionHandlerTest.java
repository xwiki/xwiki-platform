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
package org.xwiki.extension.xar;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.lib.action.CustomAction;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.RepositoryUtils;
import org.xwiki.extension.xar.internal.handler.XarExtensionHandler;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackageConfiguration;
import org.xwiki.extension.xar.internal.handler.packager.DefaultPackager;
import org.xwiki.extension.xar.internal.handler.packager.DocumentMergeImporter;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentImporterHandler;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.XWikiStubContextProvider;

public class XarExtensionHandlerTest extends AbstractBridgedComponentTestCase
{
    private XWiki mockXWiki;

    private XWikiStoreInterface mockStore;

    private XWikiRightService mockRightService;

    private JobStatus mockJobStatus;

    private XWikiAttachmentStoreInterface mockAttachmentStore;

    private Map<DocumentReference, Map<String, XWikiDocument>> documents =
        new HashMap<DocumentReference, Map<String, XWikiDocument>>();

    private ExtensionId localXarExtensiontId1;

    private ExtensionId localXarExtensiontId2;

    private RepositoryUtils repositoryUtil;

    private JobManager jobManager;

    private InstalledExtensionRepository xarExtensionRepository;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    private DefaultPackager defaultPackager;

    private DocumentMergeImporter importer;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtils(getComponentManager(), getMockery());
        this.repositoryUtil.setup();

        // mock

        this.mockJobStatus = getMockery().mock(JobStatus.class);

        this.mockXWiki = getMockery().mock(XWiki.class);
        getContext().setWiki(this.mockXWiki);
        getContext().setDatabase("xwiki");
        this.contextUser = new DocumentReference(getContext().getDatabase(), "XWiki", "ExtensionUser");

        this.mockStore = getMockery().mock(XWikiStoreInterface.class);
        this.mockAttachmentStore = getMockery().mock(XWikiAttachmentStoreInterface.class);

        this.mockRightService = getMockery().mock(XWikiRightService.class);

        this.localXarExtensiontId1 = new ExtensionId("test", "1.0");
        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getDocument(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("getDocument")
                {
                    @Override
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        Map<String, XWikiDocument> documentLanguages = documents.get(invocation.getParameter(0));

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<String, XWikiDocument>();
                            documents.put((DocumentReference) invocation.getParameter(0), documentLanguages);
                        }

                        XWikiDocument document = documentLanguages.get("");

                        if (document == null) {
                            document = new XWikiDocument((DocumentReference) invocation.getParameter(0));
                        }

                        return document;
                    }
                });

                allowing(mockStore).loadXWikiDoc(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(new CustomAction("loadXWikiDoc")
                {
                    @Override
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        XWikiDocument providedDocument = (XWikiDocument) invocation.getParameter(0);
                        Map<String, XWikiDocument> documentLanguages =
                            documents.get(providedDocument.getDocumentReference());

                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<String, XWikiDocument>();
                            documents.put((DocumentReference) invocation.getParameter(0), documentLanguages);
                        }

                        XWikiDocument document = documentLanguages.get(providedDocument.getLanguage());

                        if (document == null) {
                            document = new XWikiDocument(providedDocument.getDocumentReference());
                            document.setLanguage(providedDocument.getLanguage());
                            document.setDefaultLanguage(providedDocument.getDefaultLanguage());
                            document.setTranslation(providedDocument.getTranslation());
                        }

                        return document;
                    }
                });

                allowing(mockXWiki).saveDocument(with(any(XWikiDocument.class)), with(any(String.class)),
                    with(any(boolean.class)), with(any(XWikiContext.class)));
                will(new CustomAction("saveDocument")
                {
                    @Override
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);
                        boolean minorEdit = (Boolean) invocation.getParameter(2);

                        document.setMinorEdit(minorEdit);
                        document.incrementVersion();
                        document.setNew(false);

                        Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        XWikiDocument previousDocument;
                        if (documentLanguages == null) {
                            documentLanguages = new HashMap<String, XWikiDocument>();
                            documents.put(document.getDocumentReference(), documentLanguages);
                            previousDocument = null;
                        } else {
                            previousDocument = documentLanguages.get(document.getLanguage());
                        }

                        for (XWikiAttachment attachment : document.getAttachmentList()) {
                            if (!attachment.isContentDirty()) {
                                attachment.setAttachment_content(previousDocument.getAttachment(
                                    attachment.getFilename()).getAttachment_content());
                            }
                        }

                        documentLanguages.put(document.getLanguage(), document.clone());

                        return null;
                    }
                });

                allowing(mockXWiki).deleteDocument(with(any(XWikiDocument.class)), with(any(XWikiContext.class)));
                will(new CustomAction("deleteDocument")
                {
                    @Override
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        XWikiDocument document = (XWikiDocument) invocation.getParameter(0);

                        Map<String, XWikiDocument> documentLanguages = documents.get(document.getDocumentReference());

                        if (documentLanguages != null) {
                            documentLanguages.remove(document.getLanguage());
                        }

                        return null;
                    }
                });

                allowing(mockXWiki).getXClass(with(any(DocumentReference.class)), with(any(XWikiContext.class)));
                will(new CustomAction("getXClass")
                {
                    @Override
                    public Object invoke(org.jmock.api.Invocation invocation) throws Throwable
                    {
                        DocumentReference documentReference = (DocumentReference) invocation.getParameter(0);

                        return classes.get(documentReference.getName());
                    }
                });

                allowing(mockXWiki).getStore();
                will(returnValue(mockStore));

                allowing(mockXWiki).getRightService();
                will(returnValue(mockRightService));

                allowing(mockXWiki).prepareResources(with(any(XWikiContext.class)));

                allowing(mockXWiki).hasAttachmentRecycleBin(with(any(XWikiContext.class)));
                will(returnValue(false));

                allowing(mockXWiki).getAttachmentStore();
                will(returnValue(mockAttachmentStore));
            }
        });

        getContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) getComponentManager().getInstance(XWikiStubContextProvider.class))
            .initialize(getContext());

        // lookup

        this.jobManager = getComponentManager().getInstance(JobManager.class);
        this.xarExtensionRepository =
            getComponentManager().getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);
        this.defaultPackager = getComponentManager().getInstance(Packager.class);
        this.importer = getComponentManager().getInstance(DocumentMergeImporter.class);

        // Get rid of wiki macro listener
        getComponentManager().<ObservationManager> getInstance(ObservationManager.class).removeListener(
            "RegisterMacrosOnImportListener");
    }

    private XarInstalledExtension install(ExtensionId extensionId, String wiki, DocumentReference user)
        throws Throwable
    {
        return installOnNamespace(extensionId, wiki != null ? "wiki:" + wiki : null, user);
    }

    private XarInstalledExtension installOnNamespace(ExtensionId extensionId, String namespace, DocumentReference user)
        throws Throwable
    {
        InstallRequest installRequest = new InstallRequest();
        if (user != null) {
            installRequest.setProperty("user.reference", getContext().getUserReference());
            installRequest.setProperty("checkrights", true);
        }
        installRequest.addExtension(extensionId);
        if (namespace != null) {
            installRequest.addNamespace(namespace);
        }
        Job installJob = this.jobManager.executeJob(InstallJob.JOBTYPE, installRequest);

        List<LogEvent> errors = installJob.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return (XarInstalledExtension) this.xarExtensionRepository.resolve(extensionId);
    }

    private void uninstall(ExtensionId extensionId, String wiki) throws Throwable
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.setProperty("user.reference", getContext().getUserReference());
        uninstallRequest.setProperty("checkrights", true);
        uninstallRequest.addExtension(extensionId);
        if (wiki != null) {
            uninstallRequest.addNamespace("wiki:" + wiki);
        }
        Job uninstallJob = this.jobManager.executeJob(UninstallJob.JOBTYPE, uninstallRequest);

        List<LogEvent> errors = uninstallJob.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }
    }

    // Tests

    @Test
    public void testInstallOnWiki() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                exactly(2).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.mockXWiki.saveDocument(existingDocument, "", true, getContext());

        // install

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // validate

        // space.page
        XWikiDocument page = this.mockXWiki.getDocument(existingDocument.getDocumentReference(), getContext());

        Assert.assertFalse("Document wiki:space.page has not been saved in the database", page.isNew());

        Assert.assertNull(page.getXObject(object.getXClassReference()));

        Assert.assertEquals("Wrong content", "content", page.getContent());
        Assert.assertEquals("Wrong creator", new DocumentReference("wiki", "space", "existingcreator"),
            page.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, page.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, page.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "2.1", page.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, page.getLocale());
        Assert.assertFalse("Document is hidden", page.isHidden());

        BaseClass baseClass = page.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // space.pagewithattachment

        XWikiDocument pagewithattachment =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "2.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", this.contextUser, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getContext()));
        Assert.assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getContext())));
        Assert.assertEquals("xwiki:XWiki.ExtensionUser", attachment.getAuthor());

        // space1.page1

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated = this.mockXWiki.getDocument(translatedReference, getContext());

        Assert.assertNotNull("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated);
        Assert.assertFalse("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated.isNew());

        Assert.assertEquals("Wrong content", "default content", defaultTranslated.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, defaultTranslated.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, defaultTranslated.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, defaultTranslated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", defaultTranslated.getVersion());

        // translated.translated.tr
        XWikiDocument translated = this.documents.get(translatedReference).get("tr");

        Assert.assertNotNull("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated);
        Assert.assertFalse("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated.isNew());

        Assert.assertEquals("Wrong content", "tr content", translated.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, translated.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, translated.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, translated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated.getVersion());

        // translated.translated.fr
        XWikiDocument translated2 = this.documents.get(translatedReference).get("fr");

        Assert.assertNotNull("Document wiki:translated.translated in language fr has not been saved in the database",
            translated2);
        Assert.assertFalse("Document wiki:translated.translated in langauge fr has not been saved in the database",
            translated2.isNew());

        Assert.assertEquals("Wrong content", "fr content", translated2.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, translated2.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, translated2.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, translated2.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated2.getVersion());

        // space.hiddenpage

        XWikiDocument hiddenpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getContext());

        Assert.assertNotNull("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage);
        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());
    }

    @Test
    public void testInstallOnWikiWithoutAuthor() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.mockXWiki.saveDocument(existingDocument, "", true, getContext());

        // install

        install(this.localXarExtensiontId1, "wiki", null);

        // validate

        DocumentReference xarAuthorReference = new DocumentReference("wiki", "XWiki", "author");
        DocumentReference xarCreatorReference = new DocumentReference("wiki", "XWiki", "creator");
        DocumentReference xarContentAuthorReference = new DocumentReference("wiki", "XWiki", "contentAuthor");

        // space.page
        XWikiDocument page = this.mockXWiki.getDocument(existingDocument.getDocumentReference(), getContext());

        Assert.assertFalse("Document wiki:space.page has not been saved in the database", page.isNew());

        Assert.assertNull(page.getXObject(object.getXClassReference()));

        Assert.assertEquals("Wrong content", "content", page.getContent());
        Assert.assertEquals("Wrong creator", new DocumentReference("wiki", "space", "existingcreator"),
            page.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, page.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference, page.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "2.1", page.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, page.getLocale());
        Assert.assertFalse("Document is hidden", page.isHidden());

        BaseClass baseClass = page.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // space.pagewithattachment

        XWikiDocument pagewithattachment =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "2.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", xarCreatorReference, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference,
            pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getContext()));
        Assert.assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getContext())));
        Assert.assertEquals("XWiki.attachmentauthor", attachment.getAuthor());

        // space1.page1

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated = this.mockXWiki.getDocument(translatedReference, getContext());

        Assert.assertNotNull("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated);
        Assert.assertFalse("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated.isNew());

        Assert.assertEquals("Wrong content", "default content", defaultTranslated.getContent());
        Assert.assertEquals("Wrong creator", xarCreatorReference, defaultTranslated.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, defaultTranslated.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference,
            defaultTranslated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", defaultTranslated.getVersion());

        // translated.translated.tr
        XWikiDocument translated = this.documents.get(translatedReference).get("tr");

        Assert.assertNotNull("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated);
        Assert.assertFalse("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated.isNew());

        Assert.assertEquals("Wrong content", "tr content", translated.getContent());
        Assert.assertEquals("Wrong creator", xarCreatorReference, translated.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, translated.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference, translated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated.getVersion());

        // translated.translated.fr
        XWikiDocument translated2 = this.documents.get(translatedReference).get("fr");

        Assert.assertNotNull("Document wiki:translated.translated in language fr has not been saved in the database",
            translated2);
        Assert.assertFalse("Document wiki:translated.translated in langauge fr has not been saved in the database",
            translated2.isNew());

        Assert.assertEquals("Wrong content", "fr content", translated2.getContent());
        Assert.assertEquals("Wrong creator", xarCreatorReference, translated2.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, translated2.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference, translated2.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated2.getVersion());

        // space.hiddenpage

        XWikiDocument hiddenpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getContext());

        Assert.assertNotNull("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage);
        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());
    }

    @Test
    public void testUpgradeOnWiki() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                exactly(3).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // upgrade

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "samespace", "samepage"), getContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content 2", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "2.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getContext()));
        Assert.assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getContext())));

        // space2.page2

        XWikiDocument newPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space2", "page2"), getContext());

        Assert.assertFalse("Document wiki.space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", removedPage.isNew());
    }

    @Test
    public void testUpgradeOnRoot() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getVirtualWikisDatabaseNames(with(any(XWikiContext.class)));
                will(returnValue(Arrays.asList("wiki1", "wiki2")));

                exactly(3).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId1, null, this.contextUser);

        // upgrade

        install(this.localXarExtensiontId2, null, this.contextUser);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "samespace", "samepage"), getContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content 2", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "2.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        Assert.assertEquals("Wrong customclass", "customclass2", modifiedpage.getCustomClass());
        Assert.assertEquals("Wrong defaultTemplate", "defaultTemplate2", modifiedpage.getDefaultTemplate());
        Assert.assertTrue("Wrong hidden", modifiedpage.isHidden());
        Assert.assertEquals("Wrong ValidationScript", "validationScript2", modifiedpage.getValidationScript());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getContext()));
        Assert.assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getContext())));

        // space2.page2

        XWikiDocument newPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "space2", "page2"), getContext());

        Assert.assertFalse("Document wiki.space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "space1", "page1"), getContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", removedPage.isNew());
    }

    @Test
    public void testDowngradeOnWiki() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                exactly(3).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        // upgrade

        getMockery().checking(new Expectations()
        {
            {
                // One attachment exist in the recent version but not in the old one
                oneOf(mockAttachmentStore).deleteXWikiAttachment(with(new BaseMatcher<XWikiAttachment>()
                {
                    @Override
                    public boolean matches(Object arg)
                    {
                        return ((XWikiAttachment) arg).getFilename().equals("attachment.txt");
                    }

                    @Override
                    public void describeTo(Description description)
                    {
                        description.appendValue("attachment.ext");
                    }
                }), with(any(XWikiContext.class)), with(equal(true)));
            }
        });

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "samespace", "samepage"), getContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "3.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getContext()));
        Assert.assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getContext())));

        // space2.page2

        XWikiDocument newPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space2", "page2"), getContext());

        Assert.assertTrue("Document wiki.space2.page2 has not been removed from the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertFalse("Document wiki.space1.page1 has not been saved in the database", removedPage.isNew());
    }

    @Test
    public void testUninstallFromWiki() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {

                exactly(2).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // uninstall

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        uninstall(this.localXarExtensiontId1, "wiki");

        // validate

        XWikiDocument page = this.mockXWiki.getDocument(new DocumentReference("wiki", "space", "page"), getContext());

        Assert.assertTrue("Document wiki.space.page has not been removed from the database", page.isNew());

        XWikiDocument page1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki", "space1", "page1"), getContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", page1.isNew());
    }

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getVirtualWikisDatabaseNames(with(any(XWikiContext.class)));
                will(returnValue(Arrays.asList("wiki1", "wiki2")));

                exactly(2).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        // install

        install(this.localXarExtensiontId1, null, this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "space1", "page1"), getContext());

        Assert.assertFalse(pageWiki1.isNew());

        XWikiDocument pageWiki2 =
            this.mockXWiki.getDocument(new DocumentReference("wiki2", "space1", "page1"), getContext());

        Assert.assertFalse(pageWiki2.isNew());

        XWikiDocument overwrittenpage =
            this.mockXWiki.getDocument(new DocumentReference("wiki1", "space", "overwrittenpage"), getContext());

        Assert.assertFalse(overwrittenpage.isNew());
        Assert.assertEquals("1.1", overwrittenpage.getVersion());

        // uninstall

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        uninstall(this.localXarExtensiontId1, null);

        // validate

        pageWiki1 = this.mockXWiki.getDocument(new DocumentReference("wiki1", "space1", "page1"), getContext());

        Assert.assertTrue(pageWiki1.isNew());

        pageWiki2 = this.mockXWiki.getDocument(new DocumentReference("wiki2", "space1", "page1"), getContext());

        Assert.assertTrue(pageWiki2.isNew());
    }

    // DocumentImporterHandler

    private XWikiDocument importDocument(String resource, boolean interactive, String wiki)
        throws ComponentLookupException, Exception
    {
        DefaultPackageConfiguration configuration = new DefaultPackageConfiguration();
        if (interactive) {
            configuration.setInteractive(interactive);
            configuration.setJobStatus(this.mockJobStatus);
        }

        DocumentImporterHandler documentHandler =
            new DocumentImporterHandler(this.defaultPackager, getComponentManager(), wiki, this.importer);
        documentHandler.setConfiguration(configuration);

        InputStream is = getClass().getResourceAsStream(resource);
        try {
            this.defaultPackager.parseDocument(is, documentHandler);
        } finally {
            is.close();
        }

        return documentHandler.getDocument();
    }

    @Test
    public void testImportDocumentWithDifferentExistingDocument() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.mockXWiki.saveDocument(existingDocument, "", true, getContext());

        getMockery().checking(new Expectations()
        {
            {
                // Make sure it produces a conflict
                oneOf(mockJobStatus).ask(with(anything()));
            }
        });

        importDocument("/packagefile/xarextension1/space/page.xml", true, "wiki");
    }

    @Test
    public void testImportDocumentWithDifferentExistingMandatoryDocument() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        existingDocument.setSyntax(Syntax.XWIKI_2_0);
        this.mockXWiki.saveDocument(existingDocument, "", true, getContext());

        // register a mandatory document initializer
        final MandatoryDocumentInitializer mandatoryInitializer =
            registerMockComponent(MandatoryDocumentInitializer.class, "space.page");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mandatoryInitializer).updateDocument(with(any(XWikiDocument.class)));
                will(returnValue(true));

                // Make sure it does not produces any conflict
                never(mockJobStatus).ask(with(anything()));
            }
        });

        importDocument("/packagefile/xarextension1/space/page.xml", true, "wiki");
    }

    @Test
    public void testImportDocumentWithEqualsExistingDocument() throws Throwable
    {
        importDocument("/packagefile/xarextension1/space/page.xml", true, "wiki");

        // Does not produces any conflict
        importDocument("/packagefile/xarextension1/space/page.xml", true, "wiki");
    }

    // rights check

    // install

    @Test(expected = InstallException.class)
    public void testInstallOnRootWithoutAdminRights() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        install(this.localXarExtensiontId1, null, this.contextUser);
    }

    // uninstall

    @Test(expected = InstallException.class)
    public void testInstallOnWikiWithoutAdminRights() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        install(this.localXarExtensiontId1, "wiki", this.contextUser);
    }

    @Test(expected = InstallException.class)
    public void testInstallOnUnsupportedNamespace() throws Throwable
    {
        installOnNamespace(this.localXarExtensiontId1, "unsupportednamespace", this.contextUser);
    }

    // uninstall

    @Test(expected = UninstallException.class)
    public void testUninstallOnRootWithoutAdminRights() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockXWiki).getVirtualWikisDatabaseNames(with(any(XWikiContext.class)));
                will(returnValue(Arrays.asList("wiki1", "wiki2")));

                exactly(2).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId1, null, this.contextUser);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        uninstall(this.localXarExtensiontId1, null);
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnWikiWithoutAdminRights() throws Throwable
    {
        getMockery().checking(new Expectations()
        {
            {
                exactly(2).of(mockRightService).hasAccessLevel(with(equal("admin")),
                    with(equal("xwiki:XWiki.ExtensionUser")), with(equal("XWiki.XWikiPreferences")),
                    with(any(XWikiContext.class)));
                will(returnValue(true));
            }
        });

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockRightService).hasAccessLevel(with(equal("admin")), with(equal("xwiki:XWiki.ExtensionUser")),
                    with(equal("XWiki.XWikiPreferences")), with(any(XWikiContext.class)));
                will(returnValue(false));
            }
        });

        uninstall(this.localXarExtensiontId1, "wiki");
    }
}

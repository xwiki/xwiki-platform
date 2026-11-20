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
package org.xwiki.extension.xar.internal.handler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.event.WikiCreatedEvent;
import org.xwiki.bridge.event.WikiCreatingEvent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.extension.xar.question.ConflictQuestion;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.DocumentRevisionConflictException;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.util.XWikiStubContextProvider;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OldcoreTest
@AllComponents
@ExtendWith(MockitoRepositoryUtilsExtension.class)
class XarExtensionHandlerTest
{
    private static final DocumentReference PAGE_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final LocalDocumentReference CUSTOM_CLASS = new LocalDocumentReference("XWiki", "CustomClass");

    private static final LocalDocumentReference CONCURRENT_CLASS =
        new LocalDocumentReference("XWiki", "ConcurrentClass");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private XarInstalledExtensionRepository installedExtensionRepository;

    private ExtensionId localXarExtensiontId1;

    private ExtensionId localXarExtensiontId2;

    private ExtensionId collisionextension1;

    private ExtensionId collisionextension2;

    private JobExecutor jobExecutor;

    private InstalledExtensionRepository xarExtensionRepository;

    private ObservationManager observation;

    private DocumentReference contextUser;

    @BeforeEach
    void setUp() throws Exception
    {
        this.oldcore.addWiki("wiki");

        // mock
        this.contextUser = new DocumentReference(getXWikiContext().getWikiId(), "XWiki", "ExtensionUser");

        this.localXarExtensiontId1 = new ExtensionId("test", "1.0");
        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");
        this.collisionextension1 = new ExtensionId("collisionextension1", "version");
        this.collisionextension2 = new ExtensionId("collisionextension2", "version");

        // checking
        doReturn(true).when(this.oldcore.getSpyXWiki()).hasAttachmentRecycleBin(any(XWikiContext.class));

        getXWikiContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) this.componentManager.getInstance(XWikiStubContextProvider.class))
            .initialize(getXWikiContext());

        CoreConfiguration coreConfiguration = this.componentManager.getInstance(CoreConfiguration.class);
        doReturn(Syntax.PLAIN_1_0).when(coreConfiguration).getDefaultDocumentSyntax();

        // lookup

        this.jobExecutor = this.componentManager.getInstance(JobExecutor.class);
        this.xarExtensionRepository =
            this.componentManager.getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);
        this.observation = this.componentManager.getInstance(ObservationManager.class);

        // Get rid of wiki macro listener
        this.componentManager.<ObservationManager>getInstance(ObservationManager.class)
            .removeListener("RegisterMacrosOnImportListener");

        this.installedExtensionRepository =
            this.componentManager.getInstance(InstalledExtensionRepository.class, "xar");

        // Programming right is not required for XAR extensions
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(eq(Right.PROGRAM), any(), any());
    }

    private void setHasNoAdminRight() throws AccessDeniedException
    {
        doThrow(AccessDeniedException.class).when(this.oldcore.getMockAuthorizationManager())
            .checkAccess(eq(Right.ADMIN), eq(this.contextUser), any());
    }

    private void verifyHasAdminRight(int times) throws AccessDeniedException
    {
        verify(this.oldcore.getMockAuthorizationManager(), times(times)).checkAccess(eq(Right.ADMIN),
            eq(this.contextUser), any());
    }

    private XWikiContext getXWikiContext()
    {
        return this.oldcore.getXWikiContext();
    }

    private XarInstalledExtension install(ExtensionId extensionId, String wiki, DocumentReference user) throws Throwable
    {
        return installOnNamespace(extensionId, wiki != null ? "wiki:" + wiki : null, user);
    }

    private XarInstalledExtension installOnNamespace(ExtensionId extensionId, String namespace, DocumentReference user)
        throws Throwable
    {
        InstallRequest installRequest = new InstallRequest();
        if (user != null) {
            installRequest.setProperty("user.reference", getXWikiContext().getUserReference());
            installRequest.setProperty("checkrights", true);
        }
        installRequest.addExtension(extensionId);
        if (namespace != null) {
            installRequest.addNamespace(namespace);
        }
        Job installJob = this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
        installJob.join();

        List<LogEvent> errors = installJob.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            if (errors.get(0).getThrowable() != null) {
                throw errors.get(0).getThrowable();
            } else {
                throw new Exception(errors.get(0).getFormattedMessage());
            }
        }

        return (XarInstalledExtension) this.xarExtensionRepository.resolve(extensionId);
    }

    /**
     * Installs the requested extensions, answering the questions asked by the install job with their default answer.
     *
     * @param installRequest the install request
     * @param beforeAnswer called with each conflict question before answering it
     * @return the finished install job
     */
    private Job installAnsweringQuestions(InstallRequest installRequest, Consumer<ConflictQuestion> beforeAnswer)
        throws Exception
    {
        Job installJob = this.jobExecutor.execute(InstallJob.JOBTYPE, installRequest);
        JobStatus status = installJob.getStatus();
        while (true) {
            // The question is removed from the status once answered.
            await().until(() -> status.getQuestion() != null || status.getState() == JobStatus.State.FINISHED);
            if (status.getState() == JobStatus.State.FINISHED) {
                return installJob;
            }
            if (status.getQuestion() instanceof ConflictQuestion question) {
                beforeAnswer.accept(question);
            }
            status.answered();
        }
    }

    private void uninstall(ExtensionId extensionId, String wiki) throws Throwable
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.setProperty("user.reference", getXWikiContext().getUserReference());
        uninstallRequest.setProperty("checkrights", true);
        uninstallRequest.addExtension(extensionId);
        if (wiki != null) {
            uninstallRequest.addNamespace("wiki:" + wiki);
        }
        Job uninstallJob = this.jobExecutor.execute(UninstallJob.JOBTYPE, uninstallRequest);
        uninstallJob.join();

        List<LogEvent> errors = uninstallJob.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        if (!errors.isEmpty()) {
            if (errors.get(0).getThrowable() != null) {
                throw errors.get(0).getThrowable();
            } else {
                throw new Exception(errors.get(0).getFormattedMessage());
            }
        }
    }

    private void assertPageRemoved(String space, String page) throws Throwable
    {
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", space, page), getXWikiContext());
        assertTrue(document.isNew(), "Document " + "wiki" + ":" + space + "." + page + " has not been removed");
    }

    private void assertPageNotRemoved() throws Throwable
    {
        XWikiDocument document =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());
        assertFalse(document.isNew(), "Document " + "wiki" + ":" + "space" + "." + "page" + " has been removed");
    }

    // Tests

    @Test
    void installOnWiki() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getSpyXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.mandatory");
        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<XWikiDocument>getArgument(0).setTitle("mandatory title");

                return true;
            }
        });
        XWikiDocument mandatoryDocument = new XWikiDocument(new DocumentReference("wiki", "space", "mandatory"));
        mandatoryDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        mandatoryDocument.setSyntax(Syntax.PLAIN_1_0);
        mandatoryDocument.setContent("modified content");
        mandatoryDocument.setTitle("mandatory title");
        mandatoryDocument.setDefaultLocale(Locale.ENGLISH);
        this.oldcore.getSpyXWiki().saveDocument(mandatoryDocument, "", true, getXWikiContext());

        MandatoryDocumentInitializer mandatoryconfigurationInitializer = this.componentManager
            .registerMockComponent(MandatoryDocumentInitializer.class, "space.mandatoryconfiguration");
        when(mandatoryconfigurationInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);
        XWikiDocument mandatoryconfigurationDocument =
            new XWikiDocument(new DocumentReference("wiki", "space", "mandatoryconfiguration"));
        mandatoryconfigurationDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        mandatoryconfigurationDocument.setSyntax(Syntax.PLAIN_1_0);
        this.oldcore.getSpyXWiki().saveDocument(mandatoryconfigurationDocument, "", true, getXWikiContext());

        // install

        XarInstalledExtension xarInstalledExtension = install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // space.page
        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(existingDocument.getDocumentReference(), getXWikiContext());

        assertFalse(page.isNew(), "Document wiki:space.page has not been saved in the database");

        assertNull(page.getXObject(object.getXClassReference()));

        assertEquals("content", page.getContent(), "Wrong content");
        assertEquals(new DocumentReference("wiki", "space", "existingcreator"), page.getCreatorReference(),
            "Wrong creator");
        assertEquals(this.contextUser, page.getAuthorReference(), "Wrong author");
        assertEquals(this.contextUser, page.getContentAuthorReference(), "Wrong content author");
        assertEquals("2.1", page.getVersion(), "Wrong version");
        assertEquals(Locale.ROOT, page.getLocale(), "Wrong version");
        assertFalse(page.isHidden(), "Document is hidden");
        assertFalse(page.isMinorEdit(), "Document is minor edit");

        BaseClass baseClass = page.getXClass();
        assertNotNull(baseClass.getField("property"));
        assertEquals("property", baseClass.getField("property").getName());
        assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // space.pagewithattachment

        XWikiDocument pagewithattachment = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getXWikiContext());
        assertFalse(pagewithattachment.isNew());
        assertEquals("1.1", pagewithattachment.getVersion(), "Wrong version");
        assertEquals(this.contextUser, pagewithattachment.getCreatorReference(), "Wrong creator");
        assertEquals(this.contextUser, pagewithattachment.getAuthorReference(), "Wrong author");
        assertEquals(this.contextUser, pagewithattachment.getContentAuthorReference(), "Wrong content author");

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        assertNotNull(attachment);
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));
        assertEquals(this.contextUser, attachment.getAuthorReference());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        assertFalse(page1.isNew(), "Document wiki:space1.page1 has not been saved in the database");

        assertEquals(List.of(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(page1.getDocumentReference()));
        assertEquals(List.of(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(page1.getDocumentReferenceWithLocale()));
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("wiki", "space1", "page1", Locale.ENGLISH)).size());
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("otherwiki", "space1", "page1")).size());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getSpyXWiki().getDocument(translatedReference, getXWikiContext());

        assertNotNull(defaultTranslated,
            "Document wiki:translated.translated has not been saved in the database");
        assertFalse(defaultTranslated.isNew(),
            "Document wiki:translated.translated has not been saved in the database");

        assertEquals("default content", defaultTranslated.getContent(), "Wrong content");
        assertEquals(this.contextUser, defaultTranslated.getCreatorReference(), "Wrong creator");
        assertEquals(this.contextUser, defaultTranslated.getAuthorReference(), "Wrong author");
        assertEquals(this.contextUser, defaultTranslated.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", defaultTranslated.getVersion(), "Wrong version");

        assertEquals(List.of(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(defaultTranslated.getDocumentReferenceWithLocale()));

        // translated.translated.tr
        XWikiDocument translated =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("tr")));

        assertNotNull(translated,
            "Document wiki:translated.translated in language tr has not been saved in the database");
        assertFalse(translated.isNew(),
            "Document wiki:translated.translated in language tr has not been saved in the database");

        assertEquals("tr content", translated.getContent(), "Wrong content");
        assertEquals(this.contextUser, translated.getCreatorReference(), "Wrong creator");
        assertEquals(this.contextUser, translated.getAuthorReference(), "Wrong author");
        assertEquals(this.contextUser, translated.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", translated.getVersion(), "Wrong version");

        assertEquals(List.of(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(translated.getDocumentReferenceWithLocale()));

        // translated.translated.fr
        XWikiDocument translated2 =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("fr")));

        assertNotNull(translated2,
            "Document wiki:translated.translated in language fr has not been saved in the database");
        assertFalse(translated2.isNew(),
            "Document wiki:translated.translated in language fr has not been saved in the database");

        assertEquals("fr content", translated2.getContent(), "Wrong content");
        assertEquals(this.contextUser, translated2.getCreatorReference(), "Wrong creator");
        assertEquals(this.contextUser, translated2.getAuthorReference(), "Wrong author");
        assertEquals(this.contextUser, translated2.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", translated2.getVersion(), "Wrong version");

        assertEquals(List.of(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(translated2.getDocumentReferenceWithLocale()));

        // space.hiddenpage

        XWikiDocument hiddenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getXWikiContext());

        assertFalse(hiddenpage.isNew(), "Document wiki:space.hiddenpage has not been saved in the database");

        assertTrue(hiddenpage.isHidden(), "Document is not hidden");

        // space.mandatory

        XWikiDocument mandatorypage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "mandatory"), getXWikiContext());

        assertEquals("1.1", mandatorypage.getVersion(), "Document wiki:space.mandatory has been overwritten");
        assertEquals("mandatory title", mandatorypage.getTitle());

        // space.mandatoryconfiguration

        XWikiDocument mandatoryconfigurationpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "mandatoryconfiguration"), getXWikiContext());

        assertEquals("2.1", mandatoryconfigurationpage.getVersion(),
            "Document wiki:space.mandatoryconfiguration has not been overwritten");

        assertTrue(
            this.installedExtensionRepository.isAllowed(mandatoryconfigurationpage.getDocumentReference(), Right.EDIT));
        assertFalse(this.installedExtensionRepository.isAllowed(mandatoryconfigurationpage.getDocumentReference(),
            Right.DELETE));

        // space.movedpage

        XWikiDocument movedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "movedpage"), getXWikiContext());

        assertFalse(movedpage.isNew(), "Document wiki:space.movedpage has been removed");
        assertEquals("content 1.0", movedpage.getContent());

        // space.dependencypage

        XWikiDocument dependencypage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "dependencypage"), getXWikiContext());

        assertFalse(dependencypage.isNew(), "Document wiki:space.dependencypage has been removed");
        assertEquals("otherdependency 1.0", dependencypage.getContent());
    }

    @Test
    void installOnWikiWithoutAuthor() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getSpyXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        // install

        install(this.localXarExtensiontId1, "wiki", null);

        // validate

        DocumentReference xarAuthorReference = new DocumentReference("wiki", "XWiki", "author");
        DocumentReference xarCreatorReference = new DocumentReference("wiki", "XWiki", "creator");
        DocumentReference xarContentAuthorReference = new DocumentReference("wiki", "XWiki", "contentAuthor");

        // space.page
        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(existingDocument.getDocumentReference(), getXWikiContext());

        assertFalse(page.isNew(), "Document wiki:space.page has not been saved in the database");

        assertNull(page.getXObject(object.getXClassReference()));

        assertEquals("content", page.getContent(), "Wrong content");
        assertEquals(new DocumentReference("wiki", "space", "existingcreator"), page.getCreatorReference(),
            "Wrong creator");
        assertEquals(xarAuthorReference, page.getAuthorReference(), "Wrong author");
        assertEquals(xarContentAuthorReference, page.getContentAuthorReference(), "Wrong content author");
        assertEquals("2.1", page.getVersion(), "Wrong version");
        assertEquals(Locale.ROOT, page.getLocale(), "Wrong version");
        assertFalse(page.isHidden(), "Document is hidden");

        BaseClass baseClass = page.getXClass();
        assertNotNull(baseClass.getField("property"));
        assertEquals("property", baseClass.getField("property").getName());
        assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // space.pagewithattachment

        XWikiDocument pagewithattachment = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getXWikiContext());
        assertFalse(pagewithattachment.isNew());
        assertEquals("1.1", pagewithattachment.getVersion(), "Wrong version");
        assertEquals(xarCreatorReference, pagewithattachment.getCreatorReference(), "Wrong creator");
        assertEquals(xarAuthorReference, pagewithattachment.getAuthorReference(), "Wrong author");
        assertEquals(xarContentAuthorReference, pagewithattachment.getContentAuthorReference(), "Wrong content author");

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        assertNotNull(attachment);
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));
        assertEquals(new DocumentReference("wiki", "XWiki", "attachmentauthor"), attachment.getAuthorReference());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        assertFalse(page1.isNew(), "Document wiki:space1.page1 has not been saved in the database");

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getSpyXWiki().getDocument(translatedReference, getXWikiContext());

        assertNotNull(defaultTranslated,
            "Document wiki:translated.translated has not been saved in the database");
        assertFalse(defaultTranslated.isNew(),
            "Document wiki:translated.translated has not been saved in the database");

        assertEquals("default content", defaultTranslated.getContent(), "Wrong content");
        assertEquals(xarCreatorReference, defaultTranslated.getCreatorReference(), "Wrong creator");
        assertEquals(xarAuthorReference, defaultTranslated.getAuthorReference(), "Wrong author");
        assertEquals(xarContentAuthorReference, defaultTranslated.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", defaultTranslated.getVersion(), "Wrong version");

        // translated.translated.tr
        XWikiDocument translated =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("tr")));

        assertNotNull(translated,
            "Document wiki:translated.translated in language tr has not been saved in the database");
        assertFalse(translated.isNew(),
            "Document wiki:translated.translated in language tr has not been saved in the database");

        assertEquals("tr content", translated.getContent(), "Wrong content");
        assertEquals(xarCreatorReference, translated.getCreatorReference(), "Wrong creator");
        assertEquals(xarAuthorReference, translated.getAuthorReference(), "Wrong author");
        assertEquals(xarContentAuthorReference, translated.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", translated.getVersion(), "Wrong version");

        // translated.translated.fr
        XWikiDocument translated2 =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("fr")));

        assertNotNull(translated2,
            "Document wiki:translated.translated in language fr has not been saved in the database");
        assertFalse(translated2.isNew(),
            "Document wiki:translated.translated in language fr has not been saved in the database");

        assertEquals("fr content", translated2.getContent(), "Wrong content");
        assertEquals(xarCreatorReference, translated2.getCreatorReference(), "Wrong creator");
        assertEquals(xarAuthorReference, translated2.getAuthorReference(), "Wrong author");
        assertEquals(xarContentAuthorReference, translated2.getContentAuthorReference(), "Wrong content author");
        assertEquals("1.1", translated2.getVersion(), "Wrong version");

        // space.hiddenpage

        XWikiDocument hiddenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getXWikiContext());

        assertNotNull(hiddenpage, "Document wiki:space.hiddenpage has not been saved in the database");
        assertFalse(hiddenpage.isNew(), "Document wiki:space.hiddenpage has not been saved in the database");

        assertTrue(hiddenpage.isHidden(), "Document is not hidden");
    }

    @Test
    void upgradeOnWiki() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // Do some local modifications

        XWikiDocument deletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "deletedpage"), getXWikiContext());
        this.oldcore.getSpyXWiki().deleteDocument(deletedpage, getXWikiContext());

        XWikiDocument modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "modifieddeletedpage"), getXWikiContext());
        this.oldcore.getSpyXWiki().deleteDocument(modifieddeletedpage, getXWikiContext());

        XWikiDocument pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithobject"), getXWikiContext());
        pagewithobject.removeXObjects(new LocalDocumentReference("XWiki", "XWikiGroups"));
        this.oldcore.getSpyXWiki().saveDocument(pagewithobject, getXWikiContext());

        XWikiDocument deletedpagewithmodifications = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space1", "modified"), getXWikiContext());
        deletedpagewithmodifications.setContent("modified content");
        this.oldcore.getSpyXWiki().saveDocument(deletedpagewithmodifications, getXWikiContext());

        // upgrade

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(6);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext());

        assertFalse(samepage.isNew(), "Document samespace has been removed from the database");
        assertEquals("1.1", samepage.getVersion(), "Wrong versions");

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        assertFalse(modifiedpage.isNew(), "Document wiki:space.page has not been saved in the database");

        assertEquals("content 2", modifiedpage.getContent(), "Wrong content");
        assertEquals(this.contextUser, modifiedpage.getAuthorReference(), "Wrong author");
        assertEquals("2.1", modifiedpage.getVersion(), "Wrong versions");
        assertEquals(Locale.ROOT, modifiedpage.getLocale(), "Wrong version");

        assertEquals("customclass2", modifiedpage.getCustomClass(), "Wrong customclass");
        assertEquals("defaultTemplate2", modifiedpage.getDefaultTemplate(), "Wrong defaultTemplate");
        assertTrue(modifiedpage.isHidden(), "Wrong hidden");
        assertEquals("validationScript2", modifiedpage.getValidationScript(), "Wrong ValidationScript");

        BaseClass baseClass = modifiedpage.getXClass();
        assertNotNull(baseClass.getField("property"));
        assertEquals("property", baseClass.getField("property").getName());
        assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        assertNotNull(attachment);
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        assertFalse(newPage.isNew(), "Document wiki:space2.page2 has not been saved in the database");

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        assertTrue(removedPage.isNew(), "Document wiki:space1.page1 has not been removed from the database");

        // space.deletedpage

        deletedpage = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "deletedpage"),
            getXWikiContext());

        assertTrue(deletedpage.isNew(), "Document wiki:space.deleted has been restored");

        // space.modifieddeletedpage

        modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "modifieddeletedpage"), getXWikiContext());

        assertTrue(modifieddeletedpage.isNew(), "Document wiki:space.modifieddeletedpage has been restored");

        // space.pagewithobject

        pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithobject"), getXWikiContext());

        assertNull(pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")),
            "Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object");

        // space.movedpage

        XWikiDocument movedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "movedpage"), getXWikiContext());

        assertFalse(movedpage.isNew(), "Document wiki:space.movedpage has been removed");
        assertEquals("content 2.0", movedpage.getContent());

        // space.dependencypage

        XWikiDocument dependencypage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "dependencypage"), getXWikiContext());

        assertFalse(dependencypage.isNew(), "Document wiki:space.dependencypage has been removed");
        assertEquals("otherdependency 2.0", dependencypage.getContent());

        // space1.modified

        XWikiDocument space1modified = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space1", "modified"), getXWikiContext());

        assertFalse(space1modified.isNew(), "Document wiki:space.modified has been removed from the database");
    }

    /**
     * Upgrades the extension on the wiki, asking how to upgrade the customized page {@code space.page}, and saves the
     * page while the first question about it waits for an answer.
     *
     * @param answer the answer to the questions about the page
     * @return the questions asked about the page
     */
    private List<ConflictQuestion> upgradeWithSaveWhileAsking(ConflictQuestion.GlobalAction answer) throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // Customize the page, so that the upgrade merges it.
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext()).clone();
        page.newXObject(CUSTOM_CLASS, getXWikiContext());
        this.oldcore.getSpyXWiki().saveDocument(page, getXWikiContext());

        this.oldcore.checkDocumentRevision(true);
        InstallRequest installRequest = new InstallRequest();
        installRequest.setProperty("user.reference", this.contextUser);
        installRequest.addExtension(this.localXarExtensiontId2);
        installRequest.addNamespace("wiki:wiki");
        installRequest.setInteractive(true);
        installRequest.setProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_MERGE_SUCCESS,
            ConflictQuestion.GlobalAction.ASK);
        List<ConflictQuestion> pageQuestions = new ArrayList<>();
        Job installJob = installAnsweringQuestions(installRequest, question -> {
            if (question.getCurrentDocument().getDocumentReference().equals(PAGE_REFERENCE)) {
                if (pageQuestions.isEmpty()) {
                    try {
                        XWikiDocument concurrentPage =
                            this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext()).clone();
                        concurrentPage.newXObject(CONCURRENT_CLASS, getXWikiContext());
                        this.oldcore.getSpyXWiki().saveDocument(concurrentPage, getXWikiContext());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                question.setGlobalAction(answer);
                pageQuestions.add(question);
            }
        });

        assertEquals(List.of(), installJob.getStatus().getLog().getLogsFrom(LogLevel.WARN));

        return pageQuestions;
    }

    @Test
    void upgradeOnWikiWithSaveWhileAsking() throws Throwable
    {
        List<ConflictQuestion> pageQuestions = upgradeWithSaveWhileAsking(ConflictQuestion.GlobalAction.MERGED);

        // The page is merged again with the version saved by the user, which is asked again.
        assertEquals(2, pageQuestions.size());
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext());
        assertEquals("content 2", page.getContent());
        assertNotNull(page.getXObject(CUSTOM_CLASS));
        assertNotNull(page.getXObject(CONCURRENT_CLASS));
    }

    @Test
    void upgradeOnWikiWithSaveWhileAskingAnsweredWithNextVersion() throws Throwable
    {
        List<ConflictQuestion> pageQuestions = upgradeWithSaveWhileAsking(ConflictQuestion.GlobalAction.NEXT);

        // Replacing the page with the new version is asked again, with the version saved by the user, which the first
        // answer didn't take into account.
        assertEquals(2, pageQuestions.size());
        assertNotNull(pageQuestions.get(1).getCurrentDocument().getXObject(CONCURRENT_CLASS));
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext());
        assertEquals("content 2", page.getContent());
    }

    @Test
    void resetWithContextUser() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        DocumentReference pageReference = new DocumentReference("wiki", "space", "page");
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(pageReference, getXWikiContext()).clone();
        page.setContent("modified content");
        page.setAuthorReference(new DocumentReference("wiki", "XWiki", "OtherUser"));
        this.oldcore.getSpyXWiki().saveDocument(page, getXWikiContext());

        this.componentManager.<Packager>getInstance(Packager.class)
            .resetWithContextUser(new DocumentReference(pageReference, Locale.ROOT));

        page = this.oldcore.getSpyXWiki().getDocument(pageReference, getXWikiContext());
        assertEquals("content", page.getContent());
        assertEquals(this.contextUser, page.getAuthorReference());
    }

    /**
     * @return a request upgrading the extension on the wiki, asking about deleted pages and successful merges
     */
    private InstallRequest createAskingUpgradeRequest()
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.setProperty("user.reference", this.contextUser);
        installRequest.addExtension(this.localXarExtensiontId2);
        installRequest.addNamespace("wiki:wiki");
        installRequest.setInteractive(true);
        installRequest.setProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_MERGE_SUCCESS,
            ConflictQuestion.GlobalAction.ASK);
        installRequest.setProperty(ConflictQuestion.REQUEST_CONFLICT_DEFAULTANSWER_CURRENT_DELETED,
            ConflictQuestion.GlobalAction.ASK);
        return installRequest;
    }

    private void saveConcurrently(Consumer<XWikiDocument> change)
    {
        try {
            XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext()).clone();
            change.accept(page);
            this.oldcore.getSpyXWiki().saveDocument(page, getXWikiContext());
        } catch (XWikiException e) {
            throw new RuntimeException(e);
        }
    }

    private void addXObject(XWikiDocument document, LocalDocumentReference classReference)
    {
        try {
            document.newXObject(classReference, getXWikiContext());
        } catch (XWikiException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void upgradeOnWikiWithPageCreatedWhileAskingWithoutRevisionCheck() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);
        this.oldcore.getSpyXWiki().deleteDocument(
            this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext()), getXWikiContext());
        List<ConflictQuestion> pageQuestions = new ArrayList<>();

        // Without the store refusing outdated saves, the importer still notices that the page was created after the
        // merge.
        StoreConfiguration storeConfiguration = spy(this.componentManager.<StoreConfiguration>getInstance(
            StoreConfiguration.class));
        doReturn(false).when(storeConfiguration).isRevisionCheckEnabled();
        this.componentManager.registerComponent(StoreConfiguration.class, storeConfiguration);
        Job installJob = installAnsweringQuestions(createAskingUpgradeRequest(), question -> {
            if (question.getNextDocument().getDocumentReference().equals(PAGE_REFERENCE)) {
                if (question.getCurrentDocument() == null) {
                    // The user creates the page again while asked whether to restore it, and the answer is to
                    // restore it.
                    saveConcurrently(page -> {
                        page.setSyntax(Syntax.XWIKI_2_0);
                        page.setContent("content");
                        addXObject(page, CONCURRENT_CLASS);
                    });
                    question.setGlobalAction(ConflictQuestion.GlobalAction.NEXT);
                } else {
                    question.setGlobalAction(ConflictQuestion.GlobalAction.MERGED);
                }
                pageQuestions.add(question);
            }
        });

        assertEquals(List.of(), installJob.getStatus().getLog().getLogsFrom(LogLevel.WARN));
        // The page created by the user is merged again, and upgraded instead of being replaced.
        assertEquals(2, pageQuestions.size());
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext());
        assertEquals("content 2", page.getContent());
        assertNotNull(page.getXObject(CONCURRENT_CLASS));
    }

    @Test
    void upgradeOnWikiWithPageAlwaysSavedWhileAsking() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);
        saveConcurrently(page -> addXObject(page, CUSTOM_CLASS));

        this.oldcore.checkDocumentRevision(true);
        List<ConflictQuestion> pageQuestions = new ArrayList<>();
        Job installJob = installAnsweringQuestions(createAskingUpgradeRequest(), question -> {
            if (question.getCurrentDocument().getDocumentReference().equals(PAGE_REFERENCE)) {
                saveConcurrently(page -> addXObject(page, CONCURRENT_CLASS));
                question.setGlobalAction(ConflictQuestion.GlobalAction.MERGED);
                pageQuestions.add(question);
            }
        });

        // The page is merged 5 times, then left as saved by the user, and the failure is logged.
        assertEquals(5, pageQuestions.size());
        List<LogEvent> errors = installJob.getStatus().getLog().getLogsFrom(LogLevel.WARN);
        assertEquals(1, errors.size());
        assertEquals("Failed to install document [wiki:space.page()] because it kept being saved concurrently",
            errors.get(0).getFormattedMessage());
        XWikiDocument page = this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext());
        assertEquals("content", page.getContent());
        assertEquals(5, page.getXObjects(CONCURRENT_CLASS).size());
    }

    @Test
    void upgradeOnWikiFailingToLoadPage() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        XWikiException loadFailure = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC, "Failed to load");
        doThrow(loadFailure).when(this.oldcore.getSpyXWiki()).getDocument(
            argThat((DocumentReference reference) -> reference != null
                && PAGE_REFERENCE.equals(new DocumentReference(reference, (Locale) null))),
            any(XWikiContext.class));

        // Failing to load a document fails the whole upgrade instead of silently skipping the document.
        Throwable failure = assertThrows(Throwable.class, () -> install(this.localXarExtensiontId2, "wiki", null));
        assertTrue(ExceptionUtils.getThrowableList(failure).contains(loadFailure), failure::toString);
    }

    @Test
    void resetWithContextUserWhenSavedConcurrently() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);
        saveConcurrently(page -> page.setContent("modified content"));

        this.oldcore.checkDocumentRevision(true);
        DocumentReference pageReference = new DocumentReference(PAGE_REFERENCE, Locale.ROOT);
        // The page is saved again after the reset loaded it.
        AtomicBoolean loaded = new AtomicBoolean();
        doAnswer(invocation -> {
            Object document = invocation.callRealMethod();
            if (loaded.compareAndSet(false, true)) {
                saveConcurrently(page -> page.setContent("modified again"));
            }
            return document;
        }).when(this.oldcore.getSpyXWiki()).getDocument(eq(pageReference), any(XWikiContext.class));
        Packager packager = this.componentManager.getInstance(Packager.class);

        // The reset discards the version the user saw, not the one saved since, so it fails.
        assertThrows(DocumentRevisionConflictException.class, () -> packager.resetWithContextUser(pageReference));

        assertEquals("modified again",
            this.oldcore.getSpyXWiki().getDocument(PAGE_REFERENCE, getXWikiContext()).getContent());
    }

    @Test
    void upgradeOnRoot() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(3);

        // Do some local modifications

        XWikiDocument deletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "deletedpage"), getXWikiContext());
        this.oldcore.getSpyXWiki().deleteDocument(deletedpage, getXWikiContext());

        XWikiDocument modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "modifieddeletedpage"), getXWikiContext());
        this.oldcore.getSpyXWiki().deleteDocument(modifieddeletedpage, getXWikiContext());

        XWikiDocument pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "pagewithobject"), getXWikiContext());
        pagewithobject.removeXObjects(new LocalDocumentReference("XWiki", "XWikiGroups"));
        this.oldcore.getSpyXWiki().saveDocument(pagewithobject, getXWikiContext());

        XWikiDocument deletedpagewithmodifications = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "modified"), getXWikiContext());
        deletedpagewithmodifications.setContent("modified content");
        this.oldcore.getSpyXWiki().saveDocument(deletedpagewithmodifications, getXWikiContext());

        // upgrade

        install(this.localXarExtensiontId2, null, this.contextUser);

        verifyHasAdminRight(6);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "samespace", "samepage"), getXWikiContext());

        assertEquals("1.1", samepage.getVersion(), "Wrong versions");

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(modifiedpage.isNew(), "Document wiki:space.page has not been saved in the database");

        assertEquals("content 2", modifiedpage.getContent(), "Wrong content");
        assertEquals(this.contextUser, modifiedpage.getAuthorReference(), "Wrong author");
        assertEquals("2.1", modifiedpage.getVersion(), "Wrong versions");
        assertEquals(Locale.ROOT, modifiedpage.getLocale(), "Wrong version");

        assertEquals("customclass2", modifiedpage.getCustomClass(), "Wrong customclass");
        assertEquals("defaultTemplate2", modifiedpage.getDefaultTemplate(), "Wrong defaultTemplate");
        assertTrue(modifiedpage.isHidden(), "Wrong hidden");
        assertEquals("validationScript2", modifiedpage.getValidationScript(), "Wrong ValidationScript");

        BaseClass baseClass = modifiedpage.getXClass();
        assertNotNull(baseClass.getField("property"));
        assertEquals("property", baseClass.getField("property").getName());
        assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        assertNotNull(attachment);
        assertEquals("attachment.txt", attachment.getFilename());
        assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));

        // space2.page2

        XWikiDocument newPage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space2", "page2"), getXWikiContext());

        assertFalse(newPage.isNew(), "Document wiki:space2.page2 has not been saved in the database");

        // space1.page1

        XWikiDocument removedPage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext());

        assertTrue(removedPage.isNew(), "Document wiki:space1.page1 has not been removed from the database");

        // space.deletedpage

        deletedpage = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "deletedpage"),
            getXWikiContext());

        assertTrue(deletedpage.isNew(), "Document wiki:space.deleted has been restored");

        // space.modifieddeletedpage

        modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "modifieddeletedpage"), getXWikiContext());

        assertTrue(modifieddeletedpage.isNew(), "Document wiki:space.modifieddeletedpage has been restored");

        // space.pagewithobject

        pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "pagewithobject"), getXWikiContext());

        assertNull(pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")),
            "Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object");

        // space1.modified

        XWikiDocument space1modified = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "modified"), getXWikiContext());

        assertFalse(space1modified.isNew(), "Document wiki:space1.modified has been removed from the database");
    }

    @Test
    void downgradeOnWiki() throws Throwable
    {
        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // upgrade

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(4);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext());

        assertEquals("1.1", samepage.getVersion(), "Wrong versions");

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        assertFalse(modifiedpage.isNew(), "Document wiki:space.page has not been saved in the database");

        assertEquals("content", modifiedpage.getContent(), "Wrong content");
        assertEquals(this.contextUser, modifiedpage.getAuthorReference(), "Wrong author");
        assertEquals("2.1", modifiedpage.getVersion(), "Wrong versions");
        assertEquals(Locale.ROOT, modifiedpage.getLocale(), "Wrong version");

        BaseClass baseClass = modifiedpage.getXClass();
        assertNotNull(baseClass.getField("property"));
        assertEquals("property", baseClass.getField("property").getName());
        assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // The attachment does not exist in version 1.0
        assertNull(modifiedpage.getAttachment("attachment.txt"));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        assertTrue(newPage.isNew(), "Document wiki:space2.page2 has not been removed from the database");

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        assertFalse(removedPage.isNew(), "Document wiki:space1.page1 has not been saved in the database");
    }

    @Test
    void uninstallFromWiki() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(4);

        // validate

        // space.page belong to several extensions
        assertPageNotRemoved();

        // pages which only belong to the uninstalled extension
        assertPageRemoved("space1", "page1");
        assertPageRemoved("space", "class");
    }

    @Test
    void uninstallMandatory() throws Throwable
    {
        // register a mandatory document initializer
        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.page");

        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(4);

        // validate

        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        assertFalse(page.isNew(), "Document wiki:space.page has been removed from the database");
    }

    static Stream<Arguments> uninstallExtensionWithCommonDocument()
    {
        return Stream.of(
            arguments("wiki", "wiki"),
            arguments(null, null),
            arguments("wiki", null),
            arguments(null, "wiki")
        );
    }

    @ParameterizedTest
    @MethodSource("uninstallExtensionWithCommonDocument")
    void uninstallExtensionWithCommonDocument(String ext1Namespace, String ext2Namespace) throws Throwable
    {
        install(this.collisionextension1, ext1Namespace, this.contextUser);
        install(this.collisionextension2, ext2Namespace, this.contextUser);

        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext()).isNew());

        // uninstall
        uninstall(this.collisionextension1, ext1Namespace);

        // validate
        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext()).isNew());
    }

    @Test
    void installOnRoot() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(3);

        // validate

        XWikiDocument pageWiki1 = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext());

        assertFalse(pageWiki1.isNew());

        XWikiDocument pageWiki2 = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext());

        assertFalse(pageWiki2.isNew());

        XWikiDocument overwrittenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "overwrittenpage"), getXWikiContext());

        assertFalse(overwrittenpage.isNew());
        assertEquals("1.1", overwrittenpage.getVersion());

        // uninstall

        uninstall(this.localXarExtensiontId1, null);

        verifyHasAdminRight(4);

        // validate

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space1", "page1"),
            getXWikiContext());

        assertTrue(pageWiki1.isNew(), "Document wiki1:space1.page1 hasn't been removed from the database");

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space1", "page1"),
            getXWikiContext());

        assertTrue(pageWiki2.isNew());
    }

    // rights check

    // install

    @Test
    void installOnRootWithoutAdminRights() throws Throwable
    {
        setHasNoAdminRight();

        assertThrows(InstallException.class,
            () -> install(this.localXarExtensiontId1, null, this.contextUser));
    }

    // uninstall

    @Test
    void installOnWikiWithoutAdminRights() throws Throwable
    {
        setHasNoAdminRight();

        assertThrows(InstallException.class,
            () -> install(this.localXarExtensiontId1, "wiki", this.contextUser));
    }

    @Test
    void installOnUnsupportedNamespace()
    {
        assertThrows(InstallException.class,
            () -> installOnNamespace(this.localXarExtensiontId1, "unsupportednamespace", this.contextUser));
    }

    // uninstall

    @Test
    void uninstallOnRootWithoutAdminRights() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(3);

        setHasNoAdminRight();

        assertThrows(UninstallException.class,
            () -> uninstall(this.localXarExtensiontId1, null));
    }

    @Test
    void uninstallOnWikiWithoutAdminRights() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        setHasNoAdminRight();

        assertThrows(UninstallException.class,
            () -> uninstall(this.localXarExtensiontId1, "wiki"));
    }

    @Test
    void installOnNamespaceThenOnRoot() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1.setContent("modified content");
        this.oldcore.getSpyXWiki().saveDocument(pageWiki1, getXWikiContext());

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("2.1", pageWiki1.getVersion());

        // install on root

        install(this.localXarExtensiontId1, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("2.1", pageWiki1.getVersion());
        // Make sure modifications are kept
        assertEquals("modified content", pageWiki1.getContent());
    }

    @Test
    void installOnNamespaceThenUpgradeOnRoot() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
            getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("1.1", pageWiki1.getVersion());

        XWikiDocument pageWiki2 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        assertTrue(pageWiki2.isNew());

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
            getXWikiContext());

        assertTrue(pageWiki2.isNew());

        // install on root

        install(this.localXarExtensiontId2, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("2.1", pageWiki1.getVersion());

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
            getXWikiContext());

        assertFalse(pageWiki1.isNew());
        assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        assertFalse(pageWiki2.isNew());
        assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
            getXWikiContext());

        assertFalse(pageWiki2.isNew());
        assertEquals("1.1", pageWiki2.getVersion());
    }

    @Test
    void installOnWikiWithOnlyAdminRight() throws Throwable
    {
        assertTrue(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext()).isNew());

        // install

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        // validate

        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext()).isNew());
    }

    @Test
    void createNewWiki() throws Throwable
    {
        doReturn(List.of("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext()).isNew());
        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext()).isNew());
        assertTrue(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("newwiki", "space1", "page1"), getXWikiContext()).isNew());

        this.observation.notify(new WikiCreatingEvent("newwiki"), null, this.oldcore.getXWikiContext());
        this.observation.notify(new WikiCreatedEvent("newwiki"), null, this.oldcore.getXWikiContext());

        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext()).isNew());
        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext()).isNew());
        assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("newwiki", "space1", "page1"), getXWikiContext()).isNew());
    }
}

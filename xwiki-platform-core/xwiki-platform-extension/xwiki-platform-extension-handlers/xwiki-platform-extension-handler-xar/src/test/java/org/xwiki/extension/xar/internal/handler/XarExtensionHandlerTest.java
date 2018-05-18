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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.extension.xar.XarExtensionConfiguration.DocumentProtection;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.util.XWikiStubContextProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@AllComponents
public class XarExtensionHandlerTest
{
    private MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private MockitoComponentManager componentManager = this.oldcore.getMocker();

    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil =
        new MockitoRepositoryUtilsRule(this.componentManager, this.oldcore);

    private XarInstalledExtensionRepository installedExtensionRepository;

    private ExtensionId localXarExtensiontId1;

    private ExtensionId localXarExtensiontId2;

    private ExtensionId collisionextension1;

    private ExtensionId collisionextension2;

    private JobExecutor jobExecutor;

    private InstalledExtensionRepository xarExtensionRepository;

    private ObservationManager observation;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    @Before
    public void setUp() throws Exception
    {
        // mock

        this.contextUser = new DocumentReference(getXWikiContext().getWikiId(), "XWiki", "ExtensionUser");

        this.localXarExtensiontId1 = new ExtensionId("test", "1.0");
        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");
        this.collisionextension1 = new ExtensionId("collisionextension1", "version");
        this.collisionextension2 = new ExtensionId("collisionextension2", "version");

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

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
        this.observation = this.repositoryUtil.getComponentManager().getInstance(ObservationManager.class);

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

    // Tests

    @Test
    public void testInstallOnWiki() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getSpyXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.mandatory");
        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);
        XWikiDocument mandatoryDocument = new XWikiDocument(new DocumentReference("wiki", "space", "mandatory"));
        mandatoryDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        mandatoryDocument.setSyntax(Syntax.PLAIN_1_0);
        mandatoryDocument.setContent("modified content");
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

        verifyHasAdminRight(2);

        // validate

        // space.page
        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(existingDocument.getDocumentReference(), getXWikiContext());

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
        Assert.assertFalse("Document is minor edit", page.isMinorEdit());

        BaseClass baseClass = page.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // space.pagewithattachment

        XWikiDocument pagewithattachment = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getXWikiContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "1.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", this.contextUser, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        Assert.assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));
        Assert.assertEquals(this.contextUser, attachment.getAuthorReference());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        assertEquals(Arrays.asList(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(page1.getDocumentReference()));
        assertEquals(Arrays.asList(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(page1.getDocumentReferenceWithLocale()));
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("wiki", "space1", "page1", Locale.ENGLISH)).size());
        assertEquals(0, this.installedExtensionRepository
            .getXarInstalledExtensions(new DocumentReference("otherwiki", "space1", "page1")).size());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getSpyXWiki().getDocument(translatedReference, getXWikiContext());

        Assert.assertNotNull("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated);
        Assert.assertFalse("Document wiki:translated.translated has not been saved in the database",
            defaultTranslated.isNew());

        Assert.assertEquals("Wrong content", "default content", defaultTranslated.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, defaultTranslated.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, defaultTranslated.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, defaultTranslated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", defaultTranslated.getVersion());

        assertEquals(Arrays.asList(xarInstalledExtension), this.installedExtensionRepository
            .getXarInstalledExtensions(defaultTranslated.getDocumentReferenceWithLocale()));

        // translated.translated.tr
        XWikiDocument translated =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("tr")));

        Assert.assertNotNull("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated);
        Assert.assertFalse("Document wiki:translated.translated in langauge tr has not been saved in the database",
            translated.isNew());

        Assert.assertEquals("Wrong content", "tr content", translated.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, translated.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, translated.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, translated.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated.getVersion());

        assertEquals(Arrays.asList(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(translated.getDocumentReferenceWithLocale()));

        // translated.translated.fr
        XWikiDocument translated2 =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("fr")));

        Assert.assertNotNull("Document wiki:translated.translated in language fr has not been saved in the database",
            translated2);
        Assert.assertFalse("Document wiki:translated.translated in langauge fr has not been saved in the database",
            translated2.isNew());

        Assert.assertEquals("Wrong content", "fr content", translated2.getContent());
        Assert.assertEquals("Wrong creator", this.contextUser, translated2.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, translated2.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, translated2.getContentAuthorReference());
        Assert.assertEquals("Wrong version", "1.1", translated2.getVersion());

        assertEquals(Arrays.asList(xarInstalledExtension),
            this.installedExtensionRepository.getXarInstalledExtensions(translated2.getDocumentReferenceWithLocale()));

        // space.hiddenpage

        XWikiDocument hiddenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getXWikiContext());

        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());

        // space.mandatory

        XWikiDocument mandatorypage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "mandatory"), getXWikiContext());

        Assert.assertEquals("Document wiki:space.mandatory has been overwritten", "1.1", mandatorypage.getVersion());

        // space.mandatoryconfiguration

        XWikiDocument mandatoryconfigurationpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "mandatoryconfiguration"), getXWikiContext());

        assertEquals("Document wiki:space.mandatoryconfiguration has not been overwritten", "2.1",
            mandatoryconfigurationpage.getVersion());

        assertTrue(
            this.installedExtensionRepository.isAllowed(mandatoryconfigurationpage.getDocumentReference(), Right.EDIT));
        assertFalse(this.installedExtensionRepository.isAllowed(mandatoryconfigurationpage.getDocumentReference(),
            Right.DELETE));
    }

    @Test
    public void testInstallOnWikiWithoutAuthor() throws Throwable
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

        XWikiDocument pagewithattachment = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithattachment"), getXWikiContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "1.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", xarCreatorReference, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference,
            pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        Assert.assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));
        Assert.assertEquals(new DocumentReference("wiki", "XWiki", "attachmentauthor"),
            attachment.getAuthorReference());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getSpyXWiki().getDocument(translatedReference, getXWikiContext());

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
        XWikiDocument translated =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("tr")));

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
        XWikiDocument translated2 =
            this.oldcore.getDocuments().get(new DocumentReference(translatedReference, new Locale("fr")));

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

        XWikiDocument hiddenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "hiddenpage"), getXWikiContext());

        Assert.assertNotNull("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage);
        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());
    }

    @Test
    public void testUpgradeOnWiki() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

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

        // upgrade

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext());

        Assert.assertFalse("Document samespace has been removed from the database", samepage.isNew());
        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content 2", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "2.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        Assert.assertEquals("Wrong customclass", "customclass2", modifiedpage.getCustomClass());
        Assert.assertEquals("Wrong defaultTemplate", "defaultTemplate2", modifiedpage.getDefaultTemplate());
        Assert.assertEquals("Wrong hidden", true, modifiedpage.isHidden());
        Assert.assertEquals("Wrong ValidationScript", "validationScript2", modifiedpage.getValidationScript());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        Assert.assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        Assert.assertFalse("Document wiki:space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertTrue("Document wiki:space1.page1 has not been removed from the database", removedPage.isNew());

        // space.deletedpage

        deletedpage = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "deletedpage"),
            getXWikiContext());

        Assert.assertTrue("Document wiki:space.deleted has been restored", deletedpage.isNew());

        // space.modifieddeletedpage

        modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "modifieddeletedpage"), getXWikiContext());

        Assert.assertTrue("Document wiki:space.modifieddeletedpage has been restored", modifieddeletedpage.isNew());

        // space.pagewithobject

        pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space", "pagewithobject"), getXWikiContext());

        Assert.assertNull("Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object",
            pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")));

        // space1.modified

        XWikiDocument space1modified = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "space1", "modified"), getXWikiContext());

        Assert.assertFalse("Document wiki:space.modified has been removed from the database", space1modified.isNew());
    }

    @Test
    public void testUpgradeOnRoot() throws Throwable
    {
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

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

        // upgrade

        install(this.localXarExtensiontId2, null, this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "samespace", "samepage"), getXWikiContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content 2", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "2.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        Assert.assertEquals("Wrong customclass", "customclass2", modifiedpage.getCustomClass());
        Assert.assertEquals("Wrong defaultTemplate", "defaultTemplate2", modifiedpage.getDefaultTemplate());
        Assert.assertEquals("Wrong hidden", true, modifiedpage.isHidden());
        Assert.assertEquals("Wrong ValidationScript", "validationScript2", modifiedpage.getValidationScript());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        XWikiAttachment attachment = modifiedpage.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentLongSize(getXWikiContext()));
        Assert.assertEquals("attachment content",
            IOUtils.toString(attachment.getContentInputStream(getXWikiContext()), StandardCharsets.UTF_8));

        // space2.page2

        XWikiDocument newPage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space2", "page2"), getXWikiContext());

        Assert.assertFalse("Document wiki:space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext());

        Assert.assertTrue("Document wiki:space1.page1 has not been removed from the database", removedPage.isNew());

        // space.deletedpage

        deletedpage = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "deletedpage"),
            getXWikiContext());

        Assert.assertTrue("Document wiki:space.deleted has been restored", deletedpage.isNew());

        // space.modifieddeletedpage

        modifieddeletedpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "modifieddeletedpage"), getXWikiContext());

        Assert.assertTrue("Document wiki:space.modifieddeletedpage has been restored", modifieddeletedpage.isNew());

        // space.pagewithobject

        pagewithobject = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "pagewithobject"), getXWikiContext());

        Assert.assertNull("Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object",
            pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")));

        // space1.modified

        XWikiDocument space1modified = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "modified"), getXWikiContext());

        Assert.assertFalse("Document wiki:space1.modified has been removed from the database", space1modified.isNew());
    }

    @Test
    public void testDowngradeOnWiki() throws Throwable
    {
        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(1);

        // upgrade

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has not been saved in the database", modifiedpage.isNew());

        Assert.assertEquals("Wrong content", "content", modifiedpage.getContent());
        Assert.assertEquals("Wrong author", this.contextUser, modifiedpage.getAuthorReference());
        Assert.assertEquals("Wrong versions", "2.1", modifiedpage.getVersion());
        Assert.assertEquals("Wrong version", Locale.ROOT, modifiedpage.getLocale());

        BaseClass baseClass = modifiedpage.getXClass();
        Assert.assertNotNull(baseClass.getField("property"));
        Assert.assertEquals("property", baseClass.getField("property").getName());
        Assert.assertSame(NumberClass.class, baseClass.getField("property").getClass());

        // The attachment does not exist in version 1.0
        Assert.assertNull(modifiedpage.getAttachment("attachment.txt"));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        Assert.assertTrue("Document wiki.space2.page2 has not been removed from the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki.space1.page1 has not been saved in the database", removedPage.isNew());
    }

    @Test
    public void testUninstallFromWiki() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);

        // validate

        // space.page belong to several extensions
        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has been removed from the database", page.isNew());

        // space1.page1 only belong to the uninstalled extension
        XWikiDocument page1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", page1.isNew());
    }

    @Test
    public void testUninstallMandatory() throws Throwable
    {
        // register a mandatory document initializer
        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.page");

        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);

        // validate

        XWikiDocument page =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has been removed from the database", page.isNew());
    }

    @Test
    public void testUninstallExtensionWithCommonDocumentOnWiki() throws Throwable
    {
        install(this.collisionextension1, "wiki", this.contextUser);
        install(this.collisionextension2, "wiki", this.contextUser);

        // uninstall

        uninstall(this.collisionextension1, "wiki");

        XWikiDocument page = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki", "samespace", "samepage"), getXWikiContext());

        assertFalse(page.isNew());
    }

    @Test
    public void testUninstallExtensionWithCommonDocumentOnRoot() throws Throwable
    {
        install(this.collisionextension1, null, this.contextUser);
        install(this.collisionextension2, null, this.contextUser);
    }

    @Test
    public void testUninstallExtensionWithCommonDocumentOnRootAndWiki() throws Throwable
    {
        install(this.collisionextension1, "wiki", this.contextUser);

        install(this.collisionextension2, null, this.contextUser);
    }

    @Test
    public void testUninstallExtensionWithCommonDocumentOnWikiAndRoot() throws Throwable
    {
        install(this.collisionextension1, null, this.contextUser);
        install(this.collisionextension2, "wiki", this.contextUser);
    }

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

        // validate

        XWikiDocument pageWiki1 = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());

        XWikiDocument pageWiki2 = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());

        XWikiDocument overwrittenpage = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space", "overwrittenpage"), getXWikiContext());

        Assert.assertFalse(overwrittenpage.isNew());
        Assert.assertEquals("1.1", overwrittenpage.getVersion());

        // uninstall

        uninstall(this.localXarExtensiontId1, null);

        verifyHasAdminRight(3);

        // validate

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space1", "page1"),
            getXWikiContext());

        Assert.assertTrue("Document wiki1:space1.page1 hasn't been removed from the database", pageWiki1.isNew());

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space1", "page1"),
            getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());
    }

    // rights check

    // install

    @Test(expected = InstallException.class)
    public void testInstallOnRootWithoutAdminRights() throws Throwable
    {
        setHasNoAdminRight();

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(1);
    }

    // uninstall

    @Test(expected = InstallException.class)
    public void testInstallOnWikiWithoutAdminRights() throws Throwable
    {
        setHasNoAdminRight();

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(1);
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
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

        setHasNoAdminRight();

        uninstall(this.localXarExtensiontId1, null);

        verifyHasAdminRight(3);
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnWikiWithoutAdminRights() throws Throwable
    {
        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        setHasNoAdminRight();

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);
    }

    @Test
    public void testInstallOnNamespaceThenOnRoot() throws Throwable
    {
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1.setContent("modified content");
        this.oldcore.getSpyXWiki().saveDocument(pageWiki1, getXWikiContext());

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("2.1", pageWiki1.getVersion());

        // install on root

        install(this.localXarExtensiontId1, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("2.1", pageWiki1.getVersion());
        // Make sure modifications are kept
        Assert.assertEquals("modified content", pageWiki1.getContent());
    }

    @Test
    public void testInstallOnNamespaceThenUpgradeOnRoot() throws Throwable
    {
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
            getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        XWikiDocument pageWiki2 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
            getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());

        // install on root

        install(this.localXarExtensiontId2, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("2.1", pageWiki1.getVersion());

        pageWiki1 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
            getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 =
            this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 = this.oldcore.getSpyXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
            getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());
        Assert.assertEquals("1.1", pageWiki2.getVersion());
    }

    @Test
    public void testInstallOnWikiWithOnlyAdminRight() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getSpyXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        // install

        install(this.localXarExtensiontId1, "wiki", this.contextUser);
    }

    @Test
    public void testCreateNewWiki() throws Throwable
    {
        doReturn(Arrays.asList("wiki1", "wiki2")).when(this.oldcore.getWikiDescriptorManager()).getAllIds();

        install(this.localXarExtensiontId1, null, this.contextUser);

        Assert.assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext()).isNew());
        Assert.assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext()).isNew());
        Assert.assertTrue(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("newwiki", "space1", "page1"), getXWikiContext()).isNew());

        this.observation.notify(new WikiCreatingEvent("newwiki"), null, this.oldcore.getXWikiContext());
        this.observation.notify(new WikiCreatedEvent("newwiki"), null, this.oldcore.getXWikiContext());

        Assert.assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki1", "space1", "page1"), getXWikiContext()).isNew());
        Assert.assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("wiki2", "space1", "page1"), getXWikiContext()).isNew());
        Assert.assertFalse(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference("newwiki", "space1", "page1"), getXWikiContext()).isNew());
    }
}

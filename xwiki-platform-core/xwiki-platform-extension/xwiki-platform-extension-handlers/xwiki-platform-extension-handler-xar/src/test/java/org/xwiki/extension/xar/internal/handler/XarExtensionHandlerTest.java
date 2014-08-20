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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.internal.InstallJob;
import org.xwiki.extension.job.internal.UninstallJob;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.util.XWikiStubContextProvider;

@AllComponents
public class XarExtensionHandlerTest
{
    private MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.componentManager);

    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule(this.componentManager,
        this.oldcore);

    private ExtensionId localXarExtensiontId1;

    private ExtensionId localXarExtensiontId2;

    private JobExecutor jobExecutor;

    private InstalledExtensionRepository xarExtensionRepository;

    private Map<String, BaseClass> classes = new HashMap<String, BaseClass>();

    private DocumentReference contextUser;

    @Before
    public void setUp() throws Exception
    {
        // mock

        this.contextUser = new DocumentReference(getXWikiContext().getWikiId(), "XWiki", "ExtensionUser");

        this.localXarExtensiontId1 = new ExtensionId("test", "1.0");
        this.localXarExtensiontId2 = new ExtensionId("test", "2.0");

        // classes

        BaseClass styleSheetClass = new BaseClass();
        this.classes.put("StyleSheetExtension", styleSheetClass);

        // checking

        when(this.oldcore.getMockXWiki().hasAttachmentRecycleBin(any(XWikiContext.class))).thenReturn(true);

        getXWikiContext().setUserReference(this.contextUser);

        ((XWikiStubContextProvider) this.componentManager.getInstance(XWikiStubContextProvider.class))
            .initialize(getXWikiContext());

        CoreConfiguration coreConfiguration = this.componentManager.getInstance(CoreConfiguration.class);
        when(coreConfiguration.getDefaultDocumentSyntax()).thenReturn(Syntax.PLAIN_1_0);

        // lookup

        this.jobExecutor = this.componentManager.getInstance(JobExecutor.class);
        this.xarExtensionRepository =
            this.componentManager.getInstance(InstalledExtensionRepository.class, XarExtensionHandler.TYPE);

        // Get rid of wiki macro listener
        this.componentManager.<ObservationManager> getInstance(ObservationManager.class).removeListener(
            "RegisterMacrosOnImportListener");
    }

    private void mockHasAdminRight(boolean right) throws XWikiException
    {
        when(
            this.oldcore.getMockAuthorizationManager().hasAccess(eq(Right.ADMIN), eq(this.contextUser),
                any(EntityReference.class))).thenReturn(right);
    }

    private void verifyHasAdminRight(int times) throws XWikiException
    {
        verify(this.oldcore.getMockAuthorizationManager(), times(times)).hasAccess(eq(Right.ADMIN),
            eq(this.contextUser), any(EntityReference.class));
    }

    private XWikiContext getXWikiContext()
    {
        return this.oldcore.getXWikiContext();
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
        mockHasAdminRight(true);

        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getMockXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.mandatory");
        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);
        XWikiDocument mandatoryDocument = new XWikiDocument(new DocumentReference("wiki", "space", "mandatory"));
        mandatoryDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        mandatoryDocument.setSyntax(Syntax.PLAIN_1_0);
        mandatoryDocument.setContent("modified content");
        this.oldcore.getMockXWiki().saveDocument(mandatoryDocument, "", true, getXWikiContext());

        // install

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // validate

        // space.page
        XWikiDocument page =
            this.oldcore.getMockXWiki().getDocument(existingDocument.getDocumentReference(), getXWikiContext());

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

        XWikiDocument pagewithattachment =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "pagewithattachment"),
                getXWikiContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "1.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", this.contextUser, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", this.contextUser, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", this.contextUser, pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getXWikiContext()));
        Assert
            .assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getXWikiContext())));
        Assert.assertEquals(this.contextUser.toString(), attachment.getAuthor());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getMockXWiki().getDocument(translatedReference, getXWikiContext());

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

        // space.hiddenpage

        XWikiDocument hiddenpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "hiddenpage"),
                getXWikiContext());

        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());

        // space.mandatory

        XWikiDocument mandatorypage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "mandatory"),
                getXWikiContext());

        Assert.assertEquals("Document wiki:space.mandatory has been overwritten", "1.1", mandatorypage.getVersion());
    }

    @Test
    public void testInstallOnWikiWithoutAuthor() throws Throwable
    {
        XWikiDocument existingDocument = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        BaseObject object = new BaseObject();
        object.setXClassReference(new DocumentReference("wiki", "space", "class"));
        existingDocument.addXObject(object);
        existingDocument.setCreatorReference(new DocumentReference("wiki", "space", "existingcreator"));
        this.oldcore.getMockXWiki().saveDocument(existingDocument, "", true, getXWikiContext());

        // install

        install(this.localXarExtensiontId1, "wiki", null);

        // validate

        DocumentReference xarAuthorReference = new DocumentReference("wiki", "XWiki", "author");
        DocumentReference xarCreatorReference = new DocumentReference("wiki", "XWiki", "creator");
        DocumentReference xarContentAuthorReference = new DocumentReference("wiki", "XWiki", "contentAuthor");

        // space.page
        XWikiDocument page =
            this.oldcore.getMockXWiki().getDocument(existingDocument.getDocumentReference(), getXWikiContext());

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
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "pagewithattachment"),
                getXWikiContext());
        Assert.assertFalse(pagewithattachment.isNew());
        Assert.assertEquals("Wrong version", "1.1", pagewithattachment.getVersion());
        Assert.assertEquals("Wrong creator", xarCreatorReference, pagewithattachment.getCreatorReference());
        Assert.assertEquals("Wrong author", xarAuthorReference, pagewithattachment.getAuthorReference());
        Assert.assertEquals("Wrong content author", xarContentAuthorReference,
            pagewithattachment.getContentAuthorReference());

        XWikiAttachment attachment = pagewithattachment.getAttachment("attachment.txt");
        Assert.assertNotNull(attachment);
        Assert.assertEquals("attachment.txt", attachment.getFilename());
        Assert.assertEquals(18, attachment.getContentSize(getXWikiContext()));
        Assert
            .assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getXWikiContext())));
        Assert.assertEquals("XWiki.attachmentauthor", attachment.getAuthor());

        // space1.page1

        XWikiDocument page1 =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki:space1.page1 has not been saved in the database", page1.isNew());

        // translated.translated
        DocumentReference translatedReference = new DocumentReference("wiki", "translated", "translated");
        XWikiDocument defaultTranslated =
            this.oldcore.getMockXWiki().getDocument(translatedReference, getXWikiContext());

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

        XWikiDocument hiddenpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "hiddenpage"),
                getXWikiContext());

        Assert.assertNotNull("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage);
        Assert.assertFalse("Document wiki:space.hiddenpage has not been saved in the database", hiddenpage.isNew());

        Assert.assertTrue("Document is not hidden", hiddenpage.isHidden());
    }

    @Test
    public void testUpgradeOnWiki() throws Throwable
    {
        mockHasAdminRight(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // Do some local modifications

        XWikiDocument deletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "deletedpage"),
                getXWikiContext());
        this.oldcore.getMockXWiki().deleteDocument(deletedpage, getXWikiContext());

        XWikiDocument modifieddeletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "modifieddeletedpage"),
                getXWikiContext());
        this.oldcore.getMockXWiki().deleteDocument(modifieddeletedpage, getXWikiContext());

        XWikiDocument pagewithobject =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "pagewithobject"),
                getXWikiContext());
        pagewithobject.removeXObjects(new LocalDocumentReference("XWiki", "XWikiGroups"));
        this.oldcore.getMockXWiki().saveDocument(pagewithobject, getXWikiContext());

        XWikiDocument deletedpagewithmodifications =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space1", "modified"),
                getXWikiContext());
        deletedpagewithmodifications.setContent("modified content");

        // upgrade

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertFalse("Document samespace has been removed from the database", samepage.isNew());
        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

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
        Assert.assertEquals(18, attachment.getContentSize(getXWikiContext()));
        Assert
            .assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getXWikiContext())));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        Assert.assertFalse("Document wiki:space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertTrue("Document wiki:space1.page1 has not been removed from the database", removedPage.isNew());

        // space.deletedpage

        deletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "deletedpage"),
                getXWikiContext());

        Assert.assertTrue("Document wiki:space.deleted has been restored", deletedpage.isNew());

        // space.modifieddeletedpage

        modifieddeletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "modifieddeletedpage"),
                getXWikiContext());

        Assert.assertTrue("Document wiki:space.modifieddeletedpage has been restored", modifieddeletedpage.isNew());

        // space.pagewithobject

        pagewithobject =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "pagewithobject"),
                getXWikiContext());

        Assert.assertNull("Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object",
            pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")));

        // space1.modified

        XWikiDocument space1modified =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space1", "modified"),
                getXWikiContext());

        Assert.assertFalse("Document wiki:space.modified has been removed from the database", space1modified.isNew());
    }

    @Test
    public void testUpgradeOnRoot() throws Throwable
    {
        when(this.oldcore.getMockXWiki().getVirtualWikisDatabaseNames(any(XWikiContext.class))).thenReturn(
            Arrays.asList("wiki1", "wiki2"));

        mockHasAdminRight(true);

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

        // Do some local modifications

        XWikiDocument deletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "deletedpage"),
                getXWikiContext());
        this.oldcore.getMockXWiki().deleteDocument(deletedpage, getXWikiContext());

        XWikiDocument modifieddeletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "modifieddeletedpage"),
                getXWikiContext());
        this.oldcore.getMockXWiki().deleteDocument(modifieddeletedpage, getXWikiContext());

        XWikiDocument pagewithobject =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "pagewithobject"),
                getXWikiContext());
        pagewithobject.removeXObjects(new LocalDocumentReference("XWiki", "XWikiGroups"));
        this.oldcore.getMockXWiki().saveDocument(pagewithobject, getXWikiContext());

        XWikiDocument deletedpagewithmodifications =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space1", "modified"),
                getXWikiContext());
        deletedpagewithmodifications.setContent("modified content");

        // upgrade

        install(this.localXarExtensiontId2, null, this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

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
        Assert.assertEquals(18, attachment.getContentSize(getXWikiContext()));
        Assert
            .assertEquals("attachment content", IOUtils.toString(attachment.getContentInputStream(getXWikiContext())));

        // space2.page2

        XWikiDocument newPage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space2", "page2"),
                getXWikiContext());

        Assert.assertFalse("Document wiki:space2.page2 has not been saved in the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space1", "page1"),
                getXWikiContext());

        Assert.assertTrue("Document wiki:space1.page1 has not been removed from the database", removedPage.isNew());

        // space.deletedpage

        deletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "deletedpage"),
                getXWikiContext());

        Assert.assertTrue("Document wiki:space.deleted has been restored", deletedpage.isNew());

        // space.modifieddeletedpage

        modifieddeletedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "modifieddeletedpage"),
                getXWikiContext());

        Assert.assertTrue("Document wiki:space.modifieddeletedpage has been restored", modifieddeletedpage.isNew());

        // space.pagewithobject

        pagewithobject =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "pagewithobject"),
                getXWikiContext());

        Assert.assertNull("Document wiki:space.pagewithobject does not contain an XWiki.XWikiGroups object",
            pagewithobject.getXObject(new LocalDocumentReference("XWiki", "XWikiGroups")));

        // space1.modified

        XWikiDocument space1modified =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space1", "modified"),
                getXWikiContext());

        Assert.assertFalse("Document wiki:space1.modified has been removed from the database", space1modified.isNew());
    }

    @Test
    public void testDowngradeOnWiki() throws Throwable
    {
        mockHasAdminRight(true);

        install(this.localXarExtensiontId2, "wiki", this.contextUser);

        verifyHasAdminRight(1);

        // upgrade

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(3);

        // validate

        // samespace.samepage

        XWikiDocument samepage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertEquals("Wrong versions", "1.1", samepage.getVersion());

        // space.page

        XWikiDocument modifiedpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

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
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space2", "page2"), getXWikiContext());

        Assert.assertTrue("Document wiki.space2.page2 has not been removed from the database", newPage.isNew());

        // space1.page1

        XWikiDocument removedPage =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertFalse("Document wiki.space1.page1 has not been saved in the database", removedPage.isNew());
    }

    @Test
    public void testUninstallFromWiki() throws Throwable
    {
        mockHasAdminRight(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);

        // validate

        XWikiDocument page =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertTrue("Document wiki.space.page has not been removed from the database", page.isNew());

        XWikiDocument page1 =
            this.oldcore.getMockXWiki()
                .getDocument(new DocumentReference("wiki", "space1", "page1"), getXWikiContext());

        Assert.assertTrue("Document wiki.space1.page1 has not been removed from the database", page1.isNew());
    }

    @Test
    public void testUninstallMandatory() throws Throwable
    {
        // register a mandatory document initializer
        MandatoryDocumentInitializer mandatoryInitializer =
            this.componentManager.registerMockComponent(MandatoryDocumentInitializer.class, "space.page");

        when(mandatoryInitializer.updateDocument(any(XWikiDocument.class))).thenReturn(true);

        mockHasAdminRight(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        // uninstall

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);

        // validate

        XWikiDocument page =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki", "space", "page"), getXWikiContext());

        Assert.assertFalse("Document wiki.space.page has been removed from the database", page.isNew());
    }

    @Test
    public void testInstallOnRoot() throws Throwable
    {
        mockHasAdminRight(true);
        when(this.oldcore.getMockXWiki().getVirtualWikisDatabaseNames(any(XWikiContext.class))).thenReturn(
            Arrays.asList("wiki1", "wiki2"));

        // install

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space1", "page1"),
                getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());

        XWikiDocument pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "space1", "page1"),
                getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());

        XWikiDocument overwrittenpage =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "overwrittenpage"),
                getXWikiContext());

        Assert.assertFalse(overwrittenpage.isNew());
        Assert.assertEquals("1.1", overwrittenpage.getVersion());

        // uninstall

        uninstall(this.localXarExtensiontId1, null);

        verifyHasAdminRight(3);

        // validate

        pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space1", "page1"),
                getXWikiContext());

        Assert.assertTrue("Document wiki1:space1.page1 hasn't been removed from the database", pageWiki1.isNew());

        pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "space1", "page1"),
                getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());
    }

    // rights check

    // install

    @Test(expected = InstallException.class)
    public void testInstallOnRootWithoutAdminRights() throws Throwable
    {
        mockHasAdminRight(false);

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(1);
    }

    // uninstall

    @Test(expected = InstallException.class)
    public void testInstallOnWikiWithoutAdminRights() throws Throwable
    {
        mockHasAdminRight(false);

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
        mockHasAdminRight(true);
        when(this.oldcore.getMockXWiki().getVirtualWikisDatabaseNames(any(XWikiContext.class))).thenReturn(
            Arrays.asList("wiki1", "wiki2"));

        install(this.localXarExtensiontId1, null, this.contextUser);

        verifyHasAdminRight(2);

        mockHasAdminRight(false);

        uninstall(this.localXarExtensiontId1, null);

        verifyHasAdminRight(3);
    }

    @Test(expected = UninstallException.class)
    public void testUninstallOnWikiWithoutAdminRights() throws Throwable
    {
        mockHasAdminRight(true);

        install(this.localXarExtensiontId1, "wiki", this.contextUser);

        verifyHasAdminRight(2);

        mockHasAdminRight(false);

        uninstall(this.localXarExtensiontId1, "wiki");

        verifyHasAdminRight(3);
    }

    @Test
    public void testInstallOnNamespaceThenOnRoot() throws Throwable
    {
        mockHasAdminRight(true);
        when(this.oldcore.getMockXWiki().getVirtualWikisDatabaseNames(any(XWikiContext.class))).thenReturn(
            Arrays.asList("wiki1", "wiki2"));

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1.setContent("modified content");
        this.oldcore.getMockXWiki().saveDocument(pageWiki1, getXWikiContext());

        // install on root

        install(this.localXarExtensiontId1, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());
    }

    @Test
    public void testInstallOnNamespaceThenUpgradeOnRoot() throws Throwable
    {
        mockHasAdminRight(true);
        when(this.oldcore.getMockXWiki().getVirtualWikisDatabaseNames(any(XWikiContext.class))).thenReturn(
            Arrays.asList("wiki1", "wiki2"));

        // install on wiki

        install(this.localXarExtensiontId1, "wiki1", this.contextUser);

        // validate

        XWikiDocument pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        XWikiDocument pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());

        pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertTrue(pageWiki2.isNew());

        // install on root

        install(this.localXarExtensiontId2, null, this.contextUser);

        // validate

        pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("2.1", pageWiki1.getVersion());

        pageWiki1 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki1", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertFalse(pageWiki1.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "space", "page"), getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());
        Assert.assertEquals("1.1", pageWiki1.getVersion());

        pageWiki2 =
            this.oldcore.getMockXWiki().getDocument(new DocumentReference("wiki2", "samespace", "samepage"),
                getXWikiContext());

        Assert.assertFalse(pageWiki2.isNew());
        Assert.assertEquals("1.1", pageWiki2.getVersion());
    }
}

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
package org.xwiki.attachment.internal.refactoring.job;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceComponentList;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.doc.ListAttachmentArchive;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import ch.qos.logback.classic.Level;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MoveAttachmentJob}. The documents the job loads, modifies and saves are actual
 * {@link XWikiDocument} instances going through the test store, so that the assertions can be made both on the calls
 * performed by the job and on what it ends up persisting.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@OldcoreTest
@UserReferenceComponentList
class MoveAttachmentJobTest
{
    private static final DocumentReference SOURCE_LOCATION = new DocumentReference("xwiki", "Space", "Source");

    private static final AttachmentReference SOURCE_ATTACHMENT_LOCATION =
        new AttachmentReference("oldName", SOURCE_LOCATION);

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "User1");

    private static final DocumentReference USER2_REFERENCE = new DocumentReference("xwiki", "XWiki", "User2");

    private static final DocumentReference TARGET_LOCATION = new DocumentReference("xwiki", "Space", "Target");

    private static final AttachmentReference TARGET_ATTACHMENT_LOCATION =
        new AttachmentReference("newName", TARGET_LOCATION);

    private static final AttachmentReference RENAMED_ATTACHMENT_LOCATION =
        new AttachmentReference("newName", SOURCE_LOCATION);

    private static final String ATTACHMENT_CONTENT = "The content of the attachment.";

    private static final String SOURCE_SAVE_COMMENT = "attachment.job.saveDocument.source [xwiki:Space.Target]";

    private static final String TARGET_SAVE_COMMENT = "attachment.job.saveDocument.target [xwiki:Space.Source]";

    private static final String IN_PLACE_SAVE_COMMENT = "attachment.job.saveDocument.inPlace [oldName, newName]";

    private static final String ROLLBACK_SAVE_COMMENT =
        "attachment.job.rollbackDocument.target [oldName, xwiki:Space.Source]";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private MoveAttachmentJob job;

    /**
     * Mocked because no translation bundle is available, so that the save comments can be asserted.
     */
    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private AttachmentsManager attachmentsManager;

    @MockComponent
    private ModelBridge modelBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private MoveAttachmentRequest request;

    /**
     * The document instance held by the store before the job runs, which the job must leave untouched since modifying
     * it would corrupt the document cache.
     */
    private XWikiDocument cachedSourceDocument;

    private UserReference authorUserReference;

    @BeforeEach
    void setUp() throws Exception
    {
        this.request = new MoveAttachmentRequest();
        this.job.initialize(this.request);

        this.authorUserReference = this.oldcore.getMocker()
            .<UserReferenceResolver<DocumentReference>>getInstance(UserReferenceResolver.TYPE_DOCUMENT_REFERENCE,
                "document")
            .resolve(AUTHOR_REFERENCE);

        // Grant global view and edit right.
        AuthorizationManager authorizationManager = this.oldcore.getMockAuthorizationManager();
        when(authorizationManager.hasAccess(eq(Right.VIEW), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);
        when(authorizationManager.hasAccess(eq(Right.EDIT), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);

        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
            "xwiki:Space.Target")).thenReturn(SOURCE_SAVE_COMMENT);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
            "xwiki:Space.Source")).thenReturn(TARGET_SAVE_COMMENT);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
            "oldName", "newName")).thenReturn(IN_PLACE_SAVE_COMMENT);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.rollbackDocument.target",
            "oldName", "xwiki:Space.Source")).thenReturn(ROLLBACK_SAVE_COMMENT);

        // Store the document holding the attachment to move.
        XWikiDocument sourceDocument = new XWikiDocument(SOURCE_LOCATION);
        XWikiAttachment attachment = new XWikiAttachment(sourceDocument, "oldName");
        attachment.setContent(new ByteArrayInputStream(ATTACHMENT_CONTENT.getBytes(UTF_8)));
        // Set the archive explicitly so that the attachment history is not loaded from the store.
        attachment.setAttachment_archive(new ListAttachmentArchive(attachment));
        sourceDocument.setAttachment(attachment);
        this.oldcore.getSpyXWiki().saveDocument(sourceDocument, this.oldcore.getXWikiContext());
        this.cachedSourceDocument = getDocument(SOURCE_LOCATION);

        // Only the documents saved by the job must be taken into account by the verifications.
        clearInvocations(this.oldcore.getSpyXWiki());
    }

    @Test
    void process() throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, AUTHOR_REFERENCE);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        XWikiDocument sourceDocument = getDocument(SOURCE_LOCATION);
        assertNull(sourceDocument.getExactAttachment("oldName"),
            "The attachment is still present in the source document.");
        // The redirection has been initialized on the source document.
        assertRedirection(sourceDocument, "xwiki:Space.Target");
        assertAuthors(sourceDocument);
        verifySave(SOURCE_LOCATION, SOURCE_SAVE_COMMENT);

        XWikiDocument targetDocument = getDocument(TARGET_LOCATION);
        assertAttachment(targetDocument, "newName");
        assertAuthors(targetDocument);
        verifySave(TARGET_LOCATION, TARGET_SAVE_COMMENT);

        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verifyRemoveExistingRedirection(TARGET_LOCATION, "newName");
        assertCachedSourceDocumentNotModified();
    }

    @Test
    void processTargetSaveFail() throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, AUTHOR_REFERENCE);
        doThrow(new XWikiException()).when(this.oldcore.getSpyXWiki())
            .saveDocument(argThat(document -> TARGET_LOCATION.equals(document.getDocumentReference())), anyString(),
                any(XWikiContext.class));

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // The source document has first been saved without the attachment, then rolled back with it.
        verifySave(SOURCE_LOCATION, SOURCE_SAVE_COMMENT);
        verify(this.oldcore.getSpyXWiki()).saveDocument(
            argThat(document -> SOURCE_LOCATION.equals(document.getDocumentReference())), eq(ROLLBACK_SAVE_COMMENT),
            eq(true), any(XWikiContext.class));

        XWikiDocument sourceDocument = getDocument(SOURCE_LOCATION);
        assertAttachment(sourceDocument, "oldName");
        assertNull(getDocument(TARGET_LOCATION).getExactAttachment("newName"),
            "The attachment has been added to the target document even though its save failed.");
        verifyRemoveExistingRedirection(TARGET_LOCATION, "newName");
        // The redirection added to the source document before the failed save of the target document must be
        // removed on rollback, since it would otherwise point to a target attachment that was never created.
        verifyRemoveExistingRedirection(SOURCE_LOCATION, "oldName");
        assertCachedSourceDocumentNotModified();

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Failed to move attachment [Attachment xwiki:Space.Source@oldName] to "
                + "[Attachment xwiki:Space.Target@newName]. Cause: [XWikiException: Error number 0 in 0]",
            this.logCapture.getMessage(0));
    }

    @Test
    void processTargetSaveFailWithoutAutoRedirect() throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, AUTHOR_REFERENCE, false);
        doThrow(new XWikiException()).when(this.oldcore.getSpyXWiki())
            .saveDocument(argThat(document -> TARGET_LOCATION.equals(document.getDocumentReference())), anyString(),
                any(XWikiContext.class));

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        XWikiDocument sourceDocument = getDocument(SOURCE_LOCATION);
        assertAttachment(sourceDocument, "oldName");
        assertNull(sourceDocument.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE),
            "A redirection has been added even though auto-redirect is disabled.");
        assertNull(getDocument(TARGET_LOCATION).getExactAttachment("newName"),
            "The attachment has been added to the target document even though its save failed.");
        verifyRemoveExistingRedirection(TARGET_LOCATION, "newName");
        // Auto-redirect is disabled, so the job never added a redirection to the source document, and none must be
        // removed from it on rollback.
        verify(this.attachmentsManager, never()).removeExistingRedirection(eq("oldName"), any(XWikiDocument.class));
        assertCachedSourceDocumentNotModified();

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "Failed to move attachment [Attachment xwiki:Space.Source@oldName] to "
                + "[Attachment xwiki:Space.Target@newName]. Cause: [XWikiException: Error number 0 in 0]",
            this.logCapture.getMessage(0));
    }

    @Test
    void processRename() throws Exception
    {
        initializeRequest(RENAMED_ATTACHMENT_LOCATION, AUTHOR_REFERENCE);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // The renamed attachment must be the only one left in the document, i.e. the rename must neither lose the
        // attachment nor keep a copy under the old name.
        XWikiDocument document = getDocument(SOURCE_LOCATION);
        assertEquals(1, document.getAttachmentList().size());
        assertNull(document.getExactAttachment("oldName"),
            "The attachment is still present in the document under its old name.");
        assertAttachment(document, "newName");
        // The redirection has been initialized on the document, which is also the destination.
        assertRedirection(document, "xwiki:Space.Source");
        assertAuthors(document);

        // Since the source and the destination are the same document, that document is saved once and no other
        // document is saved.
        verifySave(SOURCE_LOCATION, IN_PLACE_SAVE_COMMENT);
        verify(this.oldcore.getSpyXWiki(), times(1)).saveDocument(any(XWikiDocument.class), anyString(),
            any(XWikiContext.class));

        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verifyRemoveExistingRedirection(SOURCE_LOCATION, "newName");
        assertCachedSourceDocumentNotModified();
    }

    @ParameterizedTest
    @CsvSource({
        "true, true",
        "false, true",
        "true, false",
        "false, false"
    })
    void failWithoutRights(boolean canView, boolean canEdit) throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, USER2_REFERENCE);

        AuthorizationManager authorizationManager = this.oldcore.getMockAuthorizationManager();
        when(authorizationManager.hasAccess(Right.EDIT, USER2_REFERENCE, SOURCE_ATTACHMENT_LOCATION))
            .thenReturn(canEdit);
        when(authorizationManager.hasAccess(Right.VIEW, USER2_REFERENCE, SOURCE_ATTACHMENT_LOCATION))
            .thenReturn(canView);
        when(authorizationManager.hasAccess(Right.EDIT, USER2_REFERENCE, TARGET_ATTACHMENT_LOCATION))
            .thenReturn(false);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // Verify nothing has been modified.
        verify(this.oldcore.getSpyXWiki(), never()).saveDocument(any(XWikiDocument.class), anyString(),
            any(XWikiContext.class));
        assertAttachment(getDocument(SOURCE_LOCATION), "oldName");
        assertNull(getDocument(TARGET_LOCATION).getExactAttachment("newName"),
            "The attachment has been moved despite the missing rights.");
        verifyNoInteractions(this.attachmentsManager);
        assertCachedSourceDocumentNotModified();

        if (!canEdit || !canView) {
            assertEquals("You don't have sufficient permissions over the source attachment "
                + "[Attachment xwiki:Space.Source@oldName].", this.logCapture.getMessage(0));
        } else {
            assertEquals("You don't have sufficient permissions over the destination attachment "
                + "[Attachment xwiki:Space.Target@newName].", this.logCapture.getMessage(0));
        }
    }

    private void initializeRequest(AttachmentReference destination, DocumentReference userReference)
    {
        initializeRequest(destination, userReference, true);
    }

    private void initializeRequest(AttachmentReference destination, DocumentReference userReference,
        boolean autoRedirect)
    {
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, destination);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, autoRedirect);
        this.request.setInteractive(false);
        this.request.setUserReference(userReference);
        this.request.setAuthorReference(AUTHOR_REFERENCE);
    }

    private XWikiDocument getDocument(DocumentReference documentReference) throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
    }

    private void verifySave(DocumentReference documentReference, String comment) throws Exception
    {
        verify(this.oldcore.getSpyXWiki()).saveDocument(
            argThat(document -> documentReference.equals(document.getDocumentReference())), eq(comment),
            any(XWikiContext.class));
    }

    private void verifyRemoveExistingRedirection(DocumentReference documentReference, String attachmentName)
    {
        ArgumentCaptor<XWikiDocument> documentCaptor = ArgumentCaptor.forClass(XWikiDocument.class);
        verify(this.attachmentsManager).removeExistingRedirection(eq(attachmentName), documentCaptor.capture());
        assertEquals(documentReference, documentCaptor.getValue().getDocumentReference());
    }

    private void assertAuthors(XWikiDocument document)
    {
        assertEquals(this.authorUserReference, document.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(this.authorUserReference, document.getAuthors().getOriginalMetadataAuthor());
    }

    private void assertRedirection(XWikiDocument document, String targetLocation)
    {
        BaseObject redirection = document.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE);
        assertNotNull(redirection, String.format("The redirection is missing from the document [%s].",
            document.getDocumentReference()));
        assertEquals("oldName",
            redirection.getStringValue(RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD));
        assertEquals(targetLocation,
            redirection.getStringValue(RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD));
        assertEquals("newName",
            redirection.getStringValue(RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD));
    }

    private void assertAttachment(XWikiDocument document, String attachmentName) throws Exception
    {
        XWikiAttachment attachment = document.getExactAttachment(attachmentName);
        assertNotNull(attachment, String.format("The attachment [%s] is missing from the document [%s].",
            attachmentName, document.getDocumentReference()));
        assertSame(document, attachment.getDoc());
        try (InputStream contentInputStream = attachment.getContentInputStream(this.oldcore.getXWikiContext())) {
            assertEquals(ATTACHMENT_CONTENT, IOUtils.toString(contentInputStream, UTF_8));
        }
    }

    private void assertCachedSourceDocumentNotModified()
    {
        assertNotNull(this.cachedSourceDocument.getExactAttachment("oldName"),
            "The attachment has been removed from the cached document.");
        assertNull(this.cachedSourceDocument.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE),
            "The redirection has been added to the cached document.");
    }
}

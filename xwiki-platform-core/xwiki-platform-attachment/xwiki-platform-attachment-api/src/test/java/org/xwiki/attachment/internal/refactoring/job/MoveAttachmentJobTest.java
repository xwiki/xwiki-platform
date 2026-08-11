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

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.doc.ListAttachmentArchive;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import ch.qos.logback.classic.Level;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MoveAttachmentJob}. The documents the job loads, modifies and saves are actual
 * {@link XWikiDocument} instances going through the test store, so that the assertions are made on what the job
 * persists.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@OldcoreTest
@ReferenceComponentList
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

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private MoveAttachmentRequest request;

    @BeforeEach
    void setUp() throws Exception
    {
        this.request = new MoveAttachmentRequest();
        this.job.initialize(this.request);

        when(this.documentReferenceUserReferenceResolver.resolve(AUTHOR_REFERENCE))
            .thenReturn(GuestUserReference.INSTANCE);

        // Grant global view and edit right.
        AuthorizationManager authorizationManager = this.oldcore.getMockAuthorizationManager();
        when(authorizationManager.hasAccess(eq(Right.VIEW), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);
        when(authorizationManager.hasAccess(eq(Right.EDIT), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);

        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
            "xwiki:Space.Target"))
            .thenReturn("attachment.job.saveDocument.source [xwiki:Space.Target]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
            "xwiki:Space.Source"))
            .thenReturn("attachment.job.saveDocument.target [xwiki:Space.Source]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
            "oldName", "newName")).thenReturn("attachment.job.saveDocument.inPlace [oldName, newName]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.rollbackDocument.target",
            "oldName", "xwiki:Space.Source"))
            .thenReturn("attachment.job.rollbackDocument.target [oldName, xwiki:Space.Source]");

        // Store the document holding the attachment to move.
        XWikiDocument sourceDocument = new XWikiDocument(SOURCE_LOCATION);
        XWikiAttachment attachment = new XWikiAttachment(sourceDocument, "oldName");
        attachment.setContent(new ByteArrayInputStream(ATTACHMENT_CONTENT.getBytes(UTF_8)));
        // Set the archive explicitly so that the attachment history is not loaded from the store.
        attachment.setAttachment_archive(new ListAttachmentArchive(attachment));
        sourceDocument.setAttachment(attachment);
        this.oldcore.getSpyXWiki().saveDocument(sourceDocument, this.oldcore.getXWikiContext());
    }

    @Test
    void process() throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, AUTHOR_REFERENCE);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        XWikiDocument sourceDocument = getDocument(SOURCE_LOCATION);
        assertNull(sourceDocument.getExactAttachment("oldName"),
            "The attachment is still present in the source document.");
        assertEquals("attachment.job.saveDocument.source [xwiki:Space.Target]", sourceDocument.getComment());
        // The redirection has been initialized on the source document.
        assertRedirection(sourceDocument, "xwiki:Space.Target");

        XWikiDocument targetDocument = getDocument(TARGET_LOCATION);
        assertAttachmentContent(targetDocument, "newName");
        assertEquals("attachment.job.saveDocument.target [xwiki:Space.Source]", targetDocument.getComment());
        assertEquals(GuestUserReference.INSTANCE, targetDocument.getAuthors().getEffectiveMetadataAuthor());
        assertEquals(GuestUserReference.INSTANCE, sourceDocument.getAuthors().getEffectiveMetadataAuthor());

        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verify(this.attachmentsManager).removeExistingRedirection(eq("newName"), any(XWikiDocument.class));
    }

    @Test
    void processTargetSaveFail() throws Exception
    {
        initializeRequest(TARGET_ATTACHMENT_LOCATION, AUTHOR_REFERENCE);
        doThrow(new XWikiException()).when(this.oldcore.getSpyXWiki())
            .saveDocument(argThat(document -> TARGET_LOCATION.equals(document.getDocumentReference())), anyString(),
                any(XWikiContext.class));

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // The attachment has been put back in the source document.
        XWikiDocument sourceDocument = getDocument(SOURCE_LOCATION);
        assertAttachmentContent(sourceDocument, "oldName");
        assertEquals("attachment.job.rollbackDocument.target [oldName, xwiki:Space.Source]",
            sourceDocument.getComment());
        assertNull(getDocument(TARGET_LOCATION).getExactAttachment("newName"),
            "The attachment has been added to the target document even though its save failed.");

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
        assertAttachmentContent(document, "newName");
        assertEquals("attachment.job.saveDocument.inPlace [oldName, newName]", document.getComment());
        // The redirection has been initialized on the document, which is also the destination.
        assertRedirection(document, "xwiki:Space.Source");

        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verify(this.attachmentsManager).removeExistingRedirection("newName", document);
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
        assertAttachmentContent(getDocument(SOURCE_LOCATION), "oldName");
        assertNull(getDocument(TARGET_LOCATION).getExactAttachment("newName"),
            "The attachment has been moved despite the missing rights.");
        verifyNoInteractions(this.attachmentsManager);

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
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, destination);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);
        this.request.setUserReference(userReference);
        this.request.setAuthorReference(AUTHOR_REFERENCE);
    }

    private XWikiDocument getDocument(DocumentReference documentReference) throws Exception
    {
        return this.oldcore.getSpyXWiki().getDocument(documentReference, this.oldcore.getXWikiContext());
    }

    private void assertRedirection(XWikiDocument document, String expectedTargetLocation)
    {
        var redirection = document.getXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE);
        assertNotNull(redirection, String.format("The redirection is missing from the document [%s].",
            document.getDocumentReference()));
        assertEquals("oldName", redirection.getStringValue(
            RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD));
        assertEquals(expectedTargetLocation, redirection.getStringValue(
            RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD));
        assertEquals("newName", redirection.getStringValue(
            RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD));
    }

    private void assertAttachmentContent(XWikiDocument document, String attachmentName) throws Exception
    {
        XWikiAttachment attachment = document.getExactAttachment(attachmentName);
        assertNotNull(attachment,
            String.format("The attachment [%s] is missing from the document [%s].", attachmentName,
                document.getDocumentReference()));
        try (InputStream contentInputStream = attachment.getContentInputStream(this.oldcore.getXWikiContext())) {
            assertEquals(ATTACHMENT_CONTENT, IOUtils.toString(contentInputStream, UTF_8));
        }
    }
}

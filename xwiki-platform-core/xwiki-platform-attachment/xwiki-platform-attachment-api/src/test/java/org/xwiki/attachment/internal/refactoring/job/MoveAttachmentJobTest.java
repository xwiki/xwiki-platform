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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer;
import org.xwiki.attachment.refactoring.MoveAttachmentRequest;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import ch.qos.logback.classic.Level;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MoveAttachmentJob}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
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

    @InjectMockComponents
    private MoveAttachmentJob job;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @MockComponent
    private EntityReferenceSerializer<String> referenceSerializer;

    @MockComponent
    private AttachmentsManager attachmentsManager;

    @MockComponent
    private ModelBridge modelBridge;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument sourceDocument;

    @Mock
    private XWikiDocument targetDocument;

    @Mock
    private DocumentAuthors sourceAuthors;

    @Mock
    private DocumentAuthors targetAuthors;

    @Mock
    private UserReference authorReference;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private MoveAttachmentRequest request;

    @BeforeEach
    void setUp() throws Exception
    {
        this.request = new MoveAttachmentRequest();
        this.job.initialize(this.request);
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);

        // The cast is mandatory otherwise the wrong method is mocked (the DocumentReference one).
        when(this.wiki.getDocument((EntityReference) SOURCE_LOCATION, this.context)).thenReturn(this.sourceDocument);
        when(this.wiki.getDocument((EntityReference) TARGET_LOCATION, this.context)).thenReturn(this.targetDocument);
        when(this.sourceDocument.getAuthors()).thenReturn(this.sourceAuthors);
        when(this.targetDocument.getAuthors()).thenReturn(this.targetAuthors);
        when(this.sourceDocument.getDocumentReference()).thenReturn(SOURCE_LOCATION);
        when(this.targetDocument.getDocumentReference()).thenReturn(TARGET_LOCATION);
        when(this.referenceSerializer.serialize(TARGET_ATTACHMENT_LOCATION)).thenReturn("xwiki:Space.Target@newName");
        when(this.referenceSerializer.serialize(SOURCE_ATTACHMENT_LOCATION)).thenReturn("xwiki:Space.Source@oldName");
        when(this.referenceSerializer.serialize(TARGET_LOCATION)).thenReturn("xwiki:Space.Target");
        when(this.referenceSerializer.serialize(SOURCE_LOCATION)).thenReturn("xwiki:Space.Source");
        when(this.documentReferenceUserReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(this.authorReference);

        // Grant global view and edit right.
        when(this.authorizationManager.hasAccess(eq(Right.VIEW), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);
        when(this.authorizationManager.hasAccess(eq(Right.EDIT), eq(AUTHOR_REFERENCE), any(AttachmentReference.class)))
            .thenReturn(true);
    }

    @Test
    void process() throws Exception
    {
        // Request initialization.
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, TARGET_ATTACHMENT_LOCATION);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);
        this.request.setUserReference(AUTHOR_REFERENCE);
        this.request.setAuthorReference(AUTHOR_REFERENCE);

        XWikiAttachment sourceAttachment = mock(XWikiAttachment.class);
        XWikiAttachment targetAttachment = mock(XWikiAttachment.class);

        when(sourceAttachment.getFilename()).thenReturn("oldName");
        when(this.sourceDocument.getExactAttachment("oldName")).thenReturn(sourceAttachment);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
            "xwiki:Space.Target"))
            .thenReturn("attachment.job.saveDocument.source [xwiki:Space.Target]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
            "xwiki:Space.Source"))
            .thenReturn("attachment.job.saveDocument.target [xwiki:Space.Source]");
        when(sourceAttachment.clone("newName", this.context)).thenReturn(targetAttachment);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);
        verify(this.sourceDocument).removeAttachment(sourceAttachment);
        verify(this.attachmentsManager).removeExistingRedirection("newName", this.targetDocument);
        // Initialization of the redirection.
        verify(this.sourceDocument).createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, this.context);
        verify(this.wiki).saveDocument(this.sourceDocument,
            "attachment.job.saveDocument.source [xwiki:Space.Target]", this.context);
        verify(this.wiki).saveDocument(this.targetDocument,
            "attachment.job.saveDocument.target [xwiki:Space.Source]", this.context);
        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verify(this.targetAuthors).setEffectiveMetadataAuthor(this.authorReference);
        verify(this.sourceAuthors).setEffectiveMetadataAuthor(this.authorReference);
        verify(this.targetAuthors).setOriginalMetadataAuthor(this.authorReference);
        verify(this.sourceAuthors).setOriginalMetadataAuthor(this.authorReference);
        verify(targetAttachment).setDoc(this.targetDocument);
        verify(this.targetDocument).setAttachment(targetAttachment);
    }

    @Test
    void processTargetSaveFail() throws Exception
    {
        // Request initialization.
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, TARGET_ATTACHMENT_LOCATION);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);
        this.request.setUserReference(AUTHOR_REFERENCE);
        this.request.setAuthorReference(AUTHOR_REFERENCE);

        XWikiAttachment sourceAttachment = mock(XWikiAttachment.class);
        XWikiAttachment targetAttachment = mock(XWikiAttachment.class);
        XWikiAttachment newSourceAttachment = mock(XWikiAttachment.class);
        when(sourceAttachment.getFilename()).thenReturn("oldName");
        when(this.sourceDocument.getExactAttachment("oldName")).thenReturn(sourceAttachment);
        when(this.targetDocument.getExactAttachment("newName")).thenReturn(targetAttachment);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
            "xwiki:Space.Target"))
            .thenReturn("attachment.job.saveDocument.source [xwiki:Space.Target]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
            "xwiki:Space.Source"))
            .thenReturn("attachment.job.saveDocument.target [xwiki:Space.Source]");
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.rollbackDocument.target",
            "oldName", "xwiki:Space.Source")).thenReturn(
            "attachment.job.rollbackDocument.target [oldName, xwiki:Space.Source]");
        doThrow(new XWikiException()).when(this.wiki)
            .saveDocument(this.targetDocument, "attachment.job.saveDocument.target [xwiki:Space.Source]", this.context);
        when(sourceAttachment.clone("newName", this.context)).thenReturn(targetAttachment);
        when(targetAttachment.clone("oldName", this.context)).thenReturn(newSourceAttachment);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);
        verify(this.sourceDocument).removeAttachment(sourceAttachment);
        verify(this.attachmentsManager).removeExistingRedirection("newName", this.targetDocument);
        // Initialization of the redirection.
        verify(this.sourceDocument).createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, this.context);
        verify(this.wiki).saveDocument(this.sourceDocument,
            "attachment.job.saveDocument.source [xwiki:Space.Target]", this.context);
        // Check that the attachment is rolled back and saved again.
        verify(this.wiki).saveDocument(this.sourceDocument,
            "attachment.job.rollbackDocument.target [oldName, xwiki:Space.Source]", true, this.context);
        verify(targetAttachment).setDoc(this.targetDocument);
        verify(this.targetDocument).setAttachment(targetAttachment);
        verify(newSourceAttachment).setDoc(this.sourceDocument);
        verify(this.sourceDocument).setAttachment(newSourceAttachment);
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
        // Request initialization.
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION,
            new AttachmentReference("newName", SOURCE_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);
        this.request.setUserReference(AUTHOR_REFERENCE);
        this.request.setAuthorReference(AUTHOR_REFERENCE);

        XWikiAttachment sourceAttachment = mock(XWikiAttachment.class);
        XWikiAttachment targetAttachment = mock(XWikiAttachment.class);
        when(sourceAttachment.getFilename()).thenReturn("oldName");
        when(this.sourceDocument.getExactAttachment("oldName")).thenReturn(sourceAttachment);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
            "oldName", "newName")).thenReturn("attachment.job.saveDocument.inPlace [oldName, newName]");
        when(sourceAttachment.clone("newName", this.context)).thenReturn(targetAttachment);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // Since we rename inside the source, the target document must not be modified.
        verifyNoInteractions(this.targetDocument);
        verify(this.sourceDocument).removeAttachment(sourceAttachment);
        verify(this.attachmentsManager).removeExistingRedirection("newName", this.sourceDocument);
        verify(targetAttachment).setDoc(this.sourceDocument);
        verify(this.sourceDocument).setAttachment(targetAttachment);
        // Initialization of the redirection.
        verify(this.sourceDocument).createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, this.context);
        verify(this.wiki).saveDocument(this.sourceDocument, "attachment.job.saveDocument.inPlace [oldName, newName]",
            this.context);
        verify(this.modelBridge).setContextUserReference(AUTHOR_REFERENCE);
        verify(this.sourceAuthors, times(2)).setEffectiveMetadataAuthor(this.authorReference);
        verify(this.sourceAuthors, times(2)).setOriginalMetadataAuthor(this.authorReference);
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
        // Request initialization.
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, TARGET_ATTACHMENT_LOCATION);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);
        this.request.setUserReference(USER2_REFERENCE);
        this.request.setAuthorReference(AUTHOR_REFERENCE);

        when(this.authorizationManager.hasAccess(Right.EDIT, USER2_REFERENCE, SOURCE_ATTACHMENT_LOCATION))
            .thenReturn(canEdit);
        when(this.authorizationManager.hasAccess(Right.VIEW, USER2_REFERENCE, SOURCE_ATTACHMENT_LOCATION))
            .thenReturn(canView);
        when(this.authorizationManager.hasAccess(Right.EDIT, USER2_REFERENCE, TARGET_ATTACHMENT_LOCATION))
            .thenReturn(false);

        this.job.process(SOURCE_ATTACHMENT_LOCATION);

        // Verify nothing has been modified.
        verifyNoInteractions(this.sourceDocument, this.targetDocument);
        verifyNoInteractions(this.attachmentsManager);
        verify(this.wiki, never()).saveDocument(any(), any(), any());
        verifyNoInteractions(this.targetAuthors, this.sourceAuthors);

        if (!canEdit || !canView) {
            assertEquals("You don't have sufficient permissions over the source attachment "
                + "[Attachment xwiki:Space.Source@oldName].", this.logCapture.getMessage(0));
        } else {
            assertEquals("You don't have sufficient permissions over the destination attachment "
                + "[Attachment xwiki:Space.Target@newName].", this.logCapture.getMessage(0));
        }
    }
}

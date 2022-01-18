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
package org.xwiki.attachment.internal.job;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.attachment.MoveAttachmentRequest;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
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

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument sourceDocument;

    @Mock
    private XWikiDocument targetDocument;

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
        when(this.referenceSerializer.serialize(TARGET_ATTACHMENT_LOCATION)).thenReturn("xwiki:Space.Target@newName");
        when(this.referenceSerializer.serialize(SOURCE_ATTACHMENT_LOCATION)).thenReturn("xwiki:Space.Source@oldName");
    }

    @Test
    void process() throws Exception
    {
        // Request initialization.
        this.request.setEntityReferences(singletonList(SOURCE_ATTACHMENT_LOCATION));
        this.request.setProperty(MoveAttachmentRequest.DESTINATION, TARGET_ATTACHMENT_LOCATION);
        this.request.setProperty(MoveAttachmentRequest.AUTO_REDIRECT, true);
        this.request.setInteractive(false);

        XWikiAttachment sourceAttachment = mock(XWikiAttachment.class);
        when(this.sourceDocument.getAttachment("oldName")).thenReturn(sourceAttachment);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.source",
            "xwiki:Space.Target@newName"))
            .thenReturn("attachment.job.saveDocument.source [xwiki:Space.Target@newName]");

        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.target",
            "xwiki:Space.Source@oldName"))
            .thenReturn("attachment.job.saveDocument.target [xwiki:Space.Source@oldName]");
        
        this.job.process(SOURCE_ATTACHMENT_LOCATION);
        assertEquals(3, this.request.getId().size());
        assertEquals("refactoring", this.request.getId().get(0));
        assertEquals("moveAttachment", this.request.getId().get(1));
        verify(this.sourceDocument).removeAttachment(sourceAttachment);
        verify(sourceAttachment).getContentInputStream(this.context);
        verify(this.targetDocument).setAttachment("newName", null, this.context);
        verify(this.attachmentsManager).removeExistingRedirection("newName", this.targetDocument);
        // Initialization of the redirection.
        verify(this.sourceDocument).createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, this.context);
        verify(this.wiki).saveDocument(this.sourceDocument,
            "attachment.job.saveDocument.source [xwiki:Space.Target@newName]", this.context);
        verify(this.wiki).saveDocument(this.targetDocument,
            "attachment.job.saveDocument.target [xwiki:Space.Source@oldName]", this.context);
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

        XWikiAttachment sourceAttachment = mock(XWikiAttachment.class);
        when(this.sourceDocument.getAttachment("oldName")).thenReturn(sourceAttachment);
        when(this.contextualLocalizationManager.getTranslationPlain("attachment.job.saveDocument.inPlace",
            "oldName", "newName")).thenReturn("attachment.job.saveDocument.inPlace [oldName, newName]");
        this.job.process(SOURCE_ATTACHMENT_LOCATION);
        assertEquals(3, this.request.getId().size());
        assertEquals("refactoring", this.request.getId().get(0));
        assertEquals("moveAttachment", this.request.getId().get(1));

        // Since we rename inside the source, the target document must not be modified.
        verifyNoInteractions(this.targetDocument);
        verify(this.sourceDocument).removeAttachment(sourceAttachment);
        verify(sourceAttachment).getContentInputStream(this.context);
        verify(this.sourceDocument).setAttachment("newName", null, this.context);
        verify(this.attachmentsManager).removeExistingRedirection("newName", this.sourceDocument);
        // Initialization of the redirection.
        verify(this.sourceDocument).createXObject(RedirectAttachmentClassDocumentInitializer.REFERENCE, this.context);
        verify(this.wiki).saveDocument(this.sourceDocument, "attachment.job.saveDocument.inPlace [oldName, newName]",
            this.context);
    }
}

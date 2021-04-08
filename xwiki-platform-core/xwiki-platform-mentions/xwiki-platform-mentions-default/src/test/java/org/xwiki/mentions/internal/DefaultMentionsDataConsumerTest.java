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
package org.xwiki.mentions.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.internal.analyzer.CreatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.internal.analyzer.UpdatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultMentionsDataConsumer}.
 *
 * @version $Id$
 * @since 12.6
 */
@ComponentTest
class DefaultMentionsDataConsumerTest
{
    private static final String AUTHOR_REFERENCE = "xwiki:XWiki.Author";

    private static final DocumentReference AUTHOR_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "Author");

    private static final String DOCUMENT = "xwiki:XWiki.Doc";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    @InjectMockComponents
    private DefaultMentionsDataConsumer dataConsumer;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private Execution execution;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private CreatedDocumentMentionsAnalyzer createdDocumentMentionsAnalyzer;

    @MockComponent
    private UpdatedDocumentMentionsAnalyzer updatedDocumentMentionsAnalyzer;

    @Test
    void consumeVersionNotFound() throws Exception
    {
        when(this.documentReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(AUTHOR_DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1"))
            .thenReturn(null);

        this.dataConsumer.consume(new MentionsData()
            .setAuthorReference(AUTHOR_REFERENCE)
            .setDocumentReference(DOCUMENT)
            .setVersion("1.1"));

        verifyNoInteractions(this.observationManager);
        verify(this.execution).removeContext();
    }

    @Test
    void consumeCreatedDocumentNoMentions() throws Exception
    {
        when(this.documentReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(AUTHOR_DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1"))
            .thenReturn(doc);
        when(doc.getPreviousVersion()).thenReturn(null);
        when(doc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.createdDocumentMentionsAnalyzer.analyze(doc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE))
            .thenReturn(emptyList());

        this.dataConsumer.consume(new MentionsData()
            .setAuthorReference(AUTHOR_REFERENCE)
            .setDocumentReference(DOCUMENT)
            .setVersion("1.1"));

        verifyNoInteractions(this.updatedDocumentMentionsAnalyzer);
        verify(this.createdDocumentMentionsAnalyzer)
            .analyze(doc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE);
        verifyNoInteractions(this.observationManager);
        verify(this.execution).removeContext();
    }

    @Test
    void consumeCreatedDocumentNewMentions() throws Exception
    {
        when(this.documentReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(AUTHOR_DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1"))
            .thenReturn(doc);
        when(doc.getPreviousVersion()).thenReturn(null);
        when(doc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(AUTHOR_REFERENCE, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT,
                "1.1")
                .addNewMention("user", new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1", DisplayStyle.FIRST_NAME));
        when(this.createdDocumentMentionsAnalyzer.analyze(doc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE))
            .thenReturn(singletonList(
                mentionNotificationParameters));

        this.dataConsumer.consume(new MentionsData()
            .setAuthorReference(AUTHOR_REFERENCE)
            .setDocumentReference(DOCUMENT)
            .setVersion("1.1"));

        verifyNoInteractions(this.updatedDocumentMentionsAnalyzer);
        verify(this.createdDocumentMentionsAnalyzer)
            .analyze(doc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE);
        verify(this.observationManager)
            .notify(isA(NewMentionsEvent.class), eq(AUTHOR_REFERENCE), eq(mentionNotificationParameters));
        verify(this.execution).removeContext();
    }

    @Test
    void consumeUpdatedDocumentNoMentions() throws Exception
    {
        when(this.documentReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(AUTHOR_DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.0"))
            .thenReturn(oldDoc);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1"))
            .thenReturn(newDoc);

        when(newDoc.getPreviousVersion()).thenReturn("1.0");
        when(newDoc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE))
            .thenReturn(emptyList());

        this.dataConsumer.consume(new MentionsData()
            .setAuthorReference(AUTHOR_REFERENCE)
            .setDocumentReference(DOCUMENT)
            .setVersion("1.1"));

        verifyNoInteractions(this.createdDocumentMentionsAnalyzer);
        verify(this.updatedDocumentMentionsAnalyzer)
            .analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE);
        verifyNoInteractions(this.observationManager);
        verify(this.execution).removeContext();
    }

    @Test
    void consumeUpdatedDocumentNewMentions() throws Exception
    {
        when(this.documentReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(AUTHOR_DOCUMENT_REFERENCE);
        when(this.documentReferenceResolver.resolve(DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.0"))
            .thenReturn(oldDoc);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1"))
            .thenReturn(newDoc);
        when(newDoc.getPreviousVersion()).thenReturn("1.0");
        when(newDoc.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(AUTHOR_REFERENCE, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT,
                "1.1")
                .addNewMention("user", new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1", DisplayStyle.FIRST_NAME));
        when(this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE))
            .thenReturn(singletonList(
                mentionNotificationParameters));

        this.dataConsumer.consume(new MentionsData()
            .setAuthorReference(AUTHOR_REFERENCE)
            .setDocumentReference(DOCUMENT)
            .setVersion("1.1"));

        verifyNoInteractions(this.createdDocumentMentionsAnalyzer);
        verify(this.updatedDocumentMentionsAnalyzer)
            .analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR_REFERENCE);
        verify(this.observationManager)
            .notify(isA(NewMentionsEvent.class), eq(AUTHOR_REFERENCE), eq(mentionNotificationParameters));
        verify(this.execution).removeContext();
    }
}

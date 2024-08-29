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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.internal.analyzer.CreatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.internal.analyzer.UpdatedDocumentMentionsAnalyzer;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

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
    private static final String EFFECTIVE_METADATA_AUTHOR_REFERENCE = "xwiki:XWiki.EffectiveAuthor";

    private static final String ORIGINAL_METADATA_AUTHOR_REFERENCE = "xwiki:XWiki.OriginalAuthor";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    @InjectMockComponents
    private DefaultMentionsDataConsumer dataConsumer;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private CreatedDocumentMentionsAnalyzer createdDocumentMentionsAnalyzer;

    @MockComponent
    private UpdatedDocumentMentionsAnalyzer updatedDocumentMentionsAnalyzer;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Mock
    private XWikiDocument doc;

    @Mock
    private UserReference effectiveMetadataAuthorReference;

    @Mock
    private UserReference originalMetadataAuthorReference;

    @BeforeEach
    void setUp()
    {
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        when(this.doc.getAuthors()).thenReturn(documentAuthors);
        when(documentAuthors.getEffectiveMetadataAuthor()).thenReturn(effectiveMetadataAuthorReference);
        when(documentAuthors.getOriginalMetadataAuthor()).thenReturn(originalMetadataAuthorReference);
        when(this.userReferenceSerializer.serialize(effectiveMetadataAuthorReference))
            .thenReturn(EFFECTIVE_METADATA_AUTHOR_REFERENCE);
        when(this.userReferenceSerializer.serialize(originalMetadataAuthorReference))
            .thenReturn(ORIGINAL_METADATA_AUTHOR_REFERENCE);
        when(this.doc.getVersion()).thenReturn("1.1");
    }

    @Test
    void consumeVersionNotFound() throws Exception
    {
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1")).thenReturn(null);

        this.dataConsumer.consume(DOCUMENT_REFERENCE, "1.1");

        verifyNoInteractions(this.observationManager);
    }

    @Test
    void consumeCreatedDocumentNoMentions() throws Exception
    {
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1")).thenReturn(this.doc);
        when(this.doc.getPreviousVersion()).thenReturn(null);
        when(this.doc.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        when(this.createdDocumentMentionsAnalyzer.analyze(this.doc, DOCUMENT_REFERENCE, "1.1",
            ORIGINAL_METADATA_AUTHOR_REFERENCE))
            .thenReturn(emptyList());

        this.dataConsumer.consume(DOCUMENT_REFERENCE, "1.1");

        verifyNoInteractions(this.updatedDocumentMentionsAnalyzer);
        verify(this.createdDocumentMentionsAnalyzer).analyze(this.doc, DOCUMENT_REFERENCE, "1.1",
            ORIGINAL_METADATA_AUTHOR_REFERENCE);
        verifyNoInteractions(this.observationManager);
    }

    @Test
    void consumeCreatedDocumentNewMentions() throws Exception
    {
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1")).thenReturn(this.doc);
        when(this.doc.getPreviousVersion()).thenReturn(null);
        when(this.doc.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(ORIGINAL_METADATA_AUTHOR_REFERENCE, DOCUMENT_REFERENCE,
                MentionLocation.DOCUMENT, "1.1")
                .addNewMention("user",
                    new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1", DisplayStyle.FIRST_NAME));
        when(this.createdDocumentMentionsAnalyzer.analyze(this.doc, DOCUMENT_REFERENCE, "1.1",
            ORIGINAL_METADATA_AUTHOR_REFERENCE))
            .thenReturn(singletonList(mentionNotificationParameters));

        this.dataConsumer.consume(DOCUMENT_REFERENCE, "1.1");

        verifyNoInteractions(this.updatedDocumentMentionsAnalyzer);
        verify(this.createdDocumentMentionsAnalyzer)
            .analyze(this.doc, DOCUMENT_REFERENCE, "1.1", ORIGINAL_METADATA_AUTHOR_REFERENCE);
        verify(this.observationManager)
            .notify(isA(NewMentionsEvent.class), eq(ORIGINAL_METADATA_AUTHOR_REFERENCE),
                eq(mentionNotificationParameters));
    }

    @Test
    void consumeUpdatedDocumentNoMentions() throws Exception
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.0")).thenReturn(oldDoc);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1")).thenReturn(this.doc);

        when(this.doc.getPreviousVersion()).thenReturn("1.0");
        when(this.doc.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        when(
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, this.doc, DOCUMENT_REFERENCE, "1.1",
                ORIGINAL_METADATA_AUTHOR_REFERENCE))
            .thenReturn(emptyList());

        this.dataConsumer.consume(DOCUMENT_REFERENCE, "1.1");

        verifyNoInteractions(this.createdDocumentMentionsAnalyzer);
        verify(this.updatedDocumentMentionsAnalyzer).analyze(oldDoc, this.doc, DOCUMENT_REFERENCE, "1.1",
            ORIGINAL_METADATA_AUTHOR_REFERENCE);
        verifyNoInteractions(this.observationManager);
    }

    @Test
    void consumeUpdatedDocumentNewMentions() throws Exception
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.0")).thenReturn(oldDoc);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, "1.1")).thenReturn(this.doc);
        when(this.doc.getPreviousVersion()).thenReturn("1.0");
        when(this.doc.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(ORIGINAL_METADATA_AUTHOR_REFERENCE, DOCUMENT_REFERENCE,
                MentionLocation.DOCUMENT, "1.1")
                .addNewMention("user",
                    new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1", DisplayStyle.FIRST_NAME));
        when(
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, this.doc, DOCUMENT_REFERENCE, "1.1",
                ORIGINAL_METADATA_AUTHOR_REFERENCE))
            .thenReturn(singletonList(mentionNotificationParameters));

        this.dataConsumer.consume(DOCUMENT_REFERENCE, "1.1");

        verifyNoInteractions(this.createdDocumentMentionsAnalyzer);
        verify(this.updatedDocumentMentionsAnalyzer)
            .analyze(oldDoc, this.doc, DOCUMENT_REFERENCE, "1.1", ORIGINAL_METADATA_AUTHOR_REFERENCE);
        verify(this.observationManager)
            .notify(isA(NewMentionsEvent.class), eq(ORIGINAL_METADATA_AUTHOR_REFERENCE),
                eq(mentionNotificationParameters));
    }
}

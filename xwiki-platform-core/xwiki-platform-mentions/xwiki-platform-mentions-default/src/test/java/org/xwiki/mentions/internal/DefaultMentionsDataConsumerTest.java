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
//    private static final DocumentReference COMMENTS_DOCUMENT_REFERENCE =
//        new DocumentReference("xwiki", "XWiki", "XWikiComments");

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
                .addNewMention("user", new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1"));
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
                .addNewMention("user", new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1"));
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

//    @Test
//    void consumeCreate() throws Exception
//    {
//        String user1 = "xwiki:XWiki.U1";
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "U2");
//        String authorReferenceId = "xwiki:XWiki.U2";
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//        Map<String, String> mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U1");
//        mentionParams.put("anchor", "anchor1");
//        MacroBlock mention1 = new MacroBlock("mention", mentionParams, false);
//
//        mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U2");
//        MacroBlock mention2 = new MacroBlock("mention", mentionParams, false);
//
//        mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U1");
//        mentionParams.put("anchor", "anchor2");
//        MacroBlock mention3 = new MacroBlock("mention", mentionParams, false);
//
//        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
//            new NewLineBlock(),
//            new MacroBlock("macro0", new HashMap<>(), false),
//            new NewLineBlock(),
//            mention1,
//            mention2,
//            mention3
//        ))));
//
//        List<MacroBlock> mentions = asList(mention1, mention2, mention3);
//        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);
//
//        Map<MentionedActorReference, List<String>> value = new HashMap<>();
//        value.put(new MentionedActorReference(user1, null), asList("anchor1", "anchor2"));
//        value.put(new MentionedActorReference(authorReferenceId, null), asList("", null));
//        when(this.xdomService.groupAnchorsByUserReference(mentions)).thenReturn(value);
//        when(this.documentReferenceResolver.resolve(authorReferenceId)).thenReturn(documentAuthorReference);
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
//        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "5.8")).thenReturn(xWikiDocument);
//        when(xWikiDocument.getDocumentReference()).thenReturn(documentReference);
//        when(xWikiDocument.getAuthorReference()).thenReturn(documentAuthorReference);
//        when(xWikiDocument.getXDOM()).thenReturn(xdom);
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn(authorReferenceId);
//
//        this.dataConsumer.consume(
//            new MentionsData()
//                .setVersion("5.8")
//                .setWikiId("xwiki")
//                .setDocumentReference("xwiki:XWiki.Doc")
//                .setAuthorReference(authorReferenceId));
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq(authorReferenceId),
//                eq(new MentionNotificationParameters(authorReferenceId, documentReference, DOCUMENT, "5.8")
//                    .addNewMention(null, new MentionNotificationParameter(user1, "anchor1"))
//                    .addNewMention(null, new MentionNotificationParameter(user1, "anchor2"))
//                    .addNewMention(null, new MentionNotificationParameter(authorReferenceId, "")))
//            );
//    }
//
//    @Test
//    void consumeCreateString() throws Exception
//    {
//        String user1 = "xwiki:XWiki.U1";
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "U2");
//        String authorReferenceId = "xwiki:XWiki.U2";
//
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//        Map<String, String> mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U1");
//        mentionParams.put("anchor", "anchor1");
//        MacroBlock mention1 = new MacroBlock("mention", mentionParams, false);
//
//        mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U2");
//        MacroBlock mention2 = new MacroBlock("mention", mentionParams, false);
//
//        mentionParams = new HashMap<>();
//        mentionParams.put("reference", "XWiki.U1");
//        mentionParams.put("anchor", "anchor2");
//        MacroBlock mention3 = new MacroBlock("mention", mentionParams, false);
//
//        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
//            new NewLineBlock(),
//            new MacroBlock("macro0", new HashMap<>(), false),
//            new NewLineBlock(),
//            mention1,
//            mention2,
//            mention3
//        ))));
//
//        List<MacroBlock> mentions = asList(mention1, mention2, mention3);
//        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);
//
//        Map<MentionedActorReference, List<String>> value = new HashMap<>();
//        value.put(new MentionedActorReference(user1, null), asList("anchor1", "anchor2"));
//        value.put(new MentionedActorReference(authorReferenceId, null), asList("", null));
//        when(this.xdomService.groupAnchorsByUserReference(mentions))
//            .thenReturn(value);
//        when(this.xdomService.parse("some content with mentions", XWIKI_2_1)).thenReturn(Optional.of(xdom));
//        when(this.documentReferenceResolver.resolve(authorReferenceId)).thenReturn(documentAuthorReference);
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
//        XWikiDocument doc = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(doc);
//        when(doc.getDocumentReference()).thenReturn(documentReference);
//        when(doc.getAuthorReference()).thenReturn(documentAuthorReference);
//        when(doc.getXDOM()).thenReturn(xdom);
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn(authorReferenceId);
//
//        this.dataConsumer.consume(
//            new MentionsData()
//                .setVersion("1.2")
//                .setWikiId("xwiki")
//                .setDocumentReference("xwiki:XWiki.Doc")
//                .setAuthorReference(authorReferenceId));
//
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq(authorReferenceId),
//                eq(new MentionNotificationParameters(authorReferenceId, documentReference, DOCUMENT, "1.2")
//                    .addNewMention(null, new MentionNotificationParameter(user1, "anchor1"))
//                    .addNewMention(null, new MentionNotificationParameter(user1, "anchor2"))
//                    .addNewMention(null, new MentionNotificationParameter(authorReferenceId, "")))
//            );
//    }
//
//    @Test
//    void consumeCreateNoMention() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "U2");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
//            new NewLineBlock(),
//            new MacroBlock("macro0", new HashMap<>(), false),
//            new NewLineBlock()
//
//        ))));
//        when(this.xdomService.listMentionMacros(xdom)).thenReturn(emptyList());
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.U2")).thenReturn(documentAuthorReference);
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.1")).thenReturn(mock(XWikiDocument.class));
//
//        XWikiDocument doc = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.1")).thenReturn(doc);
//        when(doc.getDocumentReference()).thenReturn(documentReference);
//        when(doc.getAuthorReference()).thenReturn(documentAuthorReference);
//        when(doc.getXDOM()).thenReturn(xdom);
//
//        this.dataConsumer.consume(
//            new MentionsData()
//                .setVersion("1.1")
//                .setWikiId("xwiki")
//                .setDocumentReference("xwiki:XWiki.Doc")
//                .setAuthorReference("xwiki:XWiki.U2"));
//
//        verify(this.observationManager, never()).notify(any(), any(), any());
//    }
//
//    @Test
//    void consumeUpdate() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "Author");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        BaseObject newComment = buildUpdateNewComment(documentReference);
//        BaseObject oldComment = buildUpdateOldComment();
//        XDOM newCommentXDOM = buildNewCommentXDOM();
//        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
//        initUserU1(documentAuthorReference, documentReference, newCommentNewMentions);
//        initUpdatedNewDocument(documentAuthorReference, documentReference, singletonList(newComment), "5.2");
//        initUpdatedOldDocument(documentReference, singletonList(oldComment));
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn("xwiki:XWiki.Author");
//
//        this.dataConsumer.consume(buildDefaultUpdateMentionData("5.2"));
//
//        MentionNotificationParameter mentionNotificationParameter =
//            new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1");
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq("xwiki:XWiki.Author"),
//                eq(new MentionNotificationParameters("xwiki:XWiki.Author",
//                    new ObjectPropertyReference("comment",
//                        new ObjectReference("XWiki.XWikiComments", documentReference)), COMMENT, "5.2")
//                    .addNewMention(null, mentionNotificationParameter)
//                    .addMention(null, mentionNotificationParameter))
//            );
//    }
//
//    @Test
//    void consumeUpdateBaseObjectNull() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "Creator");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        BaseObject newComment = buildUpdateNewComment(documentReference);
//        BaseObject oldComment = buildUpdateOldComment();
//        XDOM newCommentXDOM = buildNewCommentXDOM();
//        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
//        initUserU1(documentAuthorReference, documentReference, newCommentNewMentions);
//        initUpdatedNewDocument(documentAuthorReference, documentReference, asList(null, newComment), "3.2");
//        initUpdatedOldDocument(documentReference, singletonList(oldComment));
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn("xwiki:XWiki.Creator");
//
//        this.dataConsumer.consume(buildDefaultUpdateMentionData("3.2"));
//
//        MentionNotificationParameter mentionNotificationParameter =
//            new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1");
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq("xwiki:XWiki.Creator"),
//                eq(new MentionNotificationParameters("xwiki:XWiki.Creator",
//                    new ObjectPropertyReference("comment",
//                        new ObjectReference("XWiki.XWikiComments", documentReference)), COMMENT, "3.2")
//                    .addMention(null, mentionNotificationParameter)
//                    .addNewMention(null, mentionNotificationParameter))
//            );
//    }
//
//    @Test
//    void consumeUpdateDocumentNull() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "Creator");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        BaseObject newComment = buildUpdateNewComment(documentReference);
//        BaseObject oldComment = buildUpdateOldComment();
//        XDOM newCommentXDOM = buildNewCommentXDOM();
//        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
//        initUserU1(documentAuthorReference, documentReference, newCommentNewMentions);
//        initUpdatedNewDocument(documentAuthorReference, documentReference, singletonList(newComment), "1.5");
//        initUpdatedOldDocument(documentReference, asList(null, oldComment, null));
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn("xwiki:XWiki.Creator");
//
//        this.dataConsumer.consume(buildDefaultUpdateMentionData("1.5"));
//
//        MentionNotificationParameter mentionNotificationParameter =
//            new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1");
//        verify(this.observationManager).notify(any(NewMentionsEvent.class), eq("xwiki:XWiki.Creator"),
//            eq(new MentionNotificationParameters("xwiki:XWiki.Creator",
//                new ObjectPropertyReference("comment", new ObjectReference("XWiki.XWikiComments", documentReference)),
//                COMMENT, "1.5")
//                .addMention(null, mentionNotificationParameter)
//                .addNewMention(null, mentionNotificationParameter))
//        );
//    }
//
//    @Test
//    void consumeUpdateMissing() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "Creator");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        BaseStringProperty value = new BaseStringProperty();
//        value.setValue("annotation");
//
//        BaseObject newComment = initComment(value, documentReference);
//
//        XDOM newCommentXDOM = new XDOM(emptyList());
//        when(this.xdomService.parse("COMMENT 1 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(newCommentXDOM));
//        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
//
//        initUserU1(documentAuthorReference, documentReference, newCommentNewMentions);
//
//        XWikiDocument doc = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "14.1")).thenReturn(doc);
//        when(doc.getDocumentReference()).thenReturn(documentReference);
//        when(doc.getAuthorReference()).thenReturn(documentAuthorReference);
//        when(doc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        when(doc.getPreviousVersion()).thenReturn("1.2");
//        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
//        newXObjects.put(documentReference, singletonList(newComment));
//        when(doc.getXObjects()).thenReturn(newXObjects);
//
//        XWikiDocument oldDoc = mock(XWikiDocument.class);
//        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(oldDoc);
//        when(oldDoc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
//        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn("xwiki:XWiki.Creator");
//
//        this.dataConsumer.consume(
//            new MentionsData()
//                .setVersion("14.1")
//                .setWikiId("xwiki")
//                .setDocumentReference("xwiki:XWiki.Doc")
//                .setAuthorReference("xwiki:XWiki.Creator"));
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq("xwiki:XWiki.Creator"),
//                eq(new MentionNotificationParameters("xwiki:XWiki.Creator", new ObjectPropertyReference("comment",
//                    new ObjectReference("XWiki.XWikiComments", documentReference)), ANNOTATION, "14.1")
//                    .addNewMention(null, new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1")))
//            );
//    }
//
//    @Test
//    void consumeUpdateMissingAnnotationNull() throws Exception
//    {
//        DocumentReference documentAuthorReference = new DocumentReference("xwiki", "XWiki", "Creator");
//        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
//
//        BaseObject newComment = initComment(null, documentReference);
//
//        XDOM newCommentXDOM = new XDOM(emptyList());
//        when(this.xdomService.parse("COMMENT 1 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(newCommentXDOM));
//        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
//
//        initUserU1(documentAuthorReference, documentReference, newCommentNewMentions);
//
//        XWikiDocument doc = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "5.2")).thenReturn(doc);
//        when(doc.getDocumentReference()).thenReturn(documentReference);
//        when(doc.getAuthorReference()).thenReturn(documentAuthorReference);
//        when(doc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        when(doc.getPreviousVersion()).thenReturn("1.2");
//        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
//        newXObjects.put(documentReference, singletonList(newComment));
//        when(doc.getXObjects()).thenReturn(newXObjects);
//
//        XWikiDocument oldDoc = mock(XWikiDocument.class);
//        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(oldDoc);
//        when(oldDoc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
//        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
//        when(this.entityReferenceSerializer.serialize(documentAuthorReference)).thenReturn("xwiki:XWiki.Creator");
//
//        this.dataConsumer.consume(
//            new MentionsData()
//                .setVersion("5.2")
//                .setWikiId("xwiki")
//                .setDocumentReference("xwiki:XWiki.Doc")
//                .setAuthorReference("xwiki:XWiki.Creator"));
//        MentionNotificationParameter anchor1 = new MentionNotificationParameter("xwiki:XWiki.U1", "anchor1");
//        verify(this.observationManager)
//            .notify(any(NewMentionsEvent.class), eq("xwiki:XWiki.Creator"),
//                eq(new MentionNotificationParameters("xwiki:XWiki.Creator", new ObjectPropertyReference("comment",
//                    new ObjectReference("XWiki.XWikiComments", documentReference)), COMMENT, "5.2")
//                    .addNewMention(null, anchor1))
//            );
//    }
//
//    @Test
//    void consumeDocumentNull() throws Exception
//    {
//        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Creator")).thenReturn(authorReference);
//        when(this.documentRevisionProvider.getRevision(any(DocumentReference.class), any(String.class)))
//            .thenReturn(null);
//        this.dataConsumer.consume(buildDefaultUpdateMentionData("1.0"));
//
//        verify(this.xdomService, never()).groupAnchorsByUserReference(any());
//        verify(this.xdomService, never()).listMentionMacros(any());
//        verify(this.xdomService, never()).parse(any(), any());
//        verify(this.observationManager, never()).notify(any(), any(), any());
//    }
//
//    private void initUserU1(DocumentReference authorReference, DocumentReference documentReference,
//        List<MacroBlock> newCommentNewMentions)
//    {
//        Map<MentionedActorReference, List<String>> mentionsCount = new HashMap<>();
//        mentionsCount.put(new MentionedActorReference("xwiki:XWiki.U1", null), singletonList("anchor1"));
//        when(this.xdomService.groupAnchorsByUserReference(newCommentNewMentions))
//            .thenReturn(mentionsCount);
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Creator")).thenReturn(authorReference);
//        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
//    }
//
//    private MentionsData buildDefaultUpdateMentionData(String version)
//    {
//        return new MentionsData()
//            .setAuthorReference("xwiki:XWiki.Creator")
//            .setDocumentReference("xwiki:XWiki.Doc")
//            .setWikiId("xwiki")
//            .setVersion(version);
//    }
//
//    private XDOM buildNewCommentXDOM()
//    {
//        XDOM newCommentXDOM = new XDOM(singletonList(new MacroBlock("mention", new HashMap<>(), false)));
//        XDOM oldCommentXDOM = new XDOM(emptyList());
//        when(this.xdomService.parse("COMMENT 1 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(newCommentXDOM));
//        when(this.xdomService.parse("COMMENT 0 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(oldCommentXDOM));
//        return newCommentXDOM;
//    }
//
//    private BaseObject buildUpdateNewComment(DocumentReference documentReference)
//    {
//        BaseObject newComment = mock(BaseObject.class);
//        when(newComment.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
//        LargeStringProperty newCommentLSP = new LargeStringProperty();
//        newCommentLSP.setValue("COMMENT 1 CONTENT");
//        newCommentLSP.setName("comment");
//        BaseObject object = mock(BaseObject.class);
//        when(object.getReference()).thenReturn(
//            new BaseObjectReference(new ObjectReference("XWiki.XWikiComments", documentReference), documentReference));
//        when(object.getField(SELECTION_FIELD)).thenReturn(new BaseStringProperty());
//        newCommentLSP.setObject(object);
//        when(newComment.getField("comment")).thenReturn(newCommentLSP);
//        return newComment;
//    }
//
//    private void initUpdatedOldDocument(DocumentReference documentReference, List<BaseObject> baseObjects)
//        throws XWikiException
//    {
//        XWikiDocument oldDoc = mock(XWikiDocument.class);
//        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(oldDoc);
//        when(oldDoc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
//        oldXObjects.put(documentReference, baseObjects);
//        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
//    }
//
//    private BaseObject buildUpdateOldComment()
//    {
//        BaseObject oldComment = mock(BaseObject.class);
//        LargeStringProperty oldCommentLSP = new LargeStringProperty();
//        oldCommentLSP.setValue("COMMENT 0 CONTENT");
//        oldCommentLSP.setName("old comment");
//        when(oldComment.getField("comment")).thenReturn(oldCommentLSP);
//        return oldComment;
//    }
//
//    private void initUpdatedNewDocument(DocumentReference authorReference, DocumentReference documentReference,
//        List<BaseObject> baseObjects, String revision) throws XWikiException
//    {
//        XWikiDocument doc = mock(XWikiDocument.class);
//        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
//        when(this.documentRevisionProvider.getRevision(documentReference, revision)).thenReturn(doc);
//        when(doc.getDocumentReference()).thenReturn(documentReference);
//        when(doc.getAuthorReference()).thenReturn(authorReference);
//        when(doc.getXDOM()).thenReturn(new XDOM(emptyList()));
//        when(doc.getPreviousVersion()).thenReturn("1.2");
//        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
//        newXObjects.put(documentReference, baseObjects);
//        when(doc.getXObjects()).thenReturn(newXObjects);
//    }
//
//    private List<MacroBlock> initNewCommentMentions(XDOM newCommentXDOM)
//    {
//        Map<String, String> parameters = new HashMap<>();
//        parameters.put("reference", "XWiki.U1");
//        List<MacroBlock> newCommentNewMentions = singletonList(new MacroBlock("comment", parameters, false));
//        when(this.xdomService.listMentionMacros(newCommentXDOM)).thenReturn(newCommentNewMentions);
//        return newCommentNewMentions;
//    }
//
//    private BaseObject initComment(BaseStringProperty value, DocumentReference documentReference)
//    {
//        BaseObject newComment = mock(BaseObject.class);
//        when(newComment.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
//        LargeStringProperty newCommentLSP = new LargeStringProperty();
//        newCommentLSP.setValue("COMMENT 1 CONTENT");
//        newCommentLSP.setName("comment");
//        BaseObject object = mock(BaseObject.class);
//        when(object.getField(SELECTION_FIELD)).thenReturn(value);
//        when(object.getReference()).thenReturn(
//            new BaseObjectReference(new ObjectReference("XWiki.XWikiComments", documentReference), documentReference));
//        newCommentLSP.setObject(object);
//        when(newComment.getField("comment")).thenReturn(newCommentLSP);
//        return newComment;
//    }
}

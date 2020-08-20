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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.internal.async.MentionsData;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Test of {@link DefaultMentionsDataConsumer}.
 *
 * @version $Id$
 * @since 12.6
 */
@ComponentTest
class DefaultMentionsDataConsumerTest
{
    private static final DocumentReference COMMENTS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiComments");

    @InjectMockComponents
    private DefaultMentionsDataConsumer dataConsumer;

    @MockComponent
    private MentionNotificationService notificationService;

    @MockComponent
    private MentionXDOMService xdomService;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @Test
    void consumeCreate() throws Exception
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        Map<String, String> mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U1");
        mentionParams.put("anchor", "anchor1");
        MacroBlock mention1 = new MacroBlock("mention", mentionParams, false);

        mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U2");
        MacroBlock mention2 = new MacroBlock("mention", mentionParams, false);

        mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U1");
        mentionParams.put("anchor", "anchor2");
        MacroBlock mention3 = new MacroBlock("mention", mentionParams, false);

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock(),
            mention1,
            mention2,
            mention3
        ))));

        List<MacroBlock> mentions = asList(mention1, mention2, mention3);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);

        Map<DocumentReference, List<String>> value = new HashMap<>();
        value.put(user1, asList("anchor1", "anchor2"));
        value.put(authorReference, asList("", null));
        when(this.xdomService.countByIdentifier(mentions)).thenReturn(value);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.U2")).thenReturn(authorReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
        XWikiDocument mock = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.0")).thenReturn(mock);
        when(mock.getDocumentReference()).thenReturn(documentReference);
        when(mock.getAuthorReference()).thenReturn(authorReference);
        when(mock.getXDOM()).thenReturn(xdom);

        this.dataConsumer.consume(
            new MentionsData()
                .setVersion("1.0")
                .setWikiId("xwiki")
                .setDocumentReference("xwiki:XWiki.Doc")
                .setAuthorReference("xwiki:XWiki.U2"));
        verify(this.notificationService)
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, user1, DOCUMENT, "anchor1",
                    xdom));
        verify(this.notificationService)
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, user1, DOCUMENT, "anchor2",
                    xdom));
        verify(this.notificationService, times(1))
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, authorReference, DOCUMENT, "",
                    xdom));
    }

    @Test
    void consumeCreateString() throws Exception
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        Map<String, String> mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U1");
        mentionParams.put("anchor", "anchor1");
        MacroBlock mention1 = new MacroBlock("mention", mentionParams, false);

        mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U2");
        MacroBlock mention2 = new MacroBlock("mention", mentionParams, false);

        mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U1");
        mentionParams.put("anchor", "anchor2");
        MacroBlock mention3 = new MacroBlock("mention", mentionParams, false);

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock(),
            mention1,
            mention2,
            mention3
        ))));

        List<MacroBlock> mentions = asList(mention1, mention2, mention3);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);

        Map<DocumentReference, List<String>> value = new HashMap<>();
        value.put(user1, asList("anchor1", "anchor2"));
        value.put(authorReference, asList("", null));
        when(this.xdomService.countByIdentifier(mentions)).thenReturn(value);
        when(this.xdomService.parse("some content with mentions", XWIKI_2_1)).thenReturn(Optional.of(xdom));
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.U2")).thenReturn(authorReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.0")).thenReturn(doc);
        when(doc.getDocumentReference()).thenReturn(documentReference);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        when(doc.getXDOM()).thenReturn(xdom);

        this.dataConsumer.consume(
            new MentionsData()
                .setVersion("1.0")
                .setWikiId("xwiki")
                .setDocumentReference("xwiki:XWiki.Doc")
                .setAuthorReference("xwiki:XWiki.U2"));

        verify(this.notificationService)
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, user1, DOCUMENT, "anchor1",
                    xdom));
        verify(this.notificationService)
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, user1, DOCUMENT, "anchor2",
                    xdom));
        verify(this.notificationService, times(1))
            .sendNotification(
                new MentionNotificationParameters(authorReference, documentReference, authorReference, DOCUMENT, "",
                    xdom));
    }

    @Test
    void consumeCreateNoMention() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock()

        ))));
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(emptyList());
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.U2")).thenReturn(authorReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.0")).thenReturn(mock(XWikiDocument.class));

        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.1")).thenReturn(doc);
        when(doc.getDocumentReference()).thenReturn(documentReference);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        when(doc.getXDOM()).thenReturn(xdom);

        this.dataConsumer.consume(
            new MentionsData()
                .setVersion("1.1")
                .setWikiId("xwiki")
                .setDocumentReference("xwiki:XWiki.Doc")
                .setAuthorReference("xwiki:XWiki.U2"));

        verify(this.notificationService, never()).sendNotification(any());
    }

    @Test
    void consumeUpdate() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        BaseObject newComment = buildUpdateNewComment();
        BaseObject oldComment = buildUpdateOldComment();
        XDOM newCommentXDOM = buildNewCommentXDOM();
        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
        DocumentReference userU1 = initUserU1(authorReference, documentReference, newCommentNewMentions);
        initUpdatedNewDocument(authorReference, documentReference, singletonList(newComment));
        initUpdatedOldDocument(documentReference, singletonList(oldComment));

        this.dataConsumer.consume(buildDefaultUpdateMentionData());

        verify(this.notificationService).sendNotification(
            new MentionNotificationParameters(authorReference, documentReference, userU1, COMMENT,
                "anchor1",
                newCommentXDOM));
    }

    @Test
    void consumeUpdateBaseObjectNull() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        BaseObject newComment = buildUpdateNewComment();
        BaseObject oldComment = buildUpdateOldComment();
        XDOM newCommentXDOM = buildNewCommentXDOM();
        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
        DocumentReference userU1 = initUserU1(authorReference, documentReference, newCommentNewMentions);
        initUpdatedNewDocument(authorReference, documentReference, asList(null, newComment));
        initUpdatedOldDocument(documentReference, singletonList(oldComment));

        this.dataConsumer.consume(buildDefaultUpdateMentionData());

        verify(this.notificationService).sendNotification(
            new MentionNotificationParameters(authorReference, documentReference, userU1, COMMENT,
                "anchor1",
                newCommentXDOM));
    }

    @Test
    void consumeUpdateDocumentNull() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        BaseObject newComment = buildUpdateNewComment();
        BaseObject oldComment = buildUpdateOldComment();
        XDOM newCommentXDOM = buildNewCommentXDOM();
        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);
        DocumentReference userU1 = initUserU1(authorReference, documentReference, newCommentNewMentions);
        initUpdatedNewDocument(authorReference, documentReference, singletonList(newComment));
        initUpdatedOldDocument(documentReference, asList(null, oldComment, null));

        this.dataConsumer.consume(buildDefaultUpdateMentionData());

        verify(this.notificationService).sendNotification(
            new MentionNotificationParameters(authorReference, documentReference, userU1, COMMENT,
                "anchor1",
                newCommentXDOM));
    }

    @Test
    void consumeUpdateMissing() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        BaseObject newComment = mock(BaseObject.class);
        when(newComment.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
        LargeStringProperty newCommentLSP = new LargeStringProperty();
        newCommentLSP.setValue("COMMENT 1 CONTENT");
        Map fields = new HashMap();
        BaseStringProperty value = new BaseStringProperty();
        value.setValue("annotation");
        fields.put(SELECTION_FIELD, value);
        BaseObject object = new BaseObject();
        object.setFields(fields);
        newCommentLSP.setObject(object);
        when(newComment.getField("comment")).thenReturn(newCommentLSP);
        Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<>();
        xObjects.put(new DocumentReference("xwiki", "XWiki", "NewComment"), singletonList(newComment));

        XDOM newCommentXDOM = new XDOM(emptyList());
        when(this.xdomService.parse("COMMENT 1 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(newCommentXDOM));
        List<MacroBlock> newCommentNewMentions = initNewCommentMentions(newCommentXDOM);

        DocumentReference userU1 = initUserU1(authorReference, documentReference, newCommentNewMentions);

        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, "3.4")).thenReturn(doc);
        when(doc.getDocumentReference()).thenReturn(documentReference);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        when(doc.getXDOM()).thenReturn(new XDOM(emptyList()));
        when(doc.getPreviousVersion()).thenReturn("1.2");
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        newXObjects.put(documentReference, singletonList(newComment));
        when(doc.getXObjects()).thenReturn(newXObjects);

        XWikiDocument oldDoc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(oldDoc);
        when(oldDoc.getXDOM()).thenReturn(new XDOM(emptyList()));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);

        this.dataConsumer.consume(
            new MentionsData()
                .setVersion("3.4")
                .setWikiId("xwiki")
                .setDocumentReference("xwiki:XWiki.Doc")
                .setAuthorReference("xwiki:XWiki.Creator"));
        MentionNotificationParameters anchor1 =
            new MentionNotificationParameters(authorReference, documentReference, userU1, ANNOTATION,
                "anchor1", newCommentXDOM);
        verify(this.notificationService).sendNotification(anchor1);
    }

    @Test
    void consumeDocumentNull() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Creator")).thenReturn(authorReference);
        when(this.documentRevisionProvider.getRevision(any(DocumentReference.class), any(String.class)))
            .thenReturn(null);
        this.dataConsumer.consume(buildDefaultUpdateMentionData()
        );

        verify(this.xdomService, never()).countByIdentifier(any());
        verify(this.xdomService, never()).listMentionMacros(any());
        verify(this.xdomService, never()).parse(any(), any());
        verify(this.notificationService, never()).sendNotification(any());
    }

    private DocumentReference initUserU1(DocumentReference authorReference, DocumentReference documentReference,
        List<MacroBlock> newCommentNewMentions)
    {
        DocumentReference documentReference1 = new DocumentReference("xwiki", "XWiki", "U1");
        Map<DocumentReference, List<String>> mentionsCount = new HashMap<>();
        mentionsCount.put(documentReference1, Collections.singletonList("anchor1"));
        when(this.xdomService.countByIdentifier(newCommentNewMentions)).thenReturn(mentionsCount);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Creator")).thenReturn(authorReference);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.Doc")).thenReturn(documentReference);
        return documentReference1;
    }

    private MentionsData buildDefaultUpdateMentionData()
    {
        return new MentionsData()
                   .setAuthorReference("xwiki:XWiki.Creator")
                   .setDocumentReference("xwiki:XWiki.Doc")
                   .setWikiId("xwiki")
                   .setVersion("1.3");
    }

    private XDOM buildNewCommentXDOM()
    {
        XDOM newCommentXDOM = new XDOM(singletonList(new MacroBlock("mention", new HashMap<>(), false)));
        XDOM oldCommentXDOM = new XDOM(emptyList());
        when(this.xdomService.parse("COMMENT 1 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(newCommentXDOM));
        when(this.xdomService.parse("COMMENT 0 CONTENT", XWIKI_2_1)).thenReturn(Optional.of(oldCommentXDOM));
        return newCommentXDOM;
    }

    private BaseObject buildUpdateNewComment()
    {
        BaseObject newComment = mock(BaseObject.class);
        when(newComment.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
        LargeStringProperty newCommentLSP = new LargeStringProperty();
        newCommentLSP.setValue("COMMENT 1 CONTENT");
        newCommentLSP.setName("comment");
        BaseObject object = new BaseObject();
        Map fields = new HashMap();
        fields.put(SELECTION_FIELD, new BaseStringProperty());
        object.setFields(fields);
        newCommentLSP.setObject(object);
        when(newComment.getField("comment")).thenReturn(newCommentLSP);
        return newComment;
    }

    private void initUpdatedOldDocument(DocumentReference documentReference, List<BaseObject> baseObjects)
        throws XWikiException
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.2")).thenReturn(oldDoc);
        when(oldDoc.getXDOM()).thenReturn(new XDOM(emptyList()));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        oldXObjects.put(documentReference, baseObjects);
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
    }

    private BaseObject buildUpdateOldComment()
    {
        BaseObject oldComment = mock(BaseObject.class);
        LargeStringProperty oldCommentLSP = new LargeStringProperty();
        oldCommentLSP.setValue("COMMENT 0 CONTENT");
        when(oldComment.getField("comment")).thenReturn(oldCommentLSP);
        return oldComment;
    }

    private void initUpdatedNewDocument(DocumentReference authorReference, DocumentReference documentReference,
        List<BaseObject> baseObjects) throws XWikiException
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
        when(this.documentRevisionProvider.getRevision(documentReference, "1.3")).thenReturn(doc);
        when(doc.getDocumentReference()).thenReturn(documentReference);
        when(doc.getAuthorReference()).thenReturn(authorReference);
        when(doc.getXDOM()).thenReturn(new XDOM(emptyList()));
        when(doc.getPreviousVersion()).thenReturn("1.2");
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        newXObjects.put(documentReference, baseObjects);
        when(doc.getXObjects()).thenReturn(newXObjects);
    }

    private List<MacroBlock> initNewCommentMentions(XDOM newCommentXDOM)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", "XWiki.U1");
        List<MacroBlock> newCommentNewMentions = singletonList(new MacroBlock("comment", parameters, false));
        when(this.xdomService.listMentionMacros(newCommentXDOM)).thenReturn(newCommentNewMentions);
        return newCommentNewMentions;
    }
}

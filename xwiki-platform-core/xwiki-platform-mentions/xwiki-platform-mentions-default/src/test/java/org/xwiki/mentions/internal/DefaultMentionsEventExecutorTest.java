package org.xwiki.mentions.internal;/*
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.internal.async.MentionsThreadPoolExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.NewLineBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Test of {@link DefaultMentionsEventExecutor}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class DefaultMentionsEventExecutorTest
{
    @InjectMockComponents
    private DefaultMentionsEventExecutor executor;

    @MockComponent
    private MentionNotificationService notificationService;

    @MockComponent
    private MentionXDOMService xdomService;

    @Mock
    private XWikiDocument document;

    @BeforeEach
    void setUp()
    {
        MentionsThreadPoolExecutor threadPoolExecutor = mock(MentionsThreadPoolExecutor.class);
        this.executor.setExecutor(threadPoolExecutor);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(threadPoolExecutor).execute(any());
    }

    @Test
    void executeCreate()
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        HashMap<String, String> mentionParams = new HashMap<>();
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

        List<MacroBlock> mentions = Arrays.asList(mention1, mention2, mention3);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);

        Map<DocumentReference, List<String>> value = new HashMap<>();
        value.put(user1, Arrays.asList("anchor1", "anchor2"));
        value.put(authorReference, Arrays.asList("", null));
        when(this.xdomService.countByIdentifier(mentions)).thenReturn(value);

        this.executor.executeCreate(xdom, authorReference, documentReference, DOCUMENT);
        verify(this.notificationService).sendNotif(authorReference, documentReference, user1, DOCUMENT, "anchor1");
        verify(this.notificationService).sendNotif(authorReference, documentReference, user1, DOCUMENT, "anchor2");
        verify(this.notificationService, times(1)).sendNotif(authorReference, documentReference, authorReference,
            DOCUMENT, "");
    }

    @Test
    void executeCreateString()
    {
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "U1");
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        HashMap<String, String> mentionParams = new HashMap<>();
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

        List<MacroBlock> mentions = Arrays.asList(mention1, mention2, mention3);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);
        //
        Map<DocumentReference, List<String>> value = new HashMap<>();
        value.put(user1, Arrays.asList("anchor1", "anchor2"));
        value.put(authorReference, Arrays.asList("", null));
        when(this.xdomService.countByIdentifier(mentions)).thenReturn(value);
        when(this.xdomService.parse("some content with mentions")).thenReturn(Optional.of(xdom));

        this.executor.executeCreate("some content with mentions", authorReference, documentReference, DOCUMENT);

        verify(this.notificationService).sendNotif(authorReference, documentReference, user1, DOCUMENT, "anchor1");
        verify(this.notificationService).sendNotif(authorReference, documentReference, user1, DOCUMENT, "anchor2");
        verify(this.notificationService, times(1)).sendNotif(authorReference, documentReference, authorReference,
            DOCUMENT, "");
    }

    @Test
    void executeCreateNoMention()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock()

        ))));
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(emptyList());

        this.executor.executeCreate(xdom, authorReference, documentReference, DOCUMENT);

        verify(this.notificationService, never()).sendNotif(any(), any(), any(), any(), any());
    }

    @Test
    void executeUpdate()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference commentDocRef = new DocumentReference("xwiki", "XWiki", "TheComment");

        BaseObject newComment = mock(BaseObject.class);
        when(newComment.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        LargeStringProperty newCommentLSP = new LargeStringProperty();
        newCommentLSP.setValue("COMMENT 1 CONTENT");
        newCommentLSP.setName("comment");
        BaseObject object = new BaseObject();
        Map fields = new HashMap();
        fields.put(SELECTION_FIELD, new BaseStringProperty());
        object.setFields(fields);
        newCommentLSP.setObject(object);
        when(newComment.getField("comment")).thenReturn(newCommentLSP);

        BaseObject oldComment = mock(BaseObject.class);
        LargeStringProperty oldCommentLSP = new LargeStringProperty();
        oldCommentLSP.setValue("COMMENT 0 CONTENT");
        when(oldComment.getField("comment")).thenReturn(oldCommentLSP);

        XDOM newCommentXDOM = new XDOM(singletonList(new MacroBlock("mention", new HashMap<>(), false)));
        XDOM oldCommentXDOM = new XDOM(emptyList());
        when(this.xdomService.parse("COMMENT 1 CONTENT")).thenReturn(Optional.of(newCommentXDOM));
        when(this.xdomService.parse("COMMENT 0 CONTENT")).thenReturn(Optional.of(oldCommentXDOM));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", "XWiki.U1");
        List<MacroBlock> newCommentNewMentions = singletonList(new MacroBlock("comment", parameters, false));
        when(this.xdomService.listMentionMacros(newCommentXDOM)).thenReturn(newCommentNewMentions);

        DocumentReference U1 = new DocumentReference("xwiki", "XWiki", "U1");
        Map<DocumentReference, List<String>> mentionsCount = new HashMap<>();
        mentionsCount.put(U1, Collections.singletonList("anchor1"));
        when(this.xdomService.countByIdentifier(newCommentNewMentions)).thenReturn(mentionsCount);

        this.executor.executeUpdate(oldCommentXDOM, newCommentXDOM, authorReference, documentReference, COMMENT);

        verify(this.notificationService).sendNotif(authorReference, documentReference, U1, COMMENT, "anchor1");
    }

    @Test
    void executeUpdateMissing()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        BaseObject newComment = mock(BaseObject.class);
        when(newComment.getXClassReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
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
        when(this.xdomService.parse("COMMENT 1 CONTENT")).thenReturn(Optional.of(newCommentXDOM));
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", "XWiki.U1");
        List<MacroBlock> newCommentNewMentions = singletonList(new MacroBlock("comment", parameters, false));
        when(this.xdomService.listMentionMacros(newCommentXDOM)).thenReturn(newCommentNewMentions);

        DocumentReference U1 = new DocumentReference("xwiki", "XWiki", "U1");
        Map<DocumentReference, List<String>> mentionsCount = new HashMap<>();
        mentionsCount.put(U1, Collections.singletonList("anchor1"));
        when(this.xdomService.countByIdentifier(newCommentNewMentions)).thenReturn(mentionsCount);

        this.executor.executeUpdate(null, newCommentXDOM, authorReference, documentReference, ANNOTATION);

        verify(this.notificationService).sendNotif(authorReference, documentReference, U1, ANNOTATION, "anchor1");
    }
}
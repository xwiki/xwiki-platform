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
package org.xwiki.mentions.internal.async.jobs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.internal.MentionXDOMService;
import org.xwiki.mentions.internal.async.MentionsUpdatedRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Test of {@link MentionsUpdateJob}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class MentionsUpdateJobTest
{
    @InjectMockComponents
    private MentionsUpdateJob job;

    @Mock
    private XWikiDocument newDocument;

    @Mock
    private XWikiDocument oldDocument;

    @Mock
    private XWikiContext context;

    @MockComponent
    private MentionXDOMService xdomService;

    @MockComponent
    private MentionNotificationService notificationService;

    @Test
    void runInternalNewMention()
    {
        XDOM dom1Mention = new XDOM(singletonList(new IdBlock("ID DOM1")));
        XDOM dom2Mentions = new XDOM(singletonList(new IdBlock("ID DOM2")));
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Creator");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        DocumentReference u1 = new DocumentReference("xwiki", "XWiki", "u1");
        DocumentReference u2 = new DocumentReference("xwiki", "XWiki", "u2");

        when(this.context.getDoc()).thenReturn(this.oldDocument);
        when(this.oldDocument.getXDOM()).thenReturn(dom1Mention);
        when(this.newDocument.getXDOM()).thenReturn(dom2Mentions);

        when(this.newDocument.getAuthorReference()).thenReturn(authorReference);
        when(this.newDocument.getDocumentReference()).thenReturn(documentReference);

        List<MacroBlock> l1mention = mock(List.class);
        when(this.xdomService.listMentionMacros(dom1Mention)).thenReturn(l1mention);

        List<MacroBlock> l2mentions = mock(List.class);
        when(this.xdomService.listMentionMacros(dom2Mentions)).thenReturn(l2mentions);

        Map<DocumentReference, List<String>> mentionsCountL1 = new HashMap<>();
        mentionsCountL1.put(u1, Arrays.asList("", "anchor1"));
        mentionsCountL1.put(u2, Arrays.asList("anchor1", null));
        when(this.xdomService.countByIdentifier(l1mention)).thenReturn(mentionsCountL1);

        Map<DocumentReference, List<String>> mentionsCountL2 = new HashMap<>();
        mentionsCountL2.put(u1, Arrays.asList("", "anchor1", null, "anchor2"));
        mentionsCountL2.put(u2, Collections.singletonList("anchor2"));
        when(this.xdomService.countByIdentifier(l2mentions)).thenReturn(mentionsCountL2);
        
        this.job.initialize(new MentionsUpdatedRequest(this.newDocument, this.oldDocument, authorReference));
        this.job.runInternal();

        verify(this.notificationService).sendNotif(authorReference, documentReference, u1, DOCUMENT, "");
        verify(this.notificationService).sendNotif(authorReference, documentReference, u1, DOCUMENT, "anchor2");
        verify(this.notificationService).sendNotif(authorReference, documentReference, u2, DOCUMENT, "anchor2");
    }

    @Test
    void runInternalNewComment()
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
        when(this.newDocument.getXObjects()).thenReturn(xObjects);
        when(this.newDocument.getDocumentReference()).thenReturn(documentReference);

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
       

        this.job.initialize(new MentionsUpdatedRequest(this.newDocument, this.oldDocument, authorReference));
        this.job.runInternal();

        verify(this.notificationService).sendNotif(authorReference, documentReference, U1, ANNOTATION, "anchor1");
    }

    @Test
    void runInternalEditComment()
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
        Map<DocumentReference, List<BaseObject>> newDocXObjects = new HashMap<>();
        newDocXObjects.put(commentDocRef, singletonList(newComment));
        when(this.newDocument.getXObjects()).thenReturn(newDocXObjects);
        when(this.newDocument.getDocumentReference()).thenReturn(documentReference);

        BaseObject oldComment = mock(BaseObject.class);
        LargeStringProperty oldCommentLSP = new LargeStringProperty();
        oldCommentLSP.setValue("COMMENT 0 CONTENT");
        when(oldComment.getField("comment")).thenReturn(oldCommentLSP);
        
        Map<DocumentReference, List<BaseObject>> oldDocXObjects = new HashMap<>();
        oldDocXObjects.put(commentDocRef, singletonList(oldComment));
        when(this.oldDocument.getXObjects()).thenReturn(oldDocXObjects);

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

        this.job.initialize(new MentionsUpdatedRequest(this.newDocument, this.oldDocument, authorReference));
        this.job.runInternal();

        verify(this.notificationService).sendNotif(authorReference, documentReference, U1, COMMENT, "anchor1");
    }

    @Test
    void getType()
    {
        assertEquals("mentions-update-job", this.job.getType());
    }
}
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
import org.xwiki.mentions.internal.async.MentionsCreatedRequest;
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
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;

/**
 * Test of {@link MentionsCreateJob}
 *
 * @version $Id$
 * @since 12.5RC1
 */
@ComponentTest
public class MentionsCreateJobTest
{
    @InjectMockComponents
    private MentionsCreateJob job;

    @Mock
    private XWikiDocument document;

    @MockComponent
    private MentionXDOMService xdomService;

    @MockComponent
    private MentionNotificationService notificationService;

    @Test
    void runInternal()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");
        HashMap<String, String> mentionParams = new HashMap<>();
        mentionParams.put("reference", "XWiki.U1");
        mentionParams.put("anchor", "anchor1");
        MacroBlock mention = new MacroBlock("mention", mentionParams, false);
        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock(),
            mention
        ))));

        when(this.document.getAuthorReference()).thenReturn(authorReference);
        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.document.getXDOM()).thenReturn(xdom);
        List<MacroBlock> mentions = singletonList(mention);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(mentions);
        DocumentReference user1 = new DocumentReference("xwiki", "XWiki", "U1");
        Map<DocumentReference, List<String>> value = new HashMap<>();
        value.put(user1, Collections.singletonList("anchor1"));
        when(this.xdomService.countByIdentifier(mentions)).thenReturn(value);

        this.job.initialize(new MentionsCreatedRequest(this.document));
        this.job.runInternal();

        verify(this.notificationService).sendNotif(authorReference, documentReference, user1, DOCUMENT, "anchor1");
    }

    @Test
    void runInternalNoMention()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock()

        ))));
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(emptyList());

        when(this.document.getAuthorReference()).thenReturn(authorReference);
        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.document.getXDOM()).thenReturn(xdom);

        this.job.initialize(new MentionsCreatedRequest(this.document));
        this.job.runInternal();

        verify(this.notificationService, never()).sendNotif(any(), any(), any(), any(), any());
    }

    @Test
    void runInternalAWMFields()
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "U2");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Doc");

        XDOM xdom = new XDOM(singletonList(new ParagraphBlock(asList(
            new NewLineBlock(),
            new MacroBlock("macro0", new HashMap<>(), false),
            new NewLineBlock()

        ))));
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(emptyList());

        when(this.document.getAuthorReference()).thenReturn(authorReference);
        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.document.getXDOM()).thenReturn(xdom);
        Map<DocumentReference, List<BaseObject>> xObjects = new HashMap<>();
        BaseObject baseObject = new BaseObject();
        baseObject.addField("f1", new DateProperty());
        LargeStringProperty element = new LargeStringProperty();
        element.setValue("CONTENT 1");
        baseObject.addField("f2", element);

        BaseObject baseObject2 = new BaseObject();
        LargeStringProperty element1 = new LargeStringProperty();
        element1.setValue("CONTENT 2");
        baseObject2.addField("f3", element1);
        xObjects.put(documentReference, Arrays.asList(baseObject, baseObject2));
        when(this.document.getXObjects()).thenReturn(xObjects);

        HashMap<String, String> parameters = new HashMap<>();
        parameters.put("reference", "XWiki.User");
        MacroBlock mention = new MacroBlock("mention", parameters, false);
        List<MacroBlock> mentionsBlocks = singletonList(mention);
        XDOM xdom1 = new XDOM(mentionsBlocks);
        XDOM xdom2 = new XDOM(singletonList(new MacroBlock("macro0", new HashMap<>(), false)));
        when(this.xdomService.parse("CONTENT 1")).thenReturn(Optional.of(xdom1));
        when(this.xdomService.parse("CONTENT 2")).thenReturn(Optional.of(xdom2));

        when(this.xdomService.listMentionMacros(xdom1)).thenReturn(mentionsBlocks);
        when(this.xdomService.listMentionMacros(xdom2)).thenReturn(emptyList());

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
        Map<DocumentReference, List<String>> mentionsCount = new HashMap<>();
        mentionsCount.put(user, Collections.singletonList("anchor1"));
        when(this.xdomService.countByIdentifier(mentionsBlocks)).thenReturn(mentionsCount);

        this.job.initialize(new MentionsCreatedRequest(this.document));
        this.job.runInternal();

        verify(this.xdomService).parse("CONTENT 1");
        verify(this.xdomService).parse("CONTENT 2");
        verify(this.xdomService).listMentionMacros(xdom1);
        verify(this.xdomService).listMentionMacros(xdom2);
        verify(this.xdomService).countByIdentifier(mentionsBlocks);
        verify(this.notificationService).sendNotif(authorReference, documentReference, user, AWM_FIELD, "anchor1");
    }

    @Test
    void getType()
    {
        assertEquals("mentions-create-job", this.job.getType());
    }
}
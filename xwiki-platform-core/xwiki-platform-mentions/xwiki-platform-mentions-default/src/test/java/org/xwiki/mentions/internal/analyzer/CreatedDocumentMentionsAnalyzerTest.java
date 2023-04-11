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
package org.xwiki.mentions.internal.analyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.internal.MentionXDOMService;
import org.xwiki.mentions.internal.MentionedActorReference;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.LargeStringProperty;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.DisplayStyle.FIRST_NAME;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Test of {@link CreatedDocumentMentionsAnalyzer}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class CreatedDocumentMentionsAnalyzerTest
{
    private static final DocumentReference COMMENTS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiComments");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String AUTHOR = "xwiki:XWiki.Author";

    private static final String USER_U1 = "xwiki:XWiki.U1";

    @InjectMockComponents
    private CreatedDocumentMentionsAnalyzer createdDocumentMentionsAnalyzer;

    @MockComponent
    private MentionXDOMService xdomService;

    @Test
    void analyzeNoMentionFound()
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
        XDOM xdom = new XDOM(asList());
        when(doc.getXDOM()).thenReturn(xdom);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(asList());
        when(this.xdomService.groupAnchorsByUserReference(asList())).thenReturn(new HashMap<>());

        Map<DocumentReference, List<BaseObject>> mapBaseObjects = new HashMap<>();
        BaseObject baseObject = new BaseObject();
        baseObject.setDocumentReference(DOCUMENT_REFERENCE);
        LargeStringProperty largeStringProperty = new LargeStringProperty();
        largeStringProperty.setObject(baseObject);
        largeStringProperty.setValue("SOME CONTENT");
        XDOM xdomComment = new XDOM(asList());
        when(this.xdomService.parse("SOME CONTENT", XWIKI_2_1)).thenReturn(Optional.of(xdomComment));
        List<MacroBlock> commentBLock = asList();
        when(this.xdomService.listMentionMacros(xdomComment)).thenReturn(commentBLock);
        when(this.xdomService.groupAnchorsByUserReference(commentBLock)).thenReturn(new HashMap<>());
        baseObject.addField("comment", largeStringProperty);
        baseObject.addField("height", new FloatProperty());
        mapBaseObjects.put(null, asList(baseObject));

        when(doc.getXObjects()).thenReturn(mapBaseObjects);

        List<MentionNotificationParameters> actual =
            this.createdDocumentMentionsAnalyzer.analyze(doc, DOCUMENT_REFERENCE, "1.0", AUTHOR);

        assertEquals(asList(), actual);
    }

    @Test
    void analyzeNewMentionInBody()
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
        XDOM xdom = new XDOM(asList());
        when(doc.getXDOM()).thenReturn(xdom);
        List<MacroBlock> blocks = asList(buildMentionMacro(USER_U1, "anchor0", FIRST_NAME));
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(blocks);
        Map<MentionedActorReference, List<String>> mapAnchors = new HashMap<>();
        mapAnchors.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(blocks)).thenReturn(mapAnchors);

        List<MentionNotificationParameters> actual =
            this.createdDocumentMentionsAnalyzer.analyze(doc, DOCUMENT_REFERENCE, "1.0", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT, "1.0")
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor0", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor0", FIRST_NAME))
        ), actual);
    }

    @Test
    void analyzeNewMentionInXObject()
    {
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getSyntax()).thenReturn(XWIKI_2_1);
        XDOM xdom = new XDOM(asList());
        when(doc.getXDOM()).thenReturn(xdom);
        when(this.xdomService.listMentionMacros(xdom)).thenReturn(asList());
        when(this.xdomService.groupAnchorsByUserReference(asList())).thenReturn(new HashMap<>());

        Map<DocumentReference, List<BaseObject>> mapBaseObjects = new HashMap<>();
        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE),
                DOCUMENT_REFERENCE);
        when(baseObject.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty largeStringProperty = new LargeStringProperty();
        largeStringProperty.setObject(baseObject);
        largeStringProperty.setValue("SOME CONTENT");
        largeStringProperty.setName("comment");
        when(baseObject.getProperties()).thenReturn(new Object[] { largeStringProperty, new FloatProperty() });
        MacroBlock mentionComment1 = buildMentionMacro(USER_U1, "anchor1", FIRST_NAME);
        MacroBlock mentionComment2 = buildMentionMacro(USER_U1, "anchor2", FIRST_NAME);
        XDOM xdomComment = new XDOM(asList(mentionComment1, mentionComment2));
        when(this.xdomService.parse("SOME CONTENT", XWIKI_2_1)).thenReturn(Optional.of(xdomComment));
        List<MacroBlock> commentBLock = asList(mentionComment1, mentionComment2);
        when(this.xdomService.listMentionMacros(xdomComment)).thenReturn(commentBLock);
        Map<MentionedActorReference, List<String>> mapMentionsComment = new HashMap<>();
        mapMentionsComment.put(new MentionedActorReference(USER_U1, "user"), asList("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(commentBLock)).thenReturn(mapMentionsComment);
        mapBaseObjects.put(null, asList(baseObject));

        when(doc.getXObjects()).thenReturn(mapBaseObjects);

        List<MentionNotificationParameters> actual =
            this.createdDocumentMentionsAnalyzer.analyze(doc, DOCUMENT_REFERENCE, "1.0", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference("comment", baseObjectReference),
                MentionLocation.TEXT_FIELD, "1.0")
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
        ), actual);
    }

    private MacroBlock buildMentionMacro(String reference, String anchor, DisplayStyle displayStyle)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("reference", reference);
        parameters.put("anchor", anchor);
        parameters.put("style", displayStyle.toString());
        return new MacroBlock("mention", parameters, false);
    }
}
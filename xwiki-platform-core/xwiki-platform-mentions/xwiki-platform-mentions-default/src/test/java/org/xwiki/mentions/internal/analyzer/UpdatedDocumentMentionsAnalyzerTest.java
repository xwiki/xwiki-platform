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
import org.xwiki.rendering.block.WordBlock;
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
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.mentions.DisplayStyle.FIRST_NAME;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.COMMENT;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Test of {@link UpdatedDocumentMentionsAnalyzer}.
 *
 * @version $Id$
 * @since 12.10
 */
@ComponentTest
class UpdatedDocumentMentionsAnalyzerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String AUTHOR = "xwiki:XWiki.Author";

    private static final String USER_U1 = "xwiki:XWiki.U1";

    private static final DocumentReference ACLASS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "AClass");

    private static final DocumentReference COMMENTS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "XWikiComments");

    @InjectMockComponents
    private UpdatedDocumentMentionsAnalyzer updatedDocumentMentionsAnalyzer;

    @MockComponent
    private MentionXDOMService xdomService;

    /**
     * test empty anchors
     */
    @Test
    void analyzeNoNewMention()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("v1.0")));
        when(oldDoc.getXDOM()).thenReturn(oldXDOM);
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        BaseObject oldAWMField = mock(BaseObject.class);
        LargeStringProperty oldLSP = new LargeStringProperty();
        oldLSP.setValue("OLD AWM CONTENT");
        when(oldAWMField.getField("lspfield")).thenReturn(oldLSP);
        oldXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(oldAWMField));
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
        XDOM newXDOM = new XDOM(asList(new WordBlock("v1.1")));
        when(newDoc.getXDOM()).thenReturn(newXDOM);
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newAWMField = mock(BaseObject.class);
        when(newAWMField.getXClassReference()).thenReturn(ACLASS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.AClass", DOCUMENT_REFERENCE), DOCUMENT_REFERENCE);
        when(newAWMField.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty newLSP = new LargeStringProperty();
        newLSP.setName("lspfield");
        newLSP.setValue("NEW AWM CONTENT");
        newLSP.setObject(newAWMField);
        when(newAWMField.getProperties()).thenReturn(new Object[] { newLSP, new FloatProperty() });
        newXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(newAWMField));
        when(newDoc.getXObjects()).thenReturn(newXObjects);
        when(newDoc.getSyntax()).thenReturn(XWIKI_2_1);

        List<MacroBlock> oldMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(new XDOM(asList(new WordBlock("oldword")))));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(new XDOM(asList(new WordBlock("newword")))));

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(asList(), analyze);
    }

    @Test
    void analyzeNewMentionInBody()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("v1.0")));
        when(oldDoc.getXDOM()).thenReturn(oldXDOM);
        XDOM newXDOM = new XDOM(asList(new WordBlock("v1.1")));
        when(newDoc.getXDOM()).thenReturn(newXDOM);

        List<MacroBlock> oldMentions = asList(
            buildMentionMacro(USER_U1, "anchor0", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(
            buildMentionMacro(USER_U1, "anchor0", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(
            asList(new MentionNotificationParameters(AUTHOR, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor0", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
            ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedAWMField()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        when(oldDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.0"))));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        BaseObject oldAWMField = mock(BaseObject.class);
        LargeStringProperty oldLSP = new LargeStringProperty();
        oldLSP.setValue("OLD AWM CONTENT");
        when(oldAWMField.getField("lspfield")).thenReturn(oldLSP);
        oldXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(oldAWMField));
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(newDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.1"))));
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newAWMField = mock(BaseObject.class);
        when(newAWMField.getXClassReference()).thenReturn(ACLASS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.AClass", DOCUMENT_REFERENCE), DOCUMENT_REFERENCE);
        when(newAWMField.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty newLSP = new LargeStringProperty();
        newLSP.setName("lspfield");
        newLSP.setValue("NEW AWM CONTENT");
        newLSP.setObject(newAWMField);
        when(newAWMField.getProperties()).thenReturn(new Object[] { newLSP, new FloatProperty() });
        newXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(newAWMField));
        when(newDoc.getXObjects()).thenReturn(newXObjects);
        when(newDoc.getSyntax()).thenReturn(XWIKI_2_1);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("oldword")));
        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(asList(new WordBlock("newword")));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME));
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference("lspfield", baseObjectReference),
                MentionLocation.TEXT_FIELD, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInNewAWMField()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        when(oldDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.0"))));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        BaseObject oldAWMField = mock(BaseObject.class);
        LargeStringProperty oldLSP = new LargeStringProperty();
        oldLSP.setValue("OLD AWM CONTENT");
        when(oldAWMField.getField("lspfield")).thenReturn(oldLSP);
//        oldXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(oldAWMField));
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(newDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.1"))));
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newAWMField = mock(BaseObject.class);
        when(newAWMField.getXClassReference()).thenReturn(ACLASS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.AClass", DOCUMENT_REFERENCE), DOCUMENT_REFERENCE);
        when(newAWMField.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty newLSP = new LargeStringProperty();
        newLSP.setName("lspfield");
        newLSP.setValue("NEW AWM CONTENT");
        newLSP.setObject(newAWMField);
        when(newAWMField.getProperties()).thenReturn(new Object[] { newLSP, new FloatProperty() });
        newXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(newAWMField));
        when(newDoc.getXObjects()).thenReturn(newXObjects);
        when(newDoc.getSyntax()).thenReturn(XWIKI_2_1);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("oldword")));
        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(asList(new WordBlock("newword")));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference("lspfield", baseObjectReference),
                MentionLocation.TEXT_FIELD, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedCommentField()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        when(oldDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.0"))));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        BaseObject oldAWMField = mock(BaseObject.class);
        LargeStringProperty oldLSP = new LargeStringProperty();
        oldLSP.setValue("OLD AWM CONTENT");
        String fieldName = "comment";
        when(oldAWMField.getField(fieldName)).thenReturn(oldLSP);
        oldXObjects.put(COMMENTS_DOCUMENT_REFERENCE, asList(oldAWMField));
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(newDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.1"))));
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newAWMField = mock(BaseObject.class);
        when(newAWMField.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.AClass", DOCUMENT_REFERENCE), DOCUMENT_REFERENCE);
        when(newAWMField.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty newLSP = new LargeStringProperty();
        newLSP.setName(fieldName);
        newLSP.setValue("NEW AWM CONTENT");
        newLSP.setObject(newAWMField);
        when(newAWMField.getField(fieldName)).thenReturn(newLSP);
        newXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(newAWMField));
        when(newDoc.getXObjects()).thenReturn(newXObjects);
        when(newDoc.getSyntax()).thenReturn(XWIKI_2_1);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("oldword")));
        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(asList(new WordBlock("newword")));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME));
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(fieldName, baseObjectReference),
                COMMENT, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedAnnotationField()
    {
        XWikiDocument oldDoc = mock(XWikiDocument.class);
        XWikiDocument newDoc = mock(XWikiDocument.class);

        when(oldDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.0"))));
        Map<DocumentReference, List<BaseObject>> oldXObjects = new HashMap<>();
        BaseObject oldAWMField = mock(BaseObject.class);
        LargeStringProperty oldLSP = new LargeStringProperty();
        oldLSP.setValue("OLD AWM CONTENT");
        String fieldName = "comment";
        when(oldAWMField.getField(fieldName)).thenReturn(oldLSP);
        oldXObjects.put(COMMENTS_DOCUMENT_REFERENCE, asList(oldAWMField));
        when(oldDoc.getXObjects()).thenReturn(oldXObjects);
        when(newDoc.getXDOM()).thenReturn(new XDOM(asList(new WordBlock("v1.1"))));
        Map<DocumentReference, List<BaseObject>> newXObjects = new HashMap<>();
        BaseObject newAWMField = mock(BaseObject.class);
        when(newAWMField.getXClassReference()).thenReturn(COMMENTS_DOCUMENT_REFERENCE);
        BaseObjectReference baseObjectReference =
            new BaseObjectReference(new ObjectReference("XWiki.AClass", DOCUMENT_REFERENCE), DOCUMENT_REFERENCE);
        when(newAWMField.getReference()).thenReturn(baseObjectReference);
        LargeStringProperty newLSP = new LargeStringProperty();
        newLSP.setName(fieldName);
        newLSP.setValue("NEW AWM CONTENT");
        newLSP.setObject(newAWMField);

        when(newAWMField.getField(fieldName)).thenReturn(newLSP);
        LargeStringProperty selectionField = new LargeStringProperty();
        selectionField.setValue("annotated text");
        when(newAWMField.getField(SELECTION_FIELD)).thenReturn(selectionField);
        newXObjects.put(ACLASS_DOCUMENT_REFERENCE, asList(newAWMField));
        when(newDoc.getXObjects()).thenReturn(newXObjects);
        when(newDoc.getSyntax()).thenReturn(XWIKI_2_1);

        XDOM oldXDOM = new XDOM(asList(new WordBlock("oldword")));
        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(asList(new WordBlock("newword")));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = asList(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = asList(
            buildMentionMacro(USER_U1, null, FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList(null, "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(fieldName, baseObjectReference),
                ANNOTATION, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, null, FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, null, FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
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
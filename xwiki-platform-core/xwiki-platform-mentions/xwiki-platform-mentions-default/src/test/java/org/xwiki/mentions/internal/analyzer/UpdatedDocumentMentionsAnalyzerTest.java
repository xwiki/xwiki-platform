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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.internal.AnnotationClassDocumentInitializer;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.internal.MentionXDOMService;
import org.xwiki.mentions.internal.MentionedActorReference;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
@OldcoreTest
@ComponentList({
    XWikiCommentsDocumentInitializer.class,
    AnnotationClassDocumentInitializer.class,
    DefaultConverterManager.class,
    ContextComponentManagerProvider.class,
    EnumConverter.class,
    ConvertUtilsConverter.class
})
@ReferenceComponentList
class UpdatedDocumentMentionsAnalyzerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String AUTHOR = "xwiki:XWiki.Author";

    private static final String USER_U1 = "xwiki:XWiki.U1";

    private static final DocumentReference ACLASS_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "AClass");

    private static final LocalDocumentReference COMMENTS_DOCUMENT_REFERENCE
        = XWikiCommentsDocumentInitializer.LOCAL_REFERENCE;

    private static final String TEXT_FIELD = "lspfield";

    private static final String NUMBER_FIELD = "number";

    @InjectMockComponents
    private UpdatedDocumentMentionsAnalyzer updatedDocumentMentionsAnalyzer;

    @MockComponent
    private MentionXDOMService xdomService;

    @MockComponent
    private ContentParser contentParser;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    // Needed for the mandatory document initializer
    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    @Named("document")
    private SheetBinder sheetBinder;

    @MockComponent
    private AnnotationConfiguration annotationConfiguration;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.annotationConfiguration.getAnnotationClassReference())
            .thenReturn(new DocumentReference(COMMENTS_DOCUMENT_REFERENCE, DOCUMENT_REFERENCE.getWikiReference()));
        when(this.annotationConfiguration.isInstalled()).thenReturn(true);

        this.oldcore.getSpyXWiki().initializeMandatoryDocuments(this.oldcore.getXWikiContext());
        XWikiDocument classDocument =
            this.oldcore.getSpyXWiki().getDocument(ACLASS_DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        classDocument.getXClass().addTextField(TEXT_FIELD, "Large Field", 30);
        classDocument.getXClass().addNumberField(NUMBER_FIELD, "Number", 10, NumberClass.TYPE_FLOAT);
        this.oldcore.getSpyXWiki().saveDocument(classDocument, "Initialize AClass", this.oldcore.getXWikiContext());
    }

    /**
     * test empty anchors
     */
    @Test
    void analyzeNoNewMention() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();

        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldContent = "v1.0";
        oldDoc.setContent(oldContent);
        BaseObject object = oldDoc.newXObject(ACLASS_DOCUMENT_REFERENCE, context);
        String oldAwmContent = "OLD AWM CONTENT";
        object.setLargeStringValue(TEXT_FIELD, oldAwmContent);

        XDOM oldXDOM = new XDOM(List.of(new WordBlock(oldContent)));
        when(this.contentParser.parse(oldContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(oldXDOM);

        XWikiDocument newDoc = oldDoc.clone();
        String newContent = "v1.1";
        newDoc.setContent(newContent);
        BaseObject newObject = newDoc.getXObject(object.getReference());
        String newAwmContent = "NEW AWM CONTENT";
        newObject.setLargeStringValue(TEXT_FIELD, newAwmContent);

        XDOM newXDOM = new XDOM(List.of(new WordBlock(newContent)));
        when(this.contentParser.parse(newContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(newXDOM);

        List<MacroBlock> oldMentions = List.of(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = List.of(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList("anchor0", "anchor1"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        when(this.xdomService.parse(oldAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(new XDOM(List.of(new WordBlock("oldword")))));
        when(this.xdomService.parse(newAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(new XDOM(List.of(new WordBlock("newword")))));

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(List.of(), analyze);

        verify(this.xdomService).listMentionMacros(oldXDOM);
        verify(this.xdomService).listMentionMacros(newXDOM);
        verify(this.xdomService).groupAnchorsByUserReference(same(oldMentions));
        verify(this.xdomService).groupAnchorsByUserReference(same(newMentions));
        verify(this.xdomService).parse(oldAwmContent, XWIKI_2_1);
        verify(this.xdomService).parse(newAwmContent, XWIKI_2_1);
    }

    @Test
    void analyzeNewMentionInBody() throws Exception
    {
        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldContent = "v1.0";
        oldDoc.setContent(oldContent);

        XDOM oldXDOM = new XDOM(List.of(new WordBlock(oldContent)));
        when(this.contentParser.parse(oldContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(oldXDOM);

        XWikiDocument newDoc = oldDoc.clone();
        String newContent = "v1.1";
        newDoc.setContent(newContent);

        XDOM newXDOM = new XDOM(List.of(new WordBlock(newContent)));
        when(this.contentParser.parse(newContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(newXDOM);

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
            List.of(new MentionNotificationParameters(AUTHOR, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor0", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
            ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedAWMField() throws Exception
    {
        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldContent = "v1.0";
        oldDoc.setContent(oldContent);

        when(this.contentParser.parse(oldContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(oldContent))));

        BaseObject oldObject = oldDoc.newXObject(ACLASS_DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        String oldAwmContent = "OLD AWM CONTENT";
        oldObject.setLargeStringValue(TEXT_FIELD, oldAwmContent);

        XWikiDocument newDoc = oldDoc.clone();
        String newContent = "v1.1";
        newDoc.setContent(newContent);

        when(this.contentParser.parse(newContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(newContent))));

        BaseObject newObject = newDoc.getXObject(oldObject.getReference());
        String newAwmContent = "NEW AWM CONTENT";
        newObject.setLargeStringValue(TEXT_FIELD, newAwmContent);

        XDOM oldXDOM = new XDOM(List.of(new WordBlock("oldword")));
        when(this.xdomService.parse(oldAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(List.of(new WordBlock("newword")));
        when(this.xdomService.parse(newAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = List.of(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = List.of(
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME));
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), List.of("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), List.of("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(List.of(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(TEXT_FIELD, oldObject.getReference()),
                MentionLocation.TEXT_FIELD, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInNewAWMField() throws Exception
    {
        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldContent = "v1.0";
        oldDoc.setContent(oldContent);

        when(this.contentParser.parse(oldContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(oldContent))));

        XWikiDocument newDoc = oldDoc.clone();
        String newContent = "v1.1";
        newDoc.setContent(newContent);

        when(this.contentParser.parse(newContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(newContent))));

        BaseObject newObject = newDoc.newXObject(ACLASS_DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        String newAwmContent = "NEW AWM CONTENT";
        newObject.setLargeStringValue(TEXT_FIELD, newAwmContent);
        newObject.setFloatValue(NUMBER_FIELD, 1.0f);

        XDOM oldXDOM = new XDOM(List.of(new WordBlock("oldword")));
        when(this.xdomService.parse("OLD AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(List.of(new WordBlock("newword")));
        when(this.xdomService.parse("NEW AWM CONTENT", XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = List.of(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = List.of(
            buildMentionMacro(USER_U1, "anchor1", FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), List.of("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), List.of("anchor1", "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        assertEquals(List.of(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(TEXT_FIELD, newObject.getReference()),
                MentionLocation.TEXT_FIELD, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedCommentField() throws Exception
    {
        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldContent = "v1.0";
        oldDoc.setContent(oldContent);

        when(this.contentParser.parse(oldContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(oldContent))));

        BaseObject oldObject = oldDoc.newXObject(COMMENTS_DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        String oldAwmContent = "OLD AWM CONTENT";
        String fieldName = "comment";
        oldObject.setLargeStringValue(fieldName, oldAwmContent);

        XWikiDocument newDoc = oldDoc.clone();
        String newContent = "v1.1";
        newDoc.setContent(newContent);

        when(this.contentParser.parse(newContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(newContent))));

        BaseObject newObject = newDoc.getXObject(oldObject.getReference());
        String newAwmContent = "NEW AWM CONTENT";
        newObject.setLargeStringValue(fieldName, newAwmContent);

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
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(fieldName, oldObject.getReference()),
                COMMENT, "1.1")
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor1", FIRST_NAME))
                .addNewMention("user", new MentionNotificationParameter(USER_U1, "anchor2", FIRST_NAME))
        ), analyze);
    }

    @Test
    void analyzeNewMentionInUpdatedAnnotationField() throws Exception
    {
        XWikiDocument oldDoc = new XWikiDocument(DOCUMENT_REFERENCE);
        oldDoc.setSyntax(XWIKI_2_1);
        String oldDocumentContent = "v1.0";
        oldDoc.setContent(oldDocumentContent);

        when(this.contentParser.parse(oldDocumentContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(oldDocumentContent))));

        BaseObject oldObject = oldDoc.newXObject(COMMENTS_DOCUMENT_REFERENCE, this.oldcore.getXWikiContext());
        String oldAwmContent = "OLD AWM CONTENT";
        String fieldName = "comment";
        oldObject.setLargeStringValue(fieldName, oldAwmContent);

        XWikiDocument newDoc = oldDoc.clone();
        String newDocumentContent = "v1.1";
        newDoc.setContent(newDocumentContent);

        when(this.contentParser.parse(newDocumentContent, XWIKI_2_1, DOCUMENT_REFERENCE))
            .thenReturn(new XDOM(List.of(new WordBlock(newDocumentContent))));

        BaseObject newObject = newDoc.getXObject(oldObject.getReference());
        String newAwmContent = "NEW AWM CONTENT";
        newObject.setLargeStringValue(fieldName, newAwmContent);
        String annotatedText = "annotated text";
        newObject.setLargeStringValue(SELECTION_FIELD, annotatedText);

        XDOM oldXDOM = new XDOM(List.of(new WordBlock("oldword")));
        when(this.xdomService.parse(oldAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(oldXDOM));
        XDOM newXDOM = new XDOM(List.of(new WordBlock("newword")));
        when(this.xdomService.parse(newAwmContent, XWIKI_2_1))
            .thenReturn(Optional.of(newXDOM));

        List<MacroBlock> oldMentions = List.of(new MacroBlock("mention", new HashMap<>(), false));
        when(this.xdomService.listMentionMacros(oldXDOM)).thenReturn(oldMentions);
        List<MacroBlock> newMentions = List.of(
            buildMentionMacro(USER_U1, null, FIRST_NAME),
            buildMentionMacro(USER_U1, "anchor2", FIRST_NAME)
        );
        when(this.xdomService.listMentionMacros(newXDOM)).thenReturn(newMentions);

        // anchor0 is removed and anchor1 and anchor2 are added, all mentionning U1.
        Map<MentionedActorReference, List<String>> oldCounts = new HashMap<>();
        oldCounts.put(new MentionedActorReference(USER_U1, "user"), List.of("anchor0"));
        when(this.xdomService.groupAnchorsByUserReference(oldMentions)).thenReturn(oldCounts);
        Map<MentionedActorReference, List<String>> newCounts = new HashMap<>();
        newCounts.put(new MentionedActorReference(USER_U1, "user"), asList(null, "anchor2"));
        when(this.xdomService.groupAnchorsByUserReference(newMentions)).thenReturn(newCounts);

        List<MentionNotificationParameters> analyze =
            this.updatedDocumentMentionsAnalyzer.analyze(oldDoc, newDoc, DOCUMENT_REFERENCE, "1.1", AUTHOR);

        verify(this.xdomService, never()).parse(annotatedText, XWIKI_2_1);

        assertEquals(asList(
            new MentionNotificationParameters(AUTHOR, new ObjectPropertyReference(fieldName, oldObject.getReference()),
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

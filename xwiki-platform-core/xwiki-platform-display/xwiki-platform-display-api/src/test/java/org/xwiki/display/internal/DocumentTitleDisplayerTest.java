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
package org.xwiki.display.internal;

import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentTitleDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(DocumentReferenceDequeContext.class)
class DocumentTitleDisplayerTest
{
    private static final String SDOC = "sdoc";

    private static final String SERIALIZED_REFERENCE = "wiki:Space.Page";

    @InjectMockComponents
    private DocumentTitleDisplayer documentTitleDisplayer;

    @MockComponent
    private Execution execution;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainTextParser;

    @MockComponent
    private ModelContext modelContext;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    @MockComponent
    private DisplayConfiguration displayConfiguration;

    @MockComponent
    private TransformationManager transformationManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private ErrorBlockGenerator errorBlockGenerator;

    private Map<String, Object> xwikiContext;

    @BeforeEach
    void configure()
    {
        // The execution context is expected to have the "xwikicontext" property set.
        ExecutionContext executionContext = new ExecutionContext();
        this.xwikiContext = new HashMap<>();
        executionContext.setProperty("xwikicontext", this.xwikiContext);
        when(this.execution.getContext()).thenReturn(executionContext);
    }

    @Test
    void fallbackOnSpaceNameWhenSpaceHomePageTitleIsEmpty() throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", List.of("Space"), "Page"));

        XDOM titleXDOM = new XDOM(List.of(new WordBlock("Space")));

        when(this.plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        assertSame(titleXDOM, this.documentTitleDisplayer.display(document, params));

        ArgumentCaptor<Reader> argument = ArgumentCaptor.forClass(Reader.class);
        verify(this.plainTextParser).parse(argument.capture());
        assertEquals("Space", IOUtils.toString(argument.getValue()));
    }

    @Test
    void whenSettingTheContextDocumentTheContextWikiIsAlsoSet() throws Exception
    {
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("Page", EntityType.DOCUMENT));

        DocumentModelBridge document = mock(DocumentModelBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", List.of("Space"), "Page");
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getTitle()).thenReturn("title");

        XDOM titleXDOM = new XDOM(List.of(new WordBlock("title")));

        when(this.plainTextParser.parse(any(StringReader.class))).thenReturn(titleXDOM);

        WikiReference currentWikiReference = new WikiReference("currentWiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(currentWikiReference);

        when(this.authorizationManager.hasAccess(eq(Right.SCRIPT), eq(EntityType.DOCUMENT), any(), any()))
            .thenReturn(true);

        VelocityEngine velocityEngine = mock();
        when(this.velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(invocationOnMock -> {
            Writer output = invocationOnMock.getArgument(1);
            output.write("title");
            return null;
        }).when(velocityEngine).evaluate(any(), any(), any(), any(VelocityTemplate.class));

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);
        params.setExecutionContextIsolated(true);

        this.documentTitleDisplayer.display(document, params);

        // Check that the context is set.
        verify(this.dab).pushDocumentInContext(any(), same(document));
        verify(this.modelContext).setCurrentEntityReference(documentReference.getWikiReference());

        // Check that the context is restored.
        verify(this.dab).popDocumentFromContext(any());
        verify(this.modelContext).setCurrentEntityReference(currentWikiReference);
    }

    @Test
    void titleExtractedFromContentIsExecutedInTheContextOfTheDocument() throws Exception
    {
        // Enable the title compatibility mode, in which the title is extracted from the first heading of the content.
        when(this.xwikicfg.getProperty("xwiki.title.compatibility", "0")).thenReturn("1");
        when(this.displayConfiguration.getTitleHeadingDepth()).thenReturn(2);

        DocumentReference documentReference = new DocumentReference("wiki", List.of("Space"), "Page");
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn(SERIALIZED_REFERENCE);

        DocumentModelBridge document = mock();
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        WordBlock headingContent = new WordBlock("heading");
        when(document.getPreparedXDOM()).thenReturn(
            new XDOM(List.of(new HeaderBlock(List.of(headingContent), HeaderLevel.LEVEL1)),
                new MetaData(Map.of(MetaData.SOURCE, SERIALIZED_REFERENCE))));

        // The title is displayed while the content of another document is being executed.
        Object otherSecureDocument = new Object();
        this.xwikiContext.put(SDOC, otherSecureDocument);

        doAnswer(invocationOnMock -> {
            XDOM headingXDOM = invocationOnMock.getArgument(0);
            TransformationContext transformationContext = invocationOnMock.getArgument(1);
            // The heading is executed in the context of the document it has been extracted from.
            assertSame(document, this.xwikiContext.get(SDOC));
            assertEquals(SERIALIZED_REFERENCE, transformationContext.getId());
            assertEquals(SERIALIZED_REFERENCE, headingXDOM.getMetaData().getMetaData(MetaData.SOURCE));
            return null;
        }).when(this.transformationManager).performTransformations(any(XDOM.class), any(TransformationContext.class));

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        assertEquals(List.of(headingContent), this.documentTitleDisplayer.display(document, params).getChildren());

        verify(this.transformationManager).performTransformations(any(XDOM.class), any(TransformationContext.class));
        // The secure document of the caller is restored.
        assertSame(otherSecureDocument, this.xwikiContext.get(SDOC));
    }

    @Test
    void titleExtractedFromContentRemovesTheSecureDocumentWhenThereWasNone() throws Exception
    {
        when(this.xwikicfg.getProperty("xwiki.title.compatibility", "0")).thenReturn("1");
        when(this.displayConfiguration.getTitleHeadingDepth()).thenReturn(2);

        DocumentModelBridge document = mock();
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", List.of("Space"), "Page"));
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(document.getPreparedXDOM()).thenReturn(
            new XDOM(List.of(new HeaderBlock(List.of(new WordBlock("heading")), HeaderLevel.LEVEL1))));

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        this.documentTitleDisplayer.display(document, params);

        assertFalse(this.xwikiContext.containsKey(SDOC));
    }

    @Test
    void titleExtractedFromContentFallsBackToDocumentNameOnRenderingError() throws Exception
    {
        when(this.xwikicfg.getProperty("xwiki.title.compatibility", "0")).thenReturn("1");
        when(this.displayConfiguration.getTitleHeadingDepth()).thenReturn(2);
        when(this.defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));

        DocumentModelBridge document = mock();
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", List.of("Space"), "Page"));
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(document.getPreparedXDOM()).thenReturn(
            new XDOM(List.of(new HeaderBlock(List.of(new WordBlock("heading")), HeaderLevel.LEVEL1))));

        // The macro of the heading couldn't be executed, so the transformed heading contains a rendering error.
        when(this.errorBlockGenerator.containsError(any(XDOM.class))).thenReturn(true);

        XDOM staticTitle = new XDOM(List.of(new WordBlock("Page")));
        when(this.plainTextParser.parse(any(StringReader.class))).thenReturn(staticTitle);

        DocumentDisplayerParameters params = new DocumentDisplayerParameters();
        params.setTitleDisplayed(true);

        assertSame(staticTitle, this.documentTitleDisplayer.display(document, params));

        ArgumentCaptor<Reader> argument = ArgumentCaptor.captor();
        verify(this.plainTextParser).parse(argument.capture());
        assertEquals("Page", IOUtils.toString(argument.getValue()));
    }
}

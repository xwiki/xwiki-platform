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
package org.xwiki.rendering.internal.macro;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.PageReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.display.DisplayMacro;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.display.DisplayMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.test.integration.junit5.BlockAssert;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.DefaultAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.test.integration.junit5.BlockAssert.assertBlocks;

/**
 * Unit tests for {@link DisplayMacro}.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents(excludes = {
    CurrentMacroEntityReferenceResolver.class,
    DefaultAuthorizationManager.class
})
class DisplayMacroTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private DocumentAccessBridge dab;

    // Make sure to not load the standard AuthorizationManager which trigger too many things
    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> currentAttachmentReferenceResolver;

    /**
     * Mocks the component that is used to resolve the 'reference' parameter.
     */
    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @Mock
    private DocumentModelBridge displayedDocument;

    private DisplayMacro displayMacro;

    private PrintRendererFactory rendererFactory;

    @BeforeEach
    public void setUp() throws Exception
    {
        MemoryConfigurationSource memoryConfigurationSource = new MemoryConfigurationSource();
        this.componentManager.registerComponent(ConfigurationSource.class, memoryConfigurationSource);
        this.componentManager.registerComponent(ConfigurationSource.class, "xwikicfg", memoryConfigurationSource);

        this.displayMacro = this.componentManager.getInstance(Macro.class, "display");
        this.rendererFactory = this.componentManager.getInstance(PrintRendererFactory.class, "event/1.0");

        // Put a fake XWiki context on the execution context.
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ExecutionContext ec = new ExecutionContext();
        ecm.initialize(ec);
        execution.getContext().setProperty("xwikicontext", new HashMap<>());

        when(this.contextualAuthorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
        // Register a WikiModel mock so that we're in wiki mode (otherwise links will be considered as URLs for ex).
        this.componentManager.registerMockComponent(WikiModel.class);
    }

    @Test
    void executeShowsVelocityMacrosAreIsolated() throws Exception
    {
        // @formatter:off
        String expected =
            "beginDocument\n"
                + "beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n"
                + "beginMacroMarkerStandalone [velocity] [] [#testmacro]\n"
                + "beginParagraph\n"
                + "onSpecialSymbol [#]\n"
                + "onWord [testmacro]\n"
                + "endParagraph\n"
                + "endMacroMarkerStandalone [velocity] [] [#testmacro]\n"
                + "endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n"
                + "endDocument";
        // @formatter:on

        // We verify that a Velocity macro set in the page doing the display is not seen in the displayed page.
        List<Block> blocks =
            runDisplayMacroWithPreVelocity("#macro(testmacro)#end", "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithNoDocumentSpecified()
    {
        DisplayMacroParameters parameters = new DisplayMacroParameters();

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.displayMacro.execute(parameters, null, createMacroTransformationContext("whatever", false)));
        assertEquals("You must specify a 'reference' parameter pointing to the entity to display.",
            exception.getMessage());
    }

    /**
     * Verify that relative links returned by the display macro as wrapped with a MetaDataBlock.
     */
    @Test
    void executeWhenDisplayingDocumentWithRelativeReferences() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[displayedWiki:displayedSpace.displayedPage]"
            + "[source]=[displayedWiki:displayedSpace.displayedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "beginLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "endLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "onSpace\n"
            + "beginLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "endLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "onSpace\n"
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true] [Itest.png]\n"
            + "endParagraph\n"
            + "endMetaData [[base]=[displayedWiki:displayedSpace.displayedPage]"
            + "[source]=[displayedWiki:displayedSpace.displayedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DocumentReference displayedDocumentReference =
            new DocumentReference("displayedWiki", "displayedSpace", "displayedPage");
        PageReference displayedPageReference = new PageReference("displayedWiki", "displayedSpace", "displayedPage");
        setupDocumentMocks("displayedWiki:displayedSpace.displayedPage", displayedDocumentReference,
            "displayedWiki:displayedSpace/displayedPage", displayedPageReference,
            "[[page]] [[attach:test.png]] image:test.png");
        when(this.dab.getCurrentDocumentReference()).thenReturn(displayedDocumentReference);

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("displayedWiki:displayedSpace.displayedPage");

        List<Block> blocks =
            this.displayMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);

        parameters.setPage("displayedWiki:displayedSpace/displayedPage");

        blocks =
            this.displayMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);

    }

    @Test
    void executeWithRecursiveDisplay() throws Exception
    {
        // Other tests use the real DocumentDisplayer component implementation but for this test we mock it so that
        // we can control how it behaves.
        DocumentDisplayer documentDisplayer = mock(DocumentDisplayer.class);
        this.displayMacro.setDocumentDisplayer(documentDisplayer);

        MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("wiki:space.page");

        DocumentReference displayedDocumentReference = new DocumentReference("wiki", "space", "page");
        setupDocumentMocks("wiki:space.page", displayedDocumentReference, "");

        when(documentDisplayer.display(same(this.displayedDocument), any(DocumentDisplayerParameters.class)))
            .thenAnswer((Answer) invocation -> {
                // Call again the display macro when the document displayer executes to simulate a recursive call.
                // Verify that it raises a MacroExecutionException in this case.
                Throwable exception = assertThrows(MacroExecutionException.class,
                    () -> this.displayMacro.execute(parameters, null, macroContext));
                assertTrue(exception.getMessage().contains("Found recursive display of document [wiki:space.page]"));
                throw exception;
            }
        );

        // Verify that the exception bubbles up.
        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.displayMacro.execute(parameters, null, macroContext));
        assertEquals("Found recursive display of document [wiki:space.page]", exception.getMessage());
    }

    @Test
    void executeInsideBaseMetaDataBlockAndWithRelativeDocumentReferencePassed() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:space.relativePage][source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[base]=[wiki:space.relativePage][source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("relativePage");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        // Add a Source MetaData Block as a parent of the display Macro block.
        new MetaDataBlock(Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()),
            new MetaData(Collections.singletonMap(MetaData.BASE, "wiki:space.page")));

        final DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "relativePage");
        setupDocumentMocks("relativePage", resolvedReference, "content");
        when(this.dab.getCurrentDocumentReference()).thenReturn(sourceReference);

        List<Block> blocks = this.displayMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void adaptIdsOfDisplayedHeadingsAndImages() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[includedWiki:includedSpace.includedPage][source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]\n"
            + "beginSection\n"
            + "beginHeader [1, HHeading-1]\n"
            + "onWord [Heading]\n"
            + "endHeader [1, HHeading-1]\n"
            + "beginParagraph\n"
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true] [Itest.png-1]\n"
            + "endParagraph\n"
            + "endSection\n"
            + "endMetaData [[base]=[includedWiki:includedSpace.includedPage][source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        String documentContent = "= Heading =\n"
            + "image:test.png";

        DocumentReference includedDocumentReference =
            new DocumentReference("includedWiki", "includedSpace", "includedPage");
        setupDocumentMocks("includedWiki:includedSpace.includedPage", includedDocumentReference,
            documentContent);

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("includedWiki:includedSpace.includedPage");

        MacroTransformationContext context = createMacroTransformationContext("whatever", false);
        // Initialize XDOM with ids from the including page.
        context.setXDOM(getXDOM(documentContent));

        List<Block> blocks = this.displayMacro.execute(parameters, null, context);

        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenSectionSpecified() throws Exception
    {
        // @formatter:off
        String expected =
            "beginDocument\n"
                + "beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n"
                + "beginHeader [1, Hsection]\n"
                + "onWord [section]\n"
                + "endHeader [1, Hsection]\n"
                + "beginParagraph\n"
                + "onWord [content2]\n"
                + "endParagraph\n"
                + "endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n"
                + "endDocument";
        // @formatter:on

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setSection("Hsection");

        List<Block> blocks = runDisplayMacro(parameters, "content1\n\n= section =\ncontent2");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenInvalidSectionSpecified()
    {
        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setSection("unknown");

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> runDisplayMacro(parameters, "content"));
        assertEquals("Cannot find section [unknown] in document [wiki:Space.DisplayedPage]", exception.getMessage());
    }

    @Test
    void executeWhenExcludeFirstHeadingTrueAndHeadingIsFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(true);

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "= Heading =\ncontent");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.displayMacro.execute(parameters, null, macroContext);

        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenExcludeFirstHeadingFalseAndHeadingIsFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginSection\n"
            + "beginHeader [1, Hcontent]\n"
            + "onWord [content]\n"
            + "endHeader [1, Hcontent]\n"
            + "endSection\n"
            + "endMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(false);

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "=content=");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.displayMacro.execute(parameters, null, macroContext);

        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenExcludeFirstHeadingTrueAndHeadingIsNotFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginGroup\n"
            + "beginSection\n"
            + "beginHeader [1, Hcontent]\n"
            + "onWord [content]\n"
            + "endHeader [1, Hcontent]\n"
            + "endSection\n"
            + "endGroup\n"
            + "endMetaData [[base]=[wiki:space.document][source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(true);

        // Getting the macro context
        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "(((= content =)))");

        List<Block> blocks = this.displayMacro.execute(parameters, null, macroContext);
        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeInInlineContextWithInlineContent() throws Exception
    {
        String expected = """
            beginDocument
            beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage][syntax]=[XWiki 2.0]]
            onWord [content]
            endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage][syntax]=[XWiki 2.0]]
            endDocument""";

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("document");

        List<Block> blocks = runDisplayMacro(parameters, "content", true);

        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeInInlineContextWithNonInlineContent() throws Exception
    {
        String expected = """
            beginDocument
            beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage][syntax]=[XWiki 2.0]]
            beginParagraph
            onWord [first]
            endParagraph
            beginParagraph
            onWord [second]
            endParagraph
            endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage][syntax]=[XWiki 2.0]]
            endDocument""";

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("document");

        List<Block> blocks = runDisplayMacro(parameters, "first\n\nsecond", true);

        BlockAssert.assertBlocks(expected, blocks, this.rendererFactory);
    }

    private MacroTransformationContext createMacroTransformationContext(String documentName, boolean isInline)
    {
        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(isInline);
        MacroBlock macroBlock =
            new MacroBlock("display", Collections.singletonMap("reference", documentName), isInline);
        context.setCurrentMacroBlock(macroBlock);
        return context;
    }

    private void setupDocumentMocks(String displayedReferenceString, DocumentReference displayedReference,
        String displayedContent) throws Exception
    {
        setupDocumentMocks(displayedReferenceString, displayedReference, null, null, displayedContent);
    }

    private void setupDocumentMocks(String displayedDocumentReferenceString,
        DocumentReference displayedDocumentReference, String displayedPageReferenceString,
        PageReference displayedPageReference, String displayedContent) throws Exception
    {
        when(this.macroEntityReferenceResolver.resolve(eq(displayedDocumentReferenceString), eq(EntityType.DOCUMENT),
            any(MacroBlock.class))).thenReturn(displayedDocumentReference);
        when(this.dab.getDocumentInstance((EntityReference) displayedDocumentReference))
            .thenReturn(this.displayedDocument);
        if (displayedPageReference != null) {
            when(this.macroEntityReferenceResolver.resolve(eq(displayedPageReferenceString), eq(EntityType.PAGE),
                any(MacroBlock.class))).thenReturn(displayedPageReference);
            when(this.dab.getDocumentInstance(displayedPageReference))
                .thenReturn(this.displayedDocument);
        }

        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, displayedDocumentReference)).thenReturn(true);
        when(this.dab.getTranslatedDocumentInstance(this.displayedDocument)).thenReturn(this.displayedDocument);
        when(this.displayedDocument.getDocumentReference()).thenReturn(displayedDocumentReference);
        when(this.displayedDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_0);
        when(this.displayedDocument.getPreparedXDOM()).thenReturn(getXDOM(displayedContent));
        when(this.displayedDocument.getRealLanguage()).thenReturn("");
    }

    private XDOM getXDOM(String content) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, "xwiki/2.0");
        return parser.parse(new StringReader(content));
    }

    private List<Block> runDisplayMacroWithPreVelocity(String velocity, String displayedContent) throws Exception
    {
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer,
            "wiki:Space.DisplayingPage", velocity);

        return runDisplayMacro(displayedContent);
    }

    private List<Block> runDisplayMacro(String displayedContent) throws Exception
    {
        return runDisplayMacro(new DisplayMacroParameters(), displayedContent);
    }

    private List<Block> runDisplayMacro(DisplayMacroParameters parameters, String displayedContent)
        throws Exception
    {
        return runDisplayMacro(parameters, displayedContent, false);
    }

    private List<Block> runDisplayMacro(DisplayMacroParameters parameters, String displayedContent, boolean isInline)
        throws Exception
    {
        DocumentReference displayedDocumentReference = new DocumentReference("wiki", "Space", "DisplayedPage");
        String displayedDocStringRef = "wiki:space.page";
        setupDocumentMocks(displayedDocStringRef, displayedDocumentReference, displayedContent);

        parameters.setReference(displayedDocStringRef);

        // Create a Macro transformation context with the Macro transformation object defined so that the display
        // macro can transform displayed page which is using a new context.
        MacroTransformation macroTransformation = this.componentManager.getInstance(Transformation.class, "macro");
        MacroTransformationContext macroContext = createMacroTransformationContext(displayedDocStringRef, isInline);
        macroContext.setId("wiki:Space.DisplayingPage");
        macroContext.setTransformation(macroTransformation);

        return this.displayMacro.execute(parameters, null, macroContext);
    }
}

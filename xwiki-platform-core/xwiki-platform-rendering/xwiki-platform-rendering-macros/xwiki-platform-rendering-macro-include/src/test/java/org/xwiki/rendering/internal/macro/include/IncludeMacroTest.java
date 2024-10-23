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
package org.xwiki.rendering.internal.macro.include;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
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
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Author;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.security.authorization.AuthorExecutor;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.test.integration.junit5.BlockAssert.assertBlocks;
import static org.xwiki.rendering.test.integration.junit5.BlockAssert.assertBlocksStartsWith;

/**
 * Unit tests for {@link IncludeMacro}.
 *
 * @version $Id$
 * @since 1.5M2
 */
@ComponentTest
@AllComponents(excludes = {CurrentMacroEntityReferenceResolver.class, DefaultAuthorizationManager.class})
class IncludeMacroTest
{
    private final static DocumentReference INCLUDER_AUHOR = new DocumentReference("wiki", "XWiki", "includer");

    private final static DocumentReference INCLUDED_AUHOR = new DocumentReference("wiki", "XWiki", "included");

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

    @MockComponent
    private AuthorExecutor authorExecutor;

    @Mock
    private DocumentModelBridge includedDocument;

    /**
     * Mocks the component that is used to resolve the 'reference' parameter.
     */
    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    private IncludeMacro includeMacro;

    private PrintRendererFactory rendererFactory;

    @BeforeEach
    public void setUp() throws Exception
    {
        MemoryConfigurationSource memoryConfigurationSource = new MemoryConfigurationSource();
        this.componentManager.registerComponent(ConfigurationSource.class, memoryConfigurationSource);
        this.componentManager.registerComponent(ConfigurationSource.class, "xwikicfg", memoryConfigurationSource);

        this.includeMacro = this.componentManager.getInstance(Macro.class, "include");
        this.rendererFactory = this.componentManager.getInstance(PrintRendererFactory.class, "event/1.0");

        when(this.dab.getCurrentAuthorReference()).thenReturn(INCLUDER_AUHOR);

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

        when(this.authorExecutor.call(any(), any(), any())).then(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                return ((Callable<Void>) invocation.getArgument(0)).call();
            }
        });
    }

    @Test
    void executeWithPRAuthors() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [word]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        when(this.authorizationManager.hasAccess(Right.PROGRAM, INCLUDED_AUHOR, null)).thenReturn(true);

        List<Block> blocks = runIncludeMacro(Context.CURRENT, "word", false);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithCURRENTAuthor() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [word]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        List<Block> blocks = runIncludeMacro(Context.CURRENT, Author.CURRENT, "word", false);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithNoPRAuthor() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [word]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        List<Block> blocks = runIncludeMacro(Context.CURRENT, Author.AUTO, "word", false);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithTARGETAuthor() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [word]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        List<Block> blocks = runIncludeMacro(Context.CURRENT, Author.TARGET, "word", false);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithCurrentUserNoView() throws Exception
    {
        String referenceString = "reference";
        DocumentReference reference = new DocumentReference("wiki", "space", "page");

        when(this.macroEntityReferenceResolver.resolve(eq(referenceString), eq(EntityType.DOCUMENT),
            any(MacroBlock.class))).thenReturn(reference);
        when(this.dab.getDocumentInstance((EntityReference) reference)).thenReturn(this.includedDocument);
        when(this.includedDocument.getDocumentReference()).thenReturn(reference);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, reference)).thenReturn(false);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference(referenceString);

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false)));
        assertEquals("Current user [null] doesn't have view rights on document [wiki:space.page]",
            exception.getMessage());
    }

    @Test
    void executeWithNewContextShowsVelocityMacrosAreIsolated() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:Space.IncludedPage][source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginMacroMarkerStandalone [velocity] [] [#testmacro]\n"
            + "beginParagraph\n"
            + "onSpecialSymbol [#]\n"
            + "onWord [testmacro]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [#testmacro]\n"
            + "endMetaData [[base]=[wiki:Space.IncludedPage][source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        // We verify that a Velocity macro set in the including page is not seen in the included page.
        List<Block> blocks =
            runIncludeMacroWithPreVelocity(Context.NEW, "#macro(testmacro)#end", "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithNewContextShowsPassingOnRestrictedFlag() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData "
            + "[[base]=[wiki:Space.IncludedPage][source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "beginMacroMarkerStandalone [velocity] [] [$foo]\n"
            + "beginGroup [[class]=[xwikirenderingerror]]\n"
            + "onWord [Failed to execute the [velocity] macro. Cause: [The execution of the [velocity] script "
            + "macro is not allowed. Check the rights of its last author or the parameters if it's rendered "
            + "from another script.]. Click on this message for details.]\n"
            + "endGroup [[class]=[xwikirenderingerror]]\n"
            + "beginGroup [[class]=[xwikirenderingerrordescription hidden]]\n"
            + "onVerbatim [org.xwiki.rendering.macro.MacroExecutionException: "
            + "The execution of the [velocity] script macro is not allowed.";
        // @formatter:on

        // We verify that a Velocity macro set in the including page is not seen in the included page.
        List<Block> blocks = runIncludeMacro(Context.NEW, "{{velocity}}$foo{{/velocity}}", true);

        assertBlocksStartsWith(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithCurrentContextShowsVelocityMacrosAreShared() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "onMacroStandalone [velocity] [] [#testmacro]\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        when(this.authorizationManager.hasAccess(Right.PROGRAM, INCLUDED_AUHOR, null)).thenReturn(true);

        // We verify that a Velocity macro set in the including page is seen in the included page.
        List<Block> blocks = runIncludeMacroWithPreVelocity(Context.CURRENT, "#macro(testmacro)#end",
            "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithNoDocumentSpecified()
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false)));
        assertEquals("You must specify a 'reference' parameter pointing to the entity to include.",
            exception.getMessage());
    }

    /**
     * Verify that relative links returned by the Include macro as wrapped with a MetaDataBlock.
     */
    @Test
    void executeWhenIncludingDocumentWithRelativeReferences() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[includedWiki:includedSpace.includedPage]"
            + "[source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "beginLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "endLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "onSpace\n"
            + "beginLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "endLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "onSpace\n"
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true] [Itest.png]\n"
            + "endParagraph\n"
            + "endMetaData [[base]=[includedWiki:includedSpace.includedPage]"
            + "[source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DocumentReference includedDocumentReference =
            new DocumentReference("includedWiki", "includedSpace", "includedPage");
        PageReference includedPageReference = new PageReference("includedWiki", "includedSpace", "includedPage");
        setupDocumentMocks("includedWiki:includedSpace.includedPage", includedDocumentReference,
            "includedWiki:includedSpace/includedPage", includedPageReference,
            "[[page]] [[attach:test.png]] image:test.png");
        when(this.dab.getCurrentDocumentReference()).thenReturn(includedDocumentReference);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("includedWiki:includedSpace.includedPage");
        parameters.setContext(Context.NEW);

        List<Block> blocks =
            this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
        verify(this.dab).pushDocumentInContext(any(Map.class), any(DocumentModelBridge.class));
        verify(this.dab).popDocumentFromContext(any(Map.class));

        parameters.setPage("includedWiki:includedSpace/includedPage");

        blocks = this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    /**
     * Verify that ids are adapted if they would duplicate an id of the parent document.
     */
    @Test
    void adaptIdsOfIncludedHeadingsAndImages() throws Exception
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

        String documentContent = "= Heading =\n" + "image:test.png";

        DocumentReference includedDocumentReference =
            new DocumentReference("includedWiki", "includedSpace", "includedPage");
        setupDocumentMocks("includedWiki:includedSpace.includedPage", includedDocumentReference, documentContent);
        when(this.dab.getCurrentDocumentReference()).thenReturn(includedDocumentReference);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("includedWiki:includedSpace.includedPage");
        parameters.setContext(Context.NEW);

        MacroTransformationContext context = createMacroTransformationContext("whatever", false);
        // Initialize XDOM with ids from the including page.
        context.setXDOM(toXDOM(documentContent));

        List<Block> blocks = this.includeMacro.execute(parameters, null, context);

        assertBlocks(expected, blocks, this.rendererFactory);
        verify(this.dab).pushDocumentInContext(any(Map.class), any(DocumentModelBridge.class));
        verify(this.dab).popDocumentFromContext(any(Map.class));
    }

    @Test
    void adaptInlineContent() throws Exception
    {
        String expected = """
            beginDocument
            beginMetaData [[source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]
            onWord [content]
            endMetaData [[source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]
            endDocument""";

        String documentContent = "content";

        DocumentReference includedDocumentReference = new DocumentReference("includedWiki", "includedSpace", "includedPage");
        setupDocumentMocks("includedWiki:includedSpace.includedPage", includedDocumentReference, documentContent);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("includedWiki:includedSpace.includedPage");
        MacroTransformationContext context = createMacroTransformationContext("whatever", true);
        List<Block> blocks = this.includeMacro.execute(parameters, null, context);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWithRecursiveIncludeContextCurrent() throws Exception
    {
        MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);
        // Add an Include Macro MarkerBlock as a parent of the include Macro block since this is what would have
        // happened if an Include macro is included in another Include macro.
        MacroMarkerBlock includeMacroMarker =
            new MacroMarkerBlock("include", Collections.singletonMap("reference", "space.page"),
                Collections.singletonList(macroContext.getCurrentMacroBlock()), false);

        when(this.macroEntityReferenceResolver.resolve("wiki:space.page", EntityType.DOCUMENT,
            macroContext.getCurrentMacroBlock())).thenReturn(new DocumentReference("wiki", "space", "page"));
        when(this.macroEntityReferenceResolver.resolve("space.page", EntityType.DOCUMENT, includeMacroMarker))
            .thenReturn(new DocumentReference("wiki", "space", "page"));

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("wiki:space.page");
        parameters.setContext(Context.CURRENT);

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, macroContext));
        assertTrue(exception.getMessage().startsWith("Found recursive inclusion"));
    }

    @Test
    void executeWithRecursiveIncludeContextNew() throws Exception
    {
        // Other tests use the real DocumentDisplayer component implementation but for this test we mock it so that
        // we can control how it behaves.
        DocumentDisplayer documentDisplayer = mock(DocumentDisplayer.class);
        this.includeMacro.setDocumentDisplayer(documentDisplayer);

        MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("wiki:space.page");
        parameters.setContext(Context.NEW);

        DocumentReference includedDocumentReference = new DocumentReference("wiki", "space", "page");
        setupDocumentMocks("wiki:space.page", includedDocumentReference, "");

        when(documentDisplayer.display(same(this.includedDocument), any(DocumentDisplayerParameters.class)))
            .thenAnswer((Answer) invocation -> {
                // Call again the include macro when the document displayer executes to simulate a recursive call.
                // Verify that it raises a MacroExecutionException in this case.
                Throwable exception = assertThrows(MacroExecutionException.class,
                    () -> this.includeMacro.execute(parameters, null, macroContext));
                assertEquals("Found recursive inclusion of document [wiki:space.page]", exception.getMessage());
                throw exception;
            });

        // Verify that the exception bubbles up.
        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, macroContext));
        assertEquals("Found recursive inclusion of document [wiki:space.page]", exception.getMessage());
    }

    @Test
    void executeInsideSourceMetaDataBlockAndWithRelativeDocumentReferencePassed() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("relativePage");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        // Add a Source MetaData Block as a parent of the include Macro block.
        new MetaDataBlock(Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()),
            new MetaData(Collections.singletonMap(MetaData.BASE, "wiki:space.page")));

        DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "relativePage");
        setupDocumentMocks("relativePage", resolvedReference, "content");
        when(this.dab.getCurrentDocumentReference()).thenReturn(sourceReference);

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenSectionSpecified() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginHeader [1, Hsection]\n"
            + "onWord [section]\n"
            + "endHeader [1, Hsection]\n"
            + "beginParagraph\n"
            + "onWord [content2]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setSection("Hsection");
        parameters.setAuthor(Author.CURRENT);

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "content1\n\n= section =\ncontent2");

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenInvalidSectionSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setSection("unknown");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "content");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, macroContext));
        assertEquals("Cannot find section [unknown] in document [wiki:space.document]", exception.getMessage());
    }

    @Test
    void executeWhenExcludeFirstHeadingTrueAndSectionIsFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(true);
        parameters.setAuthor(Author.CURRENT);

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "= Heading =\ncontent");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenExcludeFirstHeadingTrueAndSectionSpecifiedAndHeadingIsFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(true);
        parameters.setSection("HHeading");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "= Heading =\ncontent");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        when(this.authorizationManager.hasAccess(Right.PROGRAM, INCLUDED_AUHOR, null)).thenReturn(true);

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenExcludeFirstHeadingFalseAndSectionIsFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginSection\n"
            + "beginHeader [1, Hcontent]\n"
            + "onWord [content]\n"
            + "endHeader [1, Hcontent]\n"
            + "endSection\n"
            + "endMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(false);
        parameters.setAuthor(Author.CURRENT);

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "=content=");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeWhenExcludeFirstHeadingTrueAndSectionIsNotFirstBlock() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "beginGroup\n"
            + "beginSection\n"
            + "beginHeader [1, Hcontent]\n"
            + "onWord [content]\n"
            + "endHeader [1, Hcontent]\n"
            + "endSection\n"
            + "endGroup\n"
            + "endMetaData [[source]=[wiki:space.document][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setExcludeFirstHeading(true);
        parameters.setAuthor(Author.CURRENT);

        // Getting the macro context
        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "(((= content =)))");

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);
        assertBlocks(expected, blocks, this.rendererFactory);
    }

    private MacroTransformationContext createMacroTransformationContext(String documentName, boolean isInline)
    {
        MacroTransformationContext context = new MacroTransformationContext();
        context.setInline(isInline);
        MacroBlock includeMacro =
            new MacroBlock("include", Collections.singletonMap("reference", documentName), isInline);
        XDOM xdom = new XDOM(List.of(includeMacro));
        context.setCurrentMacroBlock(includeMacro);
        context.setXDOM(xdom);

        return context;
    }

    private void setupDocumentMocks(String includedReferenceString, DocumentReference includedReference,
        String includedContent) throws Exception
    {
        setupDocumentMocks(includedReferenceString, includedReference, null, null, includedContent);
    }

    private void setupDocumentMocks(String includedDocumentReferenceString, DocumentReference includedDocumentReference,
        String includedPageReferenceString, PageReference includedPageReference, String includedContent)
        throws Exception
    {
        when(this.macroEntityReferenceResolver.resolve(eq(includedDocumentReferenceString), eq(EntityType.DOCUMENT),
            any(MacroBlock.class))).thenReturn(includedDocumentReference);
        when(this.dab.getDocumentInstance((EntityReference) includedDocumentReference))
            .thenReturn(this.includedDocument);
        if (includedPageReference != null) {
            when(this.macroEntityReferenceResolver.resolve(eq(includedPageReferenceString), eq(EntityType.PAGE),
                any(MacroBlock.class))).thenReturn(includedPageReference);
            when(this.dab.getDocumentInstance(includedPageReference))
                .thenReturn(this.includedDocument);
        }

        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, includedDocumentReference)).thenReturn(true);
        when(this.dab.getTranslatedDocumentInstance(this.includedDocument)).thenReturn(this.includedDocument);
        when(this.includedDocument.getDocumentReference()).thenReturn(includedDocumentReference);
        when(this.includedDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_0);
        XDOM xdom = toXDOM(includedContent);
        when(this.includedDocument.getPreparedXDOM()).thenReturn(xdom);
        when(this.includedDocument.getRealLanguage()).thenReturn("");
        when(this.includedDocument.getContentAuthorReference()).thenReturn(INCLUDED_AUHOR);
    }

    private XDOM toXDOM(String content) throws Exception
    {
        Parser parser = this.componentManager.getInstance(Parser.class, "xwiki/2.0");
        return parser.parse(new StringReader(content));
    }

    private List<Block> runIncludeMacroWithPreVelocity(Context context, String velocity, String includedContent)
        throws Exception
    {
        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer,
            "wiki:Space.IncludingPage", velocity);

        return runIncludeMacro(context, includedContent);
    }

    private List<Block> runIncludeMacro(final Context context, String includedContent) throws Exception
    {
        return runIncludeMacro(context, includedContent, false);
    }

    private List<Block> runIncludeMacro(final Context context, String includedContent, boolean restricted)
        throws Exception
    {
        return runIncludeMacro(context, null, includedContent, restricted);
    }

    private List<Block> runIncludeMacro(final Context context, final Author author, String includedContent,
        boolean restricted) throws Exception
    {
        DocumentReference includedDocumentReference = new DocumentReference("wiki", "Space", "IncludedPage");
        String includedDocStringRef = "wiki:space.page";
        setupDocumentMocks(includedDocStringRef, includedDocumentReference, includedContent);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference(includedDocStringRef);
        parameters.setContext(context);
        if (author != null) {
            parameters.setAuthor(author);
        }

        // Create a Macro transformation context with the Macro transformation object defined so that the include
        // macro can transform included page which is using a new context.
        MacroTransformation macroTransformation = this.componentManager.getInstance(Transformation.class, "macro");
        MacroTransformationContext macroContext = createMacroTransformationContext(includedDocStringRef, false);
        macroContext.setId("wiki:Space.IncludingPage");
        macroContext.setTransformation(macroTransformation);
        macroContext.getTransformationContext().setRestricted(restricted);

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        // Verify that push/pop are called when context is NEW
        if (context == Context.NEW) {
            verify(this.dab).pushDocumentInContext(any(Map.class), same(this.includedDocument));
            verify(this.dab).popDocumentFromContext(any(Map.class));
        } else {
            if (parameters.getAuthor() == Author.CURRENT || (parameters.getAuthor() == Author.AUTO
                && this.authorizationManager.hasAccess(Right.PROGRAM, INCLUDED_AUHOR, null))) {
                verifyNoInteractions(this.authorExecutor);
            } else {
                DocumentReference includedReference = this.includedDocument.getDocumentReference();
                verify(this.authorExecutor).call(any(), eq(INCLUDED_AUHOR), eq(includedReference));
            }
        }

        return blocks;
    }
}

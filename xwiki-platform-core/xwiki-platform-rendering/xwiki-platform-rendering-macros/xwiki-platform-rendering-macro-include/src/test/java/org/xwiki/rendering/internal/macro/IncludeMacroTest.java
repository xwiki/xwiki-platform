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
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
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
import static org.mockito.Mockito.verify;
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
@AllComponents(excludes = {
    CurrentMacroEntityReferenceResolver.class,
    DefaultAuthorizationManager.class
})
public class IncludeMacroTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private DocumentModelBridge includedDocument;

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

    // Register a WikiModel mock so that we're in wiki mode (otherwise links will be considered as URLs for ex).
    @MockComponent
    private WikiModel wikiModel;

    private IncludeMacro includeMacro;

    private PrintRendererFactory rendererFactory;

    /**
     * Mocks the component that is used to resolve the 'reference' parameter.
     */
    @MockComponent
    @Named("macro")
    private EntityReferenceResolver<String> macroEntityReferenceResolver;

    @BeforeEach
    public void setUp() throws Exception
    {
        MemoryConfigurationSource memoryConfigurationSource = new MemoryConfigurationSource();
        this.componentManager.registerComponent(ConfigurationSource.class, memoryConfigurationSource);
        this.componentManager.registerComponent(ConfigurationSource.class, "xwikicfg", memoryConfigurationSource);

        this.includeMacro = this.componentManager.getInstance(Macro.class, "include");
        this.rendererFactory = this.componentManager.getInstance(PrintRendererFactory.class, "event/1.0");

        // Put a fake XWiki context on the execution context.
        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContextManager ecm = this.componentManager.getInstance(ExecutionContextManager.class);
        ExecutionContext ec = new ExecutionContext();
        ecm.initialize(ec);
        execution.getContext().setProperty("xwikicontext", new HashMap<>());

        when(this.contextualAuthorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.PROGRAM)).thenReturn(true);
    }

    @Test
    void executeIncludeMacro() throws Exception
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

        List<Block> blocks = runIncludeMacro(Context.CURRENT, "word", false);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeIncludeMacroWithNewContextShowsVelocityMacrosAreIsolated() throws Exception
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
    void executeIncludeMacroWithNewContextShowsPassingOnRestrictedFlag() throws Exception
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
    void executeIncludeMacroWithCurrentContextShowsVelocityMacrosAreShared() throws Exception
    {
        // @formatter:off
        String expected = "beginDocument\n"
            + "beginMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "onMacroStandalone [velocity] [] [#testmacro]\n"
            + "endMetaData [[source]=[wiki:Space.IncludedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        // We verify that a Velocity macro set in the including page is seen in the included page.
        List<Block> blocks = runIncludeMacroWithPreVelocity(Context.CURRENT, "#macro(testmacro)#end",
            "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeIncludeMacroWithNoDocumentSpecified()
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
    void executeIncludeMacroWhenIncludingDocumentWithRelativeReferences() throws Exception
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
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true]\n"
            + "endParagraph\n"
            + "endMetaData [[base]=[includedWiki:includedSpace.includedPage]"
            + "[source]=[includedWiki:includedSpace.includedPage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";
        // @formatter:on

        DocumentReference includedDocumentReference =
            new DocumentReference("includedWiki", "includedSpace", "includedPage");
        setupDocumentMocks("includedWiki:includedSpace.includedPage", includedDocumentReference,
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
    }

    @Test
    void executeIncludeMacroWithRecursiveIncludeContextCurrent() throws Exception
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

    private static class ExpectedRecursiveInclusionException extends RuntimeException
    {
    }

    @Test
    void executeIncludeMacroWithRecursiveIncludeContextNew() throws Exception
    {
        DocumentDisplayer documentDisplayer = mock(DocumentDisplayer.class);

        this.includeMacro.setDocumentDisplayer(documentDisplayer);

        MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("wiki:space.page");
        parameters.setContext(Context.NEW);

        DocumentReference includedDocumentReference = new DocumentReference("wiki", "space", "page");
        setupDocumentMocks("wiki:space.page", includedDocumentReference, "");

        when(documentDisplayer.display(same(this.includedDocument), any(DocumentDisplayerParameters.class))).thenAnswer(
            (Answer) invocation -> {
                try {
                    this.includeMacro.execute(parameters, null, macroContext);
                } catch (Exception expected) {
                    if (expected.getMessage().contains("Found recursive inclusion")) {
                        throw new ExpectedRecursiveInclusionException();
                    }
                }
                return true;
            }
        );

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, macroContext));
        assertTrue(exception.getCause() instanceof ExpectedRecursiveInclusionException);
    }

    @Test
    void executeIncludeMacroInsideSourceMetaDataBlockAndWithRelativeDocumentReferencePassed() throws Exception
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
    void executeIncludeMacroWhenSectionSpecified() throws Exception
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

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "content1\n\n= section =\ncontent2");

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeIncludeMacroWhenInvalidSectionSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setSection("unknown");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "content");
        when(this.dab.getCurrentDocumentReference()).thenReturn(
            new DocumentReference("wiki", "Space", "IncludingPage"));

        Throwable exception = assertThrows(MacroExecutionException.class,
            () -> this.includeMacro.execute(parameters, null, macroContext));
        assertEquals("Cannot find section [unknown] in document [wiki:space.document]", exception.getMessage());
    }

    @Test
    void executeIncludeMacroWhenExcludeFirstHeadingTrueAndHeadingIsFirstBlock() throws Exception
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

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "= Heading =\ncontent");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeIncludeMacroWhenExcludeFirstHeadingFalseAndHeadingIsFirstBlock() throws Exception
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

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        setupDocumentMocks("document", resolvedReference, "=content=");
        when(this.dab.getCurrentDocumentReference())
            .thenReturn(new DocumentReference("wiki", "Space", "IncludingPage"));

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    void executeIncludeMacroWhenExcludeFirstHeadingTrueAndHeadingIsNotFirstBlock() throws Exception
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
        MacroBlock includeMacro =
            new MacroBlock("include", Collections.singletonMap("reference", documentName), isInline);
        context.setCurrentMacroBlock(includeMacro);
        return context;
    }

    private void setupDocumentMocks(String includedReferenceString, DocumentReference includedReference,
        String includedContent) throws Exception
    {
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, includedReference)).thenReturn(true);
        when(this.macroEntityReferenceResolver.resolve(eq(includedReferenceString), eq(EntityType.DOCUMENT),
            any(MacroBlock.class))).thenReturn(includedReference);
        when(this.dab.getDocumentInstance((EntityReference) includedReference)).thenReturn(this.includedDocument);
        when(this.dab.getTranslatedDocumentInstance(this.includedDocument)).thenReturn(this.includedDocument);
        when(this.includedDocument.getDocumentReference()).thenReturn(includedReference);
        when(this.includedDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_0);
        when(this.includedDocument.getXDOM()).thenReturn(getXDOM(includedContent));
        when(this.includedDocument.getRealLanguage()).thenReturn("");
    }

    private XDOM getXDOM(String content) throws Exception
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
        DocumentReference includedDocumentReference = new DocumentReference("wiki", "Space", "IncludedPage");
        String includedDocStringRef = "wiki:space.page";
        setupDocumentMocks(includedDocStringRef, includedDocumentReference, includedContent);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference(includedDocStringRef);
        parameters.setContext(context);

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
        }

        return blocks;
    }
}

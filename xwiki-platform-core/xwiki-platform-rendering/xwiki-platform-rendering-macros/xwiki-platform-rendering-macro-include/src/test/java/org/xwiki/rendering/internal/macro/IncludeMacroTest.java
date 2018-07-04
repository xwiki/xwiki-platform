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

import org.hamcrest.collection.IsArray;
import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import static org.xwiki.rendering.test.BlockAssert.assertBlocks;
import static org.xwiki.rendering.test.BlockAssert.assertBlocksStartsWith;

/**
 * Unit tests for {@link IncludeMacro}.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class IncludeMacroTest extends AbstractComponentTestCase
{
    private ScriptMockSetup mockSetup;

    private IncludeMacro includeMacro;

    private PrintRendererFactory rendererFactory;

    /**
     * Mocks the component that is used to resolve the 'reference' parameter.
     */
    private EntityReferenceResolver<String> mockEntityReferenceResolver;

    private ContextualAuthorizationManager mockContextualAuthorization;

    private DocumentModelBridge mockDocument;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // Put a fake XWiki context on the execution context.
        Execution execution = getComponentManager().getInstance(Execution.class);
        execution.getContext().setProperty("xwikicontext", new HashMap<Object, Object>());
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.mockSetup = new ScriptMockSetup(getMockery(), getComponentManager());

        this.mockEntityReferenceResolver =
            registerMockComponent(EntityReferenceResolver.TYPE_STRING, "macro", "macroDocumentReferenceResolver");
        // Make sure to not load the standard AuthorizationManager which trigger too many things
        registerMockComponent(AuthorizationManager.class);

        this.mockContextualAuthorization = getComponentManager().getInstance(ContextualAuthorizationManager.class);
        this.includeMacro = getComponentManager().getInstance(Macro.class, "include");
        this.rendererFactory = getComponentManager().getInstance(PrintRendererFactory.class, "event/1.0");
    }

    @Test
    public void testIncludeMacro() throws Exception
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
    public void testIncludeMacroWithNewContextShowsVelocityMacrosAreIsolated() throws Exception
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
    public void testIncludeMacroWithNewContextShowsPassingOnRestrictedFlag() throws Exception
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
    public void testIncludeMacroWithCurrentContextShowsVelocityMacrosAreShared() throws Exception
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
    public void testIncludeMacroWithNoDocumentSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();

        try {
            this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));
            Assert.fail("An exception should have been thrown");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'reference' parameter pointing to the entity to include.",
                expected.getMessage());
        }
    }

    /**
     * Verify that relative links returned by the Include macro as wrapped with a MetaDataBlock.
     */
    @Test
    public void testIncludeMacroWhenIncludingDocumentWithRelativeReferences() throws Exception
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

        final DocumentReference includedDocumentReference =
            new DocumentReference("includedWiki", "includedSpace", "includedPage");
        setUpDocumentMock("includedWiki:includedSpace.includedPage", includedDocumentReference,
            "[[page]] [[attach:test.png]] image:test.png");
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(any(EntityReference.class)));
                will(returnValue(true));
                oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)),
                    with(any(DocumentModelBridge.class)));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(includedDocumentReference));
                oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            }
        });

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("includedWiki:includedSpace.includedPage");
        parameters.setContext(Context.NEW);

        List<Block> blocks =
            this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWithRecursiveIncludeContextCurrent() throws Exception
    {
        this.includeMacro.setDocumentAccessBridge(mockSetup.bridge);

        final MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);
        // Add an Include Macro MarkerBlock as a parent of the include Macro block since this is what would have
        // happened if an Include macro is included in another Include macro.
        final MacroMarkerBlock includeMacroMarker =
            new MacroMarkerBlock("include", Collections.singletonMap("reference", "space.page"),
                Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()), false);

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockEntityReferenceResolver).resolve("wiki:space.page", EntityType.DOCUMENT,
                    macroContext.getCurrentMacroBlock());
                will(returnValue(new DocumentReference("wiki", "space", "page")));
                allowing(mockEntityReferenceResolver).resolve("space.page", EntityType.DOCUMENT, includeMacroMarker);
                will(returnValue(new DocumentReference("wiki", "space", "page")));
            }
        });

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("wiki:space.page");
        parameters.setContext(Context.CURRENT);

        try {
            this.includeMacro.execute(parameters, null, macroContext);
            Assert.fail("The include macro hasn't checked the recursive inclusion");
        } catch (MacroExecutionException expected) {
            if (!expected.getMessage().startsWith("Found recursive inclusion")) {
                throw expected;
            }
        }
    }

    private static class ExpectedRecursiveInclusionException extends RuntimeException
    {
    }

    @Test
    public void testIncludeMacroWithRecursiveIncludeContextNew() throws Exception
    {
        final DocumentDisplayer mockDocumentDisplayer = getMockery().mock(DocumentDisplayer.class);

        this.includeMacro.setDocumentAccessBridge(mockSetup.bridge);
        this.includeMacro.setDocumentDisplayer(mockDocumentDisplayer);

        final MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);

        final IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("wiki:space.page");
        parameters.setContext(Context.NEW);

        final DocumentReference includedDocumentReference = new DocumentReference("wiki", "space", "page");
        setUpDocumentMock("wiki:space.page", includedDocumentReference, "");
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(any(EntityReference.class)));
                will(returnValue(true));

                allowing(mockDocumentDisplayer).display(with(same(mockDocument)),
                    with(any(DocumentDisplayerParameters.class)));
                will(new CustomAction("recursively call the include macro again")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        try {
                            includeMacro.execute(parameters, null, macroContext);
                        } catch (Exception expected) {
                            if (expected.getMessage().contains("Found recursive inclusion")) {
                                throw new ExpectedRecursiveInclusionException();
                            }
                        }
                        return true;
                    }
                });
            }
        });

        try {
            this.includeMacro.execute(parameters, null, macroContext);
            Assert.fail("The include macro hasn't checked the recursive inclusion");
        } catch (MacroExecutionException expected) {
            if (!(expected.getCause() instanceof ExpectedRecursiveInclusionException)) {
                throw expected;
            }
        }
    }

    @Test
    public void testIncludeMacroInsideSourceMetaDataBlockAndWithRelativeDocumentReferencePassed() throws Exception
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

        final MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        // Add a Source MetaData Block as a parent of the include Macro block.
        new MetaDataBlock(Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()),
            new MetaData(Collections.<String, Object>singletonMap(MetaData.BASE, "wiki:space.page")));

        final DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "relativePage");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockEntityReferenceResolver).resolve("relativePage", EntityType.DOCUMENT,
                    macroContext.getCurrentMacroBlock());
                will(returnValue(resolvedReference));
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(resolvedReference));
                will(returnValue(true));
                oneOf(mockSetup.bridge).getDocumentInstance((EntityReference) resolvedReference);
                will(returnValue(mockDocument));
                oneOf(mockSetup.bridge).getTranslatedDocumentInstance(resolvedReference);
                will(returnValue(mockDocument));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(sourceReference));
                oneOf(mockDocument).getXDOM();
                will(returnValue(getXDOM("content")));
                oneOf(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
                allowing(mockDocument).getDocumentReference();
                will(returnValue(resolvedReference));
                allowing(mockDocument).getRealLanguage();
                will(returnValue(""));
            }
        });

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWhenSectionSpecified() throws Exception
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

        final MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockEntityReferenceResolver).resolve("document", EntityType.DOCUMENT,
                    macroContext.getCurrentMacroBlock());
                will(returnValue(resolvedReference));
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(resolvedReference));
                will(returnValue(true));
                oneOf(mockSetup.bridge).getDocumentInstance((EntityReference) resolvedReference);
                will(returnValue(mockDocument));
                oneOf(mockSetup.bridge).getTranslatedDocumentInstance(resolvedReference);
                will(returnValue(mockDocument));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
                allowing(mockDocument).getDocumentReference();
                will(returnValue(resolvedReference));
                oneOf(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
                oneOf(mockDocument).getXDOM();
                will(returnValue(getXDOM("content1\n\n= section =\ncontent2")));
                allowing(mockDocument).getRealLanguage();
                will(returnValue(""));
            }
        });

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWhenInvalidSectionSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference("document");
        parameters.setSection("unknown");

        final MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockEntityReferenceResolver).resolve("document", EntityType.DOCUMENT,
                    macroContext.getCurrentMacroBlock());
                will(returnValue(resolvedReference));
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(resolvedReference));
                will(returnValue(true));
                oneOf(mockSetup.bridge).getDocumentInstance((EntityReference) resolvedReference);
                will(returnValue(mockDocument));
                allowing(mockSetup.bridge).getTranslatedDocumentInstance(resolvedReference);
                will(returnValue(mockDocument));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
                oneOf(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
                oneOf(mockDocument).getXDOM();
                will(returnValue(getXDOM("content")));
                allowing(mockDocument).getDocumentReference();
                will(returnValue(resolvedReference));
                allowing(mockDocument).getRealLanguage();
                will(returnValue(""));
            }
        });

        try {
            this.includeMacro.execute(parameters, null, macroContext);
            Assert.fail("Should have raised an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("Cannot find section [unknown] in document [wiki:space.document]",
                expected.getMessage());
        }
    }

    private MacroTransformationContext createMacroTransformationContext(String documentName, boolean isInline)
    {
        MacroTransformationContext context = new MacroTransformationContext();
        MacroBlock includeMacro =
            new MacroBlock("include", Collections.singletonMap("reference", documentName), isInline);
        context.setCurrentMacroBlock(includeMacro);
        return context;
    }

    private void setUpDocumentMock(final String resolve, final DocumentReference reference, final String content)
        throws Exception
    {
        this.mockDocument = getMockery().mock(DocumentModelBridge.class, resolve);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockEntityReferenceResolver).resolve(with(resolve), with(EntityType.DOCUMENT),
                    with(IsArray.array(any(MacroBlock.class))));
                will(returnValue(reference));
                allowing(mockSetup.bridge).getDocumentInstance((EntityReference) reference);
                will(returnValue(mockDocument));
                allowing(mockSetup.bridge).getTranslatedDocumentInstance(reference);
                will(returnValue(mockDocument));
                allowing(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
                allowing(mockDocument).getXDOM();
                will(returnValue(getXDOM(content)));
                allowing(mockDocument).getDocumentReference();
                will(returnValue(reference));
                allowing(mockDocument).getRealLanguage();
                will(returnValue(""));
            }
        });
    }

    private XDOM getXDOM(String content) throws Exception
    {
        Parser parser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");
        return parser.parse(new StringReader(content));
    }

    private List<Block> runIncludeMacroWithPreVelocity(Context context, String velocity, String includedContent)
        throws Exception
    {
        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
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
        final DocumentReference includedDocumentReference = new DocumentReference("wiki", "Space", "IncludedPage");
        String includedDocStringRef = "wiki:space.page";
        setUpDocumentMock(includedDocStringRef, includedDocumentReference, includedContent);
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockContextualAuthorization).hasAccess(with(Right.VIEW), with(same(includedDocumentReference)));
                will(returnValue(true));
                // Verify that push/pop are called when context is NEW
                if (context == Context.NEW) {
                    oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(same(mockDocument)));
                    oneOf(mockSetup.bridge).getCurrentDocumentReference();
                    will(returnValue(includedDocumentReference));
                    oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
                } else {
                    oneOf(mockSetup.bridge).getCurrentDocumentReference();
                    will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
                }
            }
        });
        this.includeMacro.setDocumentAccessBridge(this.mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setReference(includedDocStringRef);
        parameters.setContext(context);

        // Create a Macro transformation context with the Macro transformation object defined so that the include
        // macro can transform included page which is using a new context.
        MacroTransformation macroTransformation = getComponentManager().getInstance(Transformation.class, "macro");
        MacroTransformationContext macroContext = createMacroTransformationContext(includedDocStringRef, false);
        macroContext.setId("wiki:Space.IncludingPage");
        macroContext.setTransformation(macroTransformation);
        macroContext.getTransformationContext().setRestricted(restricted);

        return this.includeMacro.execute(parameters, null, macroContext);
    }
}

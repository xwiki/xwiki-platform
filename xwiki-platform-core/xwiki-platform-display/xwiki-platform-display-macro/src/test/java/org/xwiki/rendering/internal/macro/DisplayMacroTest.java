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

import org.apache.commons.lang3.reflect.FieldUtils;
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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.display.DisplayMacro;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.display.DisplayMacroParameters;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import static org.xwiki.rendering.test.BlockAssert.assertBlocks;

/**
 * Unit tests for {@link DisplayMacro}.
 *
 * @version $Id$
 */
public class DisplayMacroTest extends AbstractComponentTestCase
{
    private ScriptMockSetup mockSetup;

    private DisplayMacro displayMacro;

    private PrintRendererFactory rendererFactory;

    /**
     * Mocks the component that is used to resolve the 'reference' parameter.
     */
    private DocumentReferenceResolver<String> mockDocumentReferenceResolver;

    private AuthorizationManager mockAuthorization;

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

        this.mockDocumentReferenceResolver =
            registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "macro", "macroDocumentReferenceResolver");
        this.mockAuthorization = registerMockComponent(AuthorizationManager.class);

        this.displayMacro = (DisplayMacro) getComponentManager().getInstance(Macro.class, "display");
        this.rendererFactory = getComponentManager().getInstance(PrintRendererFactory.class, "event/1.0");
    }

    @Test
    public void testDisplayMacroShowsVelocityMacrosAreIsolated() throws Exception
    {
        String expected =
            "beginDocument\n" + "beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n" + "beginMacroMarkerStandalone [velocity] [] [#testmacro]\n"
                + "beginParagraph\n" + "onSpecialSymbol [#]\n" + "onWord [testmacro]\n" + "endParagraph\n"
                + "endMacroMarkerStandalone [velocity] [] [#testmacro]\n"
                + "endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n" + "endDocument";

        // We verify that a Velocity macro set in the including page is not seen in the displayed page.
        List<Block> blocks =
            runDisplayMacroWithPreVelocity("#macro(testmacro)#end", "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testDisplayMacroWithNoDocumentSpecified() throws Exception
    {
        DisplayMacroParameters parameters = new DisplayMacroParameters();

        try {
            this.displayMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));
            Assert.fail("An exception should have been thrown");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'reference' parameter pointing to the entity to display.",
                expected.getMessage());
        }
    }

    /**
     * Verify that relative links returned by the display macro as wrapped with a MetaDataBlock.
     */
    @Test
    public void testDisplayMacroWhenDisplayingDocumentWithRelativeReferences() throws Exception
    {
        String expected = "beginDocument\n" + "beginMetaData [[base]=[displayedWiki:displayedSpace.displayedPage]"
            + "[source]=[displayedWiki:displayedSpace.displayedPage][syntax]=[XWiki 2.0]]\n" + "beginParagraph\n"
            + "beginLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "endLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n" + "onSpace\n"
            + "beginLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "endLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n" + "onSpace\n"
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true]\n" + "endParagraph\n"
            + "endMetaData [[base]=[displayedWiki:displayedSpace.displayedPage]"
            + "[source]=[displayedWiki:displayedSpace.displayedPage][syntax]=[XWiki 2.0]]\n" + "endDocument";

        final DocumentReference displayedDocumentReference =
            new DocumentReference("displayedWiki", "displayedSpace", "displayedPage");
        setUpDocumentMock("displayedWiki:displayedSpace.displayedPage", displayedDocumentReference,
            "[[page]] [[attach:test.png]] image:test.png");
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class)));
                will(returnValue(true));
                oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)),
                    with(any(DocumentModelBridge.class)));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(displayedDocumentReference));
                oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            }
        });

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("displayedWiki:displayedSpace.displayedPage");

        List<Block> blocks =
            this.displayMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    private static class ExpectedRecursiveInclusionException extends RuntimeException
    {
    }

    @Test
    public void testDisplayMacroWithRecursiveDisplay() throws Exception
    {
        final DocumentDisplayer mockDocumentDisplayer = getMockery().mock(DocumentDisplayer.class);

        this.displayMacro.setDocumentAccessBridge(mockSetup.bridge);
        FieldUtils.writeField(this.displayMacro, "documentDisplayer", mockDocumentDisplayer, true);

        final MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);

        final DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("wiki:space.page");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentReferenceResolver).resolve("wiki:space.page", macroContext.getCurrentMacroBlock());
                will(returnValue(new DocumentReference("wiki", "space", "page")));

                allowing(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class)));
                will(returnValue(true));
                allowing(mockSetup.bridge).getDocument(with(any(DocumentReference.class)));
                will(returnValue(null));

                allowing(mockDocumentDisplayer).display(with(same((DocumentModelBridge) null)),
                    with(any(DocumentDisplayerParameters.class)));
                will(new CustomAction("recursively call the include macro again")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        try {
                            displayMacro.execute(parameters, null, macroContext);
                        } catch (Exception expected) {
                            if (expected.getMessage().contains("Found recursive display")) {
                                throw new ExpectedRecursiveInclusionException();
                            }
                        }
                        return true;
                    }
                });
            }
        });

        try {
            this.displayMacro.execute(parameters, null, macroContext);
            Assert.fail("The display macro hasn't checked the recursive display");
        } catch (MacroExecutionException expected) {
            if (!(expected.getCause() instanceof ExpectedRecursiveInclusionException)) {
                throw expected;
            }
        }
    }

    @Test
    public void testDisplayMacroInsideBaseMetaDataBlockAndWithRelativeDocumentReferencePassed() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[base]=[wiki:space.relativePage][source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "beginParagraph\n" + "onWord [content]\n" + "endParagraph\n"
            + "endMetaData [[base]=[wiki:space.relativePage][source]=[wiki:space.relativePage][syntax]=[XWiki 2.0]]\n"
            + "endDocument";

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setReference("relativePage");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        // Add a Source MetaData Block as a parent of the display Macro block.
        new MetaDataBlock(Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()),
            new MetaData(Collections.<String, Object>singletonMap(MetaData.BASE, "wiki:space.page")));

        final DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "relativePage");
        setUpDocumentMock("relativePage", resolvedReference, "content");

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentReferenceResolver).resolve(with("wiki:space.page"),
                    with(IsArray.array(any(MacroBlock.class))));
                will(returnValue(sourceReference));
                allowing(mockDocumentReferenceResolver).resolve(with("relativePage"),
                    with(IsArray.array(any(MacroBlock.class))));
                will(returnValue(resolvedReference));
                oneOf(mockSetup.bridge).isDocumentViewable(resolvedReference);
                will(returnValue(true));
                oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(same(mockDocument)));
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(resolvedReference));
                oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            }
        });

        List<Block> blocks = this.displayMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testDisplayMacroWhenSectionSpecified() throws Exception
    {
        String expected =
            "beginDocument\n" + "beginMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n" + "beginHeader [1, Hsection]\n" + "onWord [section]\n"
                + "endHeader [1, Hsection]\n" + "beginParagraph\n" + "onWord [content2]\n" + "endParagraph\n"
                + "endMetaData [[base]=[wiki:Space.DisplayedPage][source]=[wiki:Space.DisplayedPage]"
                + "[syntax]=[XWiki 2.0]]\n" + "endDocument";

        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setSection("Hsection");

        List<Block> blocks = runDisplayMacro(parameters, "content1\n\n= section =\ncontent2");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testDisplayMacroWhenInvalidSectionSpecified() throws Exception
    {
        DisplayMacroParameters parameters = new DisplayMacroParameters();
        parameters.setSection("unknown");

        try {
            runDisplayMacro(parameters, "content");
            Assert.fail("Should have raised an exception");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("Cannot find section [unknown] in document [wiki:Space.DisplayedPage]",
                expected.getMessage());
        }
    }

    private MacroTransformationContext createMacroTransformationContext(String documentName, boolean isInline)
    {
        MacroTransformationContext context = new MacroTransformationContext();
        MacroBlock displayMacro =
            new MacroBlock("display", Collections.singletonMap("reference", documentName), isInline);
        context.setCurrentMacroBlock(displayMacro);
        return context;
    }

    private void setUpDocumentMock(final String resolve, final DocumentReference reference, final String content)
        throws Exception
    {
        mockDocument = getMockery().mock(DocumentModelBridge.class, resolve);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDocumentReferenceResolver).resolve(with(resolve),
                    with(IsArray.array(any(MacroBlock.class))));
                will(returnValue(reference));
                allowing(mockSetup.bridge).getDocument(reference);
                will(returnValue(mockDocument));
                allowing(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
                allowing(mockDocument).getXDOM();
                will(returnValue(getXDOM(content)));
                allowing(mockDocument).getDocumentReference();
                will(returnValue(reference));
            }
        });
    }

    private XDOM getXDOM(String content) throws Exception
    {
        Parser xwiki20Parser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");
        return xwiki20Parser.parse(new StringReader(content));
    }

    private List<Block> runDisplayMacroWithPreVelocity(String velocity, String displayedContent) throws Exception
    {
        VelocityManager velocityManager = getComponentManager().getInstance(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer,
            "wiki:Space.DisplayingPage", velocity);

        return runDisplayMacro(displayedContent);
    }

    private List<Block> runDisplayMacro(String displayedContent) throws Exception
    {
        return runDisplayMacro(new DisplayMacroParameters(), displayedContent);
    }

    private List<Block> runDisplayMacro(DisplayMacroParameters parameters, String displayedContent) throws Exception
    {
        final DocumentReference displayedDocumentReference = new DocumentReference("wiki", "Space", "DisplayedPage");
        String displayedDocStringRef = "wiki:space.page";
        setUpDocumentMock(displayedDocStringRef, displayedDocumentReference, displayedContent);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockSetup.bridge).isDocumentViewable(with(same(displayedDocumentReference)));
                will(returnValue(true));
                oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(same(mockDocument)));
                atMost(1).of(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(displayedDocumentReference));
                oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            }
        });
        this.displayMacro.setDocumentAccessBridge(this.mockSetup.bridge);

        parameters.setReference(displayedDocStringRef);

        // Create a Macro transformation context with the Macro transformation object defined so that the display
        // macro can transform displayed page which is using a new context.
        MacroTransformation macroTransformation =
            (MacroTransformation) getComponentManager().getInstance(Transformation.class, "macro");
        MacroTransformationContext macroContext = createMacroTransformationContext(displayedDocStringRef, false);
        macroContext.setId("wiki:Space.DisplayingPage");
        macroContext.setTransformation(macroTransformation);

        return this.displayMacro.execute(parameters, null, macroContext);
    }
}

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

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
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
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import static org.xwiki.rendering.test.BlockAssert.*;

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

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        // Put a fake XWiki context on the execution context.
        getComponentManager().lookup(Execution.class).getContext()
            .setProperty("xwikicontext", new HashMap<Object, Object>());
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        
        this.mockSetup = new ScriptMockSetup(getMockery(), getComponentManager());
        this.includeMacro = (IncludeMacro) getComponentManager().lookup(Macro.class, "include");
        this.rendererFactory = getComponentManager().lookup(PrintRendererFactory.class, "event/1.0");
    }

    @Test
    public void testIncludeMacroWithNewContextShowsVelocityMacrosAreIsolated() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[syntax]=[XWiki 2.0][source]=[wiki:space.page]]\n"
            + "beginMacroMarkerStandalone [velocity] [] [#testmacro]\n"
            + "beginParagraph\n"
            + "onSpecialSymbol [#]\n"
            + "onWord [testmacro]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [#testmacro]\n"
            + "endMetaData [[syntax]=[XWiki 2.0][source]=[wiki:space.page]]\n"
            + "endDocument";

        // We verify that a Velocity macro set in the including page is not seen in the included page.
        List<Block> blocks = runIncludeMacroWithPreVelocity(Context.NEW, "#macro(testmacro)#end",
            "{{velocity}}#testmacro{{/velocity}}");

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWithCurrentContextShowsVelocityMacrosAreShared() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[syntax]=[XWiki 2.0][source]=[wiki:space.page]]\n"
            + "onMacroStandalone [velocity] [] [#testmacro]\n"
            + "endMetaData [[syntax]=[XWiki 2.0][source]=[wiki:space.page]]\n"
            + "endDocument";

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
            Assert.assertEquals("You must specify a 'document' parameter pointing to the document to include.",
                expected.getMessage());
        }
    }

    /**
     * Verify that relative links returned by the Include macro as wrapped with a MetaDataBlock.
     */
    @Test
    public void testIncludeMacroWhenIncludingDocumentWithRelativeReferences() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[syntax]=[XWiki 2.0][source]=[includedWiki:includedSpace.includedPage]]\n"
            + "beginParagraph\n"
            + "beginLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "endLink [Typed = [false] Type = [doc] Reference = [page]] [false]\n"
            + "onSpace\n"
            + "beginLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "endLink [Typed = [true] Type = [attach] Reference = [test.png]] [false]\n"
            + "onSpace\n"
            + "onImage [Typed = [false] Type = [attach] Reference = [test.png]] [true]\n"
            + "endParagraph\n"
            + "endMetaData [[syntax]=[XWiki 2.0][source]=[includedWiki:includedSpace.includedPage]]\n"
            + "endDocument";

        setUpDocumentMock("includedWiki:includedSpace.includedPage",
            new DocumentReference("includedWiki", "includedSpace", "includedPage"),
            "[[page]] [[attach:test.png]] image:test.png");
        getMockery().checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
        }});
        
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("includedWiki:includedSpace.includedPage");
        parameters.setContext(Context.NEW);

        List<Block> blocks = this.includeMacro.execute(parameters, null,
            createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
    }
    
    @Test
    public void testIncludeMacroWithRecursiveInclude() throws Exception
    {
        getMockery().checking(new Expectations() {{
            allowing(mockSetup.documentReferenceResolver).resolve("wiki:space.page");
                will(returnValue(new DocumentReference("wiki", "space", "page")));
            allowing(mockSetup.documentReferenceResolver).resolve("space.page");
                will(returnValue(new DocumentReference("wiki", "space", "page")));
        }});

        this.includeMacro.setDocumentAccessBridge(mockSetup.bridge);

        MacroTransformationContext macroContext = createMacroTransformationContext("wiki:space.page", false);
        // Add an Include Macro MarkerBlock as a parent of the include Macro block since this is what would have
        // happened if an Include macro is included in another Include macro.
        new MacroMarkerBlock("include", Collections.singletonMap("document", "space.page"),
            Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()), false);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:space.page");
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

    @Test
    public void testIncludeMacroInsideSourceMetaDataBlockAndWithRelativeDocumentReferencePassed()
        throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[syntax]=[XWiki 2.0][source]=[relativePage]]\n"
            + "beginParagraph\n"
            + "onWord [content]\n"
            + "endParagraph\n"
            + "endMetaData [[syntax]=[XWiki 2.0][source]=[relativePage]]\n"
            + "endDocument";

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("relativePage");

        MacroTransformationContext macroContext = createMacroTransformationContext("whatever", false);
        // Add a Source MetaData Block as a parent of the include Macro block.
        new MetaDataBlock(Collections.<Block>singletonList(macroContext.getCurrentMacroBlock()),
            new MetaData(Collections.<String, Object>singletonMap(MetaData.SOURCE, "wiki:space.page")));

        final DocumentReference sourceReference = new DocumentReference("wiki", "space", "page");
        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "relativePage");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(mockSetup.documentReferenceResolver).resolve("wiki:space.page");
                will(returnValue(sourceReference));
            oneOf(mockSetup.documentReferenceResolver).resolve("relativePage", sourceReference);
                will(returnValue(resolvedReference));
            oneOf(mockSetup.bridge).isDocumentViewable(resolvedReference); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocument(resolvedReference); will(returnValue(mockDocument));
            oneOf(mockSetup.bridge).getCurrentDocumentReference(); will(returnValue(sourceReference));
            oneOf(mockDocument).getXDOM(); will(returnValue(getXDOM("content")));
            oneOf(mockDocument).getSyntax(); will(returnValue(Syntax.XWIKI_2_0));
        }});

        List<Block> blocks = this.includeMacro.execute(parameters, null, macroContext);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWhenSectionSpecified() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMetaData [[syntax]=[XWiki 2.0][source]=[document]]\n"
            + "beginHeader [1, Hsection]\n"
            + "onWord [section]\n"
            + "endHeader [1, Hsection]\n"
            + "beginParagraph\n"
            + "onWord [content2]\n"
            + "endParagraph\n"
            + "endMetaData [[syntax]=[XWiki 2.0][source]=[document]]\n"
            + "endDocument";

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("document");
        parameters.setSection("Hsection");

        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(mockSetup.documentReferenceResolver).resolve("document");
                will(returnValue(resolvedReference));
            oneOf(mockSetup.bridge).isDocumentViewable(resolvedReference); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocument(resolvedReference); will(returnValue(mockDocument));
            oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
            oneOf(mockDocument).getSyntax(); will(returnValue(Syntax.XWIKI_2_0));
            oneOf(mockDocument).getXDOM(); will(returnValue(getXDOM("content1\n\n= section =\ncontent2")));
        }});

        List<Block> blocks = this.includeMacro.execute(parameters, null,
            createMacroTransformationContext("whatever", false));

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWhenInvalidSectionSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("document");
        parameters.setSection("unknown");

        final DocumentReference resolvedReference = new DocumentReference("wiki", "space", "document");
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations() {{
            oneOf(mockSetup.documentReferenceResolver).resolve("document");
                will(returnValue(resolvedReference));
            oneOf(mockSetup.bridge).isDocumentViewable(resolvedReference); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocument(resolvedReference); will(returnValue(mockDocument));
            oneOf(mockSetup.bridge).getCurrentDocumentReference();
                will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
            oneOf(mockDocument).getSyntax(); will(returnValue(Syntax.XWIKI_2_0));
            oneOf(mockDocument).getXDOM(); will(returnValue(getXDOM("content")));
            oneOf(mockDocument).getDocumentReference(); will(returnValue(resolvedReference));
        }});

        try {
            this.includeMacro.execute(parameters, null, createMacroTransformationContext("whatever", false));
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
            new MacroBlock("include", Collections.singletonMap("document", documentName), isInline);
        context.setCurrentMacroBlock(includeMacro);
        return context;
    }

    private void setUpDocumentMock(final String resolve, final DocumentReference reference, final String content)
        throws Exception
    {
        final DocumentModelBridge mockDocument = getMockery().mock(DocumentModelBridge.class, resolve);
        getMockery().checking(new Expectations() {{
            allowing(mockSetup.documentReferenceResolver).resolve(resolve);
                will(returnValue(reference));
            allowing(mockSetup.bridge).getDocument(reference); will(returnValue(mockDocument));
            allowing(mockDocument).getSyntax(); will(returnValue(Syntax.XWIKI_2_0));
            allowing(mockDocument).getXDOM(); will(returnValue(getXDOM(content)));
            allowing(mockDocument).getDocumentReference(); will(returnValue(reference));
        }});
    }

    private XDOM getXDOM(String content) throws Exception
    {
        return getComponentManager().lookup(Parser.class, "xwiki/2.0").parse(new StringReader(content));
    }

    private List<Block> runIncludeMacroWithPreVelocity(Context context, String velocity, String includedContent)
        throws Exception
    {
        VelocityManager velocityManager = getComponentManager().lookup(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer,
            "wiki:Space.IncludingPage", velocity);

        return runIncludeMacro(context, includedContent);
    }

    private List<Block> runIncludeMacro(final Context context, String includedContent) throws Exception
    {
        final DocumentReference includedDocumentReference = new DocumentReference("wiki", "Space", "IncludedPage");
        String includedDocStringRef = "wiki:space.page";
        setUpDocumentMock(includedDocStringRef, includedDocumentReference, includedContent);
        getMockery().checking(new Expectations() {{
            allowing(mockSetup.bridge).isDocumentViewable(with(same(includedDocumentReference)));
                will(returnValue(true));
            // Verify that push/pop are called when context is NEW
            if (context == Context.NEW) {
                oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)),
                    with(same(includedDocumentReference)));
                oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            } else {
                oneOf(mockSetup.bridge).getCurrentDocumentReference();
                    will(returnValue(new DocumentReference("wiki", "Space", "IncludingPage")));
            }
        }});
        this.includeMacro.setDocumentAccessBridge(this.mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument(includedDocStringRef);
        parameters.setContext(context);

        // Create a Macro transformation context with the Macro transformation object defined so that the include
        // macro can transform included page which is using a new context.
        MacroTransformation macroTransformation =
            (MacroTransformation) getComponentManager().lookup(Transformation.class, "macro");
        MacroTransformationContext macroContext = createMacroTransformationContext(includedDocStringRef, false);
        macroContext.setId("wiki:Space.IncludingPage");
        macroContext.setTransformation(macroTransformation);

        return this.includeMacro.execute(parameters, null, macroContext);
    }
}

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

import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MacroMarkerBlock;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.internal.transformation.MacroTransformation;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

import static org.xwiki.rendering.scaffolding.BlockAssert.*;

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
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        
        this.mockSetup = new ScriptMockSetup(getComponentManager());
        this.includeMacro = (IncludeMacro) getComponentManager().lookup(Macro.class, "include");
        this.rendererFactory = getComponentManager().lookup(PrintRendererFactory.class, "event/1.0");
    }

    @Test
    public void testIncludeMacroWithNewContext() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginMacroMarkerStandalone [velocity] [] [$myvar]\n"
            + "beginParagraph\n"
            + "onWord [hello]\n"
            + "endParagraph\n"
            + "endMacroMarkerStandalone [velocity] [] [$myvar]\n"
            + "endDocument";

        // Since it's not in the same context, we verify that a Velocity variable set in the including page is not
        // seen in the included page.
        VelocityManager velocityManager = getComponentManager().lookup(VelocityManager.class);
        StringWriter writer = new StringWriter();
        velocityManager.getVelocityEngine().evaluate(velocityManager.getVelocityContext(), writer, "template",
            "#set ($myvar = 'hello')");

        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocumentContent(with(any(String.class))); 
                will(returnValue("{{velocity}}$myvar{{/velocity}}"));
            oneOf(mockSetup.bridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue(Syntax.XWIKI_2_0.toIdString()));
            oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            oneOf(mockSetup.documentReferenceResolver).resolve("wiki:Space.Page");
                will(returnValue(new DocumentReference("wiki", "Space", "Page")));
        }});
        this.includeMacro.setDocumentAccessBridge(this.mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.NEW);

        // Create a Macro transformation context with the Macro transformation object defined so that the include
        // macro can transform included page which is using a new context.
        MacroTransformation macroTransformation =
            (MacroTransformation) getComponentManager().lookup(Transformation.class, "macro");
        MacroTransformationContext context = new MacroTransformationContext();
        context.setTransformation(macroTransformation);

        List<Block> blocks = this.includeMacro.execute(parameters, null, context);

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWithCurrentContext() throws Exception
    {
        String expected = "beginDocument\n"
            + "onMacroStandalone [someMacro] []\n"
            + "endDocument";

        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocumentContent(with(any(String.class))); 
                will(returnValue("{{someMacro/}}"));
            oneOf(mockSetup.bridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue(Syntax.XWIKI_2_0.toIdString()));
            oneOf(mockSetup.documentReferenceResolver).resolve("wiki:Space.Page");
                will(returnValue(new DocumentReference("wiki", "Space", "Page")));
        }});
        
        this.includeMacro.setDocumentAccessBridge(mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.CURRENT);

        List<Block> blocks = this.includeMacro.execute(parameters, null, new MacroTransformationContext());

        assertBlocks(expected, blocks, this.rendererFactory);
    }

    @Test
    public void testIncludeMacroWithNoDocumentSpecified() throws Exception
    {
        IncludeMacroParameters parameters = new IncludeMacroParameters();

        try {
            this.includeMacro.execute(parameters, null, new MacroTransformationContext());
            Assert.fail("An exception should have been thrown");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'document' parameter pointing to the document to include.",
                expected.getMessage());
        }
    }

    /**
     * Verify that relative links are made absolute in the XDOM returned by the Include macro.
     */
    @Test
    public void testIncludeMacroWhenIncludingDocumentWithRelativeLinks() throws Exception
    {
        String expected = "beginDocument\n"
            + "beginParagraph\n"
            + "beginLink [Reference = [wiki:space.page]] [false]\n"
            + "endLink [Reference = [wiki:space.page]] [false]\n"
            + "endParagraph\n"
            + "endDocument";

        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(DocumentReference.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocumentContent("includedWiki:includedSpace.includedPage");
                will(returnValue("[[page]]"));
            oneOf(mockSetup.bridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue(Syntax.XWIKI_2_0.toIdString()));
            oneOf(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(any(DocumentReference.class)));
            oneOf(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
            oneOf(mockSetup.documentReferenceResolver).resolve("page");
                will(returnValue(new DocumentReference("wiki", "space", "page")));
            oneOf(mockSetup.documentReferenceResolver).resolve("includedWiki:includedSpace.includedPage");
                will(returnValue(new DocumentReference("includedWiki", "includedSpace", "includedPage")));
        }});
        
        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("includedWiki:includedSpace.includedPage");
        parameters.setContext(Context.NEW);

        List<Block> blocks = this.includeMacro.execute(parameters, null, new MacroTransformationContext());

        assertBlocks(expected, blocks, this.rendererFactory);
    }
    
    @Test
    public void testIncludeMacroWithRecursiveInclude() throws Exception
    {
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.documentReferenceResolver).resolve("wiki:Space.Page");
                will(returnValue(new DocumentReference("wiki", "Space", "Page")));
            oneOf(mockSetup.documentReferenceResolver).resolve("Space.Page");
                will(returnValue(new DocumentReference("wiki", "Space", "Page")));
        }});
        
        this.includeMacro.setDocumentAccessBridge(mockSetup.bridge);

        MacroBlock includeMacro = new  MacroBlock("include", Collections.singletonMap("document", "wiki:Space.Page"), false);
        new MacroMarkerBlock("include", Collections.singletonMap("document", "Space.Page"), Collections.<Block>singletonList(includeMacro), false);
        MacroTransformationContext context = new MacroTransformationContext();
        context.setCurrentMacroBlock(includeMacro);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.CURRENT);
        
        List<Block> blocks;
        try {
            blocks = this.includeMacro.execute(parameters, null, context);
            
            Assert.fail("The include macro did not checked the recusive inclusion");
        } catch (MacroExecutionException expected) {
            if (!expected.getMessage().startsWith("Found recursive inclusion")) {
                throw expected;
            }
        }
    }
}

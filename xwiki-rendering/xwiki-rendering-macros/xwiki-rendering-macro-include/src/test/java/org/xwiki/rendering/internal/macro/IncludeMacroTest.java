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
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.jmock.Expectations;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.internal.transformation.MacroTransformation;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.include.IncludeMacroParameters;
import org.xwiki.rendering.macro.include.IncludeMacroParameters.Context;
import org.xwiki.rendering.macro.script.MockSetup;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.velocity.VelocityManager;

/**
 * Unit tests for {@link IncludeMacro}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class IncludeMacroTest extends AbstractComponentTestCase
{
    private MockSetup mockSetup;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        
        this.mockSetup = new MockSetup(getComponentManager());
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

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(Macro.class, "include");
        
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(String.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocumentContent(with(any(String.class))); 
                will(returnValue("{{velocity}}$myvar{{/velocity}}"));
            oneOf(mockSetup.bridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue(new Syntax(SyntaxType.XWIKI, "2.0").toIdString()));
            allowing(mockSetup.bridge).pushDocumentInContext(with(any(Map.class)), with(any(String.class)));
            allowing(mockSetup.bridge).popDocumentFromContext(with(any(Map.class)));
        }});
        macro.setDocumentAccessBridge(this.mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.NEW);

        // Create a Macro transformation context with the Macro transformation object defined so that the include
        // macro can transform included page which is using a new context.
        MacroTransformation macroTransformation =
            (MacroTransformation) getComponentManager().lookup(Transformation.class, "macro");
        MacroTransformationContext context = new MacroTransformationContext();
        context.setMacroTransformation(macroTransformation);

        List<Block> blocks = macro.execute(parameters, null, context);

        assertBlocks(expected, blocks);
    }

    @Test
    public void testIncludeMacroWithCurrentContext() throws Exception
    {
        String expected = "beginDocument\n"
            + "onMacroStandalone [someMacro] []\n"
            + "endDocument";

        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(Macro.class, "include");
        
        this.mockSetup.mockery.checking(new Expectations() {{
            oneOf(mockSetup.bridge).isDocumentViewable(with(any(String.class))); will(returnValue(true));
            oneOf(mockSetup.bridge).getDocumentContent(with(any(String.class))); 
                will(returnValue("{{someMacro/}}"));
            oneOf(mockSetup.bridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue(new Syntax(SyntaxType.XWIKI, "2.0").toIdString()));
        }});
        
        macro.setDocumentAccessBridge(mockSetup.bridge);

        IncludeMacroParameters parameters = new IncludeMacroParameters();
        parameters.setDocument("wiki:Space.Page");
        parameters.setContext(Context.CURRENT);

        List<Block> blocks = macro.execute(parameters, null, new MacroTransformationContext());

        assertBlocks(expected, blocks);
    }

    @Test
    public void testIncludeMacroWithNoDocumentSpecified() throws Exception
    {
        IncludeMacro macro = (IncludeMacro) getComponentManager().lookup(Macro.class, "include");
        IncludeMacroParameters parameters = new IncludeMacroParameters();

        try {
            macro.execute(parameters, null, new MacroTransformationContext());
            Assert.fail("An exception should have been thrown");
        } catch (MacroExecutionException expected) {
            Assert.assertEquals("You must specify a 'document' parameter pointing to the document to include.",
                expected.getMessage());
        }
    }
    
    private void assertBlocks(String expected, List<Block> blocks) throws Exception
    {
        // Assert the result by parsing it through the EventsRenderer to generate easily
        // assertable events.
        XDOM dom = new XDOM(blocks);
        WikiPrinter printer = new DefaultWikiPrinter();

        PrintRendererFactory factory = getComponentManager().lookup(PrintRendererFactory.class, "event/1.0");
        PrintRenderer eventRenderer = factory.createRenderer(printer);

        dom.traverse(eventRenderer);
        Assert.assertEquals(expected, printer.toString());
    }
}

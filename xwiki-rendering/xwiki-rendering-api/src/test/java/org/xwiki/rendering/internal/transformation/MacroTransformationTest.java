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
package org.xwiki.rendering.internal.transformation;

import java.util.Arrays;
import java.util.Collections;

import org.xwiki.rendering.scaffolding.AbstractRenderingTestCase;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

public class MacroTransformationTest extends AbstractRenderingTestCase
{
    private MacroTransformation transformation;
    
    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.transformation = (MacroTransformation) getComponentManager().lookup(Transformation.ROLE, "macro");
    }

    /**
     * Test that a simple macro is correctly evaluated.
     */
    public void testSimpleMacroTransform() throws Exception
    {
        String expected = "beginDocument\n"
        	+ "beginMacroMarker [testsimplemacro] [] [null]\n"
            + "beginParagraph\n"
            + "onWord [simplemacro0]\n"
            + "endParagraph\n"
            + "endMacroMarker [testsimplemacro] [] [null]\n"
            + "endDocument";

        XDOM dom = new XDOM(Arrays.asList((Block) new MacroBlock("testsimplemacro",
            Collections.<String, String>emptyMap(), false)));
        
        this.transformation.transform(dom, new Syntax(SyntaxType.XWIKI, "2.0"));

        WikiPrinter printer = new DefaultWikiPrinter();
        dom.traverse(new EventsRenderer(printer));
        assertEquals(expected, printer.toString());
    }

    /**
     * Test that a macro can generate another macro.
     */
    public void testNestedMacroTransform() throws Exception
    {
        String expected = "beginDocument\n"
        	+ "beginMacroMarker [testnestedmacro] [] [null]\n"
            + "beginParagraph\n"
            + "onWord [simplemacro0]\n"
            + "endParagraph\n"
            + "endMacroMarker [testnestedmacro] [] [null]\n"
            + "endDocument";

        XDOM dom = new XDOM(Arrays.asList((Block) new MacroBlock("testnestedmacro",
            Collections.<String, String>emptyMap(), false)));
    
        this.transformation.transform(dom, new Syntax(SyntaxType.XWIKI, "2.0"));

        WikiPrinter printer = new DefaultWikiPrinter();
        dom.traverse(new EventsRenderer(printer));
        assertEquals(expected, printer.toString());
    }
    
    /**
     * Test that we have a safeguard against infinite recursive macros.
     */
    public void testInfiniteRecursionMacroTransform() throws Exception
    {
        String expected = "beginDocument\n"
        	+ "beginMacroMarker [testrecursivemacro] [] [null]\n"
            + "onMacroStandalone [testrecursivemacro] [] [null]\n"
            + "endMacroMarker [testrecursivemacro] [] [null]\n"
            + "endDocument";
        
        XDOM dom = new XDOM(Arrays.asList((Block) new MacroBlock("testrecursivemacro",
            Collections.<String, String>emptyMap(), false)));
    
        this.transformation.transform(dom, new Syntax(SyntaxType.XWIKI, "2.0"));

        WikiPrinter printer = new DefaultWikiPrinter();
        dom.traverse(new EventsRenderer(printer));
        assertEquals(expected, printer.toString());
    }
    
    /**
     * Test that macro priorities are working.
     */
    public void testPrioritiesMacroTransform() throws Exception
    {
        String expected = "beginDocument\n"
        	+ "beginMacroMarker [testsimplemacro] [] [null]\n"
            + "beginParagraph\n"
            + "onWord [simplemacro1]\n"
            + "endParagraph\n"
            + "endMacroMarker [testsimplemacro] [] [null]\n"
            + "beginMacroMarker [testprioritymacro] [] [null]\n"
            + "beginParagraph\n"
            + "onWord [word]\n"
            + "endParagraph\n"
            + "endMacroMarker [testprioritymacro] [] [null]\n"
            + "endDocument";

        XDOM dom = new XDOM(Arrays.asList(
            (Block) new MacroBlock("testsimplemacro", Collections.<String, String>emptyMap(), false),
            (Block) new MacroBlock("testprioritymacro", Collections.<String, String>emptyMap(), false)));

        this.transformation.transform(dom, new Syntax(SyntaxType.XWIKI, "2.0"));

        WikiPrinter printer = new DefaultWikiPrinter();
        dom.traverse(new EventsRenderer(printer));
        assertEquals(expected, printer.toString());
    }
    
}

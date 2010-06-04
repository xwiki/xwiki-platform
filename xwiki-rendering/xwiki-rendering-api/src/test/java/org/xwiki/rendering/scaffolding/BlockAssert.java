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
package org.xwiki.rendering.scaffolding;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import java.util.List;

/**
 * Class to be imported in unit tests as a static import and which contains helper methods to assert Rendering Blocks.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class BlockAssert
{
    /**
     * Protect constructor since it is a static only class
     */
    protected BlockAssert()
    {
        // Nothing to do
    }

    public static void assertBlocks(String expected, List<Block> blocks, PrintRendererFactory factory) throws Exception
    {
        // Assert the result by parsing it through the EventsRenderer to generate easily
        // assertable events.
        XDOM dom = new XDOM(blocks);
        WikiPrinter printer = new DefaultWikiPrinter();

        PrintRenderer eventRenderer = factory.createRenderer(printer);

        dom.traverse(eventRenderer);
        junit.framework.Assert.assertEquals(expected, printer.toString());
    }
}

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

import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.EventsRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.AbstractXWikiComponentTestCase;

public abstract class AbstractRenderingTestCase extends AbstractXWikiComponentTestCase
{
    public AbstractRenderingTestCase()
    {
        super();
    }

    public AbstractRenderingTestCase(String testName)
    {
        super(testName);
    }

    protected void assertBlocks(String expected, List<Block> blocks)
    {
        // Assert the result by parsing it through the EventsRenderer to generate easily
        // assertable events.
        XDOM dom = new XDOM(blocks);
        WikiPrinter printer = new DefaultWikiPrinter();
        dom.traverse(new EventsRenderer(printer));
        assertEquals(expected, printer.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        getComponentManager().registerComponent(MockConfigurationSourceCollection.getComponentDescriptor());
        getComponentManager().registerComponent(MockDocumentNameSerializer.getComponentDescriptor());
    }
}

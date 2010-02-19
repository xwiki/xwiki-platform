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
package org.xwiki.rendering.internal.renderer;

import java.util.Collection;
import java.util.Collections;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.PrintRenderer;

/**
 * Common code for BlockRender implementation that uses Print Renderer Factory.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractBlockRenderer implements BlockRenderer
{
    /**
     * @return provide the factory to use to create a new {@link PrintRenderer}.
     */
    protected abstract PrintRendererFactory getPrintRendererFactory();

    /**
     * {@inheritDoc}
     * 
     * @see BlockRenderer#render(org.xwiki.rendering.block.Block, org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    public void render(Block block, WikiPrinter printer)
    {
        render(Collections.singletonList(block), printer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see BlockRenderer#render(java.util.Collection, org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    public void render(Collection<Block> blocks, WikiPrinter printer)
    {
        PrintRenderer renderer = getPrintRendererFactory().createRenderer(printer);
        for (Block block : blocks) {
            block.traverse(renderer);
        }
    }
}

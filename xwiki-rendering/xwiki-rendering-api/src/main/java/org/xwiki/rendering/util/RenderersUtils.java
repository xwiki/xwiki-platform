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
package org.xwiki.rendering.util;

import java.util.Collection;
import java.util.Collections;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.PlainTextRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Methods for helping in rendering.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class RenderersUtils
{
    /**
     * Use the {@link PlainTextRenderer} to print provided block.
     * 
     * @param block the block to render
     * @return the rendered plain text String
     * @see PlainTextRenderer
     */
    public String renderPlainText(Block block)
    {
        return renderPlainText(Collections.singleton(block));
    }

    /**
     * Use the {@link PlainTextRenderer} to print provided blocks.
     * 
     * @param blocks the blocks to render
     * @return the rendered plain text String
     * @see PlainTextRenderer
     */
    public String renderPlainText(Collection<Block> blocks)
    {
        WikiPrinter wikiPrinter = new DefaultWikiPrinter();
        PlainTextRenderer plainTextRenderer = new PlainTextRenderer(wikiPrinter, null);

        for (Block block : blocks) {
            block.traverse(plainTextRenderer);
        }

        return wikiPrinter.toString();
    }
}

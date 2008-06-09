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
package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

import java.util.Arrays;
import java.util.List;

/**
 * A type of {@link Block} that has children. For example the Paragraph Block, the Bold Block, the List Block, etc.
 *
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractFatherBlock extends AbstractBlock
{
    /**
     * Constructs a block with children blocks.
     *
     * @param childrenBlocks the list of children blocks of the block to construct
     */
    public AbstractFatherBlock(List<Block> childrenBlocks)
    {
        addChildren(childrenBlocks);
    }

    /**
     * Helper constructor to construct a block with a single child block.
     *
     * @param childBlock the single child block to add 
     */
    public AbstractFatherBlock(Block childBlock)
    {
        this(Arrays.asList(childBlock));
    }

    /**
     * Send {@link Listener} events corresponding to the start of the father block. For example for a Bold block, this
     * allows an XHTML Listener (aka a Renderer) to output <code>&lt;b&gt;</code>.
     *
     * @param listener the listener that will receive the events sent by the father block before the children blocks
     *        have emitted their own events.
     */
    public abstract void before(Listener listener);

    /**
     * Send {@link Listener} events corresponding to the end of the father block. For example for a Bold block, this
     * allows an XHTML Listener (aka a Renderer) to output <code>&lt;/b&gt;</code>.
     *
     * @param listener the listener that will receive the events sent by the father block after the children blocks
     *        have emitted their own events.
     */
    public abstract void after(Listener listener);

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        before(listener);

        for (Block block: getChildren()) {
            block.traverse(listener);
        }

        after(listener);
    }
}

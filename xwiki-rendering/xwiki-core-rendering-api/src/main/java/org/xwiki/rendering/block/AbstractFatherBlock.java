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

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Default implementation for {@link FatherBlock}.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public abstract class AbstractFatherBlock extends AbstractBlock implements FatherBlock
{
    /**
     * Constructs a block with children blocks.
     * 
     * @param childrenBlocks the list of children blocks of the block to construct
     */
    public AbstractFatherBlock(List<Block> childrenBlocks)
    {
        this(childrenBlocks, Collections.<String, String> emptyMap());
    }

    /**
     * Construct a block with children blocks and parameters.
     * 
     * @param childrenBlocks the list of children blocks of the block to construct
     * @param parameters the parameters to set
     */
    public AbstractFatherBlock(List<Block> childrenBlocks, Map<String, String> parameters)
    {
        super(parameters);
        addChildren(childrenBlocks);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        before(listener);

        for (Block block : getChildren()) {
            block.traverse(listener);
        }

        after(listener);
    }
}

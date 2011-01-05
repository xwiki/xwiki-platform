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

import java.util.List;
import java.util.Map;

/**
 * A special block that can be used to indifferently represent either a single block or a list of blocks. This makes
 * it easy for all APIs to take a {@link Block} as parameter instead of a {@code List<Block>} which is a bit
 * cumbersome since when you have a single block to need to write things like {@code Arrays.asList(myBlock)}.
 *
 * Note that this block doesn't represent any content, it's a "technical" block only.
 *
 * @version $Id$
 * @since 3.0M1
 */
public class CollectionBlock extends AbstractBlock
{
    /**
     * Constructs a block with children blocks.
     *
     * @param childrenBlocks the list of children blocks of the block to construct
     */
    public CollectionBlock(List<Block> childrenBlocks)
    {
        super(childrenBlocks);
    }

    /**
     * Construct a block with children blocks and parameters.
     *
     * @param childrenBlocks the list of children blocks of the block to construct
     * @param parameters the parameters to set
     */
    public CollectionBlock(List<Block> childrenBlocks, Map<String, String> parameters)
    {
        super(childrenBlocks, parameters);
    }
}

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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;

/**
 * Represents a grouping of blocks.
 * 
 * @version $Id: $
 * @since 1.8.3
 */
public class GroupBlock extends AbstractFatherBlock
{
    /**
     * Create an empty group block with no children. This is useful when the user wants to call
     * {@link #addChild(Block)} manually for adding children one by one after the block is
     * constructed.
     */
    public GroupBlock()
    {
        this(Collections.<Block> emptyList());
    }

    /**
     * Create an empty group block with no children. This is useful when the user wants to call
     * {@link #addChild(Block)} manually for adding children one by one after the block is
     * constructed.
     * 
     * @param parameters the parameters of the group
     */
    public GroupBlock(Map<String, String> parameters)
    {
        this(Collections.<Block> emptyList(), parameters);
    }

    /**
     * @param blocks the children blocks of the group
     */
    public GroupBlock(List<Block> blocks)
    {
        super(blocks);
    }

    /**
     * @param blocks the children blocks of the group
     * @param parameters the parameters of the group
     */
    public GroupBlock(List<Block> blocks, Map<String, String> parameters)
    {
        super(blocks, parameters);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginGroup(getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endGroup(getParameters());
    }
}

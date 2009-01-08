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

import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;

/**
 * Represents a numbered List.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class NumberedListBlock extends AbstractFatherBlock implements ListBLock
{
    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#AbstractFatherBlock(java.util.List)
     */
    public NumberedListBlock(List<Block> childrenBlocks)
    {
        super(childrenBlocks);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#AbstractFatherBlock(java.util.List, java.util.Map)
     */
    public NumberedListBlock(List<Block> childrenBlocks, Map<String, String> parameters)
    {
        super(childrenBlocks, parameters);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginList(ListType.NUMBERED, getParameters());
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#after(org.xwiki.rendering.listener.Listener) 
     */
    public void after(Listener listener)
    {
        listener.endList(ListType.NUMBERED, getParameters());
    }
}

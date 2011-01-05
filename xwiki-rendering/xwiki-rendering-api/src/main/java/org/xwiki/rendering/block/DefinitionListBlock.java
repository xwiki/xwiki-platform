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

/**
 * Represents a definition list. For example in HTML this is the equivalent of &lt;dl&gt;.
 *
 * @version $Id$
 * @since 1.6M2
 */
public class DefinitionListBlock extends AbstractBlock implements ListBLock
{
    /**
     * Construct a Definition List block with no parameters.
     *
     * @param childrenBlocks the blocks making the Definition list
     */
    public DefinitionListBlock(List<Block> childrenBlocks)
    {
        super(childrenBlocks);
    }

    /**
     * Construct a Definition List Block with parameters.
     *
     * @param childrenBlocks the blocks making the Definition list
     * @param parameters see {@link org.xwiki.rendering.block.AbstractBlock#getParameter(String)} for more details on
     *        parameters
     */
    public DefinitionListBlock(List<Block> childrenBlocks, Map<String, String> parameters)
    {
        super(childrenBlocks, parameters);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginDefinitionList(getParameters());
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endDefinitionList(getParameters());
    }
}

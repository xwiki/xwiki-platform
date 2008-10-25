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

/**
 * Represents an empty line between 2 standalone Blocks. A standalone block is block that is not included in
 * another block. Standalone blocks are Paragraph, Standalone Macro, Lists, Table, etc.
 *
 * @version $Id$
 * @since 1.6M2
 */
public class EmptyLinesBlock  extends AbstractBlock
{
    /**
     * Number of empty lines between 2 standalone Blocks.
     */
    private int count;

    /**
     * @param count the number of empty lines between 2 standalone Blocks
     */
    public EmptyLinesBlock(int count)
    {
        this.count = count;
    }

    /**
     * @return the number of empty lines between 2 standalone Blocks
     */
    public int getEmptyLinesCount()
    {
        return this.count;
    }

    /**
     * {@inheritDoc}
     * @see AbstractBlock#traverse(Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onEmptyLines(getEmptyLinesCount());
    }
}

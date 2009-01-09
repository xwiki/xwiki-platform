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

import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.Listener;

import java.util.List;

/**
 * Represents a text formatting block (bold, italic, etc).
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class FormatBlock extends AbstractFatherBlock
{
    /**
     * The formatting to apply to the children blocks.
     */
    private Format format;

    /**
     * @param childrenBlocks the nested children blocks
     * @param format the formatting to apply to the children blocks
     */
    public FormatBlock(List<Block> childrenBlocks, Format format)
    {
        super(childrenBlocks);
        this.format = format;
    }

    /**
     * @return the formatting to apply to the children blocks
     */
    public Format getFormat()
    {
        return this.format;
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginFormat(getFormat(), getParameters());
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractFatherBlock#after(org.xwiki.rendering.listener.Listener)  
     */
    public void after(Listener listener)
    {
        listener.endFormat(getFormat(), getParameters());
    }
}

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
 * Represents an explicit line break specified in the wiki syntax. For example for XWiki this would be "\\".
 * Note that this is different from a new line which is triggered when the new line character is found ("\n").
 *
 * @version $Id$
 * @since 1.5M2
 * @see org.xwiki.rendering.block.NewLineBlock
 */
public final class LineBreakBlock extends AbstractBlock
{
    /**
     * The single instance for a line break block. There's no need for more than one instance since there's
     * no state in the line break block.
     */
    public static final LineBreakBlock LINE_BREAK_BLOCK = new LineBreakBlock();

    /**
     * Private constructor to prevent instantiation. Instead use {@link #LINE_BREAK_BLOCK}.
     */
    private LineBreakBlock()
    {
        // Voluntarily empty
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.block.AbstractBlock#traverse(org.xwiki.rendering.listener.Listener)
     */
    public void traverse(Listener listener)
    {
        listener.onLineBreak();
    }
}

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
 * Represents an implicit new line triggered when the new line character is found ("\n"). Note
 * that this is different from a line break which is explicitely specified in wiki syntax.
 *
 * @version $Id$
 * @since 1.5M2
 * @see LineBreakBlock
 */
public final class NewLineBlock extends AbstractBlock
{
    /**
     * The single instance for a new line block. There's no need for more than one instance since there's
     * no state in the new line block.
     */
    public static final NewLineBlock NEW_LINE_BLOCK = new NewLineBlock();

    /**
     * Private constructor to prevent instantiation. Instead use {@link #NEW_LINE_BLOCK}.
     */
    private NewLineBlock()
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

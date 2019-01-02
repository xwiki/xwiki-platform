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
package org.xwiki.rendering.async.internal.block;

import org.xwiki.rendering.async.internal.AsyncRendererResult;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;

/**
 * A {@link AsyncRendererResult} which also remember the {@link XDOM} which was rendered into the result {@link String}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class BlockAsyncRendererResult extends AsyncRendererResult
{
    private Block block;

    /**
     * @param result the resulting {@link String} (generally html)
     * @param block the block which was rendered into the passed {@link String}
     */
    public BlockAsyncRendererResult(String result, Block block)
    {
        super(result);

        this.block = block;
    }

    /**
     * @return the block the block which was rendered into the passed {@link String}
     */
    public Block getBlock()
    {
        return block;
    }
}

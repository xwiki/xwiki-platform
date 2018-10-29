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

import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Configuration to pass to {@link BlockAsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class BlockAsyncRendererConfiguration
{
    private List<String> id;

    private Block block;

    private Syntax defaultSyntax;

    private Syntax targetSyntax;

    /**
     * @param id the id used as prefix (concatenated with contextual information) for the actual job identifier
     * @param block the block to transform
     */
    public BlockAsyncRendererConfiguration(List<String> id, Block block)
    {
        this.id = id;
        this.block = block;
    }

    /**
     * @return the id
     */
    public List<String> getId()
    {
        return this.id;
    }

    /**
     * @param id the id to set
     */
    public void setId(List<String> id)
    {
        this.id = id;
    }

    /**
     * @return the block
     */
    public Block getBlock()
    {
        return this.block;
    }

    /**
     * @return the defaultSyntax
     */
    public Syntax getDefaultSyntax()
    {
        return this.defaultSyntax;
    }

    /**
     * @return the targetSyntax
     */
    public Syntax getTargetSyntax()
    {
        return this.targetSyntax;
    }
}

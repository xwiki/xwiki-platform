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
package org.xwiki.rendering.renderer;

import java.util.Collection;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.block.Block;

/**
 * Renders a {@link Block} in some target syntax.
 *
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface BlockRenderer
{
    /**
     * @param block the block to render in the target syntax
     * @param printer the object where to output the result of the rendering
     */
    void render(Block block, WikiPrinter printer);

    /**
     * @param blocks the list of blocks to render in the target syntax
     * @param printer the object where to output the result of the rendering
     * @todo remove this API once we introduce the notion of BlockCollection
     */
    void render(Collection<Block> blocks, WikiPrinter printer);
}

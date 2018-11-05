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

import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;

/**
 * Give a chance to prepare and cleanup before and after the actual renderer execution.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@FunctionalInterface
public interface BlockAsyncRendererDecorator
{
    /**
     * @param renderer the renderer to execute
     * @return the result of the renderer execution
     * @throws RenderingException when failing to execute the renderer
     */
    BlockAsyncRendererResult render(AsyncRenderer renderer) throws RenderingException;
}

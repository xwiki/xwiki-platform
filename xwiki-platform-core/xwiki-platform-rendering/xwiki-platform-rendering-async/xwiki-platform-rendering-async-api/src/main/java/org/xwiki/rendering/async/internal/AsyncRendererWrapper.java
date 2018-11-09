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
package org.xwiki.rendering.async.internal;

import java.util.List;

import org.xwiki.rendering.RenderingException;

/**
 * Call another {@link AsyncRenderer}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class AsyncRendererWrapper implements AsyncRenderer
{
    protected AsyncRenderer renderer;

    /**
     * @param renderer the renderer
     */
    public AsyncRendererWrapper(AsyncRenderer renderer)
    {
        this.renderer = renderer;
    }

    @Override
    public List<String> getId()
    {
        return this.renderer.getId();
    }

    @Override
    public AsyncRendererResult render(boolean async, boolean cached) throws RenderingException
    {
        return this.renderer.render(async, cached);
    }

    @Override
    public boolean isAsyncAllowed()
    {
        return this.renderer.isAsyncAllowed();
    }

    @Override
    public boolean isCacheAllowed()
    {
        return this.renderer.isCacheAllowed();
    }

}

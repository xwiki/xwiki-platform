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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.rendering.async.internal.AsyncRendererWrapper;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link BlockAsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Singleton
public class DefaultBlockAsyncRendererExecutor implements BlockAsyncRendererExecutor
{
    private static class DecoratorWrapper extends AsyncRendererWrapper implements BlockAsyncRenderer
    {
        private BlockAsyncRendererDecorator decorator;

        /**
         * @param decorator the decorator
         * @param renderer the renderer
         */
        DecoratorWrapper(BlockAsyncRendererDecorator decorator, BlockAsyncRenderer renderer)
        {
            super(renderer);

            this.decorator = decorator;
        }

        @Override
        public BlockAsyncRendererResult render(boolean async, boolean cached) throws RenderingException
        {
            return this.decorator.render((BlockAsyncRenderer) this.renderer, async, cached);
        }

        @Override
        public boolean isInline()
        {
            return ((BlockAsyncRenderer) this.renderer).isInline();
        }

    }

    @Inject
    private AsyncRendererExecutor executor;

    @Inject
    private Provider<DefaultBlockAsyncRenderer> rendererProvider;

    @Override
    public Block execute(BlockAsyncRendererConfiguration configuration, Set<String> contextEntries)
        throws JobException, RenderingException
    {
        return execute(configuration, contextEntries, null, null);
    }

    @Override
    public Block execute(BlockAsyncRendererConfiguration configuration, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException, RenderingException
    {
        // Create renderer (it might not be used but it should not be very expensive and it makes the code much simpler)
        DefaultBlockAsyncRenderer renderer = this.rendererProvider.get();
        renderer.initialize(configuration);

        // Start renderer execution if there is none already running/available
        return execute(configuration.getDecorator() != null
            ? new DecoratorWrapper(configuration.getDecorator(), renderer) : renderer, contextEntries, right,
            rightEntity);
    }

    @Override
    public Block execute(BlockAsyncRenderer renderer, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException, RenderingException
    {
        // Start renderer execution if there is none already running/available
        AsyncRendererJobStatus status = this.executor.render(renderer, contextEntries, right, rightEntity);

        // Get result
        BlockAsyncRendererResult result = (BlockAsyncRendererResult) status.getResult();

        if (result != null) {
            return result.getBlock();
        }

        // Return a placeholder waiting for the result
        Block placeholder;
        if (renderer.isInline()) {
            placeholder = new FormatBlock();
        } else {
            placeholder = new GroupBlock();
        }
        placeholder.setParameter("class", "xwiki-async");
        // Provide it directly as it's going to be used in the client side (the URL fragment to use in the ajax request)
        placeholder.setParameter("data-xwiki-async-id",
            status.getRequest().getId().stream().map(this::encodeURL).collect(Collectors.joining("/")));

        return placeholder;
    }

    private String encodeURL(String element)
    {
        try {
            return URLEncoder.encode(element, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // If Java does not support UTF8 we probably won't reach this point anyway
            throw new RuntimeException("UTF8 encoding is not supported", e);
        }
    }
}

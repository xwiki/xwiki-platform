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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererExecutorResponse;
import org.xwiki.rendering.async.internal.AsyncRendererWrapper;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xml.XMLUtils;

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
    private static final String CLASS_ATTRIBUTE = "class";

    private static final String PLACEHOLDER_CLASS = "xwiki-async";

    private static final String ASYNC_ID_ATTRIBUTE = "data-xwiki-async-id";

    private static final String ASYNC_CLIENT_ID_ATTRIBUTE = "data-xwiki-async-client-id";

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

        @Override
        public Syntax getTargetSyntax()
        {
            return ((BlockAsyncRenderer) this.renderer).getTargetSyntax();
        }
    }

    @Inject
    private AsyncRendererExecutor executor;

    @Inject
    private Provider<DefaultBlockAsyncRenderer> rendererProvider;

    @Inject
    private JobProgressManager progress;

    @Override
    public Block execute(BlockAsyncRendererConfiguration configuration) throws JobException, RenderingException
    {
        this.progress.pushLevelProgress(3, this);

        try {
            this.progress.startStep(this, "async.block.progress.createRenderer", "Create asynchronous block renderer");

            // Create renderer (it might not be used but it should not be very expensive and it makes the code much
            // simpler)
            DefaultBlockAsyncRenderer renderer = this.rendererProvider.get();

            this.progress.startStep(this, "async.block.progress.initRenderer",
                "Initialize asynchronous block renderer");

            renderer.initialize(configuration);

            this.progress.startStep(this, "async.block.progress.executeRenderer",
                "Execute asynchronous block renderer");

            // Start renderer execution if there is none already running/available
            return execute(configuration.getDecorator() != null
                ? new DecoratorWrapper(configuration.getDecorator(), renderer) : renderer, configuration);
        } finally {
            this.progress.popLevelProgress(this);
        }
    }

    @Override
    public Block execute(BlockAsyncRenderer renderer, AsyncRendererConfiguration configuration)
        throws JobException, RenderingException
    {
        // Start renderer execution if there is none already running/available
        AsyncRendererExecutorResponse response = this.executor.render(renderer, configuration);

        // Get result
        BlockAsyncRendererResult result = (BlockAsyncRendererResult) response.getStatus().getResult();

        if (result != null && !configuration.isPlaceHolderForced()) {
            return result.getBlock();
        }

        // Return a placeholder waiting for the result
        Block placeholder;
        if (renderer.isInline()) {
            placeholder = new FormatBlock();
        } else {
            placeholder = new GroupBlock();
        }
        placeholder.setParameter(CLASS_ATTRIBUTE, PLACEHOLDER_CLASS);
        // Provide it directly as it's going to be used in the client side (the URL fragment to use in the ajax request)
        placeholder.setParameter(ASYNC_ID_ATTRIBUTE, response.getJobIdHTTPPath());
        placeholder.setParameter(ASYNC_CLIENT_ID_ATTRIBUTE, response.getAsyncClientId());

        return placeholder;
    }

    @Override
    public String render(BlockAsyncRenderer renderer, AsyncRendererConfiguration configuration)
        throws JobException, RenderingException
    {
        // Start renderer execution if there is none already running/available
        AsyncRendererExecutorResponse response = this.executor.render(renderer, configuration);

        // Get result
        BlockAsyncRendererResult result = (BlockAsyncRendererResult) response.getStatus().getResult();

        if (result != null && !configuration.isPlaceHolderForced()) {
            return result.getResult();
        }

        // Return a placeholder waiting for the result
        String elementName = renderer.isInline() ? "span" : "div";
        StringBuilder str = new StringBuilder();

        str.append('<').append(elementName);
        appendAttribute(CLASS_ATTRIBUTE, PLACEHOLDER_CLASS, str);
        // Provide it directly as it's going to be used in the client side (the URL fragment to use in the ajax request)
        appendAttribute(ASYNC_ID_ATTRIBUTE, response.getJobIdHTTPPath(), str);
        appendAttribute(ASYNC_CLIENT_ID_ATTRIBUTE, response.getAsyncClientId(), str);
        // Don't use a self-closing tag as HTML doesn't support it for div and span elements
        str.append("></").append(elementName).append('>');

        return str.toString();
    }

    private void appendAttribute(String name, String value, StringBuilder str)
    {
        if (value != null) {
            str.append(' ').append(name).append("=\"").append(XMLUtils.escapeAttributeValue(value)).append('"');
        }
    }
}

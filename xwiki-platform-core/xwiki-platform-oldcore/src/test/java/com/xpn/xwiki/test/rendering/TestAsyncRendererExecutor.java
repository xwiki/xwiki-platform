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
package com.xpn.xwiki.test.rendering;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererExecutorResponse;
import org.xwiki.rendering.async.internal.AsyncRendererJobRequest;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.rendering.async.internal.AsyncRendererResult;
import org.xwiki.security.authorization.AuthorExecutor;

/**
 * Test implementation of {@link AsyncRendererExecutor}. Automatically overwritten if a "real" one exist.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class TestAsyncRendererExecutor implements AsyncRendererExecutor
{
    @Inject
    protected AuthorExecutor authorExecutor;

    @Override
    public AsyncRendererJobStatus getAsyncStatus(List<String> id, String clientId)
    {
        return null;
    }

    @Override
    public AsyncRendererJobStatus getAsyncStatus(List<String> id, String clientId, long time, TimeUnit unit)
        throws InterruptedException
    {
        return null;
    }

    @Override
    public AsyncRendererExecutorResponse render(AsyncRenderer renderer, AsyncRendererConfiguration configuration)
        throws JobException, RenderingException
    {
        AsyncRendererJobRequest request = new AsyncRendererJobRequest();
        request.setRenderer(renderer);

        AsyncRendererResult result = syncRender(renderer, false, configuration);

        // Create a pseudo job status
        AsyncRendererJobStatus status = new AsyncRendererJobStatus(request, result);

        return new AsyncRendererExecutorResponse(status);
    }

    private AsyncRendererResult syncRender(AsyncRenderer renderer, boolean cached,
        AsyncRendererConfiguration configuration) throws RenderingException
    {
        if (configuration.isSecureReferenceSet()) {
            try {
                return this.authorExecutor.call(() -> renderer.render(false, cached),
                    configuration.getSecureAuthorReference(), configuration.getSecureDocumentReference());
            } catch (Exception e) {
                throw new RenderingException("Failed to execute renderer", e);
            }
        } else {
            return renderer.render(false, cached);
        }
    }
}

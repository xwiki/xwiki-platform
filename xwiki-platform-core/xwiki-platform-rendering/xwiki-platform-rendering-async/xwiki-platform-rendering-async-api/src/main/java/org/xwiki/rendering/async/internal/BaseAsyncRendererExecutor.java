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
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobException;
import org.xwiki.rendering.RenderingException;

/**
 * Minimum implementation of {@link AsyncRendererExecutor} mostly used for tests.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class BaseAsyncRendererExecutor implements AsyncRendererExecutor
{
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

        AsyncRendererResult result = renderer.render(false, false);

        AsyncRendererJobStatus status = new AsyncRendererJobStatus(request, result);

        return new AsyncRendererExecutorResponse(status);
    }
}

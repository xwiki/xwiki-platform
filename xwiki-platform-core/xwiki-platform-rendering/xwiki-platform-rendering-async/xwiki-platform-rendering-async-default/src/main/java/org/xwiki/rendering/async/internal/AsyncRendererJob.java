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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Request;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.ContextUse;

/**
 * Default implementation of {@link AsyncRendererJob}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Named(AsyncRendererJobStatus.JOBTYPE)
public class AsyncRendererJob extends AbstractJob<AsyncRendererJobRequest, AsyncRendererJobStatus>
{
    @Inject
    private AsyncRendererCache cache;

    @Inject
    private AsyncContext asyncContext;

    @Override
    protected AsyncRendererJobRequest castRequest(Request request)
    {
        AsyncRendererJobRequest indexerRequest;
        if (request instanceof AsyncRendererJobRequest) {
            indexerRequest = (AsyncRendererJobRequest) request;
        } else {
            indexerRequest = new AsyncRendererJobRequest(request);
        }

        return indexerRequest;
    }

    @Override
    protected AsyncRendererJobStatus createNewStatus(AsyncRendererJobRequest request)
    {
        return new AsyncRendererJobStatus(request, this.observationManager, this.loggerManager);
    }

    @Override
    public String getType()
    {
        return AsyncRendererJobStatus.JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Enable async execution since we are already in an asynchronous context
        this.asyncContext.setEnabled(true);
        // Prepare to catch stuff to invalidate the cache
        ((DefaultAsyncContext) this.asyncContext).pushContextUse();

        AsyncRenderer renderer = getRequest().getRenderer();
        AsyncRendererResult result = renderer.render();

        getStatus().setResult(result);
        // Get suff to invalidate the cache
        ContextUse contextUse = ((DefaultAsyncContext) this.asyncContext).popContextUse();
        getStatus().setReference(contextUse.getReferences());
        getStatus().setRoles(contextUse.getRoles());

        this.cache.put(getStatus());
    }
}

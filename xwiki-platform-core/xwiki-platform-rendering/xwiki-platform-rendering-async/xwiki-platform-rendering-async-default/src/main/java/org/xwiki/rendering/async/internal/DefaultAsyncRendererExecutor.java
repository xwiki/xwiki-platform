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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.AsyncContextHandler;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.ContextUse;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link AsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Singleton
public class DefaultAsyncRendererExecutor implements AsyncRendererExecutor
{
    @Inject
    @Named(AsyncRendererJobStatus.JOBTYPE)
    private Provider<Job> jobProvider;

    @Inject
    private JobExecutor executor;

    @Inject
    private ContextStoreManager contextStore;

    @Inject
    private AsyncRendererCache cache;

    @Inject
    private AsyncContext asyncContext;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    private AtomicLong uniqueId = new AtomicLong();

    @Override
    public AsyncRendererResult getResult(List<String> id, boolean wait) throws InterruptedException
    {
        //////////////////////////////////////////////
        // Try running job

        Job job = this.executor.getJob(id);

        if (job != null) {
            AsyncRendererJobStatus status = (AsyncRendererJobStatus) job.getStatus();

            if (status.getState() != State.FINISHED && wait) {
                job.join();
            }

            return status.getResult();
        }

        //////////////////////////////////////////////
        // Try cache

        AsyncRendererJobStatus status = this.cache.get(id);

        if (status != null) {
            return status.getResult();
        }

        return null;
    }

    @Override
    public AsyncRendererJobStatus render(AsyncRenderer renderer, Set<String> contextEntries)
        throws JobException, RenderingException
    {
        return render(renderer, contextEntries, null, null);
    }

    @Override
    public AsyncRendererJobStatus render(AsyncRenderer renderer, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException, RenderingException
    {
        boolean async = renderer.isAsyncAllowed() && this.asyncContext.isEnabled();

        // Get context and job id
        Map<String, Serializable> context = getContext(renderer, async, contextEntries);

        // Generate job id
        List<String> jobId = getJobId(renderer, context);

        if (renderer.isCacheAllowed()) {
            AsyncRendererJobStatus status = getCurrent(jobId);

            if (status != null) {
                if (status.getResult() != null) {
                    injectUses(status);
                }

                return status;
            }
        }

        ////////////////////////////////
        // Execute the renderer

        AsyncRendererJobStatus status;

        AsyncRendererJobRequest request = new AsyncRendererJobRequest();
        request.setRenderer(renderer);

        if (async) {
            if (context != null) {
                request.setContext(context);
            }

            // If cache is disabled make sure the id is unique
            if (!renderer.isCacheAllowed()) {
                jobId.add(String.valueOf(this.uniqueId.incrementAndGet()));
            }

            request.setId(jobId);
            request.setRight(right, rightEntity);

            Job job = this.executor.execute(AsyncRendererJobStatus.JOBTYPE, request);

            status = (AsyncRendererJobStatus) job.getStatus();
        } else {
            // If async is disabled run the renderer in the current thread
            if (renderer.isCacheAllowed()) {
                // Prepare to catch stuff to invalidate the cache
                ((DefaultAsyncContext) this.asyncContext).pushContextUse();

                AsyncRendererResult result = renderer.render(false, true);

                // Get suff to invalidate the cache
                ContextUse contextUse = ((DefaultAsyncContext) this.asyncContext).popContextUse();

                // Create a pseudo job status
                status = new AsyncRendererJobStatus(request, result, contextUse.getReferences(),
                    contextUse.getRoleTypes(), contextUse.getRoles(), contextUse.getUses());

                request.setId(jobId);

                this.cache.put(status);
            } else {
                AsyncRendererResult result = renderer.render(false, false);

                // Ceate a pseudo job status
                status = new AsyncRendererJobStatus(request, result);
            }
        }

        return status;
    }

    private void injectUses(AsyncRendererJobStatus status)
    {
        Map<String, Collection<Object>> uses = status.getUses();

        if (uses != null) {
            for (Map.Entry<String, Collection<Object>> entry : uses.entrySet()) {
                AsyncContextHandler handler;
                try {
                    handler = this.componentManager.getInstance(AsyncContextHandler.class, entry.getKey());
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to get AsyncContextHandler with type [{}]", entry.getKey(), e);

                    continue;
                }

                handler.use(entry.getValue());
            }
        }
    }

    private Map<String, Serializable> getContext(AsyncRenderer renderer, boolean async, Set<String> contextEntries)
        throws JobException
    {
        if ((async || renderer.isCacheAllowed()) && contextEntries != null) {
            try {
                return this.contextStore.save(contextEntries);
            } catch (ComponentLookupException e) {
                throw new JobException("Failed to save the context", e);
            }
        }

        return null;
    }

    private AsyncRendererJobStatus getCurrent(List<String> jobId)
    {
        // Try to find the job status in a running job
        Job job = this.executor.getJob(jobId);

        // Found a running job, return it
        if (job instanceof AsyncRendererJob) {
            return (AsyncRendererJobStatus) job.getStatus();
        }

        // Try to find the job status in the cache
        AsyncRendererJobStatus status = this.cache.get(jobId);

        // Found a cache entry, return it
        if (status != null) {
            return status;
        }

        return null;
    }

    private List<String> getJobId(AsyncRenderer renderer, Map<String, Serializable> context)
    {
        List<String> rendererId = renderer.getId();

        if (MapUtils.isEmpty(context)) {
            return rendererId;
        }

        // Order context key to have a reliable job id
        Map<String, Serializable> orderedMap = new TreeMap<>(context);

        List<String> id = new ArrayList<>(rendererId.size() + (orderedMap.size() * 2));
        id.addAll(rendererId);
        for (Map.Entry<String, Serializable> entry : orderedMap.entrySet()) {
            id.add(entry.getKey());
            id.add(String.valueOf(entry.getValue()));
        }

        return id;
    }
}

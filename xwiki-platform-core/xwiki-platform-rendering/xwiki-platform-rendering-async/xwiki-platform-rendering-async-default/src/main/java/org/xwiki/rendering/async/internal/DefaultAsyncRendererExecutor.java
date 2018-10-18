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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link AsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
public class DefaultAsyncRendererExecutor implements AsyncRendererExecutor
{
    @Inject
    private JobExecutor executor;

    @Inject
    private ContextStoreManager contextStore;

    @Inject
    private AsyncRendererCache cache;

    @Override
    public AsyncRendererJobStatus renderer(AsyncRenderer renderer, Set<String> contextEntries) throws JobException
    {
        return renderer(renderer, contextEntries, null, null);
    }

    @Override
    public AsyncRendererJobStatus renderer(AsyncRenderer renderer, Set<String> contextEntries, Right right,
        EntityReference rightEntity) throws JobException
    {
        // Get context
        Map<String, Serializable> context;
        if (contextEntries != null) {
            try {
                context = this.contextStore.save(contextEntries);
            } catch (ComponentLookupException e) {
                throw new JobException("Failed to save the context", e);
            }
        } else {
            context = null;
        }

        // Generate job id
        List<String> jobId = getJobId(renderer.getId(), context);

        // Try to find the job status in a running job
        Job job = this.executor.getJob(jobId);

        // Found a running job, return it
        if (job instanceof AsyncRendererJob) {
            return (AsyncRendererJobStatus) job.getStatus();
        }

        // Generate cache key
        String cacheKey = toCacheKey(jobId);

        // Try to find the job status in the cache
        AsyncRendererJobStatus status = this.cache.get(cacheKey);

        // Found a cache entry, return it
        if (status != null) {
            return status;
        }

        // Start a new job
        AsyncRendererJobRequest request = new AsyncRendererJobRequest();
        request.setId(jobId);
        request.setRenderer(renderer);
        request.setRight(right, rightEntity);
        if (context != null) {
            request.setContext(context);
        }

        job = this.executor.execute(AsyncRendererJobStatus.JOBTYPE, request);

        return (AsyncRendererJobStatus) job.getStatus();
    }

    private List<String> getJobId(List<String> prefix, Map<String, Serializable> context)
    {
        if (context == null || context.isEmpty()) {
            return prefix;
        }

        List<String> id = new ArrayList<>(prefix.size() + (context.size() * 2));
        for (Map.Entry<String, Serializable> entry : context.entrySet()) {
            id.add(entry.getKey());
            id.add(String.valueOf(entry.getValue()));
        }

        return id;
    }

    private String toCacheKey(List<String> jobId)
    {
        StringBuilder builder = new StringBuilder();

        for (String element : jobId) {
            builder.append(element.length()).append(':').append(element);
        }

        return builder.toString();
    }

    @Override
    public void flush()
    {
        this.cache.flush();
    }
}

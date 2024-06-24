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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.CacheControl;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.AsyncContextHandler;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.ContextUse;
import org.xwiki.security.authorization.AuthorExecutor;

import com.xpn.xwiki.internal.context.XWikiContextContextStore;

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
    protected AuthorExecutor authorExecutor;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private CacheControl cacheControl;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    private AtomicLong clientIdCount = new AtomicLong();

    private String newClientId()
    {
        return String.valueOf(this.clientIdCount.incrementAndGet());
    }

    @Override
    public AsyncRendererJobStatus getAsyncStatus(List<String> id, String clientId)
    {
        //////////////////////////////////////////////
        // Try running job

        Job job = this.executor.getJob(id);

        if (job != null) {
            AsyncRendererJobStatus status = (AsyncRendererJobStatus) job.getStatus();

            if (status.getClients().contains(clientId)) {
                return status;
            }
        }

        //////////////////////////////////////////////
        // Try cache

        AsyncRendererJobStatus status = this.cache.getAsync(clientId);

        if (status != null) {
            return status;
        }

        return null;
    }

    @Override
    public AsyncRendererJobStatus getAsyncStatus(List<String> id, String clientId, long time, TimeUnit unit)
        throws InterruptedException
    {
        AsyncRendererJobStatus status = getAsyncStatus(id, clientId);

        if (status != null && status.getState() != State.FINISHED) {
            Job job = this.executor.getJob(id);

            if (job != null) {
                // Wait for the job to be finished (or until the timeout is reached)
                job.join(time, unit);
            }
        }

        return status;
    }

    @Override
    public AsyncRendererExecutorResponse render(AsyncRenderer renderer, AsyncRendererConfiguration configuration)
        throws JobException, RenderingException
    {
        // if placeholder is forced, then we always consider it as async.
        boolean asyncAllowed = configuration.isPlaceHolderForced()
                                || (renderer.isAsyncAllowed() && this.asyncContext.isEnabled());
        boolean cacheAllowed = renderer.isCacheAllowed();

        // Get context and job id
        Map<String, Serializable> context = getContext(asyncAllowed, cacheAllowed, configuration);

        // Generate job id
        List<String> jobId = getJobId(renderer, context);

        if (cacheAllowed) {
            this.cache.getLock().readLock().lock();

            try {
                AsyncRendererJobStatus status = getCurrent(jobId);

                if (status != null
                    && (status.getEndDate() == null || this.cacheControl.isCacheReadAllowed(status.getEndDate()))) {
                    if (status.getResult() != null && !configuration.isPlaceHolderForced()) {
                        // Available cached result, return it

                        injectUses(status);

                        return new AsyncRendererExecutorResponse(status);
                    } else if (asyncAllowed) {
                        // Already running job, associate it with another client
                        return new AsyncRendererExecutorResponse(status, newClientId());
                    }
                }
            } finally {
                this.cache.getLock().readLock().unlock();
            }
        }

        ////////////////////////////////
        // Execute the renderer

        AsyncRendererExecutorResponse response;

        AsyncRendererJobRequest request = new AsyncRendererJobRequest();
        request.setRenderer(renderer);
        request.setJobGroupPath(renderer.getJobGroupPath());

        if (asyncAllowed) {
            this.cache.getLock().writeLock().lock();

            try {
                if (context != null) {
                    request.setContext(context);
                }

                String asyncClientId = newClientId();

                // If cache is disabled make sure the id is unique
                if (!renderer.isCacheAllowed()) {
                    jobId.add(asyncClientId);
                }

                request.setId(jobId);

                Job job = this.executor.execute(AsyncRendererJobStatus.JOBTYPE, request);

                AsyncRendererJobStatus status = (AsyncRendererJobStatus) job.getStatus();

                response = new AsyncRendererExecutorResponse(status, asyncClientId);
            } finally {
                this.cache.getLock().writeLock().unlock();
            }
        } else {
            AsyncRendererJobStatus status;

            // If async is disabled run the renderer in the current thread
            if (renderer.isCacheAllowed()) {
                // Prepare to catch stuff to invalidate the cache
                if (this.asyncContext instanceof DefaultAsyncContext) {
                    ((DefaultAsyncContext) this.asyncContext).pushContextUse();
                }

                // Mark the context document as used if it was explicitly set in the context, unless the context 
                // document is null.
                if (configuration.getContextEntries() != null && configuration.getContextEntries()
                    .contains(XWikiContextContextStore.PROP_DOCUMENT_REFERENCE))
                {
                    DocumentReference currentDocumentReference =
                        this.documentAccessBridge.getCurrentDocumentReference();
                    if (currentDocumentReference != null) {
                        this.asyncContext.useEntity(currentDocumentReference);
                    }
                }

                AsyncRendererResult result = syncRender(renderer, true, configuration);

                // Get suff to invalidate the cache
                if (this.asyncContext instanceof DefaultAsyncContext) {
                    ContextUse contextUse = ((DefaultAsyncContext) this.asyncContext).popContextUse();

                    // Create a pseudo job status
                    status = new AsyncRendererJobStatus(request, result, contextUse.getReferences(),
                        contextUse.getRoleTypes(), contextUse.getRoles(), contextUse.getRights(), contextUse.getUses());
                } else {
                    // Create a pseudo job status
                    status = new AsyncRendererJobStatus(request, result, null, null, null, null, null);
                }

                request.setId(jobId);

                this.cache.put(status);
            } else {
                AsyncRendererResult result = syncRender(renderer, false, configuration);

                // Create a pseudo job status
                status = new AsyncRendererJobStatus(request, result);
            }

            response = new AsyncRendererExecutorResponse(status);
        }

        return response;
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

    private Map<String, Serializable> getContext(boolean asyncAllowed, boolean cacheAllowed,
        AsyncRendererConfiguration configuration) throws JobException
    {
        Map<String, Serializable> savedContext = null;

        if (asyncAllowed || cacheAllowed) {
            if (configuration.getContextEntries() != null) {
                try {
                    savedContext = this.contextStore.save(configuration.getContextEntries());
                } catch (ComponentLookupException e) {
                    throw new JobException("Failed to save the context", e);
                }
            }

            // If async inject the configured author
            if (asyncAllowed && configuration.isSecureReferenceSet()) {
                if (savedContext == null) {
                    savedContext = new HashMap<>();
                } else {
                    // The map generated by #save might not be modifiable
                    savedContext = new HashMap<>(savedContext);
                }

                savedContext.put(XWikiContextContextStore.PROP_SECURE_AUTHOR, configuration.getSecureAuthorReference());
                savedContext.put(XWikiContextContextStore.PROP_SECURE_DOCUMENT,
                    configuration.getSecureDocumentReference());
            }
        }

        return savedContext;
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
        AsyncRendererJobStatus status = this.cache.getSync(jobId);

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
            // We don't really need actual value so let's play it safe regarding values not well supported by
            // application servers in URLs
            id.add(encodeId(String.valueOf(entry.getValue())));
        }

        return id;
    }

    private String encodeId(String value)
    {
        StringBuilder builder = new StringBuilder(value.length() * 3);

        for (int i = 0; i < value.length(); ++i) {
            char c = value.charAt(i);

            boolean encode = false;

            switch (c) {
                // '/' and '\' have been known to cause issues with various default server setup (Tomcat for example)
                case '/', '\\':
                    encode = true;
                    break;

                default:
                    break;
            }

            if (encode) {
                encode(c, builder);
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private void encode(char c, StringBuilder builder)
    {
        byte[] ba = String.valueOf(c).getBytes(StandardCharsets.UTF_8);

        for (int j = 0; j < ba.length; j++) {
            builder.append('%');

            char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
            builder.append(ch);

            ch = Character.forDigit(ba[j] & 0xF, 16);
            builder.append(ch);
        }
    }
}

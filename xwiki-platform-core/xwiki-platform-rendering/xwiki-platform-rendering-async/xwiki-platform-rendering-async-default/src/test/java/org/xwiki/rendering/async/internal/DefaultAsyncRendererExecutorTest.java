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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_DOCUMENT_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultAsyncRendererExecutor}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultAsyncRendererExecutorTest
{
    private static final String CELEMENT1 = "celement1";

    private static final String CELEMENT2 = "celement2";

    private static final Set<String> CELEMENTS = new LinkedHashSet<>(Arrays.asList(CELEMENT1, CELEMENT2));

    private AsyncRendererConfiguration configuration;

    @MockComponent
    private JobExecutor jobs;

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private AsyncRendererCache cache;

    @MockComponent
    private ContextStoreManager context;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @InjectMockComponents
    private DefaultAsyncRendererExecutor executor;

    private AsyncRenderer renderer;

    private AsyncRendererJob job;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private JobGroupPath jobGroupPath;

    @BeforeEach
    void beforeEach() throws RenderingException, ComponentLookupException, JobException
    {
        this.renderer = mock(AsyncRenderer.class);

        when(this.renderer.render(true, true)).thenReturn(new AsyncRendererResult("true true"));
        when(this.renderer.render(false, false)).thenReturn(new AsyncRendererResult("false false"));
        when(this.renderer.render(true, false)).thenReturn(new AsyncRendererResult("true false"));
        when(this.renderer.render(false, true)).thenReturn(new AsyncRendererResult("false true"));
        this.jobGroupPath = new JobGroupPath(Arrays.asList("Something", "Foo", "Bar"));
        when(this.renderer.getJobGroupPath()).thenReturn(this.jobGroupPath);

        when(this.cache.getLock()).thenReturn(this.lock);

        this.configuration = new AsyncRendererConfiguration();
        this.configuration.setContextEntries(CELEMENTS);

        Map<String, Serializable> map = new HashMap<>();
        map.put(CELEMENT1, "value1\\");
        map.put(CELEMENT2, "value2/");
        when(this.context.save(CELEMENTS)).thenReturn(Collections.unmodifiableMap(map));

        this.job = mock(AsyncRendererJob.class);

        when(this.jobs.execute(same(AsyncRendererJobStatus.JOBTYPE), any(AsyncRendererJobRequest.class)))
            .thenAnswer(invocation -> {
                AsyncRendererJobStatus status = new AsyncRendererJobStatus(invocation.getArgument(1), null, null);

                status.setResult(this.renderer.render(true, this.renderer.isCacheAllowed()));

                when(this.job.getStatus()).thenReturn(status);

                return this.job;
            });
    }

    @Test
    void rendererAsyncCachedContextDisabled() throws JobException, RenderingException
    {
        // Disabled async in the context
        when(this.asyncContext.isEnabled()).thenReturn(false);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNull(response.getAsyncClientId());
        assertEquals("1/2/celement1/value1%25255c/celement2/value2%25252f", response.getJobIdHTTPPath());
        assertEquals(Arrays.asList("1", "2", "celement1", "value1%5c", "celement2", "value2%2f"),
            response.getStatus().getRequest().getId());
        assertEquals("false true", response.getStatus().getResult().getResult());
    }

    @Test
    void rendererAsyncCachedContextEnabled() throws JobException, RenderingException
    {
        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNotNull(response.getAsyncClientId());
        assertEquals(Arrays.asList("1", "2", "celement1", "value1%5c", "celement2", "value2%2f"),
            response.getStatus().getRequest().getId());
        assertEquals("1/2/celement1/value1%25255c/celement2/value2%25252f", response.getJobIdHTTPPath());
        assertEquals("true true", response.getStatus().getResult().getResult());
        assertEquals(this.jobGroupPath, response.getStatus().getRequest().getJobGroupPath());
    }

    @Test
    void renderAsyncDocumentRequested() throws Exception
    {
        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "XWiki", "CurrentDoc");

        this.configuration.setContextEntries(Set.of(PROP_DOCUMENT_REFERENCE, "Document"));

        when(this.renderer.isCacheAllowed()).thenReturn(true);
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocumentReference);

        this.executor.render(this.renderer, this.configuration);

        verify(this.asyncContext).useEntity(currentDocumentReference);
    }

    @Test
    void renderAsyncDocumentRequestedButNotInContext() throws Exception
    {
        this.configuration.setContextEntries(Set.of(PROP_DOCUMENT_REFERENCE, "Document"));

        when(this.renderer.isCacheAllowed()).thenReturn(true);

        this.executor.render(this.renderer, this.configuration);

        verify(this.asyncContext, never()).useEntity(any());
    }

    @Test
    void rendererNotAsyncCachedContextEnabled() throws JobException, RenderingException
    {
        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(false);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNull(response.getAsyncClientId());
        assertEquals(Arrays.asList("1", "2", "celement1", "value1%5c", "celement2", "value2%2f"),
            response.getStatus().getRequest().getId());
        assertEquals("1/2/celement1/value1%25255c/celement2/value2%25252f", response.getJobIdHTTPPath());
        assertEquals("false true", response.getStatus().getResult().getResult());
        assertEquals(this.jobGroupPath, response.getStatus().getRequest().getJobGroupPath());
    }

    @Test
    void rendererAsyncNotCachedContextEnabled() throws JobException, RenderingException
    {
        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(false);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNotNull(response.getAsyncClientId());
        assertEquals(Arrays.asList("1", "2", "celement1", "value1%5c", "celement2", "value2%2f",
            String.valueOf(response.getAsyncClientId())), response.getStatus().getRequest().getId());
        assertEquals("1/2/celement1/value1%25255c/celement2/value2%25252f/" + response.getAsyncClientId(),
            response.getJobIdHTTPPath());
        assertEquals("true false", response.getStatus().getResult().getResult());
        assertEquals(this.jobGroupPath, response.getStatus().getRequest().getJobGroupPath());
    }

    @Test
    void rendererNotAsyncNotCachedContextEnabled() throws JobException, RenderingException
    {
        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(false);
        when(this.renderer.isCacheAllowed()).thenReturn(false);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNull(response.getAsyncClientId());
        assertNull(response.getStatus().getRequest().getId());
        assertNull(response.getJobIdHTTPPath());
        assertEquals("false false", response.getStatus().getResult().getResult());
        assertEquals(this.jobGroupPath, response.getStatus().getRequest().getJobGroupPath());
    }

    @Test
    void rendererAsyncAlreadyRunning() throws JobException, RenderingException
    {
        List<String> jobId = Arrays.asList("1", "2", "celement1", "value1%5c", "celement2", "value2%2f");

        AsyncRendererJobRequest request = mock(AsyncRendererJobRequest.class);
        AsyncRendererJobStatus status = mock(AsyncRendererJobStatus.class);
        when(status.getRequest()).thenReturn(request);
        when(this.job.getStatus()).thenReturn(status);
        when(this.jobs.getJob(jobId)).thenReturn(this.job);

        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNotNull(response.getAsyncClientId());
        assertSame(status, response.getStatus());
    }
}

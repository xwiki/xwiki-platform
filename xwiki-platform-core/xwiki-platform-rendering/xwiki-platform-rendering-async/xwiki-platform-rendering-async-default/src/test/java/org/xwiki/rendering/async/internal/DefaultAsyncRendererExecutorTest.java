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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Named;
import javax.inject.Provider;

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
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_DOCUMENT_REFERENCE;
import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_SECURE_AUTHOR;
import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_SECURE_DOCUMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    /**
     * The identifier of an asynchronous job, which includes the current author and secure document.
     */
    private static final List<String> ASYNC_JOB_ID = List.of("1", "2", "author", "xwiki:XWiki.Author", "celement1",
        "value1%5c", "celement2", "value2%2f", "secureDocument", "xwiki:Space.Secure");

    private static final String ASYNC_JOB_HTTP_PATH = "1/2/author/xwiki%253AXWiki.Author/celement1/value1%25255c"
        + "/celement2/value2%25252f/secureDocument/xwiki%253ASpace.Secure";

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

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private AuthorExecutor authorExecutor;

    private XWikiContext xcontext;

    private XWikiDocument secureDocument;

    private final DocumentReference secureDocumentReference = new DocumentReference("xwiki", "Space", "Secure");

    private final DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");

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

        this.xcontext = mock();
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.secureDocument = mock();
        when(this.secureDocument.getDocumentReference()).thenReturn(this.secureDocumentReference);
        when(this.xcontext.getSecureDocument()).thenReturn(this.secureDocument);
        when(this.xcontext.getAuthorReference()).thenReturn(this.authorReference);

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
        assertEquals(ASYNC_JOB_HTTP_PATH, response.getJobIdHTTPPath());
        assertEquals(ASYNC_JOB_ID, response.getStatus().getRequest().getId());
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
        assertEquals(ASYNC_JOB_ID, response.getStatus().getRequest().getId());
        assertEquals(ASYNC_JOB_HTTP_PATH, response.getJobIdHTTPPath());
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
        // The result depends on the rights of the current author, so they are part of the identifier of the cached
        // result even when it's not rendered asynchronously.
        assertEquals(ASYNC_JOB_ID, response.getStatus().getRequest().getId());
        assertEquals(ASYNC_JOB_HTTP_PATH, response.getJobIdHTTPPath());
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
        List<String> expectedId = new ArrayList<>(ASYNC_JOB_ID);
        expectedId.add(response.getAsyncClientId());
        assertEquals(expectedId, response.getStatus().getRequest().getId());
        assertEquals(ASYNC_JOB_HTTP_PATH + "/" + response.getAsyncClientId(), response.getJobIdHTTPPath());
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
        AsyncRendererJobRequest request = mock(AsyncRendererJobRequest.class);
        AsyncRendererJobStatus status = mock(AsyncRendererJobStatus.class);
        when(status.getRequest()).thenReturn(request);
        when(this.job.getStatus()).thenReturn(status);
        when(this.jobs.getJob(ASYNC_JOB_ID)).thenReturn(this.job);

        // Enable async in the context
        when(this.asyncContext.isEnabled()).thenReturn(true);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        assertNotNull(response.getAsyncClientId());
        assertSame(status, response.getStatus());
    }

    @Test
    void renderAsyncWithCurrentAuthor() throws Exception
    {
        AsyncRendererExecutorResponse response = renderAsync();

        assertNotNull(response.getAsyncClientId());
        Map<String, Serializable> jobContext = response.getStatus().getRequest().getContext();
        assertEquals(this.authorReference, jobContext.get(PROP_SECURE_AUTHOR));
        assertEquals(this.secureDocumentReference, jobContext.get(PROP_SECURE_DOCUMENT));
        assertEquals("value1\\", jobContext.get(CELEMENT1));
    }

    @Test
    void renderAsyncWithConfiguredAuthor() throws Exception
    {
        DocumentReference configuredDocument = new DocumentReference("xwiki", "Space", "Configured");
        DocumentReference configuredAuthor = new DocumentReference("xwiki", "XWiki", "ConfiguredAuthor");
        this.configuration.setSecureReference(configuredDocument, configuredAuthor);
        // The configured author doesn't depend on the current context.
        when(this.renderingContext.isRestricted()).thenReturn(true);
        when(this.xcontext.hasDroppedPermissions()).thenReturn(true);

        AsyncRendererExecutorResponse response = renderAsync();

        assertNotNull(response.getAsyncClientId());
        Map<String, Serializable> jobContext = response.getStatus().getRequest().getContext();
        assertEquals(configuredAuthor, jobContext.get(PROP_SECURE_AUTHOR));
        assertEquals(configuredDocument, jobContext.get(PROP_SECURE_DOCUMENT));
    }

    @Test
    void renderSyncCachedWithConfiguredAuthor() throws Exception
    {
        DocumentReference configuredDocument = new DocumentReference("xwiki", "Space", "Configured");
        DocumentReference configuredAuthor = new DocumentReference("xwiki", "XWiki", "ConfiguredAuthor");
        this.configuration.setSecureReference(configuredDocument, configuredAuthor);

        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isCacheAllowed()).thenReturn(true);

        AsyncRendererExecutorResponse response = this.executor.render(this.renderer, this.configuration);

        // The content is executed with the rights of the configured author, so the author and its document are part
        // of the identifier of the cached result.
        assertEquals(List.of("1", "2", "author", "xwiki:XWiki.ConfiguredAuthor", "celement1", "value1%5c",
            "celement2", "value2%2f", "secureDocument", "xwiki:Space.Configured"),
            response.getStatus().getRequest().getId());
        verify(this.authorExecutor).call(any(), same(configuredAuthor), same(configuredDocument));
    }

    @Test
    void renderInRestrictedRenderingContext() throws Exception
    {
        when(this.renderingContext.isRestricted()).thenReturn(true);

        assertSyncRender(renderAsync());
    }

    @Test
    void renderWithRestrictedSecureDocument() throws Exception
    {
        when(this.secureDocument.isRestricted()).thenReturn(true);

        assertSyncRender(renderAsync());
    }

    @Test
    void renderWithDroppedPermissions() throws Exception
    {
        when(this.xcontext.hasDroppedPermissions()).thenReturn(true);

        assertSyncRender(renderAsync());
    }

    @Test
    void renderForcedPlaceHolderWithDroppedPermissions() throws Exception
    {
        when(this.xcontext.hasDroppedPermissions()).thenReturn(true);
        this.configuration.setPlaceHolderForced(true);

        AsyncRendererExecutorResponse response = renderAsync();

        // The content is rendered in the current thread, but the result is made available to the placeholder.
        String clientId = response.getAsyncClientId();
        assertNotNull(clientId);
        AsyncRendererJobStatus status = response.getStatus();
        assertEquals("false false", status.getResult().getResult());
        assertEquals(List.of("1", "2", clientId), status.getRequest().getId());
        assertTrue(status.getClients().contains(clientId));
        verify(this.cache).put(status);
        verify(this.jobs, never()).execute(any(), any());
    }

    @Test
    void renderForcedPlaceHolderWithDroppedPermissionsAndCache() throws Exception
    {
        when(this.xcontext.hasDroppedPermissions()).thenReturn(true);
        when(this.renderer.isCacheAllowed()).thenReturn(true);
        this.configuration.setPlaceHolderForced(true);

        AsyncRendererExecutorResponse response = renderAsync();

        String clientId = response.getAsyncClientId();
        assertNotNull(clientId);
        AsyncRendererJobStatus status = response.getStatus();
        assertEquals("false true", status.getResult().getResult());
        assertEquals(ASYNC_JOB_ID, status.getRequest().getId());
        assertTrue(status.getClients().contains(clientId));
        verify(this.cache).put(status);
        verify(this.jobs, never()).execute(any(), any());
    }

    private AsyncRendererExecutorResponse renderAsync() throws Exception
    {
        when(this.asyncContext.isEnabled()).thenReturn(true);
        when(this.renderer.getId()).thenReturn(Arrays.asList("1", "2"));
        when(this.renderer.isAsyncAllowed()).thenReturn(true);

        return this.executor.render(this.renderer, this.configuration);
    }

    private void assertSyncRender(AsyncRendererExecutorResponse response) throws Exception
    {
        assertNull(response.getAsyncClientId());
        assertEquals("false false", response.getStatus().getResult().getResult());
        verify(this.jobs, never()).execute(any(), any());
    }
}

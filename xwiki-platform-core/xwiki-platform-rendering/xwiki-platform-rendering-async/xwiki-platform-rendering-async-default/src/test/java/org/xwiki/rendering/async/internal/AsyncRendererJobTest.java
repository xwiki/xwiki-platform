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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.DefaultAsyncContext.ContextUse;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_DOCUMENT_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link AsyncRendererJob}.
 * 
 * @version $Id$
 */
@ComponentTest
class AsyncRendererJobTest
{
    @InjectMockComponents
    private AsyncRendererJob job;

    private DefaultAsyncContext asyncContext;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private AsyncRenderer renderer;

    private AsyncRendererJobRequest request = new AsyncRendererJobRequest();

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        // Register the default implementation type as the job only collects the uses with it.
        this.asyncContext = mock();
        componentManager.registerComponent(AsyncContext.class, this.asyncContext);
    }

    @BeforeEach
    void beforeEach()
    {
        this.request.setRenderer(this.renderer);

        when(this.asyncContext.popContextUse()).thenReturn(new ContextUse());
    }

    @Test
    void runWithNullContext() throws RenderingException
    {
        this.job.initialize(this.request);

        this.job.run();

        verify(this.renderer).render(true, false);
    }

    @Test
    void runInternalDocumentRequested()
    {
        this.request.setContext(Map.of(PROP_DOCUMENT_REFERENCE, "Document"));

        DocumentReference currentDocumentReference = new DocumentReference("xwiki", "XWiki", "CurrentDoc");
        when(this.documentAccessBridge.getCurrentDocumentReference())
            .thenReturn(currentDocumentReference);

        this.job.initialize(this.request);
        this.job.run();

        verify(this.asyncContext).useEntity(currentDocumentReference);
    }

    @Test
    void runInternalDocumentRequestedButNotInContext()
    {
        this.request.setContext(Map.of(PROP_DOCUMENT_REFERENCE, "Document"));

        this.job.initialize(this.request);
        this.job.run();

        verify(this.asyncContext, never()).useEntity(any());
    }

    @Test
    void runSetsResultAfterUses() throws RenderingException
    {
        AsyncRendererResult result = new AsyncRendererResult("result");
        when(this.renderer.render(true, false)).thenReturn(result);

        ContextUse contextUse = new ContextUse();
        contextUse.getUses().put("type", List.of("value"));
        AtomicReference<AsyncRendererResult> resultWhenCollectingUses = new AtomicReference<>();
        when(this.asyncContext.popContextUse()).thenAnswer(invocation -> {
            resultWhenCollectingUses.set(this.job.getStatus().getResult());

            return contextUse;
        });

        this.job.initialize(this.request);
        this.job.run();

        // The status of the running job is read without lock by other threads: the result must be published last.
        assertNull(resultWhenCollectingUses.get());
        Map<String, Collection<Object>> uses = this.job.getStatus().getUses();
        assertEquals(List.of("value"), uses.get("type"));
        assertEquals(result, this.job.getStatus().getResult());
    }
}

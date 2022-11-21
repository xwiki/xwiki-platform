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

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static com.xpn.xwiki.internal.context.XWikiContextContextStore.PROP_DOCUMENT_REFERENCE;
import static org.mockito.ArgumentMatchers.any;
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

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private AsyncRenderer renderer;

    private AsyncRendererJobRequest request = new AsyncRendererJobRequest();

    @BeforeEach
    void beforeEach()
    {
        this.request.setRenderer(this.renderer);
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
}

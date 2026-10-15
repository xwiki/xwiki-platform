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
package org.xwiki.rendering.async.internal.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.rendering.async.AsyncContextHandler;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AsyncRendererResourceReferenceHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class AsyncRendererResourceReferenceHandlerTest
{
    private static final String HANDLER_TYPE = "test";

    @MockComponent
    private AsyncRendererExecutor executor;

    @MockComponent
    private Container container;

    @MockComponent
    private ComponentManager componentManager;

    @InjectMockComponents
    private AsyncRendererResourceReferenceHandler handler;

    @Test
    void handleFinishedWithMultiLineHTMLHead() throws Exception
    {
        AsyncRendererResourceReference reference = new AsyncRendererResourceReference(
            AsyncRendererResourceReferenceHandler.TYPE, List.of("id"), "clientId", 10, "wiki");

        AsyncRendererJobStatus status = mock();
        when(status.getState()).thenReturn(State.FINISHED);
        when(status.getUses()).thenReturn(Map.of(HANDLER_TYPE, List.of("value")));
        when(this.executor.getAsyncStatus(List.of("id"), "clientId", 10, TimeUnit.MILLISECONDS)).thenReturn(status);

        AsyncContextHandler contextHandler = mock();
        doAnswer(invocation -> invocation.<StringBuilder>getArgument(0)
            .append("<link rel='stylesheet' href='a.css'/>\n<link rel='stylesheet' href='b.css'/>\n"))
            .when(contextHandler).addHTMLHead(any(), anyCollection(), eq(false));
        doAnswer(invocation -> invocation.<StringBuilder>getArgument(0)
            .append("<script src='a.js'></script>\r\n<script src='b.js'></script>\n"))
            .when(contextHandler).addHTMLScripts(any(), anyCollection());
        when(this.componentManager.getInstance(AsyncContextHandler.class, HANDLER_TYPE)).thenReturn(contextHandler);

        HttpServletResponse httpResponse = mock();
        ServletOutputStream outputStream = mock();
        when(httpResponse.getOutputStream()).thenReturn(outputStream);
        when(this.container.getResponse()).thenReturn(new ServletResponse(httpResponse));

        ResourceReferenceHandlerChain chain = mock();
        this.handler.handle(reference, chain);

        // Line breaks are forbidden in header values.
        verify(httpResponse).addHeader("X-XWIKI-HTML-HEAD",
            "<link rel='stylesheet' href='a.css'/> <link rel='stylesheet' href='b.css'/> ");
        verify(httpResponse).addHeader("X-XWIKI-HTML-SCRIPTS",
            "<script src='a.js'></script>  <script src='b.js'></script> ");
    }
}

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
package org.xwiki.rendering.async.internal.block;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.rendering.async.internal.AsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.AsyncRendererExecutor;
import org.xwiki.rendering.async.internal.AsyncRendererExecutorResponse;
import org.xwiki.rendering.async.internal.AsyncRendererJobStatus;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultBlockAsyncRendererExecutor}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultBlockAsyncRendererExecutorTest
{
    private static final String JOB_ID_PATH = "template/environment%253A%252Ftemplates%252Ftest.vm/\"&{a}";

    private static final String ESCAPED_JOB_ID_PATH =
        "template/environment%253A%252Ftemplates%252Ftest.vm/&#34;&#38;&#123;a}";

    private static final String CLIENT_ID = "client1";

    @InjectMockComponents
    private DefaultBlockAsyncRendererExecutor blockExecutor;

    @MockComponent
    private AsyncRendererExecutor executor;

    private final BlockAsyncRenderer renderer = mock();

    private final AsyncRendererConfiguration configuration = new AsyncRendererConfiguration();

    private final AsyncRendererJobStatus status = mock();

    @BeforeEach
    void beforeEach() throws Exception
    {
        AsyncRendererExecutorResponse response = mock();
        when(response.getStatus()).thenReturn(this.status);
        when(response.getJobIdHTTPPath()).thenReturn(JOB_ID_PATH);
        when(response.getAsyncClientId()).thenReturn(CLIENT_ID);
        when(this.executor.render(this.renderer, this.configuration)).thenReturn(response);
    }

    @Test
    void renderReturnsResultWhenAvailable() throws Exception
    {
        when(this.status.getResult()).thenReturn(new BlockAsyncRendererResult("result", null));

        assertEquals("result", this.blockExecutor.render(this.renderer, this.configuration));
    }

    @Test
    void renderReturnsBlockPlaceholderWhenResultNotAvailable() throws Exception
    {
        assertEquals("<div class=\"xwiki-async\" data-xwiki-async-id=\"" + ESCAPED_JOB_ID_PATH
            + "\" data-xwiki-async-client-id=\"" + CLIENT_ID + "\"></div>",
            this.blockExecutor.render(this.renderer, this.configuration));
    }

    @Test
    void renderReturnsInlinePlaceholderWhenPlaceholderForced() throws Exception
    {
        when(this.renderer.isInline()).thenReturn(true);
        when(this.status.getResult()).thenReturn(new BlockAsyncRendererResult("result", null));
        this.configuration.setPlaceHolderForced(true);

        assertEquals("<span class=\"xwiki-async\" data-xwiki-async-id=\"" + ESCAPED_JOB_ID_PATH
            + "\" data-xwiki-async-client-id=\"" + CLIENT_ID + "\"></span>",
            this.blockExecutor.render(this.renderer, this.configuration));
    }
}

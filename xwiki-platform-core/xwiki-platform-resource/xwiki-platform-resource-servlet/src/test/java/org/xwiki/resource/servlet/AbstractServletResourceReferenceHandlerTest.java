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
package org.xwiki.resource.servlet;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Response;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Test of {@link AbstractServletResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class AbstractServletResourceReferenceHandlerTest
{
    @InjectMockComponents
    private TestableServletResourceReferenceHandler referenceHandler;

    @MockComponent
    private Container container;

    @Mock
    private InputStream inputStream;

    @Mock
    private InputStream filterStream;

    @Mock
    private Response response;

    @Mock
    private OutputStream outputStream;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    /**
     * Local component to test AbstractServletResourceReferenceHandler even though it is an abstract class.
     */
    @Component
    public static class TestableServletResourceReferenceHandler
        extends AbstractServletResourceReferenceHandler<ResourceReference>
    {
        InputStream inputStream;

        InputStream filterStream;

        boolean cachePublic;

        boolean accessible = true;

        @Override
        public List<ResourceType> getSupportedResourceReferences()
        {
            return null;
        }

        @Override
        protected InputStream getResourceStream(ResourceReference resourceReference)
        {
            return this.inputStream;
        }

        @Override
        protected String getResourceName(ResourceReference resourceReference)
        {
            return null;
        }

        @Override
        protected InputStream filterResource(ResourceReference resourceReference, InputStream resourceStream)
        {
            return this.filterStream;
        }

        @Override
        protected boolean isResourceAccessible(ResourceReference resourceReference)
        {
            return this.accessible;
        }

        @Override
        protected boolean isResourceCachePublic(ResourceReference resourceReference)
        {
            return this.cachePublic;
        }
    }

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.container.getResponse()).thenReturn(this.response);
        when(this.response.getOutputStream()).thenReturn(this.outputStream);

        // Initialize the reference handler with input stream mocks in order to be able to verify them in the tests.
        this.referenceHandler.inputStream = this.inputStream;
        this.referenceHandler.filterStream = this.filterStream;
    }

    /**
     * Verify that the input and filter streams are closed after use.
     */
    @Test
    void handleStreamClosed() throws Exception
    {
        this.referenceHandler.handle(mock(ResourceReference.class), mock(ResourceReferenceHandlerChain.class));

        verify(this.inputStream).close();
        verify(this.filterStream).close();
    }

    /**
     * Verify that the resources are closed even if an error occurs during the execution.
     */
    @Test
    void handleStreamClosedWhenErrorOnServeResource() throws Exception
    {
        // Fail at first call in serveResource, then return a result in sendError
        when(this.container.getResponse()).thenThrow(new RuntimeException()).thenReturn(this.response);

        this.referenceHandler
            .handle(mock(ResourceReference.class), mock(ResourceReferenceHandlerChain.class));

        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to read resource [null]", this.logCapture.getMessage(0));

        verify(this.filterStream).close();
        verify(this.inputStream).close();
    }

    /**
     * Verify the resources are closed even when filterResource returns the same stream as getResourceStream.
     */
    @Test
    void handleFilteredStreamUnchanged() throws Exception
    {
        this.referenceHandler.filterStream = this.inputStream;

        this.referenceHandler.handle(mock(ResourceReference.class), mock(ResourceReferenceHandlerChain.class));

        verify(this.inputStream, times(2)).close();
    }

    @ParameterizedTest
    @CsvSource({ "false, 'private, no-cache'", "true, public" })
    void handleSetsCacheHeaders(boolean cachePublic, String expectedCacheControl) throws Exception
    {
        ServletResponse servletResponse = mock();
        HttpServletResponse httpServletResponse = mock();
        when(servletResponse.getHttpServletResponse()).thenReturn(httpServletResponse);
        when(servletResponse.getOutputStream()).thenReturn(this.outputStream);
        when(this.container.getResponse()).thenReturn(servletResponse);
        this.referenceHandler.filterStream = new ByteArrayInputStream(new byte[] { 1, 2, 3 });
        this.referenceHandler.cachePublic = cachePublic;

        this.referenceHandler.handle(mock(ResourceReference.class), mock(ResourceReferenceHandlerChain.class));

        verify(httpServletResponse).setHeader("Cache-Control", expectedCacheControl);
        verify(httpServletResponse, times(cachePublic ? 1 : 0)).setDateHeader(eq("Expires"), anyLong());
        verify(httpServletResponse).setDateHeader(eq("Last-Modified"), anyLong());
    }

    @ParameterizedTest
    @CsvSource({ "true, 304", "false, 403" })
    void handleRevalidationChecksAccess(boolean accessible, int expectedStatus) throws Exception
    {
        ServletRequest servletRequest = mock();
        HttpServletRequest httpServletRequest = mock();
        when(servletRequest.getHttpServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("If-Modified-Since")).thenReturn("Fri, 09 Oct 2026 13:00:00 GMT");
        when(this.container.getRequest()).thenReturn(servletRequest);
        ServletResponse servletResponse = mock();
        HttpServletResponse httpServletResponse = mock();
        when(servletResponse.getHttpServletResponse()).thenReturn(httpServletResponse);
        when(this.container.getResponse()).thenReturn(servletResponse);
        this.referenceHandler.accessible = accessible;

        this.referenceHandler.handle(mock(ResourceReference.class), mock(ResourceReferenceHandlerChain.class));

        if (accessible) {
            verify(httpServletResponse).setStatus(expectedStatus);
            verify(httpServletResponse, never()).sendError(anyInt(), any());
        } else {
            verify(httpServletResponse).sendError(eq(expectedStatus), any());
            verify(httpServletResponse, never()).setStatus(anyInt());
        }
        verify(this.inputStream, never()).close();
    }
}

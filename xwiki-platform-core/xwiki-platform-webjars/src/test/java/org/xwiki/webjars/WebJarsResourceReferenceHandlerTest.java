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
package org.xwiki.webjars;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.webjars.internal.TestableWebJarsResourceReferenceHandler;

/**
 * Unit tests for {@link org.xwiki.webjars.internal.WebJarsResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class WebJarsResourceReferenceHandlerTest
{
    @Rule
    public MockitoComponentMockingRule<TestableWebJarsResourceReferenceHandler> componentManager =
        new MockitoComponentMockingRule<TestableWebJarsResourceReferenceHandler>(TestableWebJarsResourceReferenceHandler.class);

    @Test
    public void executeWhenResourceDoesntExist() throws Exception
    {
        EntityResourceReference reference = new EntityResourceReference(new DocumentReference("wiki", "space", "page"),
            EntityResourceAction.VIEW);
        reference.addParameter("value", "angular/2.1.11/angular.js");
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        TestableWebJarsResourceReferenceHandler handler = this.componentManager.getComponentUnderTest();

        ClassLoader classLoader = mock(ClassLoader.class);

        handler.setClassLoader(classLoader);

        handler.handle(reference, chain);

        verify(classLoader).getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js");
    }

    @Test
    public void executeWhenResourceExists() throws Exception
    {
        EntityResourceReference reference = new EntityResourceReference(new DocumentReference("wiki", "space", "page"),
            EntityResourceAction.VIEW);
        reference.addParameter("value", "angular/2.1.11/angular.js");
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        TestableWebJarsResourceReferenceHandler handler = this.componentManager.getComponentUnderTest();

        ClassLoader classLoader = mock(ClassLoader.class);
        ByteArrayInputStream bais = new ByteArrayInputStream("content".getBytes());
        when(classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            bais);

        Container container = this.componentManager.getInstance(Container.class);
        ServletResponse response = mock(ServletResponse.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(response.getHttpServletResponse()).thenReturn(httpResponse);
        when(container.getResponse()).thenReturn(response);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(baos);
        handler.setClassLoader(classLoader);

        handler.handle(reference, chain);

        // Verify that the resource content has been copied to the Response output stream.
        assertEquals("content", baos.toString());
        // Verify that the correct Content Type has been set.
        verify(response).setContentType("application/javascript");

        // Also verify that the "Last-Modified" header has been set in the response so that the browser will send
        // an If-Modified-Since header for the next request and we can tell it to use its cache.
        verify(httpResponse).setDateHeader(eq("Last-Modified"), anyLong());
    }

    @Test
    public void return304WhenIfModifiedSinceHeader() throws Exception
    {
        EntityResourceReference reference = new EntityResourceReference(new DocumentReference("wiki", "space", "page"),
            EntityResourceAction.VIEW);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        TestableWebJarsResourceReferenceHandler handler = this.componentManager.getComponentUnderTest();

        Container container = this.componentManager.getInstance(Container.class);

        ServletResponse response = mock(ServletResponse.class);
        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(response.getHttpServletResponse()).thenReturn(httpResponse);
        when(container.getResponse()).thenReturn(response);

        ServletRequest request = mock(ServletRequest.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getHeader("If-Modified-Since")).thenReturn("some value");
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(container.getRequest()).thenReturn(request);

        handler.handle(reference, chain);

        // This the test: we verify that 304 is returned when the "If-Modified-Since" header is found in the request
        verify(httpResponse).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
    }

}

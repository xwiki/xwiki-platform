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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.exception.VelocityException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.webjars.internal.TestableWebJarsResourceReferenceHandler;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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
        new MockitoComponentMockingRule<TestableWebJarsResourceReferenceHandler>(
            TestableWebJarsResourceReferenceHandler.class);

    private ServletRequest request;

    private ServletResponse response;

    private EntityResourceReference reference = new EntityResourceReference(new DocumentReference("wiki", "Space",
        "Page"), EntityResourceAction.VIEW);

    private ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);

    private TestableWebJarsResourceReferenceHandler handler;

    private ClassLoader classLoader = mock(ClassLoader.class);

    @Before
    public void configure() throws Exception
    {
        Container container = this.componentManager.getInstance(Container.class);

        this.response = mock(ServletResponse.class);
        ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
        when(this.response.getOutputStream()).thenReturn(responseOutputStream);

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(this.response.getHttpServletResponse()).thenReturn(httpResponse);
        when(container.getResponse()).thenReturn(this.response);

        this.request = mock(ServletRequest.class);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(this.request.getHttpServletRequest()).thenReturn(httpRequest);
        when(container.getRequest()).thenReturn(this.request);

        this.handler = this.componentManager.getComponentUnderTest();
        this.handler.setClassLoader(this.classLoader);
    }

    @Test
    public void executeWhenResourceDoesntExist() throws Exception
    {
        this.reference.addParameter("value", "angular/2.1.11/angular.js");

        this.handler.handle(this.reference, this.chain);

        verify(this.classLoader).getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js");
        verify(this.response.getHttpServletResponse())
            .sendError(404, "Resource not found [angular/2.1.11/angular.js].");
        verify(this.chain).handleNext(this.reference);
    }

    @Test
    public void executeWhenResourceExists() throws Exception
    {
        this.reference.addParameter("value", "angular/2.1.11/angular.js");

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        Long now = new Date().getTime();
        this.handler.handle(this.reference, this.chain);

        // Verify that the resource content has been copied to the Response output stream.
        assertEquals("content", this.response.getOutputStream().toString());
        // Verify that the correct Content Type has been set.
        verify(this.response).setContentType("application/javascript");

        // Verify that the static resource is cached permanently.
        verify(this.response.getHttpServletResponse()).setHeader("Cache-Control", "public");
        ArgumentCaptor<Long> expireDate = ArgumentCaptor.forClass(Long.class);
        verify(this.response.getHttpServletResponse()).setDateHeader(eq("Expires"), expireDate.capture());
        // The expiration date should be in one year from now.
        assertTrue(expireDate.getValue() >= (now + 365 * 24 * 3600 * 1000L));

        // Also verify that the "Last-Modified" header has been set in the response so that the browser will send
        // an If-Modified-Since header for the next request and we can tell it to use its cache.
        verify(this.response.getHttpServletResponse()).setDateHeader(eq("Last-Modified"), anyLong());

        verify(this.chain).handleNext(this.reference);
    }

    @Test
    public void return304WhenIfModifiedSinceHeader() throws Exception
    {
        when(this.request.getHttpServletRequest().getHeader("If-Modified-Since")).thenReturn("some value");

        this.handler.handle(this.reference, this.chain);

        // This the test: we verify that 304 is returned when the "If-Modified-Since" header is found in the request
        verify(this.response.getHttpServletResponse()).setStatus(304);

        verify(this.chain).handleNext(this.reference);
    }

    @Test
    public void evaluateResource() throws Exception
    {
        this.reference.addParameter("value", "angular/2.1.11/angular.js");
        this.reference.addParameter("evaluate", "true");

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(new Answer<Void>()
        {
            public Void answer(InvocationOnMock invocation)
            {
                ((StringWriter) invocation.getArguments()[1]).write("evaluated content");
                return null;
            }
        }).when(velocityEngine).evaluate(any(VelocityContext.class), any(StringWriter.class),
            eq("angular/2.1.11/angular.js"), any(Reader.class));

        this.handler.handle(this.reference, this.chain);

        // Verify that the resource content has been evaluated and copied to the Response output stream.
        assertEquals("evaluated content", this.response.getOutputStream().toString());

        // Verify that the correct Content Type has been set.
        verify(this.response).setContentType("application/javascript");

        // Verify that the dynamic resource is not cached.
        verify(this.response.getHttpServletResponse(), never()).setHeader(any(String.class), any(String.class));
        verify(this.response.getHttpServletResponse(), never()).setDateHeader(any(String.class), any(Long.class));
    }

    @Test
    public void failingResourceEvaluation() throws Exception
    {
        this.reference.addParameter("value", "angular/2.1.11/angular.js");
        this.reference.addParameter("evaluate", "true");

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        VelocityManager velocityManager = this.componentManager.getInstance(VelocityManager.class);
        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(velocityEngine.evaluate(any(VelocityContext.class), any(StringWriter.class),
            eq("angular/2.1.11/angular.js"), any(Reader.class))).thenThrow(new VelocityException("Bad code!"));

        this.handler.handle(this.reference, this.chain);

        // Verify the exception is logged.
        verify(this.componentManager.getMockedLogger()).error(
            eq("Faild to evaluate the Velocity code from WebJar resource [angular/2.1.11/angular.js]"),
            any(ResourceReferenceHandlerException.class));

        // Verify that the client is properly notified about the failure.
        verify(this.response.getHttpServletResponse()).sendError(500,
            "Faild to evaluate the Velocity code from WebJar resource [angular/2.1.11/angular.js]");

        // The next handlers are still called.
        verify(this.chain).handleNext(this.reference);
    }
}

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
package org.xwiki.webjars.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.velocity.exception.VelocityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.classloader.ClassLoaderManager;
import org.xwiki.classloader.NamespaceURLClassLoader;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import static ch.qos.logback.classic.Level.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Unit tests for {@link WebJarsResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class WebJarsResourceReferenceHandlerTest
{
    @InjectMockComponents
    private WebJarsResourceReferenceHandler handler;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private ClassLoaderManager classLoaderManager;

    @MockComponent
    private LESSCompiler lessCompiler;

    @MockComponent
    private Container container;

    @Mock
    private ServletRequest request;

    @Mock
    private ServletResponse response;

    @Mock
    private ResourceReferenceHandlerChain chain;

    @Mock
    private NamespaceURLClassLoader classLoader;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @BeforeEach
    void setUp() throws Exception
    {
        ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
        when(this.response.getOutputStream()).thenReturn(responseOutputStream);

        HttpServletResponse httpResponse = mock(HttpServletResponse.class);
        when(this.response.getHttpServletResponse()).thenReturn(httpResponse);
        when(this.container.getResponse()).thenReturn(this.response);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(this.request.getHttpServletRequest()).thenReturn(httpRequest);
        when(this.container.getRequest()).thenReturn(this.request);

        when(this.classLoaderManager.getURLClassLoader("wiki:wiki", true)).thenReturn(this.classLoader);
    }

    @Test
    void executeWhenResourceDoesntExist() throws Exception
    {
        WebJarsResourceReference reference =
            new WebJarsResourceReference("wiki:wiki", Arrays.asList("angular", "2.1.11", "angular.js"));

        this.handler.handle(reference, this.chain);

        verify(this.classLoader).getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js");
        verify(this.response.getHttpServletResponse())
            .sendError(404, "Resource not found [angular/2.1.11/angular.js].");
        verify(this.chain).handleNext(reference);
    }

    @Test
    void executeWhenResourceExists() throws Exception
    {
        WebJarsResourceReference reference =
            new WebJarsResourceReference("wiki:wiki", Arrays.asList("angular", "2.1.11", "angular.js"));

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        long now = new Date().getTime();
        this.handler.handle(reference, this.chain);

        assertEquals(1, this.handler.getSupportedResourceReferences().size());
        assertEquals(WebJarsResourceReference.TYPE, this.handler.getSupportedResourceReferences().get(0));

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

        verify(this.chain).handleNext(reference);
    }

    @Test
    void return304WhenIfModifiedSinceHeader() throws Exception
    {
        WebJarsResourceReference reference =
            new WebJarsResourceReference("wiki:wiki", Arrays.asList("angular", "2.1.11", "angular.js"));

        when(this.request.getHttpServletRequest().getHeader("If-Modified-Since")).thenReturn("some value");

        this.handler.handle(reference, this.chain);

        // This the test: we verify that 304 is returned when the "If-Modified-Since" header is found in the request
        verify(this.response.getHttpServletResponse()).setStatus(304);

        verify(this.chain).handleNext(reference);
    }

    @Test
    void evaluateResource() throws Exception
    {
        WebJarsResourceReference reference =
            new WebJarsResourceReference("wiki:wiki", Arrays.asList("angular", "2.1.11", "angular.js"));
        reference.addParameter("evaluate", true);

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                ((StringWriter) invocation.getArguments()[1]).write("evaluated content");
                return null;
            }
        }).when(velocityEngine).evaluate(any(), any(), eq("angular/2.1.11/angular.js"), any(Reader.class));

        this.handler.handle(reference, this.chain);

        // Verify that the resource content has been evaluated and copied to the Response output stream.
        assertEquals("evaluated content", this.response.getOutputStream().toString());

        // Verify that the correct Content Type has been set.
        verify(this.response).setContentType("application/javascript");

        // Verify that the dynamic resource is not cached.
        verify(this.response.getHttpServletResponse(), never()).setHeader(any(), any());
        verify(this.response.getHttpServletResponse(), never()).setDateHeader(any(), anyLong());
    }

    @Test
    void failingResourceEvaluation() throws Exception
    {
        WebJarsResourceReference reference =
            new WebJarsResourceReference("wiki:wiki", Arrays.asList("angular", "2.1.11", "angular.js"));
        reference.addParameter("evaluate", "true");

        ByteArrayInputStream resourceStream = new ByteArrayInputStream("content".getBytes());
        when(this.classLoader.getResourceAsStream("META-INF/resources/webjars/angular/2.1.11/angular.js")).thenReturn(
            resourceStream);

        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(velocityEngine);

        when(velocityEngine.evaluate(any(), any(), eq("angular/2.1.11/angular.js"), any(Reader.class)))
            .thenThrow(new VelocityException("Bad code!"));

        this.handler.handle(reference, this.chain);

        // Verify the exception is logged.
        assertEquals(1, this.logCapture.size());
        assertEquals(ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Failed to evaluate the Velocity code from WebJar resource [angular/2.1.11/angular.js]",
            this.logCapture.getMessage(0));

        // Verify that the client is properly notified about the failure.
        verify(this.response.getHttpServletResponse()).sendError(500,
            "Failed to evaluate the Velocity code from WebJar resource [angular/2.1.11/angular.js]");

        // The next handlers are still called.
        verify(this.chain).handleNext(reference);
    }

    @Test
    void filterResourceLessNoEvaluate() throws Exception
    {
        WebJarsResourceReference resourceReference =
            new WebJarsResourceReference("testNamespace", Arrays.asList("testdirectory", "testfile.less"));
        InputStream resourceStream = mock(InputStream.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        InputStream stream = this.handler.filterResource(resourceReference, resourceStream, httpServletResponse);
        assertSame(resourceStream, stream);
        verifyNoInteractions(resourceStream);
        verifyNoInteractions(httpServletResponse);
    }
    
    @Test
    void filterResourceLessAndEvaluate() throws Exception
    {
        WebJarsResourceReference resourceReference =
            new WebJarsResourceReference("testNamespace", Arrays.asList("testdirectory", "testfile.less"));
        resourceReference.addParameter("evaluate", "true");
        InputStream resourceStream = mock(InputStream.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        when(this.lessCompiler.compile(any(LESSResourceReference.class), eq(true), eq(false), eq(false)))
            .thenReturn("compiled less");
        
        
        InputStream stream = this.handler.filterResource(resourceReference, resourceStream, httpServletResponse);
        assertNotSame(resourceStream, stream);
        verifyNoInteractions(resourceStream);
        verify(httpServletResponse).setHeader(HttpHeaders.CONTENT_TYPE, "text/css");
        verify(this.lessCompiler).compile(any(LESSResourceReference.class), eq(true), eq(false), eq(false));
    }
}

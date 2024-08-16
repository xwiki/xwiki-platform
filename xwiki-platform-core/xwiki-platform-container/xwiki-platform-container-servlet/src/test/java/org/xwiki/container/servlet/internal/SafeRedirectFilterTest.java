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
package org.xwiki.container.servlet.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.servlet.filters.internal.SafeRedirectFilter;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.URLSecurityManager;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Validate {@link SafeRedirectFilter}.
 * 
 * @version $Id$
 */
@ComponentTest
class SafeRedirectFilterTest
{
    @MockComponent
    private URLSecurityManager urlSecurityManager;

    @InjectComponentManager
    private ComponentManager componentManager;

    private SafeRedirectFilter filter;

    private HttpServletResponse httpServletResponse;

    private HttpServletResponse safeServletResponse;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent
    void beforeComponent(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        mockitoComponentManager.registerComponent(ComponentManager.class, "context", mockitoComponentManager);
    }

    @BeforeEach
    void setup() throws IOException, ServletException
    {
        this.httpServletResponse = mock();

        this.filter = new SafeRedirectFilter();

        FilterConfig filterConfig = mock();
        ServletContext servletContext = mock();
        when(filterConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(ComponentManager.class.getName())).thenReturn(this.componentManager);
        this.filter.init(filterConfig);

        this.filter.doFilter(null, this.httpServletResponse, new FilterChain()
        {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException
            {
                safeServletResponse = (HttpServletResponse) response;
            }
        });
    }

    @Test
    void sendRedirect() throws IOException, URISyntaxException
    {
        this.safeServletResponse.sendRedirect("");
        verify(this.httpServletResponse, never()).sendRedirect(any());

        String location = "//xwiki.org/xwiki/something/";
        URI expectedURI = new URI("//xwiki.org/xwiki/something/");
        when(this.urlSecurityManager.parseToSafeURI(location)).thenReturn(expectedURI);
        this.safeServletResponse.sendRedirect(location);
        verify(this.httpServletResponse).sendRedirect(location);

        when(this.urlSecurityManager.parseToSafeURI(location)).thenThrow(new SecurityException("Unsafe location"));
        this.safeServletResponse.sendRedirect(location);
        assertEquals(1, this.logCapture.size());
        assertEquals(
            "Possible phishing attack, attempting to redirect to [//xwiki.org/xwiki/something/], this request"
                + " has been blocked. If the request was legitimate, please check the URL security configuration. "
                + "You might need to add the domain related to this request in the list of trusted domains in the "
                + "configuration: it can be configured in xwiki.properties in url.trustedDomains.",
            this.logCapture.getMessage(0));
    }
}

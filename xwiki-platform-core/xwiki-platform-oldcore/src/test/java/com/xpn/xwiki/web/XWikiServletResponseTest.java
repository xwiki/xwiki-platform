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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.URLSecurityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiServletResponse}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiServletResponseTest
{
    @MockComponent
    private URLSecurityManager urlSecurityManager;

    private XWikiServletResponse servletResponse;
    private HttpServletResponse httpServletResponse;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent
    void beforeComponent(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        mockitoComponentManager.registerComponent(ComponentManager.class, "context", mockitoComponentManager);
        Utils.setComponentManager(mockitoComponentManager);
    }

    @BeforeEach
    void setup()
    {
        this.httpServletResponse = mock(HttpServletResponse.class);
        this.servletResponse = new XWikiServletResponse(this.httpServletResponse);
    }

    @Test
    void sendRedirect() throws IOException, URISyntaxException
    {
        this.servletResponse.sendRedirect("");
        verify(this.httpServletResponse, never()).sendRedirect(any());

        String location = "//xwiki.org/xwiki/something/";
        URI expectedURI = new URI("//xwiki.org/xwiki/something/");
        when(this.urlSecurityManager.parseToSafeURI(location)).thenReturn(expectedURI);
        this.servletResponse.sendRedirect(location);
        verify(this.httpServletResponse).sendRedirect(location);

        when(this.urlSecurityManager.parseToSafeURI(location)).thenThrow(new SecurityException("Unsafe location"));
        this.servletResponse.sendRedirect(location);
        assertEquals(1, this.logCapture.size());
        assertEquals("Possible phishing attack, attempting to redirect to [//xwiki.org/xwiki/something/], this request"
                + " has been blocked. If the request was legitimate, please check the URL security configuration. "
                + "You might need to add the domain related to this request in the list of trusted domains in the "
                + "configuration: it can be configured in xwiki.properties in url.trustedDomains.",
            this.logCapture.getMessage(0));
    }
}

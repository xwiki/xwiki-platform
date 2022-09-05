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
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.ecs.wml.U;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.URLSecurityManager;

import static org.junit.jupiter.api.Assertions.*;
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
    void sendRedirect() throws IOException
    {
        this.servletResponse.sendRedirect("");
        verify(this.httpServletResponse, never()).sendRedirect(any());

        this.servletResponse.sendRedirect("/xwiki/\n/something/");
        verify(this.httpServletResponse, never()).sendRedirect(any());

        this.servletResponse.sendRedirect("//xwiki.org/xwiki/something/");
        verify(this.httpServletResponse, never()).sendRedirect(any());

        String redirect = "http://xwiki.org/xwiki/something/";
        URL redirectUrl = new URL(redirect);
        when(this.urlSecurityManager.isDomainTrusted(redirectUrl)).thenReturn(false);
        this.servletResponse.sendRedirect(redirect);
        verify(this.httpServletResponse, never()).sendRedirect(any());
        verify(this.urlSecurityManager).isDomainTrusted(redirectUrl);

        redirect = "http:/xwiki.com/xwiki/something/";
        redirectUrl = new URL(redirect);
        when(this.urlSecurityManager.isDomainTrusted(redirectUrl)).thenReturn(false);
        this.servletResponse.sendRedirect(redirect);
        verify(this.httpServletResponse, never()).sendRedirect(any());
        verify(this.urlSecurityManager).isDomainTrusted(redirectUrl);

        redirect = "https://floo";
        redirectUrl = new URL(redirect);
        when(this.urlSecurityManager.isDomainTrusted(redirectUrl)).thenReturn(false);
        this.servletResponse.sendRedirect(redirect);
        verify(this.httpServletResponse, never()).sendRedirect(any());
        verify(this.urlSecurityManager).isDomainTrusted(redirectUrl);

        redirect = "ftp://xwiki.org/xwiki/something/";
        redirectUrl = new URL(redirect);
        when(this.urlSecurityManager.isDomainTrusted(redirectUrl)).thenReturn(false);
        this.servletResponse.sendRedirect(redirect);
        verify(this.httpServletResponse, never()).sendRedirect(any());
        verify(this.urlSecurityManager).isDomainTrusted(redirectUrl);

        this.servletResponse.sendRedirect("/xwiki/something/");
        verify(this.httpServletResponse).sendRedirect("/xwiki/something/");

        redirect = "http://xwiki.org/foo/";
        redirectUrl = new URL(redirect);
        when(this.urlSecurityManager.isDomainTrusted(redirectUrl)).thenReturn(true);
        this.servletResponse.sendRedirect(redirect);
        verify(this.httpServletResponse).sendRedirect(redirect);
        verify(this.urlSecurityManager).isDomainTrusted(redirectUrl);
    }
}
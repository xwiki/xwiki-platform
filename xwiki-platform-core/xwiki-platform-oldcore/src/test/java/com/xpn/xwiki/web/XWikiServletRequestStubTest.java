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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Validate {@link XWikiServletRequestStub}.
 * 
 * @version $Id$
 */
class XWikiServletRequestStubTest
{
    @Test
    void copy() throws Exception
    {
        XWikiServletRequestStub request = createRequest();
        request.setDaemon(false);

        XWikiServletRequestStub copiedRequest = new XWikiServletRequestStub(request);

        assertEquals(request.getRequestURL().toString(), copiedRequest.getRequestURL().toString());
        assertEquals(request.getContextPath(), copiedRequest.getContextPath());
        assertEquals(request.getParameter("language"), copiedRequest.getParameter("language"));
        assertArrayEquals(request.getParameterValues("page"), copiedRequest.getParameterValues("page"));
        assertEquals(Collections.list(request.getHeaderNames()), Collections.list(copiedRequest.getHeaderNames()));
        assertEquals(request.getHeader("user-agent"), copiedRequest.getHeader("usER-aGent"));
        assertEquals(Collections.list(request.getHeaders("aCCept")),
            Collections.list(copiedRequest.getHeaders("AccepT")));
        assertEquals(request.getCookie("color").getValue(), copiedRequest.getCookie("color").getValue());
        assertEquals(request.getRemoteAddr(), copiedRequest.getRemoteAddr());
        assertFalse(copiedRequest.isDaemon());
    }

    @Test
    void builder() throws Exception
    {
        XWikiServletRequestStub request = createRequest();
        assertEquals("https://xwiki.org/test/path", request.getRequestURL().toString());
        assertEquals("/test", request.getContextPath());
        assertEquals("en", request.getParameter("language"));
        assertArrayEquals(new String[] {"Some.Page", "Other.Page"}, request.getParameterValues("page"));
        assertEquals(Arrays.asList("Accept", "User-Agent"), Collections.list(request.getHeaderNames()));
        assertEquals("Firefox", request.getHeader("usER-aGent"));
        assertEquals(Arrays.asList("text/html", "application/xhtml+xml"),
            Collections.list(request.getHeaders("AccepT")));
        assertEquals("master", request.getCookie("level").getValue());
        assertEquals("172.12.0.3", request.getRemoteAddr());
    }

    private XWikiServletRequestStub createRequest() throws Exception
    {
        Map<String, String[]> parameters = new HashMap<>();
        parameters.put("language", new String[] {"en"});
        parameters.put("page", new String[] {"Some.Page", "Other.Page"});

        Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("Firefox"));
        headers.put("Accept", Arrays.asList("text/html", "application/xhtml+xml"));

        Cookie[] cookies = new Cookie[] {new Cookie("color", "red"), new Cookie("level", "master")};

        return new XWikiServletRequestStub.Builder().setRequestURL(new URL("https://xwiki.org/test/path"))
            .setContextPath("/test").setRequestParameters(parameters).setHeaders(headers).setCookies(cookies)
            .setRemoteAddr("172.12.0.3").build();
    }
}

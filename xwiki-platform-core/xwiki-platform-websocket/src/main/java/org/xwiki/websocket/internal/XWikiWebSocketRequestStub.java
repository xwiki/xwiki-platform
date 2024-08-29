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
package org.xwiki.websocket.internal;

import java.net.HttpCookie;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Adapts a {@link HandshakeRequest} to {@link XWikiRequest}.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
public class XWikiWebSocketRequestStub extends XWikiServletRequestStub
{
    private final HandshakeRequest request;

    /**
     * Creates a new XWiki request that wraps the given WebSocket handshake request.
     * 
     * @param request the WebSocket handshake request to wrap
     */
    public XWikiWebSocketRequestStub(HandshakeRequest request)
    {
        super(buildFromHandshakeRequest(request));

        this.request = request;
    }

    private static Builder buildFromHandshakeRequest(HandshakeRequest request)
    {
        Map<String, List<String>> headers = getHeaders(request);
        Optional<String> cookieHeader = headers.getOrDefault("Cookie", Collections.emptyList()).stream().findFirst();
        return new Builder().setRequestParameters(adaptParameterMap(request.getParameterMap()))
            .setCookies(parseCookies(cookieHeader)).setHeaders(headers)
            // The WebSocket API (JSR-356) doesn't expose the client IP address but at least we can avoid a null pointer
            // exception.
            .setRemoteAddr("");
    }

    private static Map<String, List<String>> getHeaders(HandshakeRequest request)
    {
        // Make sure header names are matched case insensitive.
        Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        headers.putAll(request.getHeaders());
        return headers;
    }

    private static Cookie[] parseCookies(Optional<String> cookieHeader)
    {
        if (cookieHeader.isEmpty()) {
            return new Cookie[0];
        } else {
            String[] cookieStrings = cookieHeader.get().split("\\s*;\\s*");
            return Arrays.asList(cookieStrings).stream().map(HttpCookie::parse).flatMap(Collection::stream)
                .map(cookie -> new Cookie(cookie.getName(), cookie.getValue())).toArray(size -> new Cookie[size]);
        }
    }

    @Override
    public long getDateHeader(String name)
    {
        String value = getHeader(name);
        if (value != null) {
            return ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();
        } else {
            return -1;
        }
    }

    @Override
    public String getMethod()
    {
        return "GET";
    }

    @Override
    public String getRequestURI()
    {
        return this.request.getRequestURI().toString();
    }

    private static Map<String, String[]> adaptParameterMap(Map<String, List<String>> params)
    {
        Map<String, String[]> parameters = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            parameters.put(entry.getKey(), entry.getValue().toArray(new String[] {}));
        }
        return parameters;
    }

    @Override
    public HttpSession getSession()
    {
        return getSession(true);
    }

    @Override
    public HttpSession getSession(boolean create)
    {
        return (HttpSession) this.request.getHttpSession();
    }

    @Override
    public String getServletPath()
    {
        return "";
    }

    @Override
    public String getPathInfo()
    {
        return this.request.getRequestURI().getPath();
    }

    @Override
    public String getScheme()
    {
        return this.request.getRequestURI().getScheme();
    }

    @Override
    public String getQueryString()
    {
        return this.request.getQueryString();
    }

    @Override
    public Principal getUserPrincipal()
    {
        return this.request.getUserPrincipal();
    }

    @Override
    public boolean isUserInRole(String role)
    {
        return this.request.isUserInRole(role);
    }
}

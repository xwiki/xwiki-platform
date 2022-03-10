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
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;

import org.apache.commons.lang3.StringUtils;

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

    private final Map<String, Cookie> cookies;

    /**
     * Creates a new XWiki request that wraps the given WebSocket handshake request.
     * 
     * @param request the WebSocket handshake request to wrap
     */
    public XWikiWebSocketRequestStub(HandshakeRequest request)
    {
        super(null, adaptParameterMap(request.getParameterMap()));

        this.request = request;
        this.cookies = parseCookies();
    }

    @Override
    public String getHeader(String name)
    {
        List<String> values = getHeaderValues(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    private List<String> getHeaderValues(String name)
    {
        for (Map.Entry<String, List<String>> entry : this.request.getHeaders().entrySet()) {
            if (StringUtils.equalsIgnoreCase(name, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return Collections.enumeration(this.request.getHeaders().keySet());
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        List<String> values = getHeaderValues(name);
        return values != null ? Collections.enumeration(values) : Collections.emptyEnumeration();
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
    public int getIntHeader(String name)
    {
        String value = getHeader(name);
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return -1;
        }
    }

    @Override
    public Cookie getCookie(String cookieName)
    {
        return this.cookies.get(cookieName);
    }

    @Override
    public Cookie[] getCookies()
    {
        return this.cookies.values().toArray(new Cookie[] {});
    }

    private Map<String, Cookie> parseCookies()
    {
        String cookieHeader = getHeader("Cookie");
        if (cookieHeader == null) {
            return Collections.emptyMap();
        } else {
            String[] cookieStrings = cookieHeader.split("\\s*;\\s*");
            return Arrays.asList(cookieStrings).stream().map(HttpCookie::parse).flatMap(Collection::stream)
                .map(cookie -> new Cookie(cookie.getName(), cookie.getValue()))
                .collect(Collectors.toMap(Cookie::getName, Function.identity()));
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

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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.Cookie;
import javax.websocket.HandshakeResponse;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletResponseStub;

/**
 * Adapts a {@link HandshakeResponse} to {@link XWikiResponse}.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
public class XWikiWebSocketResponseStub extends XWikiServletResponseStub
{
    private final HandshakeResponse response;

    /**
     * Creates a new XWiki response that wraps the given WebSocket handshake response.
     * 
     * @param response the WebSocket handshake response to wrap
     */
    public XWikiWebSocketResponseStub(HandshakeResponse response)
    {
        this.response = response;
    }

    @Override
    public void addHeader(String name, String value)
    {
        List<String> values = getHeaderValues(name);
        if (values == null) {
            values = new ArrayList<>();
            this.response.getHeaders().put(name, values);
        }
        values.add(value);
    }

    @Override
    public boolean containsHeader(String name)
    {
        List<String> values = getHeaderValues(name);
        return values != null && !values.isEmpty();
    }

    @Override
    public String getHeader(String name)
    {
        List<String> values = getHeaderValues(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    private List<String> getHeaderValues(String name)
    {
        for (Map.Entry<String, List<String>> entry : this.response.getHeaders().entrySet()) {
            if (StringUtils.equalsIgnoreCase(name, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public Collection<String> getHeaders(String name)
    {
        List<String> values = getHeaderValues(name);
        return values != null ? new ArrayList<>(values) : Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames()
    {
        return new LinkedHashSet<>(this.response.getHeaders().keySet());
    }

    @Override
    public void setHeader(String name, String value)
    {
        Set<String> namesToRemove = this.response.getHeaders().keySet().stream()
            .filter(headerName -> StringUtils.equalsIgnoreCase(name, headerName)).collect(Collectors.toSet());
        this.response.getHeaders().keySet().removeAll(namesToRemove);
        this.response.getHeaders().put(name, new ArrayList<>(Arrays.asList(value)));
    }

    @Override
    public void setDateHeader(String name, long date)
    {
        setHeader(name, toDateString(date));
    }

    @Override
    public void setIntHeader(String name, int value)
    {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addDateHeader(String name, long date)
    {
        addHeader(name, toDateString(date));
    }

    private String toDateString(long date)
    {
        return DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneId.of("GMT")).format(Instant.ofEpochMilli(date));
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        addHeader(name, String.valueOf(value));
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        StringBuilder header = new StringBuilder();

        header.append(cookie.getName()).append("=\"").append(cookie.getValue()).append('"');
        if (cookie.getDomain() != null) {
            header.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getPath() != null) {
            header.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getMaxAge() >= 0) {
            header.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.isHttpOnly()) {
            header.append("; HttpOnly");
        }
        if (cookie.getSecure()) {
            header.append("; Secure");
        }

        addHeader("Set-Cookie", header.toString());
    }

    @Override
    public void removeCookie(String cookieName, XWikiRequest request)
    {
        Cookie cookie = request.getCookie(cookieName);
        if (cookie != null) {
            cookie.setMaxAge(0);
            cookie.setPath(cookie.getPath());
            addCookie(cookie);
        }
    }
}

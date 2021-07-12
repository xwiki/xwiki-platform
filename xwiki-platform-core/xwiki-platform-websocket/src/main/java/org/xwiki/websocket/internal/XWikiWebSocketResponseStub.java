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
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
 * @since 13.6RC1
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
        List<String> values = this.response.getHeaders().getOrDefault(name, new ArrayList<>());
        values.add(value);
        this.response.getHeaders().put(value, values);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return this.response.getHeaders().containsKey(name);
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
        return new HashSet<>(this.response.getHeaders().keySet());
    }

    @Override
    public void setHeader(String name, String value)
    {
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
        return DateTimeFormatter.RFC_1123_DATE_TIME.format(Instant.ofEpochMilli(date));
    }

    @Override
    public void addIntHeader(String name, int value)
    {
        addHeader(name, String.valueOf(value));
    }

    @Override
    public void addCookie(Cookie cookie)
    {
        HttpCookie httpCookie = new HttpCookie(cookie.getName(), cookie.getValue());
        httpCookie.setComment(cookie.getComment());
        httpCookie.setDomain(cookie.getDomain());
        httpCookie.setHttpOnly(cookie.isHttpOnly());
        httpCookie.setMaxAge(cookie.getMaxAge());
        httpCookie.setPath(cookie.getPath());
        httpCookie.setSecure(cookie.getSecure());
        httpCookie.setVersion(cookie.getVersion());
        addHeader("Set-Cookie", httpCookie.toString());
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

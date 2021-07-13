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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.websocket.HandshakeResponse;

import org.junit.jupiter.api.Test;

import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiWebSocketResponseStub}.
 * 
 * @version $Id$
 */
class XWikiWebSocketResponseStubTest
{
    @Test
    void verifyStub() throws Exception
    {
        Map<String, List<String>> headers = new LinkedHashMap<>();

        HandshakeResponse handshakeResponse = mock(HandshakeResponse.class);
        when(handshakeResponse.getHeaders()).thenReturn(headers);

        XWikiWebSocketResponseStub stub = new XWikiWebSocketResponseStub(handshakeResponse);

        stub.addCookie(new Cookie("foo", "bar"));

        XWikiRequest request = mock(XWikiRequest.class);
        Cookie cookie = new Cookie("bar", "abc");
        cookie.setMaxAge(3600);
        cookie.setPath("/xwiki/websocket");
        when(request.getCookie("bar")).thenReturn(cookie);
        stub.removeCookie("bar", request);

        stub.addDateHeader("Date", 1212491130000L);
        stub.addHeader("Content-Type", "application/json");
        stub.addIntHeader("Size", 123);

        assertEquals("Tue, 3 Jun 2008 11:05:30 GMT", stub.getHeader("Date"));
        assertEquals(new LinkedHashSet<String>(Arrays.asList("Set-Cookie", "Date", "Content-Type", "Size")),
            stub.getHeaderNames());
        assertEquals(Arrays.asList("foo=bar", "bar=abc"), stub.getHeaders("Set-Cookie"));

        assertTrue(stub.containsHeader("Date"));
        assertFalse(stub.containsHeader("Age"));
    }
}

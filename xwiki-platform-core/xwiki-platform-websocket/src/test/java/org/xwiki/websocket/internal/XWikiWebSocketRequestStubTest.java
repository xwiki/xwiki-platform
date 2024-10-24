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

import java.net.URI;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;
import jakarta.websocket.server.HandshakeRequest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiWebSocketRequestStub}.
 * 
 * @version $Id$
 */
class XWikiWebSocketRequestStubTest
{
    @Test
    void verifyStub() throws Exception
    {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        // Note that we duplicate the JSESSIONID in order to check that it doesn't fail the cookie parsing.
        headers.put("Cookie", Collections.singletonList("JSESSIONID=abc; username=\"foo\";"
            + " password=\"bar\"; rememberme=\"false\"; JSESSIONID=abc; validation=\"xyz\""));
        headers.put("Date", Collections.singletonList("Tue, 3 Jun 2008 11:05:30 GMT"));
        headers.put("Int", Arrays.asList("12", "31"));

        HandshakeRequest handshakeRequest = mock(HandshakeRequest.class);
        when(handshakeRequest.getHeaders()).thenReturn(headers);

        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("color", Arrays.asList("red", "blue"));
        params.put("age", Collections.singletonList("23"));
        when(handshakeRequest.getParameterMap()).thenReturn(params);

        HttpSession session = mock(HttpSession.class);
        when(handshakeRequest.getHttpSession()).thenReturn(session);

        when(handshakeRequest.getQueryString()).thenReturn("?color=red&age=23");
        when(handshakeRequest.getRequestURI()).thenReturn(new URI("wss://www.xwiki.org/xwiki/websocket/echo"));

        Principal userPrincipal = mock(Principal.class);
        when(handshakeRequest.getUserPrincipal()).thenReturn(userPrincipal);

        XWikiWebSocketRequestStub stub = new XWikiWebSocketRequestStub(handshakeRequest);

        assertEquals("red", stub.getParameter("color"));
        assertArrayEquals(new String[] {"red", "blue"}, stub.getParameterValues("color"));

        assertEquals("xyz", stub.getCookie("validation").getValue());
        assertEquals(6, stub.getCookies().length);

        assertEquals(1212491130000L, stub.getDateHeader("daTe"));
        assertEquals(-1, stub.getDateHeader("missing"));
        assertEquals(12, stub.getIntHeader("iNt"));
        assertEquals(-1, stub.getIntHeader("Missing"));
        assertEquals(2, Collections.list(stub.getHeaders("inT")).size());
        assertEquals(Arrays.asList("Cookie", "Date", "Int"), Collections.list(stub.getHeaderNames()));

        assertEquals("GET", stub.getMethod());
        assertEquals("/xwiki/websocket/echo", stub.getPathInfo());
        assertEquals("?color=red&age=23", stub.getQueryString());
        assertEquals("wss://www.xwiki.org/xwiki/websocket/echo", stub.getRequestURI());
        assertEquals("wss", stub.getScheme());
        assertEquals("", stub.getServletPath());
        assertSame(session, stub.getSession());
        assertSame(userPrincipal, stub.getUserPrincipal());

        when(handshakeRequest.isUserInRole("tester")).thenReturn(true);
        assertFalse(stub.isUserInRole("developer"));
        assertTrue(stub.isUserInRole("tester"));
    }
}

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
package com.xpn.xwiki.internal.render;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.render.ScriptHttpSession;
import com.xpn.xwiki.render.ScriptXWikiServletRequest;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ScriptXWikiServletRequest}.
 * 
 * @version $Id$
 */
public class ScriptXWikiServletRequestTest
{
    private final ContextualAuthorizationManager authorization = mock(ContextualAuthorizationManager.class);

    private final XWikiRequest request = mock(XWikiRequest.class);

    private final HttpSession session = mock(HttpSession.class);

    private final ServletContext servletContext = mock(ServletContext.class);

    private final Map<String, Object> sessionMap = new HashMap<>();

    private ScriptXWikiServletRequest scriptRequest;

    @BeforeEach
    void beforeEach()
    {
        when(this.request.getSession()).thenReturn(this.session);
        when(this.request.getSession(anyBoolean())).thenReturn(this.session);
        when(this.request.getServletContext()).thenReturn(this.servletContext);
        when(this.request.getHttpServletRequest()).thenReturn(this.request);
        when(this.session.getServletContext()).thenReturn(this.servletContext);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                sessionMap.put(invocation.getArgument(0), invocation.getArgument(1));

                return null;
            }
        }).when(this.session).setAttribute(anyString(), any());
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return sessionMap.get(invocation.getArgument(0));
            }
        }).when(this.session).getAttribute(anyString());
        doAnswer(new Answer<Enumeration<String>>()
        {
            @Override
            public Enumeration<String> answer(InvocationOnMock invocation) throws Throwable
            {
                return Collections.enumeration(sessionMap.keySet());
            }
        }).when(this.session).getAttributeNames();

        this.scriptRequest = new ScriptXWikiServletRequest(this.request, this.authorization);
    }

    private void setProgramingRight(boolean allowed)
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(allowed);
    }

    private <T> void assertEqualsSet(Collection<T> expect, Enumeration<T> actual)
    {
        assertEquals(new HashSet<>(expect), new HashSet<>(Collections.list(actual)));
    }

    // Tests

    @Test
    void getServletContext()
    {
        setProgramingRight(true);

        assertSame(this.servletContext, this.scriptRequest.getServletContext());

        setProgramingRight(false);

        assertNull(this.scriptRequest.getServletContext());
    }

    @Test
    void getHttpServletRequest()
    {
        setProgramingRight(true);

        assertSame(this.request, this.scriptRequest.getHttpServletRequest());

        setProgramingRight(false);

        assertSame(this.scriptRequest, this.scriptRequest.getHttpServletRequest());
    }

    @Test
    void getRequest()
    {
        setProgramingRight(true);

        assertSame(this.request, this.scriptRequest.getRequest());

        setProgramingRight(false);

        assertNull(this.scriptRequest.getRequest());
    }

    @Test
    void sessionGetServletContext()
    {
        ScriptHttpSession scriptSession = (ScriptHttpSession) this.scriptRequest.getSession();

        setProgramingRight(true);

        assertSame(this.servletContext, scriptSession.getServletContext());

        setProgramingRight(false);

        assertNull(scriptSession.getServletContext());
    }

    @Test
    void sessionInvalidate()
    {
        ScriptHttpSession scriptSession = (ScriptHttpSession) this.scriptRequest.getSession();

        setProgramingRight(false);

        scriptSession.invalidate();

        verifyNoInteractions(this.session);

        setProgramingRight(true);

        scriptSession.invalidate();

        verify(this.session).invalidate();
    }

    @Test
    void sessionAttributes()
    {
        ScriptHttpSession scriptSession = (ScriptHttpSession) this.scriptRequest.getSession();

        setProgramingRight(false);

        assertNull(scriptSession.getAttribute("name"));

        scriptSession.setAttribute("name", "value");

        assertEquals("value", scriptSession.getAttribute("name"));
        assertEquals("value", scriptSession.getSafeAttribute("name"));

        scriptSession.setSafeAttribute("safename", "safevalue");

        assertEquals("safevalue", scriptSession.getAttribute("safename"));
        assertEquals("safevalue", scriptSession.getSafeAttribute("safename"));

        scriptSession.removeAttribute("safename");

        assertNull(scriptSession.getAttribute("safename"));
        assertNull(scriptSession.getSafeAttribute("safename"));

        setProgramingRight(true);

        assertNull(scriptSession.getAttribute("name"));
        assertEquals("value", scriptSession.getSafeAttribute("name"));

        scriptSession.setAttribute("name", "unsafevalue");

        assertEquals("unsafevalue", scriptSession.getAttribute("name"));
        assertEquals("value", scriptSession.getSafeAttribute("name"));

        assertEqualsSet(Arrays.asList("name", ScriptHttpSession.class.getName()), scriptSession.getAttributeNames());

        setProgramingRight(false);

        assertEquals("value", scriptSession.getAttribute("name"));
        assertEquals("value", scriptSession.getSafeAttribute("name"));

        assertEqualsSet(Arrays.asList("name"), scriptSession.getAttributeNames());
    }

    @Test
    void getRequestURL()
    {
        StringBuffer buffer = new StringBuffer();
        when(this.request.getRequestURL()).thenReturn(buffer);

        assertSame(buffer, this.scriptRequest.getRequestURL());
    }
}

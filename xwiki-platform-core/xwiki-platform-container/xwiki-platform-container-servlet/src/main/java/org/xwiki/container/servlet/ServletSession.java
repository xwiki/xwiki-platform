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
package org.xwiki.container.servlet;

import java.util.Enumeration;

import jakarta.servlet.http.HttpSession;

import org.xwiki.container.Request;
import org.xwiki.container.Session;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.stability.Unstable;

/**
 * This is the implementation of {@link Request} for {@link HttpSession}.
 * 
 * @version $Id$
 */
public class ServletSession implements Session
{
    private final HttpSession httpSession;

    /**
     * @param request the servlet request
     * @deprecated use {@link #ServletSession(HttpSession)} instead
     */
    @Deprecated(since = "17.0.0RC1")
    public ServletSession(javax.servlet.http.HttpServletRequest request)
    {
        this(JakartaServletBridge.toJakarta(request.getSession(true)));
    }

    /**
     * @param session the Servlet session
     * @since 17.0.0RC1
     */
    @Unstable
    public ServletSession(HttpSession session)
    {
        this.httpSession = session;
    }

    /**
     * @return the current Servlet session
     * @deprecated use {@link #getSession()} instead
     */
    @Deprecated(since = "17.0.0RC1")
    public javax.servlet.http.HttpSession getHttpSession()
    {
        return JakartaServletBridge.toJavax(this.httpSession);
    }

    /**
     * @return the current Servlet session
     * @since 17.0.0RC1
     */
    @Unstable
    public HttpSession getSession()
    {
        return this.httpSession;
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.httpSession.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.httpSession.getAttributeNames();
    }

    @Override
    public void removeAttribute(String name)
    {
        this.httpSession.removeAttribute(name);
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        this.httpSession.setAttribute(name, value);
    }
}

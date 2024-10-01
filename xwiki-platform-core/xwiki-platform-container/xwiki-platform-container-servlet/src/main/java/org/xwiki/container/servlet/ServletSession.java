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

import org.xwiki.container.Session;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.stability.Unstable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class ServletSession implements Session
{
    private HttpSession httpSession;

    /**
     * @param request the servlet request
     * @deprecated use {@link #ServletSession(HttpServletRequest)} instead
     */
    @Deprecated(since = "42.0.0")
    public ServletSession(javax.servlet.http.HttpServletRequest request)
    {
        this(JakartaServletBridge.toJakarta(request));
    }

    /**
     * @param request the servlet request
     * @since 42.0.0
     */
    @Unstable
    public ServletSession(HttpServletRequest request)
    {
        this.httpSession = request.getSession(true);
    }

    /**
     * @deprecated use {@link #getJakartaHttpSession()} instead
     */
    @Deprecated(since = "42.0.0")
    public javax.servlet.http.HttpSession getHttpSession()
    {
        return JakartaServletBridge.toJavax(this.httpSession);
    }

    /**
     * @return the current servlet session
     * @since 42.0.0
     */
    @Unstable
    public HttpSession getJakartaHttpSession()
    {
        return this.httpSession;
    }
}

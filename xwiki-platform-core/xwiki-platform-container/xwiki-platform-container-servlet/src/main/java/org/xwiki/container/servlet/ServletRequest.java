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

import org.xwiki.container.Request;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.user.UserReference;

import jakarta.servlet.http.HttpServletRequest;

/**
 * This is the implementation of {@link Request} for {@link HttpServletRequest}.
 *
 * @version $Id$
 */
public class ServletRequest implements Request
{
    private final HttpServletRequest jakartaHttpServletRequest;

    private javax.servlet.http.HttpServletRequest javaxHttpServletRequest;

    /**
     * @param jakartaHttpServletRequest the standard Jakarta {@link HttpServletRequest} instance
     * @since 42.0.0
     */
    public ServletRequest(HttpServletRequest jakartaHttpServletRequest)
    {
        this.jakartaHttpServletRequest = jakartaHttpServletRequest;
    }

    /**
     * @param javaxHttpServletRequest the legacy Javax {@link javax.servlet.http.HttpServletRequest} instance
     * @deprecated use {@link #ServletRequest(HttpServletRequest)} instead
     */
    @Deprecated(since = "42.0.0")
    public ServletRequest(javax.servlet.http.HttpServletRequest javaxHttpServletRequest)
    {
        this.javaxHttpServletRequest = javaxHttpServletRequest;
        this.jakartaHttpServletRequest = JakartaServletBridge.toJakarta(javaxHttpServletRequest);
    }

    /**
     * @return the standard Jakarta {@link HttpServletRequest} instance
     * @since 42.0.0
     */
    public HttpServletRequest getRequest()
    {
        return this.jakartaHttpServletRequest;
    }

    /**
     * @return the legacy Javax {@link javax.servlet.http.HttpServletRequest} instance
     * @deprecated use {@link #getRequest()} instead
     */
    @Deprecated(since = "42.0.0")
    public javax.servlet.http.HttpServletRequest getHttpServletRequest()
    {
        if (this.javaxHttpServletRequest == null) {
            this.javaxHttpServletRequest = JakartaServletBridge.toJavax(this.jakartaHttpServletRequest);
        }

        return this.javaxHttpServletRequest;
    }

    @Override
    public Object getParameter(String key)
    {
        return this.jakartaHttpServletRequest.getParameter(key);
    }

    @Override
    public String[] getParameterValues(String name)
    {
        return this.jakartaHttpServletRequest.getParameterValues(name);
    }

    @Override
    public Enumeration<String> getParameterNames()
    {
        return this.jakartaHttpServletRequest.getParameterNames();
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.jakartaHttpServletRequest.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return this.jakartaHttpServletRequest.getAttributeNames();
    }

    @Override
    public void setAttribute(String name, Object o)
    {
        this.jakartaHttpServletRequest.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name)
    {
        this.jakartaHttpServletRequest.removeAttribute(name);
    }

    @Override
    public UserReference getEffectiveAuthor()
    {
        return (UserReference) getAttribute(ATTRIBUTE_EFFECTIVE_AUTHOR);
    }
}

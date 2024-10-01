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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.xwiki.container.Request;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

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
    public HttpServletRequest getJakartaHttpServletRequest()
    {
        return this.jakartaHttpServletRequest;
    }

    /**
     * @return the legacy Javax {@link javax.servlet.http.HttpServletRequest} instance
     * @deprecated use {@link #getJakartaHttpServletRequest()} instead
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
    public Object getProperty(String key)
    {
        Object result;

        // Look first in the Query Parameters and then in the Query Attributes
        result = this.jakartaHttpServletRequest.getParameter(key);
        if (result == null) {
            result = this.jakartaHttpServletRequest.getAttribute(key);
        }

        return result;
    }

    @Override
    public List<Object> getProperties(String key)
    {
        List<Object> result = new ArrayList<>();

        // Look first in the Query Parameters and then in the Query Attributes
        Object[] requestParameters = this.jakartaHttpServletRequest.getParameterValues(key);
        if (requestParameters != null) {
            result.addAll(Arrays.asList(requestParameters));
        }
        Object attributeValue = this.jakartaHttpServletRequest.getAttribute(key);
        if (attributeValue != null) {
            result.add(attributeValue);
        }

        return result;
    }

    @Override
    public void setProperty(String key, Object value)
    {
        this.jakartaHttpServletRequest.setAttribute(key, value);
    }

    @Override
    public void removeProperty(String key)
    {
        this.jakartaHttpServletRequest.removeAttribute(key);
    }
}

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

import java.io.IOException;
import java.io.OutputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.xwiki.container.RedirectResponse;
import org.xwiki.container.Response;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

/**
 * This is the implementation of {@link Response} for {@link HttpServletResponse}.
 *
 * @version $Id$
 */
public class ServletResponse implements RedirectResponse
{
    private final HttpServletResponse jakartaHttpServletResponse;

    /**
     * @param jakartaHttpServletResponse the standard Jakarta {@link HttpServletResponse} instance
     * @since 42.0.0
     */
    public ServletResponse(HttpServletResponse jakartaHttpServletResponse)
    {
        this.jakartaHttpServletResponse = jakartaHttpServletResponse;
    }

    /**
     * @param javaxHttpServletResponse the legacy Javax {@link javax.servlet.http.HttpServletResponse} instance
     * @deprecated use {@link #ServletResponse(HttpServletResponse)} instead
     */
    @Deprecated(since = "42.0.0")
    public ServletResponse(javax.servlet.http.HttpServletResponse javaxHttpServletResponse)
    {
        this.jakartaHttpServletResponse = JakartaServletBridge.toJakarta(javaxHttpServletResponse);
    }

    /**
     * @return the standard Jakarta {@link HttpServletResponse} instance
     * @since 42.0.0
     */
    public HttpServletResponse getResponse()
    {
        return this.jakartaHttpServletResponse;
    }

    /**
     * @return the legacy Javax {@link javax.servlet.http.HttpServletResponse} instance
     * @deprecated use {@link #getResponse()} instead
     */
    @Deprecated(since = "42.0.0")
    public javax.servlet.http.HttpServletResponse getHttpServletResponse()
    {
        return JakartaServletBridge.toJavax(this.jakartaHttpServletResponse);
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        try {
            return this.jakartaHttpServletResponse.getOutputStream();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    @Override
    public void setContentLength(int length)
    {
        this.jakartaHttpServletResponse.setContentLength(length);
    }

    @Override
    public void setContentType(String mimeType)
    {
        this.jakartaHttpServletResponse.setContentType(mimeType);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        this.jakartaHttpServletResponse.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc)
    {
        this.jakartaHttpServletResponse.setStatus(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        this.jakartaHttpServletResponse.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        this.jakartaHttpServletResponse.sendRedirect(null);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return this.jakartaHttpServletResponse.containsHeader(name);
    }

    @Override
    public void setHeader(String name, String value)
    {
        this.jakartaHttpServletResponse.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value)
    {
        this.jakartaHttpServletResponse.addHeader(name, value);
    }
}

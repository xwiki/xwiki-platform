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

import org.xwiki.container.RedirectResponse;
import org.xwiki.container.Response;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;

import jakarta.servlet.http.HttpServletResponse;

/**
 * This is the implementation of {@link Response} for {@link HttpServletResponse}.
 *
 * @version $Id$
 */
public class ServletResponse implements Response, RedirectResponse
{
    private final HttpServletResponse jakartaHttpServletResponse;

    private javax.servlet.http.HttpServletResponse javaxHttpServletResponse;

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
        this.javaxHttpServletResponse = javaxHttpServletResponse;
        this.jakartaHttpServletResponse = JakartaServletBridge.toJakarta(javaxHttpServletResponse);
    }

    /**
     * @return the standard Jakarta {@link HttpServletResponse} instance
     * @since 42.0.0
     */
    public HttpServletResponse getJakartaHttpServletResponse()
    {
        return this.jakartaHttpServletResponse;
    }

    /**
     * @return the legacy Javax {@link javax.servlet.http.HttpServletResponse} instance
     * @deprecated use {@link #getJakartaHttpServletResponse()} instead
     */
    @Deprecated(since = "42.0.0")
    public javax.servlet.http.HttpServletResponse getHttpServletResponse()
    {
        if (this.javaxHttpServletResponse == null) {
            this.javaxHttpServletResponse = JakartaServletBridge.toJavax(this.jakartaHttpServletResponse);
        }

        return this.javaxHttpServletResponse;
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
}

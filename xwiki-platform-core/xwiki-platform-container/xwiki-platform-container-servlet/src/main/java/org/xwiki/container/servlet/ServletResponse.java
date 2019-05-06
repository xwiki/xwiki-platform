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

import javax.servlet.http.HttpServletResponse;

import org.xwiki.container.RedirectResponse;
import org.xwiki.container.Response;

/**
 * This is the implementation of {@link Response} for {@link HttpServletResponse}.
 *
 * @version $Id$
 */
public class ServletResponse implements Response, RedirectResponse
{
    private HttpServletResponse httpServletResponse;

    public ServletResponse(HttpServletResponse httpServletResponse)
    {
        this.httpServletResponse = httpServletResponse;
    }

    public HttpServletResponse getHttpServletResponse()
    {
        return this.httpServletResponse;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        try {
            return this.httpServletResponse.getOutputStream();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    @Override
    public void setContentLength(int length)
    {
        this.httpServletResponse.setContentLength(length);
    }

    @Override
    public void setContentType(String mimeType)
    {
        this.httpServletResponse.setContentType(mimeType);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        this.httpServletResponse.sendRedirect(location);
    }
}

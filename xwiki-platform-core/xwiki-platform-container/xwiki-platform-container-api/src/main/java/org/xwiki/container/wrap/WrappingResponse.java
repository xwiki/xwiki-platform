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
package org.xwiki.container.wrap;

import java.io.IOException;
import java.io.OutputStream;

import org.xwiki.container.Response;

/**
 * A wrapper around {@link Response}.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class WrappingResponse implements Response
{
    protected final Response response;

    /**
     * @param response the wrapped response
     */
    public WrappingResponse(Response response)
    {
        this.response = response;
    }

    /**
     * @return the wrapped response
     */
    public Response getResponse()
    {
        return this.response;
    }

    @Override
    public OutputStream getOutputStream() throws IOException
    {
        return this.response.getOutputStream();
    }

    @Override
    public void setContentLength(int length)
    {
        this.response.setContentLength(length);
    }

    @Override
    public void setContentType(String mimeType)
    {
        this.response.setContentType(mimeType);
    }

    @Override
    public void sendRedirect(String location) throws IOException
    {
        this.response.sendRedirect(location);
    }

    @Override
    public void setStatus(int sc)
    {
        this.response.setStatus(sc);
    }

    @Override
    public void sendError(int sc, String msg) throws IOException
    {
        this.response.sendError(sc, msg);
    }

    @Override
    public void sendError(int sc) throws IOException
    {
        this.response.sendError(sc);
    }

    @Override
    public boolean containsHeader(String name)
    {
        return this.response.containsHeader(name);
    }

    @Override
    public void setHeader(String name, String value)
    {
        this.response.setHeader(name, value);
    }

    @Override
    public void addHeader(String name, String value)
    {
        this.response.addHeader(name, value);
    }
}

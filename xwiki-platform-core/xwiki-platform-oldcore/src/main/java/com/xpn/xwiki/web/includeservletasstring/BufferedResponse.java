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
package com.xpn.xwiki.web.includeservletasstring;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class BufferedResponse extends HttpServletResponseWrapper
{
    protected HttpServletResponse internalResponse;

    protected BufferOutputStream outputStream;

    protected PrintWriter writer;

    /** Creates a new instance of BufferedResponse */
    public BufferedResponse(HttpServletResponse internalResponse)
    {
        super(internalResponse);

        this.internalResponse = internalResponse;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        if (this.outputStream == null) {
            this.outputStream = new BufferOutputStream();
        }

        return this.outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException
    {
        if (this.writer == null) {
            this.writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), getCharacterEncoding()));
        }

        return this.writer;
    }

    public byte[] getBufferAsByteArray() throws IOException
    {
        if (this.writer != null) {
            this.writer.flush();
        }

        this.outputStream.flush();

        return this.outputStream.getContentsAsByteArray();
    }

}

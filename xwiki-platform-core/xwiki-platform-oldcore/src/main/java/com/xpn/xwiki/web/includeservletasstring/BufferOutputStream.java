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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class BufferOutputStream extends ServletOutputStream
{
    protected ByteArrayOutputStream buffer;

    /** Creates a new instance of BufferOutputStream */
    public BufferOutputStream()
    {
        buffer = new ByteArrayOutputStream();
    }

    @Override
    public void write(int b) throws IOException
    {
        buffer.write(b);
    }

    @Override
    public void write(byte b[]) throws IOException
    {
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        buffer.write(b, off, len);
    }

    @Override
    public void flush() throws IOException
    {
        buffer.flush();
    }

    @Override
    public void close() throws IOException
    {
        buffer.close();
    }

    public byte[] getContentsAsByteArray() throws IOException
    {
        flush();
        return buffer.toByteArray();
    }

}

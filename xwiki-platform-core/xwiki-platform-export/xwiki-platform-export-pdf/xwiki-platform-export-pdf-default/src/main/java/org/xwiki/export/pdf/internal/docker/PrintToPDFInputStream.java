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
package org.xwiki.export.pdf.internal.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.github.kklisura.cdt.protocol.commands.IO;
import com.github.kklisura.cdt.protocol.types.io.Read;

/**
 * Input stream used to read the result of printing a web page to PDF.
 * 
 * @version $Id$
 * @since 14.4.1
 * @since 14.5RC1
 */
public class PrintToPDFInputStream extends InputStream
{
    private IO io;

    private String stream;

    private boolean finished;

    private int bufferOffset;

    private byte[] buffer = new byte[] {};

    /**
     * Creates a new instance for reading the specified PDF stream.
     * 
     * @param io the service used to read the PDF data
     * @param stream a handle of the stream that holds the PDF data
     */
    public PrintToPDFInputStream(IO io, String stream)
    {
        this.io = io;
        this.stream = stream;
    }

    @Override
    public int read() throws IOException
    {
        if (this.bufferOffset >= this.buffer.length) {
            this.bufferOffset = 0;
            this.buffer = readBuffer();
            if (this.buffer.length == 0) {
                io.close(stream);
                return -1;
            }
        }

        return this.buffer[this.bufferOffset++];
    }

    private byte[] readBuffer()
    {
        if (this.finished) {
            return new byte[] {};
        }

        Read read = io.read(stream);
        this.finished = read.getEof() == Boolean.TRUE;
        if (read.getBase64Encoded() == Boolean.TRUE) {
            return Base64.getDecoder().decode(read.getData());
        } else {
            return read.getData().getBytes(StandardCharsets.UTF_8);
        }
    }
}

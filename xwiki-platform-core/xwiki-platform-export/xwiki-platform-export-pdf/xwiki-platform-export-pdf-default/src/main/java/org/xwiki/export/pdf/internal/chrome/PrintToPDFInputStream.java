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
package org.xwiki.export.pdf.internal.chrome;

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

    private Runnable closeCallback;

    /**
     * Creates a new instance for reading the specified PDF stream.
     * 
     * @param io the service used to read the PDF data
     * @param stream a handle of the stream that holds the PDF data
     * @param closeCallback the code to execute when this input stream is closed
     */
    public PrintToPDFInputStream(IO io, String stream, Runnable closeCallback)
    {
        this.io = io;
        this.stream = stream;
        this.closeCallback = closeCallback;
    }

    @Override
    public int read() throws IOException
    {
        if (this.bufferOffset >= this.buffer.length) {
            this.bufferOffset = 0;
            this.buffer = readBuffer();
            if (this.buffer.length == 0) {
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

        Read read = this.io.read(this.stream);
        this.finished = read.getEof() == Boolean.TRUE;
        String data = read.getData();
        if (read.getBase64Encoded() == Boolean.TRUE) {
            data = new String(Base64.getDecoder().decode(data));
        }

        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException
    {
        this.io.close(this.stream);
        if (this.closeCallback != null) {
            this.closeCallback.run();
        }
    }
}

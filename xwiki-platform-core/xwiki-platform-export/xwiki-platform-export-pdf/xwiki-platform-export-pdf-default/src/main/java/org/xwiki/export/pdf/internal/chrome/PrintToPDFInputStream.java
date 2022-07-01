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
import java.util.Base64;

import com.github.kklisura.cdt.protocol.commands.IO;
import com.github.kklisura.cdt.protocol.types.io.Read;

/**
 * Input stream used to read the result of printing a web page to PDF.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PrintToPDFInputStream extends InputStream
{
    private final IO io;

    private final String stream;

    private final Runnable closeCallback;

    private final int bufferSize;

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
        this(io, stream, () -> {
        });
    }

    /**
     * Creates a new instance for reading the specified PDF stream.
     * 
     * @param io the service used to read the PDF data
     * @param stream a handle of the stream that holds the PDF data
     * @param closeCallback the code to execute when this input stream is closed
     */
    public PrintToPDFInputStream(IO io, String stream, Runnable closeCallback)
    {
        // Read chunks of 1MB.
        this(io, stream, closeCallback, 1 << 20);
    }

    /**
     * Creates a new instance for reading the specified PDF stream.
     * 
     * @param io the service used to read the PDF data
     * @param stream a handle of the stream that holds the PDF data
     * @param closeCallback the code to execute when this input stream is closed
     * @param bufferSize the maximum number of bytes to read at once from the specified stream
     */
    public PrintToPDFInputStream(IO io, String stream, Runnable closeCallback, int bufferSize)
    {
        this.io = io;
        this.stream = stream;
        this.closeCallback = closeCallback;
        this.bufferSize = bufferSize;
    }

    @Override
    public int read() throws IOException
    {
        if (this.bufferOffset >= this.buffer.length) {
            this.bufferOffset = 0;
            this.buffer = readBuffer();
            if (this.buffer.length == 0) {
                // Signal end of stream.
                return -1;
            }
        }

        // The value byte must be returned as an integer in the range 0 to 255. This is needed in order to avoid
        // confusing the signed -1 byte (255 unsigned) with the end of stream (see above where we return -1).
        return Byte.toUnsignedInt(this.buffer[this.bufferOffset++]);
    }

    private byte[] readBuffer()
    {
        if (this.finished) {
            return new byte[] {};
        }

        Read read = this.io.read(this.stream, null, this.bufferSize);
        this.finished = read.getEof() == Boolean.TRUE;
        if (read.getBase64Encoded() == Boolean.TRUE) {
            return Base64.getDecoder().decode(read.getData());
        } else {
            return read.getData().getBytes();
        }
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

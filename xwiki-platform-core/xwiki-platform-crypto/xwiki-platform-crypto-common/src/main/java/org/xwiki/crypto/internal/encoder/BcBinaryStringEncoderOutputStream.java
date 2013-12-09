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
package org.xwiki.crypto.internal.encoder;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An output stream wrapper that will encode binary data to string data in block.
 *
 * @version $Id$
 * @since 5.4M1
 */
class BcBinaryStringEncoderOutputStream extends FilterOutputStream
{
    /** Number of bytes of binary data that should be encoded together. */
    private final int blockSize;

    /** A one byte buffer to optimize one byte writing. */
    private final byte[] oneByte = new byte[1];

    /** An overflow buffer to collate pending output data. */
    private byte[] ofBuf;

    /** Current length in byte of valid data in the overflow buffer. */
    private int ofLen;

    /** Encoder to process data. */
    private final InternalBinaryStringEncoder encoder;

    /**
     * Construct a filter stream for the given encoder, following the specifications.
     *
     * @param outputStream the underlying output stream that will receive encoded data.
     * @param encoder the encoder used to encode data.
     */
    public BcBinaryStringEncoderOutputStream(OutputStream outputStream, InternalBinaryStringEncoder encoder)
    {
        super(outputStream);
        this.encoder = encoder;
        this.blockSize = encoder.getEncodingBlockSize();
        this.ofBuf = new byte[blockSize];
    }

    @Override
    public void write(int i) throws IOException
    {
        oneByte[0] = (byte) i;
        write(oneByte, 0, 1);
    }

    @Override
    public void write(byte[] input, int offset, int length) throws IOException
    {
        if ((offset | length | (input.length - (length + offset)) | (offset + length)) < 0) {
            throw new IndexOutOfBoundsException();
        }

        if (length == 0) {
            return;
        }

        int off = offset;
        int len = length;

        // Is some pending data of a previous call available ?
        if (ofLen > 0) {
            // Complete the overflow buffer, trying to reach a full buffer.
            int underflow = blockSize - ofLen;
            if (underflow > len) {
                underflow = len;
            }
            System.arraycopy(input, off, ofBuf, ofLen, underflow);
            ofLen += underflow;
            off += underflow;
            len -= underflow;

            // Encode and output the overflow buffer if full.
            if (ofLen == blockSize) {
                encoder.encode(ofBuf, 0, blockSize, out);
                ofLen = 0;
            }
        }

        // Still some data to write ?
        if (len > 0) {

            // Store overflow in the overflow buffer
            int overflow = len - ((len / blockSize) * blockSize);
            if (overflow > 0) {
                System.arraycopy(input, (off + len - overflow), ofBuf, 0, overflow);
                ofLen += overflow;
                len -= overflow;
            }

            // If there is still some data to write, encode and write them
            if (len > 0) {
                encoder.encode(input, off, len, out);
            }
        }
    }

    @Override
    public void flush() throws IOException
    {
        if (ofLen > 0) {
            encoder.encode(ofBuf, 0, ofLen, out);
            ofLen = 0;
        }
        out.flush();
    }

    @Override
    public void close() throws IOException
    {
        flush();
        super.close();
    }
}

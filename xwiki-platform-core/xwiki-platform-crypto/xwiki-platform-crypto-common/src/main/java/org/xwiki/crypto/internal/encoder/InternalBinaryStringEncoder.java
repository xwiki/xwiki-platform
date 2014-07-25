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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Encode and decode byte arrays (typically from binary to 7-bit ASCII encodings).
 *
 * @version $Id$
 * @since 5.4M1
 */
public interface InternalBinaryStringEncoder
{
    /**
     * Encode the given buffer area to the given output stream.
     * @param buffer buffer to encode.
     * @param offset offset in the buffer to start from.
     * @param length len of the area to encode from the buffer.
     * @param outputStream the output stream to write data to.
     * @return the number bytes written to the output stream.
     * @throws IOException on error.
     */
    int encode(byte[] buffer, int offset, int length, OutputStream outputStream) throws IOException;

    /**
     * Decode the given buffer area to the given input stream.
     * @param buffer buffer to decode.
     * @param offset offset in the buffer to start from.
     * @param length len of the area to decode from the buffer.
     * @param outputStream the output stream to write data to.
     * @return the number bytes written to the output stream.
     * @throws IOException on error.
     */
    int decode(byte[] buffer, int offset, int length, OutputStream outputStream) throws IOException;

    /**
     * Check is the given byte is a valid byte of encoded data.
     * @param b byte to check.
     * @return true if the byte is a valid encoded data.
     */
    boolean isValidEncoding(byte b);

    /**
     * @return the number of bytes of binary data that should be encoded together during progressive encoding.
     */
    int getEncodingBlockSize();

    /**
     * @return the number of bytes of string data that should be decoded together during progressive decoding.
     */
    int getDecodingBlockSize();
}

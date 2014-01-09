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
package org.xwiki.crypto;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Encoder to encode binary data to string data.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Role
@Unstable
public interface BinaryStringEncoder
{
    /**
     * Return a decoding input stream based on this encoder.
     *
     * @param is an input stream to filter.
     * @return a filtered input stream based on this encoder.
     */
    FilterInputStream getDecoderInputStream(InputStream is);

    /**
     * Return a encoding output stream based on this encoder.
     *
     * @param os an output stream to filter.
     * @return a filtered output stream based on this encoder.
     */
    FilterOutputStream getEncoderOutputStream(OutputStream os);

    /**
     * Return a encoding output stream based on this encoder, wrapping lines at a fixed length.
     *
     * @param os an output stream to filter.
     * @param wrapAt maximum length of a line.
     * @return a filtered output stream based on this encoder.
     */
    FilterOutputStream getEncoderOutputStream(OutputStream os,  int wrapAt);

    /**
     * Encode input with this encoder and without any wrapping.
     * @param input a byte array.
     * @return a string representing the encoded byte array.
     * @throws IOException on error.
     */
    String encode(byte[] input) throws IOException;

    /**
     * Encode input with this encoder, wrapping line at a fixed length.
     * @param input a byte array.
     * @param wrapAt maximum length of a line.
     * @return a string representing the encoded byte array.
     * @throws IOException on error.
     */
    String encode(byte[] input, int wrapAt) throws IOException;

    /**
     * Encode input with this encoder and without any wrapping.
     * @param input a byte array.
     * @param off offset to start with in the byte array.
     * @param len to encode in the byte array.
     * @return a string representing the encoded byte array.
     * @throws IOException on error.
     */
    String encode(byte[] input, int off, int len) throws IOException;

    /**
     * Encode input with this encoder, wrapping line at a fixed length.
     * @param input a byte array.
     * @param off offset to start with in the byte array.
     * @param len to encode in the byte array.
     * @param  wrapAt maximum length of a line.
     * @return a string representing the encoded byte array.
     * @throws IOException on error.
     */
    String encode(byte[] input, int off, int len, int wrapAt) throws IOException;

    /**
     * Decode input string to bytes, whitespace and line-feed are ignored.
     * @param input a string to decode.
     * @return a bytes array.
     * @throws IOException on error.
     */
    byte[] decode(String input) throws IOException;
}

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

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.internal.LineWrapperOutputStream;

/**
 * Abstract base class for encoder based on Bouncy Castle encoders.
 *
 * @version $Id$
 * @since 5.3M1
 */
public abstract class AbstractBinaryStringEncoder implements BinaryStringEncoder
{
    /** Charset used for String <-> byte[] conversion. */
    private static final String CHARSET = "UTF-8";

    abstract InternalBinaryStringEncoder getEncoder();

    @Override
    public String encode(byte[] input) throws IOException
    {
        return encode(input, 0, input.length);
    }

    @Override
    public String encode(byte[] input, int wrapAt) throws IOException
    {
        return encode(input, 0, input.length, wrapAt);
    }

    @Override
    public String encode(byte[] input, int off, int len) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getEncoder().encode(input, off, len, baos);
        baos.close();
        return baos.toString(CHARSET);
    }

    @Override
    public String encode(byte[] input, int off, int len, int wrapAt) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LineWrapperOutputStream lwos = new LineWrapperOutputStream(baos, wrapAt);
        getEncoder().encode(input, off, len, lwos);
        lwos.close();
        return baos.toString(CHARSET);
    }

    @Override
    public byte[] decode(String input) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] inBytes = input.getBytes();
        getEncoder().decode(inBytes, 0, inBytes.length, baos);
        baos.close();
        return baos.toByteArray();
    }

    @Override
    public FilterInputStream getDecoderInputStream(InputStream is)
    {
        return new BcBinaryStringEncoderInputStream(is, getEncoder());
    }

    @Override
    public FilterOutputStream getEncoderOutputStream(OutputStream os)
    {
        return new BcBinaryStringEncoderOutputStream(os, getEncoder());
    }

    @Override
    public FilterOutputStream getEncoderOutputStream(OutputStream os, int wrapAt)
    {
        return new BcBinaryStringEncoderOutputStream(new LineWrapperOutputStream(os, wrapAt), getEncoder());
    }
}

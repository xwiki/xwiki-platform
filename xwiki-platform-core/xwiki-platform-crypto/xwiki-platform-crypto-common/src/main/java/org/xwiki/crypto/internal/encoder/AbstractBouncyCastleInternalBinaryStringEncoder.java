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

import org.bouncycastle.util.encoders.Encoder;

/**
 * Abstract base class to build binary string encoder based on Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBouncyCastleInternalBinaryStringEncoder implements InternalBinaryStringEncoder
{
    private final Encoder encoder;
    private final int blockSize;
    private final int charSize;

    /**
     * Create a wrapper over the given encoder, providing size methods.
     * @param encoder the bouncy castle encoder.
     * @param blockSize the blocksize to report for encoding.
     * @param charSize the blocksize to report for decoding.
     */
    public AbstractBouncyCastleInternalBinaryStringEncoder(Encoder encoder, int blockSize, int charSize)
    {
        this.encoder = encoder;
        this.blockSize = blockSize;
        this.charSize = charSize;
    }

    @Override
    public int encode(byte[] buffer, int offset, int length, OutputStream outputStream) throws IOException
    {
        return encoder.encode(buffer, offset, length, outputStream);
    }

    @Override
    public int decode(byte[] buffer, int offset, int length, OutputStream outputStream) throws IOException
    {
        return encoder.decode(buffer, offset, length, outputStream);
    }

    @Override
    public int getEncodingBlockSize()
    {
        return blockSize;
    }

    @Override
    public int getDecodingBlockSize()
    {
        return charSize;
    }
}

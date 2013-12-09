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

import javax.inject.Named;
import javax.inject.Singleton;

import org.bouncycastle.util.encoders.HexEncoder;
import org.xwiki.component.annotation.Component;

/**
 * Encoder/Decoder of hexadecimal value represented in ascii string.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("Hex")
public class HexBinaryStringEncoder extends AbstractBinaryStringEncoder
{
    /**
     * Number of bytes of binary data that should be encoded together.
     * Hex encode 1 bytes into 2 chars.
     */
    private static final int BLOCK_SIZE = 1;

    /**
     * Number of bytes of string data that should be decoded together.
     * Hex decode 2 chars into 1 bytes.
     */
    private static final int CHAR_SIZE = 2;

    @Override
    InternalBinaryStringEncoder getEncoder()
    {
        return new AbstractBouncyCastleInternalBinaryStringEncoder(new HexEncoder(), BLOCK_SIZE, CHAR_SIZE) {
            @Override
            public boolean isValidEncoding(byte b)
            {
                return ((b >= 0x2f && b <= 0x39) || (b >= 0x41 && b <= 0x46) || (b >= 0x61 && b <= 0x66));
            }
        };
    }
}

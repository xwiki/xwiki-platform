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

import org.bouncycastle.util.encoders.Base64Encoder;
import org.xwiki.component.annotation.Component;

/**
 * Encoder/Decoder of base64 data.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("Base64")
public class Base64BinaryStringEncoder extends AbstractBinaryStringEncoder
{
    /**
     * Number of bytes of binary data that should be encoded together.
     * Base64 encode 3 bytes into 4 chars.
     */
    protected static final int BLOCK_SIZE = 3;

    /**
     * Number of bytes of string data that should be decoded together.
     * Base64 decode 4 chars into 3 bytes.
     */
    protected static final int CHAR_SIZE = 4;


    @Override
    InternalBinaryStringEncoder getEncoder()
    {
        return new AbstractBouncyCastleInternalBinaryStringEncoder(new Base64Encoder(), BLOCK_SIZE, CHAR_SIZE) {
            @Override
            public boolean isValidEncoding(byte b)
            {
                return (b == 0x2b || b == 0x3d || (b >= 0x2f && b <= 0x39)
                    || (b >= 0x41 && b <= 0x5a) || (b >= 0x61 && b <= 0x7a));
            }
        };
    }
}

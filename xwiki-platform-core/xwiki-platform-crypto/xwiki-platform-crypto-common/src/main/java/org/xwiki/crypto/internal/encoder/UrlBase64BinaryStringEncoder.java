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

import org.bouncycastle.util.encoders.UrlBase64Encoder;
import org.xwiki.component.annotation.Component;

/**
 * Encoder/Decoder of base64 data adapted for usage in URLs.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("URLBase64")
public class UrlBase64BinaryStringEncoder extends Base64BinaryStringEncoder
{
    @Override
    InternalBinaryStringEncoder getEncoder()
    {
        return new AbstractBouncyCastleInternalBinaryStringEncoder(new UrlBase64Encoder(), BLOCK_SIZE, CHAR_SIZE) {
            @Override
            public boolean isValidEncoding(byte b)
            {
                return (b == 0x2d || b == 0x2e || b == 0x5f || (b >= 0x30 && b <= 0x39)
                    || (b >= 0x41 && b <= 0x5a) || (b >= 0x61 && b <= 0x7a));
            }
        };
    }
}

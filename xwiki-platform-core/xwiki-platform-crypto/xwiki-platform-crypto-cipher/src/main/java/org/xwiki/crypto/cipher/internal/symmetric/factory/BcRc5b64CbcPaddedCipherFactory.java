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
package org.xwiki.crypto.cipher.internal.symmetric.factory;

import javax.inject.Singleton;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.engines.RC532Engine;
import org.bouncycastle.crypto.params.RC5Parameters;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.params.cipher.symmetric.RC5KeyParameters;

/**
 * Cipher factory for the RC5 Cipher with a 64bits block size.
 *
 * WARNING: RC5 is protected by U.S. Patents 5,724,428 and 5,835,600. Therefore, before expiration of these patents,
 * the usage of this algorithm is subject to restricted usage on the US territories.
 * RC5 is a trademark of RSA Security Inc.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "RC5-32/CBC/PKCS5Padding", "RC5-32/CBC/PKCS7Padding",
                     "RC5/CBC/PKCS5Padding", "RC5/CBC/PKCS7Padding",
                     "1.2.840.113549.3.9" })
@Singleton
public class BcRc5b64CbcPaddedCipherFactory extends AbstractBcCbcPaddedCipherFactory
{
    /** Supported key sizes for this Cipher. RC5 support any size up to 256 bytes, but we limit multiple of 64bits. */
    private static final int[] KEY_SIZES = newKeySizeArray(0, 256, 8);

    @Override
    protected org.bouncycastle.crypto.CipherParameters getBcKeyParameter(KeyParameter parameter) {
        if (parameter instanceof RC5KeyParameters) {
            return new RC5Parameters(parameter.getKey(), ((RC5KeyParameters) parameter).getRounds());
        } else {
            return new org.bouncycastle.crypto.params.KeyParameter(parameter.getKey());
        }
    }

    @Override
    protected BlockCipher getEngineInstance()
    {
        return new RC532Engine();
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return KEY_SIZES;
    }
}

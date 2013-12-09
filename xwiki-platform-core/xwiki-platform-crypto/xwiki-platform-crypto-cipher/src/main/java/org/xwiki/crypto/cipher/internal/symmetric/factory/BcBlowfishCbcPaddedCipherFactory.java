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
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.xwiki.component.annotation.Component;

/**
 * Cipher factory for the AES Cipher.
 *
 * Note: The OID used is not really reserved but suggested by not-yet-common-ssl implementation and test data.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "Blowfish/CBC/PKCS5Padding", "Blowfish/CBC/PKCS7Padding", "1.3.6.1.4.1.3029.1.2" })
@Singleton
public class BcBlowfishCbcPaddedCipherFactory extends AbstractBcCbcPaddedCipherFactory
{
    /** Supported key sizes for this Cipher. */
    private static final int[] KEY_SIZES = newKeySizeArray(4, 56, 1);

    /** PKCS #8 encoded keys seems to not define the key length and to use {@value #DEFAULT_KEY_SIZE} as a default. */
    private static final int DEFAULT_KEY_SIZE = 16;

    @Override
    protected BlockCipher getEngineInstance()
    {
        return new BlowfishEngine();
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return KEY_SIZES;
    }

    @Override
    public int getKeySize()
    {
        return DEFAULT_KEY_SIZE;
    }
}

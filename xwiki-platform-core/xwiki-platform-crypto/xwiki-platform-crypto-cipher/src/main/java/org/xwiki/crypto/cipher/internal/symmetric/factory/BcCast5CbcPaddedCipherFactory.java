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
import org.bouncycastle.crypto.engines.CAST5Engine;
import org.xwiki.component.annotation.Component;

/**
 * Cipher factory for the AES Cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "CAST5/CBC/PKCS5Padding", "CAST5/CBC/PKCS7Padding", "1.2.840.113533.7.66.10" })
@Singleton
public class BcCast5CbcPaddedCipherFactory extends AbstractBcCbcPaddedCipherFactory
{
    /** Supported key sizes for this Cipher. */
    private static final int[] KEY_SIZES = newKeySizeArray(5, 16, 1);

    @Override
    protected BlockCipher getEngineInstance()
    {
        return new CAST5Engine();
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return KEY_SIZES;
    }
}

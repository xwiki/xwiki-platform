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
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.params.RC2Parameters;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.params.cipher.symmetric.RC2KeyParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;

/**
 * Cipher factory for the RC2 Cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "RC2/CBC/PKCS5Padding", "RC2/CBC/PKCS7Padding", "1.2.840.113549.3.2" })
@Singleton
public class Rc2CbcPaddedCipherFactory extends AbstractBcCbcPaddedCipherFactory
{
    /** Supported key sizes for this Cipher. RC2 support up to 128 bytes, but we limit here to 64 since more is rare. */
    private static final int[] KEY_SIZES = newKeySizeArray(1, 64, 1);

    @Override
    protected org.bouncycastle.crypto.CipherParameters getBcKeyParameter(KeyParameter parameter) {
        if (parameter instanceof RC2KeyParameters) {
            return new RC2Parameters(parameter.getKey(), ((RC2KeyParameters) parameter).getEffectiveBits());
        } else {
            return new org.bouncycastle.crypto.params.KeyParameter(parameter.getKey());
        }
    }

    @Override
    protected BlockCipher getEngineInstance()
    {
        return new RC2Engine();
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return KEY_SIZES;
    }
}

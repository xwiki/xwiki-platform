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

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.xwiki.crypto.cipher.SymmetricCipher;
import org.xwiki.crypto.cipher.internal.symmetric.BcPaddedSymmetricCipher;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;

/**
 * Abstract base class for a Cipher Factory of CBC padded Bouncy Castle cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcCbcPaddedCipherFactory extends AbstractBcSymmetricCipherFactory
{
    private ParametersWithIV toParametersWithIV(SymmetricCipherParameters parameters) {
        if (!(parameters instanceof KeyWithIVParameters)) {
            throw new IllegalArgumentException("Invalid parameters for cipher: " + parameters.getClass().getName());
        }
        KeyWithIVParameters params = (KeyWithIVParameters) parameters;

        return new ParametersWithIV(
            getBcKeyParameter(params.getKeyParameter()),
            params.getIV()
        );
    }

    @Override
    protected BlockCipher getCipherInstance(boolean forEncryption, SymmetricCipherParameters parameters)
    {
        return new CBCBlockCipher(getEngineInstance());
    }

    @Override
    public SymmetricCipher getInstance(boolean forEncryption, CipherParameters parameters)
    {
        if (!(parameters instanceof SymmetricCipherParameters)) {
            throw new IllegalArgumentException("Unexpected parameters received for a symmetric cipher: "
                + parameters.getClass().getName());
        }
        return new BcPaddedSymmetricCipher(getCipherInstance(forEncryption, (SymmetricCipherParameters) parameters),
            forEncryption, toParametersWithIV((SymmetricCipherParameters) parameters));
    }
}

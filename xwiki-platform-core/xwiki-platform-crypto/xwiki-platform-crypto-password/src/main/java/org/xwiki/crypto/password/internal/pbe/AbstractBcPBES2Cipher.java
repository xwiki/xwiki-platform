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
package org.xwiki.crypto.password.internal.pbe;

import java.io.IOException;

import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.internal.kdf.AbstractBcKDF;
import org.xwiki.crypto.password.internal.kdf.PBES2Parameters;

/**
 * Abstract base class for PBES2 Password Based Cipher using Bouncy Castle as the underlying implementation.
 *
 * @version $Id$
 */
public abstract class AbstractBcPBES2Cipher extends AbstractBcPBCipher
{
    /**
     * New PBE Cipher instance, that wrap the cipher created using the given key derivation function and parameters.
     *
     * @param cipher the cipher to wrap.
     * @param kdf the key derivation function used to derive the key of this cipher.
     * @param parameters the cipher parameter used.
     */
    public AbstractBcPBES2Cipher(Cipher cipher, KeyDerivationFunction kdf, SymmetricCipherParameters parameters)
    {
        super(cipher, kdf, parameters);
    }

    protected abstract EncryptionScheme getScheme(SymmetricCipherParameters parameters);

    @Override
    public AlgorithmIdentifier getPBEParameters() throws IOException
    {
        KeyDerivationFunc kdfParams;

        if (getKeyDerivationFunction() instanceof AbstractBcKDF) {
            kdfParams = ((AbstractBcKDF) getKeyDerivationFunction()).getKeyDerivationFunction();
        } else {
            kdfParams = KeyDerivationFunc.getInstance(getKeyDerivationFunction().getEncoded());
        }

        EncryptionScheme scheme = getScheme(getParameters());

        return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_PBES2, new PBES2Parameters(kdfParams, scheme));
    }
}

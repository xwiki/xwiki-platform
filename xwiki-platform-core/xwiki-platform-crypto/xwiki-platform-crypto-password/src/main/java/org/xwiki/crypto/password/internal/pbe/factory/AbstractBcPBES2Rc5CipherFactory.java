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
package org.xwiki.crypto.password.internal.pbe.factory;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.params.cipher.symmetric.RC5KeyParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.internal.pbe.AbstractBcPBES2Cipher;
import org.xwiki.crypto.password.internal.pbe.RC5CBCParameter;

/**
 * Abstract base class for PBES2 RC5 cipher factory.
 *
 * WARNING: RC5 is protected by U.S. Patents 5,724,428 and 5,835,600. Therefore, before expiration of these patents,
 * the usage of this algorithm is subject to restricted usage on the US territories.
 * RC5 is a trademark of RSA Security Inc.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcPBES2Rc5CipherFactory extends AbstractBcPBES2CipherFactory
{
    private static final ASN1ObjectIdentifier ALG_ID = PKCSObjectIdentifiers.encryptionAlgorithm.branch("9");

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunction kdf)
    {
        KeyWithIVParameters params = null;

        if (password instanceof KeyWithIVParameters) {
            KeyParameter passkey = ((KeyWithIVParameters) password).getKeyParameter();
            if (passkey instanceof RC5KeyParameters) {
                params = new KeyWithIVParameters(
                    new RC5KeyParameters(kdf.derive(passkey.getKey()).getKey(),
                        ((RC5KeyParameters) passkey).getRounds()),
                    ((KeyWithIVParameters) password).getIV());
            }
        } else if (password instanceof RC5KeyParameters) {
            params = kdf.derive(((KeyParameter) password).getKey(), getIVSize());
            params = new KeyWithIVParameters(new RC5KeyParameters(params.getKey(),
                ((RC5KeyParameters) password).getRounds()), params.getIV());
        }

        if (params == null) {
            throw new IllegalArgumentException("Invalid cipher parameters for RC5-32 password based cipher: "
                + password.getClass().getName());
        }

        return getPasswordBasedCipher(forEncryption, kdf, params);
    }

    @Override
    protected PasswordBasedCipher getPasswordBasedCipher(boolean forEncryption, KeyDerivationFunction kdf,
        SymmetricCipherParameters params)
    {
        return new AbstractBcPBES2Cipher(getCipherFactory().getInstance(forEncryption, params), kdf, params)
        {
            @Override
            protected EncryptionScheme getScheme(SymmetricCipherParameters parameters)
            {
                return new EncryptionScheme(ALG_ID,
                    new RC5CBCParameter(
                        ((RC5KeyParameters) ((KeyWithIVParameters) parameters).getKeyParameter()).getRounds(),
                        getOutputBlockSize(),
                        ((KeyWithIVParameters) parameters).getIV()));
            }
        };
    }

    @Override
    protected PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, KeyDerivationFunc kdfParams,
        EncryptionScheme scheme)
    {
        KeyDerivationFunction kdf = getKeyDerivationFunction(kdfParams);
        RC5CBCParameter rc5Params = RC5CBCParameter.getInstance(scheme.getParameters());

        return getPasswordBasedCipher(forEncryption, kdf, getRC5CipherParameters(password, rc5Params, kdf));
    }

    private SymmetricCipherParameters getRC5CipherParameters(byte[] password, RC5CBCParameter rc5Params,
        KeyDerivationFunction df)
    {
        KeyParameter keyParam =
            new RC5KeyParameters(df.derive(password).getKey(), rc5Params.getRounds().intValue());
        if (rc5Params.getIV() != null) {
            return new KeyWithIVParameters(keyParam, rc5Params.getIV());
        } else {
            return keyParam;
        }
    }
}

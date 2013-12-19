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

import java.io.IOException;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.KeyDerivationFunctionFactory;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.internal.kdf.PBES2Parameters;
import org.xwiki.crypto.password.internal.kdf.factory.AbstractBcKDFFactory;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;

/**
 * Abstract base class for PBES2 Password Based Cipher factory using Bouncy Castle as the underlying implementation.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcPBES2CipherFactory extends AbstractBcPBCipherFactory
{
    private static final RuntimeException UNSUPPORTED =
        new UnsupportedOperationException("Sorry, no concrete implementation to create an instance.");

    private KeyDerivationFunctionFactory safeGetKDFFactory()
    {
        try {
            return getKDFFactory();
        } catch (UnsupportedOperationException e) {
            throw UNSUPPORTED;
        }
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, ASN1Encodable parameters)
    {
        AlgorithmIdentifier alg = AlgorithmIdentifier.getInstance(parameters);

        if (!alg.getAlgorithm().equals(PKCSObjectIdentifiers.id_PBES2)) {
            throw new IllegalArgumentException("Illegal algorithm identifier for PBES2: " + alg.getAlgorithm().getId());
        }

        PBES2Parameters params = PBES2Parameters.getInstance(alg.getParameters());
        return getInstance(forEncryption, password, params.getKeyDerivationFunc(), params.getEncryptionScheme());
    }


    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters parameters)
    {
        KeyDerivationFunction kdf = safeGetKDFFactory().getInstance(parameters);

        if (kdf.getKeySize() < 0 || !isSupportedKeySize(kdf.getKeySize())) {
            kdf.overrideKeySize(getKeySize());
        }

        return getInstance(forEncryption, password, kdf);
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunction kdf)
    {
        SymmetricCipherParameters params;

        if (password instanceof KeyWithIVParameters) {
            params = new KeyWithIVParameters(kdf.derive(((KeyWithIVParameters) password).getKey()),
                ((KeyWithIVParameters) password).getIV());
        } else if (password instanceof KeyParameter) {
            params = kdf.derive(((KeyParameter) password).getKey(), getIVSize());
        } else {
            throw new IllegalArgumentException("Invalid cipher parameters for this password based cipher: "
                + password.getClass().getName());
        }

        return getPasswordBasedCipher(forEncryption, kdf, params);
    }

    /**
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param password the password that will be used to derive the key.
     * @param kdfParams key derivation function parameters.
     * @param scheme encryption scheme.
     * @return a initialized key derivation function with a specific password bytes conversion mode.
     */
    protected PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, KeyDerivationFunc kdfParams,
        EncryptionScheme scheme)
    {
        KeyDerivationFunction kdf = getKeyDerivationFunction(kdfParams);

        // Fix key size if needed.
        if (kdf.getKeySize() < 0 || !isSupportedKeySize(kdf.getKeySize())) {
            kdf.overrideKeySize(getKeySize());
        }

        return getPasswordBasedCipher(forEncryption, kdf, new KeyWithIVParameters(kdf.derive(password).getKey(),
            ((ASN1OctetString) scheme.getParameters()).getOctets()));
    }

    /**
     * Create a new instance of a password based cipher.
     *
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param kdf the key derivation function
     * @param params the cipher parameters
     * @return a initialized password based cipher.
     */
    protected PasswordBasedCipher getPasswordBasedCipher(boolean forEncryption, final KeyDerivationFunction kdf,
        SymmetricCipherParameters params) {
        throw UNSUPPORTED;
    }

    protected KeyDerivationFunction getKeyDerivationFunction(KeyDerivationFunc func)
    {
        KeyDerivationFunctionFactory kdfFactory = safeGetKDFFactory();

        // Optimization
        if (kdfFactory instanceof AbstractBcKDFFactory) {
            return ((AbstractBcKDFFactory) kdfFactory).getInstance(func);
        }

        // Generic fallback
        try {
            return kdfFactory.getInstance(func.toASN1Primitive().getEncoded());
        } catch (IOException e) {
            // Very unlikely to happen
            throw new RuntimeException("Unexpected exception during parameter encoding");
        }
    }
}

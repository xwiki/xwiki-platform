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

import javax.inject.Inject;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.KeyDerivationFunctionFactory;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.PasswordBasedCipherFactory;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;

/**
 * Abstract base class for password based cipher factory based on Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcPBCipherFactory implements PasswordBasedCipherFactory
{
    private static final RuntimeException UNSUPPORTED =
        new UnsupportedOperationException("Sorry, this factory does implement any concrete cipher.");

    @Inject
    private KeyDerivationFunctionFactory kdfFactory;

    /**
     * @return an instance of the key derivation function factory in use for this password based cipher.
     */
    protected KeyDerivationFunctionFactory getKDFFactory()
    {
        return kdfFactory;
    }

    private CipherFactory safeGetCipherFactory()
    {
        try {
            return getCipherFactory();
        } catch (UnsupportedOperationException e) {
            throw UNSUPPORTED;
        }
    }

    @Override
    public String getCipherAlgorithmName()
    {
        return safeGetCipherFactory().getCipherAlgorithmName();
    }

    @Override
    public int getIVSize()
    {
        return safeGetCipherFactory().getIVSize();
    }

    @Override
    public int getKeySize()
    {
        return safeGetCipherFactory().getKeySize();
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return safeGetCipherFactory().getSupportedKeySizes();
    }

    @Override
    public boolean isSupportedKeySize(int keySize)
    {
        return safeGetCipherFactory().isSupportedKeySize(keySize);
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters parameters)
    {
        throw new UnsupportedOperationException("Sorry, no concrete implementation to create an instance.");
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, byte[] encoded)
    {
        return getInstance(forEncryption, password, ASN1Sequence.getInstance(encoded));
    }

    /**
     * @return an instance of the block cipher factory in use for this password based cipher.
     */
    protected abstract CipherFactory getCipherFactory();

    /**
     * Get a Password based cipher that is Bouncy Castle based.
     *
     * The ASN.1 parameter will be parsed as an algorithm identifier and should contains the derivation function and
     * cipher parameters appropriately.
     *
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param password the password that will be used to derive the key.
     * @param parameters ASN.1 representation of the parameters needed to define a Password Based Cipher.
     * @return a initialized key derivation function with a specific password bytes conversion mode.
     */
    public abstract PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, ASN1Encodable parameters);
}

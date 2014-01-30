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
package org.xwiki.crypto.password.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.PasswordBasedCipherFactory;
import org.xwiki.crypto.password.PrivateKeyPasswordBasedEncryptor;
import org.xwiki.crypto.password.internal.pbe.AbstractBcPBCipher;
import org.xwiki.crypto.password.internal.pbe.factory.AbstractBcPBCipherFactory;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.PBKDF2Parameters;

/**
 * Encrypter / Decrypter of private keys using password based ciphers as defined in PKCS #8.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component
@Singleton
public class DefaultPrivateKeyPasswordBasedEncryptor implements PrivateKeyPasswordBasedEncryptor
{
    @Inject
    private ComponentManager manager;

    @Inject
    private AsymmetricKeyFactory keyFactory;

    @Inject
    private Provider<SecureRandom> randomProvider;

    @Override
    public PrivateKeyParameters decrypt(byte[] password, byte[] encoded)
        throws GeneralSecurityException, IOException
    {
        EncryptedPrivateKeyInfo encKeyInfo = EncryptedPrivateKeyInfo.getInstance(encoded);
        return decrypt(password, encKeyInfo.getEncryptionAlgorithm(), encKeyInfo.getEncryptedData());
    }

    @Override
    public PrivateKeyParameters decrypt(byte[] password, javax.crypto.EncryptedPrivateKeyInfo privateKeyInfo)
        throws GeneralSecurityException, IOException
    {
        return decrypt(password, privateKeyInfo.getEncoded());
    }

    private PrivateKeyParameters decrypt(byte[] password, AlgorithmIdentifier algId, byte[] encoded)
        throws GeneralSecurityException, IOException
    {
        return keyFactory.fromPKCS8(getPBECipher(password, algId).doFinal(encoded));
    }

    private PasswordBasedCipher getPBECipher(byte[] password, AlgorithmIdentifier algId) throws IOException
    {
        PasswordBasedCipherFactory factory = getPBEFactory(algId.getAlgorithm().getId());

        // Optimization
        if (factory instanceof AbstractBcPBCipherFactory) {
            return ((AbstractBcPBCipherFactory) factory).getInstance(false, password, algId);
        }

        return factory.getInstance(false, password, algId.getEncoded());
    }

    private PasswordBasedCipherFactory getPBEFactory(String hint) {
        try {
            return manager.getInstance(PasswordBasedCipherFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Password based cipher factory not found: " + hint, e);
        }
    }

    @Override
    public byte[] encrypt(String algHint, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters kdfParameters, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException
    {
        PasswordBasedCipher cipher = getPBEFactory(algHint).getInstance(true, password, kdfParameters);
        return encrypt(cipher, privateKey);
    }

    @Override
    public byte[] encrypt(String algHint, SymmetricCipherParameters password, KeyDerivationFunction function,
        PrivateKeyParameters privateKey) throws GeneralSecurityException, IOException
    {
        PasswordBasedCipher cipher = getPBEFactory(algHint).getInstance(true, password, function);
        return encrypt(cipher, privateKey);
    }

    @Override
    public byte[] encrypt(String algHint, byte[] password, byte[] encoded, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException
    {
        PasswordBasedCipher cipher = getPBEFactory(algHint).getInstance(true, password, encoded);
        return encrypt(cipher, privateKey);
    }

    @Override
    public byte[] encrypt(String algHint, byte[] password, KeyDerivationFunctionParameters kdfParameters,
        PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException
    {
        PasswordBasedCipherFactory factory = getPBEFactory(algHint);
        PasswordBasedCipher cipher = factory.getInstance(true,
            new KeyWithIVParameters(password, factory.getIVSize(), randomProvider.get()),
            kdfParameters);
        return encrypt(cipher, privateKey);
    }

    @Override
    public byte[] encrypt(byte[] password, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException
    {
        PasswordBasedCipherFactory factory = getPBEFactory("PBES2-AES-CBC-Pad");
        PasswordBasedCipher cipher = factory.getInstance(true,
            new KeyWithIVParameters(password, factory.getIVSize(), randomProvider.get()),
            new PBKDF2Parameters(randomProvider.get()));
        return encrypt(cipher, privateKey);
    }

    @Override
    public byte[] encrypt(PasswordBasedCipher cipher, PrivateKeyParameters privateKey)
        throws IOException, GeneralSecurityException
    {
        AlgorithmIdentifier algId;

        // Optimization
        if (cipher instanceof AbstractBcPBCipher) {
            algId = ((AbstractBcPBCipher) cipher).getPBEParameters();
        } else {
            algId = AlgorithmIdentifier.getInstance(cipher.getEncoded());
        }

        return new EncryptedPrivateKeyInfo(algId, cipher.doFinal(privateKey.getEncoded())).getEncoded();
    }
}

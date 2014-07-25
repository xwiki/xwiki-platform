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

import java.math.BigInteger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RC2CBCParameter;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.params.cipher.symmetric.RC2KeyParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.internal.pbe.AbstractBcPBES2Cipher;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;

/**
 * Implement PBES2 encryption scheme with RC2 encryption.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PBES2-RC2-CBC-Pad", "1.2.840.113549.3.2" })
@Singleton
public class BcPBES2Rc2CipherFactory extends AbstractBcPBES2CipherFactory
{
    @Inject
    @Named("RC2/CBC/PKCS5Padding")
    private CipherFactory cipherFactory;

    @Override
    protected CipherFactory getCipherFactory()
    {
        return cipherFactory;
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters parameters)
    {
        KeyDerivationFunction kdf = getKDFFactory().getInstance(parameters);

        if (kdf.getKeySize() < 0 || !isSupportedKeySize(kdf.getKeySize())) {
            KeyParameter keyPass;
            if (password instanceof KeyWithIVParameters) {
                keyPass = ((KeyWithIVParameters) password).getKeyParameter();
            } else {
                keyPass = (KeyParameter) password;
            }
            if (keyPass instanceof RC2KeyParameters) {
                kdf.overrideKeySize((((RC2KeyParameters) keyPass).getEffectiveBits() + 7) / 8);
            } else {
                kdf.overrideKeySize(getKeySize());
            }
        }

        return getInstance(forEncryption, password, kdf);
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunction kdf)
    {
        KeyWithIVParameters params;

        if (password instanceof KeyWithIVParameters) {
            KeyParameter passkey = ((KeyWithIVParameters) password).getKeyParameter();
            if (passkey instanceof RC2KeyParameters) {
                params = new KeyWithIVParameters(
                    new RC2KeyParameters(kdf.derive(passkey.getKey()).getKey(),
                        ((RC2KeyParameters) passkey).getEffectiveBits()),
                    ((KeyWithIVParameters) password).getIV());
            } else {
                params = new KeyWithIVParameters(kdf.derive(((KeyWithIVParameters) password).getKey()),
                    ((KeyWithIVParameters) password).getIV());
            }
        } else if (password instanceof RC2KeyParameters) {
            params = kdf.derive(((KeyParameter) password).getKey(), getIVSize());
            params = new KeyWithIVParameters(new RC2KeyParameters(params.getKey(),
                                                ((RC2KeyParameters) password).getEffectiveBits()), params.getIV());
        } else if (password instanceof KeyParameter) {
            params = kdf.derive(((KeyParameter) password).getKey(), getIVSize());
        } else {
            throw new IllegalArgumentException("Invalid cipher parameters for RC2 password based cipher: "
                + password.getClass().getName());
        }

        // ensure RC2Version is computable
        getRC2Version(params);

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
                return new EncryptionScheme(PKCSObjectIdentifiers.RC2_CBC,
                    new RC2CBCParameter(getRC2Version((KeyWithIVParameters) parameters),
                        ((KeyWithIVParameters) parameters).getIV()));
            }
        };
    }

    @Override
    protected PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, KeyDerivationFunc kdfParams,
        EncryptionScheme scheme)
    {
        KeyDerivationFunction kdf = getKeyDerivationFunction(kdfParams);
        RC2CBCParameter rc2Params = RC2CBCParameter.getInstance(scheme.getParameters());

        return getPasswordBasedCipher(forEncryption, kdf, getRC2CipherParameters(password, rc2Params, kdf));
    }

    private int getRC2Version(KeyWithIVParameters parameters)
    {
        KeyParameter keyParams = parameters.getKeyParameter();
        int keySize;
        if (keyParams instanceof RC2KeyParameters) {
            keySize = ((RC2KeyParameters) keyParams).getEffectiveBits();
        } else {
            keySize = keyParams.getKey().length * 8;
        }

        switch(keySize) {
            case 40:
                return 160;
            case 64:
                return 120;
            case 128:
                return 58;
            default:
                if (keySize < 256) {
                    throw new IllegalArgumentException("Invalid cipher key size for PBES2 RC2 password based cipher: "
                        + keySize + " bits. Valid key size are 40, 64, 128 and 256 and more.");
                }
                return keySize;
        }
    }

    private SymmetricCipherParameters getRC2CipherParameters(byte[] password, RC2CBCParameter rc2Params,
        KeyDerivationFunction df)
    {
        KeyParameter keyParam;
        BigInteger version = rc2Params.getRC2ParameterVersion();
        if (version != null) {
            int bits = getRC2EffectiveBits(version.intValue());
            df.overrideKeySize((bits + 7) / 8);
            keyParam = new RC2KeyParameters(df.derive(password).getKey(), bits);
        } else {
            df.overrideKeySize(4);
            keyParam = new KeyParameter(df.derive(password).getKey());
        }

        return new KeyWithIVParameters(keyParam, rc2Params.getIV());
    }

    private int getRC2EffectiveBits(int version) {
        switch(version) {
            case 160:
                return 40;
            case 120:
                return 64;
            case 58:
                return 128;
            default:
                return version;
        }
    }
}

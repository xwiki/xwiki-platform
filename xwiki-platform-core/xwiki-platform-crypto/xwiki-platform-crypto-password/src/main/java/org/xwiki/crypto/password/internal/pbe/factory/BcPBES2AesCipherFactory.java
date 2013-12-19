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
import javax.inject.Named;
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.internal.pbe.AbstractBcPBES2Cipher;

/**
 * Implement PBES2 encryption scheme with AES encryption according to PKCS #5 v2.1 draft.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PBES2-AES-CBC-Pad",
                     "2.16.840.1.101.3.4.1.2", "2.16.840.1.101.3.4.1.22", "2.16.840.1.101.3.4.1.42",
                     // Add wrong OID that may be existing due to typos in earlier publication.
                     "2.16.840.1.101.3.4.2", "2.16.840.1.101.3.4.22", "2.16.840.1.101.3.4.42" })
@Singleton
public class BcPBES2AesCipherFactory extends AbstractBcPBES2CipherFactory
{
    @Inject
    @Named("AES/CBC/PKCS7Padding")
    private CipherFactory cipherFactory;

    @Override
    protected CipherFactory getCipherFactory()
    {
        return cipherFactory;
    }

    @Override
    protected PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, KeyDerivationFunc kdfParams,
        EncryptionScheme scheme)
    {
        KeyDerivationFunction kdf = getKeyDerivationFunction(kdfParams);

        // Set key size according to the encryption scheme algorithm used.
        kdf.overrideKeySize(getAESKeySize(scheme.getAlgorithm()));

        return getPasswordBasedCipher(forEncryption, kdf, new KeyWithIVParameters(kdf.derive(password).getKey(),
            ((ASN1OctetString) scheme.getParameters()).getOctets()));
    }

    @Override
    protected PasswordBasedCipher getPasswordBasedCipher(boolean forEncryption, final KeyDerivationFunction kdf,
        SymmetricCipherParameters params)
    {
        /** Overwrite the key length with itself, since the key length will be encoded in the algorithm identifier */
        kdf.overrideKeySize(kdf.getKeySize());

        return new AbstractBcPBES2Cipher(getCipherFactory().getInstance(forEncryption, params), kdf, params)
        {
            @Override
            protected EncryptionScheme getScheme(SymmetricCipherParameters parameters)
            {
                return new EncryptionScheme(
                    getAESAlgoritmIdentifier(((KeyWithIVParameters) parameters).getKey().length),
                    new DEROctetString(((KeyWithIVParameters) parameters).getIV()));
            }
        };
    }

    private int getAESKeySize(ASN1ObjectIdentifier algId)
    {
        if (algId.equals(NISTObjectIdentifiers.id_aes128_CBC)) {
            return 16;
        } else if (algId.equals(NISTObjectIdentifiers.id_aes192_CBC)) {
            return 24;
        } else if (algId.equals(NISTObjectIdentifiers.id_aes256_CBC)) {
            return 32;
        }
        throw new IllegalArgumentException("Unexpected algorithm identifier used for PBES2 AES encryption scheme: "
                                            + algId.toString());
    }

    private ASN1ObjectIdentifier getAESAlgoritmIdentifier(int keySize)
    {
        switch (keySize) {
            case 16:
                return NISTObjectIdentifiers.id_aes128_CBC;
            case 24:
                return NISTObjectIdentifiers.id_aes192_CBC;
            case 32:
                return NISTObjectIdentifiers.id_aes256_CBC;
            default:
                throw new IllegalArgumentException("Unexpected key size used for PBES2 AES encryption scheme: "
                    + keySize);
        }
    }
}

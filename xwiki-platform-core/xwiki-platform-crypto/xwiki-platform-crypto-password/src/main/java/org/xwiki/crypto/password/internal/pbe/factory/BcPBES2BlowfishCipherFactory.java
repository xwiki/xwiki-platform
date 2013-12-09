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
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.internal.pbe.AbstractBcPBES2Cipher;

/**
 * Implement PBES2 encryption scheme with Blowfish encryption (GNU like).
 *
 * Note: The OID used is not really reserved but suggested by not-yet-common-ssl implementation and test data.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PBES2-Blowfish-CBC-Pad", "1.3.6.1.4.1.3029.1.2" })
@Singleton
public class BcPBES2BlowfishCipherFactory extends AbstractBcPBES2CipherFactory
{
    private static final ASN1ObjectIdentifier ALG_ID = new ASN1ObjectIdentifier("1.3.6.1.4.1.3029.1.2");

    @Inject
    @Named("Blowfish/CBC/PKCS5Padding")
    private CipherFactory cipherFactory;

    @Override
    protected CipherFactory getCipherFactory()
    {
        return cipherFactory;
    }

    @Override
    protected PasswordBasedCipher getPasswordBasedCipher(boolean forEncryption, final KeyDerivationFunction kdf,
        SymmetricCipherParameters params)
    {
        return new AbstractBcPBES2Cipher(getCipherFactory().getInstance(forEncryption, params), kdf, params)
        {
            @Override
            protected EncryptionScheme getScheme(SymmetricCipherParameters parameters)
            {
                return new EncryptionScheme(ALG_ID,
                    new DEROctetString(((KeyWithIVParameters) parameters).getIV()));
            }
        };
    }
}

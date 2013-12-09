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
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.PasswordBasedCipherFactory;
import org.xwiki.crypto.password.internal.kdf.PBES2Parameters;
import org.xwiki.crypto.password.internal.pbe.RC5CBCParameter;

/**
 * Implement the parsing of PBES2 encryption scheme.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PBES2", "1.2.840.113549.1.5.13" })
@Singleton
public class BcPBES2CipherFactory extends AbstractBcPBES2CipherFactory
{
    private static final RuntimeException UNSUPPORTED =
        new UnsupportedOperationException("Unexpected internal function call.");

    @Inject
    private ComponentManager manager;

    @Override
    protected CipherFactory getCipherFactory()
    {
        throw UNSUPPORTED;
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, byte[] encoded)
    {
        ASN1Sequence seq = ASN1Sequence.getInstance(encoded);
        AlgorithmIdentifier alg = getPBES2AlgorithmIdentifier(seq);

        PBES2Parameters params = PBES2Parameters.getInstance(alg.getParameters());
        PasswordBasedCipherFactory pbecf = getPBES2CipherFactory(params.getEncryptionScheme());
        PasswordBasedCipher cipher = getBcPBES2PasswordBasedCipher(pbecf, forEncryption, password, seq);

        if (cipher != null) {
            return cipher;
        }
        return pbecf.getInstance(forEncryption, password, encoded);
    }

    @Override
    public PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, ASN1Encodable parameters)
    {
        return getBcPBES2PasswordBasedCipher(
            getPBES2CipherFactory(
                PBES2Parameters.getInstance(
                    getPBES2AlgorithmIdentifier(parameters).getParameters()).getEncryptionScheme()),
            forEncryption, password, parameters);
    }

    private AlgorithmIdentifier getPBES2AlgorithmIdentifier(ASN1Encodable parameters)
    {
        AlgorithmIdentifier alg = AlgorithmIdentifier.getInstance(parameters);

        if (!alg.getAlgorithm().equals(PKCSObjectIdentifiers.id_PBES2)) {
            throw new IllegalArgumentException("Illegal algorithm identifier for PBES2: " + alg.getAlgorithm().getId());
        }
        return alg;
    }

    private PasswordBasedCipher getBcPBES2PasswordBasedCipher(PasswordBasedCipherFactory pbecf, boolean forEncryption,
        byte[] password, ASN1Encodable parameters)
    {
        if (pbecf instanceof AbstractBcPBES2CipherFactory) {
            return ((AbstractBcPBES2CipherFactory) pbecf).getInstance(forEncryption, password, parameters);
        }
        return null;
    }

    @Override
    protected PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, KeyDerivationFunc kdfParams,
        EncryptionScheme scheme)
    {
        // Avoid spurious issues, this one should never be called anymore since the above one is overwritten.
        throw UNSUPPORTED;
    }

    private PasswordBasedCipherFactory getPBES2CipherFactory(EncryptionScheme scheme)
    {
        try {
            if (scheme.getAlgorithm().equals(PKCSObjectIdentifiers.encryptionAlgorithm.branch("9"))) {
                RC5CBCParameter rc5Param = RC5CBCParameter.getInstance(scheme.getParameters());
                if (rc5Param.getBlockSizeInBits().intValue() > 64) {
                    // RC5-CBC-Pad with a 128bits block size
                    return manager.getInstance(PasswordBasedCipherFactory.class, "PBES2-RC5-64-CBC-Pad");
                }
            }
            return manager.getInstance(PasswordBasedCipherFactory.class, scheme.getAlgorithm().getId());
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Password based cipher factory not found.", e);
        }
    }
}

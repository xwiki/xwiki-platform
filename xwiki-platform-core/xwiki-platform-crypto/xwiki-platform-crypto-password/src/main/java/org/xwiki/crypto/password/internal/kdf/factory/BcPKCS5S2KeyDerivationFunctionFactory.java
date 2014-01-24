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
package org.xwiki.crypto.password.internal.kdf.factory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.crypto.internal.digest.factory.AbstractBcDigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcDigestFactory;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.internal.kdf.AbstractBcPBKDF2;
import org.xwiki.crypto.password.internal.kdf.PBKDF2Params;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.PBKDF2Parameters;

/**
 * Implementation of the key derivation function for PBKDF2 with Hmac SHA-1 digest using Bouncy Castle.
 *
 * Functions provided by this factory are conform with the PKCS 5 V2.0 Scheme 2 published as IETF's RFC 2898.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "PKCS5S2", "1.2.840.113549.1.5.12" })
@Singleton
public class BcPKCS5S2KeyDerivationFunctionFactory extends AbstractBcKDFFactory
{
    private static final AlgorithmIdentifier HMAC_SHA1   =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA1);
    private static final AlgorithmIdentifier HMAC_SHA224 =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA224);
    private static final AlgorithmIdentifier HMAC_SHA256 =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA256);
    private static final AlgorithmIdentifier HMAC_SHA384 =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA384);
    private static final AlgorithmIdentifier HMAC_SHA512 =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA512);

    @Inject
    private ComponentManager manager;

    @Override
    public KeyDerivationFunction getInstance(KeyDerivationFunctionParameters params)
    {
        if (!(params instanceof PBKDF2Parameters)) {
            throw new IllegalArgumentException("Invalid parameter used for PKCS5S2 function: "
                + params.getClass().getName());
        }

        PBKDF2Parameters kdfParams = (PBKDF2Parameters) params;
        PKCS5S2ParametersGenerator generator;
        BcDigestFactory factory = null;
        if (kdfParams.getPseudoRandomFuntionHint() != null) {
            factory = this.getDigestFactory(kdfParams.getPseudoRandomFuntionHint());
            generator = new PKCS5S2ParametersGenerator(factory.getDigestInstance());
        } else {
            generator = new PKCS5S2ParametersGenerator();
        }

        return new AbstractBcPBKDF2(generator, (PBKDF2Parameters) params,
                        (factory != null)
                            ? toHmacAlgId(factory.getAlgorithmIdentifier())
                            : HMAC_SHA1) {
            @Override
            public KeyDerivationFunc getKeyDerivationFunction()
            {
                PBKDF2Parameters parameters = (PBKDF2Parameters) getParameters();
                AlgorithmIdentifier algId = getPRFAlgorithmIdentifier();
                return new KeyDerivationFunc(PKCSObjectIdentifiers.id_PBKDF2,
                    (isKeySizeOverwritten()) ? new PBKDF2Params(parameters.getSalt(),
                                                                parameters.getIterationCount(),
                                                                algId)
                                             : new PBKDF2Params(parameters.getSalt(),
                                                                parameters.getIterationCount(),
                                                                parameters.getKeySize(),
                                                                algId));
            }
        };
    }

    @Override
    public KeyDerivationFunction getInstance(ASN1Encodable parameters)
    {
        KeyDerivationFunc kdf = KeyDerivationFunc.getInstance(parameters);

        if (!kdf.getAlgorithm().equals(PKCSObjectIdentifiers.id_PBKDF2)) {
            throw new IllegalArgumentException("Illegal algorithm identifier for PBKDF2: "
                + kdf.getAlgorithm().getId());
        }

        PBKDF2Params params = PBKDF2Params.getInstance(kdf.getParameters());

        return getInstance(
            new PBKDF2Parameters((params.getKeyLength() != null) ? params.getKeyLength().intValue() : -1,
                params.getIterationCount().intValue(),
                params.getSalt(),
                toDigestHint(params.getPseudoRandomFunctionIdentifier()))
        );
    }

    private BcDigestFactory getDigestFactory(String hint)
    {
        try {
            DigestFactory factory = manager.getInstance(DigestFactory.class, hint);

            if (!(factory instanceof BcDigestFactory)) {
                throw new IllegalArgumentException(
                    "Requested digest algorithm is not implemented by a factory compatible with this factory."
                        + " Factory found: " + factory.getClass().getName());
            }

            return (AbstractBcDigestFactory) factory;
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Digest algorithm not found: " + hint, e);
        }
    }

    private AlgorithmIdentifier toHmacAlgId(AlgorithmIdentifier algorithmIdentifier)
    {
        ASN1ObjectIdentifier algId = algorithmIdentifier.getAlgorithm();
        AlgorithmIdentifier hmac = null;

        if (algId.equals(X509ObjectIdentifiers.id_SHA1)) {
            hmac = HMAC_SHA1;
        } else if (algId.equals(NISTObjectIdentifiers.id_sha224)) {
            hmac = HMAC_SHA224;
        } else if (algId.equals(NISTObjectIdentifiers.id_sha256)) {
            hmac =  HMAC_SHA256;
        } else if (algId.equals(NISTObjectIdentifiers.id_sha384)) {
            hmac = HMAC_SHA384;
        } else if (algId.equals(NISTObjectIdentifiers.id_sha512)) {
            hmac = HMAC_SHA512;
        }
        if (hmac == null) {
            throw new IllegalArgumentException("HMac algorithm not found for digest: " + algId.getId());
        }

        return hmac;
    }

    private String toDigestHint(AlgorithmIdentifier algorithmIdentifier)
    {
        if (algorithmIdentifier == null) {
            return null;
        }

        ASN1ObjectIdentifier algId = algorithmIdentifier.getAlgorithm();
        String hint = null;

        if (algId.equals(HMAC_SHA1.getAlgorithm())) {
            hint = X509ObjectIdentifiers.id_SHA1.getId();
        } else if (algId.equals(HMAC_SHA224.getAlgorithm())) {
            hint = NISTObjectIdentifiers.id_sha224.getId();
        } else if (algId.equals(HMAC_SHA256.getAlgorithm())) {
            hint = NISTObjectIdentifiers.id_sha256.getId();
        } else if (algId.equals(HMAC_SHA384.getAlgorithm())) {
            hint = NISTObjectIdentifiers.id_sha384.getId();
        } else if (algId.equals(HMAC_SHA512.getAlgorithm())) {
            hint = NISTObjectIdentifiers.id_sha512.getId();
        }
        if (hint == null) {
            throw new IllegalArgumentException("Digest hint not found for HMac algorithm: " + algId.getId());
        }

        return hint;
    }
}

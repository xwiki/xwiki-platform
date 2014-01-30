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
package org.xwiki.crypto.signer.internal.factory;

import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.RSASSAPSSparams;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.internal.digest.factory.BcDigestFactory;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyParameters;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.params.PssParameters;
import org.xwiki.crypto.signer.params.PssSignerParameters;

/**
 * Factory for SHA-1 Digest with RSA encryption signature processing.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component(hints = { "RSASSA-PSS", "1.2.840.113549.1.1.10" })
@Singleton
public class BcRsaSsaPssSignerFactory extends AbstractBcPssSignerFactory
{
    private static final String PSS_PARAMS_ERROR = "PSS signer parameters are invalid: ";

    @Override
    protected AsymmetricBlockCipher getCipherEngine()
    {
        return new RSABlindedEngine();
    }

    @Override
    public Signer getInstance(boolean forSigning, CipherParameters parameters, byte[] encoded)
    {
        AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(encoded);
        ASN1Encodable algParams = algId.getParameters();

        if (DERNull.INSTANCE.equals(algParams)) {
            return getInstance(forSigning, parameters);
        } else {
            RSASSAPSSparams pssParams = RSASSAPSSparams.getInstance(algId.getParameters());

            if (parameters instanceof AsymmetricKeyParameters) {
                return getInstance(forSigning, new PssSignerParameters((AsymmetricKeyParameters) parameters,
                    pssParams.getHashAlgorithm().getAlgorithm().getId(),
                    AlgorithmIdentifier
                        .getInstance(pssParams.getMaskGenAlgorithm().getParameters()).getAlgorithm().getId(),
                    pssParams.getSaltLength().intValue(),
                    pssParams.getTrailerField().intValue()));
            }
        }

        throw new UnsupportedOperationException(PSS_PARAMS_ERROR + parameters.getClass().getName());
    }

    @Override
    protected AlgorithmIdentifier getSignerAlgorithmIdentifier(AsymmetricCipherParameters parameters)
    {
        if (parameters instanceof AsymmetricKeyParameters) {
            AlgorithmIdentifier sha1AlgId = new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1);
            return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSASSA_PSS, DERNull.INSTANCE);
        } else if (parameters instanceof PssSignerParameters) {
            PssParameters pssParams = ((PssSignerParameters) parameters).getPssParameters();
            BcDigestFactory factory = getDigestFactory(pssParams.getHashAlgorithm());

            return new AlgorithmIdentifier(PKCSObjectIdentifiers.id_RSASSA_PSS, new RSASSAPSSparams(
                factory.getAlgorithmIdentifier(),
                new AlgorithmIdentifier(PKCSObjectIdentifiers.id_mgf1,
                    getDigestFactory(pssParams.getMaskGenAlgorithm()).getAlgorithmIdentifier()),
                new ASN1Integer(pssParams.getSaltLength() >= 0 ? pssParams.getSaltLength() : factory.getDigestSize()),
                new ASN1Integer(pssParams.getTrailerField())));
        }

        throw new UnsupportedOperationException(PSS_PARAMS_ERROR + parameters.getClass().getName());
    }
}

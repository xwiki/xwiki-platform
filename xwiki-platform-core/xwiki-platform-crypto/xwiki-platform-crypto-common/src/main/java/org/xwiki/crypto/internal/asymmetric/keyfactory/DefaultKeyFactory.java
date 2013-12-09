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
package org.xwiki.crypto.internal.asymmetric.keyfactory;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.ua.UAObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.dsa.DSAUtil;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.RSAUtil;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;

/**
 * Default asymmetric key factory.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
public class DefaultKeyFactory extends AbstractBcKeyFactory
{
    @Inject
    private ComponentManager manager;

    @Override
    protected AsymmetricKeyInfoConverter getKeyInfoConverter()
    {
        throw new UnsupportedOperationException("Unexpected illegal internal call");
    }

    @Override
    protected String checkKeyType(BcAsymmetricKeyParameters key)
    {
        // Not key type check for this generic factory.
        return null;
    }

    private AsymmetricKeyFactory getKeyFactory(ASN1Object keyInfo)
    {
        return getKeyFactory(getKeyFactoryHint(keyInfo));
    }

    private String getKeyFactoryHint(ASN1Object keyInfo)
    {
        ASN1ObjectIdentifier algId = getAlgorithmId(keyInfo);

        String hint = null;

        if (RSAUtil.isRsaOid(algId)) {
            hint = "RSA";
        } else if (DSAUtil.isDsaOid(algId)) {
            hint = "DSA";
        } else if (algId.equals(PKCSObjectIdentifiers.dhKeyAgreement)
                    || algId.equals(X9ObjectIdentifiers.dhpublicnumber)) {
            hint = "DH";
        } else if (algId.equals(OIWObjectIdentifiers.elGamalAlgorithm)) {
            hint = "ElGamal";
        } else if (algId.equals(CryptoProObjectIdentifiers.gostR3410_94)) {
            hint = "GOST3410";
        } else if (algId.equals(UAObjectIdentifiers.dstu4145le) || algId.equals(UAObjectIdentifiers.dstu4145be)) {
            hint = "DSTU";
        }

        if (hint == null) {
            throw new UnsupportedOperationException("Asymmetric key algorithm not supported: " + algId.getId());
        }

        return hint;
    }

    private ASN1ObjectIdentifier getAlgorithmId(ASN1Object keyInfo)
    {
        if (keyInfo instanceof PrivateKeyInfo) {
            return ((PrivateKeyInfo) keyInfo).getPrivateKeyAlgorithm().getAlgorithm();
        } else if (keyInfo instanceof SubjectPublicKeyInfo) {
            return ((SubjectPublicKeyInfo) keyInfo).getAlgorithm().getAlgorithm();
        } else {
            throw new IllegalArgumentException("Asymmetric key expected but received: " + keyInfo.getClass().getName());
        }
    }

    private AsymmetricKeyFactory getKeyFactory(String hint)
    {
        try {
            return manager.getInstance(AsymmetricKeyFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Asymmetric key algorithm not found.", e);
        }
    }

    //
    // AsymmetricKeyInfoConverter
    //

    @Override
    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException
    {
        AsymmetricKeyFactory factory = getKeyFactory(privateKeyInfo);

        if (factory instanceof AsymmetricKeyInfoConverter) {
            return ((AsymmetricKeyInfoConverter) factory).generatePrivate(privateKeyInfo);
        }

        return factory.toKey(fromPKCS8(privateKeyInfo.getEncoded()));
    }

    @Override
    public PublicKey generatePublic(SubjectPublicKeyInfo publicKeyInfo) throws IOException
    {
        AsymmetricKeyFactory factory = getKeyFactory(publicKeyInfo);

        if (factory instanceof AsymmetricKeyInfoConverter) {
            return ((AsymmetricKeyInfoConverter) factory).generatePublic(publicKeyInfo);
        }

        return factory.toKey(fromX509(publicKeyInfo.getEncoded()));
    }
}

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
package org.xwiki.crypto.pkix.internal;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Date;

import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.xwiki.crypto.internal.asymmetric.BcPublicKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.internal.extension.BcX509Extensions;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.internal.factory.BcSignerFactory;

/**
 * Generic implementation of X509CertifiedPublicKey wrapping a Bouncy Castle holder.
 *
 * @version $Id$
 * @since 5.4
 */
public class BcX509CertifiedPublicKey implements X509CertifiedPublicKey
{
    private final X509CertificateHolder holder;
    private final SignerFactory signerFactory;

    BcX509CertifiedPublicKey(X509CertificateHolder holder, SignerFactory signerFactory)
    {
        this.holder = holder;
        this.signerFactory = signerFactory;
    }

    /**
     * @return the native bouncy castle wrapped holder.
     */
    public X509CertificateHolder getX509CertificateHolder()
    {
        return holder;
    }

    @Override
    public DistinguishedName getIssuer()
    {
        return new DistinguishedName(holder.getIssuer());
    }

    @Override
    public DistinguishedName getSubject()
    {
        return new DistinguishedName(holder.getSubject());
    }

    @Override
    public Date getNotAfter()
    {
        return holder.getNotAfter();
    }

    @Override
    public Date getNotBefore()
    {
        return holder.getNotBefore();
    }

    @Override
    public int getVersionNumber()
    {
        return holder.getVersionNumber();
    }

    @Override
    public BigInteger getSerialNumber()
    {
        return holder.getSerialNumber();
    }

    @Override
    public boolean isValidOn(Date date)
    {
        return holder.isValidOn(date);
    }

    @Override
    public X509Extensions getExtensions()
    {
        return new BcX509Extensions(holder.getExtensions());
    }

    @Override
    public PublicKeyParameters getPublicKeyParameters()
    {
        try {
            return new BcPublicKeyParameters(PublicKeyFactory.createKey(holder.getSubjectPublicKeyInfo()));
        } catch (IOException e) {
            // Very unlikely
            throw new UnsupportedOperationException("Unsupported public key encoding.", e);
        }
    }

    @Override
    public boolean isSignedBy(PublicKeyParameters publicKey) throws GeneralSecurityException
    {
        TBSCertificate tbsCert = holder.toASN1Structure().getTBSCertificate();

        if (!BcUtils.isAlgorithlIdentifierEqual(tbsCert.getSignature(), holder.getSignatureAlgorithm())) {
            return false;
        }

        Signer signer = null;

        // Optimisation
        if (signerFactory instanceof BcSignerFactory) {
            signer = ((BcSignerFactory) signerFactory).getInstance(false, publicKey, tbsCert.getSignature());
        } else {
            try {
                signer = signerFactory.getInstance(false, publicKey, holder.getSignatureAlgorithm().getEncoded());
            } catch (IOException e) {
                return false;
            }
        }

        try {
            return BcUtils.updateDEREncodedObject(signer, tbsCert).verify(holder.getSignature());
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public byte[] getEncoded() throws IOException
    {
        return holder.getEncoded();
    }
}

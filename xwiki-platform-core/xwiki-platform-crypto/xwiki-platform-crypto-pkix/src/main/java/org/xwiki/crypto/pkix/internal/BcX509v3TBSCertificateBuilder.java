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
import java.util.Date;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.internal.extension.BcX509Extensions;
import org.xwiki.crypto.pkix.internal.extension.DefaultX509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.signer.Signer;

/**
 * To Be Signed version 3 certificate builder.
 *
 * @version $Id$
 * @since 5.4
 */
public class BcX509v3TBSCertificateBuilder implements X509TBSCertificateBuilder
{
    private final V3TBSCertificateGenerator tbsGen = new V3TBSCertificateGenerator();

    @Override
    public X509TBSCertificateBuilder setSerialNumber(BigInteger serial)
    {
        tbsGen.setSerialNumber(new ASN1Integer(serial));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setSubjectPublicKeyInfo(PublicKeyParameters subject)
    {
        tbsGen.setSubjectPublicKeyInfo(BcUtils.getSubjectPublicKeyInfo(subject));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setIssuer(PrincipalIndentifier issuer)
    {
        tbsGen.setIssuer(BcUtils.getX500Name(issuer));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setSubject(PrincipalIndentifier subject)
    {
        tbsGen.setSubject(BcUtils.getX500Name(subject));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setStartDate(Date time)
    {
        tbsGen.setStartDate(new Time(time));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setEndDate(Date time)
    {
        tbsGen.setEndDate(new Time(time));
        return this;
    }

    @Override
    public X509TBSCertificateBuilder setSignature(Signer signer)
    {
        tbsGen.setSignature(BcUtils.getSignerAlgoritmIdentifier(signer));
        return this;
    }

    @Override
    public TBSCertificate build()
    {
        return tbsGen.generateTBSCertificate();
    }

    /**
     * Set the extensions of a self-signed v3 certificate.
     *
     * @param subject the subject certified public key parameters to compute the Subject Key Identifier, or null for
     *                none.
     * @param extensions1 the common extensions set.
     * @param extensions2 the subject extensions set.
     * @return this builder to allow chaining.
     * @throws IOException on encoding error.
     */
    public BcX509v3TBSCertificateBuilder setExtensions(PublicKeyParameters subject,
        X509Extensions extensions1, X509Extensions extensions2) throws IOException
    {
        DefaultX509ExtensionBuilder extBuilder = new DefaultX509ExtensionBuilder();

        extBuilder.addAuthorityKeyIdentifier(subject)
            .addSubjectKeyIdentifier(subject)
            .addExtensions(extensions1)
            .addExtensions(extensions2);

        if (!extBuilder.isEmpty())
        {
            tbsGen.setExtensions(((BcX509Extensions) extBuilder.build()).getExtensions());
        }
        return this;
    }

    /**
     * Set the extensions of a v3 certificate.
     *
     * @param issuer the issuer certified public key to compute the Authority Key Identifier, or null for none.
     * @param subject the subject certified public key parameters to compute the Subject Key Identifier, or null for
     *                none.
     * @param extensions1 the common extensions set.
     * @param extensions2 the subject extensions set.
     * @return this builder to allow chaining.
     * @throws IOException on encoding error.
     */
    public BcX509v3TBSCertificateBuilder setExtensions(CertifiedPublicKey issuer, PublicKeyParameters subject,
        X509Extensions extensions1, X509Extensions extensions2) throws IOException
    {
        DefaultX509ExtensionBuilder extBuilder = new DefaultX509ExtensionBuilder();



        extBuilder.addAuthorityKeyIdentifier(issuer)
            .addSubjectKeyIdentifier(subject)
            .addExtensions(extensions1)
            .addExtensions(extensions2);

        if (!extBuilder.isEmpty())
        {
            tbsGen.setExtensions(((BcX509Extensions) extBuilder.build()).getExtensions());
        }
        return this;
    }
}

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

import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V1TBSCertificateGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.signer.Signer;

/**
 * To Be Signed version 1 certificate builder.
 *
 * @version $Id$
 * @since 5.4
 */
public class BcX509v1TBSCertificateBuilder implements X509TBSCertificateBuilder
{
    private final V1TBSCertificateGenerator tbsGen = new V1TBSCertificateGenerator();

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
}

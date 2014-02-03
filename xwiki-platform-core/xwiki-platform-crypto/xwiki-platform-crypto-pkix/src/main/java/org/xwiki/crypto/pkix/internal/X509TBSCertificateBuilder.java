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

import org.bouncycastle.asn1.x509.TBSCertificate;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.signer.Signer;

/**
 * Common interface for X.509 TBS builders.
 *
 * @version $Id$
 * @since 5.4
 */
public interface X509TBSCertificateBuilder
{
    /**
     * Set the serialNumber value.
     *
     * @param serial the serialNumber value.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setSerialNumber(BigInteger serial);

    /**
     * Set the public key of the subject.
     *
     * @param subject public key parameters.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setSubjectPublicKeyInfo(PublicKeyParameters subject);

    /**
     * Set the issuer (subject distinguished name) value.
     *
     * @param issuer a principal identifier.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setIssuer(PrincipalIndentifier issuer);

    /**
     * Set the subject (subject distinguished name) value.
     *
     * @param subject a principal identifier.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setSubject(PrincipalIndentifier subject);

    /**
     * Set the date before which the certificate is not valid.
     *
     * @param time a date.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setStartDate(Date time);

    /**
     * Set the date after which the certificate is not valid.
     *
     * @param time a date.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setEndDate(Date time);

    /**
     * Set the signature algorithm.
     *
     * @param signer the signer that will be used to sign the certificate.
     * @return this builder to allow chaining.
     */
    X509TBSCertificateBuilder setSignature(Signer signer);

    /**
     * Build the TBS certificate.
     *
     * @return a TBS certificate.
     */
    TBSCertificate build();
}

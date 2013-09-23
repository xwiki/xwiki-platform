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
package org.xwiki.crypto.x509;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.xwiki.crypto.internal.Convert;
import org.xwiki.crypto.x509.internal.AbstractX509CertificateWrapper;


/**
 * X509 certificate wrapper with several additional helper methods, aimed to be more scripting-friendly.
 *
 * This class cannot be an interface because it extends AbstractX509CertificateWrapper which extends X509Certificate
 * which is not an interface. Most bouncycastle code requires an X509Certificate so if we used an interface then
 * it would just have to be casted every time somebody wanted to use it with non xwiki-crypto cryptographic apis.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class XWikiX509Certificate extends AbstractX509CertificateWrapper
{
    /** Supported certificate type. */
    private static final String CERT_TYPE = "X509";

    /** Digest algorithm used to generate the fingerprint. */
    private static final String FINGERPRINT_ALGORITHM = "SHA1";

    /** Marks the beginning of a certificate in PEM format. */
    private static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----";

    /** Marks the end of a certificate in PEM format. */
    private static final String CERT_END = "-----END CERTIFICATE-----";

    /** Certificate fingerprint. */
    private final String fingerprint;

    /** Certificate fingrprint of the issuer. */
    private final String issuerFingerprint;

    /**
     * Create new {@link XWikiX509Certificate}. Assume that the certificate is self-signed.
     * 
     * @param certificate the actual certificate to use
     */
    public XWikiX509Certificate(X509Certificate certificate)
    {
        this(certificate, null);
    }

    /**
     * Create new {@link XWikiX509Certificate}.
     * 
     * @param certificate the actual certificate to use
     * @param issuerFp fingerprint of the issuer certificate, null if self-signed
     */
    public XWikiX509Certificate(X509Certificate certificate, String issuerFp)
    {
        super(certificate);
        this.fingerprint = XWikiX509Certificate.calculateFingerprint(certificate);
        if (issuerFp == null && (certificate instanceof XWikiX509Certificate)) {
            this.issuerFingerprint = ((XWikiX509Certificate) certificate).getIssuerFingerprint();
        } else if (issuerFp == null) {
            this.issuerFingerprint = this.fingerprint;
        } else {
            this.issuerFingerprint = issuerFp;
        }
    }

    /**
     * Calculate the fingerprint of the given certificate. Throws a {@link RuntimeException} on errors.
     * 
     * @param certificate the certificate to use
     * @return certificate fingerprint in hex
     */
    public static String calculateFingerprint(Certificate certificate)
    {
        try {
            MessageDigest hash = MessageDigest.getInstance(FINGERPRINT_ALGORITHM);
            BigInteger result = new BigInteger(1, hash.digest(certificate.getEncoded()));
            return String.format("%0" + hash.getDigestLength() * 2 + "x", result);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public int hashCode()
    {
        return this.fingerprint.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj instanceof XWikiX509Certificate) {
            XWikiX509Certificate cert = (XWikiX509Certificate) obj;
            return getFingerprint().equals(cert.getFingerprint());
        }
        return false;
    }

    @Override
    public String toString()
    {
        final String format = "%20s : %s\n";
        StringBuilder builder = new StringBuilder();
        builder.append("XWikiX509Certificate\n");
        builder.append("---------------------------------------------------------------\n");
        builder.append(String.format(format, "Fingerprint", getFingerprint()));
        builder.append(String.format(format, "SubjectDN", getAuthorName()));
        builder.append(String.format(format, "IssuerDN", getIssuerName()));
        builder.append(String.format(format, "Issuer Fingerprint", getIssuerFingerprint()));
        builder.append(String.format(format, "SerialNumber", getSerialNumber().toString(16)));
        builder.append(String.format(format, "Start Date", getNotBefore()));
        builder.append(String.format(format, "Final Date", getNotAfter()));
        builder.append(String.format(format, "Public Key Algorithm", getPublicKey().getAlgorithm()));
        builder.append(String.format(format, "Signature Algorithm", getSigAlgName()));
        try {
            builder.append(this.toPEMString());
        } catch (CertificateEncodingException exception) {
            // ignore
        }
        return builder.toString();
    }

    /**
     * @return the fingerprint
     */
    public String getFingerprint()
    {
        return fingerprint;
    }

    /**
     * Get the internal X509 certificate in a standard PEM format.
     * 
     * @return the certificate in PEM format
     * @throws CertificateEncodingException on errors
     * @see XWikiX509Certificate#fromPEMString()
     */
    public String toPEMString() throws CertificateEncodingException
    {
        StringBuilder builder = new StringBuilder();
        builder.append(CERT_BEGIN);
        builder.append(Convert.getNewline());
        builder.append(Convert.toChunkedBase64String(this.certificate.getEncoded()));
        builder.append(CERT_END);
        builder.append(Convert.getNewline());
        return builder.toString();
    }

    /**
     * Constructor from a PEM formatted string.
     * This constructor will search the given string until it finds {@link XWikiX509Certificate#CERT_BEGIN} and assume
     * everything until the next {@link XWikiX509Certificate#CERT_END} is a valid PEM formatted certificate. If there
     * are multiple certificates in the passed string the first will be parsed, its issuer fingerprint will be set to
     * the fingerprint of the second certificate and all subsequent certificates will be ignored.
     * 
     * @param pemEncoded a String containing an X509 certificate in PEM format
     * @throws GeneralSecurityException If there isn't a valid {@link XWikiX509Certificate#CERT_BEGIN} or
     *                                  {@link XWikiX509Certificate#CERT_END} tag, or if there is an exception parsing
     *                                  the content inbetween.
     * @return an XWikiX509Certificate from the PEM input.
     * @see XWikiX509Certificate#toPEMString()
     */
    public static XWikiX509Certificate fromPEMString(String pemEncoded) throws GeneralSecurityException
    {
        final byte[] base64Bytes = Convert.fromBase64String(pemEncoded, CERT_BEGIN, CERT_END);
        final CertificateFactory factory = CertificateFactory.getInstance(CERT_TYPE);
        final Certificate cert = factory.generateCertificate(new ByteArrayInputStream(base64Bytes));
        if (!(cert instanceof X509Certificate)) {
            throw new GeneralSecurityException("Unsupported certificate type: " + cert.getType());
        }
        // if there is a second certificate, use it to calculate correct issuer fingerprint
        int second = pemEncoded.indexOf(CERT_BEGIN, pemEncoded.indexOf(CERT_END));
        if (second > 0) {
            byte[] cert2 = Convert.fromBase64String(pemEncoded.substring(second), CERT_BEGIN, CERT_END);
            try {
                String issuerFp = calculateFingerprint(factory.generateCertificate(new ByteArrayInputStream(cert2)));
                return new XWikiX509Certificate((X509Certificate) cert, issuerFp);
            } catch (GeneralSecurityException exception) {
                // completely ignore the second certificate on error
            }
        }
        // assume self-signed certificate by default
        return new XWikiX509Certificate((X509Certificate) cert);
    }

    /**
     * Convert a chain of {@link Certificate}s into a chain of {@link XWikiX509Certificate}s, correctly setting the
     * issuer fingerprint. The last certificate in the chain is assumed to be self-signed.
     * <p>
     * Each certificate in the input chain must be a subclass of {@link X509Certificate}, otherwise a runtime exception
     * is thrown (the type is Certificate[] and not X509Certificate[] just for convenience, since certificate factories
     * create certificate chains of this type).</p>
     * 
     * @param x509Chain a chain if X509 certificates
     * @return a corresponding chain of {@link XWikiX509Certificate}s wrapping the certificates from the input chain
     */
    public static XWikiX509Certificate[] fromCertificateChain(Certificate[] x509Chain)
    {
        XWikiX509Certificate[] outChain = new XWikiX509Certificate[x509Chain.length];
        String issuerFP = null;
        // go through the chain backwards so that we don't need to recalculate the issuer fingerprint
        for (int i = x509Chain.length - 1; i >= 0; i--) {
            if (!(x509Chain[i] instanceof X509Certificate)) {
                throw new IllegalArgumentException("Only X509 certificates are supported, found: "
                        + x509Chain[i].getType());
            }
            outChain[i] = new XWikiX509Certificate((X509Certificate) x509Chain[i], issuerFP);
            issuerFP = outChain[i].getFingerprint();
        }
        return outChain;
    }

    /**
     * Get issuer name of this certificate. Same as {@link #getAuthorName()} of the certificate
     * obtained via {@link #getIssuerFingerprint()}.
     * 
     * @return issuer name
     */
    public String getIssuerName()
    {
        return getIssuerX500Principal().getName();
    }

    /**
     * Get the fingerprint of the issuer certificate.
     * 
     * @return issuer fingerprint
     */
    public String getIssuerFingerprint()
    {
        return issuerFingerprint;
    }

    /**
     * Get name of the author (subject name) of this certificate.
     *  
     * @return author name
     */
    public String getAuthorName()
    {
        return getSubjectX500Principal().getName();
    }

    /**
     * Get user name (stored as UID in the distinguished subject name) of this certificate's author, or empty string
     * if UID is not present.
     * 
     * @return author UID
     */
    public String getAuthorUID()
    {
        try {
            X509Principal author = PrincipalUtil.getSubjectX509Principal(this.certificate);
            if (author.getValues(X509Name.UID).size() == 0) {
                return "";
            }
            return (String) author.getValues(X509Name.UID).get(0);
        } catch (CertificateEncodingException exception) {
            // should not happen
            return "";
        }
    }
}


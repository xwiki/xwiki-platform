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
package org.xwiki.crypto.x509.internal;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Vector;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.JDKKeyPairGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;


/**
 * Keymaker allows you to create keypairs and X509Certificates.
 * 
 * @version $Id$
 * @since 2.5
 */
public class X509Keymaker
{
    /** The name used for the heading underwhich all of the generated CA certificates will show in the browser. */
    private static final String CA_ORGANIZATION_NAME = "Fake authorities for trusting client certificates";

    /** A certificate generator. Use of this must be synchronized. */
    private final X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();

    /** A key pair generator. Use of this must be synchronized. */
    private final JDKKeyPairGenerator.RSA keyPairGen = new JDKKeyPairGenerator.RSA();

    /** Milliseconds in an hour. */
    private final long anHour = 60 * 60 * 1000L;

    /** Milliseconds in a day. */
    private final long aDay = 24 * anHour;

    /** Signature algorithm to use. */
    private final String certSignatureAlgorithm = "SHA1WithRSAEncryption";

    /** If this is set then it will be used to sign all client keys. */
    private KeyPair authorityKeyPair;

    /** If this is set then it will be returned by the script service with all client certificates. */
    private X509Certificate authorityCertificate;

    /** Make sure the BouncyCastle provider is added to java security providers. */
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /** @return a newly generated RSA KeyPair. */
    public KeyPair newKeyPair()
    {
        return this.keyPairGen.generateKeyPair();
    }

    /**
     * If called then all future client certificates will be signed with this KeyPair.
     * Excluding reflection, you can be assured that the KeyPair set here will not leave this object.
     *
     * @param authorityKeyPair the KeyPair to sign all client keys with.
     */
    public void setAuthorityKeyPair(KeyPair authorityKeyPair)
    {
        this.authorityKeyPair = authorityKeyPair;
    }

    /**
     * If called then all future client certificates will be packaged with this certificate authority.
     * It's important that this certificate is either the same public key as authorityKeyPair or the holder of
     * this certificate has signed the certificate associated with authorityKeyPair.
     *
     * @param authorityCertificate the certificate authority to provide with client certificates.
     */
    public void setAuthorityCertificate(X509Certificate authorityCertificate)
    {
        this.authorityCertificate = authorityCertificate;
    }

    /** @return the certificate authority designated for providing with client certificates. */
    public X509Certificate getAuthorityCertificate()
    {
        return this.authorityCertificate;
    }

    /**
     * Create a new X509 client certificate and a certificate authority certificate.
     * This method will use authorityKeyPair if it is set, this method is also guaranteed to use the same
     * authorityKeyPair for both the client cert signature and the CA cert.
     *
     * @param forCert the public key which will be embedded in the certificate, whoever has the matching private key
     *                "owns" the certificate.
     * @param daysOfValidity number of days the cert should be valid for.
     * @param nonRepudiable this should only be true if the private key is not stored on the server.
     * @param webId the URI to put as the alternative name (for FOAFSSL webId compatibility)
     * @param userName a String representation of the name of the user getting the certificate.
     * @return an array of 2 new X509 certificates, with the client certificate at 0-th index, and CA cert at 1-st index
     * @throws GeneralSecurityException if something goes wrong.
     */
    public synchronized X509Certificate[] makeClientAndAuthorityCertificates(final PublicKey forCert,
                                                                             final int daysOfValidity,
                                                                             final boolean nonRepudiable,
                                                                             final String webId,
                                                                             final String userName)
        throws GeneralSecurityException
    {
        KeyPair auth = this.authorityKeyPair;
        if (auth == null) {
            auth = this.newKeyPair();
        }
        final X509Certificate[] out = new X509Certificate[2];
        out[0] = this.makeClientCertificate(forCert, auth, daysOfValidity, nonRepudiable, webId, userName);
        out[1] = this.getAuthorityCertificate();
        if (out[1] == null) {
            out[1] = this.makeCertificateAuthority(auth, daysOfValidity, webId);
        }
        return out;
    }

    /**
     * Create a new X509 client certificate.
     *
     * @param forCert the public key which will be embedded in the certificate, whoever has the matching private key
     *                "owns" the certificate.
     * @param toSignWith the private key in this pair will be used to sign the certificate.
     * @param daysOfValidity number of days the cert should be valid for.
     * @param nonRepudiable this should only be true if the private key is not stored on the server.
     * @param webId the URI to put as the alternative name (for FOAFSSL webId compatibility)
     * @param userName a String representation of the name of the user getting the certificate.
     * @return a new X509 certificate.
     * @throws GeneralSecurityException if something goes wrong.
     */
    public synchronized X509Certificate makeClientCertificate(final PublicKey forCert,
                                                              final KeyPair toSignWith,
                                                              final int daysOfValidity,
                                                              final boolean nonRepudiable,
                                                              final String webId,
                                                              final String userName)
        throws GeneralSecurityException
    {
        try {
            // the UID (same for issuer since this certificate confers no authority)
            final X509Name dName = new X509Name("UID=" + userName);

            this.prepareGenericCertificate(forCert, daysOfValidity, dName, dName);

            // Not a CA
            certGenerator.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));

            // Client cert
            certGenerator.addExtension(MiscObjectIdentifiers.netscapeCertType,
                                       false,
                                       new NetscapeCertType(NetscapeCertType.sslClient | NetscapeCertType.smime));

            // Key Usage extension.
            int keyUsage =   KeyUsage.digitalSignature
                           | KeyUsage.keyEncipherment
                           | KeyUsage.dataEncipherment
                           | KeyUsage.keyAgreement;
            if (nonRepudiable) {
                keyUsage |= KeyUsage.nonRepudiation;
            }
            certGenerator.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(keyUsage));

            // Set the authority key identifier to be the CA key which we are using.
            certGenerator.addExtension(X509Extensions.AuthorityKeyIdentifier,
                                       false,
                                       new AuthorityKeyIdentifierStructure(toSignWith.getPublic()));

            // FOAFSSL compatibility.
            final GeneralNames subjectAltNames =
                new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, webId));
            certGenerator.addExtension(X509Extensions.SubjectAlternativeName, true, subjectAltNames);

            return this.generate(toSignWith);

        } finally {
            // Clean up after ourselves so that it is more difficult to try to extract private keys from the heap.
            this.certGenerator.reset();
        }
    }

    /**
     * Create a new self signed X509 certificate authority certificate.
     *
     * @param keyPair the public key will appear in the certificate and the private key will be used to sign it.
     * @param daysOfValidity number of days the cert should be valid for.
     * @param commonName what to put in the common name field, this field will identify this certificate authority
     *                   in the list on the user's browser.
     * @return a new X509 certificate authority.
     * @throws GeneralSecurityException if something goes wrong.
     */
    public synchronized X509Certificate makeCertificateAuthority(final KeyPair keyPair,
                                                                 final int daysOfValidity,
                                                                 final String commonName)
        throws GeneralSecurityException
    {
        try {
            final X509Name name = new X509Name(
                new Vector<DERObjectIdentifier>() {
                    {
                        this.add(X509Name.O);
                        this.add(X509Name.CN);
                    }
                },
                new Vector<String>() {
                    {
                        this.add(X509Keymaker.CA_ORGANIZATION_NAME);
                        this.add(commonName);
                    }
                });

            this.prepareGenericCertificate(keyPair.getPublic(), daysOfValidity, name, name);

            // This authority can't sign other CA's.
            certGenerator.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(0));

            // Allow certificate signing only.
            certGenerator.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.keyCertSign));

            // Adds the subject key identifier extension. Self singed so uses it's own key.
            certGenerator.addExtension(X509Extensions.SubjectKeyIdentifier,
                                       false,
                                       new SubjectKeyIdentifierStructure(keyPair.getPublic()));

            return this.generate(keyPair);
        } finally {
            // Clean up after ourselves so that it is more difficult to try to extract private keys from the heap.
            this.certGenerator.reset();
        }
    }

    /**
     * Prepare the certificate generator to generate a generic certificate.
     *
     * @param forCert the public key will appear in the certificate.
     * @param daysOfValidity number of days the cert should be valid for.
     * @param subjectDN subject name
     * @param issuerDN issuer name
     */
    private synchronized void prepareGenericCertificate(final PublicKey forCert,
                                                        final int daysOfValidity,
                                                        final X509Name subjectDN,
                                                        final X509Name issuerDN)
    {
        // We reset and use a "shared" cert generator which is why this method is synchronized.
        this.certGenerator.reset();

        // Set up the validity dates.
        this.certGenerator.setNotBefore(new Date(System.currentTimeMillis() - this.anHour));
        this.certGenerator.setNotAfter(new Date(System.currentTimeMillis() + (this.aDay * daysOfValidity)));

        // Set a serial number to the current time.
        this.certGenerator.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()).abs());

        // Set public key and algorithm.
        this.certGenerator.setPublicKey(forCert);
        this.certGenerator.setSignatureAlgorithm(this.certSignatureAlgorithm);

        // set subject and issuer names
        this.certGenerator.setSubjectDN(subjectDN);
        this.certGenerator.setIssuerDN(issuerDN);
    }

    /**
     * Makes the current certificate in the cert generator.
     *
     * @param toSignWith the private key in this pair will be used to sign the certificate.
     * @return a new X509 certificate.
     * @throws GeneralSecurityException if something goes wrong.
     */
    private synchronized X509Certificate generate(final KeyPair toSignWith)
        throws GeneralSecurityException
    {
        // Creates and sign this certificate.
        final X509Certificate cert = this.certGenerator.generate(toSignWith.getPrivate());

        // Checks that this certificate has indeed been correctly signed.
        cert.verify(toSignWith.getPublic());

        return cert;
    }
}

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
package org.xwiki.crypto.script;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.internal.RSACryptoHelper;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.params.generator.asymmetric.RSAKeyGenerationParameters;
import org.xwiki.crypto.pkix.CertificateChainBuilder;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.crypto.signer.CMSSignedDataGenerator;
import org.xwiki.crypto.signer.CMSSignedDataVerifier;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.script.service.ScriptService;

/**
 * Script service allowing a user to create keys pairs and issue certificates.
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Named(CryptoScriptService.ROLEHINT + '.' + RSACryptoScriptService.ROLEHINT)
@Singleton
public class RSACryptoScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "rsa";

    @Inject
    @Named("RSA")
    private KeyPairGenerator keyPairGenerator;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Inject
    private CMSSignedDataGenerator cmsSignedDataGenerator;

    @Inject
    @Named("X509")
    private CertificateChainBuilder certificateChainBuilder;

    @Inject
    private CMSSignedDataVerifier cmsSignedDataVerifier;

    @Inject
    private RSACryptoHelper rsaCryptoHelper;

    /**
     * Generate a new RSA key pair.
     *
     * The key strength will be {@value RSAKeyGenerationParameters#DEFAULT_STRENGTH}.
     * The key public exponent will be 0x10001.
     * The probability a chosen prime could not be a real prime will be smaller
     * than 2^-{@value RSAKeyGenerationParameters#DEFAULT_CERTAINTY}.
     *
     * @return an new asymmetric key pair.
     */
    public AsymmetricKeyPair generateKeyPair()
    {
        return keyPairGenerator.generate();
    }

    /**
     * Generate a new RSA key pair of given strength. The strength should be given in number of bytes, so for
     * a 2048 bits key, you should use 256 (bytes) as the integer parameter. The minimum valid strength is 2.
     *
     * The key public exponent will be 0x10001.
     * The probability a chosen prime could not be a real prime will be smaller
     * than 2^-{@value RSAKeyGenerationParameters#DEFAULT_CERTAINTY}.
     *
     * @param strength the strength in bytes.
     * @return an new asymmetric key pair.
     */
    public AsymmetricKeyPair generateKeyPair(int strength)
    {
        return keyPairGenerator.generate(new RSAKeyGenerationParameters(strength));
    }

    /**
     * Build a new instance with all custom parameters. The strength
     * should be given in number of bytes, so for a 2048 bits key, you should use 256 (bytes) as the integer parameter.
     * The minimum valid strength is 2. The exponent should be an odd number. The probability a chosen prime could
     * not be a real prime will be smaller than 2^certainty.
     *
     * @param strength the key strength in bytes.
     * @param publicExponent the public exponent.
     * @param certainty certainty for prime evaluation.
     *
     * @return an new asymmetric key pair.
     */
    public AsymmetricKeyPair generateKeyPair(int strength, BigInteger publicExponent, int certainty)
    {
        return keyPairGenerator.generate(new RSAKeyGenerationParameters(strength, publicExponent, certainty));
    }

    /**
     * Create a CertifiedKeyPair from a private key and a certificate.
     *
     * @param privateKey the private key.
     * @param certificate the certified public key.
     * @return a certified key pair.
     */
    public CertifiedKeyPair createCertifiedKeyPair(PrivateKeyParameters privateKey, CertifiedPublicKey certificate)
    {
        return new CertifiedKeyPair(privateKey, certificate);
    }

    /**
     * Create a self-signed certificate for a Root CA.
     *
     * @param keyPair the keypair to issue the certificate for and used for signing it.
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @return a certified public key.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedKeyPair issueRootCACertificate(AsymmetricKeyPair keyPair, String dn, int validity)
        throws IOException, GeneralSecurityException
    {
        return this.rsaCryptoHelper.issueRootCACertificate(keyPair, dn, validity);
    }

    /**
     * Create an intermediate CA certificate.
     *
     * @param issuer the certified keypair for issuing the certificate
     * @param keyPair the keyPair of the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @return a certified keypair.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedKeyPair issueIntermediateCertificate(CertifiedKeyPair issuer, AsymmetricKeyPair keyPair,
        String dn, int validity)
        throws IOException, GeneralSecurityException
    {
        return new CertifiedKeyPair(
            keyPair.getPrivate(),
            issueIntermediateCertificate(issuer, keyPair.getPublic(), dn, validity)
        );
    }

    /**
     * Create an intermediate CA certificate.
     *
     * @param privateKey the private key for signing the certificate
     * @param issuer the certificate of the issuer of the certificate
     * @param publicKey the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @return a certified public key.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedPublicKey issueIntermediateCertificate(PrivateKeyParameters privateKey, CertifiedPublicKey issuer,
        PublicKeyParameters publicKey, String dn, int validity)
        throws IOException, GeneralSecurityException
    {
        return issueIntermediateCertificate(new CertifiedKeyPair(privateKey, issuer), publicKey, dn, validity);
    }

    /**
     * Create an intermediate CA certificate.
     *
     * @param issuer the certified keypair for issuing the certificate
     * @param publicKey the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @return a certified public key.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedPublicKey issueIntermediateCertificate(CertifiedKeyPair issuer, PublicKeyParameters publicKey,
        String dn, int validity)
        throws IOException, GeneralSecurityException
    {
        return this.rsaCryptoHelper.issueIntermediateCertificate(issuer, publicKey, dn, validity);
    }

    /**
     * Create an end entity certificate.
     *
     * @param issuer the certified keypair for issuing the certificate
     * @param keyPair the keyPair of the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @param subjectAltName the alternative names for the certificate
     * @return a certified keypair.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedKeyPair issueCertificate(CertifiedKeyPair issuer, AsymmetricKeyPair keyPair, String dn,
        int validity, List<X509GeneralName> subjectAltName) throws IOException, GeneralSecurityException
    {
        return new CertifiedKeyPair(
            keyPair.getPrivate(),
            issueCertificate(issuer, keyPair.getPublic(), dn, validity, subjectAltName)
        );
    }

    /**
     * Create an end entity certificate.
     *
     * @param privateKey the private key for signing the certificate
     * @param issuer the certificate of the issuer of the certificate
     * @param publicKey the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @param subjectAltName the alternative names for the certificate
     * @return a certified public key.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedPublicKey issueCertificate(PrivateKeyParameters privateKey, CertifiedPublicKey issuer,
        PublicKeyParameters publicKey, String dn, int validity, List<X509GeneralName> subjectAltName)
        throws IOException, GeneralSecurityException
    {
        return issueCertificate(new CertifiedKeyPair(privateKey, issuer), publicKey, dn, validity, subjectAltName);
    }

    /**
     * Create an end entity certificate. By default, the key can be used for encryption and signing. If the end entity
     * contains some alternate subject names of type X509Rfc822Name a extended email protection usage is added. If the
     * end entity contains some alternate subject names of type X509DnsName or X509IpAddress extended server and client
     * authentication usages are added.
     *
     * @param issuer the keypair for issuing the certificate
     * @param publicKey the public key to certify
     * @param dn the distinguished name for the new the certificate.
     * @param validity the validity of the certificate from now in days.
     * @param subjectAltName the alternative names for the certificate
     * @return a certified public key.
     * @throws IOException in case on error while reading the public key.
     * @throws GeneralSecurityException in case of error.
     */
    public CertifiedPublicKey issueCertificate(CertifiedKeyPair issuer, PublicKeyParameters publicKey,
        String dn, int validity, List<X509GeneralName> subjectAltName) throws IOException, GeneralSecurityException
    {
        return this.rsaCryptoHelper.issueCertificate(issuer, publicKey, dn, validity, subjectAltName);
    }

    /**
     * Generate a CMS (Cryptographic Message Syntax) signature for a given byte content. The resulting signature
     * might contains the content itself.
     *
     * @param data the data to be signed
     * @param keyPair the certified key pair used for signing
     * @param embedContent if true, the signed content is embedded with the signature.
     * @return the resulting signature encoded ASN.1 and in accordance with RFC 3852.
     * @throws GeneralSecurityException on error.
     */
    public byte[] cmsSign(byte[] data, CertifiedKeyPair keyPair, boolean embedContent)
        throws GeneralSecurityException
    {
        return cmsSign(data, keyPair, null, null, embedContent);
    }

    /**
     * Generate a CMS (Cryptographic Message Syntax) signature for a given byte content. The resulting signature
     * might contains the content itself and the certificate chain of the key used to sign.
     *
     * @param data the data to be signed
     * @param keyPair the certified key pair used for signing
     * @param certificateProvider Optionally, a certificate provider for obtaining the chain of certificate to embed.
     *                            If null, no certificate are embedded with the signature.
     * @param embedContent if true, the signed content is embedded with the signature.
     * @return the resulting signature encoded ASN.1 and in accordance with RFC 3852.
     * @throws GeneralSecurityException on error.
     */
    public byte[] cmsSign(byte[] data, CertifiedKeyPair keyPair, CertificateProvider certificateProvider,
        boolean embedContent) throws GeneralSecurityException
    {
        return cmsSign(data, keyPair, certificateProvider, null, embedContent);
    }

    /**
     * Generate a CMS (Cryptographic Message Syntax) signature for a given byte content. The resulting signature
     * might contains the content itself and the certificate chain of the key used to sign.
     *
     * @param data the data to be signed
     * @param keyPair the certified key pair used for signing
     * @param certificateProvider Optionally, a certificate provider for obtaining the chain of certificate to embed.
     *                            If null, no certificate are embedded with the signature.
     * @param existingSignature if not null, a existing signature on the same data that should be kept.
     * @param embedContent if true, the signed content is embedded with the signature.
     * @return the resulting signature encoded ASN.1 and in accordance with RFC 3852.
     * @throws GeneralSecurityException on error.
     */
    public byte[] cmsSign(byte[] data, CertifiedKeyPair keyPair, CertificateProvider certificateProvider,
        CMSSignedDataVerified existingSignature, boolean embedContent) throws GeneralSecurityException
    {
        return this.rsaCryptoHelper.cmsSign(data, keyPair, certificateProvider, existingSignature, embedContent);
    }

    /**
     * Verify a CMS signature with embedded content and containing all the certificate required for validation.
     *
     * @param signature the CMS signature to verify. The signature should have the signed content embedded as well as
     *                  all the certificates for the signers.
     * @return result of the verification.
     * @throws GeneralSecurityException on error.
     */
    public CMSSignedDataVerified cmsVerify(byte[] signature)
        throws GeneralSecurityException
    {
        return cmsSignedDataVerifier.verify(signature);
    }

    /**
     * Verify a CMS signature without embedded content but containing all the certificate required for validation.
     *
     * @param signature the CMS signature to verify.
     * @param data the content to verify the signature against, or null of the content is embedded in the signature.
     * @return a the result of the verification.
     * @throws GeneralSecurityException on error.
     */
    public CMSSignedDataVerified cmsVerify(byte[] signature, byte[] data)
        throws GeneralSecurityException
    {
        return cmsSignedDataVerifier.verify(signature, data);
    }

    /**
     * Verify a CMS signature with embedded content, but requiring external certificates to be validated.
     *
     * @param signature the CMS signature to verify.
     * @param certificateProvider Optionally, a certificate provider for obtaining the chain of certificate for
     *                            verifying the signatures. If null, certificat should all be embedded in the signature.
     * @return a the result of the verification.
     * @throws GeneralSecurityException on error.
     */
    public CMSSignedDataVerified cmsVerify(byte[] signature, CertificateProvider certificateProvider)
        throws GeneralSecurityException
    {
        return cmsSignedDataVerifier.verify(signature, certificateProvider);
    }

    /**
     * Verify a CMS signature without embedded content, and requiring external certificates to be validated.
     *
     * @param signature the CMS signature to verify.
     * @param data the content to verify the signature against, or null of the content is embedded in the signature.
     * @param certificateProvider Optionally, a certificate provider for obtaining the chain of certificate for
     *                            verifying the signatures. If null, certificat should all be embedded in the signature.
     * @return a the result of the verification.
     * @throws GeneralSecurityException on error.
     */
    public CMSSignedDataVerified cmsVerify(byte[] signature, byte[] data, CertificateProvider certificateProvider)
        throws GeneralSecurityException
    {
        return cmsSignedDataVerifier.verify(signature, data, certificateProvider);
    }

    /**
     * Check that an X509 certificate chain is complete and valid now.
     *
     * @param chain the ordered chain of certificate starting from root CA.
     * @return true if the chain is a X509 certificate chain complete and valid on the given date.
     */
    public boolean checkX509CertificateChainValidity(Collection<CertifiedPublicKey> chain)
    {
        return checkX509CertificateChainValidity(chain, null);
    }

    /**
     * Check that an X509 certificate chain is complete and is valid on a given date.
     *
     * @param chain the ordered chain of certificate starting from root CA.
     * @param date the date to check the validity for, or null to check for now.
     * @return true if the chain is a X509 certificate chain complete and valid on the given date.
     */
    public boolean checkX509CertificateChainValidity(Collection<CertifiedPublicKey> chain, Date date)
    {
        return this.rsaCryptoHelper.checkX509CertificateChainValidity(chain, date);
    }
}

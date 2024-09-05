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

package org.xwiki.crypto.internal;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.CertificateChainBuilder;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.CertifyingSigner;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DnsName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509IpAddress;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Rfc822Name;
import org.xwiki.crypto.signer.CMSSignedDataGenerator;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.crypto.signer.param.CMSSignedDataVerified;
import org.xwiki.crypto.signer.param.CMSSignerInfo;

/**
 * Helper component for operations provided in {@link org.xwiki.crypto.script.RSACryptoScriptService}.
 *
 * @version $Id$
 * @since 16.8.0RC1
 */
@Component(roles = RSACryptoHelper.class)
@Singleton
public class RSACryptoHelper
{
    @Inject
    private Provider<X509ExtensionBuilder> extensionBuilder;

    @Inject
    @Named("X509")
    private CertificateGeneratorFactory certificateGeneratorFactory;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Inject
    private CMSSignedDataGenerator cmsSignedDataGenerator;

    @Inject
    @Named("X509")
    private CertificateChainBuilder certificateChainBuilder;

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
        X509CertificateParameters params;
        X509ExtensionBuilder builder = extensionBuilder.get().addKeyUsage(EnumSet.of(KeyUsage.digitalSignature,
            KeyUsage.dataEncipherment));

        if (subjectAltName != null) {
            params = new X509CertificateParameters(
                extensionBuilder.get().addSubjectAltName(false, subjectAltName.toArray(new X509GeneralName[]{}))
                    .build());
            Set<String> extUsage = new HashSet<>();
            for (X509GeneralName genName : subjectAltName) {
                if (genName instanceof X509Rfc822Name) {
                    extUsage.add(ExtendedKeyUsages.EMAIL_PROTECTION);
                } else if (genName instanceof X509DnsName || genName instanceof X509IpAddress) {
                    extUsage.add(ExtendedKeyUsages.SERVER_AUTH);
                    extUsage.add(ExtendedKeyUsages.CLIENT_AUTH);
                }
                builder.addExtendedKeyUsage(false, new ExtendedKeyUsages(extUsage));
            }
        } else {
            params = new X509CertificateParameters();
        }


        return certificateGeneratorFactory.getInstance(
                CertifyingSigner.getInstance(true, issuer, signerFactory),
                new X509CertificateGenerationParameters(validity, builder.build()))
            .generate(new DistinguishedName(dn), publicKey, params);
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
        return new CertifiedKeyPair(
            keyPair.getPrivate(),
            certificateGeneratorFactory.getInstance(signerFactory.getInstance(true, keyPair.getPrivate()),
                    new X509CertificateGenerationParameters(
                        validity,
                        extensionBuilder.get().addBasicConstraints(true)
                            .addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign,
                                KeyUsage.cRLSign))
                            .build()))
                .generate(new DistinguishedName(dn), keyPair.getPublic(),
                    new X509CertificateParameters())
        );
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
        return certificateGeneratorFactory.getInstance(
                CertifyingSigner.getInstance(true, issuer, signerFactory),
                new X509CertificateGenerationParameters(
                    validity,
                    extensionBuilder.get().addBasicConstraints(0)
                        .addKeyUsage(EnumSet.of(KeyUsage.keyCertSign,
                            KeyUsage.cRLSign))
                        .build()))
            .generate(new DistinguishedName(dn), publicKey,
                new X509CertificateParameters());
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
        if (chain == null || chain.isEmpty()) {
            return false;
        }

        Date checkDate = (date != null) ? date : new Date();
        boolean rootExpected = true;
        for (CertifiedPublicKey cert : chain) {
            if (!(cert instanceof X509CertifiedPublicKey)) {
                return false;
            }
            if (rootExpected) {
                if (!((X509CertifiedPublicKey) cert).isRootCA()) {
                    return false;
                }
                rootExpected = false;
            }
            if (!((X509CertifiedPublicKey) cert).isValidOn(checkDate)) {
                return false;
            }
        }
        return true;
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
        CMSSignedDataGeneratorParameters parameters = new CMSSignedDataGeneratorParameters()
            .addSigner(CertifyingSigner.getInstance(true, keyPair, signerFactory));

        if (existingSignature != null) {
            for (CMSSignerInfo existingSigner : existingSignature.getSignatures()) {
                parameters.addSignature(existingSigner);
            }
        }

        Set<CertifiedPublicKey> certs = new HashSet<>();
        if (existingSignature != null && existingSignature.getCertificates() != null) {
            certs.addAll(existingSignature.getCertificates());
        }

        if (certificateProvider != null) {
            if (existingSignature != null) {
                for (CMSSignerInfo existingSigner : existingSignature.getSignatures()) {
                    if (existingSigner.getSubjectKeyIdentifier() != null) {
                        addCertificateChain(
                            certificateProvider.getCertificate(existingSigner.getSubjectKeyIdentifier()),
                            certificateProvider, certs);
                    } else {
                        addCertificateChain(
                            certificateProvider.getCertificate(existingSigner.getIssuer(),
                                existingSigner.getSerialNumber()),
                            certificateProvider, certs);
                    }
                }
            }

            addCertificateChain(keyPair.getCertificate(), certificateProvider, certs);
        }

        if (!certs.isEmpty()) {
            parameters.addCertificates(certs);
        }

        return cmsSignedDataGenerator.generate(data, parameters, embedContent);
    }

    private void addCertificateChain(CertifiedPublicKey certificate, CertificateProvider certificateProvider,
        Collection<CertifiedPublicKey> certs)
    {
        Collection<CertifiedPublicKey> chain = certificateChainBuilder.build(certificate, certificateProvider);
        if (chain != null) {
            certs.addAll(chain);
        }
    }
}

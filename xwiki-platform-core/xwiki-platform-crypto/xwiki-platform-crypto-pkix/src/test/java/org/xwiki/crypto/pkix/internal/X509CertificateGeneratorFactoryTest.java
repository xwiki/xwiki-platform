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

import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.bouncycastle.util.IPAddress;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.internal.DefaultSecureRandomProvider;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcDSAKeyFactory;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.CertificateGenerator;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.CertifyingSigner;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.internal.extension.DefaultX509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DirectoryName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509DnsName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509IpAddress;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Rfc822Name;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509StringGeneralName;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509URI;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.internal.factory.BcSHA1withRsaSignerFactory;
import org.xwiki.crypto.signer.internal.factory.DefaultSignerFactory;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@ComponentList({Base64BinaryStringEncoder.class, DefaultSecureRandomProvider.class, BcRSAKeyFactory.class,
    BcDSAKeyFactory.class, BcSHA1DigestFactory.class, BcSHA1withRsaSignerFactory.class, DefaultSignerFactory.class})
public class X509CertificateGeneratorFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CertificateGeneratorFactory> mocker =
        new MockitoComponentMockingRule(X509CertificateGeneratorFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<X509ExtensionBuilder> builderMocker =
        new MockitoComponentMockingRule(DefaultX509ExtensionBuilder.class);

    private static final String RSA_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDCmjim/3likJ4"
        + "VF564UyygqPjIX/z090AImLl0fDLUkIyCVTSd18wJ3axr1qjLtSgNPWet0puSxO"
        + "FH0AzFKRCJOjUkQRU8iAkz64MLAf9xrx4nBECciqeB941s01kLtG8C/UqC3O9Sw"
        + "HSdhtUpUU8V/91SiD09yNJsnODi3WqM3oLg1QYzKhoaD2mVo2xJLQ/QXqr2XIc5"
        + "i2Mlpfq6S5JNbFD/I+UFhBUlBNuDOEV7ttIt2eFMEUsfkCestGo0YoQYOpTLPcP"
        + "GRS7MnSY1CLWGUYqaMSnes0nS8ke2PPD4Q0suAZz4msnhNufanscstM8tcNtsZF"
        + "6hj0JvbZok89szAgMBAAECggEBAKWJ1SlR5ysORDtDBXRc5HiiZEbnSGIFtYXaj"
        + "N/nCsJBWBVCb+jZeirmU9bEGoB20OQ6WOjHYCnAqraQ51wMK5HgXvZBGtSMD/AH"
        + "pkiF4YsOYULlXiUL2aQ4NijdvEC1sz1Cw9CAKmElb83UtZ1ZGkJnjhi35giZvU5"
        + "BQRgbK5k57DFY66yv9VDg8tuD/enI9sRsCUZfCImuShGv4nLqhPMPg+1UxDPGet"
        + "Vs8uEaJQ017E14wLKLA0DlED13icelU1A7ufkEdeBSv/yZ7ENjervzPwa9nITK/"
        + "19uzqaHOcYZxmDQn6UHTnaLpIEaUvpp/pbed5S97ETSsqUBC8fqEUECgYEA/Sba"
        + "o6efydhlXDHbXtyvaJWao19sbI9OfxGC6dR2fZiBx8Do9kVDDbMtb1PYEfLhYbi"
        + "urmKGbUtcLSFgxNbZifUmG54M92nBsnsetMCqvMVNzYl2Je83V+NrIsLJjFIZ2C"
        + "BvZa/FKOLDTwSe35fNqaS0ExdwcGNMIT//bDQCmyECgYEAxMq6rN+HpBRuhvvst"
        + "V99zV+lI/1DzZuXExd+c3PSchiqkJrTLaQDvcaHQir9hK7RqF9vO7tvdluJjgX+"
        + "f/CMPNQuC5k6vY/0fS4V2NQWtln9BBSzHtocTnZzFNq8tAZqyEhZUHIbkncroXv"
        + "eUXqtlfOnKB2aYI/+3gPEMYJlH9MCgYA4exjA9r9B65QB0+Xb7mT8cpSD6uBoAD"
        + "lFRITu4sZlE0exZ6sSdzWUsutqMUy+BHCguvHOWpEfhXbVYuMSR9VVYGrWMpc2B"
        + "FSBG9MoBOyTHXpUZ10C7bJtW4IlyUvqkM7PV71C9MqKar2kvaUswdPTC7pZoBso"
        + "GB9+M6crXxdNwQKBgDUVMlGbYi1CTaYfonQyM+8IE7WnhXiatZ+ywKtH3MZmHOw"
        + "wtzIigdfZC3cvvX7i4S73vztvjdtxSaODvmiobEukOF9sj8m+YQa7Pa1lWFML5x"
        + "IIu2BhGS2ZCeXgMvKkoH0x9tWaUhGqD5zZmtiDrPs75CUQBypw7SDaBzwLnld9A"
        + "oGBAPgUh90PvUzbVVkzpVCPI82cmOIVMI1rDE6uCeNzIlN6Xu80RimCSaaDsESi"
        + "tBtoVWLRWWmuCINyqr6e9AdyvbvT6mQCjbn9+y7t6ZAhLaya5ZMUVEBLyLLqMzr"
        + "y oi/huj7m4nV4kPZz9LKxDRu3r6o0Pah+daDsTxEYObtsKa7e";

    private static final String RSA_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAwpo4pv95YpCeFReeuFM"
        + "soKj4yF/89PdACJi5dHwy1JCMglU0ndfMCd2sa9aoy7UoDT1nrdKbksThR9AMxS"
        + "kQiTo1JEEVPIgJM+uDCwH/ca8eJwRAnIqngfeNbNNZC7RvAv1KgtzvUsB0nYbVK"
        + "VFPFf/dUog9PcjSbJzg4t1qjN6C4NUGMyoaGg9plaNsSS0P0F6q9lyHOYtjJaX6"
        + "ukuSTWxQ/yPlBYQVJQTbgzhFe7bSLdnhTBFLH5AnrLRqNGKEGDqUyz3DxkUuzJ0"
        + "mNQi1hlGKmjEp3rNJ0vJHtjzw+ENLLgGc+JrJ4Tbn2p7HLLTPLXDbbGReoY9Cb2"
        + "2aJPPbMwIDAQAB";

    private static final String DSA_PRIVATE_KEY = "MIIBTAIBADCCASwGByqGSM44BAEwggEfAoGBANQ9Oa1j9sWAhdXNyqz8HL/bA/e"
        + "d2VrBw6TPkgMyV1Upix58RSjOHMQNrgemSGkb80dRcLqVDYbI3ObnIJh83Zx6ze"
        + "aTpvUohGLyTa0F7UY15LkbJpyz8WFJaVykH85nz3Zo6Md9Z4X95yvF1+h9qYuak"
        + "jWcHW31+pN4u3cJNg5FAhUAj986cVG9NgWgWzFVSLbB9pEPbFUCgYEAmQrZFH3M"
        + "X5CLX/5vDvxyTeeRPZHLWc0ik3GwrIJExuVrOkuFInpx0aVbuJTxrEnY2fuc/+B"
        + "yj/F56DDO31+qPu7ZxbSvvD33OOk8eFEfn+Hia3QmA+dGrhUqoMpfDf4/GBgJhn"
        + "yQtFzMddHmYB0QnS9yX1n6DOWj/CSX0PvrlMYEFwIVAIO1GUQjAddL4btiFQnhe"
        + "N4fxBTa";

    private static final String DSA_PUBLIC_KEY = "MIGSMAkGByqGSM44BAEDgYQAAoGAJvnuTm8oI/RRI2tiZHtPkvSQaA3FP4PRsVx"
        + "6z1oIGg9OAxrtSS/aiQa+HWFg7fjHlMJ30Vh0yqt7igj70jaLGyDvr3MPDyiO++"
        + "72IiGUluc6yHg6m9cQ53eeJt9i44LJfTOw1S3YMU1ST7alokSnJRTICp5WBy0m1"
        + "scwheuTo0E=";


    private CertificateGeneratorFactory factory;
    private SignerFactory signerFactory;
    private static PrivateKeyParameters rsaPrivateKey;
    private static PublicKeyParameters rsaPublicKey;
    private static PrivateKeyParameters dsaPrivateKey;
    private static PublicKeyParameters dsaPublicKey;

    public void setupTest(MockitoComponentMockingRule<CertificateGeneratorFactory> mocker) throws Exception
    {
        // Decode keys once for all tests.
        if (rsaPrivateKey == null) {
            BinaryStringEncoder base64encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
            AsymmetricKeyFactory keyFactory = mocker.getInstance(AsymmetricKeyFactory.class, "RSA");
            rsaPrivateKey = keyFactory.fromPKCS8(base64encoder.decode(RSA_PRIVATE_KEY));
            rsaPublicKey = keyFactory.fromX509(base64encoder.decode(RSA_PUBLIC_KEY));
            keyFactory = mocker.getInstance(AsymmetricKeyFactory.class, "DSA");
            dsaPrivateKey = keyFactory.fromPKCS8(base64encoder.decode(DSA_PRIVATE_KEY));
            dsaPublicKey = keyFactory.fromX509(base64encoder.decode(DSA_PUBLIC_KEY));
        }
    }

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
        signerFactory = mocker.getInstance(SignerFactory.class, "SHA1withRSAEncryption");
        setupTest(mocker);
    }

/*
    private void dumpCert(CertifiedPublicKey certificate) throws Exception {
        BinaryStringEncoder base64encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
        System.out.println("-----BEGIN CERTIFICATE-----");
        System.out.println(base64encoder.encode(certificate.getEncoded(), 64));
        System.out.println("-----END CERTIFICATE-----");
    }
*/

    private X509CertifiedPublicKey checkSelfSigned(CertifiedPublicKey certificate, int version) throws Exception {
        assertThat(certificate.getIssuer(), equalTo((PrincipalIndentifier) new DistinguishedName("CN=Test")));
        assertThat(certificate.getSubject(), equalTo((PrincipalIndentifier) new DistinguishedName("CN=Test")));
        assertThat(certificate.getIssuer(), equalTo(certificate.getSubject()));
        assertTrue("Signature should match used private key.", certificate.isSignedBy(rsaPublicKey));
        assertTrue("Signature should match subject public key.",
            certificate.isSignedBy(certificate.getPublicKeyParameters()));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey x509cert = (X509CertifiedPublicKey) certificate;

        //dumpCert(certificate);

        assertThat(x509cert.getVersionNumber(), equalTo(version));

        Date yesterday = new Date(System.currentTimeMillis() - 86400000);
        Date inMoreThan500Days = new Date(System.currentTimeMillis() + 43286400000L);

        assertTrue("Certificate should be valid today.", x509cert.isValidOn(new Date()));
        assertThat(x509cert.getNotBefore().getTime(), greaterThan(yesterday.getTime()));
        assertThat(x509cert.getNotAfter().getTime(), lessThan(inMoreThan500Days.getTime()));
        assertFalse("Certificate should not be valid yesterday.",
            x509cert.isValidOn(yesterday));
        assertFalse("Certificate should not be valid in more than 500 days.",
            x509cert.isValidOn(inMoreThan500Days));

        return x509cert;
    }

    private X509CertifiedPublicKey checkRootSigned(CertifiedPublicKey certificate, int version) throws Exception
    {
        assertThat(certificate.getIssuer(), equalTo((PrincipalIndentifier) new DistinguishedName("CN=Test CA")));
        assertThat(certificate.getSubject(), equalTo((PrincipalIndentifier) new DistinguishedName("CN=Test End Entity")));
        assertTrue("Signature should match used private key.", certificate.isSignedBy(rsaPublicKey));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey x509cert = (X509CertifiedPublicKey) certificate;

        //dumpCert(certificate);

        assertThat(x509cert.getVersionNumber(), equalTo(version));

        Date yesterday = new Date(System.currentTimeMillis() - 86400000);
        Date inMoreThan500Days = new Date(System.currentTimeMillis() + 43286400000L);

        assertTrue("Certificate should be valid today.", x509cert.isValidOn(new Date()));
        assertThat(x509cert.getNotBefore().getTime(), greaterThan(yesterday.getTime()));
        assertThat(x509cert.getNotAfter().getTime(), lessThan(inMoreThan500Days.getTime()));
        assertFalse("Certificate should not be valid yesterday.",
            x509cert.isValidOn(yesterday));
        assertFalse("Certificate should not be valid in more than 500 days.",
            x509cert.isValidOn(inMoreThan500Days));

        return x509cert;
    }


    @Test
    public void testGenerateSelfSignedCertificateVersion1() throws Exception
    {
        CertifiedPublicKey certificate =
            factory.getInstance(signerFactory.getInstance(true, rsaPrivateKey),
                                new X509CertificateGenerationParameters())
                .generate(new DistinguishedName("CN=Test"), rsaPublicKey,
                    new X509CertificateParameters());

        checkSelfSigned(certificate, 1);
    }

    @Test
    public void testGenerateSelfSignedCertificateVersion3WithoutExtension() throws Exception
    {
        CertifiedPublicKey certificate =
            factory.getInstance(signerFactory.getInstance(true, rsaPrivateKey),
                                new X509CertificateGenerationParameters(null))
                .generate(new DistinguishedName("CN=Test"), rsaPublicKey,
                    new X509CertificateParameters());

        checkSelfSigned(certificate, 3);
    }

    @Test
    public void testGenerateSelfSignedCertificateVersion3RootCa() throws Exception
    {
        X509ExtensionBuilder builder = builderMocker.getComponentUnderTest();

        CertifiedPublicKey certificate =
            factory.getInstance(signerFactory.getInstance(true, rsaPrivateKey),
                                new X509CertificateGenerationParameters(
                                    builder.addBasicConstraints(true)
                                           .addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign,
                                                                         KeyUsage.cRLSign))
                                           .build()))
                .generate(new DistinguishedName("CN=Test"), rsaPublicKey,
                    new X509CertificateParameters());

        X509CertifiedPublicKey cert = checkSelfSigned(certificate, 3);

        assertTrue("Basic constraints should be critical.", cert.getExtensions().isCritical(X509Extensions.BASIC_CONSTRAINTS_OID));
        assertTrue("Basic constraints should be set to CA.", cert.getExtensions().hasCertificateAuthorityBasicConstraints());
        assertTrue("KeyUsage extension should be critical.", cert.getExtensions().isCritical(KeyUsage.OID));
        assertThat(cert.getExtensions().getKeyUsage(), equalTo(EnumSet.of(KeyUsage.keyCertSign,
            KeyUsage.cRLSign)));
        assertThat(cert.getExtensions().getAuthorityKeyIdentifier(), notNullValue());
        assertThat(cert.getExtensions().getAuthorityKeyIdentifier(),
            equalTo(cert.getExtensions().getSubjectKeyIdentifier()));
    }

    @Test
    public void testGenerateEndEntitySignedCertificateVersion1() throws Exception
    {
        CertifiedPublicKey caCertificate =
            factory.getInstance(signerFactory.getInstance(true, rsaPrivateKey),
                                new X509CertificateGenerationParameters())
                .generate(new DistinguishedName("CN=Test CA"), rsaPublicKey,
                    new X509CertificateParameters());

        //dumpCert(caCertificate);

        CertifiedPublicKey certificate =
            factory.getInstance(
                    CertifyingSigner.getInstance(true, new CertifiedKeyPair(rsaPrivateKey, caCertificate), signerFactory),
                    new X509CertificateGenerationParameters()
                ).generate(new DistinguishedName("CN=Test End Entity"), dsaPublicKey,
                new X509CertificateParameters());

        checkRootSigned(certificate, 1);
    }

    @Test
    public void testGenerateEndEntitySignedCertificateVersion3() throws Exception
    {
        X509ExtensionBuilder builder = builderMocker.getComponentUnderTest();

        CertifiedPublicKey caCertificate =
            factory.getInstance(signerFactory.getInstance(true, rsaPrivateKey),
                                new X509CertificateGenerationParameters(
                                    builder.addBasicConstraints(true)
                                        .addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign,
                                            KeyUsage.cRLSign))
                                        .build()))
                .generate(new DistinguishedName("CN=Test CA"), rsaPublicKey,
                    new X509CertificateParameters());

        //dumpCert(caCertificate);

        builder = builderMocker.getComponentUnderTest();

        CertificateGenerator generator = factory.getInstance(
            CertifyingSigner.getInstance(true, new CertifiedKeyPair(rsaPrivateKey, caCertificate), signerFactory),
            new X509CertificateGenerationParameters(
                builder.addKeyUsage(EnumSet.of(KeyUsage.digitalSignature,
                                               KeyUsage.dataEncipherment))
                        .addExtendedKeyUsage(false,
                            new ExtendedKeyUsages(new String[]{ExtendedKeyUsages.EMAIL_PROTECTION}))
                        .build()));

        builder = builderMocker.getComponentUnderTest();

        CertifiedPublicKey certificate =
            generator.generate(new DistinguishedName("CN=Test End Entity"), dsaPublicKey,
                new X509CertificateParameters(
                    builder.addSubjectAltName(false,
                                              new X509GeneralName[] {
                                                  new X509Rfc822Name("test@example.com"),
                                                  new X509Rfc822Name(new InternetAddress("test@test.com")),
                                                  new X509DnsName("example.com"),
                                                  new X509DirectoryName("CN=Test"),
                                                  new X509IpAddress("192.168.1.1"),
                                                  new X509IpAddress("192.168.2.0/24"),
                                                  new X509IpAddress("192.168.3.0/255.255.255.0"),
                                                  new X509IpAddress(InetAddress.getByName("192.168.4.1")),
                                                  new X509IpAddress(InetAddress.getByName("192.168.5.0"),InetAddress.getByName("255.255.255.0")),
                                                  new X509IpAddress("2001:db8:0:85a3::ac1f:8001"),
                                                  new X509IpAddress("2001:db8:1f89::/48"),
                                                  new X509IpAddress(InetAddress.getByName("2001:db8:0:85a3::ac1f:8001")),
                                                  new X509IpAddress(InetAddress.getByName("2001:db8:1f89::"),InetAddress.getByName("ffff:ffff:ffff::")),
                                                  new X509URI("http://xwiki.org"),
                                                  new X509URI(new URL("http://myxwiki.org"))
                                              })
                           .build()
                ));

        X509CertifiedPublicKey cert = checkRootSigned(certificate, 3);

        assertThat(cert.getExtensions().getExtensionOID(), equalTo(new String[] {"2.5.29.35", "2.5.29.14", "2.5.29.15", "2.5.29.37", "2.5.29.17"}));
        assertThat(cert.getExtensions().getCriticalExtensionOID(), equalTo(new String[] {"2.5.29.15"}));
        assertThat(cert.getExtensions().getNonCriticalExtensionOID(), equalTo(new String[] {"2.5.29.35", "2.5.29.14", "2.5.29.37", "2.5.29.17"}));

        assertTrue("KeyUsage extension should be critical.", cert.getExtensions().isCritical(KeyUsage.OID));
        assertThat(cert.getExtensions().getKeyUsage(), equalTo(EnumSet.of(KeyUsage.digitalSignature,
            KeyUsage.dataEncipherment)));
        assertFalse("ExtendedKeyUsage extension should be non critical.",
            cert.getExtensions().isCritical(ExtendedKeyUsages.OID));
        assertThat(cert.getExtensions().getExtendedKeyUsage().getAll().toArray(new String[0]), equalTo(
            new String[]{ExtendedKeyUsages.EMAIL_PROTECTION}));
        assertTrue("Email data protection extended usage should be set.",
            cert.getExtensions().getExtendedKeyUsage().hasUsage(ExtendedKeyUsages.EMAIL_PROTECTION));

        List<X509GeneralName> names = cert.getExtensions().getSubjectAltName();

        assertThat(names.size(), equalTo(15));
        for (X509GeneralName name : names) {
            if (name instanceof X509Rfc822Name) {
                assertThat(((X509StringGeneralName) name).getName(), anyOf(equalTo("test@example.com"), equalTo("test@test.com")));
                assertThat(((X509Rfc822Name) name).getAddress(), anyOf(equalTo(new InternetAddress("test@example.com")),
                    equalTo(new InternetAddress("test@test.com"))));
            } else if (name instanceof X509DnsName) {
                assertThat(((X509StringGeneralName) name).getName(), equalTo("example.com"));
                assertThat(((X509DnsName) name).getDomain(), equalTo("example.com"));
            } else if (name instanceof X509DirectoryName) {
                assertThat(((X509StringGeneralName) name).getName(), equalTo("CN=Test"));
            } else if (name instanceof X509URI) {
                assertThat(((X509StringGeneralName) name).getName(), anyOf(equalTo("http://xwiki.org"),
                    equalTo("http://myxwiki.org")));
                assertThat(((X509URI) name).getURI(), anyOf(equalTo(new URI("http://xwiki.org")),
                    equalTo(new URI("http://myxwiki.org"))));
                assertThat(((X509URI) name).getURL(), anyOf(equalTo(new URL("http://xwiki.org")),
                    equalTo(new URL("http://myxwiki.org"))));
            } else if (name instanceof X509IpAddress) {
                assertTrue("Invalid IP address: " + ((X509StringGeneralName) name).getName(),
                    IPAddress.isValid(((X509StringGeneralName) name).getName())
                    || IPAddress.isValidWithNetMask(((X509StringGeneralName) name).getName()));
            } else {
                fail("Unexpected SubjectAltName type.");
            }
        }
    }
}

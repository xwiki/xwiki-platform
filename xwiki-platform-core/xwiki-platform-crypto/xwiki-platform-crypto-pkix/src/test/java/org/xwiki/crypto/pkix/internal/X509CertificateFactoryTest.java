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

import java.util.EnumSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcDSAKeyFactory;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.signer.internal.factory.BcRsaSsaPssSignerFactory;
import org.xwiki.crypto.signer.internal.factory.BcSHA1withRsaSignerFactory;
import org.xwiki.crypto.signer.internal.factory.DefaultSignerFactory;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@ComponentList({Base64BinaryStringEncoder.class, BcRSAKeyFactory.class, BcDSAKeyFactory.class,
    BcSHA1DigestFactory.class, BcSHA1withRsaSignerFactory.class, BcRsaSsaPssSignerFactory.class,
    DefaultSignerFactory.class})
public class X509CertificateFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CertificateFactory> mocker =
        new MockitoComponentMockingRule(X509CertificateFactory.class);

    private static final String V1_CA_CERT = "MIICpzCCAY8CEBySdlSTKgwuylJNlQxTMNIwDQYJKoZIhvcNAQEFBQAwEjEQMA4G"
        + "A1UEAwwHVGVzdCBDQTAeFw0xNDAyMDMxMTAwMDBaFw0xNTA2MTgxMDAwMDBaMBIx"
        + "EDAOBgNVBAMMB1Rlc3QgQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB"
        + "AQDCmjim/3likJ4VF564UyygqPjIX/z090AImLl0fDLUkIyCVTSd18wJ3axr1qjL"
        + "tSgNPWet0puSxOFH0AzFKRCJOjUkQRU8iAkz64MLAf9xrx4nBECciqeB941s01kL"
        + "tG8C/UqC3O9SwHSdhtUpUU8V/91SiD09yNJsnODi3WqM3oLg1QYzKhoaD2mVo2xJ"
        + "LQ/QXqr2XIc5i2Mlpfq6S5JNbFD/I+UFhBUlBNuDOEV7ttIt2eFMEUsfkCestGo0"
        + "YoQYOpTLPcPGRS7MnSY1CLWGUYqaMSnes0nS8ke2PPD4Q0suAZz4msnhNufanscs"
        + "tM8tcNtsZF6hj0JvbZok89szAgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAB2M29kY"
        + "IwXRNpqM/CnRhc8MFCKO5XDQI35CrECFFXOyfGJsWb2W/O2FQFpi3bNHdKgt5BN6"
        + "ZVjTdr8veHPr5bQ9IrZgoAAL41xwMThQjGDvomiZri0WtulP8VfX0axFGhdde4mg"
        + "iYpYyCLYvTg5Mp8FuEW9XPtgJSumKYTNhk0prKyN7UfLxrhdI1sG3Y1/2/a8Bz3m"
        + "xzPB6DMYMNPD1rB6R/mU+QUBPCPlUSCm+zQf+gTL0Uu2r4jlUiHSVywAPcEWfGFP"
        + "/qb05hjvU8mYDbwPd3kX/mKHBUYKVqGemz9UPJqF0Yg9y7qtlivdiv7o7VaoykdK"
        + "mDzNKbH1jnI/azc=";

    private static final String V1_CERT = "MIICHzCCAQcCEQCE54ZRMYC3MU3z3c8qgTQ3MA0GCSqGSIb3DQEBBQUAMBIxEDAO"
        + "BgNVBAMMB1Rlc3QgQ0EwHhcNMTQwMjAzMTEwMDAwWhcNMTUwNjE4MTAwMDAwWjAa"
        + "MRgwFgYDVQQDDA9UZXN0IEVuZCBFbnRpdHkwgZIwCQYHKoZIzjgEAQOBhAACgYAm"
        + "+e5Obygj9FEja2Jke0+S9JBoDcU/g9GxXHrPWggaD04DGu1JL9qJBr4dYWDt+MeU"
        + "wnfRWHTKq3uKCPvSNosbIO+vcw8PKI777vYiIZSW5zrIeDqb1xDnd54m32Ljgsl9"
        + "M7DVLdgxTVJPtqWiRKclFMgKnlYHLSbWxzCF65OjQTANBgkqhkiG9w0BAQUFAAOC"
        + "AQEAoRnDJytIjg20ThKpSuoT8uJA3KIDZMZbwwIJNvVMcg+eEVRO0gRHdOehdoeY"
        + "hzKDcEAiM74kGCTq2ruxRhg8CBRa/fq4Q/fZ0C96HdOM7jZYlvRSVxXyszln3osX"
        + "sfcUeTe87sb6qjXMe7t4sWgnUzG/+xgKr9qs9kdxTgvF1yTC3I6UvUeXSCF1TgjK"
        + "ZFV7vmxcctB8W+1xBhcbgSLqDiVk3GgfdLU8+AS81+1sDKsXsOLGUasiMXCMqbRL"
        + "PrrFakMIh4EGdEH8sMaUJFw9CkqH79Dq8xXFklgBdS7a5N8fslEIGJXyQrIN88r5"
        + "36kTJXurmSwrOGC+LlzSANgMSA==";

    private static final String V3_CA_CERT = "MIIDETCCAfmgAwIBAgIQcGoZ+d8mnKUSOv270U6ZrTANBgkqhkiG9w0BAQUFADAS"
        + "MRAwDgYDVQQDDAdUZXN0IENBMB4XDTE0MDIwMzExMDAwMFoXDTE1MDYxODEwMDAw"
        + "MFowEjEQMA4GA1UEAwwHVGVzdCBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC"
        + "AQoCggEBAMKaOKb/eWKQnhUXnrhTLKCo+Mhf/PT3QAiYuXR8MtSQjIJVNJ3XzAnd"
        + "rGvWqMu1KA09Z63Sm5LE4UfQDMUpEIk6NSRBFTyICTPrgwsB/3GvHicEQJyKp4H3"
        + "jWzTWQu0bwL9SoLc71LAdJ2G1SlRTxX/3VKIPT3I0myc4OLdaozeguDVBjMqGhoP"
        + "aZWjbEktD9BeqvZchzmLYyWl+rpLkk1sUP8j5QWEFSUE24M4RXu20i3Z4UwRSx+Q"
        + "J6y0ajRihBg6lMs9w8ZFLsydJjUItYZRipoxKd6zSdLyR7Y88PhDSy4BnPiayeE2"
        + "59qexyy0zy1w22xkXqGPQm9tmiTz2zMCAwEAAaNjMGEwHwYDVR0jBBgwFoAUiBZ2"
        + "8+5dImSVCTC89MiGmBPOFWEwHQYDVR0OBBYEFIgWdvPuXSJklQkwvPTIhpgTzhVh"
        + "MA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgEGMA0GCSqGSIb3DQEBBQUA"
        + "A4IBAQCou90IDsVku2cVEUguedn1J5hFYlYXZBhK4/6QNwS8cfPetqJJywwRClnH"
        + "mJWHgc0alswX5MJ184z5IM3G9NTmqzHjplpNiw6HZ1N0/8cCYIIng3kSrK4Y6w9F"
        + "FVH2HVYy+3EspWHcwPdSr2Kcs4dFD/w4J4cjcp8UILSQyTlKpv+Dh8XHKZFv6f1U"
        + "vy+gIYdrxnmApcOcnX4vAPVwbVMd+he9HpjHxf4zsdXrtSdLM7RP6VeeFouxoox/"
        + "GZKuV59XK0DWMcGya8JAScedfCEQL6AbMs/CZBL4Hx/Y1/bnsdncmclcWPgmp0KD"
        + "i5ZotvGKE+kkN1srsfPZLx8FrU9v";

    private static final String V3_CERT = "MIIDwjCCAqqgAwIBAgIQBzqt14CdRxHQ7oMUtmv1ejANBgkqhkiG9w0BAQUFADAS"
        + "MRAwDgYDVQQDDAdUZXN0IENBMB4XDTE0MDIwMzExMDAwMFoXDTE1MDYxODEwMDAw"
        + "MFowGjEYMBYGA1UEAwwPVGVzdCBFbmQgRW50aXR5MIGSMAkGByqGSM44BAEDgYQA"
        + "AoGAJvnuTm8oI/RRI2tiZHtPkvSQaA3FP4PRsVx6z1oIGg9OAxrtSS/aiQa+HWFg"
        + "7fjHlMJ30Vh0yqt7igj70jaLGyDvr3MPDyiO++72IiGUluc6yHg6m9cQ53eeJt9i"
        + "44LJfTOw1S3YMU1ST7alokSnJRTICp5WBy0m1scwheuTo0GjggGbMIIBlzBJBgNV"
        + "HSMEQjBAgBSIFnbz7l0iZJUJMLz0yIaYE84VYaEWpBQwEjEQMA4GA1UEAwwHVGVz"
        + "dCBDQYIQcGoZ+d8mnKUSOv270U6ZrTAdBgNVHQ4EFgQUnSLsYFixuOTJNNUR2iEV"
        + "LtkWnR4wDgYDVR0PAQH/BAQDAgSQMBMGA1UdJQQMMAoGCCsGAQUFBwMEMIIBBAYD"
        + "VR0RBIH8MIH5gRB0ZXN0QGV4YW1wbGUuY29tgQ10ZXN0QHRlc3QuY29tggtleGFt"
        + "cGxlLmNvbaQRMA8xDTALBgNVBAMMBFRlc3SHBMCoAQGHCMCoAgD///8AhwjAqAMA"
        + "////AIcEwKgEAYcIwKgFAP///wCHECABDbgAAIWjAAAAAKwfgAGHICABDbgfiQAA"
        + "AAAAAAAAAAD///////8AAAAAAAAAAAAAhxAgAQ24AACFowAAAACsH4ABhyAgAQ24"
        + "H4kAAAAAAAAAAAAA////////AAAAAAAAAAAAAIYQaHR0cDovL3h3aWtpLm9yZ4YS"
        + "aHR0cDovL215eHdpa2kub3JnMA0GCSqGSIb3DQEBBQUAA4IBAQBWf010vIGJBsUs"
        + "9QikH9iypr0S5Yxi11+3c4pZwSHGIbru9QyztkyS5IUuKHy6yn21AytFUFxB066N"
        + "TBgsnEo8c+6eM8OZYn82zyx265dFU2lkn9ak5CbcBEhErpB8R/VzJ+vOzxQHPWjY"
        + "CuJmn7mqw+CdVM/3ISN552W8wxCw54gv9VIrRR+c3bulPNh18VUTX/B4N3/i+gKJ"
        + "eFmPc06CKXE4qy/9ByVkUzEuB+mR+Y5Wm5ZypHQO1laYTlpcnmZztlPtIh6x59nD"
        + "w+Wj0A2FNJSAuQXh+NX79MsJr7624qiesThJDnDY/+QQVezkUef+PLsL/8hXuMGP"
        + "cvbMdvJ1";

    private CertificateFactory factory;
    private static byte[] v1CaCert;
    private static byte[] v1Cert;
    private static byte[] v3CaCert;
    private static byte[] v3Cert;

    public void setupTest(MockitoComponentMockingRule<CertificateFactory> mocker) throws Exception
    {
        // Decode keys once for all tests.
        if (v1CaCert == null) {
            BinaryStringEncoder base64encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
            v1CaCert = base64encoder.decode(V1_CA_CERT);
            v1Cert = base64encoder.decode(V1_CERT);
            v3CaCert = base64encoder.decode(V3_CA_CERT);
            v3Cert = base64encoder.decode(V3_CERT);
        }
    }

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
        setupTest(mocker);
    }

    @Test
    public void testV1CaCert() throws Exception
    {
        CertifiedPublicKey certificate = factory.decode(v1CaCert);

        assertTrue("CA should verify itself.", certificate.isSignedBy(certificate.getPublicKeyParameters()));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey cert = (X509CertifiedPublicKey) certificate;
        assertThat(cert.getVersionNumber(), equalTo(1));
    }

    @Test
    public void testV1Cert() throws Exception
    {
        CertifiedPublicKey caCert = factory.decode(v1CaCert);
        CertifiedPublicKey certificate = factory.decode(v1Cert);

        assertTrue("End certificate should be verified by CA.", certificate.isSignedBy(caCert.getPublicKeyParameters()));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey cert = (X509CertifiedPublicKey) certificate;
        assertThat(cert.getVersionNumber(), equalTo(1));
    }

    @Test
    public void testV3CaCert() throws Exception
    {
        CertifiedPublicKey certificate = factory.decode(v3CaCert);

        assertTrue("CA should verify itself.", certificate.isSignedBy(certificate.getPublicKeyParameters()));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey cert = (X509CertifiedPublicKey) certificate;
        assertThat(cert.getVersionNumber(), equalTo(3));

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
    public void testV3Cert() throws Exception
    {
        CertifiedPublicKey caCert = factory.decode(v3CaCert);
        CertifiedPublicKey certificate = factory.decode(v3Cert);

        assertTrue("End certificate should be verified by CA.", certificate.isSignedBy(caCert.getPublicKeyParameters()));

        assertThat(certificate, instanceOf(X509CertifiedPublicKey.class));
        X509CertifiedPublicKey cert = (X509CertifiedPublicKey) certificate;
        assertThat(cert.getVersionNumber(), equalTo(3));

        assertTrue("KeyUsage extension should be critical.", cert.getExtensions().isCritical(KeyUsage.OID));
        assertThat(cert.getExtensions().getKeyUsage(), equalTo(EnumSet.of(KeyUsage.digitalSignature,
            KeyUsage.dataEncipherment)));
        assertFalse("ExtendedKeyUsage extension should be non critical.",
            cert.getExtensions().isCritical(ExtendedKeyUsages.OID));
        assertThat(cert.getExtensions().getExtendedKeyUsage().getAll().toArray(new String[0]), equalTo(
            new String[]{ExtendedKeyUsages.EMAIL_PROTECTION}));
        assertTrue("Email data protection extended usage should be set.",
            cert.getExtensions().getExtendedKeyUsage().hasUsage(ExtendedKeyUsages.EMAIL_PROTECTION));
    }

}

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

import java.security.GeneralSecurityException;
import java.security.cert.CertificateExpiredException;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.crypto.internal.BouncyCastleJcaProvider;
import org.xwiki.crypto.internal.UserDocumentUtils;
import org.xwiki.crypto.passwd.internal.DefaultPasswordCryptoService;
import org.xwiki.crypto.passwd.internal.DefaultPasswordCryptoServiceConfiguration;
import org.xwiki.crypto.x509.internal.DefaultX509CryptoService;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@ComponentList({BouncyCastleJcaProvider.class, DefaultPasswordCryptoServiceConfiguration.class,
    DefaultPasswordCryptoService.class, DefaultX509CryptoService.class})
public class DefautX509CryptoServiceTest
{
    @Rule
    public final MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private X509CryptoService service;

    @BeforeComponent
    public void initializeMocks() throws Exception {
        componentManager.registerMockComponent(ConfigurationSource.class);
        UserDocumentUtils documentUtils = componentManager.registerMockComponent(UserDocumentUtils.class);
        when(documentUtils.getCurrentUser()).thenReturn("xwiki:XWiki.Me");
        when(documentUtils.getUserDocURL(any(String.class))).thenReturn("http://www.my.web.id");
    }

    @Before
    public void configure() throws Exception
    {
        service = componentManager.getInstance(X509CryptoService.class);
    }

    /** Storing a single keypair for as many tests as possible because they take a long time to generate. */
    private static XWikiX509KeyPair keyPair;

    private XWikiX509KeyPair getKeyPair()
        throws GeneralSecurityException
    {
        if (keyPair == null) {
            keyPair = service.newCertAndPrivateKey(1, SampleTestData.PASSWORD);
        }
        return keyPair;
    }

    @Test
    public void testNewCertAndPrivateKeyWithCorrectPassword() throws GeneralSecurityException
    {
        Assert.assertNotNull("Private key is null", getKeyPair().getPrivateKey(SampleTestData.PASSWORD));
    }

    @Test
    public void testNewCertAndPrivateKeyWithWrongPassword()
    {
        try {
            Assert.assertNotNull("Private key is null", getKeyPair().getPrivateKey("asdf"));
        } catch (GeneralSecurityException exception) {
            Assert.assertTrue(exception.getMessage().contains("Could not decrypt private key, "
                + "wrong password or corrupted file."));
        }
    }

    @Test
    public void testNewCertIsValid() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = getKeyPair().getCertificate();
        cert.checkValidity();
        cert.verify(cert.getPublicKey());
    }

    @Test(expected = CertificateExpiredException.class)
    public void testNewCertIsExpired() throws GeneralSecurityException
    {
        XWikiX509KeyPair keyPair = service.newCertAndPrivateKey(0, "bla");
        XWikiX509Certificate cert = keyPair.getCertificate();
        cert.verify(cert.getPublicKey());
        cert.checkValidity();
    }

    @Test
    public void testCertFromPEM() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = service.certFromPEM(SampleTestData.CERT_PEM);
        Assert.assertEquals("Imported certificate has incorrect fingerprint",
            SampleTestData.CERT_SHA1, cert.getFingerprint());
    }

    @Test
    public void testKeyPairFromBase64() throws Exception
    {
        XWikiX509KeyPair imported = service.keyPairFromBase64(SampleTestData.KEY_PAIR_EXPORTED_AS_BASE_64);
        Assert.assertNotNull(imported.getPrivateKey(SampleTestData.PASSWORD));
        XWikiX509Certificate cert = imported.getCertificate();
        Assert.assertEquals("Certificate from the imported KP has not the correct Fingerprint",
            SampleTestData.CERTIFICATE_FINGERPRINT, cert.getFingerprint());
        Assert.assertEquals("Certificate from the imported KP does not have the correct author UID",
            "xwiki:XWiki.Me", cert.getAuthorUID());
        Assert.assertEquals("The imported public key does not match the certificate",
            cert.getPublicKey(), imported.getPublicKey());
        Assert.assertEquals("The key pair reported fingerprint does not match the certificate",
            cert.getFingerprint(),imported.getFingerprint());
    }

    @Test
    public void testKeyPairExportImportReciprocity() throws Exception
    {
        XWikiX509KeyPair keyPair = getKeyPair();

        final String exported = keyPair.serializeAsBase64();
        final XWikiX509KeyPair imported = service.keyPairFromBase64(exported);

        Assert.assertEquals("Private keys mismatch",
            keyPair.getPrivateKey(SampleTestData.PASSWORD), imported.getPrivateKey(SampleTestData.PASSWORD));
        Assert.assertEquals("Public keys mismatch", keyPair.getPublicKey(), imported.getPublicKey());
        Assert.assertEquals("Certificates mismatch", keyPair.getCertificate(), imported.getCertificate());
        Assert.assertEquals("KeyPairs mismatch", keyPair, imported);
        Assert.assertEquals("Serialized keypairs mismatch",
            keyPair.serializeAsBase64(), imported.serializeAsBase64());
    }

    @Test
    public void testSignTextAndVerifySameText()
        throws GeneralSecurityException
    {
        XWikiX509KeyPair keyPair = getKeyPair();
        String signature = service.signText("hello world", keyPair, SampleTestData.PASSWORD);
        XWikiX509Certificate cert = service.verifyText("hello world", signature);
        Assert.assertNotNull("Signtext/verifyText, failed when they should have succeeded.", cert);
        Assert.assertTrue("Signtext/verifytext returning incorrect certificate.",
            cert.getFingerprint().equals(keyPair.getCertificate().getFingerprint()));
        Assert.assertTrue("XWikiX509Certificate.equals returns false when fingerprints are the same.",
            cert.equals(keyPair.getCertificate()));
    }

    @Test
    public void testSignTextAndVerifyWrongText()
        throws GeneralSecurityException
    {
        XWikiX509KeyPair keyPair = getKeyPair();
        String signature = service.signText("hello world", keyPair, SampleTestData.PASSWORD);
        XWikiX509Certificate cert = service.verifyText("Wrong Text", signature);
        Assert.assertNull("Signtext/verifyText, succeeded with wrong text!", cert);
    }

    @Test
    public void verifyTextSignedInBrowser()
        throws GeneralSecurityException
    {
        XWikiX509Certificate cert = service.certFromPEM(SampleTestData.BROWSER_CERT);
        XWikiX509Certificate verifyResponse =
            service.verifyText(SampleTestData.BROWSER_SIGNED_TEXT, SampleTestData.BROWSER_SIGNATURE);
        Assert.assertTrue("Signtext/verifytext returning incorrect certificate.",
            cert.getFingerprint().equals(verifyResponse.getFingerprint()));
        Assert.assertTrue("XWikiX509Certificate.equals returns false when fingerprints are the same.",
            cert.equals(verifyResponse));
    }

    @Test
    public void testCertsFromSpkacTest() throws Exception
    {
        XWikiX509Certificate[] certs = service.certsFromSpkac(SampleTestData.SPKAC_SERIALIZATION, 1);

        // verify client
        certs[0].checkValidity();
        certs[0].verify(certs[1].getPublicKey());

        // verify authority
        certs[1].checkValidity();
        certs[1].verify(certs[1].getPublicKey());

        // read Basic Constraints to check second certificate is a CA
        DEROctetString obj = (DEROctetString) new ASN1InputStream(certs[1].getExtensionValue("2.5.29.19")).readObject();
        ASN1Sequence seq = (ASN1Sequence) new ASN1InputStream(obj.getOctets()).readObject();
        BasicConstraints constraints = BasicConstraints.getInstance(seq);
        Assert.assertTrue("Second certificate should be a CA certificate", constraints.isCA());
    }
}

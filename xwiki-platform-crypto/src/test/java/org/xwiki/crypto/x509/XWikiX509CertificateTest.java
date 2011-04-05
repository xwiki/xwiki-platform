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
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.crypto.internal.Convert;


/**
 * Tests {@link XWikiX509Certificate}.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class XWikiX509CertificateTest
{
    /** Fingerprint of the test certificate. */
    private static final String CERT_SHA1 = "eb31104d2fb1bc8495cf39e75124aef3f9ab7bfb";

    /** PEM encoded test certificate (XWiki SAS Web Certificate). */
    private static final String CERT_PEM = "-----BEGIN CERTIFICATE-----\n"
                                         + "MIIDWTCCAsKgAwIBAgIDEl9SMA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVT\n"
                                         + "MRAwDgYDVQQKEwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0\n"
                                         + "aWZpY2F0ZSBBdXRob3JpdHkwHhcNMTAwNDE2MDI0NTU3WhcNMTEwNTE5MDEzNjIw\n"
                                         + "WjCB4zEpMCcGA1UEBRMgQnZ2MGF3azJ0VUhSOVBCdG9VdndLbEVEYVBpbkpoanEx\n"
                                         + "CzAJBgNVBAYTAkZSMRcwFQYDVQQKFA4qLnh3aWtpc2FzLmNvbTETMBEGA1UECxMK\n"
                                         + "R1Q0MDc0ODAzNjExMC8GA1UECxMoU2VlIHd3dy5yYXBpZHNzbC5jb20vcmVzb3Vy\n"
                                         + "Y2VzL2NwcyAoYykxMDEvMC0GA1UECxMmRG9tYWluIENvbnRyb2wgVmFsaWRhdGVk\n"
                                         + "IC0gUmFwaWRTU0woUikxFzAVBgNVBAMUDioueHdpa2lzYXMuY29tMIGfMA0GCSqG\n"
                                         + "SIb3DQEBAQUAA4GNADCBiQKBgQCSiflt/i6ZlqNODL8LQLPwNfXEdb3J+II1NXye\n"
                                         + "InrU3yRCybF7DG8NGIrvy+0o40YI+I4Q1Fcvv890IObdQdHmFtz8OKzKXT+giEG7\n"
                                         + "LxJXW3DDb9NckOsbjbNuNFSA9E/aQalrxbDVWyO0droG1v3vDBmG/KzfQkPmoE8g\n"
                                         + "P4qPsQIDAQABo4GuMIGrMB8GA1UdIwQYMBaAFEjmaPkr0rKV10fYIyAQTzOYkJ/U\n"
                                         + "MA4GA1UdDwEB/wQEAwIE8DAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIw\n"
                                         + "OgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL2NybC5nZW90cnVzdC5jb20vY3Jscy9z\n"
                                         + "ZWN1cmVjYS5jcmwwHQYDVR0OBBYEFHbS5h/MPHDXIIn5ived2HiF6AwiMA0GCSqG\n"
                                         + "SIb3DQEBBQUAA4GBALPfA0VQS9pCFYl9co6k3AYLx+gWg6FsTn3aYZRjS9Eeg2qR\n"
                                         + "f7XuiIlq2ZLb1r0SA8Unn2uw2wrHXnqw2I/AARawI/vT4toKGjJwLB8cONLE6cyO\n"
                                         + "rC4qW/5AUann6D1r26EWLSGYh62AcX/jUT4bjoWLhMhblxyLOgbBe8uYPLMH\n"
                                         + "-----END CERTIFICATE-----\n";

    /** Expected author name. */
    private static final String CERT_AUTHOR = "CN=*.xwikisas.com, OU=Domain Control Validated - RapidSSL(R), "
                                            + "OU=See www.rapidssl.com/resources/cps (c)10, OU=GT40748036, "
                                            + "O=*.xwikisas.com, C=FR, SERIALNUMBER=Bvv0awk2tUHR9PBtoUvwKlEDaPinJhjq";

    /** Expected issuer name. */
    private static final String CERT_ISSUER = "OU=Equifax Secure Certificate Authority, O=Equifax, C=US";

    @Test
    public void testImportPEM() throws GeneralSecurityException
    {
        // test if succeeds
        XWikiX509Certificate.fromPEMString(CERT_PEM);
    }

    @Test
    public void testImportSHA1() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(CERT_PEM);
        Assert.assertEquals("Imported certificate has incorrect fingerprint", CERT_SHA1, cert.getFingerprint());
    }

    @Test
    public void testImportExportPEM() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(CERT_PEM);
        String exported = cert.toPEMString();
        Assert.assertEquals("Exported certificate is different", CERT_PEM, exported);
    }

    @Test
    public void testImportAuthorIssuer() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(CERT_PEM);
        // X500Principal normalizes the name
        X500Principal author = new X500Principal(cert.getAuthorName());
        X500Principal issuer = new X500Principal(cert.getIssuerName());
        Assert.assertEquals("Incorrect author name", new X500Principal(CERT_AUTHOR), author);
        Assert.assertEquals("Incorrect issuer name", new X500Principal(CERT_ISSUER), issuer);
    }

    @Test
    public void testImportedEqualCreated() throws GeneralSecurityException
    {
        CertificateFactory factory = CertificateFactory.getInstance("X509");
        Certificate cert = factory.generateCertificate(new ByteArrayInputStream(Convert.stringToBytes(CERT_PEM)));
        if (!(cert instanceof X509Certificate)) {
            throw new RuntimeException("Wrong certificate type");
        }
        XWikiX509Certificate certCreated = new XWikiX509Certificate((X509Certificate) cert);
        XWikiX509Certificate certImported = XWikiX509Certificate.fromPEMString(CERT_PEM);
        Assert.assertEquals(certImported, certCreated);
    }

    @Test
    public void testIssuerFingerprint() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(CERT_PEM);
        XWikiX509Certificate certWrapper = new XWikiX509Certificate(cert, cert.getFingerprint());
        Assert.assertEquals(cert.getFingerprint(), certWrapper.getIssuerFingerprint());
    }

    @Test
    public void testNoUID() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(CERT_PEM);
        // we use a web certificate here, it doesn't have UID
        Assert.assertEquals("Web certificate should not have UID", "", cert.getAuthorUID());
    }
}


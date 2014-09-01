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
    @Test
    public void testImportPEM() throws GeneralSecurityException
    {
        // test if succeeds
        XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
    }

    @Test
    public void testImportSHA1() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        Assert.assertEquals("Imported certificate has incorrect fingerprint",
            SampleTestData.CERT_SHA1, cert.getFingerprint());
    }

    @Test
    public void testImportExportPEM() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        String exported = cert.toPEMString();
        Assert.assertEquals("Exported certificate is different", SampleTestData.CERT_PEM, exported);
    }

    @Test
    public void testImportAuthorIssuer() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        // X500Principal normalizes the name
        X500Principal author = new X500Principal(cert.getAuthorName());
        X500Principal issuer = new X500Principal(cert.getIssuerName());
        Assert.assertEquals("Incorrect author name", new X500Principal(SampleTestData.CERT_AUTHOR), author);
        Assert.assertEquals("Incorrect issuer name", new X500Principal(SampleTestData.CERT_ISSUER), issuer);
    }

    @Test
    public void testImportedEqualCreated() throws GeneralSecurityException
    {
        CertificateFactory factory = CertificateFactory.getInstance("X509");
        Certificate cert =
            factory.generateCertificate(new ByteArrayInputStream(Convert.stringToBytes(SampleTestData.CERT_PEM)));
        if (!(cert instanceof X509Certificate)) {
            throw new RuntimeException("Wrong certificate type");
        }
        XWikiX509Certificate certCreated = new XWikiX509Certificate((X509Certificate) cert);
        XWikiX509Certificate certImported = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        Assert.assertEquals(certImported, certCreated);
    }

    @Test
    public void testIssuerFingerprint() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        XWikiX509Certificate certWrapper = new XWikiX509Certificate(cert, cert.getFingerprint());
        Assert.assertEquals(cert.getFingerprint(), certWrapper.getIssuerFingerprint());
    }

    @Test
    public void testNoUID() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(SampleTestData.CERT_PEM);
        // we use a web certificate here, it doesn't have UID
        Assert.assertEquals("Web certificate should not have UID", "", cert.getAuthorUID());
    }
}


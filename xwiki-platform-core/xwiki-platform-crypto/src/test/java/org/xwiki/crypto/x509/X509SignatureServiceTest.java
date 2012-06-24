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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.internal.X509SignatureService;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Prove that X509SignatureService can sign and verify text and can verify text signed in firefox.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class X509SignatureServiceTest extends AbstractComponentTestCase
{
    private final String browserSignedText = "This text was signed in Firefox 3.6.6-Linux using crypto.signText()";

    private final String browserSignature = "MIIGZgYJKoZIhvcNAQcCoIIGVzCCBlMCAQExCzAJBgUrDgMCGgUAMAsGCSqGSIb3\n"
                                          + "DQEHAaCCBA0wggQJMIIC8aADAgECAhBy0nsuQnInm2kjFpF4N/hmMA0GCSqGSIb3\n"
                                          + "DQEBBQUAMIGFMREwDwYDVQQKDAhGT0FGK1NTTDEmMCQGA1UECwwdVGhlIENvbW11\n"
                                          + "bml0eSBPZiBTZWxmIFNpZ25lcnMxSDBGBgoJkiaJk/IsZAEBDDhodHRwOi8vMTI3\n"
                                          + "LjAuMC4xOjgwODEveHdpa2lUcnVuay9iaW4vdmlldy9YV2lraS9BZG1pbiNtZTAe\n"
                                          + "Fw0xMDA3MDYwMjE5MzFaFw0xMTA2MjcwMzE5MzFaMIGFMREwDwYDVQQKDAhGT0FG\n"
                                          + "K1NTTDEmMCQGA1UECwwdVGhlIENvbW11bml0eSBPZiBTZWxmIFNpZ25lcnMxSDBG\n"
                                          + "BgoJkiaJk/IsZAEBDDhodHRwOi8vMTI3LjAuMC4xOjgwODEveHdpa2lUcnVuay9i\n"
                                          + "aW4vdmlldy9YV2lraS9BZG1pbiNtZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCC\n"
                                          + "AQoCggEBAJZal5ev5JLyHRTibGwr9Dy/+wPHr/KI2k9xR6af1oZegJ9iS2r59Cw8\n"
                                          + "2G7768qjTgqOABIkxkwaetUu8wcZJiKkJmltPWEDCtnEH5j2JWPb5ECFXtUXcrQu\n"
                                          + "o978LPXRX3Ysj6uUPCUiqFmpAvbMCDtmxIlNy4gDr20JgTm6FKvdLLjMC50LiNM6\n"
                                          + "MOU2TePiC2Ka+yHaDJUreLdg0N8RIN8OhzOd4xtQPMvDFG5h9TFcrKyl+5hsVTfX\n"
                                          + "17VoXl091prMUSnkP03JivwUBd86neU42GxDUrkPm7kzCI9qb3e+BI4hYWezOrUz\n"
                                          + "buLUzHkLN+TTx0wuzAnLOgcg5KlQu8cCAwEAAaNzMHEwDAYDVR0TAQH/BAIwADAR\n"
                                          + "BglghkgBhvhCAQEEBAMCBaAwDgYDVR0PAQH/BAQDAgLsMB8GA1UdIwQYMBaAFL4C\n"
                                          + "6gJ0acc7ps2ggc8TkjqC/U/pMB0GA1UdDgQWBBTvkkRSnJpwnCExr8JIPw4eFSN9\n"
                                          + "UTANBgkqhkiG9w0BAQUFAAOCAQEAxGJRUeY0J1cgyfKY2LoOejeUzu0WQLn3UDef\n"
                                          + "4Zd7231AzGCRKblnCzbgkvEgl4k5C0cfe3FPScKAUs0oR9g9hLZOVyDymxavhULI\n"
                                          + "uGumfhojA17/7R9KSo7NVFBLPtX2g1F+70oe14CapFkZ0NyvN95E82TDlRV7INGu\n"
                                          + "OZOrEMBiCJy8Qe/mcnaaD2vo9vngq1BFt5qa6t5ZUEAvjv4q9jdHxcGHjUgCt3Xf\n"
                                          + "yH/61AE7xZVpq3wShZiyRywGXkRwbVLwJgIXGUPMOOU9ERArKvHPJ3fc+He2qQcI\n"
                                          + "arVgzs/9YiySikSCP5420jnvN/dnWEYLBLxPFFFy1LPEG9udSzGCAiEwggIdAgEB\n"
                                          + "MIGaMIGFMREwDwYDVQQKDAhGT0FGK1NTTDEmMCQGA1UECwwdVGhlIENvbW11bml0\n"
                                          + "eSBPZiBTZWxmIFNpZ25lcnMxSDBGBgoJkiaJk/IsZAEBDDhodHRwOi8vMTI3LjAu\n"
                                          + "MC4xOjgwODEveHdpa2lUcnVuay9iaW4vdmlldy9YV2lraS9BZG1pbiNtZQIQctJ7\n"
                                          + "LkJyJ5tpIxaReDf4ZjAJBgUrDgMCGgUAoF0wGAYJKoZIhvcNAQkDMQsGCSqGSIb3\n"
                                          + "DQEHATAcBgkqhkiG9w0BCQUxDxcNMTAwNzE3MTkyNTIzWjAjBgkqhkiG9w0BCQQx\n"
                                          + "FgQUF6ygwoVs3JxGSUOo0V5zVCWhUZowDQYJKoZIhvcNAQEBBQAEggEABeoZltxW\n"
                                          + "wVu2OfMobJAfu0Qiyh607pqOjDsxzosgQcRMdphHLm/snARcxzdvakawZBDhptyX\n"
                                          + "hIPKK1JedMeR3JoGl6TSkfkEy9JNntnlDIP0VvvEdW+I2HeL/v/DF+M3uajhMvGR\n"
                                          + "9o2R90yRGzBap92pVtDS4zYgnm9MSx2ax5dEyO/Yw7cdivlTplx8BVtQPx5WZzY2\n"
                                          + "FVsDAbtONWD4IKSRnywIcUi/7FHNBshmczOCsYlmsh14Wl9CYGcFxhziWJ510DCz\n"
                                          + "KmNB3oyXtnslVN5GqEuTot9mhfPjAp38AXVm2HJhwRN+yJzyC4K2Houk8U7sEr21\n"
                                          + "3ubN40e3RK5UCA==";

    private final String browserCert = "-----BEGIN CERTIFICATE-----\n"
                                     + "MIIECTCCAvGgAwIBAgIQctJ7LkJyJ5tpIxaReDf4ZjANBgkqhkiG9w0BAQUFADCB\n"
                                     + "hTERMA8GA1UECgwIRk9BRitTU0wxJjAkBgNVBAsMHVRoZSBDb21tdW5pdHkgT2Yg\n"
                                     + "U2VsZiBTaWduZXJzMUgwRgYKCZImiZPyLGQBAQw4aHR0cDovLzEyNy4wLjAuMTo4\n"
                                     + "MDgxL3h3aWtpVHJ1bmsvYmluL3ZpZXcvWFdpa2kvQWRtaW4jbWUwHhcNMTAwNzA2\n"
                                     + "MDIxOTMxWhcNMTEwNjI3MDMxOTMxWjCBhTERMA8GA1UECgwIRk9BRitTU0wxJjAk\n"
                                     + "BgNVBAsMHVRoZSBDb21tdW5pdHkgT2YgU2VsZiBTaWduZXJzMUgwRgYKCZImiZPy\n"
                                     + "LGQBAQw4aHR0cDovLzEyNy4wLjAuMTo4MDgxL3h3aWtpVHJ1bmsvYmluL3ZpZXcv\n"
                                     + "WFdpa2kvQWRtaW4jbWUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCW\n"
                                     + "WpeXr+SS8h0U4mxsK/Q8v/sDx6/yiNpPcUemn9aGXoCfYktq+fQsPNhu++vKo04K\n"
                                     + "jgASJMZMGnrVLvMHGSYipCZpbT1hAwrZxB+Y9iVj2+RAhV7VF3K0LqPe/Cz10V92\n"
                                     + "LI+rlDwlIqhZqQL2zAg7ZsSJTcuIA69tCYE5uhSr3Sy4zAudC4jTOjDlNk3j4gti\n"
                                     + "mvsh2gyVK3i3YNDfESDfDoczneMbUDzLwxRuYfUxXKyspfuYbFU319e1aF5dPdaa\n"
                                     + "zFEp5D9NyYr8FAXfOp3lONhsQ1K5D5u5MwiPam93vgSOIWFnszq1M27i1Mx5Czfk\n"
                                     + "08dMLswJyzoHIOSpULvHAgMBAAGjczBxMAwGA1UdEwEB/wQCMAAwEQYJYIZIAYb4\n"
                                     + "QgEBBAQDAgWgMA4GA1UdDwEB/wQEAwIC7DAfBgNVHSMEGDAWgBS+AuoCdGnHO6bN\n"
                                     + "oIHPE5I6gv1P6TAdBgNVHQ4EFgQU75JEUpyacJwhMa/CSD8OHhUjfVEwDQYJKoZI\n"
                                     + "hvcNAQEFBQADggEBAMRiUVHmNCdXIMnymNi6Dno3lM7tFkC591A3n+GXe9t9QMxg\n"
                                     + "kSm5Zws24JLxIJeJOQtHH3txT0nCgFLNKEfYPYS2Tlcg8psWr4VCyLhrpn4aIwNe\n"
                                     + "/+0fSkqOzVRQSz7V9oNRfu9KHteAmqRZGdDcrzfeRPNkw5UVeyDRrjmTqxDAYgic\n"
                                     + "vEHv5nJ2mg9r6Pb54KtQRbeamureWVBAL47+KvY3R8XBh41IArd138h/+tQBO8WV\n"
                                     + "aat8EoWYskcsBl5EcG1S8CYCFxlDzDjlPREQKyrxzyd33Ph3tqkHCGq1YM7P/WIs\n"
                                     + "kopEgj+eNtI57zf3Z1hGCwS8TxRRctSzxBvbnUs=\n"
                                     + "-----END CERTIFICATE-----";

    private final X509SignatureService service = new X509SignatureService();

    protected PasswordCryptoService passwordService;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.passwordService = getComponentManager().getInstance(PasswordCryptoService.class);
    }

    @Test
    public void signAndVerifyTextTest()
        throws GeneralSecurityException
    {
        XWikiX509KeyPair pair = X509KeyServiceTest.getKeyPair(passwordService);
        String signature = this.service.signText("hello world", pair, X509KeyServiceTest.PASSWORD);
        XWikiX509Certificate cert = this.service.verifyText("hello world", signature);
        Assert.assertNotNull("Signtext/verifyText, failed when they should have succeeded.", cert);
        Assert.assertTrue("Signtext/verifytext returning incorrect certificate.",
                          cert.getFingerprint().equals(pair.getCertificate().getFingerprint()));
        Assert.assertTrue("XWikiX509Certificate.equals returns false when fingerprints are the same.",
                          cert.equals(pair.getCertificate()));
    }

    @Test
    public void signAndVerifyWrongTextTest()
        throws GeneralSecurityException
    {
        XWikiX509KeyPair pair = X509KeyServiceTest.getKeyPair(passwordService);
        String signature = this.service.signText("hello world", pair, X509KeyServiceTest.PASSWORD);
        XWikiX509Certificate cert = this.service.verifyText("Wrong Text", signature);
        Assert.assertNull("Signtext/verifyText, succeeded with wrong text!", cert);
    }

    @Test
    public void verifyTextSignedInBrowser()
        throws GeneralSecurityException
    {
        XWikiX509Certificate cert = XWikiX509Certificate.fromPEMString(browserCert);
        XWikiX509Certificate verifyResponse = this.service.verifyText(browserSignedText, browserSignature);
        Assert.assertTrue("Signtext/verifytext returning incorrect certificate.",
                          cert.getFingerprint().equals(verifyResponse.getFingerprint()));
        Assert.assertTrue("XWikiX509Certificate.equals returns false when fingerprints are the same.",
                          cert.equals(verifyResponse));
    }
}

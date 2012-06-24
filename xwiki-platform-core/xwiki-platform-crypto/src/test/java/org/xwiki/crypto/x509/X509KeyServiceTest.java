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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.crypto.passwd.PasswordCryptoService;
import org.xwiki.crypto.x509.internal.X509KeyService;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * KeyService test, insure that the key service is able to make keys without throwing an exception.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class X509KeyServiceTest extends AbstractComponentTestCase
{
    /** This is a public key generated in the browser and passed to the server. */
    private final String spkacSerialization = "MIICTTCCATUwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDC4aLkoIHw\n"
                                            + "Lmpwkml3LQV6s502xXDOwHnTRrFoY22oZGveVXSB13StWsphCdpT9dDdx9nT6SmF\n"
                                            + "2bRdm3XjrcvmSDWdrRBlWc6lC1kDJw9DaGzmT14tWdfbuTzILO+Cp222Rpkzpq+m\n"
                                            + "OkBmWcoU8yZpKgYkandRBrnC68gkQCXp1eTV7EIbcKhkrlv0n7TQj7zL9aTN9ZnH\n"
                                            + "OE609oTAN7o1qJsa3yOMKzwITrHXmQhLaEBysSEZP8b2LEvpv0KkPYaYVYSIPHM0\n"
                                            + "SdPx9j87tC1R815Hgn5//LNYo4avG0uS+hgVtXwd1qi5efCmz6+B6if87BUJpBhk\n"
                                            + "Ix+4/3+3z6MtAgMBAAEWDVRoZUNoYWxsZW5nZTEwDQYJKoZIhvcNAQEEBQADggEB\n"
                                            + "AH7+VePd7la+7sL1NtvaLsKbQFIiOOaxBr4DTMtZx1BrhMIdxfoBN/GBMPiqUDfX\n"
                                            + "eTv83Mv5KjGZNiDVjiXhbxWynv2zmLbLF/bcm8HOSnkRm0R24+bEHQqw1uDN1kj6\n"
                                            + "CLf7YFNlEQI8HpAzala/zuyZNuIACHwD8i5lc2+2SnhDs7ric+BCVTi/wfzi+Od2\n"
                                            + "0jdrXNw75ycww8oLFw46kuwywUl2Z1V8qZ5QOF6GIUClOSrwAlgxhstItSu8+sur\n"
                                            + "oFfUOwW0YIX4zJEqD+aB5hxN8hP+wJG/mYYoVIZFRkk9t4l2ZEo5Avu5aTfEuDUX\n"
                                            + "yj8rDgFwLxTXBpBxvdiRAgw=";

    public static final String PASSWORD = "blah";

    /** Storing a single keypair for as many tests as possible because they take a long time to generate. */
    private static XWikiX509KeyPair keyPair;

    public static XWikiX509KeyPair getKeyPair(PasswordCryptoService passwordService)
        throws GeneralSecurityException
    {
        if (keyPair == null) {
            keyPair = new X509KeyService().newCertAndPrivateKey(1,
                                                                "http://www.my.web.id",
                                                                "xwiki:XWiki.Me",
                                                                PASSWORD,
                                                                passwordService);
        }
        return keyPair;
    }

    /** The tested key service. */
    private final X509KeyService service = new X509KeyService();

    protected PasswordCryptoService passwordService;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.passwordService = getComponentManager().getInstance(PasswordCryptoService.class);
    }

    @Test
    public void certsFromSpkacTest() throws Exception
    {
        this.service.certsFromSpkac(this.spkacSerialization, 1, "http://www.my.web.id", "xwiki:XWiki.Me");
    }

    @Test
    public void newPrivateKeyCorrectPassword() throws GeneralSecurityException
    {
        Assert.assertNotNull("Private key is null", getKeyPair(this.passwordService).getPrivateKey(PASSWORD));
    }

    @Test
    public void newPrivateKeyWrongPassword()
    {
        try {
            Assert.assertNotNull("Private key is null", getKeyPair(passwordService).getPrivateKey("asdf"));
        } catch (GeneralSecurityException exception) {
            Assert.assertTrue(exception.getMessage().contains("Could not decrypt private key, "
                                                              + "wrong password or corrupted file."));
        }
    }

    @Test
    public void testNewCertIsValid() throws GeneralSecurityException
    {
        XWikiX509Certificate cert = getKeyPair(this.passwordService).getCertificate();
        cert.checkValidity();
        cert.verify(cert.getPublicKey());
    }

    @Test(expected = CertificateExpiredException.class)
    public void testNewExpiredCertIsInvalid() throws GeneralSecurityException
    {
        XWikiX509KeyPair keyPair = this.service.newCertAndPrivateKey(0,
                                                                     "http://www.my.web.id",
                                                                     "xwiki:XWiki.Me",
                                                                     "bla",
                                                                     this.passwordService);
        XWikiX509Certificate cert = keyPair.getCertificate();
        cert.verify(cert.getPublicKey());
        cert.checkValidity();
    }
}

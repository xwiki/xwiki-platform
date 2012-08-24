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
package org.xwiki.crypto.passwd;

import java.security.GeneralSecurityException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import org.xwiki.test.AbstractComponentTestCase;

/**
 * Tests {@link org.xwiki.crypto.passwd.internal.DefaultPasswordCryptoService}.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class DefaultPasswordCryptoServiceTest extends AbstractComponentTestCase
{
    /** Length = 272 byte = 17 * 16 */
    private final String textToEncrypt = "Congress shall make no law respecting an establishment of religion, or "
                                       + "prohibiting the free exercise thereof; or abridging the freedom of speech, "
                                       + "or of the press; or the right of the people peaceably to assemble, and to "
                                       + "petition the Government for a redress of grievances.";

    /** Length = 113 byte = 7 * 16 + 1 */
    private final String anotherText = "The length of this text is 113 byte. This is 1 byte more "
                                     + "than a multiple of block size for 128 bit block ciphers.";

    private final String password = "Snuffle";

    protected PasswordCryptoService service;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.service = getComponentManager().getInstance(PasswordCryptoService.class);
    }

    @Test
    public void encryptDecryptTest() throws Exception
    {
        final String out = this.service.encryptText(textToEncrypt, password);
        final String decrypted = this.service.decryptText(out, password);
        Assert.assertTrue(this.textToEncrypt.equals(decrypted));
    }

    @Test
    public void saltTest() throws Exception
    {
        final String encrypt1 = this.service.encryptText(textToEncrypt, password);
        final String encrypt2 = this.service.encryptText(textToEncrypt, password);
        Assert.assertFalse(encrypt1.equals(encrypt2));
    }

    @Test
    public void decryptRegressionTest() throws Exception
    {
        final String decrypted = this.service.decryptText(this.getEncrypted(), password);
        Assert.assertTrue(this.textToEncrypt.equals(decrypted));
    }

    protected String getEncrypted()
    {
        return "-----BEGIN PASSWORD CIPHERTEXT-----\n"
             + "rO0ABXNyADhvcmcueHdpa2kuY3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5DQVNUNVBh\n"
             + "c3N3b3JkQ2lwaGVydGV4dAAAAAAAAAABAgAAeHIAO29yZy54d2lraS5jcnlwdG8u\n"
             + "cGFzc3dkLmludGVybmFsLkFic3RyYWN0UGFzc3dvcmRDaXBoZXJ0ZXh0AAAAAAAA\n"
             + "AAECAAJbAApjaXBoZXJ0ZXh0dAACW0JMAAtrZXlGdW5jdGlvbnQAL0xvcmcveHdp\n"
             + "a2kvY3J5cHRvL3Bhc3N3ZC9LZXlEZXJpdmF0aW9uRnVuY3Rpb247eHB1cgACW0Ks\n"
             + "8xf4BghU4AIAAHhwAAABGLXi0Ndwrq3OVzjWh9XgWyRBAHlq+3+cR3PGWVY8WM26\n"
             + "owReBtKHtThDLwqDeP8zdnBDQ9CWgZm26Kdnbsz/rLLGQqHI08KPG8exYREsOupu\n"
             + "STIQG0QaZ1kmYO6fwWDnskFl6VGo8mU8AqJfG7aVcAAySAREirBOox8ScOv/QnFP\n"
             + "VaX6UgYwRY29/aI0u/oAIDIcqcvLMnJHgybW6UZgp8t4E5yhETIEug/0CnloTqsk\n"
             + "Nys1YFKAEQTzabwTvAcIlt9YiFsA89SMj2foB2ZkvnTAewuLYpmRusbBcVNl4peG\n"
             + "0d92YQo21yxzkm6wO7T5DBg5XoyaBmPaAkVUDB2fCS7YhgrVoaEW49yHwvkVJ67M\n"
             + "3LwU9ifxoKtzcgBGb3JnLnh3aWtpLmNyeXB0by5wYXNzd2QuaW50ZXJuYWwuU2Ny\n"
             + "eXB0TWVtb3J5SGFyZEtleURlcml2YXRpb25GdW5jdGlvbgAAAAAAAAABAgAFSQAJ\n"
             + "YmxvY2tTaXplSQAQZGVyaXZlZEtleUxlbmd0aEkADW1lbW9yeUV4cGVuc2VJABBw\n"
             + "cm9jZXNzb3JFeHBlbnNlWwAEc2FsdHEAfgACeHIASG9yZy54d2lraS5jcnlwdG8u\n"
             + "cGFzc3dkLmludGVybmFsLkFic3RyYWN0TWVtb3J5SGFyZEtleURlcml2YXRpb25G\n"
             + "dW5jdGlvbgAAAAAAAAABAgAAeHAAAAAIAAAAGAAABAAAAAABdXEAfgAFAAAAEKQD\n"
             + "mYpvbMVTVoq8SKFAw6Q=\n"
             + "-----END PASSWORD CIPHERTEXT-----";
    }

    @Test
    public void decryptWithWrongPasswordTest() throws Exception
    {
        final String enciphered = this.service.encryptText(textToEncrypt, password);
        String decrypted = this.service.decryptText(enciphered, "wrong password");
        if (decrypted != null) {
            if (decrypted.equals(this.textToEncrypt)) {
                Assert.fail("OMG: decrypted with wrong password!!!!");
            }
            System.out.println("Successfully decrypted with wrong password but output was \"garbage\".");
        }
    }

    @Test
    public void encryptDecryptOneByteMore() throws GeneralSecurityException
    {
        // the last block contains only one character
        String ciphertext = this.service.encryptText(anotherText, password);
        String plaintext = this.service.decryptText(ciphertext, password);
        Assert.assertEquals(anotherText, plaintext);
    }

    @Test
    public void encryptDecryptOneByteLess() throws GeneralSecurityException
    {
        // the last block has one byte of padding
        final String shorter = anotherText.substring(0, anotherText.length()-3);
        String ciphertext = this.service.encryptText(shorter, password);
        String plaintext = this.service.decryptText(ciphertext, password);
        Assert.assertEquals(shorter, plaintext);
    }

    @Test
    public void protectPasswordTest() throws Exception
    {
        String protectedPassword = this.service.protectPassword("Hello World!");
        Assert.assertTrue(this.service.isPasswordCorrect("Hello World!", protectedPassword));
        Assert.assertFalse(this.service.isPasswordCorrect("Wrong Passwd", protectedPassword));
    }
}

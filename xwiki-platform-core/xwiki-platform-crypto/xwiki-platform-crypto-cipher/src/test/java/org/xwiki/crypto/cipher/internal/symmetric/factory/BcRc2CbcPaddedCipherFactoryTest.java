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
package org.xwiki.crypto.cipher.internal.symmetric.factory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.RC2KeyParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class BcRc2CbcPaddedCipherFactoryTest extends AbstractSymmetricCipherFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CipherFactory> mocker =
        new MockitoComponentMockingRule(BcRc2CbcPaddedCipherFactory.class);

    {
        CIPHER_ALGO = "RC2/CBC/PKCS5Padding";
        BLOCK_SIZE = 8;
        KEY_SIZE = 64;
        SUPPORTED_KEY_SIZE = new int[] { 1, 2 , 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64 };

        BYTES_ENCRYPTED_SIZE = ((BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;
        ANOTHER_BYTES_ENCRYPTED_SIZE = ((ANOTHER_BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;
    }

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
    }

    @Override
    Cipher getCipherInstance(boolean forEncryption)
    {
        return factory.getInstance(forEncryption, new KeyWithIVParameters(KEY32, IV8));
    }

    @Test
    public void testRC2EffectiveBitsEncription() throws Exception
    {
        Cipher cipher27 = factory.getInstance(true, new KeyWithIVParameters(new RC2KeyParameters(KEY32, 27), IV8));

        byte[] result = cipher27.doFinal(BYTES);
        assertThat(result.length,equalTo(BYTES_ENCRYPTED_SIZE));
        assertThat(result,not(equalTo(getEncrypted())));

        result = cipher27.doFinal(ANOTHER_BYTES);
        assertThat(result.length,equalTo(ANOTHER_BYTES_ENCRYPTED_SIZE));
        assertThat(result,not(equalTo(getAnotherEncrypted())));
    }

    @Test
    public void testRC2EffectiveBitsDecription() throws Exception
    {
        Cipher cipher27Enc = factory.getInstance(true, new KeyWithIVParameters(new RC2KeyParameters(KEY32, 27), IV8));
        Cipher cipher27 = factory.getInstance(false, new KeyWithIVParameters(new RC2KeyParameters(KEY32, 27), IV8));

        byte[] result = cipher27.doFinal(cipher27Enc.doFinal(BYTES));
        assertThat(result.length, equalTo(BYTES.length));
        assertThat(result,equalTo(BYTES));

        result = cipher27.doFinal(cipher27Enc.doFinal(ANOTHER_BYTES));
        assertThat(result.length, equalTo(ANOTHER_BYTES.length));
        assertThat(result, equalTo(ANOTHER_BYTES));
    }

}

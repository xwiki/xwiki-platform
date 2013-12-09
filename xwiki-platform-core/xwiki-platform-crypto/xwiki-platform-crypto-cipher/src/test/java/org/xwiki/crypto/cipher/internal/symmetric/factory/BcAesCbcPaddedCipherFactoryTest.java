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
import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

public class BcAesCbcPaddedCipherFactoryTest extends AbstractSymmetricCipherFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CipherFactory> mocker =
        new MockitoComponentMockingRule(BcAesCbcPaddedCipherFactory.class);

    {
        CIPHER_ALGO = "AES/CBC/PKCS7Padding";
        BLOCK_SIZE = 16;
        KEY_SIZE = 32;
        SUPPORTED_KEY_SIZE = new int[] {16, 24, 32};

        BYTES_ENCRYPTED_SIZE = ((BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;
        ANOTHER_BYTES_ENCRYPTED_SIZE = ((ANOTHER_BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;

        encrypted = new byte[] { -60, -101, 2, -99, 90, -34, -49, -105, -41, -120, 119, 32, -28, -84, -54, 101, 70, -13,
            91, -54, 16, 106, 43, 58, -33, -6, 33, -52, 11, 35, -75, 106, -12, -12, 21, 23, -112, 38, -107, 66, -104,
            58, -89, -63, -93, -15, 86, 8, 118, 101, -74, -52, -57, -114, -17, 71, -72, -120, 69, -5, -69, -107, -7,
            105, 122, -28, 80, 22, 117, -82, 7, -66, -100, 98, 124, 69, -100, 96, 74, -66, 38, 122, 69, -17, 16, -118,
            49, 96, 69, 16, 37, 26, -21, -13, -107, -125, -43, 18, -37, -14, 34, -70, 118, 5, -86, 95, 31, 62, -103,
            75, 84, -68, -44, -100, -120, 63, 56, -67, 94, 55, 31, -71, 11, 127, 74, 101, -123, -112, -47, 80, -32,
            51, 74, 117, 86, -29, -69, 77, -80, 25, 0, 36, 121, 64, 88, 102, -12, 29, -2, -70, 29, 78, -89, -128, -93,
            -70, -57, -88, -106, 114, 73, 61, -126, 14, 102, -39, -7, 48, -18, 23, 125, 14, 72, 65, -36, 52, -60, 6, 79,
            91, 7, 43, 104, 37, -55, -64, 57, 86, -89, 100, 37, 93, -62, 37, 49, 118, 124, -98, 73, -98, 111, -97, -39,
            -102, 39, -58, 44, 37, -81, 86, 102, -109, -51, 80, 124, -114, 54, 112, -83, -91, -12, 62, -125, 3, -84,
            -100, -121, -36, -52, -90, 34, -86, -20, -80, 64, -13, -32, 2, -52, -92, -13, -7, -73, 89, 76, -98, 111,
            -54, -8, -128, -50, -16, 85, -25, 61, 16, 103, -31, -92, 17, -103, 77, 76, -79, -75, -55, -79, 33, 36, 127,
            -92, 16, 70, 34, -96, 125, 48, 25, 21, -99, 18, 21, 41, 70, 100, 52, -91, -4 };

        anotherEncrypted = new byte[] { 49, 115, 25, -61, 48, -117, 51, -80, -15, -99, -10, 56, -86, 61, 121, -82, 94,
            21, 71, -59, -42, -116, -124, -97, 40, -98, -71, -108, 1, 29, -53, -47, 88, 96, 105, 28, -122, 15, -78,
            -72, 22, -52, 26, 49, 35, 123, -14, 31, 85, 29, 43, -116, 72, -123, 17, 33, 30, -84, -61, -68, -96, -91,
            -111, -58, 117, 120, -14, -40, -66, -62, 50, -11, 75, 78, 123, -78, 35, 125, 107, 120, 49, 73, -120, -41,
            57, -81, -28, 62, -55, -79, 36, 32, 85, 4, -105, -128, -99, 94, -9, -82, 6, -102, 106, -84, 106, 68, 80,
            108, 70, 18, 47, 104, 76, 58, 100, 58, 8, -76, -90, 45, -61, -18, -1, 105, -28, -80, 68, 78 };
    }

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
    }

    @Override
    Cipher getCipherInstance(boolean forEncryption)
    {
        return factory.getInstance(forEncryption, new KeyWithIVParameters(KEY32, IV16));
    }
}

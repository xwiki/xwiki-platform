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

public class BcBlowfishCbcPaddedCipherFactoryTest extends AbstractSymmetricCipherFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CipherFactory> mocker =
        new MockitoComponentMockingRule(BcBlowfishCbcPaddedCipherFactory.class);

    {
        CIPHER_ALGO = "Blowfish/CBC/PKCS5Padding";
        BLOCK_SIZE = 8;
        KEY_SIZE = 16;
        SUPPORTED_KEY_SIZE = new int[] { 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
            52, 53, 54, 55, 56 };

        BYTES_ENCRYPTED_SIZE = ((BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;
        ANOTHER_BYTES_ENCRYPTED_SIZE = ((ANOTHER_BYTES.length / BLOCK_SIZE) * BLOCK_SIZE) + BLOCK_SIZE;

        encrypted = new byte[] { 101, -74, 57, 30, -11, 114, -16, 44, -127, -50, 71, 60, -33, 36, 24, -2, -43, 111, 118,
            115, -68, -124, -96, -26, 88, 121, -95, 18, -92, -67, -42, 14, -30, 106, -128, -74, 113, 80, -40, 26, -39,
            11, -40, 68, -126, 100, -63, -128, -120, 24, -107, 103, 42, -113, 44, -21, 79, -18, -4, -125, -84, -84,
            -111, 83, 21, 101, 87, -52, 76, -91, 36, 33, 116, -43, 21, -51, 59, 25, 14, 71, 10, 108, -29, -61, 94, -85,
            46, 57, -104, 70, 27, 15, -31, -124, -36, 110, 113, -123, 67, -9, 33, -18, -32, -67, 31, 88, 5, 27, -110,
            -20, 10, -74, 107, -39, 54, 57, 112, -116, -127, -26, -43, 108, 110, -128, 44, -47, 23, 45, -106, 90, -94,
            5, -61, 69, 18, -37, -23, 8, -64, 112, 39, 90, -90, 112, 96, -23, 62, 19, -115, -19, 47, -51, 112, -114,
            93, 69, 87, -105, 35, 8, -104, -72, 97, 126, -12, -31, 85, -83, 14, 4, 75, -106, 63, 6, 48, 65, -71, 21,
            82, -30, -123, -92, -71, 99, -69, -35, 119, -12, -15, -117, 46, 61, -112, 62, -60, -21, -43, 120, 29, 21,
            -4, 85, 109, 79, -70, -84, 11, -40, 32, -19, 27, 115, 52, -114, -124, 104, 12, -25, 67, 117, -8, -40, -18,
            -88, -45, 12, -76, -29, -24, -106, -46, 100, -115, -57, -124, 87, -118, -90, -94, 71, -97, -34, 32, 44, 2,
            -19, 126, 56, 63, -33, -117, 96, 29, -21, 5, -105, -85, -123, 106, 106, 94, 48, 35, 18, -51, -27, 4, 74,
            -42, 88, -95, -9, 82, 31, 77, 110, 21, -10, 99, 24 };

        anotherEncrypted = new byte[] { -41, -79, 38, 25, -127, -125, 60, 77, -113, 62, 53, 111, 22, 77, 109, -107, -25,
            66, -98, -18, -56, 104, -84, -37, 54, 27, -103, -54, 63, 75, -40, -49, 102, -71, -93, 16, -70, 46, -48, -76,
            -64, -42, 114, -14, 23, 50, -44, -62, 126, -58, -99, 76, -103, -47, -21, -52, -56, 101, 72, -115, 58, 48,
            -99, 102, -113, -43, 73, -124, -25, -27, -121, 43, 9, -5, 88, 12, -47, 81, 26, 107, -92, 1, 79, 111, 61, 10,
            101, -67, 76, -64, 117, 61, -39, -103, -64, -47, 39, 72, 58, 59, 95, 15, 94, -126, 33, 89, -81, -10, -120,
            -86, -12, -36, 117, -77, -22, -74, -58, -70, 42, 34 };
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
}

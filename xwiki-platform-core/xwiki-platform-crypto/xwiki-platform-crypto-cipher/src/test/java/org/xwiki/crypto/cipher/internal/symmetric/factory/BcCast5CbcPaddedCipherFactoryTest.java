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

public class BcCast5CbcPaddedCipherFactoryTest extends AbstractSymmetricCipherFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<CipherFactory> mocker =
        new MockitoComponentMockingRule(BcCast5CbcPaddedCipherFactory.class);

    {
        CIPHER_ALGO = "CAST5/CBC/PKCS5Padding";
        BLOCK_SIZE = 8;
        KEY_SIZE = 16;
        SUPPORTED_KEY_SIZE = new int[] { 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 };

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
        return factory.getInstance(forEncryption, new KeyWithIVParameters(KEY16, IV8));
    }
}

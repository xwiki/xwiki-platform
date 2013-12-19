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
package org.xwiki.crypto.password.kdf.factory;

import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.KeyDerivationFunctionFactory;
import org.xwiki.crypto.password.PasswordToByteConverter;
import org.xwiki.crypto.password.internal.kdf.factory.BcScryptKeyDerivationFunctionFactory;
import org.xwiki.crypto.password.params.ScryptParameters;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Please comment here
 *
 * @version $Id$
 */
public class BcScryptKeyDerivationFunctionFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<KeyDerivationFunctionFactory> mocker =
        new MockitoComponentMockingRule(BcScryptKeyDerivationFunctionFactory.class);

    KeyDerivationFunctionFactory factory;

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
    }

    KeyDerivationFunction getKDFInstance(ScryptParameters parameters)
    {
        return factory.getInstance(parameters);
    }

    @Test
    public void scryptPropertiesTest() throws Exception
    {
        assertThat(factory.getKDFAlgorithmName(), equalTo("Scrypt"));
    }

    @Test
    public void scryptConformanceTest1() throws Exception
    {
        assertThat(getKDFInstance(new ScryptParameters(64, 16, 1, 1, new byte[0]))
                    .derive(new byte[0]).getKey(),
            equalTo(Hex.decode("77 d6 57 62 38 65 7b 20 3b 19 ca 42 c1 8a 04 97"
                                + "f1 6b 48 44 e3 07 4a e8 df df fa 3f ed e2 14 42"
                                + "fc d0 06 9d ed 09 48 f8 32 6a 75 3a 0f c8 1f 17"
                                + "e8 d3 e0 fb 2e 0d 36 28 cf 35 e2 0c 38 d1 89 06")));
    }

    @Test
    public void scryptConformanceTest2() throws Exception
    {
        assertThat(getKDFInstance(new ScryptParameters(64, 1024, 16, 8, new byte[] {'N', 'a', 'C', 'l'}))
                    .derive(new byte[] {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'}).getKey(),
            equalTo(Hex.decode("fd ba be 1c 9d 34 72 00 78 56 e7 19 0d 01 e9 fe"
                                + "7c 6a d7 cb c8 23 78 30 e7 73 76 63 4b 37 31 62"
                                + "2e af 30 d9 2e 22 a3 88 6f f1 09 27 9d 98 30 da"
                                + "c7 27 af b9 4a 83 ee 6d 83 60 cb df a2 cc 06 40")));
    }

    @Test
    public void scryptConformanceTest3() throws Exception
    {
        assertThat(getKDFInstance(new ScryptParameters(64, 16384, 1, 8, new byte[] {'S', 'o', 'd', 'i', 'u', 'm', 'C', 'h', 'l', 'o', 'r', 'i', 'd', 'e'}))
            .derive(new byte[] {'p', 'l', 'e', 'a', 's', 'e', 'l', 'e', 't', 'm', 'e', 'i', 'n'}).getKey(),
            equalTo(Hex.decode("70 23 bd cb 3a fd 73 48 46 1c 06 cd 81 fd 38 eb"
                                + "fd a8 fb ba 90 4f 8e 3e a9 b5 43 f6 54 5d a1 f2"
                                + "d5 43 29 55 61 3f 0f cf 62 d4 97 05 24 2a 9a f9"
                                + "e6 1e 85 dc 0d 65 1e 40 df cf 01 7b 45 57 58 87")));
    }

    @Test
    public void scryptSerializationDeserializationTest() throws Exception
    {
        byte[] password = PasswordToByteConverter.convert("password".toCharArray());
        KeyDerivationFunction kdf = getKDFInstance(new ScryptParameters(64, 512, 8));
        KeyWithIVParameters params = kdf.derive(password, 8);

        KeyDerivationFunction kdf2 = factory.getInstance(kdf.getEncoded());
        KeyWithIVParameters params2 = kdf2.derive(password, 8);

        assertThat(params.getKey(), equalTo(params2.getKey()));
        assertThat(params2.getIV(), equalTo(params2.getIV()));
    }
}

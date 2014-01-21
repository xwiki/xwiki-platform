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
package org.xwiki.crypto.password.internal.pbe.factory;

import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.cipher.internal.symmetric.factory.BcAesCbcPaddedCipherFactory;
import org.xwiki.crypto.cipher.internal.symmetric.factory.BcBlowfishCbcPaddedCipherFactory;
import org.xwiki.crypto.cipher.internal.symmetric.factory.BcDesCbcPaddedCipherFactory;
import org.xwiki.crypto.cipher.internal.symmetric.factory.BcDesEdeCbcPaddedCipherFactory;
import org.xwiki.crypto.cipher.internal.symmetric.factory.BcRc2CbcPaddedCipherFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA224DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA256DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA384DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA512DigestFactory;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.PasswordBasedCipher;
import org.xwiki.crypto.password.PasswordBasedCipherFactory;
import org.xwiki.crypto.password.PasswordToByteConverter;
import org.xwiki.crypto.password.internal.kdf.factory.BcPKCS5S2KeyDerivationFunctionFactory;
import org.xwiki.crypto.password.internal.kdf.factory.BcScryptKeyDerivationFunctionFactory;
import org.xwiki.crypto.password.internal.kdf.factory.DefaultKeyDerivationFunctionFactory;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.PBKDF2Parameters;
import org.xwiki.crypto.password.params.ScryptParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@ComponentList({DefaultKeyDerivationFunctionFactory.class, BcPKCS5S2KeyDerivationFunctionFactory.class,
    BcPBES2Rc2CipherFactory.class, BcRc2CbcPaddedCipherFactory.class,
    BcPBES2DesCipherFactory.class, BcDesCbcPaddedCipherFactory.class,
    BcPBES2DesEdeCipherFactory.class, BcDesEdeCbcPaddedCipherFactory.class,
    BcPBES2BlowfishCipherFactory.class, BcBlowfishCbcPaddedCipherFactory.class,
    BcPBES2AesCipherFactory.class, BcAesCbcPaddedCipherFactory.class, BcScryptKeyDerivationFunctionFactory.class,
    BcSHA224DigestFactory.class, BcSHA256DigestFactory.class, BcSHA384DigestFactory.class, BcSHA512DigestFactory.class})
public class BcPBES2CipherFactoryTest
{
    private static final byte[] PASSWORD = PasswordToByteConverter.convert("changeit");

    /**
     * Sample RSA KEY and encrypted versions taken from not-yet-commons-ssl source code.
     */
    private static final byte[] RSAKEY = Base64.decode(
        "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDIY6+Wgj6MqdEd\n"
            + "Yq6FgH5xMgTBmFqAonR/eshjxY2C6MHs+WmCmNSDik2NgZWIaODvOF9uOEK2U0Zf"
            + "JEG2LcZxoeIEgg/mfII2f4DLy1JYajm/llzwFBzAd/Rkcs3qwP2ba5VKn/pSqNLl"
            + "nKHMXkXO+9SjfHDx95x2dK1dB8eGQGculOMcTm3uK7UlWNO4TSlwG9qHZ1aoM3GI"
            + "g5C1fIpbxJqDVjFq6fFAapE3KRIWIQmKd3E5ICcDErqr/AapxnfO8UFNxVWSOLW7"
            + "ZAfis4w/c8/EAgyQHw42R0dNyjUOZsToF8McCsOpRjGolSU8aUyqspvd8IWJPd5d"
            + "6HBHueXNAgMBAAECggEAV3q9MpVVPQ79TTjBO2Km0D+nt+QMzk8dUHGHfZbGejmm"
            + "Pw96shqJ24rK5FWHs+8lEwmnD3TcGsAr3mjzjtZY5U5oXtNwoYwFRElRLqZqIlLt"
            + "NugrVltRWeyD8j30CuGJVQoYOGWyX9d3ielg8NjO3NcvMtembttLoKK68/vrbH11"
            + "9W7wr5p8/xyMfyl9curnmCFk5QqJ1FBpjPWY05NDIBCUJB0tGAqViCpxEeWPSlvb"
            + "xcElqWfdbtnsYUxYU+iOTHHotoKnz4nLHYK2/njMhlCEyMXfu1DJOd8rg5yXewJF"
            + "v6NhXgWStSexAT1bZ17LROazVcHfWB9QmXF1Fm7vOQKBgQD+dZxPDOi3Y4gCFegn"
            + "Z+epNyl2aPTkseEZxrIqPKLHsGxUfYjQqkX2RdfTrq2vf4vFlN6uCXhSlZKXfLH/"
            + "iQ8FAzqenhVVHK2fv5xB0SE5zNmcHDrHshl+/zUNI2u5AMFECVO2SVbgoFjvgkou"
            + "FolK8XUXfHfb4f732LUyYI0lEwKBgQDJmkWHhzekz3P5iWaAt1SH8bZpt2hqa6Bx"
            + "A4VvMdtmjCxEDETN0Rb3CPYxw3qa3xGfW1y1j/49xi4gr69yaT2Tbca7PFGUmWRo"
            + "OJwfCUB5uBUi6UVytK19OVKReOm4666x8P3YO4cxxSI/HeoSU0HR1kkX9rGmrsGN"
            + "MgUQ15+FnwKBgAKf6/DUzUG3ARwkZbSiWb1hGEhkZMJHI29EoWnWHke5BiUI9nRQ"
            + "jVAxADzqvFfnFOYA1xssddVEPbLaUmu0WjdPBTfFoaqzFQdkzpPPOGyENGpr0B9n"
            + "MuQgdceg6eeKnnO5NOfYcdD3VnOCAInhKaFgRDjty7604hBkZ9oRLOOJAoGBAIJ+"
            + "dmUMlGr80XADjTLh+DhqsA1r542DDv44LkXUetS9BOYjHuIuZnQO+/UoOBNJMsn4"
            + "xGDNzN7FihQkRCeFkZL9arbFi3TpeUGw6vV38qEXE69eWVKvOuEkmpqJLphBDfom"
            + "KNmvZopDtTAvt9SWybL+xp9ZUpK26ZfwebD2MU63AoGBAOa2Kt01DxoqiWiQP/ds"
            + "Mc9wOw1zJsSmIK7wEiW3FkCz8uw3UgjF9ymYY/YAW3eZtuUpuxEzyENb9f21p4b2"
            + "zYoZ7nCUo4TmVXxgCjiEWglg3b/R3xjQr1dAABhTeI8bXMv5r/tMUsnS79uKqwGD"
            + "2Gc1syc3+055K4qcfZHH0XWu");

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> mocker =
        new MockitoComponentMockingRule(BcPBES2CipherFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> desMocker =
        new MockitoComponentMockingRule(BcPBES2DesCipherFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> desEdeMocker =
        new MockitoComponentMockingRule(BcPBES2DesEdeCipherFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> rc2Mocker =
        new MockitoComponentMockingRule(BcPBES2Rc2CipherFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> blowfishMocker =
        new MockitoComponentMockingRule(BcPBES2BlowfishCipherFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<PasswordBasedCipherFactory> aesMocker =
        new MockitoComponentMockingRule(BcPBES2AesCipherFactory.class);

    PasswordBasedCipherFactory factory;

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();
    }

    private void PBESEncodeDecodeTest(MockitoComponentMockingRule<? extends PasswordBasedCipherFactory> mocker,
                                      int blockSize, KeyDerivationFunctionParameters kdfParams) throws Exception
    {
        PasswordBasedCipher encCipher = mocker.getComponentUnderTest().getInstance(true,
            new KeyWithIVParameters(PASSWORD, blockSize),
            kdfParams);

        byte[] encoded = encCipher.doFinal(RSAKEY);

        PasswordBasedCipher decCipher = factory.getInstance(false, PASSWORD, encCipher.getEncoded());

        assertThat(decCipher.getEncoded(), equalTo(encCipher.getEncoded()));
        assertThat(decCipher.doFinal(encoded),equalTo(RSAKEY));
    }

    @Test
    public void testPBES2BlowfishWithPBKDF2() throws Exception
    {
        PBESEncodeDecodeTest(blowfishMocker, 8, new PBKDF2Parameters(16, 2048));
    }

    @Test
    public void testPBES2BlowfishWithScrypt() throws Exception
    {
        PBESEncodeDecodeTest(blowfishMocker, 8, new ScryptParameters(16));
    }

    @Test
    public void testPBES2AESWithHmacSHA224() throws Exception
    {
        PBESEncodeDecodeTest(aesMocker, 16, new PBKDF2Parameters(16, 2048, "SHA-224"));
    }

    @Test
    public void testPBES2AESWithHmacSHA256() throws Exception
    {
        PBESEncodeDecodeTest(aesMocker, 16, new PBKDF2Parameters(16, 2048, "SHA-256"));
    }

    @Test
    public void testPBES2AESWithHmacSHA384() throws Exception
    {
        PBESEncodeDecodeTest(aesMocker, 16, new PBKDF2Parameters(16, 2048, "SHA-384"));
    }

    @Test
    public void testPBES2AESWithHmacSHA512() throws Exception
    {
        PBESEncodeDecodeTest(aesMocker, 16, new PBKDF2Parameters(16, 2048, "SHA-512"));
    }
}

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
package org.xwiki.crypto.password.pbe.factory;

import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.encoders.Hex;
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
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2AesCipherFactory;
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2BlowfishCipherFactory;
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2CipherFactory;
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2DesCipherFactory;
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2DesEdeCipherFactory;
import org.xwiki.crypto.password.internal.pbe.factory.BcPBES2Rc2CipherFactory;
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

    private void testPBES2Conformance(MockitoComponentMockingRule<? extends PasswordBasedCipherFactory> mocker,
        String iv, int keySize, String salt, byte[] data) throws Exception
    {
        EncryptedPrivateKeyInfo info = EncryptedPrivateKeyInfo.getInstance(data);

        PasswordBasedCipher cipher = factory.getInstance(false, PASSWORD, info.getEncryptionAlgorithm().getEncoded());

        assertThat(cipher.doFinal(info.getEncryptedData()),equalTo(RSAKEY));

        cipher = mocker.getComponentUnderTest().getInstance(true,
            new KeyWithIVParameters(PASSWORD, Hex.decode(iv)),
            new PBKDF2Parameters(keySize, 2048, Hex.decode(salt)));

        assertThat(cipher.doFinal(RSAKEY),equalTo(info.getEncryptedData()));
        assertThat(cipher.getEncoded(),equalTo(info.getEncryptionAlgorithm().getEncoded()));
    }


    @Test
    public void testPBES2ConformanceTestDes() throws Exception
    {
        byte[] data = Base64.decode(
            "MIIFCzA9BgkqhkiG9w0BBQ0wMDAbBgkqhkiG9w0BBQwwDgQIGBflqeZFz8sCAggA"
            + "MBEGBSsOAwIHBAgLbTq8mvQ7QASCBMhUPEBJHO0Uvu4bW2bfb0NIvczMuPbFQDQC"
            + "GnmzPGsa6vLcE+z61rBmPLOT45ZWcbTf867wh0e0KKFERkDh70lgenwYACRcefoE"
            + "ktuvdjRBpMiSAqr+ucD3ZwyIVp1+I5xUJqLhqxV1Wd5pkwFaZsqxUCRd6DQmZvH4"
            + "kVw2ulaE1HfT0XTPGO/ewBQ72wZ/XiegYsP7yp4dNIfmwMk03A+V0K8PjQTY6Sui"
            + "IHeoe9YT0b2J3ocsf38pHAiTmsgnl0ZtedcjlOYJ8yVxXu6h8FO+Xu0MXYGN387J"
            + "m4OUqLFudtWt8oVTVQuevNO+EzP061gsZnTEUJNvQNwLajdQnmER5ht28snby3tM"
            + "f4pzwu6asM5RHDD2FBSp5sBD5G/ev0hM6R5MkTBAEcz0CB/TYSMKq1IfWCqHkY1z"
            + "bvd7Bm19vwoyszXBGEBmhaVC34kYXo3IoTw3ezvXPgSaNk6IUvFavZRvAuJLleWs"
            + "cpN13G7pbi6WeCYQzml3L2hLP62DP+VLwkYyxUW3CvwLhVP1h2w12tb+cKC+4uih"
            + "4Of7cjt0R99vNvyTuPJ4HDjcn22KT+NJwPDNviLcBrNs1XxCXFa211nZWDGDC2cf"
            + "6lCd1JVQczewMfQuFX8lUMuZOfLoAN+wq+ZWLIEtIMC+4bciZWYxzKFijOWKqnd8"
            + "JhnOTKZ3Ua1txrAYE4Z0jLYI4xXImnGoW42h882aEiWSzsGARD065zXiQr//b5ot"
            + "TtFXfJ3IiJMMTyss93mirPPzcua8uoGvwvjw3GoxLbNIttX0hmR1NBGyM3MUnghy"
            + "7lrQssx2FcENuI/zAq/6l3ZT2jGtixch+Eu8mrCBTthWRBap8KIcO428c38C/IJ3"
            + "bdWmWJw0r401gqbZS2ZoSi0ia+RFM/DByrQl1Cww8Z81v6S4tedxMbmSiI5/vpXt"
            + "TcM4xQctZr/shYU9CDpxLUznmaRXbnFb77S7ykrJdPgnk+Oq5GYIhrcrxXXwF7+S"
            + "wg+72F5MVkFhqhzsZ8VGbgpgSw+BFDJd9wHiLM87ywQW4eHYqJfQtO1lPfzAkXfz"
            + "oPDyGX/Q6Nisy7dVErZ4dTXlUKxXEXAfvZABA08/L+3w+p3HEWm1HJFpZitEKHas"
            + "dheCNwnwmmyzItSjXBE2kBg9ZtH6Zx9mQpU2ra75dl+X2SvV8096lh7Xt0uHgjgq"
            + "vtYl2Bc9yBnxcwf6ppI8+VzBTxT/Q3BBBTL5e8RuYlSB+9BTlsp/bgP2prMFL+Lr"
            + "DJUisjPHJn3gPeG09lcWnAzTdw07/JN83WV3HHRjJ/ot8TdoVix/qlrtK7IPkTH3"
            + "hyLhPA4V49aOz7f42Jg3Xb0/C3WUEaP3aJ6UEoCVd0856RN9CTOlG95SPH+IoDLP"
            + "33oFlrRMlqQ0K8Y/j40h/3VbBxaHeWCNiZr3sN5ybxNkT8TaXOr2kWfePsYdGf7c"
            + "Pbz3IEwUeOD3z/GzDdiYvOfEo4quECEEQj9dWedOVVxD2wtc5jbaaB+HEaxlPNg5"
            + "5Sqd3F7IPtpoBGl9OOgigBJIP6JQWctjYJYjLGnVn4GpXt8pWclmkQ+SsCSCeYnX"
            + "Hax2rVBqD3yMRpVw225Dzxb84R1hleqx/N6VJpjmk2agYCTwXKsMyd2tMROOJxo=");

       testPBES2Conformance(desMocker, "0b6d3abc9af43b40", -1, "1817e5a9e645cfcb", data);
    }

    @Test
    public void testPBES2ConformanceTestDesEde3() throws Exception
    {
        byte[] data = Base64.decode(
            "MIIFDjBABgkqhkiG9w0BBQ0wMzAbBgkqhkiG9w0BBQwwDgQIqpcsbmHTx+gCAggA"
            + "MBQGCCqGSIb3DQMHBAiEYtVFOafp9ASCBMhnk03sYcXPAU1eYthAr8vueotiNFFr"
            + "xpbIbhB2cjC/bXd39GxyH/8X36KjVMuBJxFknY630SOBsdTn/H7lN/G8c+fEYTcq"
            + "porajF/pxr7asdFl94xjnx/DGaQmUJooqqUp8p80On1Otj7xEZtTivlEISY5Pr6y"
            + "6ou+fKIFKK5QzDzD9C+vLzYJMqhk4YamjxvDOwzYUoDFl+enMHXcuZYpA1yEvqze"
            + "5lFk+9AxdTbHgOsPECqS3bkq64VGanrfFtqywqLLLhsakcnByjddcHdsFFvhu/dH"
            + "raFdV3vsHppedqxTHbfFpvVl2+9XTpeBzwvb9XcJ2WUi4JUMtcR1gZwOo8S6q8LL"
            + "UWzTbGjsZVG3ew4FZ5avAjUCGgiUR47LFleSJ34eWrllUptHzfVTpV5f787gfN1g"
            + "BRhAJ1YJTWqcalIhd9YEKFxI3sjfWAOLdUW4at4z4yuRZh/uUck4V3ken8/M4JCG"
            + "/Vy7B9Juf9uHRKFWtl3hYDY32UJoQt0cH7V76y/rjpbhvhVtKaAbcK4ccZ+4JuDb"
            + "9qbs81Joe8jW4OZtYL/L+g4WboW/FSGlJXxJIP674lnglXXB5C8HT/I/7CzrEEgv"
            + "wcKKAwZ25+aR7pCYqWdDLUaENQQ/Z1mARGDw9JGszonXJMdMWnQN89DXsmkp6qe/"
            + "YkmZ3Sm4rrvz9LKc7mW+v+d+WnPTeJ4FBHmnUhGkuZroDl6SrCPjm3qFqj3zvyyV"
            + "8Lk8xNMm6AEN8IOs7qeUAb811bdpfUzLebZkuNXDMl8KCoyt+nNF8UN7vpCXU3/c"
            + "PEJahN+XxmRftOR0KKBIKCMKgnR90JenNmSyW2BJbT9D++7ujYoDlL0gTfbYa34a"
            + "P4H3ChnU0s5KMA2h48UBkDUlwCOn4L860h4sZIDfRkWcptB3HFipeKP0npGAfWfb"
            + "sQhueC7Ue8XpZIq5QBDOa+Zle3l8KZdmC4+sZYTsDoYm6jKX+LPgUAvvwyCBEUS7"
            + "0zVrBgXTwFDFuGt5LYROcngfRggekAMa7gWTPbwb0mQgCIvKaVxlu3FO5cNi4ExX"
            + "tlUUJyNualG+8qsbihqhHQBPs6QYhQ95y4ZndDqMtoyPWLhcOMo/Z/CLMQNhpXRl"
            + "okJndM7TjzFW05Fqy+cQwl8LTdGVZKUOGW/RwYMD2TPxXEp/XGEDlnREHVjppToT"
            + "+SlUlc7Dh31PCz5PoRSTvpJrsV2LZAUO9hoGoDYjfBqfUbrd5mFIii/52eJva7o1"
            + "RcrsHEkLTvu4BOk3Ztp3Zi26atBraADZ2T6UqwrfzZZrD1gI4sORuKN4/jmqt0KQ"
            + "H4FMJkKZgvJ0Rfjpt6ZqRj99wSnwNfDvr7641WTpB07Ati2LI3Tr6Fx7f2Phfkg1"
            + "SbzEC+qt3bixORhzFp0PbcULwamSsNVODf/EQic56bKZjOFEC+DFw3LKpvvBG1Fj"
            + "kGWbfZhSA2GZFUghvhFbFOsuwKh/GDoetFwKXWcM8DM0+aHSPwL61Y5FQ0Kn4DnB"
            + "ci3lJnSctD0fN3ckDIcTU/Nnza/RMJn1jRwBZc2Ql7/lA98jCM8uKTMVJy76ABf9"
            + "8lQrLGp6tSMD6jo31DMLVmKA9qed7Mk4QeZiDTm0/6d2GZ6hO8xwvzFRQ6Qy1ETG"
            + "W5c=");

        testPBES2Conformance(desEdeMocker, "8462d54539a7e9f4", -1, "aa972c6e61d3c7e8", data);
    }

    @Test
    public void testPBES2ConformanceTestRC2() throws Exception
    {
        byte[] data = Base64.decode(
            "MIIFFjBIBgkqhkiG9w0BBQ0wOzAeBgkqhkiG9w0BBQwwEQQI4aRQrNlj6ZECAggA"
                + "AgEQMBkGCCqGSIb3DQMCMA0CAToECG8qyWz/E/DoBIIEyFBBZXtbGNaIuDvM/J1u"
                + "l8gKw+zfurof6G7BSxefE8zuVFVhRXc7MnLQml97awOeEzvnGC7y61JELsZ7ROqa"
                + "1JWjdfURTsxxE4DGyUe+jOu2A1YSsnADCWq2bmvAkbFbV9N3Z1LqIkJNPtfS9KJQ"
                + "ukDscB51M3m0Y8PaQDrGFBdsRK/lAErOxdLkfRYQItqUnzawF1FuysiYlcQiCC0e"
                + "JrwiN9B/wW4bvnhQXABlQeI02MsfMPX8GLrlRfoNojLKTMCL1d+WK6dKuIA0QVRq"
                + "bXzAkqWe2VjIaOHXcbzk9OcLmJAHkGX0pwkYpgSDyD2yqXB4thhkvCc9hlRQObMn"
                + "Qvj2k5wyc3FzExWRwflTDZqpW9dFGCzjyzvOzfyJ+MWgS4e4a76vPEa79CMTWZcn"
                + "UgNuSK65g7wRLFm6Ko15jxKAEP9zEGV+mKNBMmkRvL4aWWwbJOK48GYar9RaqC8u"
                + "qbMTeQB/nfk//2JdxDR5W51dCSk5z64v/1ZEiLz96Y8GV8cc7PwVVjotW/yJEQv1"
                + "mhWBqeiGLRQdymhU8AKwiNxhw2m1b/CW+pjOUpB21A7P8q3j2h5EqhD1VXneB6pH"
                + "+RTvFOUzYWp4mz21Ul8qXzc0CLsDm+Lywiqv9hDKMtaCe3cNrR/PFNoFuRYO6fZ7"
                + "OOK3R7DQ/Us8cMpUJBCuYmBBwQB81fDxiHJdvtq8BmToJ6EYh4KzyTg4SGC1CPlh"
                + "KjWkzIf0w7eZZj+hP4tKsrkeFsJ3/QSQW53bH7sNxp7uiSEwFpi3Sa/n3ABbOLit"
                + "HR+O3GSfQIQMeLdZ5kcG2ww1SFEpaYBryBJZPfF7xF4rl5fWkrLasAPgUpTxsZ52"
                + "UdD6bZb/8Ij3ZkjNJIRaGzCMlfWbUYpM4xBZ4bSQL2OGDYxC37T8TU4s6PIwGpB2"
                + "zux3razpSBMFM6cT6ROw0zTzQpbDNF3U/wjOoyZmqRNpyr9AfXlsnxjXYzevmBUU"
                + "IHEXUjEwhoGrVAv0U67YFgxLay9bWU76q6StODcz4flrcgf6Z1SK1J9WnX1FMYGK"
                + "5HK0krzesjdFSqsx7T4VNv1paf5ql8mvMrIMTT2envm2vwMVG6EkOhGLjVeF/wPA"
                + "0URcR8Fw9Wh9enObpocFnkqKiQfpyTOLBRIcYdJ07s/ER3HmqcnOPO4am5EqHeUu"
                + "+3YPVrNuV9E+BMNGKcMFzda5MIJi0rbYlF59Bz5xy8+nxiUqN2SvJYIMh/6egBue"
                + "MEq/6O6ex5ypPFMlXItmubd3cqW6WMMOGuAxj0ciFLzZTxOwGDqh4G8GvPTZ6OtL"
                + "fq2mitjPkLOKHplJ+Mjzd6qQIXxv5S7utPvpVnJPimD3jdwfWFXI5sO8pIjTVgbJ"
                + "ZzkDw8ilLa5dR4e72+KHSlygjy0w2cvDX8kDfZyiRE30gIGQiHH7l/0Sv1GtOxpd"
                + "2tmDTdA3zaI7W/gukmsMrpfBbGgHVC/YKlRVnfWFEh+a/4LLhc5gywqxyZzC59Dc"
                + "nDRVPwSIPwWsx74ViClmviq4j0QV+n94JaQ2exgVvddBjluR7+9F+6+BSbfrgCeT"
                + "nTAowzDF15LOg+/wXaAnSerzIwB1s9xfYyssGuKd31Rc6FBH+iZTZorVGS+8UARa"
                + "yLHeQoz3pbRtiA==");

        testPBES2Conformance(rc2Mocker, "6f2ac96cff13f0e8", 16, "e1a450acd963e991", data);
    }

    @Test
    public void testPBES2ConformanceTestBlowfish() throws Exception
    {
        byte[] data = Base64.decode(
            "MIIFDzBBBgkqhkiG9w0BBQ0wNDAbBgkqhkiG9w0BBQwwDgQI+SHyL0TmEd4CAggA"
            + "MBUGCSsGAQQBl1UBAgQIe5OfjWHHpAMEggTIr/Ma8RAEsq65a1K/AooVkDgwtqRD"
            + "Q6A5Wx5LOvXRvQ4RLd11ham+GCWxHKWHeMTnLLrW2gQyDdEvMPtM0TRoGmm31npu"
            + "soEDFLIruE5XUmpzsKZkqztVhNl0pamzsqD6g7vSMlckFORsRInuWq3THVIDoHVi"
            + "m54JE5RCa/tZMVjmvvYOt1KHJhAqUeyb+CQYZsO6c+mjNBZrvdNFDz4bgutVKreL"
            + "FQMYJWr4sClAqnvHCvra2SV/mwIBZ1lYj920Qq885+HoSSQfgLOwar5XYCyEb7z1"
            + "Q+NYMjaDqhEIWvlXsXu7B2YIYoSC6vse4bfXvi93fJtb1CXVjWnDKC8xJs9pqVCx"
            + "t1P9JsztRHZ5g1daHwu6WQwD/7YRubi0y9pYjHB6H4M7KSWX3CEz/hDccUWoZHKH"
            + "D/R6cNe12RqVqRr0a5tteUCvFwbXCcwKbTG8n5zd3MS+2FF29S8jvhqNXJOPuykS"
            + "Sxq1wi73puQWCXAmw5aYy8RBlv9y3TlcaTDO37AkDdhJkWQLWbNy5MzLmh7dhNCQ"
            + "R2Qy4wz9Mw/1GZYhOYM0kSovMSHcN1jIvw94YPAfflmL+k6VtI2C1iyD4BG8li1h"
            + "ChCqL02fiLYjBsZquOdE5Fkg6tT6IucB0x35Qfjm8lTwcbVnjVRHnaQyPIMZeN9N"
            + "UdZqzyb7nf0zI3rbrCHFMPsGX5741VGt2Mz9t/rQEXjezgkEuxn54Z/Q39Rm0vmX"
            + "LfcKznYCVdMkomC0tIaYHBjVQWZerBRIHPGUhozdKwwASYco+FuJNNc9SZZnJ5AU"
            + "ux2yD3NTec6c/fgseJXi3UT/qDDmUQxX9hjwnUw8CPHUjXWGTmJ/ihfQVGo1ypwg"
            + "oRZWtF0AnxB7ZfdtnW3yq5/xCFzsIrKyFy4s/kdsylC8eGw4snGA19PN23eEyTZD"
            + "bSuW5KLjgScbR+ro+e3Y1F3OTI6+VF2oMLY31XVyt8S1N2Zz1lpoN0JfKaWTrTTJ"
            + "9Ic0oe2qtgqC42g11WHQizYAe83cZYNZgzU9mRa1FIqQD4kMQh+Vv9Cca6FpdPUv"
            + "D4GS7/fA5Ez5jFV7VghOCFfK2+1Q95b+2RnQEpmHk60+U7tZG4dACXLL7i1hbJH6"
            + "YuokBuw0Wjm5dicOSG2DptwzyqFfy7C2DYgB3Gi34fSNFxKis7CeKQLNMqppmBof"
            + "q379AD8hY9Bc8gUWAwMONs9AreDiRCGyR+IokQPTgKRuVzR7ISoxWkKvfW5XIdNf"
            + "Pd5kGLhX0HfGhikQCVP2XQMkOqsJw4ShV+FAcMfzaq54/PzXoemkRCOMsCzsDUde"
            + "C6XGF1wviq52P+DnAr1tDmYGOP+jP+O0shzyXF/res1CZ0PjiLIyUb1JM8m5eMRH"
            + "dEwzX/btO/A/OwNwy5FzS7VdFwaGUjFBXK3F/xyu8jl6ov02/ooddkpqNm5Cxko2"
            + "ZlJG4LXdDbO1y3rbdwSgukYS2SeUmGqLpONlGi6u8kGRrgetcCAzkyT+XQkV3ZWg"
            + "TFfGxr3q3NV0C35hIKBxLXr/+ZZHDP4Yx6/Ok6xYnUyJ7RoM6QE7Ppyo0hFPv0Q3"
            + "VknhyfXk/85k81dxWLWV3DiXZbW/9gGa8TGRuZxliqTr1YjTRGiUwn+fCVx7YLyw"
            + "PiNp");

        testPBES2Conformance(blowfishMocker, "7b939f8d61c7a403", -1, "f921f22f44e611de", data);
    }

    @Test
    public void testPBES2ConformanceTestAES128() throws Exception
    {
        byte[] data = Base64.decode(
            "MIIFHzBJBgkqhkiG9w0BBQ0wPDAbBgkqhkiG9w0BBQwwDgQImXjLGxzpmkwCAggA"
            + "MB0GCWCGSAFlAwQBAgQQf6Bn85WfZh3mHP1w0yubaQSCBNCS2Rvfoebwb7/sIbN4"
            + "uBS2v30Q/fOtUsqbo0JGjJEZ2Qe0kRDoXlDdf+tk2dQaI7tdjhxCcIeJOuiV0qJ6"
            + "g7y7tmcUhyFHJqm7L4ODpdH4jIGhKFod2szydrQ0lDQI7ZYjucRGd5zxb+wMphqV"
            + "dDcjEw1HPsVnG30WTjMSJ6lKs+PXLv+jHsf56p+QAxmiy+5eD7fF/C6vxCk0E0wf"
            + "63ZkSiprIYsmbM1kDBGnJwsH0fmTGi5XD3mXb6zpm3dc47VootC3jL6dG4259B/q"
            + "gqXZ810mBDPaupS7zc1D7MQvqHwSAhSDFylfVxpwbKx9Lke8e81AyfZsmuUe1QnM"
            + "fqqY0Ciu9LjxkE8CkitV9/2XwNkusyvQmTFALi735kO/l6bieShFl7m6DTK3R3E8"
            + "iSx+ZB4dO8tH+/9DbXI8un0DptXFKER2goXBDzv1MElmdBdAQ94j14yT5aTX0oTU"
            + "C7gaLgfzly70sEJAmrlbxFhgyrcdOZorFgga9qnri2w/0KoDjQGian+tH452pWZe"
            + "0XadQcrAaNviFJHw6/EmjDgHX970rWj2ZXrbWI4cm8scc3BcPaCwTDP8PZrxVhLG"
            + "0SJeAjEaME4n+Y7CT3cSdOJh6MiAzyqqTJxFMEovhDOTnyUKkJ56VONG12P9JLiT"
            + "eIpQlV1ywkH3hTXPMe4Dpbmvn6dhy5dRdOAtRFHOdno4Ri+0+Kne1VGhX1t3i8nP"
            + "HzRSLUg/tq3/hAwaMfHPtnE+3MXFteQgGd2eVzHiwMwcLYAoy9ewWW7+wvXjbDaS"
            + "gQ+Oz+gDCm/U1dOMRFp33lSN/eAKluVr9ZNewJ6r8WHg530CeSMG1rp4gq5LX2vk"
            + "SDQ0RPPAf++6G2tw79GWsCGNFIW/6xHIrBx37NT/zn/0P5/NMD4qZOrzRPdy2tBx"
            + "ggszbUK20sXAikibVJp4YWHAZMpdexKLWcktPE41FylFc/4XI7+ddRyvcW6re/FT"
            + "OCZFm0JZOmiopz9rOSGOgYklrUNWqAXWrkAiBo0mco6UdvBjGVFvk2Sw2b17S4Sr"
            + "9glXPFBNMHLy1daN5mvWq1uBob2us27MiSc4+4Biz3S9FMO02HhMKxC9cnFw5ZZc"
            + "aQHo1+USBQ1qJuvA0+od7JwCC5XEm+qunlnUDqDt8bC5WLXADDsVptV3IFIZ9ZB3"
            + "xM5y2K2TBpXzjR+fVg0M2qnbQYv6SVszuLlPArZL1mrmMUj1mmddPBJQk9Sl3lsx"
            + "ER2R5bDdLA9ct4mVLFjx2u7NNyYQqyvNOKGt1RD0PAlaP1nynVzuLEpnDAKrjCQI"
            + "IDivXTsjc3UyVx5C2sc+L7HuUibi8726sBYIrlG6eU0rf4wfgstGp9l0A4yBlbRB"
            + "cJRSBPdKN780IpEFx+SfzVeBvYUeVH0DFthaJfDOWAwD1XdQoxaPUhSel2iaaSCJ"
            + "WK0eaYETMc1z1P2Yzr2BCHTSxOrL7YJEMDYb1Ii6MvT/xaNSk/yuqhz+rS8zqH0E"
            + "qalCGoIi38YJy41a1Vau7MCjkxldexn62ZbI4J4dO0W2mX0Fe8aYxxGtL2EpCqnz"
            + "9c7BBsGqGC2agru0qTkvwCKtk8duMg5RWOXnW29tBYuAyT2bmwmco20XOdTgXIUJ"
            + "qsiX6xrG5S0T7vFExbxBIX2Jhg==");

        testPBES2Conformance(aesMocker, "7fa067f3959f661de61cfd70d32b9b69", 16, "9978cb1b1ce99a4c", data);
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

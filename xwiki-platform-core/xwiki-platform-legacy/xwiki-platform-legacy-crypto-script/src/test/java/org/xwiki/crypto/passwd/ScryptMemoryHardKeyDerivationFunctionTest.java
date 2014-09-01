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

import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.encoders.Base64;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.xwiki.crypto.passwd.internal.PBKDF2KeyDerivationFunction;
import org.xwiki.crypto.passwd.internal.ScryptMemoryHardKeyDerivationFunction;
import org.xwiki.crypto.internal.SerializationUtils;


/**
 * Tests Scrypt against test outputs given in reference document.
 *
 * @since 2.5M1
 * @version $Id$
 */
public class ScryptMemoryHardKeyDerivationFunctionTest extends ScryptMemoryHardKeyDerivationFunction
{
    // scrypt("", "", 16, 1, 1, 64)  see: http://www.tarsnap.com/scrypt/scrypt.pdf
    private final String outputSample1 = "77 d6 57 62 38 65 7b 20 3b 19 ca 42 c1 8a 04 97"
                                       + "f1 6b 48 44 e3 07 4a e8 df df fa 3f ed e2 14 42"
                                       + "fc d0 06 9d ed 09 48 f8 32 6a 75 3a 0f c8 1f 17"
                                       + "e8 d3 e0 fb 2e 0d 36 28 cf 35 e2 0c 38 d1 89 06";

    // scrypt("password", "NaCl", 1024, 8, 16, 64)  see: http://www.tarsnap.com/scrypt/scrypt.pdf
    private final String outputSample2 = "fd ba be 1c 9d 34 72 00 78 56 e7 19 0d 01 e9 fe"
                                       + "7c 6a d7 cb c8 23 78 30 e7 73 76 63 4b 37 31 62"
                                       + "2e af 30 d9 2e 22 a3 88 6f f1 09 27 9d 98 30 da"
                                       + "c7 27 af b9 4a 83 ee 6d 83 60 cb df a2 cc 06 40";

    // scrypt("pleaseletmein", "SodiumChloride", 16384, 8, 1, 64)  see: http://www.tarsnap.com/scrypt/scrypt.pdf
    private final String outputSample3 = "70 23 bd cb 3a fd 73 48 46 1c 06 cd 81 fd 38 eb"
                                       + "fd a8 fb ba 90 4f 8e 3e a9 b5 43 f6 54 5d a1 f2"
                                       + "d5 43 29 55 61 3f 0f cf 62 d4 97 05 24 2a 9a f9"
                                       + "e6 1e 85 dc 0d 65 1e 40 df cf 01 7b 45 57 58 87";

    private final String scryptPBKDF2InputSaltBase64 = "JPB6lOJEJw51fxSOFTt2hzCDGqwzZ503T1HPt9laaas=";

    private final String scryptPBKDF2InputPassword = "password";

    private final int scryptPBKDF2DerivedKeyLength = 2048;

    /** Storing a sha256 hash of the output since the actual output is huge (2048 bytes) */
    private final String scryptPBKDF2OutputSha256Base64 = "jKvc+oZ+IsBUVuhQ4r4RHJNG4l2/jWCUiNpovm2IO8g=";

    /** In hex format to illistrate which part is taken by Integerify. */
    private final String integerifyAndModInputX =
        "29035cc0751930ac18833d1adbb8c7fd0ed2988d9e8979e703df9081206948c91d3a0603c911cd289dd76c9699f50f002146d506ecc6"
      + "a48e797b31b772879a93096dcd7473a2b7569db2729dcfbb3bf85e1ba0379be5d8add5731cf5786aa0d04eb860f8236b01d5ab8f09e2"
      + "c551f6cf27d48c0d1388f2dd9548b95890c318019c8ff1c69b9d89b4bdd4cc9a797201a05c614260f9a13f9a030527b58060acecd5cf"
      + "dac7647e139892e7f901d976cf51e52c35ee7621b386af0627182be9e800375baf9b401df3468d6316374453179bb71b118cbbb79ca2"
      + "191af11e1587de5833814e7036866a2b5b1129725b9f3214afecaa95a0cf61fbd64b878caa64583ee3510717572fa6d1630ceed6b480"
      + "01a604fb43f458df640c4ecf78fea40b799e2c4f50804af4c041e0341cdb90fef5cd486fb07edf3a76fd2d3711c311f4b950d0d13215"
      + "73438d84fdf71c743e2660f8286712388cc24aaa370ec34634c33c59801978bce75e61f582653d60a1754b9148f41a841b58d8309290"
      + "2dfe4c01587a7e08515c15f5196b54c6f3c8585afb47f8acbd41a9b3fb17afc7890609ab46952010c1454b14484135eb6277ba1b240e"
      + "3e064d3c385f3ff4ad0e54674d29d2ded854353eccdf055584cbf3016ea18665521ceb259edb8a1734c393ebfcdf65ff08c5496cb0b5"
      + "c285d21cca0dcdb3ece3d89919ba92ed6cdbe6a3e09a4c84f5e467fed96445f37af656f2f34cb86916d20983c01d97a6a56033742327"
      + "37f0d1ef5e7202c490d1af2ffac470d4fc4fb963bb387c8670df2010aec223420accada12e7cddc3261edf4377f2d600ec992b2e3da0"
      + "50c18c72f7c55da70025cabf2afd8d294f7279e5664a242bb25b22493265f6778c85aa730e1baaaec45e89f4675c6ac5548b90677a15"
      + "42271a7f4599b4e944e5b5e25035a450d57c86ad059c5ccf2688b7790d92486e450ee504ed0a2e0c8be9b04c6a18400b5859e2eab592"
      + "e5c53e41ebf1c4ef2abb75531d36f60ef816e56f0f5aafe9d1efd829161d4e6f09f9c62bbcd418f5e9a762e22acc12f9d96ec5bf71be"
      + "2977676a99a38997d2482d0e70eceb2d535447e13d559d3ce5a0342be5af2e6a2b3e26e831425414ee8860984059659281923fc6f4a3"
      + "a03c426f6b1aafc95289ac58d12c92e3ddd7c4a2de3ba7d81d2804bab0fa2f0c41c93e1723b7e32ab794e4052f748c487fad09cde630"
      + "991ce93960877872efbaf1219cbc52db499c61a042afe88f7fa3a9b47fe9859b9d995677bf5701ad29050ca026153ee1034670148783"
      + "f17f06069b776963c0c65241ba7b49b322cbf2455baea29c9a1b5cffc11e79b41e7edad139f6567b971f"
      + /* The part which integerify takes --> */ "bfcfe14e" /* <-- */ + "2f4cedba4e743e4c718cf22d32856392f77f6e91be3"
      + "d9cffc82055fb4f6e78c1c989d4376d09af0b40a518e3bac775a1976748d9492db3b3fc5ad27c";

    private final int integerifyAndModInputN = 131072;

    private final int integerifyAndModOutput = 118719;

    private final int integerifyAndModInteger = 1323421631;

    private final String integerifyAndMod2InputXBase64 =
        "h7a9MqzAEB0V1FooWsQ0zXmxAB+Hh3NIABGVFWiOyxHLWc2pJM40RX7saBru0L8wc3CsU41U+T9tAuz0Rof3eAIhB19iEIC/Ton1ddxFl18l"
      + "4I7v1ap/dWrY9ACk+OCkhb76XTMnKgVf81pl+5wPufO8aWSmYgQn5/TvLsy3m5Z9RJJHPcqLlxK5UhUapm0/+CzGs7ptxMN8Y9aVVa8yF5vG"
      + "dIHgQGK3ps22xrXxmyHEqlqqc3neoM483rFtMNNWC8+S2qCqcZDyl4OItuIhvwhTbKMATMmtSxinxrD6G9m6lDSp+1yhjXuRoAvkTi47Grxj"
      + "GVjWwRJ1/MDYd85qAIOIdKbYMaeq8/P9/TXWeh/ThXgw5IlkjHqb5iB+rYyUd1mm2GIts8bvEHvCYtXDTe17Gygv9Y+4WpXrz3cHb/dApcIu"
      + "u2sM9daEAh+M4S1wpCm4MUXF1BPgYumpyPhU4Lrg7Acv00Owxk/bIM4ZgGcx3zT7OCdBSdfjpXyHFxEcMqrCSbjp22Nwkz9LPo8INdy50Lju"
      + "CEBVtB24xgfWK4PjQTL8A4ADoV3EcwScbFPjXFNXoEw+tOwTT3LGBVtj+C+FoZQ9fJpWyw5Mjsx3FPUI5oTor+Fb7YhdC/biz7cD64YETG+f"
      + "lWTrMmXQR9IX7jhFCJ8CmaiHABIRr/qoliEXXo/0adycQOFF7E3OLz+7LvV96Z5Cu5qjbc4Nxg0K/WY/3qQbJ6/T5pm6HIqcNpiwSLfa129a"
      + "Qxhyq6fEtwFygSLFnTqfxoNsIJ2SsPzX5xf9A1qvLgSrBw55p6PkBojHMudJYJdfD9ukL8/FOXrSuBwlkanPg4BzSOCZzqufOVBbyMcGErPu"
      + "xfVV3l5U5oSrkZgJy+5OM0t5cz/ibq/j9KwOQg28+d9LJx7zH7RLivTfOgUJQedy48jRga2OK/k+kIjqpRFP0yGz32OFYze7j2WglRl4cfb2"
      + "9n8xjJpMMcGh+ZT4U5bPMsQOo+1xasYbkcAEwCu0oJM5jwtt5vlCrnr/DS9KH57OQmKsd6EL7+WD3YyOO+eP2rIsWnF09m/lHEW9sU+LmAB9"
      + "vUxrpS/7orzQNitQdNDjCKS9Ra8OGl24rI7NwHKIV/r3qwV1B4kdR/BdV+PT5QjMPdb21lOHPR0MtZghA6p40lPrubLjEQ6/hltMM5ksjx4y"
      + "8AwMjrLUiX/aUM0Yw8230Bou21UqSiI5RHqdrFwXK61VxVxBZr50VaJ/p322TCaGim4/6xU/2k4WZgh4TmVNrCbX/TwcRrbFOh2l7KAEFpzm"
      + "JVlxDWfV2ZJg/psJucHmvVIXXSfJx+l+R8ttvCT6j5etNCuJfd3Frsq/aLNiTzJy+nllog==";

    private final int integerifyAndMod2InputN = 131072;

    private final int integerifyAndMod2Output = 112198;

    private final int integerifyAndMod2Integer = 986035782;

    private final String blockMixInputBase64 =
        "IN9q5eH74QOe1HOcDw54bP9DU0IGNBi0a41XK0CjexiotOHHmHRexOzt2sPq/TZ6hE4OLwjAj0r32HwR73x5PC/t7/wW68n3dXdElI+zWOow"
      + "GwE9bhn67/rBJ8tbxn8jDn06cYiVe8uFaVib/z6fL7CuMqsAmVPTgd992GPHFQOG7gQ8qY6YmYrIdKHg13+o+QoohEf/9U2PdwnUcCcchb5n"
      + "eLtqzjzQ1JNRod4rqaDwR/08Sa4LIQuZyNu+H+91zmYbEjonnGupB4c98T928z1qbrDinKOjjFP1kUsjU6/+1mi5n6a0Bj8Oqhkaz9lXc/OH"
      + "6ExirEuVXYKSTco8i7Bi2vIU7oQ4extOXW0rHnoAk9wiqkxKMwH9sHV5ntf5KThGrQRnNsTwXoJ9PpzNR5PGNiRUqI/x3Nlg7vWhFh9NHEfP"
      + "Y8e8dysiW30DW1fnp+uOfHYcNbmubdaDrBDWt6ZtiPW7qOrGVZ5VBj7n74IMJ9Be4cmGNsUvJrN6TBRiwmOuF36kaPYvgAJTUtDlB84vumv3"
      + "0E1Hqq83aYNxFIS0OSEseU8XysjXJBFSi50kQJ4KfS3mQq59nhJeEXqxKSqvP+klBcjN8LggMbgMUdkDgEcMPzH1+yb7W17aoSRo2KIzx07t"
      + "OYD+s62dgghTcEB4/MAy+8m00mftZHxjLIZOktZHRKw6mrHvC1JzrGQVU0/uwpeDDC11zw/00jNy3fq93xzjLKfZqat3xbf6LlJeSeoCJDvW"
      + "reGJFjPRt08PHYc7MM67OuIoMNNATJz6B7lIjpgxm5SNRImACTcjdbnK2U5K26KfliLJrCG0Td/gM4RlVVFEWa43h6+SCjglw4obgSUeA487"
      + "DmEfDpqWdzWewr8UWj5NvdN6JQFTHjf+Nu3cN/q2BBUKCAW7lMP55nmrLW1/d39kkbmICVnuuYeVu+qdDOt+dfOYAmtvAYwurkLdAPfC7MJH"
      + "grxewmW+EEsLSu6rV1pkidicEAbbg6R6SZAZRvtQusrldrN7q/0ggFMt51Btret9zNMGg+MfzzCz7WbxXFb0HLh9xE2Gj/y4uWA82wTgctuW"
      + "/VH3XhRJkIcDQJtcnnnmHHToc96ocdkTQz6Gbybi//wPLh/iK+XbTiMKPdV+QJLb8f4NKtFWxO3aio0xvG63KSoV4y1YF8+nxJDYrQvyFYtm"
      + "rgzNmniosk7uoeYglawbFc5khyIupUM22wP/sZdbwmaVCR8etK3DWNjBQ7rsKfTEdr+RSjbZNCkFhI0eNPEfjpg4RH/PK9vwoJXgPTd75dst"
      + "0CQjUiccPctuVAKlVxNZBM12tGKm3VhVu0gg5e9JosB0HJDaTo8MUhWzzslGKL4l5zFMnw==";

    private final String blockMixOutputSha256Base64 = "U+mxxcLaXFKDHkIC0/K0gM12R3aO8Pl+O8Gp/ueNHp0=";

    /** Salsa20/8 Test */
    private final String salsa8InputBase64 = 
        "gf/rbT15g4cw/qsdiAMZQ/kkckhGcuK6VfDVMm0v4InAcYm9IFnGaw8aFkC8BB6HFlEWovHQzZsjaYXOaBrqOA==";

    private final String salsa8OutputBase64 =
        "dQmvxSiYVIIqC9RIbC0SAE2B6M9+VxMGLbfos6tY6hbFeBvAptG+FHbLETigJ1dV/xFDQbmtfLPo+PV7VNFdIw==";

    private final String serializedScryptFunctionBase64 =
        "rO0ABXNyAEZvcmcueHdpa2kuY3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5TY3J5cHRNZW1vcnlIYXJkS2V5RGVyaXZhdGlvbkZ1bmN0aW9uAAAA"
      + "AAAAAAECAAVJAAlibG9ja1NpemVJABBkZXJpdmVkS2V5TGVuZ3RoSQANbWVtb3J5RXhwZW5zZUkAEHByb2Nlc3NvckV4cGVuc2VbAARzYWx0"
      + "dAACW0J4cgBIb3JnLnh3aWtpLmNyeXB0by5wYXNzd2QuaW50ZXJuYWwuQWJzdHJhY3RNZW1vcnlIYXJkS2V5RGVyaXZhdGlvbkZ1bmN0aW9u"
      + "AAAAAAAAAAECAAB4cAAAAAgAAAAUAAACAAAAAAV1cgACW0Ks8xf4BghU4AIAAHhwAAAAEGmYEV2eu4+85nQKHIr1z2o=";

    private final String serializedScryptFunctionHashOfPassword = "e903CTczVQaDifW7R1F5EQuuKu8=";

    @Test
    public void scryptConformanceTest1() throws Exception
    {
        this.init(new byte[0], 16, 1, 1, 64);
        byte[] out = this.deriveKey(new byte[0]);
        Assert.assertTrue(Arrays.equals(Hex.decode(this.outputSample1.getBytes("US-ASCII")),  out));
    }

    @Test
    public void scryptConformanceTest2() throws Exception
    {
        this.init(new byte[] {'N', 'a', 'C', 'l'}, 1024, 8, 16, 64);
        byte[] out = this.deriveKey(new byte[] {'p', 'a', 's', 's', 'w', 'o', 'r', 'd'});
        Assert.assertTrue(Arrays.equals(Hex.decode(this.outputSample2.getBytes("US-ASCII")),  out));
    }

    @Test
    public void scryptConformanceTest3() throws Exception
    {
        this.init(new byte[] {'S', 'o', 'd', 'i', 'u', 'm', 'C', 'h', 'l', 'o', 'r', 'i', 'd', 'e'},
                    16384, 8, 1, 64);
        byte[] out = this.deriveKey(new byte[] {'p', 'l', 'e', 'a', 's', 'e', 'l', 'e', 't', 'm', 'e', 'i', 'n'});
        Assert.assertTrue(Arrays.equals(Hex.decode(this.outputSample3.getBytes("US-ASCII")),  out));
    }

    @Test
    public void scryptPBKDF2Test() throws Exception
    {
        PBKDF2KeyDerivationFunction sha256Pbkdf2 = new PBKDF2KeyDerivationFunction(new SHA256Digest());

        byte[] out = sha256Pbkdf2.generateDerivedKey(scryptPBKDF2InputPassword.getBytes("US-ASCII"),
                                                     Base64.decode(scryptPBKDF2InputSaltBase64.getBytes("US-ASCII")),
                                                     1,
                                                     scryptPBKDF2DerivedKeyLength);
        Digest d = new SHA256Digest();
        d.update(out, 0, out.length);
        byte[] hash = new byte[32];
        d.doFinal(hash, 0);
        String outStr = new String(Base64.encode(hash), "US-ASCII");
        Assert.assertTrue("Mismatch:\nExpecting: " + scryptPBKDF2OutputSha256Base64 + "\n      Got: " + outStr,
                          scryptPBKDF2OutputSha256Base64.equals(outStr));
    }

    @Test
    public void scryptIntegerifyAndModTest() throws Exception
    {
        int out = this.integerifyAndMod(Hex.decode(integerifyAndModInputX.getBytes("US-ASCII")),
                                        integerifyAndModInputN);

        Assert.assertTrue("Mismatch:\nExpecting: " + integerifyAndModOutput + "\n      Got: " + out,
                          (integerifyAndModOutput == out));
    }

    @Test
    public void scryptIntegerifyAndModTest2() throws Exception
    {
        int out = this.integerifyAndMod(Base64.decode(integerifyAndMod2InputXBase64.getBytes("US-ASCII")),
                                        integerifyAndMod2InputN);

        Assert.assertTrue("Mismatch:\nExpecting: " + integerifyAndMod2Output + "\n      Got: " + out,
                          (integerifyAndMod2Output == out));
    }

    @Test
    public void scryptUnsignedModTest2() throws Exception
    {
        long[] longs = {
            101L, // 101
            1010L, // 1010
            4099L, // 4099
            0x80000000000007D0L, // 9223372036854777808 (java considers this a negative number)
            555932188L, // collected from crypto_scrypt
        };
        int[] modulises = {
            4,
            8,
            16,
            32,
            131072
        };
        int[] results = {
            1,  // 100 % 5
            2,  // 1010 % 8
            3,  // 4099 % 16
            16, // 9223372036854777808 % 32
            55836 // 555932188 % 131072
        };
        for (int i = 0; i < longs.length; i++) {
            int out = this.unsignedMod(longs[i], modulises[i]);
            Assert.assertTrue("\nExpecting: " + results[i] + "\n      Got: " + out, (results[i] == out));
        }
    }

    @Test
    public void integerifyTest1() throws Exception
    {
        long out = this.integerify(Hex.decode(this.integerifyAndModInputX.getBytes("US-ASCII")));
        Assert.assertEquals(Long.toHexString(integerifyAndModInteger), Long.toHexString(out));
    }

    @Test
    public void integerifyTest2() throws Exception
    {
        long out = this.integerify(Base64.decode(this.integerifyAndMod2InputXBase64.getBytes("US-ASCII")));
        Assert.assertEquals(Long.toHexString(integerifyAndMod2Integer), Long.toHexString(out));
    }

    @Test
    public void blockMixTest() throws Exception
    {
        byte[] out = Base64.decode(this.blockMixInputBase64.getBytes("US-ASCII"));
        // Blockmix requires that memory be allocated.
        this.init(new byte[0], 2, 8, 1, 1);
        this.allocateMemory(true);

        this.blockMix(out);

        Digest d = new SHA256Digest();
        d.update(out, 0, out.length);
        byte[] hash = new byte[32];
        d.doFinal(hash, 0);
        String outStr = new String(Base64.encode(hash), "US-ASCII");

        Assert.assertEquals(this.blockMixOutputSha256Base64, outStr);
    }

    @Test
    public void salsa8Test() throws Exception
    {
        byte[] out = Base64.decode(this.salsa8InputBase64.getBytes("US-ASCII"));
        this.scryptSalsa8(out);
        String outStr = new String(Base64.encode(out), "US-ASCII");
        Assert.assertEquals(this.salsa8OutputBase64, outStr);
    }

    /** Prove that the function will continue to produce the same hash for a given password after serialization. */
    @Test
    public void serializationTest() throws Exception
    {
        final byte[] password = "password".getBytes();
        final MemoryHardKeyDerivationFunction originalFunction = new ScryptMemoryHardKeyDerivationFunction();

        originalFunction.init(512, 200, 20);
        byte[] serial = originalFunction.serialize();
        byte[] originalHash = originalFunction.deriveKey(password);

        // Prove that the function doesn't return the same output _every_ time
        final MemoryHardKeyDerivationFunction differentFunction = new ScryptMemoryHardKeyDerivationFunction();
        differentFunction.init(512, 200, 20);
        byte[] differentHash = differentFunction.deriveKey(password);
        Assert.assertFalse(Arrays.equals(originalHash, differentHash));

        final KeyDerivationFunction serialFunction = (KeyDerivationFunction) SerializationUtils.deserialize(serial);
        byte[] serialHash = serialFunction.deriveKey(password);
        Assert.assertTrue(Arrays.equals(originalHash, serialHash));
    }

    @Test
    public void deserializationTest() throws Exception
    {
        final KeyDerivationFunction serialFunction = (KeyDerivationFunction)
            SerializationUtils.deserialize(Base64.decode(this.serializedScryptFunctionBase64.getBytes("US-ASCII")));

        byte[] serialHash = serialFunction.deriveKey("password".getBytes());
        Assert.assertEquals(serializedScryptFunctionHashOfPassword, new String(Base64.encode(serialHash)));
    }
}

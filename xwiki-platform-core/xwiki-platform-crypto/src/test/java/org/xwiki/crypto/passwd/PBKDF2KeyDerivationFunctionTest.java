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
import org.bouncycastle.crypto.digests.SHA256Digest;

import org.xwiki.crypto.passwd.internal.PBKDF2KeyDerivationFunction;
import org.xwiki.crypto.internal.SerializationUtils;


/**
 * Tests PBKDF2KeyDerivationFunctionTest to ensure conformance with PKCS#5v2 standard for PBKDF2.
 *
 * @since 2.5M1
 * @version $Id$
 */
public class PBKDF2KeyDerivationFunctionTest
{
    private final byte[] salt = { 0x12, 0x34, 0x56, 0x78, 0x78, 0x56, 0x34, 0x12 };

    private final PBKDF2KeyDerivationFunction function = new PBKDF2KeyDerivationFunction();

    private final String serializedPBKDF2FunctionBase64 =
        "rO0ABXNyADxvcmcueHdpa2kuY3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5QQktERjJLZXlEZXJpdmF0aW9uRnVuY3Rpb24AAAAAAAAAAQIABEkA"
      + "EGRlcml2ZWRLZXlMZW5ndGhJAA5pdGVyYXRpb25Db3VudEwAD2RpZ2VzdENsYXNzTmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO1sABHNhbHR0"
      + "AAJbQnhyAD5vcmcueHdpa2kuY3J5cHRvLnBhc3N3ZC5pbnRlcm5hbC5BYnN0cmFjdEtleURlcml2YXRpb25GdW5jdGlvbgAAAAAAAAABAgAA"
      + "eHAAAAAUAAB1MHQALG9yZy5ib3VuY3ljYXN0bGUuY3J5cHRvLmRpZ2VzdHMuU0hBMjU2RGlnZXN0dXIAAltCrPMX+AYIVOACAAB4cAAAABB6"
      + "mp2kO8CyDjeRDlqmABGt";

    private final String serializedPBKDF2FunctionHashOfPassword = "jYk/zFiAoRAGv4YaijLyn7gJf1I=";

    /** from: http://www.ietf.org/rfc/rfc3211.txt */
    @Test
    public void pbkdf2ConformanceTest1() throws Exception
    {
        String password = "password";

        byte[] out = this.function.generateDerivedKey(password.getBytes("US-ASCII"), this.salt, 5, 8);
        String outStr = new String(Hex.encode(out), "US-ASCII");
        String expectOut = "d1daa78615f287e6";
        Assert.assertTrue("\nExpected: " + expectOut + "\n     Got: " + outStr,
                          expectOut.equals(outStr));
    }

    /** from: http://www.ietf.org/rfc/rfc3211.txt */
    @Test
    public void pbkdf2ConformanceTest2() throws Exception
    {
        String password = "All n-entities must communicate with other n-entities via n-1 entiteeheehees";
        byte[] out = this.function.generateDerivedKey(password.getBytes("US-ASCII"), this.salt, 500, 16);
        String outStr = new String(Hex.encode(out), "US-ASCII");
        String expectOut = "6a8970bf68c92caea84a8df285108586";
        Assert.assertTrue("\nExpected: " + expectOut + "\n     Got: " + outStr,
                          expectOut.equals(outStr));
    }

    @Test
    public void initializationProcessorTimeGuessingTest()
    {
        int targetTimeToSpend = 1000;
        this.function.init(targetTimeToSpend, 20);
        long time = System.currentTimeMillis();
        this.function.deriveKey("password".getBytes());
        int timeSpent = (int) (System.currentTimeMillis() - time);
        Assert.assertTrue("The actual time spent running the function was over 100% off "
                          + "from the specified target time.",
                          Math.abs(timeSpent - targetTimeToSpend) < targetTimeToSpend);
    }

    /** Prove that the function will continue to produce the same hash for a given password after serialization. */
    @Test
    public void serializationTest() throws Exception
    {
        final byte[] password = "password".getBytes();

        final KeyDerivationFunction sha256Function = new PBKDF2KeyDerivationFunction(new SHA256Digest());
        sha256Function.init(500, 20);
        byte[] originalHash = sha256Function.deriveKey(password);
        byte[] serial = sha256Function.serialize();

        // Prove that the function doesn't return the same output _every_ time
        sha256Function.init(500, 20);
        byte[] differentHash = sha256Function.deriveKey(password);
        Assert.assertFalse(Arrays.equals(originalHash, differentHash));

        final KeyDerivationFunction serialFunction = (KeyDerivationFunction) SerializationUtils.deserialize(serial);
        byte[] serialHash = serialFunction.deriveKey(password);
        Assert.assertTrue(Arrays.equals(originalHash, serialHash));
    }

    @Test
    public void deserializationTest() throws Exception
    {
        final KeyDerivationFunction serialFunction = (KeyDerivationFunction)
            SerializationUtils.deserialize(Base64.decode(this.serializedPBKDF2FunctionBase64.getBytes("US-ASCII")));

        byte[] serialHash = serialFunction.deriveKey("password".getBytes("US-ASCII"));
        Assert.assertEquals(serializedPBKDF2FunctionHashOfPassword, new String(Base64.encode(serialHash)));
    }
}

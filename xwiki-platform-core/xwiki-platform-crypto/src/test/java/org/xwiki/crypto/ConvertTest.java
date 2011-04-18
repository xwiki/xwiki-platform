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
package org.xwiki.crypto;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.crypto.internal.Convert;


/**
 * Tests the {@link Convert} class.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class ConvertTest
{
    /** Text containing unicode characters. */
    private static final String NONLATIN = "Kryptographie (von griechisch: \u03ba\u03c1\u03c5\u03c0\u03c4\u03cc\u03c2,"
                                         + " \u201everborgen\u201c und \u03b3\u03c1\u03ac\u03c6\u03b5\u03b9\u03bd,"
                                         + " \u201eschreiben\u201c) ist die Wissenschaft der Verschl\u00fcsselung von"
                                         + " Informationen.";

    /** Random generator. */
    private Random rnd = new Random(System.currentTimeMillis());

    @Test
    public void testBase64EncodeDecodeBytes()
    {
        byte[] data = randomData();
        byte[] encoded = Convert.toBase64(data);
        byte[] decoded = Convert.fromBase64(encoded);
        Assert.assertArrayEquals(data, decoded);
    }

    @Test
    public void testBase64EncodeDecodeString()
    {
        byte[] data = randomData();
        String encoded = Convert.toBase64String(data);
        byte[] decoded = Convert.fromBase64String(encoded);
        Assert.assertArrayEquals(data, decoded);
    }

    @Test
    public void testBase64DecodeEncode()
    {
        String base64 = "CjFATbhb9ofBA+dj/nvZlw8wCo3WqrMjZZ/IzZ4KmISco9SSzUA=";
        byte[] decoded = Convert.fromBase64String(base64);
        String encoded = Convert.toBase64String(decoded);
        Assert.assertEquals(base64, encoded);
    }

    @Test
    public void testBase64EncodeNull()
    {
        String encoded = Convert.toChunkedBase64String(null);
        Assert.assertEquals(Convert.getNewline(), encoded);
    }

    @Test
    public void testBase64DecodeNull()
    {
        byte[] decoded = Convert.fromBase64(null);
        Assert.assertArrayEquals(new byte[0], decoded);
    }

    @Test
    public void testBase64EncodeDecodeEmpty()
    {
        String encoded = Convert.toBase64String(new byte[0]);
        Assert.assertEquals("", encoded);
        byte[] decoded = Convert.fromBase64String(encoded);
        Assert.assertArrayEquals(new byte[0], decoded);
    }

    @Test
    public void testToBytesNull()
    {
        byte[] bytes = Convert.stringToBytes(null);
        Assert.assertArrayEquals(new byte[0], bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToBytesMarkerNull()
    {
        Convert.stringToBytes(null, null, null);
    }

    @Test
    public void testNonLatinConvert()
    {
        byte[] data = Convert.stringToBytes(NONLATIN);
        String recovered = Convert.bytesToString(data);
        Assert.assertEquals(NONLATIN, recovered);
    }

    /**
     * @return an array with random data
     */
    private byte[] randomData()
    {
        byte[] data = new byte[112];
        rnd.nextBytes(data);
        return data;
    }
}


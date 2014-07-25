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
package org.xwiki.crypto.params.cipher.symmetric;

import java.security.SecureRandom;

import org.xwiki.stability.Unstable;

/**
 * Symmetric cipher parameters for cipher requiring a key and an initialization vector.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class KeyWithIVParameters implements SymmetricCipherParameters
{
    /** A encryption initialization vector. */
    private final KeyParameter keyParam;
    private final byte[] iv;

    /**
     * Initialize parameters with a random initialization vector.
     * @param key the key.
     * @param ivSize the size of the initialization vector.
     */
    public KeyWithIVParameters(byte[] key, int ivSize)
    {
        this(new KeyParameter(key), ivSize);
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param iv the initialization vector.
     */
    public KeyWithIVParameters(byte[] key, byte[] iv)
    {
        this(new KeyParameter(key), iv);
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param ivSize the size of the initialization vector to randomize.
     * @param random the random source.
     */
    public KeyWithIVParameters(byte[] key, int ivSize, SecureRandom random)
    {
        this(new KeyParameter(key), ivSize, random);
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param ivSize the size of the initialization vector to randomize using a new default SecureRandom.
     */
    public KeyWithIVParameters(KeyParameter key, int ivSize)
    {
        this(key, ivSize, new SecureRandom());
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param ivSize the size of the initialization vector to randomize.
     * @param random the random source.
     */
    public KeyWithIVParameters(KeyParameter key, int ivSize, SecureRandom random)
    {
        this.keyParam = key;
        this.iv = new byte[ivSize];
        random.nextBytes(this.iv);
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param iv the initialization vector.
     */
    public KeyWithIVParameters(KeyParameter key, byte[] iv)
    {
        this.keyParam = key;
        this.iv = iv;
    }

    /**
     * @return the initialization vector.
     */
    public KeyParameter getKeyParameter()
    {
        return keyParam;
    }

    /**
     * @return the key.
     */
    public byte[] getKey()
    {
        return keyParam.getKey();
    }

    /**
     * @return the initialization vector.
     */
    public byte[] getIV()
    {
        return iv;
    }
}

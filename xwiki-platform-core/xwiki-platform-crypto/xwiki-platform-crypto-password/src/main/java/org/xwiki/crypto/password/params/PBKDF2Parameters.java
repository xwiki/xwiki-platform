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
package org.xwiki.crypto.password.params;

import java.security.SecureRandom;

import org.xwiki.stability.Unstable;

/**
 * Key derivation function parameters for PBKDF2 functions that use an iteration count and a salt.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class PBKDF2Parameters extends KeyDerivationFunctionParameters
{
    private static final int SALT_DEFAULT_SIZE = 16;
    private static final int DEFAULT_MIN_ITER = 1000;
    private static final int DEFAULT_ITER_RANGE = 2000;

    private static final SecureRandom PRND = new SecureRandom();

    private final byte[] salt;
    private final int iterationCount;
    private final String prf;

    /**
     * Initialise parameters with default values.
     *
     * Salt and iteration count are randomized.
     */
    public PBKDF2Parameters() {
        this(-1);
    }

    /**
     * Initialise parameters with default or random values and the given pseudo random function.
     *
     * @param prf a pseudo random function hint.
     */
    public PBKDF2Parameters(String prf) {
        this(-1, getRandomIterationCount(), getRandomSalt(), prf);
    }

    /**
     * Initialise parameters with a key length and default randomized values.
     *
     * Salt and iteration count are randomized.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     *
     */
    public PBKDF2Parameters(int keySize) {
        this(keySize, getRandomIterationCount(), getRandomSalt(), null);
    }

    /**
     * Initialise parameters with a key length, a pseudo random function and default randomized values.
     *
     * Salt and iteration count are randomized.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param prf a pseudo random function hint.
     */
    public PBKDF2Parameters(int keySize, String prf) {
        this(keySize, getRandomIterationCount(), getRandomSalt(), null);
    }

    /**
     * Initialise parameters with a key length, fixed iteration count and a randomized salt.
     *
     * Salt of {@value #SALT_DEFAULT_SIZE} bytes is randomized.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param iterationCount the number of iterations the "mixing" function is to be applied for.
     */
    public PBKDF2Parameters(int keySize, int iterationCount) {
        this(keySize, iterationCount, getRandomSalt(), null);
    }

    /**
     * Initialise parameters with a key length, fixed iteration count, a pseudo random function and a randomized salt.
     *
     * Salt of {@value #SALT_DEFAULT_SIZE} bytes is randomized.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param iterationCount the number of iterations the "mixing" function is to be applied for.
     * @param prf a pseudo random function hint.
     */
    public PBKDF2Parameters(int keySize, int iterationCount, String prf) {
        this(keySize, iterationCount, getRandomSalt(), prf);
    }

    /**
     * Initialise parameters with a key length, a salt and a randomized iteration count.
     *
     * Iteration count is randomized
     * (between {@value #DEFAULT_MIN_ITER} and ({@value #DEFAULT_MIN_ITER} + {@value #DEFAULT_ITER_RANGE})).
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param salt the salt to be mixed with the password.
     */
    public PBKDF2Parameters(int keySize, byte[] salt) {
        this(keySize, getRandomIterationCount(), salt, null);
    }

    /**
     * Initialise parameters with a key length, a salt, a pseudo random function and a randomized iteration count.
     *
     * Iteration count is randomized
     * (between {@value #DEFAULT_MIN_ITER} and ({@value #DEFAULT_MIN_ITER} + {@value #DEFAULT_ITER_RANGE})).
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param salt the salt to be mixed with the password.
     * @param prf a pseudo random function hint.
     */
    public PBKDF2Parameters(int keySize, byte[] salt, String prf) {
        this(keySize, getRandomIterationCount(), salt, prf);
    }

    /**
     * Initialise parameters with a key length, an iteration count and a salt.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param iterationCount the number of iterations the "mixing" function is to be applied for.
     * @param salt the salt to be mixed with the password.
     */
    public PBKDF2Parameters(int keySize, int iterationCount, byte[] salt) {
        this(keySize, iterationCount, salt, null);
    }

    /**
     * Initialise parameters with a key length, an iteration count and a salt.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param iterationCount the number of iterations the "mixing" function is to be applied for.
     * @param salt the salt to be mixed with the password.
     * @param prf the pseudo random function hint to be used to retrieve the pseudo random function.
     */
    public PBKDF2Parameters(int keySize, int iterationCount, byte[] salt, String prf) {
        super(keySize);
        this.salt = salt;
        this.iterationCount = iterationCount;
        this.prf = prf;
    }

    private static int getRandomIterationCount()
    {
        return PRND.nextInt(DEFAULT_ITER_RANGE) + DEFAULT_MIN_ITER;
    }

    private static byte[] getRandomSalt()
    {
        byte[] salt = new byte[SALT_DEFAULT_SIZE];
        PRND.nextBytes(salt);
        return salt;
    }

    @Override
    public String getAlgorithmName()
    {
        return "PKCS5S2";
    }

    /**
     * @return the number of iterations the "mixing" function is to be applied for.
     */
    public int getIterationCount()
    {
        return iterationCount;
    }

    /**
     * @return the salt to be mixed with the password.
     */
    public byte[] getSalt()
    {
        return salt;
    }

    /**
     * @return the pseudo random function hint, or null for default.
     */
    public String getPseudoRandomFuntionHint()
    {
        return prf;
    }
}

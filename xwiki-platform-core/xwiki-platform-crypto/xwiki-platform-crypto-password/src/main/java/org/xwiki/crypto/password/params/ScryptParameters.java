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

/**
 * Key derivation function parameters for Scrypt functions.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class ScryptParameters extends KeyDerivationFunctionParameters
{
    private static final int SALT_DEFAULT_SIZE = 16;
    private static final int COST_DEFAULT = 1024;
    private static final int PARALLELIZATION_DEFAULT = 1;
    private static final int BLOCK_DEFAULT_SIZE = 8;

    private static final SecureRandom PRND = new SecureRandom();

    private final byte[] salt;
    private final int costParameter;
    private final int blockSize;
    private final int parallelizationParameter;

    /**
     * Initialize all parameters to default values, using a random salt, a cost parameter of {@value #COST_DEFAULT},
     * a parallelization parameter of {@value #PARALLELIZATION_DEFAULT}
     * and a block size of {@value #BLOCK_DEFAULT_SIZE}.
     */
    public ScryptParameters() {
        this(-1, COST_DEFAULT, PARALLELIZATION_DEFAULT, BLOCK_DEFAULT_SIZE, getRandomSalt());
    }

    /**
     * Initialize key size and use a random salt, a cost parameter of {@value #COST_DEFAULT},
     * a parallelization parameter of {@value #PARALLELIZATION_DEFAULT}
     * and a block size of {@value #BLOCK_DEFAULT_SIZE}.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     */
    public ScryptParameters(int keySize) {
        this(keySize, COST_DEFAULT, PARALLELIZATION_DEFAULT, BLOCK_DEFAULT_SIZE, getRandomSalt());
    }

    /**
     * Initialize parameters to custom values, a cost parameter of {@value #COST_DEFAULT}, a parallelization parameter
     * of {@value #PARALLELIZATION_DEFAULT} and a block size of {@value #BLOCK_DEFAULT_SIZE}.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param salt a salt.
     */
    public ScryptParameters(int keySize, byte[] salt) {
        this(keySize, COST_DEFAULT, PARALLELIZATION_DEFAULT, BLOCK_DEFAULT_SIZE, salt);
    }

    /**
     * Initialize parameters to custom values, use a random salt and a block size of {@value #BLOCK_DEFAULT_SIZE}.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param costParameter CPU/Memory cost parameter, must be larger than 1, a power of 2.
     * @param parallelizationParameter Parallelization parameter, a positive integer less than or equal
     *                                 to (2^37-32) / 1024
     */
    public ScryptParameters(int keySize, int costParameter, int parallelizationParameter) {
        this(keySize, costParameter, parallelizationParameter, BLOCK_DEFAULT_SIZE, getRandomSalt());
    }

    /**
     * Initialize parameters to custom values and use a block size of {@value #BLOCK_DEFAULT_SIZE}.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param costParameter CPU/Memory cost parameter, must be larger than 1, a power of 2.
     * @param parallelizationParameter Parallelization parameter, a positive integer less than or equal
     *                                 to (2^37-32) / 1024
     * @param salt a salt.
     */
    public ScryptParameters(int keySize, int costParameter, int parallelizationParameter, byte[] salt) {
        this(keySize, costParameter, parallelizationParameter, BLOCK_DEFAULT_SIZE, salt);
    }

    /**
     * Initialize all parameters to custom values and use a random salt.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param costParameter CPU/Memory cost parameter, must be larger than 1, a power of 2
     *                      and less than 2^(16 * blockSize)
     * @param parallelizationParameter Parallelization parameter, a positive integer less than or equal
     *                                 to (2^37-32) / (128 * blockSize)
     * @param blockSize Block size parameter.
     */
    public ScryptParameters(int keySize, int costParameter, int parallelizationParameter, int blockSize)
    {
        this(keySize, costParameter, parallelizationParameter, blockSize, getRandomSalt());
    }

    /**
     * Initialize all parameters to custom values.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     * @param costParameter CPU/Memory cost parameter, must be larger than 1, a power of 2
     *                      and less than 2^(16 * blockSize)
     * @param parallelizationParameter Parallelization parameter, a positive integer less than or equal
     *                                 to (2^37-32) / (128 * blockSize)
     * @param blockSize Block size parameter.
     * @param salt a salt.
     */
    public ScryptParameters(int keySize, int costParameter, int parallelizationParameter, int blockSize, byte[] salt)
    {
        super(keySize);
        this.costParameter = costParameter;
        this.blockSize = blockSize;
        this.parallelizationParameter = parallelizationParameter;
        this.salt = salt;
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
        return "Scrypt";
    }

    /**
     * @return the CPU/Memory cost parameter N.
     */
    public int getCostParameter()
    {
        return costParameter;
    }

    /**
     * @return the parallelization parameter.
     */
    public int getParallelizationParameter()
    {
        return parallelizationParameter;
    }

    /**
     * @return the salt value.
     */
    public byte[] getSalt()
    {
        return salt;
    }

    /**
     * @return the block size parameter r.
     */
    public int getBlockSize()
    {
        return blockSize;
    }
}

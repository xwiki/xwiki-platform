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
package org.xwiki.crypto.passwd.internal;

import java.security.SecureRandom;

import org.bouncycastle.crypto.digests.SHA256Digest;

/**
 * The Scrypt Key Derivation Function.
 * A java implementation of this http://www.tarsnap.com/scrypt/scrypt.pdf
 *
 * Scrypt function includes a shortcut which may be regarded as a weakness.
 * This function may be executed with half the memory by throwing out every other blockMix output and when one of the
 * missing outputs is needed, recomputing it. This will halve the memory cost while increasing the processor cost by
 * 1/6 of the total. Using even less memory is possible but the processor cost grows at a much higher rate.
 * The existance of this shortcut has been confirmend with the designer of Scrypt who claimed it was for cases when
 * decryption was necessary on a system with less memory than the system used to encrypt the data.
 *
 * @since 2.5M1
 * @version $Id$
 */
public class ScryptMemoryHardKeyDerivationFunction extends AbstractMemoryHardKeyDerivationFunction
{
    /**
     * Fields in this class are set in stone!
     * Any changes may result in encrypted data becoming unreadable.
     * This class should be extended if any changes need to be made.
     */
    private static final long serialVersionUID = 1L;

    /** Abstract number referring to how much memory should be expended for hashing the password. */
    private int memoryExpense;

    /** Abstract number referring to how much CPU power should be expended for hashing the password. */
    private int processorExpense;

    /** How many bytes long the output key should be. */
    private int derivedKeyLength;

    /** Random salt to frustrate cracking attempts. */
    private byte[] salt;

    /** The block size to use for the scrypt blockmix function. 8 if not overridden.*/
    private int blockSize = 8;

    /**
     * A buffer which occupies a large (configurable) amount of memory which is required for this algorithm to 
     * frustrate parallel attacks. This buffer is a class scope object because it is used in a number of 
     * methods and passing it around would be cumbersome.
     */
    private transient byte[] memoryIntensiveBuffer;

    /** 
     * Buffer used by a method called inside inside of a tight inner loop.
     * Class scoped to prevent enormous amount of garbage from creation and abandonment of buffer in each cycle.
     */
    private transient byte[] blockMixBufferX;

    /** 
     * Buffer used by a method called inside inside of a tight inner loop.
     * Class scoped to prevent enormous amount of garbage from creation and abandonment of buffer in each cycle.
     */
    private transient byte[] blockMixBufferY;

    @Override
    public void init(final int kilobytesOfMemoryToUse,
                     final int millisecondsOfProcessorTimeToSpend,
                     final int derivedKeyLength)
    {
        if (kilobytesOfMemoryToUse < 0 || millisecondsOfProcessorTimeToSpend < 0 || derivedKeyLength < 1) {
            throw new IllegalArgumentException("All arguments must be positive and derivedKeyLength must be at least 1 "
                                               + "byte long");
        }

        // Set the salt. (hardcoded salt length at 16.)
        this.salt = new byte[16];
        new SecureRandom().nextBytes(this.salt);

        // Round kilobytesOfMemoryToUse to the nearest power of 2
        int memoryCost = 1;
        while (memoryCost < kilobytesOfMemoryToUse) {
            memoryCost <<= 1;
        }
        int distanceToNext = memoryCost - kilobytesOfMemoryToUse;
        int distanceFromLast = kilobytesOfMemoryToUse - (memoryCost >> 1);
        if (distanceToNext < distanceFromLast) {
            this.memoryExpense = memoryCost;
        } else {
            this.memoryExpense = memoryCost >> 1;
        }

        // blockSize is hardcoded to 8.
        this.blockSize = 8;
        this.derivedKeyLength = derivedKeyLength;

        // Dry run BlockMix once to check time cost.
        int numTrialRuns = 64;
        try {
            this.allocateMemory(true);
            byte[] testArray = new byte[1024];
            long timeSpent = 0;
            do {
                // we need to make sure the trial run takes a reasonable time
                // note that the granularity of System.currentTimeMillis() is in the range of 20+ ms
                numTrialRuns *= 2;
                long time = System.currentTimeMillis();
                for (int i = 0; i < numTrialRuns; i++) {
                    this.blockMix(testArray);
                }
                timeSpent = System.currentTimeMillis() - time;
            } while (timeSpent <= 20);
            // Now predict the time expense.
            int totalBlockMixRuns = 2 * this.memoryExpense;
            double timeCostPerCycle = totalBlockMixRuns * timeSpent / (double) numTrialRuns;
            this.processorExpense = (int) (millisecondsOfProcessorTimeToSpend / timeCostPerCycle);
            // There are no guarentees if processorExpense is 0.
            if (this.processorExpense < 1) {
                this.processorExpense = 1;
            }
        } finally {
            this.freeMemory();
        }
    }

    /**
     * This is an implementation of scrypt(P, S, N, r, p, dkLen) function.
     *
     * @param salt when encoding, a random byte array, when verifying, the same salt as was used to encode.
     * @param memoryExpense a positive integer representing the relative memory expense which the function should take.
     *                      with blocksize equal to 8 this is the number of kilobytes of memory which this function
     *                      should require.
     * @param blockSize the block size to use in the internal function.
     * @param processorExpense a positive integer representing the relative CPU expense which the function should take.
     * @param derivedKeyLength the number of bytes of length the derived key should be (dkLen)
     */
    public void init(final byte[] salt,
                     final int memoryExpense,
                     final int blockSize,
                     final int processorExpense,
                     final int derivedKeyLength)
    {
        if (((memoryExpense & (memoryExpense - 1)) != 0) || memoryExpense < 1) {
            throw new IllegalArgumentException("memoryExpense must be a power of 2");
        }
        if (blockSize < 1 || processorExpense < 1 || derivedKeyLength < 1) {
            throw new IllegalArgumentException("blockSize, processorExpense and derivedKeyLength must be positive.");
        }
        if (blockSize * processorExpense > (1 << 30)) {
            throw new IllegalArgumentException("(blockSize * processorExpense) must be less than 2^30");
        }

        this.memoryExpense = memoryExpense;
        this.blockSize = blockSize;
        this.processorExpense = processorExpense;
        this.derivedKeyLength = derivedKeyLength;

        this.salt = new byte[salt.length];
        System.arraycopy(salt, 0, this.salt, 0, salt.length);
    }

    @Override
    public byte[] deriveKey(final byte[] password)
    {
        try {
            this.allocateMemory(true);
            PBKDF2KeyDerivationFunction sha256Pbkdf2 = new PBKDF2KeyDerivationFunction(new SHA256Digest());
            int blockSizeInBytes = 128 * this.blockSize;
            int bufferBLength = blockSizeInBytes * this.processorExpense;

            /* 1: (B_0 ... B_{p-1}) <-- PBKDF2(P, S, 1, p * MFLen) */
            byte[] workingBufferB =
                sha256Pbkdf2.generateDerivedKey(password, this.salt, 1, bufferBLength);

            /* 2: for i = 0 to p - 1 do */
            // NOTE: This loop cycles processorExpense number of cycles.
            for (int i = 0; i < workingBufferB.length; i += blockSizeInBytes) {
                /* 3: B_i <-- MF(B_i, N) */
                this.smix(workingBufferB, i);
            }

            /* 5: DK <-- PBKDF2(P, B, 1, dkLen) */
            return sha256Pbkdf2.generateDerivedKey(password, workingBufferB, 1, this.derivedKeyLength);

        } finally {
            this.freeMemory();
        }
    }

    /**
     * Allocate the memory necessary to run the hash function.
     * First check and see if there is enough free memory before attempting to allocate.
     *
     * @param forReal if false then memory isn't allocated, only tested to see if there is enough to allocate.
     */
    protected void allocateMemory(final boolean forReal)
    {
        try {
            // There is no way to prove that memory is not available without trying to allocate it.
            this.memoryIntensiveBuffer = new byte[128 * this.blockSize * this.memoryExpense];
            this.blockMixBufferX = new byte[64];
            this.blockMixBufferY = new byte[128 * this.blockSize];
        } catch (OutOfMemoryError e) {
            this.freeMemory();
            int memoryToAllocate = 128 * blockSize * processorExpense;
            memoryToAllocate += 256 * blockSize;
            memoryToAllocate += 128 * blockSize * memoryExpense;
            throw new IllegalArgumentException("Cannot allocate " + (memoryToAllocate / 1048576) + "MB of memory "
                                               + "only " + (Runtime.getRuntime().freeMemory() / 1048576)
                                               + "MB of memory are available.");
        }
        if (!forReal) {
            // Free the memory so that this object doesn't carry it around everywhere.
            this.freeMemory();
        }
    }

    /**
     * Sets the working buffers to null.
     * Necessary because this function takes a large amount of memory and it should be released as quickly as possible.
     */
    protected void freeMemory()
    {
        this.memoryIntensiveBuffer = null;
        this.blockMixBufferX = null;
        this.blockMixBufferY = null;
    }

    /*-------------------------------------------------------------------------------------------*/
    /*                                     Scrypt functions                                      */
    /*-------------------------------------------------------------------------------------------*/

    /**
     * Compute B = SMix(B, N) where B is a section of bufferToMix starting at offset and proceeding for
     * (128 * blockSize) bytes and N is memoryExpense.
     *
     * @param bufferToMix part of this buffer (128 * blockSize) bytes wide will be mixed using the SMix function.
     * @param offset the index in bytes where the part to be mixed starts.
     */
    protected void smix(final byte[] bufferToMix, final int offset)
    {
        int lengthToMix = 128 * this.blockSize;
        final byte[] bufferX = new byte[lengthToMix];
        // The index of the block in memoryIntensiveBuffer to be XOR'd against bufferX
        int blockToXOR;

        /* 1: X <-- B */
        System.arraycopy(bufferToMix, offset, bufferX, 0, lengthToMix);

        /* 2: for i = 0 to N - 1 do */
        for (int i = 0; i < this.memoryExpense; i++) {
            /* 3: V_i <-- X */
            System.arraycopy(bufferX, 0, this.memoryIntensiveBuffer, i * lengthToMix, lengthToMix);

            /* 4: X <-- H(X) */
            this.blockMix(bufferX);
        }

        /* 6: for i = 0 to N - 1 do */
        for (int i = 0; i < this.memoryExpense; i++) {
            /* 7: j <-- Integerify(X) mod N */
            blockToXOR = this.integerifyAndMod(bufferX, this.memoryExpense);

            /* 8: X <-- H(X \xor V_j) */
            // This is the memory expensive part. because the output of each hash dictates which hash to load from
            // ram to resalt the hash, there is no safe way to delete any of the hash outputs from memory.
            this.bulkXOR(this.memoryIntensiveBuffer, (blockToXOR * lengthToMix), bufferX, 0, lengthToMix);
            this.blockMix(bufferX);
        }

        /* 10: B' <-- X */
        System.arraycopy(bufferX, 0, bufferToMix, offset, lengthToMix);
    }

    /**
     * Implementation of BlockMix.
     * Defined here: http://www.tarsnap.com/scrypt.html as BlockMix_salsa20/8,r (B)
     *
     * @param block 1024 byte block of bytes to scramble (B)
     */
    protected void blockMix(final byte[] block)
    {
        /* 1: X <-- B_{2r - 1} */
        System.arraycopy(block, block.length - 64, this.blockMixBufferX, 0, 64);

        /* 2: for i = 0 to 2r - 1 do */
        for (int i = 0; i < block.length; i += 64) {
            /* 3: X <-- H(X \xor B_i) */
            this.bulkXOR(block, i, this.blockMixBufferX, 0, 64);
            this.scryptSalsa8(this.blockMixBufferX);

            /* 4: Y_i <-- X */
            System.arraycopy(this.blockMixBufferX, 0, this.blockMixBufferY, i, 64);
        }

        /* 6: B' <-- (Y_0, Y_2 ... Y_{2r-2}, Y_1, Y_3 ... Y_{2r-1}) */
        for (int i = 0; i < this.blockSize; i++) {
            // Copy all of the even numbered blocks.
            System.arraycopy(this.blockMixBufferY, i * 2 * 64, block, i * 64, 64);
        }
        for (int i = 0; i < this.blockSize; i++) {
            // Copy the odd numbered blocks to locations after the last copied even number block.
            System.arraycopy(this.blockMixBufferY, (i * 2 + 1) * 64, block, (i + this.blockSize) * 64, 64);
        }
    }

    /**
     * Convert 8 bytes from a specified place in the array to an integer and take the mod of that number against
     * modulus.
     * This function uses a fast modulus operation which requires that modulus is a power of 2.
     *
     * @param array the array to take bytes from.
     * @param modulus the output will not be larger than this (must be a power of 2).
     * @return integer gathered from the array and modded against the modulus.
     */
    protected int integerifyAndMod(byte[] array, int modulus)
    {
        return this.unsignedMod(this.integerify(array), modulus);
    }

    /**
     * Convert 4 bytes from a specified place in the array to a long.
     * This is a binary operation, sign is ignored.
     *
     * The paper says Integerify takes the last 8 bytes from the array but the reference implementation
     * takes byte at index (2 * r - 1) * 64 and the 7 bytes following.
     * With a blockSize of 8, that means bytes 960-967 are used from an array of 1024.
     * This function follows the reference implementation.
     *
     * @param array the array to take bytes from.
     * @return long value gathered from the array.
     */
    protected long integerify(byte[] array)
    {
        int startIndex = (2 * this.blockSize - 1) * 64;
        //int endIndex = startIndex + 7;
        // The reference implementation only takes 4 bytes, not 8.
        int endIndex = startIndex + 3;

        long fromArray = 0;

        for (int i = endIndex; i >= startIndex; i--) {
            fromArray <<= 8;
            fromArray |= (array[i] & 0xFF);
        }
        return fromArray;
    }

    /**
     * Compute (unsignedLong % signedModulus) quickly.
     * signedModulus must be is a power of 2
     *
     * @param unsignedLong a long value which is treated as unsigned.
     * @param signedModulus a modulus which must be a positive number and is treated as signed.
     * @return value of (x - Long.MIN_VALUE) % signedModulus
     */
    protected int unsignedMod(long unsignedLong, int signedModulus)
    {
        return (int) (unsignedLong & (((long) signedModulus) - 1));
    }

    /**
     * XOR all bytes in two byte arrays.
     * The output array will be XOR'd against the input array and the result will be saved to the output array.
     * If the input array is smaller than the output, an array index exception will result.
     *
     * @param input an array which will not be modified.
     * @param inputOffset start with byte at this index.
     * @param output an array which will be modified.
     * @param outputOffset start modifying (and reading) output at this index.
     * @param length number of bytes to XOR.
     */
    protected void bulkXOR(final byte[] input,
                           final int inputOffset,
                           final byte[] output,
                           final int outputOffset,
                           final int length)
    {
        for (int i = 0; i < length; i++) {
            output[i + outputOffset] ^= input[i + inputOffset];
        }
    }

    /*-------------------------------------------------------------------------------------------*/
    /*                                    Salsa20 functions                                      */
    /*-------------------------------------------------------------------------------------------*/

    /**
     * salsa20_8 function as defined in crypto_scrypt.
     * see: http://www.tarsnap.com/scrypt.html
     *
     * @param bytesToModify 64 bytes of data to mangle with the function.
     *                      For performance reasons, this array is mutated and becomes the output.
     */  
    protected void scryptSalsa8(byte[] bytesToModify)
    {
        // Set inputAsInts to be same as input except in int[] form
        int[] inputAsInts = new int[16];
        this.bytesToIntsLittle(bytesToModify, inputAsInts);

        int[] workingBuffer = new int[16];

        // Run data through Salsa20/8
        this.salsa20Core(inputAsInts, workingBuffer, 8);

        // Convert back to byte[]
        this.intsToBytesLittle(workingBuffer, bytesToModify);
    }
    
    /**
     * Salsa20 function.
     * Implementation of function defined here: http://cr.yp.to/salsa20.html
     *
     * @param input the data to mangle with the function (array of int, 16 long)
     * @param output the output will be put into this array.
     * @param numberOfRounds if this is 8, you will have Salsa20/8 if 12, you have Salsa20/12, if 20 you have Salsa20
     */    
    protected void salsa20Core(int[] input, int[] output, int numberOfRounds)
    {
        System.arraycopy(input, 0, output, 0, 16);
        // All processing is done on the output array.
        for (int i = numberOfRounds; i > 0; i -= 2) {
            this.salsa20ColumnHalfround(output);
            this.salsa20RowHalfround(output);
        }
        for (int i = 0; i < 16; i++) {
            output[i] = output[i] + input[i];
        }
    }

    /**
     * Salsa20 column halfround function.
     * Mixes the rows but does no mixing between columns.
     *
     * @param workBuffer the data to mangle with the function (array of int, 16 long)
     */
    private void salsa20ColumnHalfround(int[] workBuffer)
    {
        workBuffer[ 4] ^= rotl((workBuffer[ 0] + workBuffer[12]),  7);
        workBuffer[ 8] ^= rotl((workBuffer[ 4] + workBuffer[ 0]),  9);
        workBuffer[12] ^= rotl((workBuffer[ 8] + workBuffer[ 4]), 13);
        workBuffer[ 0] ^= rotl((workBuffer[12] + workBuffer[ 8]), 18);

        workBuffer[ 9] ^= rotl((workBuffer[ 5] + workBuffer[ 1]),  7);
        workBuffer[13] ^= rotl((workBuffer[ 9] + workBuffer[ 5]),  9);
        workBuffer[ 1] ^= rotl((workBuffer[13] + workBuffer[ 9]), 13);
        workBuffer[ 5] ^= rotl((workBuffer[ 1] + workBuffer[13]), 18);

        workBuffer[14] ^= rotl((workBuffer[10] + workBuffer[ 6]),  7);
        workBuffer[ 2] ^= rotl((workBuffer[14] + workBuffer[10]),  9);
        workBuffer[ 6] ^= rotl((workBuffer[ 2] + workBuffer[14]), 13);
        workBuffer[10] ^= rotl((workBuffer[ 6] + workBuffer[ 2]), 18);

        workBuffer[ 3] ^= rotl((workBuffer[15] + workBuffer[11]),  7);
        workBuffer[ 7] ^= rotl((workBuffer[ 3] + workBuffer[15]),  9);
        workBuffer[11] ^= rotl((workBuffer[ 7] + workBuffer[ 3]), 13);
        workBuffer[15] ^= rotl((workBuffer[11] + workBuffer[ 7]), 18);
    }

    /**
     * Salsa20 row halfround function.
     * Mixes the columns but does no mixing between rows.
     *
     * @param workBuffer the data to mangle with the function (array of int, 16 long)
     */
    private void salsa20RowHalfround(int[] workBuffer)
    {
        workBuffer[ 1] ^= rotl((workBuffer[ 0] + workBuffer[ 3]),  7);
        workBuffer[ 2] ^= rotl((workBuffer[ 1] + workBuffer[ 0]),  9);
        workBuffer[ 3] ^= rotl((workBuffer[ 2] + workBuffer[ 1]), 13);
        workBuffer[ 0] ^= rotl((workBuffer[ 3] + workBuffer[ 2]), 18);

        workBuffer[ 6] ^= rotl((workBuffer[ 5] + workBuffer[ 4]),  7);
        workBuffer[ 7] ^= rotl((workBuffer[ 6] + workBuffer[ 5]),  9);
        workBuffer[ 4] ^= rotl((workBuffer[ 7] + workBuffer[ 6]), 13);
        workBuffer[ 5] ^= rotl((workBuffer[ 4] + workBuffer[ 7]), 18);

        workBuffer[11] ^= rotl((workBuffer[10] + workBuffer[ 9]),  7);
        workBuffer[ 8] ^= rotl((workBuffer[11] + workBuffer[10]),  9);
        workBuffer[ 9] ^= rotl((workBuffer[ 8] + workBuffer[11]), 13);
        workBuffer[10] ^= rotl((workBuffer[ 9] + workBuffer[ 8]), 18);

        workBuffer[12] ^= rotl((workBuffer[15] + workBuffer[14]),  7);
        workBuffer[13] ^= rotl((workBuffer[12] + workBuffer[15]),  9);
        workBuffer[14] ^= rotl((workBuffer[13] + workBuffer[12]), 13);
        workBuffer[15] ^= rotl((workBuffer[14] + workBuffer[13]), 18);
    }

    /**
     * Rotate left.
     * Copied from bouncycastle.crypto.engines.Salsa20Engine
     *
     * @param   x   value to rotate
     * @param   y   amount to rotate x
     *
     * @return  rotated x
     */
    protected int rotl(int x, int y)
    {
        return (x << y) | (x >>> -y);
    }

    /**
     * Convert an array of integer to an array of little endian bytes.
     *
     * @param input the integers to convert.
     * @param output the byte array to put the output in, if this array is not at least 4 times the length of input,
     *               an array index out of bounds exception will result.
     */
    protected void intsToBytesLittle(int[] input, byte[] output)
    {
        int outCounter = 0;
        for (int i = 0; i < input.length; i++) {
            output[outCounter] = (byte) input[i];
            output[outCounter + 1] = (byte) (input[i] >>> 8);
            output[outCounter + 2] = (byte) (input[i] >>> 16);
            output[outCounter + 3] = (byte) (input[i] >>> 24);
            outCounter += 4;
        }
    }

    /**
     * Pack byte[] array into an int in little endian order.
     *
     * @param input the byte array to convert to int array.
     * @param output the int array to put the output in, if this array is not at least 1/4 the length of input,
     *               an array index out of bounds exception will result.
     */
    protected void bytesToIntsLittle(byte[] input, int[] output)
    {
        int outCounter = 0;
        for (int i = 0; i < input.length; i += 4) {
            output[outCounter] = input[i] & 255;
            output[outCounter] |= (input[i + 1] & 255) <<  8;
            output[outCounter] |= (input[i + 2] & 255) << 16;
            output[outCounter] |= input[i + 3] << 24;
            outCounter++;
        }
    }
}

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

import java.util.Properties;
import java.io.Serializable;
import java.io.IOException;

import java.security.SecureRandom;

import org.xwiki.crypto.internal.SerializationUtils;
import org.xwiki.crypto.passwd.KeyDerivationFunction;


/**
 * The abstract key derivation function.
 * Provides guess/trial based determination of the correct number of iterations for a given processor time requirement.
 *
 * @since 2.5M1
 * @version $Id$
 */
public abstract class AbstractKeyDerivationFunction implements KeyDerivationFunction, Serializable
{
    /**
     * Fields in this class are set in stone!
     * Any changes may result in encrypted data becoming unreadable.
     * This class should be extended if any changes need to be made.
     */
    private static final long serialVersionUID = 1L;

    /** Number of bytes length of the salt. This is statically set to 16. */
    private final transient int saltSize = 16;

    /** The default amount of processor time to spend deriving the key. */
    private final transient int defaultMillisecondsOfProcessorTime = 200;

    /** The default length in bytes of the derived key (output). */
    private final transient int defaultDerivedKeyLength = 32;

    @Override
    public byte[] serialize() throws IOException
    {
        return SerializationUtils.serialize(this);
    }

    @Override
    public void init()
    {
        this.init(this.getDefaultMillisecondsOfProcessorTime(), this.getDefaultDerivedKeyLength());
    }

    @Override
    public void init(Properties parameters)
    {
        int keyLength = this.getDefaultDerivedKeyLength();
        int milliseconds = this.getDefaultMillisecondsOfProcessorTime();
        try {
            final String millisecondsString = parameters.getProperty("millisecondsOfProcessorTimeToSpend");
            if (millisecondsString != null) {
                milliseconds = Integer.parseInt(millisecondsString);
            }
            final String keyLengthString = parameters.getProperty("derivedKeyLength");
            if (keyLengthString != null) {
                keyLength = Integer.parseInt(keyLengthString);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse Properties", e);
        }
        this.init(milliseconds, keyLength);
    }

    @Override
    public void init(final int millisecondsOfProcessorTimeToSpend,
                     final int derivedKeyLength)
    {
        // Generate the salt.
        final byte[] salt = new byte[this.saltSize];
        new SecureRandom().nextBytes(salt);

        // Try with 20 cycles.
        int testIterationCount = 20;

        this.init(salt, testIterationCount, derivedKeyLength);

        // Since the error is % wise, it grows as the total increases.
        // use a test length dependent on the target time to spend.
        int testLength = millisecondsOfProcessorTimeToSpend / 100;
        // Mimimum test length is 4ms.
        if (testLength < 4) {
            testLength = 4;
        }

        // Time a test run.
        long time = System.currentTimeMillis();
        // Run until 4 milliseconds have gone by.
        int numberOfCycles = 0;
        while ((System.currentTimeMillis() - time) < testLength) {
            this.deriveKey(salt);
            numberOfCycles += testIterationCount;
        }
        // Set the iteration count to target run time / testLength (because the test run went testLength milliseconds)
        // multiplied by the number of cycles in the test run.
        int iterationCount = (millisecondsOfProcessorTimeToSpend / testLength) * numberOfCycles;

        // Set the final iterationCount value.
        this.init(salt, iterationCount, derivedKeyLength);
    }

    /**
     * Initialize the function manually.
     *
     * @param salt the random salt to add to the password before hashing.
     * @param iterationCount the number of iterations which the internal function should run.
     * @param derivedKeyLength the number of bytes of length the derived key should be (dkLen)
     */
    public abstract void init(final byte[] salt,
                              final int iterationCount,
                              final int derivedKeyLength);

    /** @return the default number of milliseconds of processor time to require. */
    protected int getDefaultMillisecondsOfProcessorTime()
    {
        return this.defaultMillisecondsOfProcessorTime;
    }

    /** @return the default size of the derived key (output) int bytes. */
    protected int getDefaultDerivedKeyLength()
    {
        return this.defaultDerivedKeyLength;
    }
}

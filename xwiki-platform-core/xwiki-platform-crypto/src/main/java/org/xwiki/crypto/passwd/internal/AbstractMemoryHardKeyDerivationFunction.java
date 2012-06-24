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

import java.io.IOException;
import java.util.Properties;

import org.xwiki.crypto.internal.SerializationUtils;
import org.xwiki.crypto.passwd.MemoryHardKeyDerivationFunction;

/**
 * Abstract memory hard key derivation function. to subclass this, simply implement init(int, int, int) from
 * MemoryHardKeyDerivationFunction and isInitialized() and deriveKey(byte[]) from KeyDerivationFunction. Be careful,
 * this class is serializable, serialization and deserialization should yield a function which provides the same
 * password to key mapping, and make sure fields unnecessary to this are declared transient.
 * 
 * @since 2.5M1
 * @version $Id$
 */
public abstract class AbstractMemoryHardKeyDerivationFunction implements MemoryHardKeyDerivationFunction
{
    /**
     * Fields in this class are set in stone! Any changes may result in encrypted data becoming unreadable. This class
     * should be extended if any changes need to be made.
     */
    private static final long serialVersionUID = 1L;

    /** Number of bytes length of the salt. This is statically set to 16. */
    private final transient int saltSize = 16;

    /** Amount of memory to use by default (1MB). */
    private final transient int defaultNumberOfKilobytesOfMemoryToUse = 1024;

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
        this.init(this.getDefaultNumberOfKilobytesOfMemoryToUse(), this.getDefaultMillisecondsOfProcessorTime(),
            this.getDefaultDerivedKeyLength());
    }

    @Override
    public void init(Properties parameters)
    {
        int keyLength = this.getDefaultDerivedKeyLength();
        int milliseconds = this.getDefaultMillisecondsOfProcessorTime();
        int kilobytes = this.getDefaultNumberOfKilobytesOfMemoryToUse();
        try {
            final String millisecondsString = parameters.getProperty("millisecondsOfProcessorTimeToSpend");
            if (millisecondsString != null) {
                milliseconds = Integer.parseInt(millisecondsString);
            }
            final String keyLengthString = parameters.getProperty("derivedKeyLength");
            if (keyLengthString != null) {
                keyLength = Integer.parseInt(keyLengthString);
            }
            final String kilobytesString = parameters.getProperty("numberOfKilobytesOfMemoryToUse");
            if (kilobytesString != null) {
                kilobytes = Integer.parseInt(kilobytesString);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not parse Properties", e);
        }
        this.init(kilobytes, milliseconds, keyLength);
    }

    @Override
    public void init(final int millisecondsOfProcessorTimeToSpend, final int derivedKeyLength)
    {
        this.init(this.getDefaultNumberOfKilobytesOfMemoryToUse(), millisecondsOfProcessorTimeToSpend,
            derivedKeyLength);
    }

    /** @return the number of kilobytes of memory to require by default. */
    public int getDefaultNumberOfKilobytesOfMemoryToUse()
    {
        return this.defaultNumberOfKilobytesOfMemoryToUse;
    }

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

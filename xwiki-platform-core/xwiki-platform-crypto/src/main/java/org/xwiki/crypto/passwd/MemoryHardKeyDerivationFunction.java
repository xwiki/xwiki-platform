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

import org.xwiki.component.annotation.Role;

/**
 * A key memory hard derivation function.
 * This function must require the specified amount of memory and processor time to derive a key from a password
 * thus making the job of guessing passwords with a known key or ciphertext configurably hard.
 * Implementations must ensure there is no way to:
 * A. Compute the hash in significantly fewer processor cycles than the implementation.
 * B. Compute the hash with significantly less memory requirement than the implementation.
 *
 * @since 2.5M1
 * @version $Id$
 */
@Role
public interface MemoryHardKeyDerivationFunction extends KeyDerivationFunction
{
    /**
     * Initialize this function with the desired parameters.
     *
     * @param kilobytesOfMemoryToUse number of kilobytes or RAM which should be required by any implementation to
     *                               validate a password. This amount of memory will be occupied for the time given
     *                               by millisecondsOfProcessorTimeToSpend. Obviously if the adversary has a faster
     *                               processor, then this time will be shorter on his computer.
     * @param millisecondsOfProcessorTimeToSpend target amount of time to spend verifying the password. This will be
     *                                           tested on the system when init is called.
     * @param derivedKeyLength the number of bytes of length the derived key (output) should be.
     */
    void init(final int kilobytesOfMemoryToUse,
              final int millisecondsOfProcessorTimeToSpend,
              final int derivedKeyLength);
}

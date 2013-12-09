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

import org.xwiki.stability.Unstable;

/**
 * Derived version of key parameters to specify rounds of RC5.
 *
 * WARNING: RSA Security own the US Patent US5724428 A on this algorithm. Therefore, before expiration of this patent,
 * which will happen in november 2015, the usage of this algorithm is subject to restricted usage on the US territories.
 * RC5 is a registered trademark of RSA Security.
 *
 * @version $Id$
 */
@Unstable
public class RC5KeyParameters extends KeyParameter
{
    private static final int ROUNDS = 12;
    private final int rounds;

    /**
     * Initialize parameters with a default number of rounds set to {@value #ROUNDS}.
     * @param key the key.
     */
    public RC5KeyParameters(byte[] key)
    {
        super(key);
        this.rounds = ROUNDS;
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param rounds the number of "rounds" in the encryption operation between 8 and 127.
     */
    public RC5KeyParameters(byte[] key, int rounds)
    {
        super(key);
        this.rounds = rounds;
    }

    /**
     * @return the number of "rounds" in the encryption operation between 8 and 127.
     */
    public int getRounds()
    {
        return rounds;
    }
}

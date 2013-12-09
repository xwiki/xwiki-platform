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
 * Derived version of key parameters to specify effective key bits of RC2.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class RC2KeyParameters extends KeyParameter
{
    private final int bits;

    /**
     * Initialize parameters.
     * @param key the key, all bits are considered effective.
     */
    public RC2KeyParameters(byte[] key)
    {
        super(key);
        this.bits = (key.length > 128) ? 1024 : (key.length * 8);
    }

    /**
     * Initialize parameters.
     * @param key the key.
     * @param bits the number of effective bits in the key.
     */
    public RC2KeyParameters(byte[] key, int bits)
    {
        super(key);
        this.bits = bits;
    }

    /**
     * @return the number of effective bits that should be used for the key.
     */
    public int getEffectiveBits()
    {
        return bits;
    }
}

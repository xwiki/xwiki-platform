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

import org.xwiki.stability.Unstable;

/**
 * Key derivation function parameters.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class KeyDerivationFunctionParameters
{
    private int keySize;

    /**
     * Initialise parameters with default values.
     */
    public KeyDerivationFunctionParameters() {
        this(-1);
    }

    /**
     * Initialise parameters with a key length and default randomized values.
     *
     * @param keySize Size of key to be generated in bytes. A negative value means that the key length should be
     *                smartly deducted from the context of use.
     */
    public KeyDerivationFunctionParameters(int keySize) {
        this.keySize = keySize;
    }

    /**
     * @return the size of the key to generate in bytes.
     */
    public int getKeySize()
    {
        return keySize;
    }

    /**
     * @return the algorithm name (hint) of the Key Derivation Function that use these parameters.
     */
    public String getAlgorithmName()
    {
        return "Default";
    }
}

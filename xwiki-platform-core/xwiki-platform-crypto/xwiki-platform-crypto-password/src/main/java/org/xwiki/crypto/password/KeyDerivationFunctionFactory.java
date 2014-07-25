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
package org.xwiki.crypto.password;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.stability.Unstable;

/**
 * Factory for Key Derivation Function.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Role
@Unstable
public interface KeyDerivationFunctionFactory
{
    /**
     * @return the algorithm name of created functions which is the hint of the factory (ie: Default, PKCS5S2, ...)
     */
    String getKDFAlgorithmName();

    /**
     * Create a new initialized key derivation function from parameters.
     *
     * @param parameters parameters to initialize the function.
     * @return a initialized key derivation function.
     */
    KeyDerivationFunction getInstance(KeyDerivationFunctionParameters parameters);

    /**
     * Create a new initialized key derivation function from serialized encoding.
     *
     * @param encoded encoded parameters to initialize the function.
     * @return a initialized key derivation function.
     */
    KeyDerivationFunction getInstance(byte[] encoded);
}

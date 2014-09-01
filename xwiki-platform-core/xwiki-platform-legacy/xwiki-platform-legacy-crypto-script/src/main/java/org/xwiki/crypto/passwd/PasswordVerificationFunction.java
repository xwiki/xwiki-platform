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

import java.io.Serializable;
import java.io.IOException;


/**
 * Password verification function differs from key derivation function in that password verification function
 * stores the hashed password given it. While this is quite wrong for encryption, it is exactly what is needed
 * for verifying a password. Implementations are expected to wrap a key derivation function and store it's output.
 *
 * @since 2.5M1
 * @version $Id$
 */
@Deprecated
public interface PasswordVerificationFunction extends Serializable
{
    /**
     * Initialize this function with the desired key length.
     *
     * @param underlyingHashFunction the function which will be used when converting the password to a hash.
     * @param password the user supplied password to be verified later.
     */
    void init(final KeyDerivationFunction underlyingHashFunction,
              final byte[] password);

    /**
     * Store this function as a byte array so another function of the same class can be initialized with the same
     * array and will then produce the same key for the given password.
     *
     * @return a byte array which can be used to recreate the same function again using init.
     * @throws IOException if something fails within the serialization framework.
     */
    byte[] serialize() throws IOException;

    /**
     * Validate a user supplied password.
     *
     * @param password the user supplied password.
     * @return true if the hash of the password is the same as the stored hash.
     */
    boolean isPasswordCorrect(final byte[] password);
}

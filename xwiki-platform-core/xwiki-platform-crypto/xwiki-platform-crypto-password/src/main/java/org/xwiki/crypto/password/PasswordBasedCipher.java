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

import java.io.IOException;

import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.stability.Unstable;

/**
 * Password based cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public interface PasswordBasedCipher extends Cipher
{
    /**
     * @return the key derivation function that has been used to derive the password of this cipher.
     */
    KeyDerivationFunction getKeyDerivationFunction();

    /**
     * @return the cipher parameters that has been used to initialize this cipher.
     */
    SymmetricCipherParameters getParameters();

    /**
     * Serialize the definition of this password based cipher.
     *
     * This serialization could be provided to an appropriate factory (like the one that have been used to create this
     * cipher) to produce an equivalent cipher. The serialization contains the derivation function algorithm and
     * parameters, and the cipher parameters.
     * For best interoperability, the recommended encoding is ASN.1 in DER format.
     *
     * @return an encoded definition of this password based cipher.
     * @throws IOException on error
     */
    byte[] getEncoded() throws IOException;
}

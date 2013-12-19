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
import org.xwiki.crypto.cipher.CipherSpecifications;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.stability.Unstable;

/**
 * Factory creating block ciphers for encryption and decryption that use a password to derive their keys
 * (and initialization vector when that one is not provided).
 *
 * @version $Id$
 * @since 5.4M1
 */
@Role
@Unstable
public interface PasswordBasedCipherFactory extends CipherSpecifications
{
    /**
     * Create a new initialized password based cipher from parameters.
     *
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param password cipher parameters, using a password converted to bytes for the key.
     * @param kdfParameters the parameters of the derivation function.
     * @return an initialized cipher ready to process data.
     */
    PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters kdfParameters);

    /**
     * Create a new initialized password based cipher using the given key derivation function.
     *
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param password cipher parameters, using a password converted to bytes for the key.
     * @param function the key derivation function to use for deriving key from password.
     * @return an initialized cipher ready to process data.
     */
    PasswordBasedCipher getInstance(boolean forEncryption, SymmetricCipherParameters password,
        KeyDerivationFunction function);

    /**
     * Create a new initialized password based cipher from serialized encoding.
     *
     * @param forEncryption if true the cipher is initialised for encryption, if false for decryption.
     * @param password the password used to derive the encryption key.
     * @param encoded encoded parameters to initialize the cipher and derivation function.
     * @return an initialized cipher ready to process data.
     */
    PasswordBasedCipher getInstance(boolean forEncryption, byte[] password, byte[] encoded);
}

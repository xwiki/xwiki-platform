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

import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.stability.Unstable;

/**
 * Key derivation function from password interface.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public interface KeyDerivationFunction
{
    /**
     * @return the current requested key size in bytes.
     */
    int getKeySize();

    /**
     * Override the key size receive from the factory.
     *
     * This is mainly useful internally when the key size from parameter is negative
     * (see {@link org.xwiki.crypto.password.params.KeyDerivationFunctionParameters}) to set the effective key size that
     * should be produced by the derivation function. This value will not be encoded with the function, which means
     * that the recipient of the encoded form will also have to overwrite the key size to be able to use this function.
     *
     * @param keySize the length of the key to generate. A negative or null value means use the one from parameters.
     */
    void overrideKeySize(int keySize);

    /**
     * @return true if the key size has been overwritten.
     */
    boolean isKeySizeOverwritten();

    /**
     * @return the parameters used by this key derivation function.
     */
    KeyDerivationFunctionParameters getParameters();

    /**
     * Derive a key from the provided password.
     *
     * @param password the password already converted properly to a byte array.
     *                 See ({@link PasswordToByteConverter}) for converting password properly.
     * @return a key parameters with the generated key.
     */
    KeyParameter derive(byte[] password);

    /**
     * Derive a key and an initialization vector of the requested size from the provided password.
     *
     * Security note: Deriving the initialization vector and the key from the same password is not recommended since
     * it partially defeat the purpose of the initialization vector which is to salt the resulting encrypted data.
     *
     * @param password the password already converted properly to a byte array.
     *                 See ({@link PasswordToByteConverter}) for converting password properly.
     * @param ivSize the initialization vector size in byte.
     * @return a key with iv parameters.
     */
    KeyWithIVParameters derive(byte[] password, int ivSize);

    /**
     * Serialize the definition of this key derivation function.
     *
     * This serialization could be provided to an appropriate factory (like the one that have been used to create this
     * function) to produce an equivalent function. The serialization contains the key algorithm and the key parameters.
     * For best interoperability, the recommended encoding is ASN.1 in DER format.
     *
     * @return an encoded definition of this derivation function.
     * @throws IOException on error
     */
    byte[] getEncoded() throws IOException;
}

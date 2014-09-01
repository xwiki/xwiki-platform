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

import java.security.GeneralSecurityException;


/**
 * Ciphertext represents a single password encrypted data.
 * It can be serialized and deserialized and the same password will be able to decrypt it.
 *
 * @version $Id$
 * @since 2.5M1
 */
@Deprecated
public interface PasswordCiphertext extends Serializable
{
    /**
     * Initialize this ciphertext with a given message (i.e. plaintext), password, and an initialized key derivation
     * function. To get the data back, use {@link #decrypt(String)} with the same password.
     *
     * @param message the message which will be encrypted.
     * @param password the password used to encrypt the message.
     * @param initializedKeyFunction an initialized KeyDerivationFunction which will return a key of the length given
     *                               by {@link #getRequiredKeySize()}.
     * @throws GeneralSecurityException if something goes wrong while encrypting.
     */
    void init(final byte[] message, final String password, final KeyDerivationFunction initializedKeyFunction)
        throws GeneralSecurityException;

    /**
     * Get the message (i.e. plaintext) back from this ciphertext.
     * Most of the time the response is null if the password is incorrect, 1 out of 250 times the output is 
     * unintelligable garbage which sneaks undetected past the padding scheme.
     *
     * @param password the user supplied password.
     * @return the original message or null if the password was wrong.
     * @throws GeneralSecurityException if something goes wrong while decrypting.
     */
    byte[] decrypt(final String password)
        throws GeneralSecurityException;

    /**
     * Serialize this ciphertext into a byte array which can later be deserialized and the text decrypted from that.
     *
     * @return a byte array representing this object.
     * @throws IOException if something goes wrong in the serialization framework.
     */
    byte[] serialize()
        throws IOException;

    /**
     * Get the length of the key which should be output by the keyFunction which is to be passed to 
     * {@link #init(byte[], String, KeyDerivationFunction)}.
     * Users are expected to call this method and pass the result when initializing the key derivation function then
     * pass the initialized key derivation function to {@link #init(byte[], String, KeyDerivationFunction)}.
     *
     * @return the length required for the derived key.
     * @see org.xwiki.crypto.passwd.PasswordCiphertext#getRequiredKeySize()
     */
    int getRequiredKeySize();
}

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

import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Role;

/**
 * Service allowing users to encrypt and decrypt text using a password.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Role
public interface PasswordCryptoService
{
    /**
     * Encipher the given text with the password. The same password will be able to decipher it.
     *
     * @param plaintext the text to encrypt.
     * @param password which will be needed to decrypt the text.
     * @return Base64 encoded ciphertext which can be decrypted back to plaintext only with the decryptText function.
     * @throws GeneralSecurityException if something goes wrong.
     */
    String encryptText(final String plaintext, final String password)
        throws GeneralSecurityException;

    /**
     * Decrypt a piece of text encrypted with encryptText.
     *
     * @param base64Ciphertext Base64 encoded ciphertext to decrypt.
     * @param password which was used to encrypt the text.
     * @return the decrypted text or null if the provided password was wrong.
     * @throws GeneralSecurityException if something goes wrong.
     */
    String decryptText(final String base64Ciphertext, final String password)
        throws GeneralSecurityException;

    /**
     * Encipher the given byte array with the password. The same password will be able to decipher it.
     *
     * @param message the message to encrypt.
     * @param password which will be needed to decrypt the text.
     * @return raw ciphertext which can be decrypted back to data using {@link #decryptBytes(byte[], String)}
     * @throws GeneralSecurityException if something goes wrong.
     */
    byte[] encryptBytes(final byte[] message, final String password)
        throws GeneralSecurityException;

    /**
     * Decrypt raw ciphertext created with {@link #encryptBytes(byte[], String)}.
     * Most of the time the response is null if the password is incorrect, 1 out of 250 times the output is 
     * unintelligable garbage.
     *
     * @param rawCiphertext the ciphertext to decrypt.
     * @param password which was used to encrypt the text.
     * @return the decrypted message or null if the provided password was wrong.
     * @throws GeneralSecurityException if something goes wrong.
     */
    byte[] decryptBytes(final byte[] rawCiphertext, final String password)
        throws GeneralSecurityException;

    /**
     * Hash a password with a hash function specifically designed to make password guessing attacks difficult.
     * This hash does salting and multiple iterations which incure not only CPU but memory expense.
     *
     * @param password the plain text user supplied password.
     * @return a String of base-64 formatted bytes which can be used to verify the password later using
     *         isPasswordCorrect. It is generally considered impossible to derive a password from this data however
     *         for particularly easy to guess passwords, an attacker may guess the password using isPasswordCorrect
     *         although the underlying function is designed to make that resource intensive.
     * @throws GeneralSecurityException on errors
     */
    String protectPassword(final String password) throws GeneralSecurityException;

    /**
     * Check the validity of a password.
     *
     * @param password the plain text user supplied password.
     * @param protectedPassword the result from calling protectPassword.
     * @return true if after running the user supplied password through the same underlying function, the output 
     *         matches the protectedPassword.
     * @throws GeneralSecurityException on errors
     */
    boolean isPasswordCorrect(final String password, final String protectedPassword)
        throws GeneralSecurityException;
}

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
import java.security.GeneralSecurityException;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.stability.Unstable;

/**
 * Manage encryption of private keys using password based ciphers as defined in PKCS #8.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Role
@Unstable
public interface PrivateKeyPasswordBasedEncryptor
{
    /**
     * Decrypt a private key from an encoded byte array.
     *
     * @param password the password used to derive the encryption key.
     * @param encoded the encrypted key in ASN.1 format according to PKCS #8.
     * @return decrypted private key parameters.
     * @throws GeneralSecurityException if an error occurs during decryption.
     * @throws IOException if an error occurs during decoding.
     */
    PrivateKeyParameters decrypt(byte[] password, byte[] encoded) throws GeneralSecurityException, IOException;

    /**
     * Decrypt a private key from an {@link javax.crypto.EncryptedPrivateKeyInfo}.
     *
     * @param password the password used to derive the encryption key.
     * @param privateKeyInfo the encrypted private key information.
     * @return decrypted private key parameters.
     * @throws GeneralSecurityException if an error occurs during decryption.
     * @throws IOException if an error occurs during decoding.
     */
    PrivateKeyParameters decrypt(byte[] password, javax.crypto.EncryptedPrivateKeyInfo privateKeyInfo)
        throws GeneralSecurityException, IOException;

    /**
     * Encrypt a private key using a password based cipher in a PKCS #8 format.
     *
     * @param algHint the hint of the PasswordBasedCipher to use.
     * @param password cipher parameters, using a password converted to bytes for the key.
     * @param kdfParameters the parameters of the derivation function.
     * @param privateKey the private key parameters to encrypt.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */
    byte[] encrypt(String algHint, SymmetricCipherParameters password,
        KeyDerivationFunctionParameters kdfParameters, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException;

    /**
     * Encrypt a private key using a password based cipher in a PKCS #8 format.
     *
     * @param algHint the hint of the PasswordBasedCipher to use.
     * @param password cipher parameters, using a password converted to bytes for the key.
     * @param function the key derivation function to use for deriving key from password.
     * @param privateKey cipher parameters, using a password converted to bytes for the key.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */
    byte[] encrypt(String algHint, SymmetricCipherParameters password,
        KeyDerivationFunction function, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException;

    /**
     * Encrypt a private key using a password based cipher in a PKCS #8 format.
     *
     * @param algHint the hint of the PasswordBasedCipher to use.
     * @param password the password used to derive the encryption key.
     * @param encoded encoded parameters to initialize the cipher and derivation function.
     * @param privateKey the private key parameters to encrypt.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */
    byte[] encrypt(String algHint, byte[] password, byte[] encoded, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException;

    /**
     * Encrypt a private key using a AES 256 password based cipher in a PKCS #8 format.
     *
     * @param password the password used to derive the encryption key.
     * @param privateKey the private key parameters to encrypt.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */

    byte[] encrypt(byte[] password, PrivateKeyParameters privateKey)
        throws GeneralSecurityException, IOException;

    /**
     * Encrypt a private key using a password based cipher in a PKCS #8 format.
     *
     * @param cipher the initialized PasswordBasedCipher to use.
     * @param privateKey the private key parameters to encrypt.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */
    byte[] encrypt(PasswordBasedCipher cipher, PrivateKeyParameters privateKey)
        throws IOException, GeneralSecurityException;

    /**
     * Encrypt a private key in a PKCS #8 format using a random initialization vector.
     *
     * @param algHint the hint of the PasswordBasedCipher to use.
     * @param password the password used to derive the encryption key.
     * @param kdfParameters the parameters of the derivation function.
     * @param privateKey the private key parameters to encrypt.
     * @return the encrypted key in ASN.1 format according to PKCS #8.
     * @throws GeneralSecurityException if an error occurs during encryption.
     * @throws IOException if an error occurs during encoding.
     */
    byte[] encrypt(String algHint, byte[] password, KeyDerivationFunctionParameters kdfParameters,
        PrivateKeyParameters privateKey)
        throws IOException, GeneralSecurityException;
}

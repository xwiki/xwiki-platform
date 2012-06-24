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

import java.util.Properties;

import org.xwiki.component.annotation.Role;

/**
 * The configuration for the password based cryptography service.
 * 
 * @version $Id$
 */
@Role
public interface PasswordCryptoServiceConfiguration
{
    /** @return the type of cipher to use for encryption. */
    Class<? extends PasswordCiphertext> getCipherClass();

    /** @return the type of key derivation function to use for encryption. */
    Class<? extends KeyDerivationFunction> getKeyDerivationFunctionClassForEncryption();

    /** @return the {@link Properties} for initializing the {@link KeyDerivationFunction} used for encryption. */
    Properties getKeyDerivationFunctionPropertiesForEncryption();

    /** @return the type of password validation function to use for protecting user passwords. */
    Class<? extends PasswordVerificationFunction> getPasswordVerificationFunctionClass();

    /** @return the type of key derivation function to use for password verification. */
    Class<? extends KeyDerivationFunction> getKeyDerivationFunctionClassForPasswordVerification();

    /** @return the {@link Properties} for initializing the key derivation function used for password verification. */
    Properties getKeyDerivationFunctionPropertiesForPasswordVerification();
}

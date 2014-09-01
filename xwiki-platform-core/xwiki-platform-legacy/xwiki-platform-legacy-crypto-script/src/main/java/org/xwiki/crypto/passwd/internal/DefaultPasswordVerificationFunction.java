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
package org.xwiki.crypto.passwd.internal;

import java.util.Arrays;
import java.io.IOException;

import org.xwiki.crypto.internal.SerializationUtils;
import org.xwiki.crypto.passwd.PasswordVerificationFunction;
import org.xwiki.crypto.passwd.KeyDerivationFunction;


/**
 * Default password verification function wraps a key derivation function and stores the hash output.
 *
 * @since 2.5M1
 * @version $Id$
 */
public class DefaultPasswordVerificationFunction implements PasswordVerificationFunction
{
    /**
     * Fields in this class are set in stone!
     * Any changes may result in passwords become unverifiable.
     * This class should be extended if any changes need to be made.
     */
    private static final long serialVersionUID = 1L;

    /** Password hash. */
    private byte[] passwordHash;

    /** The used key derivation function. */
    private KeyDerivationFunction underlyingHashFunction;

    @Override
    public void init(final KeyDerivationFunction underlyingHashFunction,
                     final byte[] password)
    {
        this.underlyingHashFunction = underlyingHashFunction;
        this.passwordHash = this.underlyingHashFunction.deriveKey(password);
    }

    @Override
    public byte[] serialize() throws IOException
    {
        return SerializationUtils.serialize(this);
    }

    @Override
    public boolean isPasswordCorrect(final byte[] password)
    {
        return Arrays.equals(this.passwordHash, this.underlyingHashFunction.deriveKey(password));
    }
}

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
package org.xwiki.crypto.params.cipher.asymmetric;

import org.xwiki.stability.Unstable;

/**
 * An asymmetric key pair.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class AsymmetricKeyPair
{
    private final PrivateKeyParameters privateKey;
    private final PublicKeyParameters publicKey;

    /**
     * Create a new asymmetric key pair using the given private and public key.
     *
     * @param privateKey a private key parameters.
     * @param publicKey the corresponding public key parameters.
     */
    public AsymmetricKeyPair(PrivateKeyParameters privateKey, PublicKeyParameters publicKey)
    {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    /**
     * @return the private key parameters.
     */
    public PrivateKeyParameters getPrivate()
    {
        return privateKey;
    }

    /**
     * @return the public key parameters.
     */
    public PublicKeyParameters getPublic()
    {
        return publicKey;
    }
}

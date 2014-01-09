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
package org.xwiki.crypto;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.stability.Unstable;

/**
 * Component role for creating key instances and key parameters instances.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Role
@Unstable
public interface AsymmetricKeyFactory
{
    /**
     * Create public key parameters from its X.509 encoded form.
     *
     * @param encoded an X.509 serialized form of the public key to create.
     * @return a public key.
     * @throws IOException on error.
     */
    PublicKeyParameters fromX509(byte[] encoded) throws IOException;

    /**
     * Create a private key parameters from its PKCS#8 encoded form.
     *
     * @param encoded an PKCS#8 serialized form of the private key to create.
     * @return a private key.
     * @throws IOException on error.
     */
    PrivateKeyParameters fromPKCS8(byte[] encoded) throws IOException;

    /**
     * Create a public key parameters from a (compatible) public key.
     *
     * @param key any public key.
     * @return a public key from this factory.
     */
    PublicKeyParameters fromKey(PublicKey key);

    /**
     * Create a private key parameters from a (compatible) private key.
     *
     * @param key any private key.
     * @return a private key from this factory.
     */
    PrivateKeyParameters fromKey(PrivateKey key);

    /**
     * Create a public key from public key parameters.
     *
     * @param key any public key.
     * @return a public key from this factory.
     */
    PublicKey toKey(PublicKeyParameters key);

    /**
     * Create a private key from private key parameters.
     *
     * @param key any private key.
     * @return a private key from this factory.
     */
    PrivateKey toKey(PrivateKeyParameters key);
}

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
 * Asymmetric key parameters.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public interface AsymmetricKeyParameters extends AsymmetricCipherParameters
{
    /**
     * @return true if this key is a private key.
     */
    boolean isPrivate();

    /**
     * @return the ASN.1 encoded form of this key or null if the key could not be encoded.
     */
    byte[] getEncoded();
}

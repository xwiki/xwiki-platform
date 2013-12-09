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

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.params.DigestParameters;
import org.xwiki.stability.Unstable;

/**
 * Factory for creating new digest instance.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Role
@Unstable
public interface DigestFactory
{
    /**
     * @return the algorithm name of created ciphers (ie: AES, CAST5, ...)
     */
    String getDigestAlgorithmName();

    /**
     * @return the block size of created ciphers (in bytes).
     */
    int getDigestSize();

    /**
     * @return an initialized digest ready to process data.
     */
    Digest getInstance();

    /**
     * This is a very usual need, since most digest does not take parameters, but this will allow digest that take
     * parameters to be implemented as well.
     *
     * @param parameters digest parameters to initialize the digest.
     * @return an initialized digest ready to process data based on given parameters.
     */
    Digest getInstance(DigestParameters parameters);

    /**
     * Create a new initialized digest from serialized encoding.
     *
     * @param encoded encoded parameters to initialize this digest.
     * @return an initialized digest ready to process data based on encoded data.
     */
    Digest getInstance(byte[] encoded);
}

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
package org.xwiki.crypto.cipher;

/**
 * Common specifications for cipher factories.
 *
 * @version $Id$
 * @since 5.4M1
 */
public interface CipherSpecifications
{
    /**
     * @return the algorithm name of created ciphers (ie: AES, CAST5, ...)
     */
    String getCipherAlgorithmName();

    /**
     * @return the cipher block size (in bytes) that should be used to generate an IV or -1 if the cipher does not
     *         use an initialization vector.
     */
    int getIVSize();

    /**
     * @return the recommended default key size of created ciphers (in bytes), usually the largest one supported.
     */
    int getKeySize();

    /**
     * @return an array of the supported key sizes of created ciphers (in bytes).
     */
    int[] getSupportedKeySizes();

    /**
     * @param keySize the size in bytes of a proposed key.
     * @return true if the proposed key size is supported.
     */
    boolean isSupportedKeySize(int keySize);
}

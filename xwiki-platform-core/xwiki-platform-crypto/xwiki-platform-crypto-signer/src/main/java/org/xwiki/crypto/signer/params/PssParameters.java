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
package org.xwiki.crypto.signer.params;

import org.xwiki.stability.Unstable;

/**
 * PSS parameters.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public class PssParameters
{
    private static final String DEFAULT_DIGEST = "SHA-1";
    private static final byte TRAILER_1 = (byte) 0xBC;

    private final String hashAlgorithm;
    private final String maskGenAlgorithm;
    private final int saltLength;
    private final int trailerField;

    /**
     * Construct default RSASSA-PSS parameters according to PKCS #1 definition of default value.
     */
    public PssParameters()
    {
        this(DEFAULT_DIGEST);
    }

    /**
     * Construct RSASSA-PSS parameters using default trailer and the same digest algorithm for both hash and mgf1.
     *
     * @param hashAlgorithm digest algorithm to use for hash and mgf1.
     */
    public PssParameters(String hashAlgorithm)
    {
        this(hashAlgorithm, -1);
    }

    /**
     * Construct RSASSA-PSS parameters using default trailer and the same digest algorithm for both hash and mgf1.
     *
     * @param hashAlgorithm digest algorithm to use for hash and mgf1.
     * @param saltLength size of salt in bytes.
     */
    public PssParameters(String hashAlgorithm, int saltLength)
    {
        this(hashAlgorithm, hashAlgorithm, saltLength, 1);
    }

    /**
     * Construct RSASSA-PSS parameters using custom parameters.
     *
     * @param hashAlgorithm digest algorithm to use for hash.
     * @param maskGenAlgorithm digest algorithm to use for mgf1.
     * @param saltLength size of salt in bytes, -1 means use the digest size.
     */
    public PssParameters(String hashAlgorithm, String maskGenAlgorithm, int saltLength)
    {
        this(hashAlgorithm, maskGenAlgorithm, saltLength, 1);
    }

    /**
     * Construct RSASSA-PSS parameters using custom parameters.
     *
     * @param hashAlgorithm digest algorithm to use for hash.
     * @param maskGenAlgorithm digest algorithm to use for mgf1.
     * @param saltLength size of salt in bytes, -1 means use the digest size.
     * @param trailerField trailer selection, only valid value is 1.
     */
    public PssParameters(String hashAlgorithm, String maskGenAlgorithm, int saltLength, int trailerField)
    {
        this.hashAlgorithm = hashAlgorithm;
        this.maskGenAlgorithm = maskGenAlgorithm;
        this.saltLength = saltLength;
        this.trailerField = trailerField;
    }

    /**
     * @return digest algorithm to use for hash.
     */
    public String getHashAlgorithm()
    {
        return hashAlgorithm;
    }

    /**
     * @return digest algorithm to use for mgf1.
     */
    public String getMaskGenAlgorithm()
    {
        return maskGenAlgorithm;
    }

    /**
     * @return size of salt in bytes.
     */
    public int getSaltLength()
    {
        return saltLength;
    }

    /**
     * @return trailer field.
     */
    public int getTrailerField()
    {
        return trailerField;
    }

    /**
     * @return trailer byte.
     */
    public byte getTrailerByte()
    {
        if (trailerField == 1) {
            return TRAILER_1;
        }

        throw new IllegalArgumentException("Unknown trailer field.");
    }
}

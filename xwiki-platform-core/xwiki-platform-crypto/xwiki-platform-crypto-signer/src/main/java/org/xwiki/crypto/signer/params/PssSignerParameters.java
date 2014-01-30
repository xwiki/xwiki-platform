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

import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyParameters;
import org.xwiki.stability.Unstable;

/**
 * PSS signer parameters.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public class PssSignerParameters implements AsymmetricCipherParameters
{
    private final AsymmetricKeyParameters keyParams;
    private final PssParameters pssParams;

    /**
     * Construct default RSASSA-PSS parameters according to PKCS #1 definition of default value.
     *
     * @param keyParams the key to use for the RSA cipher.
     */
    public PssSignerParameters(AsymmetricKeyParameters keyParams)
    {
        this.keyParams = keyParams;
        this.pssParams = new PssParameters();
    }

    /**
     * Construct RSASSA-PSS parameters using default trailer and the same digest algorithm for both hash and mgf1.
     *
     * @param keyParams the key to use for the RSA cipher.
     * @param hashAlgorithm digest algorithm to use for hash and mgf1.
     * @param saltLength size of salt in bytes.
     */
    public PssSignerParameters(AsymmetricKeyParameters keyParams, String hashAlgorithm, int saltLength)
    {
        this.keyParams = keyParams;
        this.pssParams = new PssParameters(hashAlgorithm, saltLength);
    }

    /**
     * Construct RSASSA-PSS parameters using custom parameters.
     *
     * @param keyParams the key to use for the RSA cipher.
     * @param hashAlgorithm digest algorithm to use for hash.
     * @param maskGenAlgorithm digest algorithm to use for mgf1.
     * @param saltLength size of salt in bytes.
     */
    public PssSignerParameters(AsymmetricKeyParameters keyParams, String hashAlgorithm, String maskGenAlgorithm,
        int saltLength)
    {
        this.keyParams = keyParams;
        this.pssParams = new PssParameters(hashAlgorithm, maskGenAlgorithm, saltLength);
    }

    /**
     * Construct RSASSA-PSS parameters using custom parameters.
     *
     * @param keyParams the key to use for the RSA cipher.
     * @param hashAlgorithm digest algorithm to use for hash.
     * @param maskGenAlgorithm digest algorithm to use for mgf1.
     * @param saltLength size of salt in bytes.
     * @param trailerField trailer selection, only valid value is 1.
     */
    public PssSignerParameters(AsymmetricKeyParameters keyParams, String hashAlgorithm, String maskGenAlgorithm,
        int saltLength, int trailerField)
    {
        this.keyParams = keyParams;
        this.pssParams = new PssParameters(hashAlgorithm, maskGenAlgorithm, saltLength, trailerField);
    }

    /**
     * @return the key paramaters.
     */
    public AsymmetricKeyParameters getKeyParameters()
    {
        return keyParams;
    }

    /**
     * @return the PSS parameters.
     */
    public PssParameters getPssParameters()
    {
        return pssParams;
    }
}

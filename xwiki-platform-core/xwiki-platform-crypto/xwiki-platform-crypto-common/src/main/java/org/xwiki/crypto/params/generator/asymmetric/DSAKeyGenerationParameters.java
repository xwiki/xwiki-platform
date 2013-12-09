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
package org.xwiki.crypto.params.generator.asymmetric;

import java.math.BigInteger;

import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.stability.Unstable;

/**
 * Shared DSA parameters for key generation.
 *
 * @version $Id$
 */
@Unstable
public class DSAKeyGenerationParameters implements KeyGenerationParameters
{
    private final BigInteger p;
    private final  BigInteger q;
    private final  BigInteger g;
    private final DSAKeyValidationParameters parameters;

    /**
     * Initialize DSA shared parameters.
     *
     * @param p the prime modulus P.
     * @param q the prime Q.
     * @param g the number G.
     */
    public DSAKeyGenerationParameters(BigInteger p, BigInteger q, BigInteger g)
    {
        this(p, q, g, null);
    }

    /**
     * Initialize DSA shared parameters.
     *
     * @param p the prime modulus P.
     * @param q the prime Q.
     * @param g the number G.
     * @param parameters the validation parameters.
     */
    public DSAKeyGenerationParameters(BigInteger p, BigInteger q, BigInteger g, DSAKeyValidationParameters parameters)
    {
        this.p = p;
        this.q = q;
        this.g = g;
        this.parameters = parameters;
    }

    /**
     * @return the prime modulus P.
     */
    public BigInteger getP()
    {
        return p;
    }

    /**
     * @return the prime Q.
     */
    public BigInteger getQ()
    {
        return q;
    }

    /**
     * @return the number G.
     */
    public BigInteger getG()
    {
        return g;
    }

    /**
     * @return the validation parameters.
     */
    public DSAKeyValidationParameters getValidationParameters()
    {
        return parameters;
    }
}

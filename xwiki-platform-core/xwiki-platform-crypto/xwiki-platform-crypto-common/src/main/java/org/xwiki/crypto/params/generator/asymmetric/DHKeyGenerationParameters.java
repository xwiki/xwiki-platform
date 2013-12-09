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

/**
 * DH parameters for key generation.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class DHKeyGenerationParameters implements KeyGenerationParameters
{
    private static final int DEFAULT_MINIMUM_LENGTH = 20;

    private final BigInteger p;
    private final BigInteger g;
    private final BigInteger q;
    private final int l;
    private final int m;
    private final BigInteger j;
    private final DHKeyValidationParameters parameters;

    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g)
    {
        this(p, g, null, 0);
    }

    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     * @param q the Sophie Germain prime Q.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g, BigInteger q)
    {
        this(p, g, q, 0);
    }

    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     * @param q the Sophie Germain prime Q.
     * @param l the private value length in bytes.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g, BigInteger q, int l)
    {
        this(p, g, q, getDefaultM(l), l, null, null);
    }

    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     * @param q the Sophie Germain prime Q.
     * @param m the minimum length of the private value in bytes.
     * @param l the private value length in bytes.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g, BigInteger q, int m, int l)
    {
        this(p, g, q, m, l, null, null);
    }

    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     * @param q the Sophie Germain prime Q.
     * @param j the subgroup factor J.
     * @param parameters the validation parameters.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g, BigInteger q, BigInteger j,
        DHKeyValidationParameters parameters)
    {
        this(p, g, q, DEFAULT_MINIMUM_LENGTH, 0, j, parameters);
    }


    /**
     * Initialize DH parameters.
     *
     * @param p public (prime) number P.
     * @param g the public (prime) base G.
     * @param q the Sophie Germain prime Q.
     * @param m the minimum length of the private value in bytes.
     * @param l the private value length in bytes.
     * @param j the subgroup factor J.
     * @param parameters the validation parameters.
     */
    public DHKeyGenerationParameters(BigInteger p, BigInteger g, BigInteger q, int m, int l, BigInteger j,
        DHKeyValidationParameters parameters)
    {
        this.p = p;
        this.g = g;
        this.q = q;
        this.m = m;
        this.l = l;
        this.parameters = parameters;
        this.j = j;
    }

    /**
     * @return the public (prime) number P.
     */
    public BigInteger getP()
    {
        return p;
    }

    /**
     * @return the public (prime) base G.
     */
    public BigInteger getG()
    {
        return g;
    }

    /**
     * @return the Sophie Germain prime Q.
     */
    public BigInteger getQ()
    {
        return q;
    }

    /**
     * @return the minimum length of the private value in bytes.
     */
    public int getM()
    {
        return m;
    }

    /**
     * @return the private value length in bytes - if set, zero otherwise.
     */
    public int getL()
    {
        return l;
    }

    /**
     * @return the subgroup factor j.
     */
    public BigInteger getJ()
    {
        return j;
    }

    /**
     * @return the validation parameters.
     */
    public DHKeyValidationParameters getValidationParameters()
    {
        return parameters;
    }

    private static int getDefaultM(int l)
    {
        if (l == 0) {
            return DEFAULT_MINIMUM_LENGTH;
        }

        return l < DEFAULT_MINIMUM_LENGTH ? l : DEFAULT_MINIMUM_LENGTH;
    }
}

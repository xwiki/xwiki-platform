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
import org.xwiki.crypto.params.generator.symmetric.GenericKeyGenerationParameters;

/**
 * Parameters for RSA key pair generation.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class RSAKeyGenerationParameters extends GenericKeyGenerationParameters implements KeyGenerationParameters
{
    /** Default value for the public exponent. */
    private static final BigInteger DEFAULT_PUBLIC_EXPONENT = BigInteger.valueOf(0x10001);

    /** Default certainty value for prime evaluation. */
    private static final int DEFAULT_CERTAINTY = 12;

    /** Default key strength. */
    private static final int DEFAULT_STRENGTH = 256;

    /** Current public exponent. */
    private final BigInteger publicExponent;

    /** Current certainty for prime evaluation. */
    private final int certainty;

    /**
     * Build a new instance with all defaults.
     *
     * The default key strength is {@value #DEFAULT_STRENGTH}.
     * The default public exponent is 0x10001.
     * The default certainty for prime evaluation is {@value #DEFAULT_CERTAINTY}.
     */
    public RSAKeyGenerationParameters()
    {
        this(DEFAULT_STRENGTH);
    }

    /**
     * Build a new instance with the given strength.
     *
     * The default public exponent is 0x10001.
     * The default certainty for prime evaluation is {@value #DEFAULT_CERTAINTY}.
     *
     * @param strength the strength in bits.
     */
    public RSAKeyGenerationParameters(int strength)
    {
        this(strength, DEFAULT_PUBLIC_EXPONENT, DEFAULT_CERTAINTY);
    }

    /**
     * Build a new instance with all custom parameters.
     *
     * @param strength the key strength.
     * @param publicExponent the public exponent.
     */
    public RSAKeyGenerationParameters(int strength, BigInteger publicExponent)
    {
        this(strength, publicExponent, DEFAULT_CERTAINTY);
    }

    /**
     * Build a new instance with all custom parameters.
     *
     * @param strength the key strength.
     * @param certainty certainty for prime evaluation.
     */
    public RSAKeyGenerationParameters(int strength, int certainty)
    {
        this(strength, DEFAULT_PUBLIC_EXPONENT, certainty);
    }

    /**
     * Build a new instance with all custom parameters.
     *
     * @param strength the key strength.
     * @param publicExponent the public exponent.
     * @param certainty certainty for prime evaluation.
     */
    public RSAKeyGenerationParameters(int strength, BigInteger publicExponent, int certainty)
    {
        super(strength);
        this.publicExponent = publicExponent;
        this.certainty = certainty;
    }

    /**
     * @return the public exponent.
     */
    public BigInteger getPublicExponent()
    {
        return publicExponent;
    }

    /**
     * @return the certainty for prime evaluation.
     */
    public int getCertainty()
    {
        return certainty;
    }
}

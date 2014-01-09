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

import org.xwiki.crypto.params.generator.symmetric.GenericKeyGenerationParameters;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.crypto.params.generator.KeyParametersGenerationParameters;
import org.xwiki.stability.Unstable;

/**
 * Parameters for DH key pair generation consistent for use in the MTI/A0 key agreement protocol.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class DHKeyParametersGenerationParameters extends GenericKeyGenerationParameters
                                                 implements KeyParametersGenerationParameters, KeyGenerationParameters
{
    /** Default key strength. */
    private static final int DEFAULT_STRENGTH = 96;

    /** Default certainty value for prime evaluation. */
    private static final int DEFAULT_CERTAINTY = 12;

    /** The certainty for prime evaluation. */
    private final int certainty;

    /**
     * New instance using default values.
     */
    public DHKeyParametersGenerationParameters()
    {
        this(DEFAULT_STRENGTH, DEFAULT_CERTAINTY);
    }

    /**
     * New instance specifying a given key strength.
     *
     * @param strength the strength of the key to generate in bits.
     */
    public DHKeyParametersGenerationParameters(int strength)
    {
        this(strength, DEFAULT_CERTAINTY);
    }

    /**
     * New instance specifying a given key strength.
     *
     * @param strength the strength of the key to generate in bits.
     * @param certainty the certainty of prime number.
     */
    public DHKeyParametersGenerationParameters(int strength, int certainty)
    {
        super(strength);
        this.certainty = certainty;
    }

    /**
     * @return the certainty for prime number.
     */
    public int getCertainty()
    {
        return certainty;
    }
}

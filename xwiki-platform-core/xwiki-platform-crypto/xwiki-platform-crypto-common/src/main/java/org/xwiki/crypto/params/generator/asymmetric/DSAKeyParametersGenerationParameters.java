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
 * Parameters for RSA key pair generation using either FIPS186.2 or FIPS186.3.
 * DSAParameters are generated from these and use to generated the key.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public class DSAKeyParametersGenerationParameters extends GenericKeyGenerationParameters
                                                  implements KeyParametersGenerationParameters, KeyGenerationParameters
{
    /** Default key strength. */
    private static final int DEFAULT_STRENGTH = 128;

    /** Default certainty value for prime evaluation. */
    private static final int DEFAULT_CERTAINTY = 20;

    /** True if the generated key should use FIPS 186-3. */
    private final boolean use186r3;

    /** The certainty for prime evaluation. */
    private final int certainty;

    /** Size of prime Q. */
    private final int nSize;

    /** Specific usage index. */
    private final DSAKeyValidationParameters.Usage usage;

    /** Cryptographic hash function to use for parameter generation. */
    private final String digest;

    /**
     * Parameters for FIPS186.2 using a default strength of {@value #DEFAULT_CERTAINTY},
     * and a certainty of {@value #DEFAULT_CERTAINTY}.
     */
    public DSAKeyParametersGenerationParameters()
    {
        this(DEFAULT_STRENGTH, DEFAULT_CERTAINTY);
    }

    /**
     * Parameters for FIPS186.2 using a default certainty of {@value #DEFAULT_CERTAINTY}.
     *
     * @param lsize size of the P prime number (the key strength) in bytes.
     */
    public DSAKeyParametersGenerationParameters(int lsize)
    {
        this(lsize, DEFAULT_CERTAINTY);
    }

    /**
     * Parameters for FIPS186.2.
     *
     * @param lsize size of the P prime number (the key strength) in bytes.
     * @param certainty certainty for prime evaluation.
     */
    public DSAKeyParametersGenerationParameters(int lsize, int certainty)
    {
        this(false, lsize, -1, certainty, DSAKeyValidationParameters.Usage.ANY, getDigestHint(getDefaultNsize(lsize)));
    }

    /**
     * Parameters for FIPS186.3 with any usage of the key.
     *
     * @param lsize size of the P prime number (the key strength) in bytes.
     * @param nsize size of the Q prime number in bytes.
     * @param certainty certainty for prime evaluation.
     */
    public DSAKeyParametersGenerationParameters(int lsize, int nsize, int certainty)
    {
        this(lsize, nsize, certainty, DSAKeyValidationParameters.Usage.ANY);
    }

    /**
     * Parameters for FIPS186.3.
     *
     * @param lsize size of the P prime number (the key strength) in bytes.
     * @param nsize size of the Q prime number in bytes.
     * @param certainty certainty for prime evaluation.
     * @param usage target usage (this has the effect of using verifiable canonical generation of G).
     */
    public DSAKeyParametersGenerationParameters(int lsize, int nsize, int certainty,
        DSAKeyValidationParameters.Usage usage)
    {
        this(true, lsize, nsize, certainty, usage, getDigestHint(nsize));
    }

    /**
     * Parameters for FIPS186.3.
     *
     * @param lsize size of the P prime number (the key strength) in bytes.
     * @param nsize size of the Q prime number in bytes.
     * @param certainty certainty for prime evaluation.
     * @param usage target usage (this has the effect of using verifiable canonical generation of G).
     * @param hashHint hint of the cryptographic hash (digest) to use.
     */
    public DSAKeyParametersGenerationParameters(int lsize, int nsize, int certainty,
        DSAKeyValidationParameters.Usage usage, String hashHint)
    {
        this(true, lsize, nsize, certainty, usage, hashHint);
    }

    /**
     * Private constructor.
     *
     * @param use186r3 true if FIPS 183.3 should be used.
     * @param lsize size of the P prime number (the key strength) in bytes.
     * @param nsize size of the Q prime number in bytes.
     * @param certainty certainty for prime evaluation.
     * @param usage target usage (this has the effect of using verifiable canonical generation of G).
     */
    private DSAKeyParametersGenerationParameters(boolean use186r3, int lsize, int nsize, int certainty,
        DSAKeyValidationParameters.Usage usage, String digest)
    {
        super(lsize);
        this.certainty = certainty;
        this.nSize = nsize;
        this.usage = usage;
        this.use186r3 = use186r3;
        this.digest = digest;
    }

    private static int getDefaultNsize(int lSize) {
        return lSize > 128 ? 32 : 20;
    }

    private static String getDigestHint(int nSize) {
        if (nSize <= 20) {
            return "SHA-1";
        } else if (nSize <= 28) {
            return "SHA-224";
        } else if (nSize <= 32) {
            return "SHA-256";
        } else if (nSize <= 48) {
            return "SHA-384";
        }
        return "SHA-512";
    }

    /**
     * @return true if the key should be generated according to FIPS 186.3.
     */
    public boolean use186r3() {
        return use186r3;
    }

    /**
     * @return the requested P prime size in byte.
     */
    public int getPrimePsize() {
        return getStrength();
    }

    /**
     * @return the requested Q prime size in byte.
     */
    public int getPrimeQsize() {
        return nSize;
    }

    /**
     * @return the requested certainty for prime number.
     */
    public int getCertainty() {
        return certainty;
    }

    /**
     * @return the requested usage for the key.
     */
    public DSAKeyValidationParameters.Usage getUsage() {
        return usage;
    }

    /**
     * @return the hints of the cryptographic hash to use.
     */
    public String getHashHint()
    {
        return digest;
    }
}

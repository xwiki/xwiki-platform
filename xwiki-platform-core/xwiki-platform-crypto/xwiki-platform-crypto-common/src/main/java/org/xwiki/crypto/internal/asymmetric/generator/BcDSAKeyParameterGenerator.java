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
package org.xwiki.crypto.internal.asymmetric.generator;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.params.DSAParameterGenerationParameters;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.crypto.KeyParametersGenerator;
import org.xwiki.crypto.internal.digest.factory.AbstractBcDigestFactory;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyParametersGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyValidationParameters;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.crypto.params.generator.KeyParametersGenerationParameters;

/**
 * DSA key parameters generator.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("DSA")
public class BcDSAKeyParameterGenerator implements KeyParametersGenerator
{
    @Inject
    private Provider<SecureRandom> random;

    @Inject
    private ComponentManager manager;

    @Override
    public KeyGenerationParameters generate()
    {
        return generate(new DSAKeyParametersGenerationParameters());
    }

    @Override
    public KeyGenerationParameters generate(KeyParametersGenerationParameters parameters)
    {
        if (!(parameters instanceof DSAKeyParametersGenerationParameters)) {
            throw new IllegalArgumentException("Invalid parameters for DSA key parameters generator: "
                + parameters.getClass().getName());
        }

        org.bouncycastle.crypto.params.DSAParameters dsaParams =
            getDsaParameters(random.get(), (DSAKeyParametersGenerationParameters) parameters);

        org.bouncycastle.crypto.params.DSAValidationParameters dsaValidParams = dsaParams.getValidationParameters();

        return new DSAKeyGenerationParameters(dsaParams.getP(), dsaParams.getQ(), dsaParams.getG(),
            new DSAKeyValidationParameters(dsaValidParams.getSeed(), dsaValidParams.getCounter(),
                                        getUsage(dsaValidParams.getUsageIndex())));
    }

    /**
     * Generate DSA parameters.
     *
     * Shared with the key generator to optimize key generation.
     *
     * @param params the parameters generation parameters.
     * @return shared DSA parameters for key generation.
     */
    org.bouncycastle.crypto.params.DSAParameters getDsaParameters(SecureRandom random,
        DSAKeyParametersGenerationParameters params)
    {
        DSAParametersGenerator paramGen = getGenerator(params.getHashHint());

        if (params.use186r3()) {
            DSAParameterGenerationParameters p = new DSAParameterGenerationParameters(
                params.getPrimePsize() * 8, params.getPrimeQsize() * 8,
                params.getCertainty(), random,
                getUsageIndex(params.getUsage()));
            paramGen.init(p);
        } else {
            paramGen.init(params.getStrength() * 8, params.getCertainty(), random);
        }

        return paramGen.generateParameters();
    }

    /**
     * Create an instance of a DSA parameter generator using the appropriate hash algorithm.
     * @param hint hint of the hash algorithm to use.
     * @return a DSA parameter generator.
     */
    private DSAParametersGenerator getGenerator(String hint) {
        if (hint == null || hint.equals("SHA-1")) {
            return new DSAParametersGenerator();
        }

        DigestFactory factory;

        try {
            factory = manager.getInstance(DigestFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Cryptographic hash (digest) algorithm not found.", e);
        }

        if (!(factory instanceof AbstractBcDigestFactory)) {
            throw new IllegalArgumentException(
                "Requested cryptographic hash algorithm is not implemented by a factory compatible with this factory."
                    + " Factory found: " + factory.getClass().getName());
        }

        return new DSAParametersGenerator(((AbstractBcDigestFactory) factory).getDigestInstance());
    }

    /**
     * Convert key usage to key usage index.
     *
     * Shared with the key generator to optimize key generation.
     *
     * @param usage a key usage.
     * @return a BC key usage index.
     */
    static int getUsageIndex(DSAKeyValidationParameters.Usage usage) {
        if (usage == DSAKeyValidationParameters.Usage.DIGITAL_SIGNATURE) {
            return DSAParameterGenerationParameters.DIGITAL_SIGNATURE_USAGE;
        } else if (usage == DSAKeyValidationParameters.Usage.KEY_ESTABLISHMENT) {
            return DSAParameterGenerationParameters.KEY_ESTABLISHMENT_USAGE;
        }
        return -1;
    }

    /**
     * Convert usage index to key usage.
     * @param usage usage index.
     * @return key usage.
     */
    private static DSAKeyValidationParameters.Usage getUsage(int usage) {
        if (usage == DSAParameterGenerationParameters.DIGITAL_SIGNATURE_USAGE) {
            return DSAKeyValidationParameters.Usage.DIGITAL_SIGNATURE;
        } else if (usage == DSAParameterGenerationParameters.KEY_ESTABLISHMENT_USAGE) {
            return DSAKeyValidationParameters.Usage.KEY_ESTABLISHMENT;
        }
        return DSAKeyValidationParameters.Usage.ANY;
    }
}

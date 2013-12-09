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

import org.bouncycastle.crypto.generators.DHParametersGenerator;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.KeyParametersGenerator;
import org.xwiki.crypto.params.generator.asymmetric.DHKeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DHKeyParametersGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DHKeyValidationParameters;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.crypto.params.generator.KeyParametersGenerationParameters;

/**
 * Diffie-Hellman key parameters generator.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("DH")
public class BcDHKeyParameterGenerator implements KeyParametersGenerator
{
    @Inject
    private Provider<SecureRandom> random;

    @Override
    public KeyGenerationParameters generate()
    {
        return generate(new DHKeyParametersGenerationParameters());
    }

    @Override
    public KeyGenerationParameters generate(KeyParametersGenerationParameters parameters)
    {
        if (!(parameters instanceof DHKeyParametersGenerationParameters)) {
            throw new IllegalArgumentException("Invalid parameters for DH key parameters generator: "
                + parameters.getClass().getName());
        }

        org.bouncycastle.crypto.params.DHParameters dhParams =
            getDhParameters(random.get(), (DHKeyParametersGenerationParameters) parameters);

        org.bouncycastle.crypto.params.DHValidationParameters dhValidParams = dhParams.getValidationParameters();

        return new DHKeyGenerationParameters(
            dhParams.getP(), dhParams.getG(), dhParams.getQ(),
            dhParams.getM() / 8, dhParams.getL() / 8,
            dhParams.getJ(),
            ((dhValidParams != null)
                ? new DHKeyValidationParameters(dhValidParams.getSeed(), dhValidParams.getCounter())
                : null)
        );
    }

    /**
     * Generate DH parameters.
     *
     * Shared with the key generator to optimize key generation.
     *
     * @param params the parameters generation parameters.
     * @return shared DSA parameters for key generation.
     */
    static org.bouncycastle.crypto.params.DHParameters getDhParameters(SecureRandom random,
        DHKeyParametersGenerationParameters params)
    {
        DHParametersGenerator paramGen = new DHParametersGenerator();

        paramGen.init(params.getStrength() * 8, params.getCertainty(), random);

        return paramGen.generateParameters();
    }
}

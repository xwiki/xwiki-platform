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

import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.KeyParametersGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyParametersGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyValidationParameters;


/**
 * Generate new RSA key pair.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("DSA")
public class BcDSAKeyPairGenerator extends AbstractBcKeyPairGenerator
{
    @Inject
    @Named("DSA")
    private AsymmetricKeyFactory factory;

    @Inject
    @Named("DSA")
    private KeyParametersGenerator parametersGenerator;

    @Inject
    private Provider<SecureRandom> random;

    @Override
    protected AsymmetricKeyFactory getFactory()
    {
        return factory;
    }

    @Override
    public AsymmetricKeyPair generate()
    {
        return generate(new DSAKeyParametersGenerationParameters());
    }

    @Override
    public AsymmetricKeyPair generate(KeyGenerationParameters parameters)
    {
        org.bouncycastle.crypto.params.DSAParameters keyGenParams;

        if (parameters instanceof DSAKeyParametersGenerationParameters) {
            keyGenParams = getDsaParameters((DSAKeyGenerationParameters)
                parametersGenerator.generate((DSAKeyParametersGenerationParameters) parameters));
        } else if (parameters instanceof DSAKeyGenerationParameters) {
            keyGenParams = getDsaParameters((DSAKeyGenerationParameters) parameters);
        } else {
            throw new IllegalArgumentException("Invalid parameters for DSA key generator: "
                + parameters.getClass().getName());
        }

        AsymmetricCipherKeyPairGenerator generator = new DSAKeyPairGenerator();
        generator.init(new org.bouncycastle.crypto.params.DSAKeyGenerationParameters(random.get(),
                                                                                     keyGenParams));

        return getKeyPair(generator.generateKeyPair());
    }

    private org.bouncycastle.crypto.params.DSAParameters getDsaParameters(DSAKeyGenerationParameters parameters) {
        DSAKeyValidationParameters dsaValidParams = parameters.getValidationParameters();

        return new org.bouncycastle.crypto.params.DSAParameters(parameters.getP(), parameters.getQ(), parameters.getG(),
            (dsaValidParams != null) ? new org.bouncycastle.crypto.params.DSAValidationParameters(
                                            dsaValidParams.getSeed(),
                                            dsaValidParams.getCounter(),
                                            BcDSAKeyParameterGenerator.getUsageIndex(dsaValidParams.getUsage()))
                                     : null);
    }
}

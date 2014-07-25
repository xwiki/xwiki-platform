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
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.RSAKeyGenerationParameters;

/**
 * Generate new RSA key pair.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
@Named("RSA")
public class BcRSAKeyPairGenerator extends AbstractBcKeyPairGenerator
{
    @Inject
    @Named("RSA")
    private AsymmetricKeyFactory factory;

    @Inject
    private Provider<SecureRandom> random;

    protected AsymmetricKeyFactory getFactory() {
        return factory;
    }

    @Override
    public AsymmetricKeyPair generate()
    {
        return generate(new RSAKeyGenerationParameters());
    }

    @Override
    public AsymmetricKeyPair generate(KeyGenerationParameters parameters)
    {
        if (!(parameters instanceof RSAKeyGenerationParameters)) {
            throw new IllegalArgumentException("Invalid parameters for RSA key generator: "
                + parameters.getClass().getName());
        }

        RSAKeyGenerationParameters params = (RSAKeyGenerationParameters) parameters;

        org.bouncycastle.crypto.params.RSAKeyGenerationParameters genParam =
            new org.bouncycastle.crypto.params.RSAKeyGenerationParameters(
                params.getPublicExponent(), random.get(), params.getStrength() * 8, params.getCertainty());

        AsymmetricCipherKeyPairGenerator generator = new RSAKeyPairGenerator();
        generator.init(genParam);

        return getKeyPair(generator.generateKeyPair());
    }
}

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
package org.xwiki.crypto.internal.symmetric.generator;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.bouncycastle.crypto.CipherKeyGenerator;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.KeyGenerator;
import org.xwiki.crypto.params.generator.symmetric.GenericKeyGenerationParameters;
import org.xwiki.crypto.params.generator.KeyGenerationParameters;

/**
 * Default key generator, not taking care of specific targeted algorithm.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
public class DefaultKeyGenerator implements KeyGenerator
{
    @Inject
    private Provider<SecureRandom> random;

    @Override
    public byte[] generate()
    {
        throw new UnsupportedOperationException("Knowing the key strength is required to generate a key.");
    }

    @Override
    public byte[] generate(KeyGenerationParameters parameters)
    {
        if (!(parameters instanceof GenericKeyGenerationParameters)) {
            throw new IllegalArgumentException("Invalid parameters for generic key generator: "
                + parameters.getClass().getName());
        }

        GenericKeyGenerationParameters params = (GenericKeyGenerationParameters) parameters;

        CipherKeyGenerator generator = getKeyGenerator();
        generator.init(new org.bouncycastle.crypto.KeyGenerationParameters(random.get(),
            params.getStrength() * 8));
        return generator.generateKey();
    }

    protected CipherKeyGenerator getKeyGenerator()
    {
        return new CipherKeyGenerator();
    }
}

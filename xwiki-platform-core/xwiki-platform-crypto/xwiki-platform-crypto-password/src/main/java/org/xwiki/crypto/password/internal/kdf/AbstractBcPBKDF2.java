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
package org.xwiki.crypto.password.internal.kdf;

import java.io.IOException;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.PBKDF2Parameters;

/**
 * Abstract base class implement key derivation function that encapsulate Bouncy Castle PBE generator.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcPBKDF2 extends AbstractBcKDF
{
    private final PBEParametersGenerator generator;
    private final PBKDF2Parameters parameters;
    private final AlgorithmIdentifier algId;

    /**
     * Construct a new derivation function based on the given generator.
     * @param generator the Bouncy Castle generator to use.
     * @param parameters the parameter for initializing the generator.
     * @param algId the algorithm identifier of the pseudo random function used for this key derivation function.
     */
    public AbstractBcPBKDF2(PBEParametersGenerator generator, PBKDF2Parameters parameters, AlgorithmIdentifier algId)
    {
        this.generator = generator;
        this.parameters = parameters;
        this.algId = algId;
    }

    @Override
    public KeyDerivationFunctionParameters getParameters()
    {
        return parameters;
    }

    /**
     * @return the algorithm identifier of the pseudo random function used for this key derivation function.
     */
    public AlgorithmIdentifier getPRFAlgorithmIdentifier()
    {
        if (parameters.getPseudoRandomFuntionHint() != null) {
            return algId;
        }
        return null;
    }

    @Override
    public org.xwiki.crypto.params.cipher.symmetric.KeyParameter derive(byte[] password)
    {
        generator.init(password, parameters.getSalt(), parameters.getIterationCount());
        KeyParameter keyParam = (KeyParameter) generator.generateDerivedParameters(getKeySize() * 8);
        return new org.xwiki.crypto.params.cipher.symmetric.KeyParameter(keyParam.getKey());
    }

    @Override
    public KeyWithIVParameters derive(byte[] password, int ivSize)
    {
        generator.init(password, parameters.getSalt(), parameters.getIterationCount());
        ParametersWithIV keyParam =
            (ParametersWithIV) generator.generateDerivedParameters(getKeySize() * 8, ivSize * 8);
        return new KeyWithIVParameters(((KeyParameter) keyParam.getParameters()).getKey(), keyParam.getIV());
    }

    @Override
    public byte[] getEncoded() throws IOException
    {
        return getKeyDerivationFunction().getEncoded();
    }
}

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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.crypto.generators.SCrypt;
import org.xwiki.crypto.params.cipher.symmetric.KeyWithIVParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.ScryptParameters;

/**
 * Scrypt key derivation function based on Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class BcScryptKDF extends AbstractBcKDF
{
    /** This OID, part of the GNU space, is not really reserved but suggested byt the IETF expired draft. */
    private static final ASN1ObjectIdentifier ALG_ID = new ASN1ObjectIdentifier("1.3.6.1.4.1.11591.4.11");

    protected final ScryptParameters parameters;

    /**
     * Construct a new SCrypt key derivation function.
     * @param parameters the parameter for initializing the generator.
     */
    public BcScryptKDF(ScryptParameters parameters)
    {
        this.parameters = parameters;
    }

    /**
     * @return an ASN.1 representation of the key derivation function parameters.
     */
    public KeyDerivationFunc getKeyDerivationFunction()
    {
        return new KeyDerivationFunc(ALG_ID,
            new ScryptKDFParams(parameters.getSalt(), parameters.getCostParameter(), parameters.getBlockSize(),
                parameters.getParallelizationParameter(), parameters.getKeySize())
        );
    }

    @Override
    public KeyDerivationFunctionParameters getParameters()
    {
        return parameters;
    }

    @Override
    public KeyParameter derive(byte[] password)
    {
        return new KeyParameter(
            SCrypt.generate(password, parameters.getSalt(), parameters.getCostParameter(),
                parameters.getBlockSize(), parameters.getParallelizationParameter(), getKeySize())
        );
    }

    @Override
    public KeyWithIVParameters derive(byte[] password, int ivSize)
    {
        int keySize = getKeySize();
        byte[] keyIV = SCrypt.generate(password, parameters.getSalt(), parameters.getCostParameter(),
            parameters.getBlockSize(), parameters.getParallelizationParameter(), keySize + ivSize);

        byte[] key = new byte[keySize];
        System.arraycopy(keyIV, 0, key, 0, keySize);

        byte[] iv = new byte[ivSize];
        System.arraycopy(keyIV, keySize, iv, 0, ivSize);

        return new KeyWithIVParameters(key, iv);
    }

    @Override
    public byte[] getEncoded() throws IOException
    {
        return getKeyDerivationFunction().getEncoded();
    }
}

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
package org.xwiki.crypto.password.internal.kdf.factory;

import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.internal.kdf.BcScryptKDF;
import org.xwiki.crypto.password.internal.kdf.ScryptKDFParams;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;
import org.xwiki.crypto.password.params.ScryptParameters;

/**
 * Implementation of the key derivation function for Scrypt using Bouncy Castle.
 *
 * Functions provided by this factory are conform with http://www.tarsnap.com/scrypt/scrypt.pdf
 * and the IETF expired draft draft-josefsson-scrypt-kdf-01.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "Scrypt", "1.3.6.1.4.1.11591.4.11" })
@Singleton
public class BcScryptKeyDerivationFunctionFactory extends AbstractBcKDFFactory
{
    /** This OID, part of the GNU space, is not really reserved but suggested byt the IETF expired draft. */
    private static final ASN1ObjectIdentifier ALG_ID = new ASN1ObjectIdentifier("1.3.6.1.4.1.11591.4.11");

    @Override
    public KeyDerivationFunction getInstance(KeyDerivationFunctionParameters params)
    {
        if (!(params instanceof ScryptParameters)) {
            throw new IllegalArgumentException("Invalid parameter used for Scrypt function: "
                + params.getClass().getName());
        }

        return new BcScryptKDF((ScryptParameters) params);
    }

    @Override
    public KeyDerivationFunction getInstance(ASN1Encodable parameters)
    {
        KeyDerivationFunc kdf = KeyDerivationFunc.getInstance(parameters);

        if (!kdf.getAlgorithm().equals(ALG_ID)) {
            throw new IllegalArgumentException("Illegal algorithm identifier for Scrypt: "
                + kdf.getAlgorithm().getId());
        }

        ScryptKDFParams params = ScryptKDFParams.getInstance(kdf.getParameters());

        return getInstance(
            new ScryptParameters((params.getKeyLength() != null) ? params.getKeyLength().intValue() : -1,
                params.getCostParameter().intValue(), params.getParallelizationParameter().intValue(),
                params.getBlockSize().intValue(), params.getSalt()));
    }
}

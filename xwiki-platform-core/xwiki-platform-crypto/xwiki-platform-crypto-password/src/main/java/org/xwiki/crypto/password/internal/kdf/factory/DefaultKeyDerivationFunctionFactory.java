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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.KeyDerivationFunctionFactory;
import org.xwiki.crypto.password.params.KeyDerivationFunctionParameters;

/**
 * Default key derivation factory to create derivation function from encoded ASN.1 identifiers and parameters.
 *
 * This factory could only create key derivation function from previously serialized function, that use the ASN.1
 * encoding standard. It delegate the creation operation to the appropriate factory. The factory is requested
 * to the component manager using the algorithm OID as hint.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
public class DefaultKeyDerivationFunctionFactory extends AbstractBcKDFFactory
{
    @Inject
    private ComponentManager manager;

    @Override
    public KeyDerivationFunction getInstance(KeyDerivationFunctionParameters params)
    {
        return getFactory(params.getAlgorithmName()).getInstance(params);
    }

    @Override
    public KeyDerivationFunction getInstance(byte[] encoded)
    {
        KeyDerivationFunc func = KeyDerivationFunc.getInstance(ASN1Sequence.getInstance(encoded));
        KeyDerivationFunctionFactory factory = getFactory(func.getAlgorithm().getId());
        KeyDerivationFunction kdf = getBcInstance(factory, func);
        if (kdf == null) {
            kdf = factory.getInstance(encoded);
        }
        return kdf;
    }

    @Override
    public KeyDerivationFunction getInstance(ASN1Encodable parameters) {
        KeyDerivationFunc func = KeyDerivationFunc.getInstance(parameters);
        return getBcInstance(getFactory(func.getAlgorithm().getId()), func);
    }

    private KeyDerivationFunction getBcInstance(KeyDerivationFunctionFactory factory, KeyDerivationFunc func)
    {
        if (factory instanceof AbstractBcKDFFactory) {
            return ((AbstractBcKDFFactory) factory).getInstance(func);
        }
        return null;
    }

    private KeyDerivationFunctionFactory getFactory(String hint)
    {
        try {
            return manager.getInstance(KeyDerivationFunctionFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Key derivation function algorithm not found.", e);
        }
    }
}

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
package org.xwiki.crypto.signer.internal.factory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;

/**
 * Default signer factory to be use for decoding existing signature algorithm identifier.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component
@Singleton
public class DefaultSignerFactory extends AbstractSignerFactory
{
    @Inject
    private ComponentManager manager;

    @Override
    public org.xwiki.crypto.signer.Signer getInstance(boolean forSigning, CipherParameters parameters)
    {
        throw new UnsupportedOperationException("This signer does not support to be created from cipher parameters");
    }

    @Override
    public Signer getInstance(boolean forSigning, CipherParameters parameters, byte[] encoded)
    {
        AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(encoded);
        return getFactory(algId.getAlgorithm().getId()).getInstance(forSigning, parameters);
    }

    protected SignerFactory getFactory(String hint)
    {
        try {
            return manager.getInstance(SignerFactory.class, hint);
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Signing algorithm not found.", e);
        }
    }
}

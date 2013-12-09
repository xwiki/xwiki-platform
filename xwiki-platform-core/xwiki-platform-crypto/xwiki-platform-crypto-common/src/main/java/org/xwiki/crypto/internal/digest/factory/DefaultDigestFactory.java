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
package org.xwiki.crypto.internal.digest.factory;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.DigestFactory;

/**
 * Default digest factory to create digest from encoded ASN.1 identifiers.
 *
 * This factory could only create digest from previously serialized digest, that use the ASN.1 encoding standard.
 * It delegate the creation operation to the appropriate factory. The factory is requested to the component manager
 * using the algorithm OID as hint.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component
@Singleton
public class DefaultDigestFactory extends AbstractBcDigestFactory
{
    private static final RuntimeException UNSUPPORTED =
        new UnsupportedOperationException("Unexpected internal function call.");

    @Inject
    private ComponentManager manager;

    @Override
    public org.bouncycastle.crypto.Digest getDigestInstance()
    {
        throw UNSUPPORTED;
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        throw UNSUPPORTED;
    }

    @Override
    public Digest getInstance()
    {
        throw new UnsupportedOperationException("Sorry, cannot get an instance without a determined algorithm.");
    }

    @Override
    public Digest getInstance(byte[] encoded)
    {
        AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(encoded);

        return getFactory(algId.getAlgorithm()).getInstance();
    }

    private DigestFactory getFactory(ASN1ObjectIdentifier algId)
    {
        try {
            return manager.getInstance(DigestFactory.class, algId.getId());
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Digest algorithm not found.", e);
        }
    }
}

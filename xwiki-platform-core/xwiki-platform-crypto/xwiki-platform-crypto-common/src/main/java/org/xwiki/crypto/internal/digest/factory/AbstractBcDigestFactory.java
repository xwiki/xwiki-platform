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

import javax.inject.Named;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.Digest;
import org.xwiki.crypto.internal.digest.BouncyCastleDigest;
import org.xwiki.crypto.params.DigestParameters;

/**
 * Abstract base class for factory creating Bouncy Castle based digest.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcDigestFactory implements BcDigestFactory
{
    @Override
    public String getDigestAlgorithmName()
    {
        String hint = null;
        Named named = this.getClass().getAnnotation(Named.class);
        if (named != null) {
            hint = named.value();
        } else {
            Component component = this.getClass().getAnnotation(Component.class);
            if (component != null && component.hints().length > 0) {
                hint = component.hints()[0];
            }
        }

        return hint;
    }

    @Override
    public int getDigestSize()
    {
        return getDigestInstance().getDigestSize();
    }

    @Override
    public Digest getInstance()
    {
        return new BouncyCastleDigest(getDigestInstance(), getAlgorithmIdentifier(), null);
    }

    @Override
    public Digest getInstance(DigestParameters parameters)
    {
        return getInstance();
    }

    @Override
    public Digest getInstance(byte[] encoded)
    {
        AlgorithmIdentifier algId = AlgorithmIdentifier.getInstance(encoded);

        if (!algId.getAlgorithm().equals(getAlgorithmIdentifier().getAlgorithm())) {
            throw new IllegalArgumentException("Invalid algorithm identifier in encoded data for this digest factory: "
                + algId.getAlgorithm().getId());
        }

        return getInstance();
    }
}

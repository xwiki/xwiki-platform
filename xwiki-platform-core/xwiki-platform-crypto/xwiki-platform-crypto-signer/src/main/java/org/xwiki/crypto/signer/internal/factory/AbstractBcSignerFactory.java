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

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.internal.BcSigner;

/**
 * Abstract base class for signer factory of Bouncy Castle based signer.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public abstract class AbstractBcSignerFactory extends AbstractSignerFactory implements BcSignerFactory
{
    /**
     * @return a new native bouncy castle instance of a signer.
     * @param parameters cipher parameters.
     */
    protected abstract org.bouncycastle.crypto.Signer getSignerInstance(AsymmetricCipherParameters parameters);

    /**
     * @return a new native bouncy castle instance of a signer.
     * @param parameters cipher parameters.
     */
    protected abstract AlgorithmIdentifier getSignerAlgorithmIdentifier(AsymmetricCipherParameters parameters);

    protected AsymmetricCipherParameters getCipherParameters(CipherParameters parameters)
    {
        if (!(parameters instanceof AsymmetricCipherParameters)) {
            throw new IllegalArgumentException("Unexpected parameters received for signer: "
                + parameters.getClass().getName());
        }

        return (AsymmetricCipherParameters) parameters;
    }

    @Override
    public Signer getInstance(boolean forSigning, CipherParameters parameters)
    {
        return new BcSigner(getSignerInstance((AsymmetricCipherParameters) parameters), forSigning,
            getBcCipherParameter(getCipherParameters(parameters)),
            getSignerAlgorithmName(), getSignerAlgorithmIdentifier((AsymmetricCipherParameters) parameters));
    }

    @Override
    public Signer getInstance(boolean forSigning, CipherParameters parameters, byte[] encoded)
    {
        return getInstance(forSigning, parameters, AlgorithmIdentifier.getInstance(encoded));
    }

    @Override
    public Signer getInstance(boolean forSigning, CipherParameters parameters, AlgorithmIdentifier algId)
    {
        if (!algId.getAlgorithm().equals(
            getSignerAlgorithmIdentifier(getCipherParameters(parameters)).getAlgorithm())) {
            throw new IllegalArgumentException("Incompatible algorithm for this signer: "
                + algId.getAlgorithm().getId());
        }

        return getInstance(forSigning, parameters);
    }

    /**
     * Convert cipher parameters to Bouncy Castle equivalent.
     *
     * @param parameters some asymmetric cipher parameters.
     * @return equivalent bouncy castle parameters.
     */
    protected org.bouncycastle.crypto.CipherParameters getBcCipherParameter(AsymmetricCipherParameters parameters)
    {
        if (parameters instanceof BcAsymmetricKeyParameters) {
            return ((BcAsymmetricKeyParameters) parameters).getParameters();
        }

        // TODO: convert parameters to compatible ones
        throw new UnsupportedOperationException("Cipher parameters are incompatible with this signer: "
            + parameters.getClass().getName());
    }
}

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

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.signers.PSSSigner;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.internal.digest.factory.AbstractBcDigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcDigestFactory;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyParameters;
import org.xwiki.crypto.signer.params.PssParameters;
import org.xwiki.crypto.signer.params.PssSignerParameters;

/**
 * Abtract base class for factories of PSS based signature processing.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public abstract class AbstractBcPssSignerFactory extends AbstractBcSignerFactory
{
    private static final String PSS_PARAMS_ERROR = "PSS signer parameters are invalid: ";

    @Inject
    private ComponentManager manager;

    protected abstract AsymmetricBlockCipher getCipherEngine();

    @Override
    protected org.bouncycastle.crypto.Signer getSignerInstance(AsymmetricCipherParameters parameters)
    {
        if (parameters instanceof AsymmetricKeyParameters) {
            return new PSSSigner(getCipherEngine(), new SHA1Digest(), 20);
        } else if (parameters instanceof PssSignerParameters) {
            PssParameters pssParams = ((PssSignerParameters) parameters).getPssParameters();
            Digest digest = getDigestFactory(pssParams.getHashAlgorithm()).getDigestInstance();

            return new PSSSigner(getCipherEngine(), digest,
                getDigestFactory(pssParams.getMaskGenAlgorithm()).getDigestInstance(),
                pssParams.getSaltLength() >= 0 ? pssParams.getSaltLength() : digest.getDigestSize(),
                pssParams.getTrailerByte());
        }

        throw new UnsupportedOperationException(PSS_PARAMS_ERROR + parameters.getClass().getName());
    }

    @Override
    protected org.bouncycastle.crypto.CipherParameters getBcCipherParameter(AsymmetricCipherParameters parameters)
    {
        AsymmetricKeyParameters keyParams = null;

        if (parameters instanceof AsymmetricKeyParameters) {
            keyParams = (AsymmetricKeyParameters) parameters;
        } else if (parameters instanceof PssSignerParameters) {
            keyParams = ((PssSignerParameters) parameters).getKeyParameters();
        }

        if (keyParams != null && keyParams instanceof BcAsymmetricKeyParameters) {
            return ((BcAsymmetricKeyParameters) keyParams).getParameters();
        }

        // TODO: convert parameters to compatible ones
        throw new UnsupportedOperationException("Cipher parameters are incompatible with this signer: "
            + parameters.getClass().getName());
    }

    protected BcDigestFactory getDigestFactory(String hint)
    {
        try {
            DigestFactory factory = manager.getInstance(DigestFactory.class, hint);

            if (!(factory instanceof BcDigestFactory)) {
                throw new IllegalArgumentException(
                    "Requested digest algorithm is not implemented by a factory compatible with this factory."
                        + " Factory found: " + factory.getClass().getName());
            }

            return (AbstractBcDigestFactory) factory;
        } catch (ComponentLookupException e) {
            throw new UnsupportedOperationException("Digest algorithm not found: " + hint, e);
        }
    }
}

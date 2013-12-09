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
package org.xwiki.crypto.cipher.internal.asymmetric.factory;

import java.security.SecureRandom;

import javax.inject.Named;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.AsymmetricCipher;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.cipher.internal.asymmetric.BcBufferedAsymmetricCipher;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.params.cipher.CipherParameters;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;

/**
 * Abstract base class for a Asymmetric Cipher Factory of Bouncy Castle cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcAsymmetricCipherFactory implements CipherFactory
{
    private static final int[] DEFAULT_SUPPORTED_KEYSIZE = new int[] {32, 48, 64, 96, 128, 256, 384, 512};

    /**
     * @return a new cipher engine instance.
     */
    protected abstract AsymmetricBlockCipher getEngineInstance();

    /**
     * @return a new initialized cipher engine.
     */
    protected abstract AsymmetricBlockCipher getCipherInstance(boolean forEncryption,
        AsymmetricCipherParameters parameters);

    /**
     * @return the random source that may be used by the cipher.
     */
    protected abstract SecureRandom getRandomSource();

    /**
     * Helper function to create supported key size arrays.
     * @param minSize minimum size supported.
     * @param maxSize maximum size supported.
     * @param step intermediate step supported.
     * @return an array of sizes.
     */
    protected static int[] newKeySizeArray(int minSize, int maxSize, int step)
    {
        int[] result = new int[((maxSize - minSize) / step) + 1];
        for (int i = minSize, j = 0; i <= maxSize; i += step, j++) {
            result[j] = i;
        }
        return result;
    }

    @Override
    public String getCipherAlgorithmName()
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
    public int getIVSize()
    {
        return -1;
    }

    @Override
    public int[] getSupportedKeySizes()
    {
        return DEFAULT_SUPPORTED_KEYSIZE;
    }

    @Override
    public int getKeySize()
    {
        int[] sizes = getSupportedKeySizes();
        return sizes[sizes.length - 1];
    }

    @Override
    public boolean isSupportedKeySize(int keySize)
    {
        int[] sizes = getSupportedKeySizes();
        for (int i : sizes) {
            if (i == keySize) {
                return true;
            }
        }
        return false;
    }

    private ParametersWithRandom toParametersWithRandom(AsymmetricCipherParameters parameters) {
        return new ParametersWithRandom(
            getBcCipherParameter(parameters),
            getRandomSource()
        );
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
        throw new UnsupportedOperationException("Cipher parameters are incompatible with this cipher: "
            + parameters.getClass().getName());
    }

    @Override
    public AsymmetricCipher getInstance(boolean forEncryption, CipherParameters parameters)
    {
        if (!(parameters instanceof AsymmetricCipherParameters)) {
            throw new IllegalArgumentException("Unexpected parameters received for a asymmetric cipher: "
                + parameters.getClass().getName());
        }
        return new BcBufferedAsymmetricCipher(getCipherInstance(forEncryption, (AsymmetricCipherParameters) parameters),
            forEncryption, toParametersWithRandom((AsymmetricCipherParameters) parameters), getCipherAlgorithmName());
    }
}


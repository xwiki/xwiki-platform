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
package org.xwiki.crypto.cipher.internal.symmetric.factory;

import javax.inject.Named;

import org.bouncycastle.crypto.BlockCipher;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.cipher.CipherFactory;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.params.cipher.symmetric.KeyParameter;

/**
 * Abstract base class for a Symmetric Cipher Factory of Bouncy Castle cipher.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcSymmetricCipherFactory implements CipherFactory
{
    /**
     * @return a new cipher engine instance.
     */
    protected abstract BlockCipher getEngineInstance();

    /**
     * @return a new initialized and wrapped cipher engine, implementing block chaining (CBC).
     */
    protected abstract BlockCipher getCipherInstance(boolean forEncryption, SymmetricCipherParameters parameters);

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
        return getEngineInstance().getBlockSize();
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

    protected org.bouncycastle.crypto.CipherParameters getBcKeyParameter(KeyParameter parameter) {
        return new org.bouncycastle.crypto.params.KeyParameter(parameter.getKey());
    }
}

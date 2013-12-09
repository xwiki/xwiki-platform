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
package org.xwiki.crypto.password.internal.pbe;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.xwiki.crypto.cipher.Cipher;
import org.xwiki.crypto.params.cipher.symmetric.SymmetricCipherParameters;
import org.xwiki.crypto.password.KeyDerivationFunction;
import org.xwiki.crypto.password.PasswordBasedCipher;

/**
 * Abstract base class for Password Based Cipher.
 *
 * Delegate cipher operation to the underlying cipher and provide access to function and cipher parameters.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractPBCipher implements PasswordBasedCipher
{
    private KeyDerivationFunction kdf;
    private SymmetricCipherParameters parameters;
    private Cipher cipher;

    /**
     * New PBE Cipher instance, that wrap the cipher created using the given key derivation function and
     * parameters.
     *
     * @param cipher the cipher to wrap.
     * @param kdf the key derivation function used to derive the key of this cipher.
     * @param parameters the cipher parameter used.
     */
    public AbstractPBCipher(Cipher cipher, KeyDerivationFunction kdf, SymmetricCipherParameters parameters)
    {
        this.cipher = cipher;
        this.kdf = kdf;
        this.parameters = parameters;
    }

    @Override
    public KeyDerivationFunction getKeyDerivationFunction()
    {
        return this.kdf;
    }

    @Override
    public SymmetricCipherParameters getParameters()
    {
        return this.parameters;
    }

    @Override
    public String getAlgorithmName()
    {
        return cipher.getAlgorithmName();
    }

    @Override
    public int getInputBlockSize()
    {
        return cipher.getInputBlockSize();
    }

    @Override
    public int getOutputBlockSize()
    {
        return cipher.getOutputBlockSize();
    }

    @Override
    public FilterInputStream getInputStream(InputStream is)
    {
        return cipher.getInputStream(is);
    }

    @Override
    public FilterOutputStream getOutputStream(OutputStream os)
    {
        return cipher.getOutputStream(os);
    }

    @Override
    public boolean isForEncryption()
    {
        return cipher.isForEncryption();
    }

    @Override
    public byte[] update(byte[] input)
    {
        return cipher.update(input);
    }

    @Override
    public byte[] update(byte[] input, int inputOffset, int inputLen)
    {
        return cipher.update(input, inputOffset, inputLen);
    }

    @Override
    public byte[] doFinal() throws GeneralSecurityException
    {
        return cipher.doFinal();
    }

    @Override
    public byte[] doFinal(byte[] input) throws GeneralSecurityException
    {
        return cipher.doFinal(input);
    }

    @Override
    public byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException
    {
        return cipher.doFinal(input, inputOffset, inputLen);
    }
}

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
package org.xwiki.crypto.cipher.internal.symmetric;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.io.CipherInputStream;
import org.bouncycastle.crypto.io.CipherOutputStream;
import org.xwiki.crypto.cipher.SymmetricCipher;

/**
 * Base class for Bouncy Castle symmetric ciphers.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class BcSymmetricCipher implements SymmetricCipher
{
    /** The underlying cipher. */
    protected final BufferedBlockCipher cipher;

    /** True if the cipher is initialized for encryption. */
    protected final boolean forEncryption;

    /**
     * Generic Bouncy Castle based block cipher.
     * @param cipher the block cipher to encapsulate, it will get buffered.
     * @param forEncryption true if the block cipher is setup for encryption.
     * @param parameters parameters to initialize the cipher.
     */
    BcSymmetricCipher(BlockCipher cipher, boolean forEncryption, CipherParameters parameters)
    {
        this.cipher = new BufferedBlockCipher(cipher);
        this.forEncryption = forEncryption;
        cipher.init(forEncryption, parameters);
    }

    /**
     * Generic Bouncy Castle based block cipher.
     * @param cipher the buffered block cipher to encapsulate.
     * @param forEncryption true if the block cipher is setup for encryption.
     * @param parameters parameters to initialize the cipher.
     */
    BcSymmetricCipher(BufferedBlockCipher cipher, boolean forEncryption, CipherParameters parameters)
    {
        this.cipher = cipher;
        this.forEncryption = forEncryption;
        cipher.init(forEncryption, parameters);
    }

    @Override
    public String getAlgorithmName()
    {
        return cipher.getUnderlyingCipher().getAlgorithmName();
    }

    @Override
    public int getInputBlockSize()
    {
        return cipher.getBlockSize();
    }

    @Override
    public int getOutputBlockSize()
    {
        return cipher.getBlockSize();
    }

    @Override
    public boolean isForEncryption()
    {
        return forEncryption;
    }

    @Override
    public FilterInputStream getInputStream(InputStream is)
    {
        cipher.reset();
        return new CipherInputStream(is, cipher);
    }

    @Override
    public FilterOutputStream getOutputStream(OutputStream os)
    {
        cipher.reset();
        return new CipherOutputStream(os, cipher);
    }

    @Override
    public byte[] update(byte[] input)
    {
        if (input != null) {
            return update(input, 0, input.length);
        } else {
            return update(null, 0, 0);
        }
    }

    @Override
    public byte[] update(byte[] input, int inputOffset, int inputLen)
    {
        int len = cipher.getUpdateOutputSize(inputLen);

        if (input == null || len == 0) {
            cipher.processBytes(input, inputOffset, inputLen, null, 0);
            return null;
        }


        byte[] out = new byte[len];
        return shrinkBuffer(out, cipher.processBytes(input, inputOffset, inputLen, out, 0));
    }

    @Override
    public byte[] doFinal() throws GeneralSecurityException
    {
        return doFinal(null, 0, 0);
    }

    @Override
    public byte[] doFinal(byte[] input) throws GeneralSecurityException
    {
        if (input != null) {
            return doFinal(input, 0, input.length);
        } else {
            return doFinal(null, 0, 0);
        }
    }

    @Override
    public byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException
    {
        byte[] out = new byte[cipher.getOutputSize(inputLen)];
        int len = 0;

        if (input != null && inputLen > 0) {
            len = cipher.processBytes(input, inputOffset, inputLen, out, 0);
        }

        try {
            len += cipher.doFinal(out, len);
        } catch (DataLengthException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        } catch (InvalidCipherTextException e) {
            throw new BadPaddingException(e.getMessage());
        }
        return shrinkBuffer(out, len);
    }

    /**
     * Return a buffer of {@code size} bytes containing the {@code size} first byte of {@code buffer}.
     * @param buffer the buffer.
     * @param size the size.
     * @return null if size is 0, the buffer if it has the right size, or a new truncated copy of buffer.
     */
    private byte[] shrinkBuffer(byte[] buffer, int size)
    {
        if (size == 0) {
            return null;
        }

        if (size == buffer.length) {
            return buffer;
        }

        byte[] output = new byte[size];
        System.arraycopy(buffer, 0, output, 0, size);
        return output;
    }
}

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
package org.xwiki.crypto.cipher;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.xwiki.stability.Unstable;

/**
 * Interface on a Block cipher engines ready to process data for encryption and decryption.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public interface Cipher
{
    /**
     * @return the algorithm name of the underlying cipher. The returned string has in the form algorithm/mode/padding,
     * like in "DES/CBC/PKCS5Padding".
     */
    String getAlgorithmName();

    /**
     * @return the size of input data process as a block by this cipher (in bytes).
     */
    int getInputBlockSize();

    /**
     * @return the size of encrypted data returned for a block of input data (in bytes).
     */
    int getOutputBlockSize();

    /**
     * @return true if the cipher is initialised for encryption, and false for decryption.
     */
    boolean isForEncryption();

    /**
     * Return a filtered input stream based on this cipher.
     *
     * The cipher is reset each time this function is called.
     * Any previously retrieved input stream or in progress operation get discarded.
     *
     * @param is an input stream to filter.
     * @return a filtered input stream based on this cipher.
     */
    FilterInputStream getInputStream(InputStream is);

    /**
     * Return a filtered output stream based on this cipher.
     *
     * The cipher is reset each time this function is called.
     * Any previously retrieved input stream or in progress operation get discarded.
     *
     * @param os an output stream to filter.
     * @return a filtered output stream based on this cipher.
     */
    FilterOutputStream getOutputStream(OutputStream os);

    /**
     * Continues a multiple-part encryption or decryption operation, processing another data chunk.
     * @param input the input buffer.
     * @return the new buffer with the result, or null if the underlying cipher is a block cipher and the input data
     * is too short to result in a new block.
     */
    byte[] update(byte[] input);

    /**
     * Continues a multiple-part encryption or decryption operation processing another data chunk.
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     * @return the new buffer with the result, or null if the underlying cipher is a block cipher and the input data
     * is too short to result in a new block.
     */
    byte[] update(byte[] input, int inputOffset, int inputLen);

    /**
     * Finishes a multiple-part encryption or decryption operation.
     *
     * Input data that may have been buffered during a previous update operation is processed, with padding
     * being applied. The result is stored in a new buffer.
     *
     * Upon finish, this method resets this cipher.
     *
     * @return the new buffer with the result.
     * @throws GeneralSecurityException if this encryption algorithm is unable to proceed properly.
     */
    byte[] doFinal() throws GeneralSecurityException;


    /**
     * Encrypts or decrypts data in a one shot operation, or finishes a multiple-part operation.
     *
     * The bytes in the input buffer, and any input bytes that may have been buffered during a previous update
     * operation, are processed, with padding being applied. The result is stored in a new buffer.
     *
     * Upon finish, this method resets this cipher.
     *
     * @param input the input buffer.
     * @return the new buffer with the result.
     * @throws GeneralSecurityException if this encryption algorithm is unable to proceed properly.
     */
    byte[] doFinal(byte[] input) throws GeneralSecurityException;


    /**
     * Encrypts or decrypts data in a one shot operation, or finishes a multiple-part operation.
     *
     * The first inputLen bytes in the input buffer, starting at inputOffset inclusive, and any input bytes that may
     * have been buffered during a previous update operation, are processed, with padding being applied. The result is
     * stored in a new buffer.
     *
     * Upon finish, this method resets this cipher.
     *
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     * @return the new buffer with the result.
     * @throws GeneralSecurityException if this encryption algorithm is unable to proceed properly.
     */
    byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException;
}

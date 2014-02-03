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
package org.xwiki.crypto.signer;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.xwiki.stability.Unstable;

/**
 * Interface on a signer engines ready to process data for signing or verification.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Unstable
public interface Signer
{
    /**
     * @return the algorithm name of the underlying signer.
     */
    String getAlgorithmName();

    /**
     * @return true if the signer is initialised for signing, and false for verifying.
     */
    boolean isForSigning();

    /**
     * Return a pass through input stream, all data will be accumulated for calculating signatures.
     *
     * The signer is reset each time this function is called.
     *
     * @param is an input stream to read from and pass through.
     * @return a pass through input stream.
     */
    FilterInputStream getInputStream(InputStream is);

    /**
     * Return a pass through output stream, all data will be accumulated for calculating signatures.
     *
     * The signer is reset each time this function is called.
     *
     * @return an output stream to this signer.
     */
    OutputStream getOutputStream();

    /**
     * Accumulate byte data to calculate signature.
     *
     * @param input a byte.
     */
    void update(byte input);

    /**
     * Accumulate byte array data to calculate signature.
     *
     * @param input a byte array.
     */
    void update(byte[] input);

    /**
     * Accumulate part of a byte array to calculate signature.
     *
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     */
    void update(byte[] input, int inputOffset, int inputLen);

    /**
     * Generate a signature for the accumulated data.
     *
     * The bytes that have been accumulated during a previous update or stream operations are signed.
     *
     * Upon finish, this method resets this signer.
     *
     * @return a buffer with the signature.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    byte[] generate() throws GeneralSecurityException;


    /**
     * Generate a signature for the accumulated data, including the provided buffer.
     *
     * The bytes that have been accumulated during a previous update or stream operations appended with the given
     * input buffer are signed.
     *
     * Upon finish, this method resets this signer.
     *
     * @param input the input buffer.
     * @return a buffer with the signature.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    byte[] generate(byte[] input) throws GeneralSecurityException;


    /**
     * Generate a signature for the accumulated data, including the part of the provided buffer.
     *
     * The bytes that have been accumulated during a previous update or stream operations appended with the first
     * inputLen bytes in the input buffer, starting at inputOffset inclusive, are signed.
     *
     * Upon finish, this method resets this signer.
     *
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     * @return a buffer with the signature.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    byte[] generate(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException;

    /**
     * Verify the given signature against the accumulated data.
     *
     * The bytes that have been accumulated during a previous update or stream operations are signed, and signatures
     * are compared.
     *
     * Upon finish, this method resets this signer.
     *
     * @param signature to be verified.
     * @return true if signatures are equals.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    boolean verify(byte[] signature) throws GeneralSecurityException;


    /**
     * Verify the given signature against the accumulated data, including the provided buffer.
     *
     * The bytes that have been accumulated during a previous update or stream operations appended with the given input
     * buffer are signed, and signatures are compared.
     *
     * Upon finish, this method resets this signer.
     *
     * @param signature to be verified.
     * @param input the input buffer.
     * @return a buffer with the signature.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    boolean verify(byte[] signature, byte[] input) throws GeneralSecurityException;


    /**
     * Verify the given signature against the accumulated data, including the part of the provided buffer.
     *
     * The bytes that have been accumulated during a previous update or stream operations appended with the first
     * inputLen bytes in the input buffer, starting at inputOffset inclusive, are signed, and signatures are compared.
     *
     * Upon finish, this method resets this signer.
     *
     * @param signature to be verified.
     * @param signOffset the offset in signature buffer where the signature starts.
     * @param signLen the signature length.
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     * @return a buffer with the signature.
     * @throws GeneralSecurityException if this signing algorithm is unable to proceed properly.
     */
    boolean verify(byte[] signature, int signOffset, int signLen, byte[] input, int inputOffset, int inputLen)
        throws GeneralSecurityException;

    /**
     * @return the algorithm identifier of this signer in encoded form.
     */
    byte[] getEncoded();
}

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
package org.xwiki.crypto;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.xwiki.crypto.params.DigestParameters;
import org.xwiki.stability.Unstable;

/**
 * Interface on a message Digest.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public interface Digest
{
    /**
     * @return the algorithm name of the underlying digest.
     */
    String getAlgorithmName();

    /**
     * @return the size of the digest produced by this message digest (in bytes).
     */
    int getDigestSize();

    /**
     * @return if any, return the parameters of this digest.
     */
    DigestParameters getParameters();

    /**
     * Return a filtered input stream computing byte read into a digest.
     *
     * The digest is reset each time this function is called.
     * Any previously retrieved input stream or in progress operation should therefore be discarded.
     *
     * @param is an input stream to read and calculate digest from. Call {@link #digest()} to get the digest.
     * @return a filtered input stream based calculating digest of bytes read.
     */
    FilterInputStream getInputStream(InputStream is);

    /**
     * Return an output stream to this digest.
     *
     * The digest is reset each time this function is called.
     * Any previously retrieved input stream or in progress operation should therefore be discarded.
     *
     * @return avn output stream writing to this digest.
     */
    OutputStream getOutputStream();

    /**
     * Continues a multiple-part digest operation, processing another data part.
     * @param input the input buffer.
     */
    void update(byte[] input);

    /**
     * Continues a multiple-part digest operation, processing another data part.
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     */
    void update(byte[] input, int inputOffset, int inputLen);

    /**
     * Finishes a multiple-part digest operation, and produce the resulting digest.
     *
     * Upon finishing, this method resets this digest.
     *
     * @return a new buffer with the resulting digest.
     */
    byte[] digest();


    /**
     * Finishes a multiple-part digest operation, and produce the resulting digest.
     *
     * Upon finishing, this method resets this digest.
     *
     * @param input the input buffer.
     * @return a new buffer with the resulting digest.
     */
    byte[] digest(byte[] input);


    /**
     * Finishes a multiple-part digest operation, and produce the resulting digest.
     *
     * Upon finishing, this method resets this digest.
     *
     * @param input the input buffer.
     * @param inputOffset the offset in input where the input starts.
     * @param inputLen the input length.
     * @return a new buffer with the resulting digest.
     */
    byte[] digest(byte[] input, int inputOffset, int inputLen);

    /**
     * Serialize the definition of this digest.
     *
     * This serialization could be provided to an appropriate factory (like the one that have been used to create this
     * digest) to produce an equivalent digest. The serialization contains the digest algorithm.
     * For best interoperability, the recommended encoding is ASN.1 in DER format.
     *
     * @return an encoded definition of this digest.
     * @throws IOException on error
     */
    byte[] getEncoded() throws IOException;
}

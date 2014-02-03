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
package org.xwiki.crypto.signer.internal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.SignatureException;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Signer;
import org.bouncycastle.crypto.io.SignerInputStream;
import org.bouncycastle.crypto.io.SignerOutputStream;

/**
 * Bouncy Castle based signer.
 *
 * @version $Id$
 * @since 5.4RC1
 */
public class BcSigner implements org.xwiki.crypto.signer.Signer
{
    /** The underlying signer. */
    protected final Signer signer;

    /** The underlying signer. */
    protected final String signerAlgorithm;

    /** True if the signer is initialized for signing. */
    protected final boolean forSigning;

    /** Algorithm identifier of this signer ASN.1 encoded. */
    protected AlgorithmIdentifier signerAlgorithmIdentifier;

    /**
     * Generic Bouncy Castle based signer.
     * @param signer the signer to encapsulate.
     * @param forSigning true if the signer is setup for signing.
     * @param parameters parameters to initialize the cipher.
     * @param signerAlgorithm the name of the algorithm implemented by this signer.
     * @param signerAlgId the algorithm identifier of the algorithm implemented by this signer.
     */
    public BcSigner(Signer signer, boolean forSigning, CipherParameters parameters,
        String signerAlgorithm, AlgorithmIdentifier signerAlgId)
    {
        this.signer = signer;
        this.signerAlgorithm = signerAlgorithm;
        this.forSigning = forSigning;
        this.signerAlgorithmIdentifier = signerAlgId;
        signer.init(forSigning, parameters);
    }

    @Override
    public String getAlgorithmName()
    {
        return signerAlgorithm;
    }

    /**
     * @return this signer algorithm identifier.
     */
    public AlgorithmIdentifier getSignerAlgorithmIdentifier() {
        return signerAlgorithmIdentifier;
    }

    @Override
    public boolean isForSigning()
    {
        return forSigning;
    }

    @Override
    public FilterInputStream getInputStream(InputStream is)
    {
        signer.reset();
        return new SignerInputStream(is, signer);
    }

    @Override
    public OutputStream getOutputStream()
    {
        signer.reset();
        return new SignerOutputStream(signer);
    }

    @Override
    public void update(byte input)
    {
        signer.update(input);
    }

    @Override
    public void update(byte[] input)
    {
        signer.update(input, 0, input.length);
    }

    @Override
    public void update(byte[] input, int inputOffset, int inputLen)
    {
        signer.update(input, inputOffset, inputLen);
    }

    @Override
    public byte[] generate() throws GeneralSecurityException
    {
        try {
            return signer.generateSignature();
        } catch (CryptoException e) {
            throw new SignatureException(e);
        }
    }

    @Override
    public byte[] generate(byte[] input) throws GeneralSecurityException
    {
        update(input);
        return generate();
    }

    @Override
    public byte[] generate(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException
    {
        update(input, inputOffset, inputLen);
        return generate();
    }

    @Override
    public boolean verify(byte[] signature) throws GeneralSecurityException
    {
        return signer.verifySignature(signature);
    }

    @Override
    public boolean verify(byte[] signature, byte[] input) throws GeneralSecurityException
    {
        update(input);
        return verify(signature);
    }

    @Override
    public boolean verify(byte[] signature, int signOffset, int signLen, byte[] input, int inputOffset, int inputLen)
        throws GeneralSecurityException
    {
        update(input, inputOffset, inputLen);

        if (signOffset != 0 || signLen != signature.length) {
            byte[] sign = new byte[signLen];
            System.arraycopy(signature, signOffset, sign, 0, signLen);
            return verify(sign);
        }

        return verify(signature);
    }

    @Override
    public byte[] getEncoded()
    {
        try {
            return signerAlgorithmIdentifier.getEncoded();
        } catch (IOException e) {
            // Very unlikely to happen
            return null;
        }
    }
}

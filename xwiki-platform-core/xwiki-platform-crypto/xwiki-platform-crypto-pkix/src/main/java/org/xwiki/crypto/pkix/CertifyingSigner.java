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
package org.xwiki.crypto.pkix;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;

import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.stability.Unstable;

/**
 * A signer for certifying certificate.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public final class CertifyingSigner implements Signer
{
    private final CertifiedPublicKey certifier;
    private final Signer signer;

    /**
     * Private constructor.
     *
     * @param certifier the certifier certified public key.
     * @param signer the signer initialized with the certifier private key.
     */
    private CertifyingSigner(CertifiedPublicKey certifier, Signer signer)
    {
        this.certifier = certifier;
        this.signer = signer;
    }

    /**
     * Get a certifying signer instance from the given signer factory for a given certifier.
     *
     * @param forSigning true for signing, and false for verifying.
     * @param certifier the certified key pair of the certifier.
     * @param factory a signer factory to create the signer.
     * @return a certifying signer.
     */
    public static CertifyingSigner getInstance(boolean forSigning, CertifiedKeyPair certifier, SignerFactory factory)
    {
        return new CertifyingSigner(certifier.getCertificate(),
            factory.getInstance(forSigning, certifier.getPrivateKey()));
    }

    /**
     * @return the certified public key of the certifier.
     */
    public CertifiedPublicKey getCertifier()
    {
        return certifier;
    }

    @Override
    public String getAlgorithmName()
    {
        return signer.getAlgorithmName();
    }

    @Override
    public boolean isForSigning()
    {
        return signer.isForSigning();
    }

    @Override
    public FilterInputStream getInputStream(InputStream is)
    {
        return signer.getInputStream(is);
    }

    @Override
    public OutputStream getOutputStream()
    {
        return signer.getOutputStream();
    }

    @Override
    public void update(byte input)
    {
        signer.update(input);
    }

    @Override
    public void update(byte[] input)
    {
        signer.update(input);
    }

    @Override
    public void update(byte[] input, int inputOffset, int inputLen)
    {
        signer.update(input, inputOffset, inputLen);
    }

    @Override
    public byte[] generate() throws GeneralSecurityException
    {
        return signer.generate();
    }

    @Override
    public byte[] generate(byte[] input) throws GeneralSecurityException
    {
        return signer.generate(input);
    }

    @Override
    public byte[] generate(byte[] input, int inputOffset, int inputLen) throws GeneralSecurityException
    {
        return signer.generate(input, inputOffset, inputLen);
    }

    @Override
    public boolean verify(byte[] signature) throws GeneralSecurityException
    {
        return signer.verify(signature);
    }

    @Override
    public boolean verify(byte[] signature, byte[] input) throws GeneralSecurityException
    {
        return signer.verify(signature, input);
    }

    @Override
    public boolean verify(byte[] signature, int signOffset, int signLen, byte[] input, int inputOffset, int inputLen)
        throws GeneralSecurityException
    {
        return signer.verify(signature, signOffset, signLen, input, inputOffset, inputLen);
    }

    @Override
    public byte[] getEncoded()
    {
        return signer.getEncoded();
    }
}

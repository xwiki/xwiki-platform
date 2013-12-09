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
package org.xwiki.crypto.x509.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.mozilla.PublicKeyAndChallenge;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

/**
 * Signed Public Key and Challenge parser for SPKAC request compatible with the <keygen> form control.
 *
 * @version $Id$
 * @since 5.3RC1
 */
public class SpkacRequest
{
    /** Public Key and Challenge. */
    private PublicKeyAndChallenge pkac;

    /** Signature algorithm of the whole request. */
    private String algorithmIdentifier;

    /** Signature hash of the whole request. */
    private byte[] signature;

    /**
     * Build a new SPKAC request based on the transmitted bytes.
     * @param bytes the bytes received from the browser for the keygen field.
     */
    public SpkacRequest(byte[] bytes)
    {
        ASN1Sequence spkac = null;
        try {
            spkac = (ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid SPKAC request format", e);
        }

        if (spkac.size() != 3) {
            throw new IllegalArgumentException("invalid SPKAC request size:" + spkac.size());
        }
        pkac = PublicKeyAndChallenge.getInstance(spkac.getObjectAt(0));
        algorithmIdentifier = AlgorithmIdentifier.getInstance(spkac.getObjectAt(1)).getAlgorithm().getId();
        signature = ((DERBitString) spkac.getObjectAt(2)).getBytes();
    }

    /**
     * Verify the received request against the requested challenge.
     * @param challenge challenge sent to the browser for signature verification.
     * @param provider JCA provider to use to create the public key instance and verify the signature.
     * @return true if the signature is verified successfully.
     * @throws InvalidKeySpecException if the public key specification is invalid.
     * @throws NoSuchAlgorithmException if the public key  algorithm is invalid.
     * @throws InvalidKeyException if the public key is invalid.
     * @throws SignatureException if an error occur while signing the request for signature verification.
     * @throws IOException on IO error
     */
    public boolean verify(String challenge, Provider provider)
        throws InvalidKeySpecException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException
    {
        if (!pkac.getChallenge().getString().equals(challenge))
        {
            return false;
        }
        Signature sig = Signature.getInstance(algorithmIdentifier, provider);

        PublicKey pubKey = this.getPublicKey(provider);
        sig.initVerify(pubKey);

        DERBitString pkBytes = new DERBitString(pkac);
        sig.update(pkBytes.getBytes());

        return sig.verify(this.signature);
    }

    /**
     * Return the public key embedded in the request.
     * @param provider the JCA provider to use to create the public key instance.
     * @return a public key.
     * @throws NoSuchAlgorithmException if the public key  algorithm is invalid.
     * @throws InvalidKeySpecException if the public key specification is invalid.
     * @throws IOException on IO error
     */
    public PublicKey getPublicKey(Provider provider)
        throws NoSuchAlgorithmException, InvalidKeySpecException, IOException
    {
        SubjectPublicKeyInfo subjectPKInfo = pkac.getSubjectPublicKeyInfo();

        X509EncodedKeySpec xspec = new X509EncodedKeySpec(new DERBitString(subjectPKInfo).getBytes());

        return KeyFactory.getInstance(subjectPKInfo.getAlgorithmId().getAlgorithm().getId(), provider)
            .generatePublic(xspec);
    }
}

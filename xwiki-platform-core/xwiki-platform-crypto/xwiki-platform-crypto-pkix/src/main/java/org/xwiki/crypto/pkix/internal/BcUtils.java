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
package org.xwiki.crypto.pkix.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.Certificate;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificate;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.internal.BcSigner;

/**
 * Utility class for converting values from/into Bouncy Castle equivalents.
 *
 * @version $Id$
 * @since 5.4
 */
public final class BcUtils
{
    private BcUtils()
    {
        // Utility class.
    }

    /**
     * Convert certified public key to certificate holder.
     *
     * @param cert the certified public key.
     * @return a certificate holder.
     */
    public static X509CertificateHolder getX509CertificateHolder(CertifiedPublicKey cert)
    {
        if (cert instanceof BcX509CertifiedPublicKey) {
            return ((BcX509CertifiedPublicKey) cert).getX509CertificateHolder();
        } else {
            try {
                return new X509CertificateHolder(cert.getEncoded());
            } catch (IOException e) {
                // Very unlikely
                throw new IllegalArgumentException("Invalid certified public key, unable to encode.");
            }
        }

    }

    /**
     * Convert public key parameters to asymmetric key parameter.
     *
     * @param publicKey the public key parameter to convert.
     * @return an asymmetric key parameter.
     */
    public static AsymmetricKeyParameter getAsymmetricKeyParameter(PublicKeyParameters publicKey)
    {
        if (publicKey instanceof BcAsymmetricKeyParameters) {
            return ((BcAsymmetricKeyParameters) publicKey).getParameters();
        } else {
            try {
                return PublicKeyFactory.createKey(publicKey.getEncoded());
            } catch (IOException e) {
                // Very unlikely
                throw new IllegalArgumentException("Invalid public key, unable to encode.");
            }
        }

    }

    /**
     * Convert public key parameter to subject public key info.
     *
     * @param publicKey the public key to convert.
     * @return a subject public key info.
     */
    public static SubjectPublicKeyInfo getSubjectPublicKeyInfo(PublicKeyParameters publicKey)
    {
        try {
            return SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(getAsymmetricKeyParameter(publicKey));
        } catch (IOException e) {
            // Very unlikely
            throw new IllegalArgumentException("Invalid public key, unable to get subject info.");
        }
    }

    /**
     * Build the structure of an X.509 certificate.
     *
     * @param tbsCert the to be signed structure
     * @param signature the signature
     * @return a X.509 certificate holder.
     */
    public static X509CertificateHolder getX509CertificateHolder(TBSCertificate tbsCert, byte[] signature)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(tbsCert);
        v.add(tbsCert.getSignature());
        v.add(new DERBitString(signature));

        return new X509CertificateHolder(Certificate.getInstance(new DERSequence(v)));
    }

    /**
     * Compare two algorithm identifier.
     *
     * @param id1 an algorithm identifier.
     * @param id2 another algorithm identifier.
     * @return true if both algorithm identifier are equals, false otherwise.
     */
    public static boolean isAlgorithlIdentifierEqual(AlgorithmIdentifier id1, AlgorithmIdentifier id2)
    {
        if (!id1.getAlgorithm().equals(id2.getAlgorithm()))
        {
            return false;
        }

        if (id1.getParameters() == null)
        {
            return !(id2.getParameters() != null && !id2.getParameters().equals(DERNull.INSTANCE));
        }

        if (id2.getParameters() == null)
        {
            return !(id1.getParameters() != null && !id1.getParameters().equals(DERNull.INSTANCE));
        }

        return id1.getParameters().equals(id2.getParameters());
    }


    /**
     * DER encode an ASN.1 object into the given signer and return the signer.
     *
     * @param signer a signer.
     * @param tbsObj the object to sign.
     * @return a the signer for chaining.
     * @throws java.io.IOException on encoding error.
     */
    public static Signer updateDEREncodedObject(Signer signer, ASN1Encodable tbsObj)
        throws IOException
    {
        OutputStream sOut = signer.getOutputStream();
        DEROutputStream dOut = new DEROutputStream(sOut);

        dOut.writeObject(tbsObj);

        sOut.close();

        return signer;
    }

    /**
     * Convert principal identifier to X.500 name.
     *
     * @param principal principal identifier to convert.
     * @return an X.500 name.
     */
    public static X500Name getX500Name(PrincipalIndentifier principal)
    {
        if (principal instanceof BcPrincipalIdentifier) {
            return ((BcPrincipalIdentifier) principal).getX500Name();
        } else {
            return new X500Name(principal.getName());
        }
    }

    /**
     * Get the algorithm identifier of a signer.
     *
     * @param signer the signer.
     * @return an algorithm identifier.
     */
    public static AlgorithmIdentifier getSignerAlgoritmIdentifier(Signer signer)
    {
        if (signer instanceof BcSigner) {
            return ((BcSigner) signer).getSignerAlgorithmIdentifier();
        } else {
            return AlgorithmIdentifier.getInstance(signer.getEncoded());
        }
    }
}

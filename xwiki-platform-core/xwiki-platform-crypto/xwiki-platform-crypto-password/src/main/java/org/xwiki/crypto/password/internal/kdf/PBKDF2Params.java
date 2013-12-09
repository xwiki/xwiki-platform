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
package org.xwiki.crypto.password.internal.kdf;

import java.math.BigInteger;
import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * Replace {@link org.bouncycastle.asn1.pkcs.PBKDF2Params} to support alternative Digest as defined in PKCS #5 v2.1.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class PBKDF2Params extends ASN1Object
{
    private final ASN1OctetString octStr;
    private final ASN1Integer iterationCount;
    private final ASN1Integer keyLength;
    private final AlgorithmIdentifier prf;

    /**
     * Initialize parameters without key length and an default SHA-1 pseudo random function.
     *
     * @param salt the salt.
     * @param iterationCount the iteration count.
     */
    public PBKDF2Params(byte[] salt, int iterationCount)
    {
        this(new DEROctetString(salt), new ASN1Integer(iterationCount), null, null);
    }

    /**
     * Initialize parameters without key length.
     *
     * @param salt the salt.
     * @param iterationCount the iteration count.
     * @param prf the pseudo random function identifier.
     */
    public PBKDF2Params(byte[] salt, int iterationCount, AlgorithmIdentifier prf)
    {
        this(new DEROctetString(salt), new ASN1Integer(iterationCount), null, prf);
    }

    /**
     * Initialize parameters with a default SHA-1 pseudo random function.
     *
     * @param salt the salt.
     * @param iterationCount the iteration count.
     * @param keyLength the key length.
     */
    public PBKDF2Params(byte[] salt, int iterationCount, int keyLength)
    {
        this(new DEROctetString(salt), new ASN1Integer(iterationCount), new ASN1Integer(keyLength), null);
    }

    /**
     * Initialize all parameters.
     *
     * @param salt the salt.
     * @param iterationCount the iteration count.
     * @param keyLength the key length.
     * @param prf the pseudo random function identifier.
     */
    public PBKDF2Params(byte[] salt, int iterationCount, int keyLength, AlgorithmIdentifier prf)
    {
        this(new DEROctetString(salt), new ASN1Integer(iterationCount), new ASN1Integer(keyLength), prf);
    }

    private PBKDF2Params(ASN1OctetString salt, ASN1Integer iterationCount, ASN1Integer keyLength,
        AlgorithmIdentifier prf)
    {
        this.octStr = salt;
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
        this.prf = prf;
    }

    /**
     * Build a new instance from ASN.1 sequence.
     *
     * @param seq an ASN.1 sequence corresponding to PBKDF2 parameters.
     */
    private PBKDF2Params(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();

        octStr = (ASN1OctetString) e.nextElement();
        iterationCount = (ASN1Integer) e.nextElement();

        if (e.hasMoreElements()) {
            Object obj = e.nextElement();
            if (obj instanceof ASN1Integer) {
                keyLength = (ASN1Integer) obj;
                if (e.hasMoreElements()) {
                    prf = AlgorithmIdentifier.getInstance(obj);
                } else {
                    prf = null;
                }
            } else {
                keyLength = null;
                prf = AlgorithmIdentifier.getInstance(obj);
            }
        } else {
            keyLength = null;
            prf = null;
        }
    }

    /**
     * Static factory methods to create new instance from existing instance or encoded data.
     *
     * @param obj a compatible object, either another instance or a ASN.1 sequence.
     * @return a new instance.
     */
    public static PBKDF2Params getInstance(Object obj)
    {
        if (obj instanceof PBKDF2Params) {
            return (PBKDF2Params) obj;
        }

        if (obj instanceof org.bouncycastle.asn1.pkcs.PBKDF2Params) {
            org.bouncycastle.asn1.pkcs.PBKDF2Params params = (org.bouncycastle.asn1.pkcs.PBKDF2Params) obj;
            if (params.getKeyLength() != null) {
                return new PBKDF2Params(params.getSalt(),
                    params.getIterationCount().intValue(), params.getKeyLength().intValue());
            } else {
                return new PBKDF2Params(params.getSalt(), params.getIterationCount().intValue());
            }
        }

        if (obj != null) {
            return new PBKDF2Params(ASN1Sequence.getInstance(obj));
        }

        return null;
    }

    /**
     * @return the salt.
     */
    public byte[] getSalt()
    {
        return octStr.getOctets();
    }

    /**
     * @return the iteration count.
     */
    public BigInteger getIterationCount()
    {
        return iterationCount.getValue();
    }

    /**
     * @return the key length.
     */
    public BigInteger getKeyLength()
    {
        if (keyLength != null) {
            return keyLength.getValue();
        }

        return null;
    }

    /**
     * @return the pseudo random function identifier, defaulting to SHA-1.
     */
    public AlgorithmIdentifier getPseudoRandomFunctionIdentifier()
    {
        return prf;
    }

    /**
     * @return the underlying primitive type.
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(octStr);
        v.add(iterationCount);

        if (keyLength != null) {
            v.add(keyLength);
        }

        if (prf != null) {
            v.add(prf);
        }

        return new DERSequence(v);
    }
}

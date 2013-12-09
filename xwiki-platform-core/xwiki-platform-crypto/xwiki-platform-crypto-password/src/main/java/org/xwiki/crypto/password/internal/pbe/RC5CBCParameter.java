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

import java.math.BigInteger;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;

/**
 * RC5-CBC-Pad encryption parameters as defined in rfc-2898.
 *
 * WARNING: RSA Security own the US Patent US5724428 A on this algorithm. Therefore, before expiration of this patent,
 * which will happen in november 2015, the usage of this algorithm is subject to restricted usage on the US territories.
 * RC5 is a registered trademark of RSA Security.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class RC5CBCParameter extends ASN1Object
{
    private ASN1Integer version;
    private ASN1Integer rounds;
    private ASN1Integer blockSizeInBits;
    private ASN1OctetString iv;

    /**
     * Create a new instance without the optional initialization vector.
     * @param rounds the number of "rounds" in the encryption operation between 8 and 127.
     * @param blockSizeInBits the block size in bits, may be 64 or 128.
     */
    public RC5CBCParameter(int rounds, int blockSizeInBits)
    {
        this(16, rounds, blockSizeInBits, null);
    }

    /**
     * Create a new instance with the optional initialization vector.
     * @param rounds the number of "rounds" in the encryption operation between 8 and 127.
     * @param blockSizeInBits the block size in bits, may be 64 or 128.
     * @param iv the initialization vector.
     */
    public RC5CBCParameter(int rounds, int blockSizeInBits, byte[] iv)
    {
        this(16, rounds, blockSizeInBits, iv);
    }

    /**
     * Create a new instance with the optional initialization vector and a specific parameter version.
     * @param parameterVersion the version of this parameter structure, should be v1-0 (16).
     * @param rounds the number of "rounds" in the encryption operation between 8 and 127.
     * @param blockSizeInBits the block size in bits, may be 64 or 128.
     * @param iv the initialization vector.
     */
    public RC5CBCParameter(int parameterVersion, int rounds, int blockSizeInBits, byte[] iv)
    {
        this.version = new ASN1Integer(parameterVersion);
        this.rounds = new ASN1Integer(rounds);
        this.blockSizeInBits = new ASN1Integer(blockSizeInBits);
        this.iv = (iv != null) ? new DEROctetString(iv) : null;
    }

    /**
     * Create a new instance from a ASN.1 sequence.
     * @param seq the ASN.1 sequence to parse.
     */
    private RC5CBCParameter(ASN1Sequence seq)
    {
        version = (ASN1Integer) seq.getObjectAt(0);
        rounds = (ASN1Integer) seq.getObjectAt(1);
        blockSizeInBits = (ASN1Integer) seq.getObjectAt(2);
        if (seq.size() > 3) {
            iv = (ASN1OctetString) seq.getObjectAt(3);
        }
    }

    /**
     * Get an instance from an ASN.1 object.
     * @param obj an ASN.1 object.
     * @return an instance.
     */
    public static RC5CBCParameter getInstance(Object  obj)
    {
        if (obj instanceof RC5CBCParameter) {
            return (RC5CBCParameter) obj;
        }
        if (obj != null) {
            return new RC5CBCParameter(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    /**
     * @return the version of this parameter structure.
     */
    public BigInteger getRC5ParameterVersion()
    {
        return version.getValue();
    }

    /**
     * @return the number of "rounds" in the encryption operation between 8 and 127.
     */
    public BigInteger getRounds()
    {
        return rounds.getValue();
    }

    /**
     * @return the block size in bits, may be 64 or 128.
     */
    public BigInteger getBlockSizeInBits()
    {
        return blockSizeInBits.getValue();
    }

    /**
     * @return the initialization vector.
     */
    public byte[] getIV()
    {
        if (iv == null) {
            return null;
        }
        return iv.getOctets();
    }

    /**
     * @return an ASN.1 structure representing these parameters.
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector  v = new ASN1EncodableVector();

        v.add(version);
        v.add(rounds);
        v.add(blockSizeInBits);
        if (iv != null) {
            v.add(iv);
        }

        return new DERSequence(v);
    }
}

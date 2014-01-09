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

import java.util.Enumeration;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * Replace {@link org.bouncycastle.asn1.pkcs.PBES2Parameters} to support alternative Digest as defined in PKCS #5 v2.1.
 *
 * @version $Id$
 * @since 5.4M1
 */
public class PBES2Parameters
    extends ASN1Object
    implements PKCSObjectIdentifiers
{
    private KeyDerivationFunc func;
    private EncryptionScheme scheme;

    /**
     * Initialize parameters.
     *
     * @param keyDevFunc the key derivation function definition.
     * @param encScheme the encryption scheme definition.
     */
    public PBES2Parameters(KeyDerivationFunc keyDevFunc, EncryptionScheme encScheme)
    {
        this.func = keyDevFunc;
        this.scheme = encScheme;
    }

    /**
     * Build a new instance from ASN.1 sequence.
     *
     * @param seq an ASN.1 sequence corresponding to PBES2 parameters.
     */
    private PBES2Parameters(ASN1Sequence seq)
    {
        Enumeration e = seq.getObjects();
        ASN1Sequence  funcSeq = ASN1Sequence.getInstance(((ASN1Encodable) e.nextElement()).toASN1Primitive());

        if (funcSeq.getObjectAt(0).equals(id_PBKDF2)) {
            func = new KeyDerivationFunc(id_PBKDF2, PBKDF2Params.getInstance(funcSeq.getObjectAt(1)));
        } else {
            func = KeyDerivationFunc.getInstance(funcSeq);
        }

        scheme = EncryptionScheme.getInstance(e.nextElement());
    }

    /**
     * Static factory methods to create new instance from existing instance or encoded data.
     *
     * @param obj a compatible object, either another instance or a ASN.1 sequence.
     * @return a new instance.
     */
    public static PBES2Parameters getInstance(Object obj)
    {
        if (obj instanceof PBES2Parameters) {
            return (PBES2Parameters) obj;
        }
        if (obj instanceof org.bouncycastle.asn1.pkcs.PBES2Parameters) {
            return new PBES2Parameters(((org.bouncycastle.asn1.pkcs.PBES2Parameters) obj).getKeyDerivationFunc(),
                                       ((org.bouncycastle.asn1.pkcs.PBES2Parameters) obj).getEncryptionScheme());
        }
        if (obj != null) {
            return new PBES2Parameters(ASN1Sequence.getInstance(obj));
        }
        return null;
    }

    /**
     * @return the key derivation function definition.
     */
    public KeyDerivationFunc getKeyDerivationFunc()
    {
        return func;
    }

    /**
     * @return the encryption scheme definition.
     */
    public EncryptionScheme getEncryptionScheme()
    {
        return scheme;
    }

    /**
     * @return the underlying primitive type.
     */
    public ASN1Primitive toASN1Primitive()
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(func);
        v.add(scheme);

        return new DERSequence(v);
    }
}

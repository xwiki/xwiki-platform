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
package org.xwiki.crypto.signer.internal.factory;

import javax.inject.Singleton;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.signers.RSADigestSigner;
import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricCipherParameters;

/**
 * Factory for SHA-1 Digest with RSA encryption signature processing.
 *
 * @version $Id$
 * @since 5.4RC1
 */
@Component(hints = { "MD5withRSAEncryption", "1.2.840.113549.1.1.4" })
@Singleton
public class BcMD5withRsaSignerFactory extends AbstractBcSignerFactory
{
    private static final AlgorithmIdentifier ALG_ID =
        new AlgorithmIdentifier(PKCSObjectIdentifiers.md5WithRSAEncryption, DERNull.INSTANCE);

    @Override
    protected org.bouncycastle.crypto.Signer getSignerInstance(AsymmetricCipherParameters parameters)
    {
        return new RSADigestSigner(new MD5Digest());
    }

    @Override
    protected AlgorithmIdentifier getSignerAlgorithmIdentifier(AsymmetricCipherParameters parameters)
    {
        return ALG_ID;
    }
}

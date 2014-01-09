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
package org.xwiki.crypto.internal.digest.factory;

import javax.inject.Singleton;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.xwiki.component.annotation.Component;

/**
 * Factory creating SHA-512 message digest processor.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Component(hints = { "SHA-512", "2.16.840.1.101.3.4.2.3" })
@Singleton
public class BcSHA512DigestFactory extends AbstractBcDigestFactory
{
    private static final AlgorithmIdentifier ALG_ID = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512);

    @Override
    public Digest getDigestInstance()
    {
        return new SHA512Digest();
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return ALG_ID;
    }
}

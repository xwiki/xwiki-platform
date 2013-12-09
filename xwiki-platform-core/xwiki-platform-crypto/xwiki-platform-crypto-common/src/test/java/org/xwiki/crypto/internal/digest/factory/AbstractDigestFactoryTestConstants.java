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

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;

public abstract class AbstractDigestFactoryTestConstants
{
    protected static final String TEXT = "Congress shall make no law respecting an establishment of religion, or "
        + "prohibiting the free exercise thereof; or abridging the freedom of speech, "
        + "or of the press; or the right of the people peaceably to assemble, and to "
        + "petition the Government for a redress of grievances.";

    protected static final String MD5_DIGEST = "dghMwlJG7pbUs8yZLiaLqg==";
    protected static final String SHA1_DIGEST = "R7eMZ8T04jGMYn+4t+JiU8QIf3w=";
    protected static final String SHA224_DIGEST = "F1RrAqOG50VKJNCXSb6I6COYZxgQ62qoJNqdcA==";
    protected static final String SHA256_DIGEST = "ab6bGZxULFYYPECKI9f9QfyHjsJjS+ZYPbFln7DpEGM=";
    protected static final String SHA384_DIGEST = "0LId4etoiEZSztZ4HswL1LrizsHBud962Zwk3qSJg3/4CXRfqeEiTL/rkA5G3YZh";
    protected static final String SHA512_DIGEST = "l1Kiq5RRUU7ftTEIcBDc0VOQhkBuowzB1Cf+ThbExxqmSv8zG4LWs4RvVf9uB3rk"
        + "8iCUvxKTmJR1wYJgcJD0GA==";

    protected static final AlgorithmIdentifier MD5_DIGEST_ALGO = new AlgorithmIdentifier(PKCSObjectIdentifiers.md5);
    protected static final AlgorithmIdentifier SHA1_DIGEST_ALGO
        = new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1);
    protected static final AlgorithmIdentifier SHA224_DIGEST_ALGO
        = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha224);
    protected static final AlgorithmIdentifier SHA256_DIGEST_ALGO
        = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256);
    protected static final AlgorithmIdentifier SHA384_DIGEST_ALGO
        = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha384);
    protected static final AlgorithmIdentifier SHA512_DIGEST_ALGO
        = new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512);
}

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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcMD5DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA224DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA256DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA384DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA512DigestFactory;
import org.xwiki.crypto.internal.digest.factory.DefaultDigestFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@ComponentList({Base64BinaryStringEncoder.class, BcMD5DigestFactory.class, BcSHA1DigestFactory.class,
    BcSHA224DigestFactory.class, BcSHA256DigestFactory.class, BcSHA384DigestFactory.class, BcSHA512DigestFactory.class})
public class DefaultDigestFactoryTest extends AbstractDigestFactoryTestConstants
{
    private static final byte[] BYTES = TEXT.getBytes();

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<DigestFactory> mocker =
        new MockitoComponentMockingRule(DefaultDigestFactory.class);

    private byte[] md5Digest;
    private byte[] sha1Digest;
    private byte[] sha224Digest;
    private byte[] sha256Digest;
    private byte[] sha384Digest;
    private byte[] sha512Digest;

    private DigestFactory factory;

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();

        if (md5Digest == null) {
            BinaryStringEncoder base64encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
            md5Digest = base64encoder.decode(MD5_DIGEST);
            sha1Digest = base64encoder.decode(SHA1_DIGEST);
            sha224Digest = base64encoder.decode(SHA224_DIGEST);
            sha256Digest = base64encoder.decode(SHA256_DIGEST);
            sha384Digest = base64encoder.decode(SHA384_DIGEST);
            sha512Digest = base64encoder.decode(SHA512_DIGEST);
        }
    }

    @Test
    public void testDefaultDigestFactory() throws Exception
    {
        assertThat(factory.getInstance(MD5_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(md5Digest));
        assertThat(factory.getInstance(SHA1_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(sha1Digest));
        assertThat(factory.getInstance(SHA224_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(sha224Digest));
        assertThat(factory.getInstance(SHA256_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(sha256Digest));
        assertThat(factory.getInstance(SHA384_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(sha384Digest));
        assertThat(factory.getInstance(SHA512_DIGEST_ALGO.getEncoded()).digest(BYTES), equalTo(sha512Digest));
    }
}

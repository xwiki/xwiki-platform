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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

@ComponentList({Base64BinaryStringEncoder.class, BcRSAKeyFactory.class})
public class BcDigestWithRsaSignerFactoryTest extends AbstractRsaSignerFactoryTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mocker =
        new MockitoComponentMockingRule(BcSHA1withRsaSignerFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mockerSha224 =
        new MockitoComponentMockingRule(BcSHA224withRsaSignerFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mockerSha256 =
        new MockitoComponentMockingRule(BcSHA256withRsaSignerFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mockerSha384 =
        new MockitoComponentMockingRule(BcSHA384withRsaSignerFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mockerSha512 =
        new MockitoComponentMockingRule(BcSHA512withRsaSignerFactory.class);

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<SignerFactory> mockerMD5 =
        new MockitoComponentMockingRule(BcMD5withRsaSignerFactory.class);

    @Before
    public void configure() throws Exception
    {
        setupTest(mocker);
    }

    private void test(MockitoComponentMockingRule<SignerFactory> mocker) throws Exception
    {
        testSignatureVerification(
            mocker.getComponentUnderTest().getInstance(true, privateKey),
            mocker.getComponentUnderTest().getInstance(false, publicKey)
        );
    }

    @Test
    public void testSha1SignatureVerification() throws Exception
    {
        test(mocker);
    }

    @Test
    public void testSha224SignatureVerification() throws Exception
    {
        test(mockerSha224);
    }

    @Test
    public void testSha256SignatureVerification() throws Exception
    {
        test(mockerSha256);
    }

    @Test
    public void testSha384SignatureVerification() throws Exception
    {
        test(mockerSha384);
    }

    @Test
    public void testSha512SignatureVerification() throws Exception
    {
        test(mockerSha512);
    }

    @Test
    public void testMd5SignatureVerification() throws Exception
    {
        test(mockerMD5);
    }
}

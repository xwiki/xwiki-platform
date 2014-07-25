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
package org.xwiki.crypto.internal.asymmetric.keyfactory;

import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.crypto.params.DSAKeyParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.internal.DefaultSecureRandomProvider;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.internal.encoder.Base64BinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

@ComponentList({Base64BinaryStringEncoder.class, BcDSAKeyFactory.class, DefaultSecureRandomProvider.class})
public class BcDSAKeyFactoryTest
{
    private static final String PRIVATE_KEY = "MIIBTAIBADCCASwGByqGSM44BAEwggEfAoGBANQ9Oa1j9sWAhdXNyqz8HL/bA/e"
        + "d2VrBw6TPkgMyV1Upix58RSjOHMQNrgemSGkb80dRcLqVDYbI3ObnIJh83Zx6ze"
        + "aTpvUohGLyTa0F7UY15LkbJpyz8WFJaVykH85nz3Zo6Md9Z4X95yvF1+h9qYuak"
        + "jWcHW31+pN4u3cJNg5FAhUAj986cVG9NgWgWzFVSLbB9pEPbFUCgYEAmQrZFH3M"
        + "X5CLX/5vDvxyTeeRPZHLWc0ik3GwrIJExuVrOkuFInpx0aVbuJTxrEnY2fuc/+B"
        + "yj/F56DDO31+qPu7ZxbSvvD33OOk8eFEfn+Hia3QmA+dGrhUqoMpfDf4/GBgJhn"
        + "yQtFzMddHmYB0QnS9yX1n6DOWj/CSX0PvrlMYEFwIVAIO1GUQjAddL4btiFQnhe"
        + "N4fxBTa";

    private static final String PUBLIC_KEY = "MIGSMAkGByqGSM44BAEDgYQAAoGAJvnuTm8oI/RRI2tiZHtPkvSQaA3FP4PRsVx"
        + "6z1oIGg9OAxrtSS/aiQa+HWFg7fjHlMJ30Vh0yqt7igj70jaLGyDvr3MPDyiO++"
        + "72IiGUluc6yHg6m9cQ53eeJt9i44LJfTOw1S3YMU1ST7alokSnJRTICp5WBy0m1"
        + "scwheuTo0E=";

    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<AsymmetricKeyFactory> mocker =
        new MockitoComponentMockingRule(BcDSAKeyFactory.class);

    private AsymmetricKeyFactory factory;

    private static byte[] privateKey;
    private static byte[] publicKey;

    @Before
    public void configure() throws Exception
    {
        factory = mocker.getComponentUnderTest();

        // Decode keys once for all tests.
        if (privateKey == null) {
            BinaryStringEncoder base64encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
            privateKey = base64encoder.decode(PRIVATE_KEY);
            publicKey = base64encoder.decode(PUBLIC_KEY);
        }
    }

    @Test
    public void testPrivateKeyFromPKCS8() throws Exception
    {
        PrivateKeyParameters key = factory.fromPKCS8(privateKey);

        assertThat(key, instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) key).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) key).getParameters()).getParameters().getP().bitLength(), equalTo(1024));

        assertThat(key.getEncoded(), equalTo(privateKey));
    }

    @Test
    public void testPublicKeyFromX509() throws Exception
    {
        PublicKeyParameters key = factory.fromX509(publicKey);

        assertThat(key, instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) key).getParameters(), instanceOf(DSAPublicKeyParameters.class));

        assertThat(key.getEncoded(), equalTo(publicKey));
    }

    @Test
    public void testPrivateKeyFromToKey() throws Exception
    {
        PrivateKeyParameters key1 = factory.fromPKCS8(privateKey);
        PrivateKey pk = factory.toKey(key1);
        PrivateKeyParameters key2 = factory.fromKey(pk);

        assertThat(key1, not(sameInstance(key2)));
        assertThat(key1, equalTo(key2));
    }

    @Test
    public void testPublicKeyFromToKey() throws Exception
    {
        PublicKeyParameters key1 = factory.fromX509(publicKey);
        PublicKey pk = factory.toKey(key1);
        PublicKeyParameters key2 = factory.fromKey(pk);

        assertThat(key1, not(sameInstance(key2)));
        assertThat(key1, equalTo(key2));
    }
}


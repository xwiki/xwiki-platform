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
package org.xwiki.crypto.internal.asymmetric.generator;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.inject.Provider;

import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.internal.FixedSecureRandomProvider;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcRSAKeyFactory;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.generator.asymmetric.RSAKeyGenerationParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@ComponentList({BcRSAKeyFactory.class, FixedSecureRandomProvider.class})
public class BcRSAKeyPairGeneratorTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<KeyPairGenerator> mocker =
        new MockitoComponentMockingRule(BcRSAKeyPairGenerator.class);

    private KeyPairGenerator generator;

    @Before
    public void configure() throws Exception
    {
        generator = mocker.getComponentUnderTest();

        // Reinitialize the random source
        Provider<SecureRandom> rndprov =
            mocker.getInstance(new DefaultParameterizedType(null, Provider.class, SecureRandom.class));
        if (rndprov instanceof FixedSecureRandomProvider) {
            ((FixedSecureRandomProvider) rndprov).initialize();
        }
    }

    @Test
    public void testGenerateWithoutArgument() throws Exception
    {
        AsymmetricKeyPair kp1 = generator.generate();
        AsymmetricKeyPair kp2 = generator.generate();

        assertThat(kp1, not(nullValue()));
        assertThat(kp2, not(nullValue()));
        assertThat(kp1.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp2.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp1.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp2.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters(), instanceOf(
            RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters(), instanceOf(
            RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getModulus().bitLength(), equalTo(2048));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getModulus().bitLength(), equalTo(2048));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getModulus().bitLength(), equalTo(2048));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getModulus().bitLength(), equalTo(2048));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getPublicExponent(), equalTo(BigInteger.valueOf(0x10001)));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getExponent(), equalTo(BigInteger.valueOf(0x10001)));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getPublicExponent(), equalTo(BigInteger.valueOf(0x10001)));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getExponent(), equalTo(BigInteger.valueOf(0x10001)));
    }

    @Test
    public void testGenerateWithStrengthParameter() throws Exception
    {
        AsymmetricKeyPair kp = generator.generate(new RSAKeyGenerationParameters(64));

        assertThat(kp, not(nullValue()));
        assertThat(kp.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters(), instanceOf(
            RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getModulus().bitLength(), equalTo(512));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getModulus().bitLength(), equalTo(512));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getPublicExponent(), equalTo(BigInteger.valueOf(0x10001)));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getExponent(), equalTo(BigInteger.valueOf(0x10001)));

        kp = generator.generate(new RSAKeyGenerationParameters(128));

        assertThat(kp, not(nullValue()));
        assertThat(kp.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters(), instanceOf(
            RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getModulus().bitLength(), equalTo(1024));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getModulus().bitLength(), equalTo(1024));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getPublicExponent(), equalTo(BigInteger.valueOf(0x10001)));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getExponent(), equalTo(BigInteger.valueOf(0x10001)));
    }

    @Test
    public void testGenerateWithPublicExponentParameter() throws Exception
    {
        AsymmetricKeyPair kp = generator.generate(new RSAKeyGenerationParameters(64, BigInteger.valueOf(0x11)));

        assertThat(kp, not(nullValue()));
        assertThat(kp.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters(), instanceOf(
            RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
        assertThat(((RSAPrivateCrtKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getPublicExponent(), equalTo(BigInteger.valueOf(0x11)));
        assertThat(((RSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getExponent(), equalTo(BigInteger.valueOf(0x11)));

    }

    @Test
    public void testGenerateWithCertaintyParameter() throws Exception
    {
        AsymmetricKeyPair kp = generator.generate(new RSAKeyGenerationParameters(64, 1));

        assertThat(kp, not(nullValue()));
        assertThat(kp.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters(), instanceOf(RSAPrivateCrtKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPublic()).getParameters(), instanceOf(RSAKeyParameters.class));
    }
}

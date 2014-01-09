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

import java.security.SecureRandom;

import javax.inject.Provider;

import org.bouncycastle.crypto.params.DSAKeyParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.KeyParametersGenerator;
import org.xwiki.crypto.internal.FixedSecureRandomProvider;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.internal.asymmetric.keyfactory.BcDSAKeyFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA1DigestFactory;
import org.xwiki.crypto.internal.digest.factory.BcSHA224DigestFactory;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyGenerationParameters;
import org.xwiki.crypto.params.generator.asymmetric.DSAKeyParametersGenerationParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@ComponentList({BcDSAKeyParameterGenerator.class, BcDSAKeyFactory.class, FixedSecureRandomProvider.class,
    BcSHA1DigestFactory.class, BcSHA224DigestFactory.class})
public class BcDSAKeyPairGeneratorTest
{
    @Rule
    @SuppressWarnings("unchecked")
    public final MockitoComponentMockingRule<KeyPairGenerator> mocker =
        new MockitoComponentMockingRule(BcDSAKeyPairGenerator.class);

    private KeyPairGenerator generator;
    private KeyParametersGenerator parameterGenerator;

    @Before
    public void configure() throws Exception
    {
        generator = mocker.getComponentUnderTest();
        parameterGenerator = mocker.getInstance(KeyParametersGenerator.class, "DSA");

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
        assertThat(((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters(), instanceOf(DSAPublicKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters(), instanceOf(DSAPublicKeyParameters.class));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
    }

    @Test
    public void testGenerateFIPS186_2() throws Exception
    {
        DSAKeyGenerationParameters params = (DSAKeyGenerationParameters) parameterGenerator.generate();

        assertThat(params, not(nullValue()));
        assertThat(params.getP().bitLength(), equalTo(1024));
        assertThat(params.getQ().bitLength(), equalTo(160));

        AsymmetricKeyPair kp1 = generator.generate(params);

        assertThat(kp1, not(nullValue()));
        assertThat(kp1.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp1.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters(), instanceOf(DSAPublicKeyParameters.class));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPrivate()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp1.getPublic()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));

        AsymmetricKeyPair kp2 = generator.generate(params);

        assertThat(kp2, not(nullValue()));
        assertThat(kp2.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp2.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters(), instanceOf(DSAPublicKeyParameters.class));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getParameters().getP().bitLength(), equalTo(1024));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPrivate()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp2.getPublic()).getParameters()).getParameters().getQ().bitLength(), equalTo(160));
        assertThat(kp2, not(equalTo(kp1)));
    }

    @Test
    public void testGenerateFIPS186_3() throws Exception
    {
        DSAKeyGenerationParameters params =
            (DSAKeyGenerationParameters) parameterGenerator.generate(
                new DSAKeyParametersGenerationParameters(256, 28, 1)
            );

        assertThat(params, not(nullValue()));
        assertThat(params.getP().bitLength(), equalTo(2048));
        assertThat(params.getQ().bitLength(), equalTo(224));

        AsymmetricKeyPair kp = generator.generate(params);

        assertThat(kp, not(nullValue()));
        assertThat(kp.getPrivate(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(kp.getPublic(), instanceOf(BcAsymmetricKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters(), instanceOf(DSAPrivateKeyParameters.class));
        assertThat(((BcAsymmetricKeyParameters) kp.getPublic()).getParameters(), instanceOf(DSAPublicKeyParameters.class));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getParameters().getP().bitLength(), equalTo(2048));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getParameters().getP().bitLength(), equalTo(2048));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPrivate()).getParameters()).getParameters().getQ().bitLength(), equalTo(224));
        assertThat(((DSAKeyParameters) ((BcAsymmetricKeyParameters) kp.getPublic()).getParameters()).getParameters().getQ().bitLength(), equalTo(224));
    }
}

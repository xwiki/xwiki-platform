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

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.internal.asymmetric.BcAsymmetricKeyParameters;
import org.xwiki.crypto.internal.asymmetric.BcPrivateKeyParameters;
import org.xwiki.crypto.internal.asymmetric.BcPublicKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;

/**
 * Abstract base class for asymmetric key factory using Bouncy Castle.
 *
 * @version $Id$
 * @since 5.4M1
 */
public abstract class AbstractBcKeyFactory implements AsymmetricKeyFactory, AsymmetricKeyInfoConverter
{
    private static final String PRIVATE = "private";
    private static final String PUBLIC = "public";
    private static final String CLASS_ERROR = "Expected a %s %s key, but key class is %s.";
    private static final String ALGORITHM_ERROR = "Expected a %s %s key, but key algorithm is %s.";

    /**
     * @return the appropriate BC converter to create JCA public and private key instance from key info.
     */
    protected abstract AsymmetricKeyInfoConverter getKeyInfoConverter();

    /**
     * Check the type of the key parameter against the expected type for the current factory.
     * @param key the parameters to check
     * @return null if the parameter is of the expected type, else a string representing the expected type.
     */
    protected abstract String checkKeyType(BcAsymmetricKeyParameters key);

    //
    // AsymmetricKeyFactory
    //

    @Override
    public PublicKeyParameters fromX509(byte[] encoded) throws IOException
    {
        BcPublicKeyParameters key = new BcPublicKeyParameters(PublicKeyFactory.createKey(encoded));
        String keyType = checkKeyType(key);
        if (keyType != null) {
            throw new IllegalArgumentException(String.format(CLASS_ERROR, keyType, PUBLIC,
                key.getParameters().getClass().getName()));
        }
        return key;
    }

    @Override
    public PrivateKeyParameters fromPKCS8(byte[] encoded) throws IOException
    {
        BcPrivateKeyParameters key = new BcPrivateKeyParameters(PrivateKeyFactory.createKey(encoded));
        String keyType = checkKeyType(key);
        if (keyType != null) {
            throw new IllegalArgumentException(String.format(CLASS_ERROR, keyType, PRIVATE,
                key.getParameters().getClass().getName()));
        }
        return key;
    }

    @Override
    public PublicKeyParameters fromKey(PublicKey publicKey)
    {
        try {
            BcPublicKeyParameters key = new BcPublicKeyParameters(PublicKeyFactory.createKey(publicKey.getEncoded()));
            String keyType = checkKeyType(key);
            if (keyType != null) {
                throw new IllegalArgumentException(String.format(ALGORITHM_ERROR, keyType, PUBLIC,
                    publicKey.getAlgorithm()));
            }
            return key;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid public key: " + publicKey.getClass().getName());
        }
    }

    @Override
    public PrivateKeyParameters fromKey(PrivateKey privateKey)
    {
        try {
            BcPrivateKeyParameters key =
                new BcPrivateKeyParameters(PrivateKeyFactory.createKey(privateKey.getEncoded()));
            String keyType = checkKeyType(key);
            if (keyType != null) {
                throw new IllegalArgumentException(String.format(ALGORITHM_ERROR, keyType, PRIVATE,
                    privateKey.getAlgorithm()));
            }
            return key;
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid private key: " + privateKey.getClass().getName());
        }
    }

    @Override
    public PublicKey toKey(PublicKeyParameters key)
    {
        try {
            // Optimization
            if (key instanceof BcAsymmetricKeyParameters) {
                String keyType = checkKeyType((BcAsymmetricKeyParameters) key);
                if (keyType != null) {
                    throw new IllegalArgumentException(String.format(CLASS_ERROR, keyType, PUBLIC,
                        key.getClass().getName()));
                }

                return generatePublic(
                    SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(
                        ((BcAsymmetricKeyParameters) key).getParameters()));
            }

            // Fallback
            return generatePublic(SubjectPublicKeyInfo.getInstance(key.getEncoded()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid public key parameters: " + key.getClass().getName());
        }
    }

    @Override
    public PrivateKey toKey(PrivateKeyParameters key)
    {
        try {
            // Optimization
            if (key instanceof BcAsymmetricKeyParameters) {
                String keyType = checkKeyType((BcAsymmetricKeyParameters) key);
                if (keyType != null) {
                    throw new IllegalArgumentException(String.format(CLASS_ERROR, keyType, PRIVATE,
                        key.getClass().getName()));
                }

                return generatePrivate(
                    PrivateKeyInfoFactory.createPrivateKeyInfo(
                        ((BcAsymmetricKeyParameters) key).getParameters()));
            }

            // Fallback
            return generatePrivate(PrivateKeyInfo.getInstance(key.getEncoded()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid private key parameters: " + key.getClass().getName());
        }
    }


    //
    // AsymmetricKeyInfoConverter
    //

    @Override
    public PrivateKey generatePrivate(PrivateKeyInfo privateKeyInfo) throws IOException
    {
        return getKeyInfoConverter().generatePrivate(privateKeyInfo);
    }

    @Override
    public PublicKey generatePublic(SubjectPublicKeyInfo publicKeyInfo) throws IOException
    {
        return getKeyInfoConverter().generatePublic(publicKeyInfo);
    }
}

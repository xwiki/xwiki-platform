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
package org.xwiki.crypto.pkix.params;

import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.stability.Unstable;

/**
 * A certified key pair.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public class CertifiedKeyPair
{
    private final PrivateKeyParameters privateKey;
    private final CertifiedPublicKey certificate;

    /**
     * Create a new certified key pair, associating a certificate and a private key.
     *
     * @param privateKey the private key.
     * @param certificate the certificate.
     */
    public CertifiedKeyPair(PrivateKeyParameters privateKey, CertifiedPublicKey certificate)
    {
        this.privateKey = privateKey;
        this.certificate = certificate;
    }

    /**
     * @return the certificate.
     */
    public CertifiedPublicKey getCertificate()
    {
        return certificate;
    }

    /**
     * @return the public key parameters.
     */
    public PublicKeyParameters getPublicKey()
    {
        return certificate.getPublicKeyParameters();
    }

    /**
     * @return the public key parameters.
     */
    public PrivateKeyParameters getPrivateKey()
    {
        return privateKey;
    }

    /**
     * @return a simple key pair.
     */
    public AsymmetricKeyPair getKeyPair() {
        return new AsymmetricKeyPair(getPrivateKey(), getPublicKey());
    }
}

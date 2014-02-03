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
package org.xwiki.crypto.pkix.internal;

import java.security.SecureRandom;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.pkix.CertificateGenerator;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.params.CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;

/**
 * X.509 certificate generator factory.
 *
 * @version $Id$
 * @since 5.4
 */
@Component
@Named("X509")
public class X509CertificateGeneratorFactory implements CertificateGeneratorFactory
{

    @Inject
    private SignerFactory signerFactory;

    @Inject
    private Provider<SecureRandom> randomProvider;

    @Override
    public CertificateGenerator getInstance(Signer signer, CertificateGenerationParameters parameters)
    {
        if (!(parameters instanceof X509CertificateGenerationParameters)) {
            throw new IllegalArgumentException("Invalid parameters for X.509 certificate: "
                + parameters.getClass().getName());
        }

        if (!signer.isForSigning()) {
            throw new IllegalArgumentException("Verifying signer used for signing certificates.");
        }

        X509CertificateGenerationParameters params = (X509CertificateGenerationParameters) parameters;

        switch (params.getX509Version()) {
            case V1:
                return new BcX509v1CertificateGenerator(signer, params, signerFactory, randomProvider.get());
            case V3:
                return new BcX509v3CertificateGenerator(signer, params, signerFactory, randomProvider.get());
            default:
                throw new IllegalArgumentException("Unknown X.509 certificate version: "
                    + params.getX509Version());
        }
    }
}

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
package org.xwiki.crypto.pkix.internal.extension;

import java.util.EnumSet;

import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.internal.BcUtils;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;

/**
 * X.509 extension set builder.
 *
 * @version $Id$
 * @since 5.4
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultX509ExtensionBuilder extends AbstractBcX509ExtensionBuilder
{
    @Override
    public X509ExtensionBuilder addBasicConstraints(boolean isCertificateAuthority)
    {
        return addExtension(Extension.basicConstraints, true, new BasicConstraints(isCertificateAuthority));
    }

    @Override
    public X509ExtensionBuilder addBasicConstraints(int pathLen)
    {
        return addExtension(Extension.basicConstraints, true, new BasicConstraints(pathLen));
    }

    @Override
    public X509ExtensionBuilder addKeyUsage(EnumSet<KeyUsage> usages)
    {
        return addKeyUsage(true, usages);
    }

    @Override
    public X509ExtensionBuilder addKeyUsage(boolean critical, EnumSet<KeyUsage> usages)
    {
        if (usages == null || usages.isEmpty()) {
            return this;
        }

        return addExtension(Extension.keyUsage, critical, BcExtensionUtils.getKeyUsage(usages));
    }

    @Override
    public X509ExtensionBuilder addExtendedKeyUsage(boolean critical, ExtendedKeyUsages usages)
    {
        if (usages == null || usages.isEmpty()) {
            return this;
        }

        return addExtension(Extension.extendedKeyUsage, critical,
            BcExtensionUtils.getExtendedKeyUsage(usages.getAll()));
    }

    @Override
    public X509ExtensionBuilder addAuthorityKeyIdentifier(CertifiedPublicKey issuer)
    {
        if (issuer == null) {
            return this;
        }

        return addExtension(Extension.authorityKeyIdentifier, false,
            new BcX509ExtensionUtils().createAuthorityKeyIdentifier(BcUtils.getX509CertificateHolder(issuer)));
    }

    @Override
    public X509ExtensionBuilder addAuthorityKeyIdentifier(PublicKeyParameters subject)
    {
        if (subject == null) {
            return this;
        }

        return addExtension(Extension.authorityKeyIdentifier, false,
            new BcX509ExtensionUtils().createAuthorityKeyIdentifier(BcUtils.getSubjectPublicKeyInfo(subject)));
    }

    @Override
    public X509ExtensionBuilder addSubjectKeyIdentifier(PublicKeyParameters subject)
    {
        if (subject == null) {
            return this;
        }

        return addExtension(Extension.subjectKeyIdentifier, false,
            new BcX509ExtensionUtils().createSubjectKeyIdentifier(BcUtils.getSubjectPublicKeyInfo(subject)));
    }

    @Override
    public X509ExtensionBuilder addSubjectAltName(boolean critical, X509GeneralName[] names)
    {
        if (names == null) {
            return this;
        }

        return addExtension(Extension.subjectAlternativeName, false, BcExtensionUtils.getGeneralNames(names));
    }

    @Override
    public X509ExtensionBuilder addIssuerAltName(X509GeneralName[] names)
    {
        if (names == null) {
            return this;
        }

        return addExtension(Extension.issuerAlternativeName, false, BcExtensionUtils.getGeneralNames(names));
    }
}

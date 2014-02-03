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
package org.xwiki.crypto.pkix;

import java.io.IOException;
import java.util.EnumSet;

import org.xwiki.component.annotation.Role;
import org.xwiki.crypto.params.cipher.asymmetric.PublicKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;
import org.xwiki.stability.Unstable;

/**
 * Builder to build X.509 extension set.
 *
 * @version $Id$
 * @since 5.4
 */
@Role
@Unstable
public interface X509ExtensionBuilder
{
    /**
     * Add an extension with the given oid and the passed in value to be included in the OCTET STRING associated with
     * the extension.
     *
     * @param oid OID for the extension.
     * @param critical true if critical, false otherwise.
     * @param value the ASN.1 object to be included in the extension.
     * @return this extensions builder to allow chaining.
     * @throws IOException on encoding error.
     */
    X509ExtensionBuilder addExtension(String oid, boolean critical, byte[] value) throws IOException;

    /**
     * Add all extension in an existing extension set to the currently built extension set.
     * @param extensionSet the extension set to copy.
     * @return this extensions builder to allow chaining.
     * @throws IOException on encoding error.
     */
    X509ExtensionBuilder addExtensions(X509Extensions extensionSet) throws IOException;

    /**
     * @return the final resulting X.509 extensions
     */
    X509Extensions build();

    /**
     * @return true if no extension has been ever added.
     */
    boolean isEmpty();

    /**
     * Add the BasicConstraints extension.
     *
     * @param isCertificateAuthority should be true for a CA certificate.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addBasicConstraints(boolean isCertificateAuthority);

    /**
     * Add the BasicConstraints extension for a CA with a limited path length.
     *
     * @param pathLen the maximum path len for this CA.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addBasicConstraints(int pathLen);

    /**
     * Add a critical key usage extensions.
     *
     * @param usages a set of key usage.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addKeyUsage(EnumSet<KeyUsage> usages);

    /**
     * Add a key usage extensions.
     *
     * @param critical should be true for a critical extension, false otherwise.
     * @param usages a set of key usage.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addKeyUsage(boolean critical, EnumSet<KeyUsage> usages);

    /**
     * Add a extended key usage extensions.
     *
     * @param critical should be true for a critical extension, false otherwise.
     * @param usages a set of extended key usage.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addExtendedKeyUsage(boolean critical, ExtendedKeyUsages usages);

    /**
     * Add the authority key identifier extension.
     *
     * This extension is automatically added by the certificate builder.
     *
     * @param issuer the certifierd public key of the issuer.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addAuthorityKeyIdentifier(CertifiedPublicKey issuer);

    /**
     * Add the authority key identifier extension for self signed certificates.
     *
     * This extension is automatically added by the certificate builder.
     *
     * @param issuer the public key parameters of the subject.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addAuthorityKeyIdentifier(PublicKeyParameters issuer);

    /**
     * Add the subject key identifier extension.
     *
     * This extension is automatically added by the certificate builder.
     *
     * @param subject the public key parameters of the subject.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addSubjectKeyIdentifier(PublicKeyParameters subject);

    /**
     * Add the subject alternative names extension.
     *
     * @param critical should be true if the subject field is empty, false otherwise.
     * @param names a collection of X.509 general name.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addSubjectAltName(boolean critical, X509GeneralName[] names);

    /**
     * Add the issuer alternative names extension.
     *
     * @param names a collection of X.509 general name.
     * @return this extensions builder to allow chaining.
     */
    X509ExtensionBuilder addIssuerAltName(X509GeneralName[] names);
}

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
package org.xwiki.crypto.pkix.params.x509certificate.extension;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

import org.bouncycastle.asn1.x509.Extension;
import org.xwiki.stability.Unstable;

/**
 * X509 Extensions set.
 *
 * @version $Id$
 * @since 5.4
 */
@Unstable
public interface X509Extensions
{
    /**
     * OID of KeyUsage.
     */
    String BASIC_CONSTRAINTS_OID = Extension.basicConstraints.getId();

    /**
     * OID of KeyUsage.
     */
    String KEY_USAGE_OID = Extension.keyUsage.getId();

    /**
     * OID of ExtendedKeyUsage.
     */
    String EXTENDED_KEY_USAGE_OID = Extension.extendedKeyUsage.getId();

    /**
     * OID of IssuerAltName.
     */
    String SUBJECT_ALT_NAME_OID = Extension.subjectAlternativeName.getId();

    /**
     * OID of IssuerAltName.
     */
    String ISSUER_ALT_NAME_OID = Extension.issuerAlternativeName.getId();

    /**
     * Gets the DER-encoded OCTET string for the extension value (extnValue) identified by the passed-in oid String.
     *
     * @param oid the oid to retrieve.
     * @return a DER-encoded octet string or null if this extensions is absent.
     */
    byte[] getExtensionValue(String oid);

    /**
     * Return true if the given oid has a critical extension.
     * @param oid the oid to check.
     * @return true if the given oid has a critical extension.
     */
    boolean isCritical(String oid);

    /**
     * @return the array of OID strings in this extensions set.
     */
    String[] getExtensionOID();

    /**
     * @return the array of OID strings in this extensions set marked critical.
     */
    String[] getCriticalExtensionOID();

    /**
     * @return the array of OID strings in this extensions set marked non-critical.
     */
    String[] getNonCriticalExtensionOID();

    /**
     * @return the ASN.1 encoded form of the extensions set.
     * @throws IOException on encoding error.
     */
    byte[] getEncoded() throws IOException;

    /**
     * @return true if these extensions identify a Certificate Authority.
     */
    boolean hasCertificateAuthorityBasicConstraints();

    /**
     * @return a positive integer representing the path len constraints of a Certificate Authority, or -1 if there is
     *         no such constraints.
     */
    int getBasicConstraintsPathLen();

    /**
     * @return the set of key usages authorized, or null of none has been assigned.
     */
    EnumSet<KeyUsage> getKeyUsage();

    /**
     * @return the set of extended key usages authorized, or null of none has been assigned.
     */
    ExtendedKeyUsages getExtendedKeyUsage();

    /**
     * @return the authority key identifier, or null of none has been assigned.
     */
    byte[] getAuthorityKeyIdentifier();

    /**
     * @return the subject key identifier, or null of none has been assigned.
     */
    byte[] getSubjectKeyIdentifier();

    /**
     * @return additional identities bound to the subject of the certificate.
     */
    List<X509GeneralName> getSubjectAltName();

    /**
     * @return additional identities bound to the issuer of the certificate.
     */
    List<X509GeneralName> getIssuerAltName();
}

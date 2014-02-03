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

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509Extension;
import org.xwiki.crypto.pkix.params.x509certificate.extension.ExtendedKeyUsages;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509GeneralName;

/**
 * Set of X.509 extensions.
 *
 * @version $Id$
 * @since 5.4
 */
public class BcX509Extensions implements X509Extensions
{
    private final Extensions extensions;

    /**
     * Build an extension set based on a Bouncy Castle one.
     * @param extensions a Bouncy Castle extensions set.
     */
    public BcX509Extensions(Extensions extensions)
    {
        this.extensions = extensions;
    }

    /**
     * @return the bouncy castle wrapped object.
     */
    public Extensions getExtensions()
    {
        return extensions;
    }

    @Override
    public byte[] getExtensionValue(String oid) {
        Extension ext = extensions.getExtension(new ASN1ObjectIdentifier(oid));

        if (ext == null) {
            return null;
        }

        return ext.getExtnValue().getOctets();
    }

    @Override
    public boolean isCritical(String oid) {
        Extension ext = extensions.getExtension(new ASN1ObjectIdentifier(oid));

        return ext != null && ext.isCritical();
    }

    @Override
    public String[] getExtensionOID()
    {
        List<String> oids = new ArrayList<String>();

        @SuppressWarnings("unchecked")
        Enumeration<ASN1ObjectIdentifier> extOids = extensions.oids();
        while (extOids.hasMoreElements()) {
            oids.add(extOids.nextElement().getId());
        }

        return oids.toArray(new String[oids.size()]);
    }

    @Override
    public String[] getCriticalExtensionOID()
    {
        ASN1ObjectIdentifier[] asnoids = extensions.getCriticalExtensionOIDs();
        return toStringArray(asnoids);
    }

    @Override
    public String[] getNonCriticalExtensionOID()
    {
        ASN1ObjectIdentifier[] asnoids = extensions.getNonCriticalExtensionOIDs();
        return toStringArray(asnoids);
    }

    private String[] toStringArray(ASN1ObjectIdentifier[] asnoids)
    {
        String[] oids = new String[asnoids.length];

        for (int i = 0; i < asnoids.length; i++) {
            oids[i] = asnoids[i].getId();
        }

        return oids;
    }

    @Override
    public byte[] getEncoded() throws IOException
    {
        return extensions.getEncoded();
    }

    @Override
    public boolean hasCertificateAuthorityBasicConstraints()
    {
        BasicConstraints bc = BasicConstraints.fromExtensions(extensions);

        return bc != null && bc.isCA();
    }

    @Override
    public int getBasicConstraintsPathLen()
    {
        BasicConstraints bc = BasicConstraints.fromExtensions(extensions);

        return (bc != null) ? bc.getPathLenConstraint().intValue() : -1;
    }

    @Override
    public EnumSet<KeyUsage> getKeyUsage()
    {
        return BcExtensionUtils.getSetOfKeyUsage(org.bouncycastle.asn1.x509.KeyUsage.fromExtensions(extensions));
    }

    @Override
    public ExtendedKeyUsages getExtendedKeyUsage()
    {
        return BcExtensionUtils.getExtendedKeyUsages(ExtendedKeyUsage.fromExtensions(extensions));
    }

    @Override
    public byte[] getAuthorityKeyIdentifier()
    {
        AuthorityKeyIdentifier id = AuthorityKeyIdentifier.fromExtensions(extensions);
        return (id != null) ? id.getKeyIdentifier() : null;
    }

    @Override
    public byte[] getSubjectKeyIdentifier()
    {
        SubjectKeyIdentifier id = SubjectKeyIdentifier.fromExtensions(extensions);
        return (id != null) ? id.getKeyIdentifier() : null;
    }

    @Override
    public List<X509GeneralName> getSubjectAltName()
    {
        return BcExtensionUtils.getX509GeneralNames(GeneralNames.fromExtensions(extensions,
            X509Extension.subjectAlternativeName));
    }

    @Override
    public List<X509GeneralName> getIssuerAltName()
    {
        return BcExtensionUtils.getX509GeneralNames(GeneralNames.fromExtensions(extensions,
            X509Extension.issuerAlternativeName));
    }
}

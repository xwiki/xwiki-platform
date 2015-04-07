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
package org.xwiki.crypto.store.wiki.internal;

import java.math.BigInteger;
import java.util.Collection;

import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.CertificateProvider;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.PrincipalIndentifier;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.wiki.internal.query.X509CertificateIssuerAndSerialQuery;
import org.xwiki.crypto.store.wiki.internal.query.X509CertificateKeyIdentifierQuery;
import org.xwiki.crypto.store.wiki.internal.query.X509CertificateSubjectQuery;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryManager;

/**
 * Abstract class to build xwiki based certificate provider.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class X509CertificateProvider implements CertificateProvider
{
    private final X509CertificateKeyIdentifierQuery keyIdentifierQuery;

    private final X509CertificateIssuerAndSerialQuery issuerAndSerialQuery;

    private final X509CertificateSubjectQuery subjectQuery;

    /**
     * Create a provider that implement the {@link CertificateProvider} interface.
     *
     * @param store the reference of a document or a space where the certificate should be stored.
     * @param factory a certificate factory used to convert byte arrays to certificate.
     * @param encoder a string encoder/decoder used to convert byte arrays to/from String.
     * @param queryManager the query manager used to build queries.
     * @throws CertificateStoreException on error creating required queries.
     */
    protected X509CertificateProvider(EntityReference store, CertificateFactory factory, BinaryStringEncoder encoder,
        QueryManager queryManager) throws CertificateStoreException
    {
        this.keyIdentifierQuery = new X509CertificateKeyIdentifierQuery(store, factory, encoder, queryManager);
        this.issuerAndSerialQuery = new X509CertificateIssuerAndSerialQuery(store, factory, encoder, queryManager);
        this.subjectQuery = new X509CertificateSubjectQuery(store, factory, encoder, queryManager);
    }

    @Override
    public CertifiedPublicKey getCertificate(byte[] keyIdentifier)
    {
        return this.keyIdentifierQuery.getCertificate(keyIdentifier);
    }

    @Override
    public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial)
    {
        return this.issuerAndSerialQuery.getCertificate(issuer, serial);
    }

    @Override
    public CertifiedPublicKey getCertificate(PrincipalIndentifier issuer, BigInteger serial, byte[] keyIdentifier)
    {
        CertifiedPublicKey certificate = getCertificate(keyIdentifier);
        if (certificate == null) {
            certificate = getCertificate(issuer, serial);
        }
        return certificate;
    }

    @Override
    public Collection<CertifiedPublicKey> getCertificate(PrincipalIndentifier subject)
    {
        return this.subjectQuery.getCertificates(subject);
    }
}

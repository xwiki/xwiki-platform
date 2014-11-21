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
package org.xwiki.crypto.store.wiki.internal.query;

import java.util.ArrayList;
import java.util.Collection;

import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.wiki.internal.X509CertificateWikiStore;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.QueryManager;

/**
 * Query all certificates in a certificate store.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class X509CertificateQuery extends AbstractX509StoreQuery
{
    private static final String SELECT_STATEMENT =
        "select obj." + X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE;

    private static final String FROM_STATEMENT = ", doc.object("
        + X509CertificateWikiStore.CERTIFICATECLASS_FULLNAME + ") obj";

    private final CertificateFactory factory;

    /**
     * Create a query selecting a certificate matching a given subject key identifier in a given store.
     *
     * @param store the reference of a document or a space where the certificate should be stored.
     * @param factory a certificate factory used to convert byte arrays to certificate.
     * @param encoder a string encoder/decoder used to convert byte arrays to/from String.
     * @param queryManager the query manager used to build queries.
     * @throws CertificateStoreException on error creating required queries.
     */
    public X509CertificateQuery(EntityReference store, CertificateFactory factory, BinaryStringEncoder encoder,
        QueryManager queryManager) throws CertificateStoreException
    {
        super(store, SELECT_STATEMENT, FROM_STATEMENT, "", encoder, queryManager);
        this.factory = factory;
    }

    /**
     * Get all certificates.
     *
     * @return all certificates from the store, or an empty list if none were found.
     */
    public Collection<CertifiedPublicKey> getCertificates()
    {
        Collection<CertifiedPublicKey> result = new ArrayList<CertifiedPublicKey>();
        try {
            for (String cert : this.<String>execute()) {
                try {
                    result.add(this.factory.decode(getEncoder().decode(cert)));
                } catch (Exception e) {
                    // Ignored
                }
            }
        } catch (Exception e) {
            // Ignored
        }
        return result;
    }
}

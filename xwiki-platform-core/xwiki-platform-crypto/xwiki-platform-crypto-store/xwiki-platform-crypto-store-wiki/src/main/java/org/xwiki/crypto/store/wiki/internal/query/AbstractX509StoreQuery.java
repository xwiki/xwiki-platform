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

import java.io.IOException;
import java.util.List;

import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

/**
 * Abstract class to build queries on CertificateClass in a certificate store.
 *
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractX509StoreQuery
{
    private static final String FROM_STATEMENT = " from Document doc";

    private static final String WHERE_STATEMENT = " where 1=1";

    private static final String SPACE = "space";

    private static final String WHERE_SPACE_STATEMENT = " where doc.space=:" + SPACE;

    private static final String DOCNAME = "name";

    private static final String WHERE_DOC_STATEMENT = " and doc.name=:" + DOCNAME;

    /**
     * Encoder used to encode/decode data to/from String.
     */
    private final BinaryStringEncoder encoder;

    /**
     * Wrapped query.
     */
    private Query query;

    /**
     * Create a query.
     *
     * @param store the reference of a document or a space where the certificate should be stored.
     * @param select the select statement specific to the query.
     * @param from the additional from statement specific to the query.
     * @param where the additional where statement specific to the query.
     * @param encoder a string encoder/decoder used to convert byte arrays to/from String.
     * @param queryManager the query manager used to build queries.
     * @throws CertificateStoreException on error creating required queries.
     */
    public AbstractX509StoreQuery(EntityReference store, String select, String from, String where,
        BinaryStringEncoder encoder, QueryManager queryManager) throws CertificateStoreException
    {
        EntityReference wiki = null;
        EntityType storeType = null;

        if (store != null) {
            wiki = store.extractReference(EntityType.WIKI);
            storeType = getEntityType(store);
        }

        if (wiki == null || storeType == null) {
            throw new IllegalArgumentException("Certificate could only be queried from wiki, space, or document.");
        }

        this.encoder = encoder;

        String statement = select + FROM_STATEMENT + from;

        if (storeType != EntityType.WIKI) {
            statement += WHERE_SPACE_STATEMENT;
            if (storeType == EntityType.DOCUMENT) {
                statement += WHERE_DOC_STATEMENT;
            }
        } else {
            statement += WHERE_STATEMENT;
        }

        statement += where;

        try {
            this.query = queryManager.createQuery(statement, Query.XWQL);
        } catch (QueryException e) {
            throw new CertificateStoreException(e);
        }

        this.query.setWiki(wiki.getName());

        if (storeType != EntityType.WIKI) {
            if (storeType == EntityType.DOCUMENT) {
                this.query.bindValue(DOCNAME, store.getName())
                          .bindValue(SPACE, store.getParent().getName());
            } else if (storeType == EntityType.SPACE) {
                this.query.bindValue(SPACE, store.getName());
            }
        }
    }

    private EntityType getEntityType(EntityReference store)
    {
        if (store.getType() == EntityType.DOCUMENT && store.getParent() != null) {
            return EntityType.DOCUMENT;
        }
        if (store.getType() == EntityType.SPACE) {
            return EntityType.SPACE;
        }
        if (store.getType() == EntityType.WIKI) {
            return EntityType.WIKI;
        }
        return null;
    }

    /**
     * @return the query.
     */
    protected Query getQuery()
    {
        return this.query;
    }

    /**
     * @return the encoder.
     */
    protected BinaryStringEncoder getEncoder()
    {
        return this.encoder;
    }

    /**
     * Execute the query.
     *
     * @return the query result.
     * @throws java.io.IOException on encoding error.
     * @throws QueryException on query error.
     */
    protected <T> List<T> execute() throws IOException, QueryException
    {
        return getQuery().execute();
    }
}

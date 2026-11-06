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
package org.xwiki.store.hibernate.internal.mixin;

import org.hibernate.SQLQuery;
import org.hibernate.query.QueryProducer;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link QueryProducer} APIs restored in the patched Hibernate.
 *
 * @version $Id$
 */
@Mixin
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public interface QueryProducerMixin extends QueryProducer
{
    /**
     * Hibernate 5 {@code QueryProducer#createQuery(String)} returned a {@link org.hibernate.Query}.
     *
     * @param queryString the HQL query
     * @return the query
     */
    @BinaryOnly("createQuery")
    default org.hibernate.Query createLegacyQuery(String queryString)
    {
        return (org.hibernate.Query) createQuery(queryString);
    }

    /**
     * Hibernate 5 {@code QueryProducer#getNamedQuery(String)} returned a {@link org.hibernate.Query}.
     *
     * @param queryName the name of the query
     * @return the query
     */
    @BinaryOnly("getNamedQuery")
    default org.hibernate.Query getLegacyNamedQuery(String queryName)
    {
        return (org.hibernate.Query) getNamedQuery(queryName);
    }

    /**
     * Hibernate 5 {@code QueryProducer#createSQLQuery(String)} returned a {@link SQLQuery}.
     *
     * @param queryString the SQL query
     * @return the query
     */
    @BinaryOnly("createSQLQuery")
    default SQLQuery createLegacySQLQuery(String queryString)
    {
        return (SQLQuery) createNativeQuery(queryString);
    }

    /**
     * @param queryName the name of the query
     * @return the query
     * @deprecated use {@link #getNamedNativeQuery(String)} instead
     */
    @Deprecated
    default org.hibernate.Query getNamedSQLQuery(String queryName)
    {
        return (org.hibernate.Query) getNamedNativeQuery(queryName);
    }
}

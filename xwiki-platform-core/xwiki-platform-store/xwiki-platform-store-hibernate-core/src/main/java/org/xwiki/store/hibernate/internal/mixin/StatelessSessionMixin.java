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

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.StatelessSession;
import org.hibernate.query.NativeQuery;
import org.xwiki.store.hibernate.internal.patcher.BinaryOnly;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Restore the Hibernate 5 methods of {@link StatelessSession} which were removed or changed signature.
 *
 * @version $Id$
 */
@Mixin
@SuppressWarnings("rawtypes")
public interface StatelessSessionMixin extends StatelessSession
{
    /**
     * Hibernate 5 {@code insert} returned a {@link Serializable}.
     *
     * @param entity a new transient instance
     * @return the identifier of the instance
     */
    @BinaryOnly("insert")
    default Serializable insertSerializable(Object entity)
    {
        return (Serializable) insert(entity);
    }

    /**
     * Hibernate 5 {@code insert} returned a {@link Serializable}.
     *
     * @param entityName the entity name
     * @param entity a new transient instance
     * @return the identifier of the instance
     */
    @BinaryOnly("insert")
    default Serializable insertSerializable(String entityName, Object entity)
    {
        return (Serializable) insert(entityName, entity);
    }

    /**
     * Hibernate 5 {@code get} took a {@link Serializable} identifier.
     *
     * @param entityClass the entity class
     * @param id the identifier
     * @return the instance, or {@code null}
     */
    @BinaryOnly("get")
    default Object getSerializable(Class entityClass, Serializable id)
    {
        return get(entityClass, (Object) id);
    }

    /**
     * Hibernate 5 {@code get} took a {@link Serializable} identifier.
     *
     * @param entityClass the entity class
     * @param id the identifier
     * @param lockMode the lock mode
     * @return the instance, or {@code null}
     */
    @BinaryOnly("get")
    default Object getSerializable(Class entityClass, Serializable id, LockMode lockMode)
    {
        return get(entityClass, (Object) id, lockMode);
    }

    /**
     * Hibernate 5 {@code get} took a {@link Serializable} identifier.
     *
     * @param entityName the entity name
     * @param id the identifier
     * @return the instance, or {@code null}
     */
    @BinaryOnly("get")
    default Object getSerializable(String entityName, Serializable id)
    {
        return get(entityName, (Object) id);
    }

    /**
     * Hibernate 5 {@code get} took a {@link Serializable} identifier.
     *
     * @param entityName the entity name
     * @param id the identifier
     * @param lockMode the lock mode
     * @return the instance, or {@code null}
     */
    @BinaryOnly("get")
    default Object getSerializable(String entityName, Serializable id, LockMode lockMode)
    {
        return get(entityName, (Object) id, lockMode);
    }

    /**
     * Hibernate 5 {@code createSQLQuery} of {@link StatelessSession} returned a {@link NativeQuery}.
     *
     * @param queryString the SQL query
     * @return the query
     */
    @BinaryOnly("createSQLQuery")
    default NativeQuery createSQLQueryNativeQuery(String queryString)
    {
        return createNativeQuery(queryString);
    }
}

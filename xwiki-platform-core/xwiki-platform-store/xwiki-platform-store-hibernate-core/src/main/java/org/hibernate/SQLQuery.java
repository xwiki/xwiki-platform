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
package org.hibernate;

import org.hibernate.query.NativeQuery;
import org.hibernate.type.Type;
import org.xwiki.store.hibernate.internal.legacy.LegacyQueries;

/**
 * Hibernate 5 legacy native query type, restored so that code compiled against Hibernate 5 keeps working.
 * <p>
 * {@link NativeQuery} extends it in the patched Hibernate.
 *
 * @param <T> the query result type
 * @version $Id$
 * @since 18.10.0RC1
 * @deprecated use {@link NativeQuery} instead
 */
@Deprecated(since = "18.10.0RC1")
public interface SQLQuery<T> extends Query<T>
{
    private NativeQuery<T> nativeQuery()
    {
        return (NativeQuery<T>) this;
    }

    /**
     * @return false, callable native queries are not supported anymore
     */
    default boolean isCallable()
    {
        return false;
    }

    /**
     * @param columnAlias the column alias
     * @return this query
     */
    default SQLQuery<T> addScalar(String columnAlias)
    {
        nativeQuery().addScalar(columnAlias);

        return this;
    }

    /**
     * @param columnAlias the column alias
     * @param type the Hibernate type of the column
     * @return this query
     */
    default SQLQuery<T> addScalar(String columnAlias, Type type)
    {
        LegacyQueries.addScalar(nativeQuery(), columnAlias, type);

        return this;
    }

    /**
     * @param entityName the entity name
     * @return this query
     */
    default SQLQuery<T> addEntity(String entityName)
    {
        nativeQuery().addEntity(entityName);

        return this;
    }

    /**
     * @param tableAlias the table alias
     * @param entityName the entity name
     * @return this query
     */
    default SQLQuery<T> addEntity(String tableAlias, String entityName)
    {
        nativeQuery().addEntity(tableAlias, entityName);

        return this;
    }

    /**
     * @param entityType the entity class
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default SQLQuery<T> addEntity(Class entityType)
    {
        nativeQuery().addEntity(entityType);

        return this;
    }

    /**
     * @param tableAlias the table alias
     * @param entityType the entity class
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default SQLQuery<T> addEntity(String tableAlias, Class entityType)
    {
        nativeQuery().addEntity(tableAlias, entityType);

        return this;
    }

    /**
     * @param querySpace the query space
     * @return this query
     */
    default SQLQuery<T> addSynchronizedQuerySpace(String querySpace)
    {
        nativeQuery().addSynchronizedQuerySpace(querySpace);

        return this;
    }

    /**
     * @param entityName the entity name
     * @return this query
     */
    default SQLQuery<T> addSynchronizedEntityName(String entityName)
    {
        nativeQuery().addSynchronizedEntityName(entityName);

        return this;
    }

    /**
     * @param entityClass the entity class
     * @return this query
     */
    @SuppressWarnings("rawtypes")
    default SQLQuery<T> addSynchronizedEntityClass(Class entityClass)
    {
        nativeQuery().addSynchronizedEntityClass(entityClass);

        return this;
    }
}

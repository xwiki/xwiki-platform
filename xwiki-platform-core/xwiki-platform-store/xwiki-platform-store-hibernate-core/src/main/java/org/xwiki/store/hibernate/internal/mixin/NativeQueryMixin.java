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

import java.util.Collection;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.Type;
import org.xwiki.store.hibernate.internal.legacy.LegacyQueries;
import org.xwiki.store.hibernate.internal.patcher.Mixin;

/**
 * Hibernate 5 {@link NativeQuery} APIs restored in the patched Hibernate.
 * <p>
 * The type parameter must have the same name as the one of {@link NativeQuery}.
 *
 * @param <T> the query result type
 * @version $Id$
 */
@Mixin(interfaces = "Lorg/hibernate/SQLQuery<TT;>;")
@SuppressWarnings({"rawtypes", "deprecation", "removal"})
public interface NativeQueryMixin<T> extends NativeQuery<T>
{
    /**
     * @param columnAlias the column alias
     * @param type the Hibernate type of the column
     * @return this query
     * @deprecated use {@link #addScalar(String, org.hibernate.type.BasicTypeReference)} instead
     */
    @Deprecated
    default NativeQuery<T> addScalar(String columnAlias, Type type)
    {
        LegacyQueries.addScalar(this, columnAlias, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     * @deprecated use {@link #setParameter(String, Object, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default NativeQuery<T> setParameter(String name, Object value, Type type)
    {
        LegacyQueries.setParameter(this, name, value, type);

        return this;
    }

    /**
     * @param position the parameter position
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     * @return this query
     * @deprecated use {@link #setParameter(int, Object, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default NativeQuery<T> setParameter(int position, Object value, Type type)
    {
        LegacyQueries.setParameter(this, position, value, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     * @deprecated use {@link #setParameterList(String, Collection, jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default NativeQuery<T> setParameterList(String name, Collection values, Type type)
    {
        LegacyQueries.setParameterList(this, name, values, type);

        return this;
    }

    /**
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     * @return this query
     * @deprecated use {@link #setParameterList(String, Object[], jakarta.persistence.metamodel.Type)} instead
     */
    @Deprecated
    default NativeQuery<T> setParameterList(String name, Object[] values, Type type)
    {
        LegacyQueries.setParameterList(this, name, List.of(values), type);

        return this;
    }

    /**
     * @param flushMode the flush mode
     * @return this query
     * @deprecated use {@link #setHibernateFlushMode(FlushMode)} instead
     */
    @Deprecated
    default NativeQuery<T> setFlushMode(FlushMode flushMode)
    {
        return setHibernateFlushMode(flushMode);
    }
}

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
package org.xwiki.store.hibernate.internal.legacy;

import java.util.Collection;

import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.type.Type;

/**
 * Implementation of the Hibernate 5 query APIs which are restored in the patched Hibernate and accept Hibernate
 * {@link Type}s.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class LegacyQueries
{
    private LegacyQueries()
    {
    }

    @SuppressWarnings("unchecked")
    private static jakarta.persistence.metamodel.Type<Object> toJPAType(Type type)
    {
        return type instanceof jakarta.persistence.metamodel.Type ? (jakarta.persistence.metamodel.Type<Object>) type
            : null;
    }

    /**
     * @param query the query
     * @param name the parameter name
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     */
    public static void setParameter(Query<?> query, String name, Object value, Type type)
    {
        jakarta.persistence.metamodel.Type<Object> jpaType = toJPAType(type);
        if (jpaType != null) {
            query.setParameter(name, value, jpaType);
        } else {
            query.setParameter(name, value);
        }
    }

    /**
     * @param query the query
     * @param position the parameter position
     * @param value the parameter value
     * @param type the Hibernate type of the parameter
     */
    public static void setParameter(Query<?> query, int position, Object value, Type type)
    {
        jakarta.persistence.metamodel.Type<Object> jpaType = toJPAType(type);
        if (jpaType != null) {
            query.setParameter(position, value, jpaType);
        } else {
            query.setParameter(position, value);
        }
    }

    /**
     * @param query the query
     * @param name the parameter name
     * @param values the parameter values
     * @param type the Hibernate type of the values
     */
    @SuppressWarnings("unchecked")
    public static void setParameterList(Query<?> query, String name, Collection<?> values, Type type)
    {
        jakarta.persistence.metamodel.Type<Object> jpaType = toJPAType(type);
        if (jpaType != null) {
            query.setParameterList(name, (Collection<Object>) values, jpaType);
        } else {
            query.setParameterList(name, values);
        }
    }

    /**
     * @param query the query
     * @param columnAlias the column alias
     * @param type the Hibernate type of the column
     */
    @SuppressWarnings("rawtypes")
    public static void addScalar(NativeQuery<?> query, String columnAlias, Type type)
    {
        if (type instanceof BasicDomainType basicType) {
            query.addScalar(columnAlias, basicType);
        } else {
            query.addScalar(columnAlias);
        }
    }
}

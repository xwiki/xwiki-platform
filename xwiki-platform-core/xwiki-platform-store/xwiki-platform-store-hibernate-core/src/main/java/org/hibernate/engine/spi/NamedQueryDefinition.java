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
package org.hibernate.engine.spi;

import org.hibernate.boot.query.NamedHqlQueryDefinition;

/**
 * Hibernate 5 definition of a named HQL query, restored so that the code written for Hibernate 5 keeps working (for
 * example when reading the values of {@code Configuration#getNamedQueries()}).
 * <p>
 * {@link NamedHqlQueryDefinition} extends it in the patched Hibernate (and already provides the Hibernate 5
 * {@code getName()} method). This is an interface while it was a class in
 * Hibernate 5, so only the code compiled against the patched Hibernate can use it.
 *
 * @version $Id$
 * @since 19.0.0RC1
 * @deprecated use {@link NamedHqlQueryDefinition} instead
 */
@Deprecated(since = "19.0.0RC1")
public interface NamedQueryDefinition
{
    /**
     * @return the HQL statement of the query
     * @deprecated use {@link NamedHqlQueryDefinition#getHqlString()} instead
     */
    @Deprecated(since = "19.0.0RC1")
    default String getQuery()
    {
        return ((NamedHqlQueryDefinition<?>) this).getHqlString();
    }
}

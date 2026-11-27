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
package org.hibernate.query.spi;

import org.hibernate.query.NativeQuery;

/**
 * Hibernate 5 SPI of the native queries, restored so that the code compiled against Hibernate 5 keeps working (for
 * example when calling {@code SessionImplementor#createSQLQuery(String)}).
 * <p>
 * {@link org.hibernate.query.sql.spi.NativeQueryImplementor} extends it in the patched Hibernate. Unlike Hibernate 5, it
 * does not extend {@link QueryImplementor} (which declares methods incompatible with {@link NativeQuery} ones).
 *
 * @param <T> the type of the results
 * @version $Id$
 * @since 19.0.0RC1
 * @deprecated use {@link org.hibernate.query.sql.spi.NativeQueryImplementor} instead
 */
@Deprecated(since = "19.0.0RC1")
public interface NativeQueryImplementor<T> extends NativeQuery<T>
{
}

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

import org.hibernate.boot.Metadata;
import org.hibernate.type.MappingContext;

/**
 * Hibernate 5 access to the mapping information, restored so that the code compiled against Hibernate 5 keeps working
 * (for example when calling {@code Column#getSqlType(Dialect, Mapping)} with a {@link Metadata}).
 * <p>
 * {@link Metadata} extends it in the patched Hibernate.
 *
 * @version $Id$
 * @since 19.0.0RC1
 * @deprecated use {@link MappingContext} instead
 */
@Deprecated(since = "19.0.0RC1")
public interface Mapping extends MappingContext
{
}

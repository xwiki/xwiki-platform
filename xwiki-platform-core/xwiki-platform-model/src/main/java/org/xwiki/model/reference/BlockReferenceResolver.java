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
package org.xwiki.model.reference;

import org.xwiki.component.annotation.Role;

/**
 * Resolve a Block reference from the referenced instance or another representation into a validated
 * {@link BlockReference} object.
 *
 * @param <T> the type of the representation (eg a Block)
 * @version $Id$
 * @since 6.0M1
 */
@Role
public interface BlockReferenceResolver<T>
{
    /**
     * @param blockReferenceRepresentation the referenced instance or representation of a block reference
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved block reference as an Object
     */
    BlockReference resolve(T blockReferenceRepresentation, Object... parameters);
}

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
package org.xwiki.annotation.reference;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Typed flavour of the {@code EntityReferenceSerializer&lt;String&gt;}, which adds the type in front of it in the
 * string serialization (e.g. DOCUMENT://wiki:Space.Page). <br>
 * Note that, although it performs the roughly the same function as the entity reference serializer this is a different
 * hierarchy because it's a different strategy and to make it obvious that typed serializers and resolvers should be
 * used together.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface TypedStringEntityReferenceSerializer
{
    /**
     * @param reference the reference to serialize
     * @return the resulted representation as a string
     */
    String serialize(EntityReference reference);
}

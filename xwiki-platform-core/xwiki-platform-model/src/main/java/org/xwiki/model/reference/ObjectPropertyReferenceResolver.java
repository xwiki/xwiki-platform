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

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;

/**
 * Resolve a Property reference defined in a given representation into a validated {@link ObjectPropertyReference}
 * object, i.e. with valid values and a valid hierarchy (e.g. a Property reference must have a parent which is an object
 * reference, reference values must not be null, etc).
 * 
 * @param <T> the type of the representation (e.g. a String)
 * @version $Id$
 * @since 2.3M1
 */
@Role
public interface ObjectPropertyReferenceResolver<T>
{
    /**
     * Type instance for ObjectPropertyReferenceResolver<String>.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_STRING = new DefaultParameterizedType(null, ObjectPropertyReferenceResolver.class,
        String.class);

    /**
     * Type instance for ObjectPropertyReferenceResolver<EntityReference>.
     * 
     * @since 4.0M1
     */
    ParameterizedType TYPE_REFERENCE = new DefaultParameterizedType(null, ObjectPropertyReferenceResolver.class,
        EntityReference.class);

    /**
     * @param propertyReferenceRepresentation the representation of an object reference (e.g. as a String)
     * @return the valid resolved object reference as an object
     */
    @Deprecated
    ObjectPropertyReference resolve(T propertyReferenceRepresentation);

    /**
     * @param propertyReferenceRepresentation the representation of an object reference (e.g. as a String)
     * @param parameters optional parameters. Their meaning depends on the resolver implementation
     * @return the valid resolved object reference as an object
     * @since 3.3M2
     */
    ObjectPropertyReference resolve(T propertyReferenceRepresentation, Object... parameters);
}

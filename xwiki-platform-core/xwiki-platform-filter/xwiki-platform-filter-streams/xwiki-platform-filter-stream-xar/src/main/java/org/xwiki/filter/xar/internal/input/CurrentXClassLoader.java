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
package org.xwiki.filter.xar.internal.input;

import org.xwiki.component.annotation.Role;

/**
 * Internal component for reading current xclass information as fallback for reading object properties.
 *
 * @version $Id$
 * @since 18.8.0RC1
 * @since 18.4.5
 * @since 17.10.13
 */
@Role
public interface CurrentXClassLoader
{
    /**
     * Read the class identified by the given reference and search for the type of the parameter identified by the
     * given name.
     *
     * @param xclassReference the reference of the class to read.
     * @param propertyName the name of the property for which to read the type.
     * @return the property type or {@code null}.
     */
    String getXClassPropertyType(String xclassReference, String propertyName);
}

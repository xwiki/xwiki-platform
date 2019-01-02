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
package org.xwiki.rest.resources.classes;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;

/**
 * Provides values for a class property.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@Role
public interface ClassPropertyValuesProvider
{
    /**
     * Provides values for a class property.
     * 
     * @param propertyReference the property to provide the values for
     * @param limit the maximum number of values to return
     * @param filterParameters additional parameters used to filter the values
     * @return the values for the specified property
     * @throws XWikiRestException if retrieving the property values fails
     */
    PropertyValues getValues(ClassPropertyReference propertyReference, int limit, Object... filterParameters)
        throws XWikiRestException;

    /**
     * Resolves the given raw value into a {@link PropertyValue} of the specified class property.
     *
     * @param propertyReference the property to provide the value for
     * @param rawValue raw value used to resolve the property value
     * @return the property value based on the raw value or null if the raw value is empty
     * @throws XWikiRestException if retrieving the property value fails
     * @since 10.9
     */
    default PropertyValue getValue(ClassPropertyReference propertyReference, Object rawValue)
        throws XWikiRestException {
        throw new UnsupportedOperationException();
    }
}

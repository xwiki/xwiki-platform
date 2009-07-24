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
package org.xwiki.properties;

/**
 * A bean can implement this interface to be provided with remaining values as custom properties.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public interface RawProperties
{
    /**
     * Set a custom property to the bean.
     * <p>
     * This method need to be named that way or any other way that don't match property getter/setter naming. We could
     * used {@link org.xwiki.properties.annotation.PropertyHidden} but it's better to avoid it if possible.
     * 
     * @param propertyName the name of the custom property
     * @param value the value of the property
     */
    void set(String propertyName, Object value);
}

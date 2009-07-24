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

import java.util.Collection;

/**
 * Contains information on a java bean.
 * <p>
 * The supported properties are:
 * <ul>
 * <li>public getters and setters. For example getSomeVar/setSomeVar correspond to "someVar" property name</li>
 * <li>public fields. The name of the property is the name of the field.</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M2
 */
public interface BeanDescriptor
{
    /**
     * @return the class of the JAVA bean containing.
     */
    Class< ? > getBeanClass();

    /**
     * @return the properties of the java bean.
     */
    Collection<PropertyDescriptor> getProperties();

    /**
     * Return descriptor for the provided property name. The property name is case sensitive.
     * 
     * @param propertyName the name of the property
     * @return the descriptor of the property.
     */
    PropertyDescriptor getProperty(String propertyName);
}

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

import java.util.Map;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Component used to populate or parse a java bean.
 * <ul>
 * <li>{@link #populate(Object, Map)} ignore properties names case</li>
 * <li>{@link #populate(Object, Map)} validate the bean based JSR 303</li>
 * <li>if the bean implements {@link RawProperties}, the remaining property (the one non populated using setters of
 * public fields) are given to it as custom non converted properties</li>
 * </ul>
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentRole
public interface BeanManager
{
    /**
     * Convert provided values and inject them in the provided java bean.
     * 
     * @param bean the java bean to populate
     * @param values the values to convert and inject in the java bean
     * @throws PropertyException error append during the populate
     */
    void populate(Object bean, Map<String, ? > values) throws PropertyException;

    /**
     * Parse provided java bean and return a descriptor with all its public properties.
     * 
     * @param beanClass the java bean class to parse.
     * @return the descriptor of the bean class.
     */
    BeanDescriptor getBeanDescriptor(Class< ? > beanClass);
}

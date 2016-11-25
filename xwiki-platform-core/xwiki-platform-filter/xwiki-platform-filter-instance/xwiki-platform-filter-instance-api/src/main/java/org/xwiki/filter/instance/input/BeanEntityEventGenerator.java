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
package org.xwiki.filter.instance.input;

import org.xwiki.filter.FilterException;

/**
 * @param <E> the type of the entity (XWikiDocument, BaseObject, BaseClass, etc.)
 * @param <P> the type of the properties bean
 * @version $Id$
 * @since 9.0RC1
 */
public interface BeanEntityEventGenerator<E, P> extends EntityEventGenerator<E>
{
    /**
     * @param entity the entity to read
     * @param filter the filter to send events to
     * @param properties the properties
     * @throws FilterException when failing to convert passed entity into events
     */
    void write(E entity, Object filter, P properties) throws FilterException;
}

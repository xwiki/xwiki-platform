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
package com.xpn.xwiki.internal.filter.output;

import org.xwiki.component.annotation.Role;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;

import com.xpn.xwiki.internal.filter.XWikiDocumentFilter;

/**
 * Create an entity from filter events.
 * 
 * @param <E> the type of the entity supported
 * @version $Id$
 * @since 9.0RC1
 */
@Role
public interface EntityOutputFilterStream<E> extends XWikiDocumentFilter
{
    /**
     * @return the filter to send events to
     */
    Object getFilter();

    /**
     * @return the entity
     */
    E getEntity();

    /**
     * @param entity the entity
     */
    void setEntity(E entity);

    /**
     * @param properties the properties to control EntityOutputFilterStream behavior
     */
    void setProperties(DocumentInstanceOutputProperties properties);

    /**
     * @return true if the listener is enabled
     */
    boolean isEnabled();

    /**
     * Enable.
     */
    void enable();

    /**
     * Disable.
     */
    void disable();
}

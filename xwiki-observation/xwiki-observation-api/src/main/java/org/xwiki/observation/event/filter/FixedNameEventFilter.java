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
 *
 */
package org.xwiki.observation.event.filter;

import java.io.Serializable;

/**
 * An {@link EventFilter} that matches exactly one document name.
 * 
 * @version $Id$
 */
public class FixedNameEventFilter implements EventFilter, Serializable
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /** The name of the matched document. */
    private String filter;

    /**
     * Constructor initializing this event filter with a document name that should be matched.
     * 
     * @param filter the name of the matched document
     */
    public FixedNameEventFilter(String filter)
    {
        this.filter = filter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventFilter#getFilter()
     */
    public String getFilter()
    {
        return this.filter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see EventFilter#getFilter()
     */
    public boolean matches(EventFilter eventFilter)
    {
        return (getFilter().equals(eventFilter.getFilter()));
    }
}

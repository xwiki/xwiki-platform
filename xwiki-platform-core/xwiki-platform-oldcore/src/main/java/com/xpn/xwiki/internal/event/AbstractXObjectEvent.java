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
package com.xpn.xwiki.internal.event;

import org.xwiki.model.reference.EntityReference;

/**
 * Base class for all object {@link org.xwiki.observation.event.Event events}.
 * 
 * @version $Id$
 * @since 3.2M1
 */
public abstract class AbstractXObjectEvent extends AbstractEntityEvent implements XObjectEvent
{
    /**
     * Constructor initializing the event filter with an
     * {@link org.xwiki.observation.event.filter.AlwaysMatchingEventFilter}, meaning that this event will match any
     * other attachment event (add, update, delete).
     */
    public AbstractXObjectEvent()
    {
    }

    /**
     * @param reference the reference of the object
     */
    public AbstractXObjectEvent(EntityReference reference)
    {
        super(reference);
    }
}

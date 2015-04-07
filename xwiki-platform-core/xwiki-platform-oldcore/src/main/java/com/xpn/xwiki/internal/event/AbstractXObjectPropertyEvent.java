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
 * Base class for all object property {@link org.xwiki.observation.event.Event events}.
 *
 * @version $Id$
 * @since 3.2M1
 */
public abstract class AbstractXObjectPropertyEvent extends AbstractXObjectEvent implements XObjectPropertyEvent
{
    /**
     * Default constructor. Matches any {@link XObjectPropertyEvent}.
     */
    public AbstractXObjectPropertyEvent()
    {
    }

    /**
     * @param reference the object property reference
     */
    public AbstractXObjectPropertyEvent(EntityReference reference)
    {
        super(reference);
    }
}

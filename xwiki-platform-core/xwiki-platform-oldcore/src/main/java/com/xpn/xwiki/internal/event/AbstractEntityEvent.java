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
 * Base class for all entity {@link org.xwiki.observation.event.Event events}.
 * 
 * @version $Id$
 * @since xxx
 */
public class AbstractEntityEvent implements EntityEvent
{
    private EntityReference reference;

    public AbstractEntityEvent()
    {
    }

    public AbstractEntityEvent(EntityReference reference)
    {
        this.reference = reference;
    }

    public EntityReference getReference()
    {
        return this.reference;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == this) {
            return true;
        }

        return otherEvent instanceof EntityEvent && matchesReference(((EntityEvent) otherEvent).getReference());
    }

    protected boolean matchesReference(EntityReference otherReference)
    {
        return getReference() == null || getReference().equals(otherReference);
    }
}

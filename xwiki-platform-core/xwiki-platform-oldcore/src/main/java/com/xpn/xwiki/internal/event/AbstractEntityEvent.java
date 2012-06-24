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

import org.apache.commons.lang3.ObjectUtils;
import org.xwiki.model.reference.EntityReference;

/**
 * Base class for all entity {@link org.xwiki.observation.event.Event events}.
 * 
 * @version $Id$
 * @since 3.2M1
 */
public abstract class AbstractEntityEvent implements EntityEvent
{
    /**
     * @see #getReference()
     */
    private EntityReference reference;

    /**
     * Default constructor. Matches any {@link EntityEvent}.
     */
    public AbstractEntityEvent()
    {
    }

    /**
     * @param reference the reference
     */
    public AbstractEntityEvent(EntityReference reference)
    {
        this.reference = reference;
    }

    @Override
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

    /**
     * Try to match the provided reference.
     * 
     * @param otherReference the reference to match
     * @return true if the provided reference is matched
     */
    protected boolean matchesReference(EntityReference otherReference)
    {
        return getReference() == null || getReference().equals(otherReference);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (super.equals(obj)) {
            return true;
        }

        return getClass() == obj.getClass() && ObjectUtils.equals(this.reference, ((EntityEvent) obj).getReference());
    }

    @Override
    public int hashCode()
    {
        return this.reference != null ? this.reference.hashCode() : 0;
    }
}

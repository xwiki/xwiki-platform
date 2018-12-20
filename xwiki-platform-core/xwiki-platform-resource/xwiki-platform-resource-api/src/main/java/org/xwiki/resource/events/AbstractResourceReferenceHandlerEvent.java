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
package org.xwiki.resource.events;

import java.util.Objects;

import org.xwiki.resource.ResourceReference;

/**
 * Base class for {@link ResourceReferenceHandlerEvent} implementations.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
public abstract class AbstractResourceReferenceHandlerEvent implements ResourceReferenceHandlerEvent
{
    /**
     * The name of the executed action.
     */
    private ResourceReference reference;

    /**
     * Match any {@link ResourceReferenceHandlerEvent}.
     */
    public AbstractResourceReferenceHandlerEvent()
    {
        // Empty voluntarily, just here to offer a default constructor
    }

    /**
     * Constructor initializing the reference of the event.
     * 
     * @param reference the reference handled
     */
    public AbstractResourceReferenceHandlerEvent(ResourceReference reference)
    {
        this.reference = reference;
    }

    @Override
    public ResourceReference getResourceReference()
    {
        return this.reference;
    }

    @Override
    public int hashCode()
    {
        if (getResourceReference() == null) {
            return 0;
        }

        return getResourceReference().hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && getClass().isAssignableFrom(object.getClass())) {
            return Objects.equals(getResourceReference(),
                ((ResourceReferenceHandlerEvent) object).getResourceReference());
        }
        return false;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == null) {
            return false;
        }

        if (getClass().isAssignableFrom(otherEvent.getClass())) {
            ResourceReferenceHandlerEvent actionEvent = (ResourceReferenceHandlerEvent) otherEvent;

            return getResourceReference() == null || getResourceReference().equals(actionEvent.getResourceReference());
        }

        return false;
    }

    @Override
    public String toString()
    {
        return getClass() + " (" + getResourceReference() + ")";
    }
}

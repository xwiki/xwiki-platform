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
package org.xwiki.component.manager;

import org.xwiki.observation.event.Event;

/**
 * Event sent to tell that a new Component Descriptor has been registered.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class ComponentDescriptorAddedEvent implements Event
{
    private Class< ? > role;
    
    public ComponentDescriptorAddedEvent(Class< ? > role)
    {
        this.role = role;
    }
    
    public Class< ? > getRole()
    {
        return this.role;
    }
    
    /**
     * {@inheritDoc}
     * @see Event#matches(Object)
     */
    public boolean matches(Object otherEvent)
    {
        boolean result = false;
        
        if (ComponentDescriptorAddedEvent.class.isAssignableFrom(otherEvent.getClass())) {
            ComponentDescriptorAddedEvent event = (ComponentDescriptorAddedEvent) otherEvent;
            if (getRole().getName().equals(event.getRole())) {
                result = true;
            }
        }
        
        return result; 
    }
}

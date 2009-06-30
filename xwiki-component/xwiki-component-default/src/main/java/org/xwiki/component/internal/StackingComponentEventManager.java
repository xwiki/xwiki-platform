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
package org.xwiki.component.internal;

import java.util.Stack;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentDescriptorAddedEvent;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.observation.ObservationManager;

/**
 * Allow stacking component events and flush them whenever the user of this class wants to. This is used for example
 * at application initialization time when we don't want to send events before the Application Context has been
 * initialized since components subscribing to these events may want to use the Application Context.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class StackingComponentEventManager implements ComponentEventManager
{
    private ObservationManager observationManager;
    
    private Stack<ComponentDescriptor< ? >> events = new Stack<ComponentDescriptor< ? >>();
    
    private boolean shouldStack = true;
    
    /**
     * {@inheritDoc}
     * @see ComponentEventManager#notify(ComponentDescriptor)
     */
    public <T> void notify(ComponentDescriptor< T > descriptor)
    {
        if (this.shouldStack) {
            synchronized (this) {
                this.events.push(descriptor);
            }
        } else {
            notifyInternal(descriptor);
        }
    }
    
    public synchronized void flushEvents()
    {
        while(!this.events.isEmpty()) {
            notifyInternal(events.remove(0));
        }
    }
    
    public void shouldStack(boolean shouldStack)
    {
        this.shouldStack = shouldStack;
    }
    
    public void setObservationManager(ObservationManager observationManager)
    {
        this.observationManager = observationManager;
    }

    private void notifyInternal(ComponentDescriptor< ? > descriptor)
    {
        if (this.observationManager != null) {
            ComponentDescriptorAddedEvent event = new ComponentDescriptorAddedEvent(descriptor.getRole());
            this.observationManager.notify(event, this, descriptor);
        }
    }
}

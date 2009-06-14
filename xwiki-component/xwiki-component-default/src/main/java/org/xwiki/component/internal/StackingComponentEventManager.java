package org.xwiki.component.internal;

import java.util.Stack;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentDescriptorAddedEvent;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.observation.ObservationManager;

public class StackingComponentEventManager implements ComponentEventManager
{
    private ObservationManager observationManager;
    
    private Stack<ComponentDescriptor< ? >> events = new Stack<ComponentDescriptor< ? >>();
    
    private boolean shouldStack = true;
    
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

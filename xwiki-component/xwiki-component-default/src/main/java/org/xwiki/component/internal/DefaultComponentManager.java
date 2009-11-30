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

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * Wraps the Component Manager in a component so that components requiring the component Manager can have it injected
 * automatically.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class DefaultComponentManager implements ComponentManager, Composable
{
    /**
     * The Component Manager instance we're wrapping.
     */
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see Composable#compose(ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#getComponentDescriptor(Class, String)
     */
    public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String roleHint)
    {
        return this.componentManager.getComponentDescriptor(role, roleHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#getComponentDescriptorList(Class)
     */
    public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role)
    {
        return this.componentManager.getComponentDescriptorList(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#lookup(Class, String)
     */
    public <T> T lookup(Class<T> role, String roleHint) throws ComponentLookupException
    {
        return this.componentManager.lookup(role, roleHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#lookup(Class)
     */
    public <T> T lookup(Class<T> role) throws ComponentLookupException
    {
        return this.componentManager.lookup(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#lookupList(Class)
     */
    public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException
    {
        return this.componentManager.lookupList(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#lookupMap(Class)
     */
    public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException
    {
        return this.componentManager.lookupMap(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#registerComponent(ComponentDescriptor)
     */
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException
    {
        this.componentManager.registerComponent(componentDescriptor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#registerComponent(org.xwiki.component.descriptor.ComponentDescriptor,
     *      java.lang.Object)
     */
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor, T componentInstance)
        throws ComponentRepositoryException
    {
        this.componentManager.registerComponent(componentDescriptor, componentInstance);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.manager.ComponentManager#unregisterComponent(java.lang.Class, java.lang.String)
     */
    public void unregisterComponent(Class< ? > role, String roleHint)
    {
        this.componentManager.unregisterComponent(role, roleHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#release(Object)
     */
    public <T> void release(T component) throws ComponentLifecycleException
    {
        this.componentManager.release(component);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#hasComponent(Class, String)
     */
    public <T> boolean hasComponent(Class<T> role, String roleHint)
    {
        return this.componentManager.hasComponent(role, roleHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#hasComponent(Class)
     */
    public <T> boolean hasComponent(Class<T> role)
    {
        return this.componentManager.hasComponent(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#getComponentEventManager()
     */
    public ComponentEventManager getComponentEventManager()
    {
        return this.componentManager.getComponentEventManager();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#setComponentEventManager(ComponentEventManager)
     */
    public void setComponentEventManager(ComponentEventManager eventManager)
    {
        this.componentManager.setComponentEventManager(eventManager);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#getParent()
     */
    public ComponentManager getParent()
    {
        return this.componentManager.getParent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ComponentManager#setParent(ComponentManager)
     */
    public void setParent(ComponentManager parentComponentManager)
    {
        this.componentManager.setParent(parentComponentManager);
    }
}

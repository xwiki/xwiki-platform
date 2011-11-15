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

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;

/**
 * Delegate all calls to a defined Component Manager, acting as a Proxy for it.
 *  
 * @version $Id$
 * @since 2.1RC1
 */
public class DelegateComponentManager implements ComponentManager
{
    /**
     * @see #getComponentManager()
     */
    private ComponentManager componentManager;
    
    /**
     * @return the Component Manager to delegate to
     */
    public ComponentManager getComponentManager()
    {
        return this.componentManager;
    }
    
    /**
     * @param componentManager see {@link #getComponentManager()}
     */
    public void setComponentManager(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    @Override
    public <T> ComponentDescriptor<T> getComponentDescriptor(Class<T> role, String roleHint)
    {
        return getComponentManager().getComponentDescriptor(role, roleHint);
    }

    @Override
    public <T> List<ComponentDescriptor<T>> getComponentDescriptorList(Class<T> role)
    {
        return getComponentManager().getComponentDescriptorList(role);
    }

    @Override
    public <T> boolean hasComponent(Class<T> role, String roleHint)
    {
        return getComponentManager().hasComponent(role, roleHint);
    }

    @Override
    public <T> boolean hasComponent(Class<T> role)
    {
        return getComponentManager().hasComponent(role);
    }

    @Override
    public <T> T lookup(Class<T> role, String roleHint) throws ComponentLookupException
    {
        return getComponentManager().lookup(role, roleHint);
    }

    @Override
    public <T> T lookup(Class<T> role) throws ComponentLookupException
    {
        return getComponentManager().lookup(role);
    }

    @Override
    public <T> List<T> lookupList(Class<T> role) throws ComponentLookupException
    {
        return getComponentManager().lookupList(role);
    }

    @Override
    public <T> Map<String, T> lookupMap(Class<T> role) throws ComponentLookupException
    {
        return getComponentManager().lookupMap(role);
    }

    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor, T componentInstance)
        throws ComponentRepositoryException
    {
        getComponentManager().registerComponent(componentDescriptor, componentInstance);
    }

    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException
    {
        getComponentManager().registerComponent(componentDescriptor);
    }

    @Override
    public <T> void release(T component) throws ComponentLifecycleException
    {
        getComponentManager().release(component);
    }

    @Override
    public ComponentEventManager getComponentEventManager()
    {
        return getComponentManager().getComponentEventManager();
    }

    @Override
    public void setComponentEventManager(ComponentEventManager eventManager)
    {
        getComponentManager().setComponentEventManager(eventManager);
    }

    @Override
    public <T> void unregisterComponent(Class<T> role, String roleHint)
    {
        getComponentManager().unregisterComponent(role, roleHint);
    }

    @Override
    public ComponentManager getParent()
    {
        return getComponentManager().getParent();
    }

    @Override
    public void setParent(ComponentManager parentComponentManager)
    {
        getComponentManager().setParent(parentComponentManager);
    }
}

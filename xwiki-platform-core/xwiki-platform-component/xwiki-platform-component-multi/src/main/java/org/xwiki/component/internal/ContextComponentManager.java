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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentEventManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * Chains Component Managers to perform lookups based on the current execution context
 * (current user, current wiki, etc).
 *  
 * @version $Id$
 * @since 2.1RC1
 */
@Component("context")
public class ContextComponentManager extends DelegateComponentManager implements Initializable
{
    /**
     * The first Component Manager in the chain.
     */
    @Requirement("user")
    private ComponentManager userComponentManager;
    
    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // The first Component Manager in the lookup chain is the user Component Manager (i.e. components registered
        // for the current user).
        setComponentManager(this.userComponentManager);
    }

    // Make the Context Component Manager "read-only". Writes should be done against specific Component Managers.

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#registerComponent(ComponentDescriptor, Object)
     */
    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor, T componentInstance)
        throws ComponentRepositoryException
    {
        throwException();
    }

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#registerComponent(ComponentDescriptor)
     */
    @Override
    public <T> void registerComponent(ComponentDescriptor<T> componentDescriptor) throws ComponentRepositoryException
    {
        throwException();
    }

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#release(Object)
     */
    @Override
    public <T> void release(T component) throws ComponentLifecycleException
    {
        throwException();
    }

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#setComponentEventManager(ComponentEventManager)
     */
    @Override
    public void setComponentEventManager(ComponentEventManager eventManager)
    {
        throwException();
    }

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#setParent(ComponentManager)
     */
    @Override
    public void setParent(ComponentManager parentComponentManager)
    {
        throwException();
    }

    /**
     * {@inheritDoc}
     * @see DelegateComponentManager#unregisterComponent(Class, String)
     */
    @Override
    public void unregisterComponent(Class< ? > role, String roleHint)
    {
        throwException();
    }
    
    /**
     * Exception to throw when trying to access a write method since this Component Manager is a chaining
     * Component Manager and should only be used for read-only access.
     */
    private void throwException()
    {
        throw new RuntimeException("The Context Component Manager should only be used for read access. Write "
            + "operations should be done against specific Component Managers.");
    }
}

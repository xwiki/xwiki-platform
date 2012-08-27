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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentInvocationHandler;
import org.xwiki.component.wiki.WikiComponentManager;
import org.xwiki.model.reference.DocumentReference;

/**
 * Default implementation of {@link WikiComponentManager}. Creates proxy objects which method invocation handler keeps a
 * reference on a set of declared method and associated wiki content to "execute".
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Singleton
public class DefaultWikiComponentManager implements WikiComponentManager
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Component manager against which wiki component will be registered.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Reference on all registered components.
     */
    private Set<WikiComponent> registeredComponents = new HashSet<WikiComponent>();
    
    @Override
    @SuppressWarnings("unchecked")
    public void registerWikiComponent(WikiComponent component) throws WikiComponentException
    {
        if (registeredComponents.contains(component)) {
            throw new WikiComponentException("Component already registered. Try unregistering it first.");
        }
        
        try {
            // Get the component role interface
            Class< ? > role = component.getRole();

            // Create the method invocation handler of the proxy
            InvocationHandler handler = new WikiComponentInvocationHandler(component, componentManager);

            // Prepare a list containing the interfaces the component implements
            List<Class<?>> implementedInterfaces = new ArrayList<Class<?>>();

            // Add all the interfaces declared through XObjects
            implementedInterfaces.addAll(component.getImplementedInterfaces());

            // If the component is a Java classes extending the default WikiComponent interface, we add all the
            // interfaces it implements to the list, except the WikiComponent one of course.
            for (Class<?> implementedInterface : component.getClass().getInterfaces()) {
                if (implementedInterface != WikiComponent.class) {
                    implementedInterfaces.add(implementedInterface);
                }
            }

            // If the component is a pure XObject implementation, we need to add the role interface to the list, since
            // it's not been added by the previous loops.
            if (!implementedInterfaces.contains(role)) {
                implementedInterfaces.add(role);
            }

            // Create the component instance and its descriptor
            Class< ? >[] implementedInterfacesArray = implementedInterfaces.toArray(new Class<?>[0]);
            Object instance = Proxy.newProxyInstance(role.getClassLoader(), implementedInterfacesArray, handler);
            ComponentDescriptor componentDescriptor = this.createComponentDescriptor(role, component.getRoleHint());

            // Since we are responsible to create the component instance,
            // we also are responsible of its initialization (if needed)
            if (this.isInitializable(implementedInterfaces)) {
                try {
                    ((Initializable) instance).initialize();
                } catch (InitializationException e) {
                    this.logger.error("Failed to initialize wiki component", e);
                }
            }

            // Finally, register the component against the CM
            componentManager.registerComponent(componentDescriptor, role.cast(instance));
            
            // And hold a reference to it.
            this.registeredComponents.add(component);
            
        } catch (ComponentRepositoryException e) {
            throw new WikiComponentException("Failed to register wiki component against component repository", e);
        }
    }

    @Override
    public void unregisterWikiComponents(DocumentReference reference)
    {
        WikiComponent unregisteredComponent = null;

        for (WikiComponent registered : this.registeredComponents) {
            if (registered.getDocumentReference().equals(reference)) {
                // Unregister component
                unregisteredComponent = registered;
                componentManager.unregisterComponent(registered.getRole(), registered.getRoleHint());
            }
        }

        // Remove reference
        if (unregisteredComponent != null) {
            this.registeredComponents.remove(unregisteredComponent);
        }
    }
    
    /**
     * Helper method to create a component descriptor from role and hint.
     * 
     * @param role the component role of the descriptor to create
     * @param roleHint the hint of the implementation for the descriptor to create
     * @return the constructed {@link ComponentDescriptor}
     */
    @SuppressWarnings("unchecked")
    private ComponentDescriptor createComponentDescriptor(Class role, String roleHint)
    {
        DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
        cd.setRole(role);
        cd.setRoleHint(roleHint);
        return cd;
    }

    /**
     * Helper method that checks if at least one of an array of interfaces is the {@link Initializable} class.
     * 
     * @param interfaces the array of interfaces to test
     * @return true if at least one of the passed interfaces is the is the {@link Initializable} class.
     */
    private boolean isInitializable(List<Class< ? >> interfaces)
    {
        for (Class< ? > iface : interfaces) {
            if (Initializable.class.equals(iface)) {
                return true;
            }
        }
        return false;
    }
}

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

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentManager;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

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
     * Component managers in which wiki component can be registered.
     */
    @Inject
    private ComponentManager rootComponentManager;

    /**
     * Allows to get and set context user and reference.
     */
    @Inject
    private WikiComponentManagerContext wikiComponentManagerContext;

    /**
     * Map of registered components.
     */
    private Set<WikiComponent> registeredComponents = new HashSet<WikiComponent>();
    
    @Override
    @SuppressWarnings("unchecked")
    public void registerWikiComponent(WikiComponent component) throws WikiComponentException
    {
        // Save current context information
        DocumentReference currentUserReference = this.wikiComponentManagerContext.getCurrentUserReference();
        EntityReference currentEntityReference = this.wikiComponentManagerContext.getCurrentEntityReference();
        
        try {
            // Get the component role interface
            Type roleType = component.getRoleType();
            Class<?> roleTypeClass = ReflectionUtils.getTypeClass(roleType);
            ComponentDescriptor componentDescriptor = this.createComponentDescriptor(roleType, component.getRoleHint());

            // Set the proper information so the component manager use the proper keys to find components to register
            this.wikiComponentManagerContext.setCurrentUserReference(component.getAuthorReference());
            this.wikiComponentManagerContext.setCurrentEntityReference(component.getDocumentReference());

            // Since we are responsible to create the component instance, we also are responsible of its initialization
            if (this.isInitializable(component.getClass().getInterfaces())) {
                try {
                    ((Initializable) component).initialize();
                } catch (InitializationException e) {
                    this.logger.error("Failed to initialize wiki component", e);
                }
            }

            getComponentManager(component.getScope()).registerComponent(componentDescriptor,
                roleTypeClass.cast(component));

            // And hold a reference to it.
            this.registeredComponents.add(component);
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to find a component manager for scope [%s] wiki "
                + "component registration failed",
                component.getScope()), e);
        } catch (ComponentRepositoryException e) {
            throw new WikiComponentException("Failed to register wiki component against component repository", e);
        }   finally {
            this.wikiComponentManagerContext.setCurrentUserReference(currentUserReference);
            this.wikiComponentManagerContext.setCurrentEntityReference(currentEntityReference);
        }
    }

    @Override
    public void unregisterWikiComponents(DocumentReference reference) throws WikiComponentException
    {
        WikiComponent unregisteredComponent = null;
        // Save current context information
        DocumentReference currentUserReference = this.wikiComponentManagerContext.getCurrentUserReference();
        EntityReference currentEntityReference = this.wikiComponentManagerContext.getCurrentEntityReference();

        for (WikiComponent registered : this.registeredComponents) {
            if (registered.getDocumentReference().equals(reference)) {
                // Unregister component
                unregisteredComponent = registered;
                try {
                    // Set the proper information so the component manager use the proper keys to find components to
                    // unregister
                    this.wikiComponentManagerContext.setCurrentUserReference(registered.getAuthorReference());
                    this.wikiComponentManagerContext.setCurrentEntityReference(registered.getDocumentReference());

                    getComponentManager(registered.getScope()).unregisterComponent(registered.getRoleType(),
                        registered.getRoleHint());
                } catch (ComponentLookupException e) {
                    throw new WikiComponentException(String.format("Failed to find a component manager for scope [%s]",
                        registered.getScope()), e);
                }  finally {
                    this.wikiComponentManagerContext.setCurrentUserReference(currentUserReference);
                    this.wikiComponentManagerContext.setCurrentEntityReference(currentEntityReference);
                }

            }
        }

        // Remove reference
        if (unregisteredComponent != null) {
            this.registeredComponents.remove(reference);
        }
    }


    /**
     * @param scope the scope required
     * @return the Component Manager to use to register/unregister the wiki macro. The Component Manager to use depends
     *         on the macro scope. For example a macro that has the "current user" scope, it must be registered against
     *         the User Component Manager.
     * @throws ComponentLookupException if the Component Manager for the specified scope cannot be found
     */
    private ComponentManager getComponentManager(WikiComponentScope scope) throws ComponentLookupException
    {
        ComponentManager cm;

        switch (scope) {
            case USER:
                cm = this.rootComponentManager.getInstance(ComponentManager.class, "user");
                break;
            case WIKI:
                cm = this.rootComponentManager.getInstance(ComponentManager.class, "wiki");
                break;
            default:
                cm = this.rootComponentManager;
        }

        return cm;
    }
    
    /**
     * Helper method to create a component descriptor from role and hint.
     * 
     * @param roleType the component role type of the descriptor to create
     * @param roleHint the hint of the implementation for the descriptor to create
     * @return the constructed {@link ComponentDescriptor}
     */
    @SuppressWarnings("unchecked")
    private ComponentDescriptor createComponentDescriptor(Type roleType, String roleHint)
    {
        DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
        cd.setRoleType(roleType);
        cd.setRoleHint(roleHint);
        return cd;
    }

    /**
     * Helper method that checks if at least one of an array of interfaces is the {@link Initializable} class.
     *
     * @param interfaces the array of interfaces to test
     * @return true if at least one of the passed interfaces is the is the {@link Initializable} class.
     */
    private boolean isInitializable(Class< ? >[] interfaces)
    {
        for (Class< ? > iface : interfaces) {
            if (Initializable.class.equals(iface)) {
                return true;
            }
        }
        return false;
    }
}

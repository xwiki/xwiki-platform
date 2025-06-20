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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
     * Map of registered components. We need to keep a cache because wiki components can be components of various
     * Role Types and we cannot ask the Component Manager for wiki component only (which we need to do when we need to
     * unregister them). A wiki page can hold one or several wiki components.
     */
    private Map<EntityReference, List<WikiComponent>> registeredComponents = new ConcurrentHashMap<>();

    @Override
    public void registerWikiComponent(WikiComponent component) throws WikiComponentException
    {
        // Save current context information
        DocumentReference currentUserReference = this.wikiComponentManagerContext.getCurrentUserReference();
        EntityReference currentEntityReference = this.wikiComponentManagerContext.getCurrentEntityReference();

        try {
            // Get the component role interface
            Type roleType = component.getRoleType();
            Class< ? > roleTypeClass = ReflectionUtils.getTypeClass(roleType);
            ComponentDescriptor componentDescriptor = createComponentDescriptor(roleType, component);

            // Set the proper information so the component manager use the proper keys to find components to register
            this.wikiComponentManagerContext.setCurrentUserReference(component.getAuthorReference());
            this.wikiComponentManagerContext.setCurrentEntityReference(component.getEntityReference());

            // Since we are responsible to create the component instance, we also are responsible of its initialization
            if (this.isInitializable(component.getClass().getInterfaces())) {
                try {
                    ((Initializable) component).initialize();
                } catch (InitializationException e) {
                    this.logger.error("Failed to initialize wiki component", e);
                }
            }

            // Register the wiki component against the Component Manager
            getComponentManager(component.getScope()).registerComponent(componentDescriptor,
                roleTypeClass.cast(component));

            // And add it the wiki component cache so that we can remove it later on. We need to do this since we need
            // to be able to unregister a wiki component associated with a wiki page
            cacheWikiComponent(component);
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to find a component manager for scope [%s] wiki "
                + "component registration failed", component.getScope()), e);
        } catch (ComponentRepositoryException e) {
            throw new WikiComponentException("Failed to register wiki component against component repository", e);
        } finally {
            this.wikiComponentManagerContext.setCurrentUserReference(currentUserReference);
            this.wikiComponentManagerContext.setCurrentEntityReference(currentEntityReference);
        }
    }

    private void cacheWikiComponent(WikiComponent component)
    {
        List<WikiComponent> wikiComponents = this.registeredComponents.get(component.getEntityReference());
        if (wikiComponents == null) {
            wikiComponents = new ArrayList<>();
            this.registeredComponents.put(component.getEntityReference(), wikiComponents);
        }
        if (!wikiComponents.contains(component)) {
            wikiComponents.add(component);
        }
    }

    @Override
    public void unregisterWikiComponents(DocumentReference reference) throws WikiComponentException
    {
        this.unregisterWikiComponents((EntityReference) reference);
    }

    @Override
    public void unregisterWikiComponents(EntityReference reference) throws WikiComponentException
    {
        List<WikiComponent> wikiComponents = this.registeredComponents.get(reference);
        if (wikiComponents != null) {
            Iterator<WikiComponent> iterator = wikiComponents.iterator();
            while (iterator.hasNext()) {
                unregisterWikiComponent(iterator);
            }
            // Clean up wiki component cache for the passed reference, if it doesn't contain any wiki component
            wikiComponents = this.registeredComponents.get(reference);
            if (wikiComponents.isEmpty()) {
                this.registeredComponents.remove(reference);
            }
        }
    }

    private void unregisterWikiComponent(Iterator<WikiComponent> iterator)
        throws WikiComponentException
    {
        WikiComponent wikiComponent = iterator.next();

        // Save current context information
        DocumentReference currentUserReference = this.wikiComponentManagerContext.getCurrentUserReference();
        EntityReference currentEntityReference = this.wikiComponentManagerContext.getCurrentEntityReference();
        try {
            // Set the proper information so the component manager use the proper keys to find components to
            // unregister
            this.wikiComponentManagerContext.setCurrentUserReference(wikiComponent.getAuthorReference());
            this.wikiComponentManagerContext.setCurrentEntityReference(wikiComponent.getEntityReference());
            // Remove from the Component Manager
            getComponentManager(wikiComponent.getScope()).unregisterComponent(wikiComponent.getRoleType(),
                wikiComponent.getRoleHint());
            // Remove from the wiki component cache
            iterator.remove();
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to find a component manager for scope [%s]",
                wikiComponent.getScope()), e);
        } finally {
            this.wikiComponentManagerContext.setCurrentUserReference(currentUserReference);
            this.wikiComponentManagerContext.setCurrentEntityReference(currentEntityReference);
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
     * @param component the component for which to create a descriptor
     * @return the constructed {@link ComponentDescriptor}
     */
    private ComponentDescriptor createComponentDescriptor(Type roleType, WikiComponent component)
    {
        DefaultComponentDescriptor cd = new DefaultComponentDescriptor();
        cd.setRoleType(roleType);
        cd.setRoleHint(component.getRoleHint());
        cd.setImplementation(component.getClass());
        cd.setRoleHintPriority(component.getRoleHintPriority());
        cd.setRoleTypePriority(component.getRoleTypePriority());

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

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
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
import org.xwiki.model.EntityType;
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
    private final Map<EntityReference, CopyOnWriteArrayList<WikiComponent>> registeredComponents =
        new ConcurrentHashMap<>();

    @Override
    public void registerWikiComponent(WikiComponent component) throws WikiComponentException
    {
        registerWikiComponent(component, true);
    }

    private void registerWikiComponent(WikiComponent component, boolean initialize) throws WikiComponentException
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
            if (initialize && this.isInitializable(component.getClass().getInterfaces())) {
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
        this.registeredComponents.computeIfAbsent(component.getEntityReference(), key -> new CopyOnWriteArrayList<>())
            .addIfAbsent(component);
    }

    @Override
    public void unregisterWikiComponents(DocumentReference reference) throws WikiComponentException
    {
        this.unregisterWikiComponents((EntityReference) reference);
    }

    @Override
    public void unregisterWikiComponents(EntityReference reference) throws WikiComponentException
    {
        CopyOnWriteArrayList<WikiComponent> wikiComponents = this.registeredComponents.get(reference);
        if (wikiComponents != null) {
            // Iterating a copy-on-write list works on a snapshot, so removing elements while iterating is safe.
            for (WikiComponent wikiComponent : wikiComponents) {
                unregisterWikiComponent(wikiComponent);
                // Remove from the wiki component cache
                wikiComponents.remove(wikiComponent);
            }
            // Clean up wiki component cache for the passed reference, if it doesn't contain any wiki component
            this.registeredComponents.computeIfPresent(reference, (key, list) -> list.isEmpty() ? null : list);
        }
    }

    private void unregisterWikiComponent(WikiComponent wikiComponent) throws WikiComponentException
    {
        WikiComponent shadowedComponent = null;

        // Save current context information
        DocumentReference currentUserReference = this.wikiComponentManagerContext.getCurrentUserReference();
        EntityReference currentEntityReference = this.wikiComponentManagerContext.getCurrentEntityReference();
        try {
            // Set the proper information so the component manager use the proper keys to find components to
            // unregister
            this.wikiComponentManagerContext.setCurrentUserReference(wikiComponent.getAuthorReference());
            this.wikiComponentManagerContext.setCurrentEntityReference(wikiComponent.getEntityReference());
            ComponentManager componentManager = getComponentManager(wikiComponent.getScope());
            if (unregisterFromComponentManager(wikiComponent, componentManager)) {
                shadowedComponent = getShadowedComponent(wikiComponent);
            }
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(String.format("Failed to find a component manager for scope [%s]",
                wikiComponent.getScope()), e);
        } finally {
            this.wikiComponentManagerContext.setCurrentUserReference(currentUserReference);
            this.wikiComponentManagerContext.setCurrentEntityReference(currentEntityReference);
        }

        // Register again the wiki component that was replaced by the unregistered one, if any, as otherwise its role
        // would be left without any wiki component even though its definition still exists.
        if (shadowedComponent != null) {
            try {
                // The component has already been initialized when it was first registered.
                registerWikiComponent(shadowedComponent, false);
            } catch (WikiComponentException e) {
                this.logger.warn("Failed to register again the wiki component defined in [{}]: [{}]",
                    shadowedComponent.getEntityReference(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Removes the passed wiki component from the Component Manager, unless it has been replaced in the meantime by
     * another component with the same role type and hint.
     *
     * @param wikiComponent the wiki component to unregister
     * @param componentManager the Component Manager the wiki component has been registered in
     * @return {@code true} if a component has been removed from the Component Manager, {@code false} otherwise
     */
    private boolean unregisterFromComponentManager(WikiComponent wikiComponent, ComponentManager componentManager)
    {
        Type roleType = wikiComponent.getRoleType();
        String roleHint = wikiComponent.getRoleHint();

        Object registeredInstance;
        try {
            registeredInstance = componentManager.getInstance(roleType, roleHint);
        } catch (ComponentLookupException e) {
            registeredInstance = null;
        }

        boolean result = false;
        if (registeredInstance == wikiComponent) {
            componentManager.unregisterComponent(roleType, roleHint);
            result = true;
        } else if (!isReplacedBy(wikiComponent, registeredInstance)) {
            // The lookup returns a component of a parent Component Manager when it has a higher priority, while the
            // wiki component might still be registered, hidden, in its own Component Manager. Unregistering through
            // the descriptor only removes the component registered in that Component Manager, and only if it has the
            // same descriptor, i.e., if it's a wiki component of the same type and priority and not, e.g., a Java
            // component that replaced it.
            ComponentDescriptor<?> descriptor = createComponentDescriptor(roleType, wikiComponent);
            if (descriptor.equals(componentManager.getComponentDescriptor(roleType, roleHint))) {
                componentManager.unregisterComponent(descriptor);
                result = true;
            }
        }

        return result;
    }

    /**
     * @param wikiComponent a wiki component
     * @param registeredInstance the component currently registered for the role type and hint of the wiki component
     * @return {@code true} if the passed instance is another wiki component that replaced the passed one in the same
     *         Component Manager
     */
    private boolean isReplacedBy(WikiComponent wikiComponent, Object registeredInstance)
    {
        return registeredInstance instanceof WikiComponent registeredWikiComponent
            && isSameComponentManagerKey(wikiComponent, registeredWikiComponent);
    }

    /**
     * @param unregisteredComponent the wiki component that has just been unregistered from the Component Manager
     * @return the best wiki component that has been replaced by the passed one in the same Component Manager, i.e.,
     *         that has the same role type, role hint and scope, isn't defined in the same entity and targets the same
     *         wiki (wiki scope) or user (user scope), or {@code null} if there is none
     */
    private WikiComponent getShadowedComponent(WikiComponent unregisteredComponent)
    {
        return this.registeredComponents.entrySet().stream()
            .filter(entry -> !entry.getKey().equals(unregisteredComponent.getEntityReference()))
            .flatMap(entry -> entry.getValue().stream())
            .filter(candidate -> isSameComponentManagerKey(unregisteredComponent, candidate))
            .min(Comparator.comparingInt(WikiComponent::getRoleHintPriority))
            .orElse(null);
    }

    private boolean isSameComponentManagerKey(WikiComponent component, WikiComponent candidate)
    {
        boolean result = component.getScope() == candidate.getScope()
            && component.getRoleType().equals(candidate.getRoleType())
            && Objects.equals(component.getRoleHint(), candidate.getRoleHint());

        if (result) {
            result = switch (component.getScope()) {
                case USER -> Objects.equals(component.getAuthorReference(), candidate.getAuthorReference());
                case WIKI -> Objects.equals(component.getEntityReference().extractReference(EntityType.WIKI),
                    candidate.getEntityReference().extractReference(EntityType.WIKI));
                default -> true;
            };
        }

        return result;
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

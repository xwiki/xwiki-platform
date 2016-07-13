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

import javax.inject.Inject;

import org.xwiki.component.internal.multi.AbstractGenericComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Proxy Component Manager that creates and queries individual Component Managers specific to the current entity in the
 * Execution Context. These Component Managers are created on the fly the first time a component is registered for the
 * current entity.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractEntityComponentManager extends AbstractGenericComponentManager implements Initializable
{
    private static class EntityComponentManagerInstance
    {
        protected final EntityReference entityReference;

        protected final ComponentManager componentManager;

        EntityComponentManagerInstance(EntityReference entityReference, ComponentManager componentManager)
        {
            this.entityReference = entityReference;
            this.componentManager = componentManager;
        }
    }

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Execution execution;

    /**
     * The key used to access the cache {@link ComponentManager} in the {@link ExecutionContext}.
     */
    private final String contextKey = getClass().getName();

    protected abstract EntityReference getCurrentReference();

    @Override
    protected ComponentManager getComponentManagerInternal()
    {
        // Get current user reference
        EntityReference entityReference = getCurrentReference();
        if (entityReference == null) {
            return null;
        }

        ExecutionContext econtext = this.execution.getContext();

        // If there is no context don't try to find or register the component manager
        if (econtext == null) {
            return super.getComponentManagerInternal();
        }

        // Try to find the user component manager in the context
        EntityComponentManagerInstance contextComponentManager =
            (EntityComponentManagerInstance) econtext.getProperty(this.contextKey);
        if (contextComponentManager != null && contextComponentManager.entityReference.equals(entityReference)) {
            return contextComponentManager.componentManager;
        }

        // Fallback on regular user component manager search
        ComponentManager componentManager = super.getComponentManagerInternal();

        // Cache the component manager
        econtext.setProperty(this.contextKey, new EntityComponentManagerInstance(entityReference, componentManager));

        return componentManager;
    }

    @Override
    protected String getKey()
    {
        EntityReference reference = getCurrentReference();

        return reference != null ? reference.getType().getLowerCase() + ':' + this.serializer.serialize(reference)
            : null;
    }

    protected void onComponentAdded()
    {
        ExecutionContext econtext = this.execution.getContext();

        // Remove any null cached ComponentManager in the context
        // Note: namespace based component managers aren't deleted yet so the opposite is not needed but it could
        // change)
        if (econtext != null) {
            EntityComponentManagerInstance contextComponentManager =
                (EntityComponentManagerInstance) econtext.getProperty(this.contextKey);
            if (contextComponentManager != null && contextComponentManager.componentManager == null) {
                econtext.removeProperty(this.contextKey);
            }
        }
    }
}

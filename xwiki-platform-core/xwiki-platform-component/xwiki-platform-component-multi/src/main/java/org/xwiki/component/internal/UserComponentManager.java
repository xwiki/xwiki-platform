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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.AbstractGenericComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Proxy Component Manager that creates and queries individual Component Managers specific to the current user in the
 * Execution Context. These Component Managers are created on the fly the first time a component is registered for the
 * current user.
 * 
 * @version $Id$
 * @since 2.1RC1
 */
@Component
@Named(UserComponentManager.ID)
@Singleton
public class UserComponentManager extends AbstractGenericComponentManager implements Initializable
{
    /**
     * The identifier of this {@link ComponentManager}.
     */
    public static final String ID = "user";

    private static final String KEY_PREFIX = ID + ':';

    private static final String CONTEXT_KEY = UserComponentManager.class.getName();

    private static class UserComponentManagerInstance
    {
        protected final DocumentReference userReference;

        protected final ComponentManager componentManager;

        public UserComponentManagerInstance(DocumentReference userReference, ComponentManager componentManager)
        {
            this.userReference = userReference;
            this.componentManager = componentManager;
        }
    }

    /**
     * Used to access the current user in the Execution Context.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the user reference.
     */
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    /**
     * The Component Manager to be used as parent when a component is not found in the current Component Manager.
     */
    @Inject
    @Named(DocumentComponentManager.ID)
    private ComponentManager documentComponentManager;

    @Inject
    private Execution execution;

    @Override
    public void initialize() throws InitializationException
    {
        // Set the parent to the Wiki Component Manager since if a component isn't found for a particular user
        // we want to check if it's available in the current wiki and if not then in the Wiki Component Manager's
        // parent.
        setInternalParent(this.documentComponentManager);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Speed up a bit component manager resolution by keeping it in the execution context.
     * 
     * @see org.xwiki.component.internal.multi.AbstractGenericComponentManager#getComponentManagerInternal()
     */
    @Override
    public ComponentManager getComponentManagerInternal()
    {
        // Get current user reference
        DocumentReference userReference = this.documentAccessBridge.getCurrentUserReference();
        if (userReference == null) {
            return null;
        }

        // Try to find the user component manager in the context
        ExecutionContext econtext = this.execution.getContext();
        UserComponentManagerInstance contextComponentManager =
            (UserComponentManagerInstance) econtext.getProperty(CONTEXT_KEY);
        if (contextComponentManager != null && contextComponentManager.userReference == userReference) {
            return contextComponentManager.componentManager;
        }

        // Fallback on regular user component manager search
        ComponentManager componentManager = super.getComponentManagerInternal();
        econtext.setProperty(CONTEXT_KEY, new UserComponentManagerInstance(userReference, componentManager));

        return componentManager;
    }

    @Override
    protected String getKey()
    {
        DocumentReference userReference = this.documentAccessBridge.getCurrentUserReference();

        return userReference != null ? KEY_PREFIX + this.referenceSerializer.serialize(userReference) : null;
    }
}

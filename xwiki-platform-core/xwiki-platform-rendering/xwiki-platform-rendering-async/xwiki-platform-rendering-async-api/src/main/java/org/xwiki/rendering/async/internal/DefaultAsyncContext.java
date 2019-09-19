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
package org.xwiki.rendering.async.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentRole;
import org.xwiki.component.descriptor.DefaultComponentRole;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContext.DeclarationBuilder;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link AsyncContext}.
 * 
 * @version $Id$$
 * @since 10.10RC1
 */
@Component
@Singleton
public class DefaultAsyncContext implements AsyncContext
{
    /**
     * Gather a right check (right, user, entity).
     * 
     * @version $Id$
     * @since 11.8RC1
     */
    public static class RightEntry
    {
        private final Right right;

        private final DocumentReference userReference;

        private final EntityReference entityReference;

        private final boolean allowed;

        /**
         * @param right the right needed for execution of the action
         * @param userReference the user to check the right for
         * @param entityReference the entity on which to check the right
         * @param result the result of the evaluation
         */
        public RightEntry(Right right, DocumentReference userReference, EntityReference entityReference, boolean result)
        {
            this.right = right;
            this.userReference = userReference;
            this.entityReference = entityReference;
            this.allowed = result;
        }

        /**
         * @return the right needed for execution of the action
         */
        public Right getRight()
        {
            return this.right;
        }

        /**
         * @return the user to check the right for
         */
        public DocumentReference getUserReference()
        {
            return this.userReference;
        }

        /**
         * @return the entity on which to check the right
         */
        public EntityReference getEntityReference()
        {
            return this.entityReference;
        }

        /**
         * @return the result of the evaluation
         */
        public boolean isAllowed()
        {
            return this.allowed;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (obj == this) {
                return true;
            }

            if (obj instanceof RightEntry) {
                EqualsBuilder builder = new EqualsBuilder();

                builder.append(this.right, ((RightEntry) obj).right);
                builder.append(this.userReference, ((RightEntry) obj).userReference);
                builder.append(this.entityReference, ((RightEntry) obj).entityReference);

                return builder.build();
            }

            return false;
        }

        @Override
        public int hashCode()
        {
            HashCodeBuilder builder = new HashCodeBuilder();

            builder.append(this.right);
            builder.append(this.userReference);
            builder.append(this.entityReference);

            return builder.build();
        }

        @Override
        public String toString()
        {
            StringBuilder builder = new StringBuilder();

            builder.append('{');
            builder.append("right=").append(this.right);
            builder.append(", user=").append(this.userReference);
            builder.append(", entity=").append(this.entityReference);
            builder.append(", allowed=").append(this.allowed);
            builder.append('}');

            return builder.toString();
        }
    }

    /**
     * Remember the entities and components manipulated during an execution.
     * 
     * @version $Id$
     */
    public static class ContextUse
    {
        private final Set<EntityReference> references = new HashSet<>();

        private final Set<Type> roleTypes = new HashSet<>();

        private final Set<ComponentRole<?>> roles = new HashSet<>();

        private final Set<RightEntry> rights = new HashSet<>();

        private final Map<String, Collection<Object>> uses = new HashMap<>();

        /**
         * @return the references
         */
        public Set<EntityReference> getReferences()
        {
            return this.references;
        }

        /**
         * @return the roleTypes
         */
        public Set<Type> getRoleTypes()
        {
            return this.roleTypes;
        }

        /**
         * @return the roles
         */
        public Set<ComponentRole<?>> getRoles()
        {
            return this.roles;
        }

        /**
         * @return the rights
         */
        public Set<RightEntry> getRights()
        {
            return this.rights;
        }

        /**
         * @return the custom values associated with a cached execution result
         */
        public Map<String, Collection<Object>> getUses()
        {
            return this.uses;
        }
    }

    private static final String KEY_ENABLED = "rendering.async.enabled";

    private static final String KEY_CONTEXTUSE = "rendering.async.contextuse";

    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceFactory factory;

    @Override
    public boolean isEnabled()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            return econtext.getProperty(KEY_ENABLED) == Boolean.TRUE;
        }

        return false;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        this.execution.getContext().setProperty(KEY_ENABLED, enabled);
    }

    /**
     * Push a new {@link ContextUse} in the context.
     */
    public void pushContextUse()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            Deque<ContextUse> deque = (Deque<ContextUse>) econtext.getProperty(KEY_CONTEXTUSE);

            if (deque == null) {
                deque = new LinkedList<>();
                DeclarationBuilder propertyBuilder = econtext.newProperty(KEY_CONTEXTUSE);
                propertyBuilder.inherited();
                propertyBuilder.makeFinal();
                propertyBuilder.initial(deque);
                propertyBuilder.declare();
            }

            deque.push(new ContextUse());
        }
    }

    /**
     * @return the current {@link ContextUse}
     */
    public ContextUse popContextUse()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            Deque<ContextUse> deque = (Deque<ContextUse>) econtext.getProperty(KEY_CONTEXTUSE);

            if (deque != null && !deque.isEmpty()) {
                return deque.pop();
            }
        }

        return null;
    }

    private ContextUse getContextUse()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            Deque<ContextUse> deque = (Deque<ContextUse>) econtext.getProperty(KEY_CONTEXTUSE);

            if (deque != null && !deque.isEmpty()) {
                return deque.peek();
            }
        }

        return null;
    }

    @Override
    public void useEntity(EntityReference reference)
    {
        ContextUse contextUse = getContextUse();

        if (contextUse != null) {
            contextUse.references.add(this.factory.getReference(reference));
        }
    }

    @Override
    public void useComponent(Type roleType)
    {
        ContextUse contextUse = getContextUse();

        if (contextUse != null) {
            contextUse.roleTypes.add(roleType);
        }
    }

    @Override
    public void useComponent(Type roleType, String roleHint)
    {
        ContextUse contextUse = getContextUse();

        if (contextUse != null) {
            contextUse.roles.add(new DefaultComponentRole<>(roleType, roleHint));
        }
    }

    @Override
    public void useRight(Right right, DocumentReference userReference, EntityReference entityReference, boolean allowed)
    {
        ContextUse contextUse = getContextUse();

        if (contextUse != null) {
            contextUse.rights.add(new RightEntry(right, userReference, entityReference, allowed));
        }
    }

    @Override
    public void use(String type, Object value)
    {
        ContextUse contextUse = getContextUse();

        if (contextUse != null) {
            Collection<Object> values = contextUse.uses.get(type);

            if (values == null) {
                values = new ArrayList<>();
                contextUse.uses.put(type, values);
            }

            values.add(value);
        }
    }
}

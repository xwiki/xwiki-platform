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

import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Base class helper to create entity related {@link ComponentManager}.
 * 
 * @version $Id$
 * @since 8.4RC1
 */
public abstract class AbstractEnityComponentManagerFactory extends AbstractComponentManagerFactory
{
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> resolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private ComponentManagerManager manager;

    protected abstract EntityType getEntityType();

    @Override
    public ComponentManager createComponentManager(String namespace, ComponentManager parentComponentManager)
    {
        // Get entity reference
        EntityReference reference =
            this.resolver.resolve(namespace.substring(getEntityType().getLowerCase().length() + 1), getEntityType());

        // Get parent reference
        EntityReference parentReference = reference.getParent();

        ComponentManager parent;
        if (parentReference != null) {
            // Get parent namespace
            String parentNamespace =
                parentReference.getType().getLowerCase() + ':' + this.serializer.serialize(parentReference);

            // Get parent component manager
            parent = this.manager.getComponentManager(parentNamespace, true);
        } else {
            parent = parentComponentManager;
        }

        return this.defaultFactory.createComponentManager(namespace, parent);

    }
}

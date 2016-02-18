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
package org.xwiki.rendering.internal.resolver;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Default entry point to convert a resource reference into an entity reference.
 * 
 * @version $Id$
 * @since 7.4.1
 */
@Component
@Singleton
public class DefaultResourceReferenceEntityReferenceResolver implements EntityReferenceResolver<ResourceReference>
{
    /**
     * Type instance for EntityReferenceResolver<ResourceReference>.
     */
    public static final ParameterizedType TYPE_RESOURCEREFERENCE =
        new DefaultParameterizedType(null, EntityReferenceResolver.class, ResourceReference.class);

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public EntityReference resolve(ResourceReference resourceReference, EntityType type, Object... parameters)
    {
        if (resourceReference == null) {
            return null;
        }

        if (this.componentManagerProvider.get().hasComponent(TYPE_RESOURCEREFERENCE,
            resourceReference.getType().getScheme())) {
            EntityReferenceResolver<ResourceReference> resolver;
            try {
                resolver = this.componentManagerProvider.get().getInstance(TYPE_RESOURCEREFERENCE,
                    resourceReference.getType().getScheme());
            } catch (ComponentLookupException e) {
                throw new RuntimeException(
                    String.format("Unknown error when trying to load resolver for reference [%s]", resourceReference),
                    e);
            }

            return resolver.resolve(resourceReference, type, parameters);
        }

        // Unsupported resource reference type
        return null;
    }
}

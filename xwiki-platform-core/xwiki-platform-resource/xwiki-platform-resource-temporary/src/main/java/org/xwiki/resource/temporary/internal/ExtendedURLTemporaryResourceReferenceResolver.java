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
package org.xwiki.resource.temporary.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Resolves a {@link TemporaryResourceReference} from an {@link ExtendedURL}. The following URL format is expected:
 * {@code http://<server>/<context>/tmp/<module id>/<owning entity reference>/<module-dependent resource path>}.
 * 
 * @version $Id$
 * @since 7.4.6
 * @since 8.2.2
 * @since 8.3
 */
@Component
@Named("standard/tmp")
@Singleton
public class ExtendedURLTemporaryResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Inject
    @Named("url")
    private EntityReferenceResolver<String> urlEntityReferenceResolver;

    @Override
    public TemporaryResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        TemporaryResourceReference reference;
        List<String> segments = extendedURL.getSegments();

        if (segments.size() < 3) {
            throw new CreateResourceReferenceException(
                String.format("Invalid temporary resource URL format [%s].", extendedURL.toString()));
        } else {
            // The first segment is the module id.
            String moduleId = segments.get(0);
            // The second segment is the serialized owning entity reference. This is used to check access rights.
            EntityReference owningEntityReference = resolveEntityReference(segments.get(1));
            // The other segments point to the resource path.
            List<String> resourcePath = segments.subList(2, segments.size());
            reference = new TemporaryResourceReference(moduleId, resourcePath, owningEntityReference);
            copyParameters(extendedURL, reference);
        }
        return reference;
    }

    private EntityReference resolveEntityReference(String representation) throws CreateResourceReferenceException
    {
        int index = representation.indexOf(':');
        if (index > 0) {
            String entityTypeString = representation.substring(0, index);
            try {
                EntityType entityType = EntityType.valueOf(entityTypeString.toUpperCase());
                return this.urlEntityReferenceResolver.resolve(representation.substring(index + 1), entityType);
            } catch (Exception e) {
                throw new CreateResourceReferenceException(
                    String.format("Unknown entity type [%s].", entityTypeString));
            }
        } else {
            throw new CreateResourceReferenceException(
                String.format("Entity type is missing from [%s].", representation));
        }
    }
}

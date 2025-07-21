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
package org.xwiki.url.internal;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Helper class to implement a Serializer to transform a XWiki Resource into a {@link ExtendedURL} object.
 * 
 * @version $Id$
 * @since 7.2M1
 */
public abstract class AbstractExtendedURLResourceReferenceSerializer
    implements ResourceReferenceSerializer<ResourceReference, ExtendedURL>
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Transforms a Resource Reference into some other representation.
     *
     * @param reference the Resource Reference to transform
     * @param formatId the id of the URL format to use (e.g. "standard", "reference", etc)
     * @return the new representation
     * @throws SerializeResourceReferenceException if there was an error while serializing the XWiki Resource object
     * @throws UnsupportedResourceReferenceException if the passed representation points to an unsupported Resource
     *         Reference type that we don't know how to serialize
     */
    public ExtendedURL serialize(ResourceReference reference, String formatId)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ResourceReferenceSerializer<ResourceReference, ExtendedURL> serializer;
        DefaultParameterizedType roleType = new DefaultParameterizedType(null,
            ResourceReferenceSerializer.class, reference.getClass(), ExtendedURL.class);
        try {
            // Step 1: Try to locate a Serializer registered for the specific passed resource type and for the passed
            // URL format id
            if (this.componentManager.hasComponent(roleType, formatId)) {
                serializer = this.componentManager.getInstance(roleType, formatId);
            } else {
                // Step 2: Try to locate a Serializer registered only for the specific passed resource type and for all
                // URL format ids
                serializer = this.componentManager.getInstance(roleType);
            }
        } catch (ComponentLookupException e) {
            throw new UnsupportedResourceReferenceException(String.format(
                "Failed to find serializer for Resource Reference [%s] and URL format [%s]", reference, formatId),
                e);
        }

        return serializer.serialize(reference);
    }
}

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
package org.xwiki.url.internal.standard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * Transforms a XWiki Resource instance into a {@link ExtendedURL} object. Note that the serialization
 * performs URL-encoding wherever necessary to generate a valid URL (see http://www.ietf.org/rfc/rfc2396.txt).
 * 
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("standard")
@Singleton
public class StandardExtendedURLResourceReferenceSerializer
    implements ResourceReferenceSerializer<ResourceReference, ExtendedURL>
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public ExtendedURL serialize(ResourceReference reference) throws UnsupportedResourceReferenceException
    {
        // First, try to locate a URL Serializer registered only for this URL scheme
        ResourceReferenceSerializer<ResourceReference, ExtendedURL> serializer;
        try {
            serializer = this.componentManager.getInstance(
                new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                    reference.getClass(), ExtendedURL.class),
                        String.format("standard/%s", reference.getType().getId()));
        } catch (ComponentLookupException e) {
            // Second, if not found, try to locate a URL Serializer registered for all URL schemes
            try {
                serializer = this.componentManager.getInstance(
                    new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                        reference.getClass(), ExtendedURL.class), reference.getType().getId());
            } catch (ComponentLookupException cle) {
                throw new UnsupportedResourceReferenceException(String.format(
                    "Failed to find serializer for Resource Reference [%s]", reference, cle));
            }
        }

        return serializer.serialize(reference);
    }
}

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
package org.xwiki.vfs.internal;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Serializer which transforms a {@link VfsResourceReference} into a {@link URI} by looking for a URI scheme-specific
 * Serializer and if none is found then returning the URI from the {@link VfsResourceReference} as is.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("truevfs")
@Singleton
public class URIVfsResourceReferenceSerializer implements ResourceReferenceSerializer<VfsResourceReference, URI>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public URI serialize(VfsResourceReference reference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        URI resultURI;

        try {
            ResourceReferenceSerializer<VfsResourceReference, URI> serializer =
                this.componentManagerProvider.get().getInstance(new DefaultParameterizedType(null,
                        ResourceReferenceSerializer.class, VfsResourceReference.class, URI.class),
                    String.format("truevfs/%s", reference.getURI().getScheme()));
            resultURI = serializer.serialize(reference);
        } catch (ComponentLookupException e) {
            // No serializer exist, we just don't perform any conversion!
            resultURI = reference.toURI();
        }

        return resultURI;
    }
}

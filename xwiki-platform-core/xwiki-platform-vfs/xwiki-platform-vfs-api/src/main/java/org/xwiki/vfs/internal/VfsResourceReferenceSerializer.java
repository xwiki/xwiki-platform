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

import java.lang.reflect.Type;
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
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Converts a {@link VfsResourceReference} into a relative {@link ExtendedURL} (with the Context Path added).
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class VfsResourceReferenceSerializer extends AbstractVfsResourceReferenceSerializer
{
    private static final Type TYPE = new DefaultParameterizedType(null,
        ResourceReferenceSerializer.class, VfsResourceReference.class, ExtendedURL.class);

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public ExtendedURL serialize(VfsResourceReference resourceReference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ExtendedURL extendedURL;

        // We need to ensure that the URI contains absolute references since the VFS handler that will handle
        // the generated URL won't have any base reference to resolve any relative refeence.
        //
        // Look for a scheme-specific serializer to convert from a relative URI into an absolute URI and if not found
        // then don't perform any transformation of the URI.
        URI uri = resourceReference.getURI();
        try {
            ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> schemeSpecificSerializer =
                this.componentManagerProvider.get().getInstance(TYPE, uri.getScheme());
            extendedURL = schemeSpecificSerializer.serialize(resourceReference);
        } catch (ComponentLookupException e) {
            extendedURL = super.serialize(resourceReference);
        }

        return extendedURL;
    }

    @Override
    protected URI makeAbsolute(URI uri)
    {
        // Don't make any transformation by default
        return uri;
    }
}

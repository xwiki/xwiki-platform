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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Helper to implemet scheme-specific serializers to Converts a {@link VfsResourceReference} into a relative
 * {@link ExtendedURL} (with the Context Path added).
 *
 * @version $Id$
 * @since 7.4M2
 */
public abstract class AbstractVfsResourceReferenceSerializer
    implements ResourceReferenceSerializer<VfsResourceReference, ExtendedURL>
{
    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Override
    public ExtendedURL serialize(VfsResourceReference resourceReference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> segments = new ArrayList<>();

        // Add the resource type segment.
        segments.add("vfs");

        // Add the VFS URI part
        segments.add(makeAbsolute(resourceReference.getURI()).toString());

        // Add the VFS path
        segments.addAll(resourceReference.getPathSegments());

        // Add all optional parameters
        ExtendedURL extendedURL = new ExtendedURL(segments, resourceReference.getParameters());

        // Normalize the URL to add the Context Path since we want a full relative URL to be returned.
        return this.extendedURLNormalizer.normalize(extendedURL);
    }

    /**
     * Converts the passed URI into a URI containing an absolute reference to the VFS.
     *
     * @param uri the URI containing a relative reference to the VFS
     * @return a URI with an absolute reference to the VFS
     */
    protected abstract URI makeAbsolute(URI uri);
}

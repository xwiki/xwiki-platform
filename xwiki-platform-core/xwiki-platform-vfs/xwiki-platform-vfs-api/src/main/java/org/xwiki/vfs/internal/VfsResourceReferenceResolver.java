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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Transform VFS URLs into a typed Resource Reference. The URL format handled is {@code http://server/<servlet
 * context>/vfs/<vfs reference as URI>/path/inside/zip}. For example:
 * <ul>
 *   <li>{@code http://localhost:8080/xwiki/vfs/encoded(attach:space.page@attachment)/some/path/file.txt}.</li>
 *   <li>{@code http://localhost:8080/xwiki/vfs/encoded(http://server/path/to/zip)/some/path/file.txt}.</li>
 *   <li>{@code http://localhost:8080/xwiki/vfs/encoded(file://server/path/to/zip)/some/path/file.txt}.</li>
 * </ul>
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("vfs")
@Singleton
public class VfsResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> referenceResolver;

    @Override
    public VfsResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> segments = extendedURL.getSegments();

        // First segment is the url-encoded VFS reference, defined as URI
        URI vfsUri;
        try {
            vfsUri = new URI(segments.get(0));
        } catch (URISyntaxException e) {
            throw new CreateResourceReferenceException(
                String.format("Invalid VFS URI [%s] for URL [%s]", segments.get(0), extendedURL));
        }

        // Other segments are the path to the archive resource
        List<String> vfsPathSegments = new ArrayList<>(segments);
        vfsPathSegments.remove(0);

        VfsResourceReference vfsReference = new VfsResourceReference(vfsUri, vfsPathSegments);
        copyParameters(extendedURL, vfsReference);
        return vfsReference;
    }
}

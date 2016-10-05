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

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

import net.java.truevfs.access.TPath;

/**
 * Handles VFS Resource References by outputting in the Container's response the resource pointed to in the archive
 * file defined in the URL.
 *
 * @version $Id$
 * @see VfsResourceReferenceResolver for the URL format handled
 * @since 7.4M2
 */
@Component
@Named("vfs")
@Singleton
public class VfsResourceReferenceHandler extends AbstractContentResourceReferenceHandler
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("cascading")
    private VfsPermissionChecker permissionChecker;

    @Inject
    @Named("truevfs")
    private ResourceReferenceSerializer<VfsResourceReference, URI> trueVfsResourceReferenceSerializer;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(VfsResourceReference.TYPE);
    }

    @Override
    public void handle(ResourceReference resourceReference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        // This code only handles VFS Resource References.
        VfsResourceReference vfsResourceReference = (VfsResourceReference) resourceReference;

        try {
            // Verify that the user has the permission for the specified VFS scheme and for the VFS URI
            this.permissionChecker.checkPermission(vfsResourceReference);

            // Extract the asked resource from inside the zip and return its content for display.

            // We need to convert the VFS Resource Reference into a hierarchical URI supported by TrueVFS
            URI trueVFSURI = convertResourceReference(vfsResourceReference);

            // We use TrueVFS. This line will automatically use the VFS Driver that matches the scheme passed in the URI
            Path path = new TPath(trueVFSURI);
            try (InputStream in = Files.newInputStream(path)) {
                List<String> pathSegments = vfsResourceReference.getPathSegments();
                serveResource(pathSegments.get(pathSegments.size() - 1), in);
            }
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to extract resource [%s]", vfsResourceReference), e);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(vfsResourceReference);
    }

    private URI convertResourceReference(VfsResourceReference reference) throws ResourceReferenceHandlerException
    {
        URI resultURI;

        try {
            resultURI = this.trueVfsResourceReferenceSerializer.serialize(reference);
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to convert VFS URI [%s] into a valid FS format", reference), e);
        }

        return resultURI;
    }
}

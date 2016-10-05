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
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.Converter;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPathFactory;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

import net.java.truevfs.access.TPath;

/**
 * Generate a {@link Path} instance but also does 2 things:
 * <ul>
 *     <li>Transform the URI into a hierarchical URI (e.g. from {@code attach:Sandbox.WebHome@my.zip/some/path} to
 *         {@code attach://Sandbox.WebHome@my.zip/some/path}</li>
 *     <li>Verify that the current user has the permissions to access that URI</li>
 * </ul>
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Singleton
public class DefaultVfsPathFactory implements VfsPathFactory
{
    @Inject
    private VfsPermissionChecker permissionChecker;

    @Inject
    private Converter<VfsResourceReference> vfsResourceReferenceConverter;

    @Inject
    @Named("truevfs")
    private ResourceReferenceSerializer<VfsResourceReference, URI> trueVfsResourceReferenceSerializer;

    @Override
    public Path create(URI uri) throws VfsException
    {
        try {
            VfsResourceReference reference =
                this.vfsResourceReferenceConverter.convert(VfsResourceReference.class, uri.toString());
            this.permissionChecker.checkPermission(reference);
            URI trueVFSURI = this.trueVfsResourceReferenceSerializer.serialize(reference);
            return new TPath(trueVFSURI);
        } catch (Exception e) {
            throw new VfsException(String.format("Failed to create Path instance for [%s]", uri.toString()), e);
        }
    }
}

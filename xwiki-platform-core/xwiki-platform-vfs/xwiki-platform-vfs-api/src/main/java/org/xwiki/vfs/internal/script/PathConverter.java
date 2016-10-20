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
package org.xwiki.vfs.internal.script;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

import net.java.truevfs.access.TPath;

/**
 * Converts {@link String} into {@link Path} objects constructed using TrueVFS. Permissions are also checked to verify
 * that the current users has the right to access the specified FileSystem Provider.
 * <p>
 * See {@link VfsResourceReferenceConverter} for example input and how the String input is first converted to a
 * {@link VfsResourceReference} and then into a TrueVFS-compatible URI suitable for TrueVFS.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class PathConverter extends AbstractConverter<Path>
{
    @Inject
    @Named("cascading")
    private VfsPermissionChecker permissionChecker;

    @Inject
    @Named("truevfs")
    private ResourceReferenceSerializer<VfsResourceReference, URI> trueVfsResourceReferenceSerializer;

    @Override
    protected Path convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        Path path;

        try {
            VfsResourceReference reference = new VfsResourceReference(new URI(value.toString()));

            // Verify that the user has the permission for the specified VFS scheme. We need to do this at this level
            // since it's possible to do the check in the driver itself since TrueVFS controls whether the driver is
            // called or not and does caching,
            // see https://java.net/projects/truezip/lists/users/archive/2015-12/message/8
            // Since this convert has to be called to use the VFS API from Velocity, we're safe that this will prevent
            // any Velocity script to execute a VFS call if the user is not allowed.
            //
            // Note: Even though the user needs View access to the attachment, we cannot check this right now because
            // of the caching issue. However we consider that if the user has Programming Rights, he can do anything he
            // wants and thus it's safe that he can access the attachment.
            this.permissionChecker.checkPermission(reference);

            URI trueVfsURI = this.trueVfsResourceReferenceSerializer.serialize(reference);
            path = new TPath(trueVfsURI);
        } catch (Exception e) {
            throw new ConversionException(String.format("Failed to convert [%s] to a Path object", value), e);
        }

        return path;
    }
}

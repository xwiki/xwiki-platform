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
package org.xwiki.vfs.script;

import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.vfs.VfsManager;
import org.xwiki.vfs.internal.VfsResourceReference;
import org.xwiki.vfs.internal.script.WrappingDirectoryStream;

/**
 * Offers scripting APIs for the VFS module.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("vfs")
@Singleton
@Unstable
public class VfsScriptService implements ScriptService
{
    @Inject
    private VfsManager vfsManager;

    /**
     * Generate a relative VFS URL to access a resource inside an archive.
     *
     * @param resourceReference the string representation of a VFS resource reference which defines the location of an
     * archive. For example {@code attach:space.page@my.zip}.
     * @param pathInArchive the path of the resource inside the archive for which to generate a URL for. For example
     * {@code /some/path/in/archive/test.txt}.
     * @return a relative URL that can be used to access the content of a file inside an archive (ZIP, EAR, TAR.GZ, etc)
     */
    public String url(String resourceReference, String pathInArchive)
    {
        try {
            VfsResourceReference vfsResourceReference =
                new VfsResourceReference(new URI(resourceReference), pathInArchive);
            return this.vfsManager.getURL(vfsResourceReference);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * List all entries inside the referenced archive, starting at the specified path and applying the passed Filter.
     * <p/>
     * WARNING: <b>it's important that the caller closes the stream or use a try-with-resource construct</b>
     *
     * @param resourceReference the reference to the archive (e.g. {@code attach:Sandbox.WebHome@my.zip})
     * @param pathInArchive the starting path in that archive (e.g {@code /})
     * @param filter the NIO2 filter to apply
     * @return a {@link DirectoryStream} containing the result or null if an error occurred. Note that this method
     *         doesn't recurse into directories
     */
    public DirectoryStream<Path> getPaths(String resourceReference, String pathInArchive,
        DirectoryStream.Filter<Path> filter)
    {
        try {
            VfsResourceReference vfsResourceReference =
                new VfsResourceReference(new URI(resourceReference), pathInArchive);
            return new WrappingDirectoryStream(this.vfsManager.getPaths(vfsResourceReference, filter));
        } catch (Exception e) {
            return null;
        }
    }
}

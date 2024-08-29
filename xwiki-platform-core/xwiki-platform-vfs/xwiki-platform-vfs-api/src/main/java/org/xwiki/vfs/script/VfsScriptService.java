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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.vfs.VfsManager;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Offers scripting APIs for the VFS module.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("vfs")
@Singleton
public class VfsScriptService implements ScriptService
{
    @Inject
    private VfsManager vfsManager;

    /**
     * Generate a relative VFS URL to access a resource inside an archive.
     *
     * @param reference the reference to a file inside a an archive.
     *                  For example {@code attach:space.page@my.zip/path/to/file}.
     * @return a relative URL that can be used to access the content of a file inside an archive (ZIP, EAR, TAR.GZ, etc)
     *         or null if the URL couldn't be constructed
     */
    public String url(VfsResourceReference reference)
    {
        try {
            return this.vfsManager.getURL(reference);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate a relative VFS URL to access a resource inside an archive.
     *
     * @param reference the reference to a file inside a an archive. For example
     *            {@code attach:space.page@my.zip/path/to/file}.
     * @param contentType the Content-Type to return with the response
     * @return a relative URL that can be used to access the content of a file inside an archive (ZIP, EAR, TAR.GZ, etc)
     *         or null if the URL couldn't be constructed
     * @since 12.3RC1
     */
    public String url(VfsResourceReference reference, String contentType)
    {
        VfsResourceReference finalReference = new VfsResourceReference(reference);
        finalReference.setContentType(contentType);

        return url(finalReference);
    }
}

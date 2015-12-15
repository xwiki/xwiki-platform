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
package org.xwiki.vfs;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * API to construct a VFS URL or access the content of an archive.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Role
@Unstable
public interface VfsManager
{
    /**
     * Generate a relative VFS URL to access a resource inside an archive.
     *
     * @param reference the reference to a file inside a an archive.
     *                  For example {@code attach:space.page@my.zip/path/to/file}.
     * @return a relative URL that can be used to access the content of a file inside an archive (ZIP, EAR, TAR.GZ, etc)
     * @exception VfsException if an error occurs computing the URL
     */
    String getURL(VfsResourceReference reference) throws VfsException;
}

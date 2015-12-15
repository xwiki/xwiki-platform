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
 * Verify if the user has permissions to access the VFS asked. For example it would be dangerous to let a user
 * access the File VFS where the XWiki instance is running.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Role
@Unstable
public interface VfsPermissionChecker
{
    /**
     * @param resourceReference the VFS path that the user wishes to access. This includes the VFS URI scheme.
     * @throws VfsException in case the permission is refused or any error happens when checking for permission
     */
    void checkPermission(VfsResourceReference resourceReference) throws VfsException;
}

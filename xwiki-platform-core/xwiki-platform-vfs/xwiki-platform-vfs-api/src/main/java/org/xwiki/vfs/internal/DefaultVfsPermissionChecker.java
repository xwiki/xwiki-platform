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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Generic Permission checked used when there's no scheme-specific Permission Checker and that verifies that the current
 * user has Programming Rights.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultVfsPermissionChecker implements VfsPermissionChecker
{
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Override
    public void checkPermission(VfsResourceReference resourceReference) throws VfsException
    {
        // By default we only allow VFS access when the current user has Programming Rights, for security reason.
        // Without this a wiki user could access the local filesystem for example by using the File URI scheme.
        if (!this.authorizationManager.hasAccess(Right.PROGRAM)) {
            throw new VfsException(String.format(
                "Current logged-in user needs to have Programming Rights to use the [%s] VFS",
                resourceReference.getURI().getScheme()));
        }
    }
}

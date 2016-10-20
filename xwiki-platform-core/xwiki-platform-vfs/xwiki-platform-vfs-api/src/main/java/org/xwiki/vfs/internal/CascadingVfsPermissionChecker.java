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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Verify VFS permission by first looking for a Permission Checker specific to a VFS URI scheme and if not found,
 * default to the generic Permission Checker.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named(CascadingVfsPermissionChecker.HINT)
@Singleton
public class CascadingVfsPermissionChecker implements VfsPermissionChecker
{
    static final String HINT = "cascading";

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public void checkPermission(VfsResourceReference resourceReference) throws VfsException
    {
        // Prevent using a VFS scheme that correspond to the hint of this component
        String scheme = resourceReference.getURI().getScheme();
        if (HINT.equals(scheme)) {
            throw new VfsException(String.format("[%s] is a reserved VFS URI scheme and cannot be used.", HINT));
        }

        // Look for a scheme-specific permission checker
        VfsPermissionChecker resolvedChecker;
        ComponentManager componentManager = this.componentManagerProvider.get();
        try {
            resolvedChecker =
                componentManager.getInstance(VfsPermissionChecker.class, scheme);
        } catch (ComponentLookupException e) {
            // Use the Generic permission checker
            try {
                resolvedChecker = componentManager.getInstance(VfsPermissionChecker.class);
            } catch (ComponentLookupException ee) {
                throw new VfsException(String.format("No VFS Permission Checked has been found in the system. "
                    + "Refusing access to VFS URI scheme [%s]", scheme), ee);
            }
        }
        resolvedChecker.checkPermission(resourceReference);
    }
}

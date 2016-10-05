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
package org.xwiki.vfs.internal.attach;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsPermissionChecker;
import org.xwiki.vfs.VfsResourceReference;

/**
 * Permission checker for the Attach VFS URI scheme. We check that the current user has view permissions on the page
 * holding the attachment. We need to do this here for the moment because of
 * <a href="http://jira.xwiki.org/browse/XWIKI-12912">this issue</a>.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Named("attach")
@Singleton
public class AttachVfsPermissionChecker implements VfsPermissionChecker
{
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private AttachmentReferenceResolver<String> defaultAttachmentReferenceresolver;

    @Override
    public void checkPermission(VfsResourceReference resourceReference) throws VfsException
    {
        // Check for view permission for the page holding the attachment and for the current user.

        // Extract the document reference from the VFS Resource Reference
        // Use a default resolver (and not a current one) since we don't have any context, we're in a new
        // request.

        DocumentReference documentReference = this.defaultAttachmentReferenceresolver.resolve(
            resourceReference.getURI().getSchemeSpecificPart()).getDocumentReference();

        if (!this.authorizationManager.hasAccess(Right.VIEW, documentReference)) {
            throw new VfsException(String.format("No View permission for document [%s]", documentReference));
        }
    }
}

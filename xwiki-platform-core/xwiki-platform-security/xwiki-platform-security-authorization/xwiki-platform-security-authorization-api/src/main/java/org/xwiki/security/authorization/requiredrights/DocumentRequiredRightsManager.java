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
package org.xwiki.security.authorization.requiredrights;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.stability.Unstable;

/**
 * Manager for document required rights.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Unstable
@Role
public interface DocumentRequiredRightsManager
{
    /**
     * Get the document required rights for the given document.
     *
     * @param documentReference the document to get the required rights for
     * @return the document required rights if the document exists, empty otherwise
     */
    Optional<DocumentRequiredRights> getRequiredRights(DocumentReference documentReference)
        throws AuthorizationException;
}

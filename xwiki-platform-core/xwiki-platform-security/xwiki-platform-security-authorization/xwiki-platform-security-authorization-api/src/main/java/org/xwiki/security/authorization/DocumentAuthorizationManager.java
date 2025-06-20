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
package org.xwiki.security.authorization;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Authorization manager that checks rights of document authors taking required rights into account.
 *
 * @version $Id$
 * @since 16.10.0RC1
 */
@Unstable
@Role
public interface DocumentAuthorizationManager
{
    /**
     * Tests if the given context author of the context document has the specified right at the given level, taking the
     * required rights of the document into account.
     *
     * @param right the right to check
     * @param level the level of the right, for example, for admin right, this could be {@link EntityType#DOCUMENT} for
     * admin right directly on the document itself or {@link EntityType#WIKI} for admin right on the whole wiki.
     * @param contextAuthor the author for which rights shall be checked
     * @param contextDocument the document for which the rights shall be checked
     * @return if the context author has the right
     */
    boolean hasAccess(Right right, EntityType level, DocumentReference contextAuthor,
        DocumentReference contextDocument);

    /**
     * Check if the given document has the given required right.
     *
     * @param right the right to check
     * @param level the level of the right
     * @param contextDocument the reference of the context document
     * @return if the document has the required right (independent of the actual author)
     * @throws AuthorizationException if loading the required rights fails
     */
    boolean hasRequiredRight(Right right, EntityType level, DocumentReference contextDocument)
        throws AuthorizationException;

    /**
     * Checks if the given context author of the context document has the specified right at the given level, taking the
     * required rights of the document into account. This function should be used at security checkpoint.
     *
     * @param right the right to check
     * @param level the level of the right, for example, for admin right, this could be {@link EntityType#DOCUMENT} for
     * admin right directly on the document itself or {@link EntityType#WIKI} for admin right on the whole wiki.
     * @param contextAuthor the author for which rights shall be checked
     * @param contextDocument the document for which the rights shall be checked
     * @throws AccessDeniedException if the context author does not have the right
     */
    void checkAccess(Right right, EntityType level, DocumentReference contextAuthor,
        DocumentReference contextDocument) throws AccessDeniedException;
}

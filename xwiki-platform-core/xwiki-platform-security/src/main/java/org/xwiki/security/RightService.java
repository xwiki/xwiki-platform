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
 *
 */
package org.xwiki.security;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * This is the API for checking the access rights of users on wiki documents.
 * @version $Id$
 */
@ComponentRole
public interface RightService
{
    /**
     * Prefix for generating full user names.
     */
    String XWIKI_SPACE_PREFIX = "XWiki.";

    /**
     * The Superadmin username.
     */
    String SUPERADMIN_USER = "superadmin";

    /**
     * The Superadmin full name.
     */
    String SUPERADMIN_USER_FULLNAME = XWIKI_SPACE_PREFIX + SUPERADMIN_USER;

    /**
     * Check if an action requiring the right identified by {@code right} is allowed to be executed on the current
     * document by the current user. This function should be used at security checkpoints.
     *
     * @param right the right needed for execution on the document
     * @throws InsufficientAuthenticationException if the context does not provide expected user authentication
     * @throws AccessDeniedException if the action should be denied
     */
    void checkUserAccess(Right right) throws InsufficientAuthenticationException, AccessDeniedException;

    /**
     * Check if an action requiring the right identified by {@code right} is allowed to be executed on the current
     * document by this document content author. This function should be used at security checkpoints.
     *
     * @param right the right needed for execution on the document
     * @throws InsufficientAuthenticationException if the context does not provide expected author authentication
     * @throws AccessDeniedException if the action should be denied
     */
    void checkAuthorAccess(Right right) throws InsufficientAuthenticationException, AccessDeniedException;

    /**
     * Verifies if the user identified by {@code userReference} has the access level identified by {@code right} on the
     * entity identified by {@code entityReference}. Note that some rights may be checked higher in hierarchy of the
     * provided entity if such right is not enabled at lowest hierarchy level provided. 
     * This function should be used for interface matters, use {@link checkUserAccess} or {@link checkAuthorAccess}
     * at security checkpoints.
     * 
     * @param right the access level to check (for example, 'view' or 'edit' or 'comment').
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference);

    /**
     * Register a new custom {@link Right}.
     * @param rightDescription the full description of the new {@link Right}
     * @return the created {@link Right}
     * @throws UnableToRegisterRightException if an error prevent creation
     */
    Right register(RightDescription rightDescription) throws UnableToRegisterRightException;
}

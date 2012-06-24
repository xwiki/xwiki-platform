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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * This is a new experimental API for checking the access rights of users on xwiki entities. It is aims to replace
 * the current com.xpn.xwiki.user.api.XWikiRightService. It should provide better extensibility and improved
 * performance while being almost fully compatible with the existing implementation.
 * See org.xwiki.security.authorization.internal.XWikiCachingRightService for a bridge to this new authorisation
 * manager for legacy code.
 *
 * The AuthorisationManager does not provide any help for authentication. Authentication should be provided by
 * another components, yet to be written.
 * Neither this authorization manager has any real use of the context (except for some still to be refactored
 * stuffs, like the read-only mode of XWiki), a separate contextual authorization manager could be written for
 * this purpose.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface AuthorizationManager
{
    /**
     * The Superadmin username.
     */
    String SUPERADMIN_USER = "superadmin";

    /**
     * Check if the user identified by {@code userReference} has the access identified by {@code right} on the
     * entity identified by {@code entityReference}. Note that some rights may be checked higher in hierarchy of the
     * provided entity if such right is not enabled at lowest hierarchy level provided.
     * This function should be used at security checkpoint.
     *
     * @param right the right needed for execution of the action
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @throws AccessDeniedException if the action should be denied
     */
    void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException;

    /**
     * Verifies if the user identified by {@code userReference} has the access identified by {@code right} on the
     * entity identified by {@code entityReference}. Note that some rights may be checked higher in hierarchy of the
     * provided entity if such right is not enabled at lowest hierarchy level provided. 
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     * 
     * @param right the right to check .
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference);

    /**
     * Register a new custom {@link Right}.
     *
     * @param rightDescription the full description of the new {@link Right}
     * @return the created {@link Right}
     * @throws UnableToRegisterRightException if an error prevent creation of the new right. Registering exactly
     * the same right does not cause an exception and return the existing right.
     */
    Right register(RightDescription rightDescription) throws UnableToRegisterRightException;
}

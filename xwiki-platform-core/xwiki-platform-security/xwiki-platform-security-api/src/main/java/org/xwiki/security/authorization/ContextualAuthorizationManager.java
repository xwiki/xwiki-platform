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
import org.xwiki.model.reference.EntityReference;

/**
 * This API is for checking the access rights of current user in the current context. It replaces
 * {@code com.xpn.xwiki.user.api.XWikiRightService}.
 *
 * The ContextualAuthorizationManager does not provide any help for authentication. Authentication should have been
 * ensured previously if needed.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Role
public interface ContextualAuthorizationManager
{
    /**
     * Check if access identified by {@code right} on the current entity is allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used at security checkpoint.
     *
     * @param right the right needed for execution of the action
     * @throws AccessDeniedException if the action should be denied, which may also happen when an error occurs
     */
    void checkAccess(Right right) throws AccessDeniedException;

    /**
     * Verifies if access identified by {@code right} on the current entity would be allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     *
     * @param right the right to check .
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    boolean hasAccess(Right right);

    /**
     * Check if access identified by {@code right} on the given entity is allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used at security checkpoint.
     *
     * @param right the right needed for execution of the action
     * @param entityReference the entity on which to check the right
     * @throws AccessDeniedException if the action should be denied, which may also happen when an error occurs
     */
    void checkAccess(Right right, EntityReference entityReference) throws AccessDeniedException;

    /**
     * Verifies if access identified by {@code right} on the given entity would be allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     *
     * @param right the right to check .
     * @param entityReference the entity on which to check the right
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    boolean hasAccess(Right right, EntityReference entityReference);
}

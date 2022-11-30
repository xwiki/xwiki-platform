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
package org.xwiki.security.authorization.script;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.script.SecurityScriptService;

/**
 * Security Authorization Script Service.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named(SecurityScriptService.ROLEHINT + '.' + SecurityAuthorizationScriptService.ID)
@Singleton
public class SecurityAuthorizationScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ID = "authorization";

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * Check if access identified by {@code right} on the current entity is allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used at security checkpoint.
     *
     * @param right the right needed for execution of the action
     * @throws AccessDeniedException if the action should be denied, which may also happen when an error occurs
     */
    public void checkAccess(Right right) throws AccessDeniedException
    {
        contextualAuthorizationManager.checkAccess(right);
    }

    /**
     * Verifies if access identified by {@code right} on the current entity would be allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     *
     * @param right the right to check .
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    public boolean hasAccess(Right right)
    {
        return contextualAuthorizationManager.hasAccess(right);
    }

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
    public void checkAccess(Right right, EntityReference entityReference) throws AccessDeniedException
    {
        contextualAuthorizationManager.checkAccess(right, entityReference);
    }

    /**
     * Verifies if access identified by {@code right} on the given entity would be allowed in the current context.
     * The context includes information like the authenticated user, the current macro being executed, the rendering
     * context restriction, the dropping of rights by macro, etc...
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     *
     * @param right the right to check.
     * @param entityReference the entity on which to check the right
     * @return {@code true} if the user has the specified right on the entity, {@code false} otherwise
     */
    public boolean hasAccess(Right right, EntityReference entityReference)
    {
        return contextualAuthorizationManager.hasAccess(right, entityReference);
    }

    /**
     * Check if the user identified by {@code userReference} has the access identified by {@code right} on the
     * entity identified by {@code entityReference}. Note that some rights may be checked higher in hierarchy of the
     * provided entity if such right is not enabled at lowest hierarchy level provided.
     * This function should be used at security checkpoint.
     *
     * @param right the right needed for execution of the action
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @throws AccessDeniedException if the action should be denied, which may also happen when an error occurs
     */
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        authorizationManager.checkAccess(right, userReference, entityReference);
    }

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
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        return authorizationManager.hasAccess(right, userReference, entityReference);
    }

    /**
     * Check that a specific right is registered or not.
     *
     * @param rightName the name of the right to check for registration.
     * @return {@code true} only if the right name can be find.
     * @since 12.7RC1
     */
    public boolean isRightRegistered(String rightName)
    {
        return Right.toRight(rightName) != Right.ILLEGAL;
    }

    /**
     * @return all the registered rights names.
     * @since 13.5RC1
     */
    public List<String> getAllRightsNames()
    {
        return Right.getAllRightsAsString();
    }
}

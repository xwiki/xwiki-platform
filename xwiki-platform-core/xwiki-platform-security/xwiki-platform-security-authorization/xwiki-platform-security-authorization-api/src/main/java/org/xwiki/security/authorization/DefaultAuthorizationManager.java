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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.SecurityReferenceFactory;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.internal.XWikiBridge;

/**
 * Default implementation of the {@link AuthorizationManager}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultAuthorizationManager implements AuthorizationManager
{
    /**
     * List of rights that should imply any newly registered right.
     */
    private static final List<Right> DEFAULT_IMPLIED_BY_RIGHTS = Arrays.asList(Right.ADMIN, Right.PROGRAM);

    /** Logger. **/
    @Inject
    private Logger logger;
    
    /** The cached rights. */
    @Inject
    private SecurityCache securityCache;

    /** The loader for filling the cache. */
    @Inject
    private SecurityCacheLoader securityCacheLoader;

    /** The security reference factory. */
    @Inject
    private SecurityReferenceFactory securityReferenceFactory;

    /** Serializer. */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /** XWiki bridge to check for read only wiki. */
    @Inject
    private XWikiBridge xwikiBridge;

    /**
     * Check if the user is the super admin.
     *
     * NOTE: We rely on that the authentication service especially
     * authenticates user names matching superadmin's in a case
     * insensitive match, and will ignore any user profile's that may
     * be matching the superadmin's user name.
     *
     * @param user A document reference representing a user identity.
     * @return {@code true} if and only if the user is determined to be the super user.
     */
    private boolean isSuperAdmin(DocumentReference user)
    {
        return user != null && StringUtils.equalsIgnoreCase(user.getName(), AuthorizationManager.SUPERADMIN_USER);
    }

    @Override
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        try {
            if (!hasSecurityAccess(right, userReference, entityReference, true)) {
                throw new AccessDeniedException(right, userReference, entityReference);
            }
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) {
                throw (AccessDeniedException) e;
            } else {
                throw new AccessDeniedException(right, userReference, entityReference, e);
            }
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        try {
            return hasSecurityAccess(right, userReference, entityReference, false);
        } catch (Exception e) {
            this.logger.error(String.format("Failed to load rights for user [%s] on [%s].",
                (userReference == null) ? AuthorizationException.NULL_USER : userReference,
                (entityReference == null) ? AuthorizationException.NULL_ENTITY : entityReference), e);
            return false;
        }
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
     * @param check if true logging of denied access are made through {@link #logDeny} (at info level), and all
     *              access logging are marked as security checkpoint.
     * @return {@code true} if the user has the specified right on the entity.
     * @throws AuthorizationException if an error occurs.
     */
    private boolean hasSecurityAccess(Right right, DocumentReference userReference, EntityReference entityReference,
        boolean check)
        throws AuthorizationException
    {
        if (isSuperAdmin(userReference)) {
            return true;
        }

        if (right == null || right == Right.ILLEGAL) {
            if (check) {
                logDeny(userReference, entityReference, right, "no such right");
            }
            return false;
        }

        if ((!right.isReadOnly() && xwikiBridge.isWikiReadOnly())
            || (userReference == null && xwikiBridge.needsAuthentication(right))) {
            return false;
        }

        return evaluateSecurityAccess(right, userReference, entityReference, check);
    }

    private boolean evaluateSecurityAccess(Right right, DocumentReference userReference,
        EntityReference entityReference, boolean check)
        throws AuthorizationException
    {
        SecurityAccess securityAccess = getAccess(
            securityReferenceFactory.newUserReference(userReference),
            securityReferenceFactory.newEntityReference(entityReference)
        );

        RuleState access = securityAccess.get(right);
        String info = check ? "security checkpoint" : "access inquiry";
        if (check && access != RuleState.ALLOW) {
            logDeny(userReference, entityReference, right, info);
        } else {
            logAccess(access, userReference, entityReference, right, info, true);
        }
        return access == RuleState.ALLOW;
    }

    @Override
    public Right register(RightDescription rightDescription) throws UnableToRegisterRightException
    {
        // By default Admin should imply any newly registered right.
        return register(rightDescription, new HashSet<>(DEFAULT_IMPLIED_BY_RIGHTS));
    }

    @Override
    public Right register(RightDescription rightDescription, Set<Right> impliedByRights)
        throws UnableToRegisterRightException
    {
        try {
            // Ensure that the default implied by rights are in the given set.
            Set<Right> augmentedImpliedByRights = new HashSet<>(impliedByRights);
            augmentedImpliedByRights.addAll(DEFAULT_IMPLIED_BY_RIGHTS);

            Right newRight = new Right(rightDescription, augmentedImpliedByRights);
            // cleanup the cache since a new right scheme enter in action
            securityCache.remove(securityReferenceFactory.newEntityReference(xwikiBridge.getMainWikiReference()));
            return newRight;
        } catch (Throwable e) {
            Right right = Right.toRight(rightDescription.getName());
            if (right != Right.ILLEGAL && right.like(rightDescription)) {
                return right;
            }
            throw new UnableToRegisterRightException(rightDescription, e);
        }
    }

    @Override
    public void unregister(Right right) throws AuthorizationException
    {
        if (Right.getStandardRights().contains(right)) {
            throw new AuthorizationException(
                String.format("Attempt to unregister the static right [%s]", right.getName()));
        }
        right.unregister();
        // cleanup the cache since a new right scheme enter in action
        securityCache.remove(securityReferenceFactory.newEntityReference(xwikiBridge.getMainWikiReference()));
    }

    /**
     * Obtain the access for the user on the given entity and load it into the cache if unavailable.
     *
     * @param user The user identity.
     * @param entity The entity.  May be of type DOCUMENT, WIKI, or SPACE.
     * @return the cached access entry.
     * @exception org.xwiki.security.authorization.AuthorizationException if an error occurs
     */
    private SecurityAccess getAccess(UserSecurityReference user, SecurityReference entity)
        throws AuthorizationException
    {
        for (SecurityReference ref = entity; ref != null; ref = ref.getParentSecurityReference()) {
            if (Right.getEnabledRights(ref.getSecurityType()).isEmpty()) {
                // Skip search on entity types that will obviously have empty/useless list of rules.
                continue;
            }
            SecurityRuleEntry entry = securityCache.get(ref);
            if (entry == null) {
                SecurityAccess access = securityCacheLoader.load(user, entity).getAccess();

                this.logger.debug("1. Loaded a new entry for user {} on {} into cache: [{}]", user, entity, access);

                return access;
            }
            if (!entry.isEmpty()) {
                SecurityAccessEntry accessEntry = securityCache.get(user, ref);
                if (accessEntry == null) {
                    SecurityAccess access = securityCacheLoader.load(user, entity).getAccess();

                    logger.debug("2. Loaded a new entry for user {} on {} into cache: [{}]", user, entity, access);

                    return access;
                } else {
                    SecurityAccess access = accessEntry.getAccess();

                    logger.debug("3. Got entry for user {} on {} from cache: [{}]", user, entity, access);

                    return access;
                }
            } 
        }

        SecurityAccess access = securityCacheLoader.load(user, entity).getAccess();

        logger.debug("4. Loaded a new default entry for user {} on {} into cache: [{}]", user, entity, access);

        return access;
    }

    /**
     * Log access conclusion.
     * @param access The ALLOW or DENY state
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     * @param debugLevel If true, is made at debug level, else logging is made at info level.
     */
    private void logAccess(RuleState access, DocumentReference user, EntityReference entity, Right right, String info,
        boolean debugLevel)
    {
        if ((debugLevel && logger.isDebugEnabled()) || (!debugLevel && logger.isInfoEnabled())) {
            String userName = (user != null) ? entityReferenceSerializer.serialize(user)
                                             : AuthorizationException.NULL_USER;
            String docName = (entity != null) ? entityReferenceSerializer.serialize(entity)
                                              : AuthorizationException.NULL_USER;
            String rightName = (right != null) ? right.getName() : "no right";
            String accessName = (access == RuleState.ALLOW) ? "granted" : "denied";
            String message = "[{}] access has been {} for user [{}] on [{}]: {}";
            if (debugLevel) {
                logger.debug(message, rightName, accessName, userName, docName, info);
            } else {
                logger.info(message, rightName, accessName, userName, docName, info);
            }
        }
    }

    /**
     * Log denied access conclusion.
     * All denied access conclusion made during a security checkpoint use this method.
     *
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    protected void logDeny(DocumentReference user, EntityReference entity,  Right right, String info)
    {
        logAccess(RuleState.DENY, user, entity, right, info, false);
    }
}

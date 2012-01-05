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
package org.xwiki.security.authorization.internal;

import java.util.Formatter;

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
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AccessLevel;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightDescription;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.UnableToRegisterRightException;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.internal.XWikiBridge;

/**
 * Default implementation of the {@link AuthorizationManager}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultAuthorizationManager implements AuthorizationManager
{
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
        return StringUtils.equalsIgnoreCase(user.getName(), AuthorizationManager.SUPERADMIN_USER);
    }

    @Override
    public void checkAccess(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AccessDeniedException
    {
        try {
            if (!hasAccessLevel(right, userReference, entityReference)) {
                throw new AccessDeniedException();
            }
        } catch (Exception e) {
            throw new AccessDeniedException(e);
        }
    }

    @Override
    public boolean hasAccess(Right right, DocumentReference userReference, EntityReference entityReference)
    {
        try {
            return hasAccessLevel(right, userReference, entityReference);
        } catch (Exception e) {
            this.logger.error("Failed to load rights for user " + userReference + ".", e);
            return false;
        }
    }

    /**
     * Verifies if the user identified by {@code userReference} has the access level identified by {@code right} on the
     * entity identified by {@code entityReference}. Note that some rights may be checked higher in hierarchy of the
     * provided entity if such right is not enabled at lowest hierarchy level provided.
     * This function should be used for interface matters, use {@link #checkAccess} at security checkpoints.
     *
     * @param right the right to check .
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @return {@code true} if the user has the specified right on the entity.
     * @throws AuthorizationException if an error occurs.
     */
    private boolean hasAccessLevel(Right right, DocumentReference userReference, EntityReference entityReference)
        throws AuthorizationException
    {
        if (userReference == null) {
            logDeny(userReference, entityReference, right, "missing user");
            return false;
        }

        if (isSuperAdmin(userReference)) {
            return true;
        }

        if (right == Right.ILLEGAL) {
            logDeny(userReference, entityReference, right, "no such right");
            return false;
        }

        if (!right.isReadOnly() && xwikiBridge.isWikiReadOnly()) {
            return false;
        }
        
        AccessLevel accessLevel = getAccessLevel(
            securityReferenceFactory.newUserReference(userReference),
            securityReferenceFactory.newEntityReference(entityReference)
        );

        RuleState access = accessLevel.get(right);
        logAccess(access, userReference, entityReference, right, "access checked");
        return access == RuleState.ALLOW;
    }

    @Override
    public Right register(RightDescription rightDescription) throws UnableToRegisterRightException
    {
        try {
            return new Right(rightDescription);
        } catch (Throwable e) {
            throw new UnableToRegisterRightException(rightDescription, e);
        }
    }

    /**
     * Obtain the access level for the user on the given entity and load it into the cache if unavailable.
     *
     * @param user The user identity.
     * @param entity The entity.  May be of type DOCUMENT, WIKI, or SPACE.
     * @return the cached access level object.
     * @exception org.xwiki.security.authorization.AuthorizationException if an error occurs
     */
    private AccessLevel getAccessLevel(UserSecurityReference user, SecurityReference entity)
        throws AuthorizationException
    {

        for (SecurityReference ref = entity; ref != null; ref = ref.getParentSecurityReference()) {
            SecurityRuleEntry entry = securityCache.get(ref);
            if (entry == null) {
                AccessLevel level = securityCacheLoader.load(user, entity).getAccessLevel();
                if (logger.isDebugEnabled()) {
                    Formatter f = new Formatter();
                    this.logger.debug(f.format("1. Loaded a new entry for %s@%s into cache: %s",
                                               entityReferenceSerializer.serialize(user),
                                               entityReferenceSerializer.serialize(entity),
                                               level).toString());
                }
                return level;
            }
            if (!entry.isEmpty()) {
                SecurityAccessEntry access = securityCache.get(user, ref);
                if (access == null) {
                    AccessLevel level = securityCacheLoader.load(user, entity).getAccessLevel();
                    if (logger.isDebugEnabled()) {
                        Formatter f = new Formatter();
                        logger.debug(f.format("2. Loaded a new entry for %s@%s into cache: %s",
                            entityReferenceSerializer.serialize(user),
                            entityReferenceSerializer.serialize(entity),
                            level).toString());
                    }
                    return level;
                } else {
                    AccessLevel level = access.getAccessLevel();
                    if (logger.isDebugEnabled()) {
                        Formatter f = new Formatter();
                        logger.debug(f.format("3. Got entry for %s@%s from cache: %s",
                            entityReferenceSerializer.serialize(user),
                            entityReferenceSerializer.serialize(entity),
                            level).toString());
                    }
                    return level;
                }
            } 
        }

        logger.debug("6. Returning default access level.");
        return XWikiAccessLevel.getDefaultAccessLevel();
    }

    /**
     * Log allow conclusion.
     * @param access The ALLOW or DENY state
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    private void logAccess(RuleState access, DocumentReference user, EntityReference entity, Right right, String info)
    {
        if ((access == RuleState.ALLOW && logger.isDebugEnabled())
            || (access != RuleState.ALLOW && logger.isInfoEnabled())) {
            String userName = (user != null) ? entityReferenceSerializer.serialize(user) : "no user";
            String docName = (entity != null) ? entityReferenceSerializer.serialize(entity) : "no entity";
            Formatter f = new Formatter();
            if (access == RuleState.ALLOW) {
                logger.debug(f.format("Access has been granted for (%s,%s,%s): %s",
                    userName, docName, right.getName(), info).toString());
            } else {
                logger.info(f.format("Access has been denied for (%s,%s,%s): %s",
                    userName, docName, right.getName(), info).toString());
            }
        }
    }

    /**
     * Log deny conclusion.
     * @param user The user name that was checked.
     * @param entity The page that was checked.
     * @param right The action that was requested.
     * @param info Additional information.
     */
    protected void logDeny(DocumentReference user, EntityReference entity,  Right right, String info)
    {
        logAccess(RuleState.DENY, user, entity, right, info);
    }
}

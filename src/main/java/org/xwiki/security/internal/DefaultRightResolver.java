/*
 * Copyright 2010 Andreas Jonsson
 * 
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
package org.xwiki.security.internal;

import org.xwiki.component.annotation.Component;

import org.xwiki.security.RightService;
import org.xwiki.security.Right;
import static org.xwiki.security.Right.*;
import org.xwiki.security.RightState;
import org.xwiki.security.RightCacheKey;
import static org.xwiki.security.RightState.*;
import org.xwiki.security.AccessLevel;
import org.xwiki.security.RightResolver;
import org.xwiki.security.RightsObject;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.EntityType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Arrays;

/**
 * The default implementation for the right resolver.
 *
 * @version $Id: $
 */
@Component
public class DefaultRightResolver implements RightResolver
{
    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(DefaultRightResolver.class);

    /** Map for resolving conflicting rights within a document hierarchy level. */
    private final Map<Right, RightState> tieResolution = new EnumMap(Right.class);
    /** Map to resolving configting rights between document hierarchy levels. */
    private final Map<Right, Boolean> smallerWin = new EnumMap(Right.class);
    /** Additional rights an admin have. */
    private final Right[] adminImpliedRights   = {LOGIN, VIEW, EDIT, DELETE, REGISTER, COMMENT };
    /** Additional rights a programmer have. */
    private final Right[] programImpliedRights = {LOGIN, VIEW, EDIT, DELETE, ADMIN, REGISTER, COMMENT };
    /**
     * The enabled rights for a document hierarcy level.  The PROGRAM
     * right should only be enabled for the main wiki, not for wikis
     * in general. 
     */
    private final Map<EntityType, Iterable<Right>> enabledRights = new HashMap();

    {
        tieResolution.put(LOGIN,    ALLOW);
        tieResolution.put(VIEW,     DENY);
        tieResolution.put(EDIT,     DENY);
        tieResolution.put(DELETE,   DENY);
        tieResolution.put(ADMIN,    ALLOW);
        tieResolution.put(PROGRAM,  ALLOW);
        tieResolution.put(REGISTER, ALLOW);
        tieResolution.put(COMMENT,  DENY);
        tieResolution.put(ILLEGAL,  DENY);
        smallerWin.put(LOGIN,    true);
        smallerWin.put(VIEW,     true);
        smallerWin.put(EDIT,     true);
        smallerWin.put(DELETE,   true);
        smallerWin.put(ADMIN,    false);
        smallerWin.put(PROGRAM,  false);
        smallerWin.put(REGISTER, false);
        smallerWin.put(COMMENT,  true);
        smallerWin.put(ILLEGAL,  false);
        Right[] pageRights        = {VIEW, EDIT, COMMENT, DELETE };
        Right[] spaceRights       = {VIEW, EDIT, COMMENT, DELETE, ADMIN }; 
        Right[] wikiRights = {VIEW, EDIT, COMMENT, DELETE, ADMIN, REGISTER, LOGIN, PROGRAM };
        enabledRights.put(EntityType.DOCUMENT, Arrays.asList(pageRights));
        enabledRights.put(EntityType.SPACE,    Arrays.asList(spaceRights));
        enabledRights.put(EntityType.WIKI,     Arrays.asList(wikiRights));
    }

    @Override
    public AccessLevel resolve(DocumentReference user,
                               EntityReference entity,
                               RightCacheKey entityKey,
                               Collection<DocumentReference> groups,
                               List<Collection<RightsObject>> rightsObjects)
    {
        AccessLevel accessLevel = new AccessLevel();

        /*
         * Allow everything for superadmin.
         */
        if (isSuperAdmin(user)) {
            for (Right right : Right.values()) {
                if (right != ILLEGAL) {
                    accessLevel.allow(right);
                }
            }
            return accessLevel.getExistingInstance();
        }

        /*
         * Creator is granted delete-rights.
         */
        if (isCreator(user, entity)) {
            accessLevel.allow(DELETE);
        }

        /*
         * Wiki owner is granted admin rights.
         */
        if (isWikiOwner(user, entity)) {
            accessLevel.allow(ADMIN);
        }

        ListIterator<Collection<RightsObject>> iterator
            = rightsObjects.listIterator(rightsObjects.size());
        EntityReference ref = entityKey.getEntityReference();
        while (iterator.hasPrevious() && ref != null) {
            resolve(user, groups, iterator.previous(), ref, accessLevel);
            ref = ref.getParent();
        }

        assert (!iterator.hasPrevious() && ref == null);

        postProcess(accessLevel);

        return accessLevel.getExistingInstance();
    }

    /**
     * Fill out default values, allow additional rights in case of
     * program or admin rights, etc.
     * @param accessLevel The accumulated result.
     */
    private void postProcess(AccessLevel accessLevel)
    {
        for (Right right : Right.values()) {
            if (accessLevel.get(right) == UNDETERMINED) {
                accessLevel.set(right, AccessLevel.DEFAULT_ACCESS_LEVEL.get(right));
            }
        }

        if (accessLevel.get(PROGRAM) == ALLOW) {
            for (Right right : programImpliedRights) {
                accessLevel.allow(right);
            }
        } else if (accessLevel.get(ADMIN)  == ALLOW) {
            for (Right right : adminImpliedRights) {
                accessLevel.allow(right);
            }
        }

    }

    /**
     * Compute the access level of a particular document hierarchy level.
     * @param user The user.
     * @param groups The groups where the user is a member.
     * @param rightsObjects The rights objects at this level in the document hierarchy.
     * @param ref The entity reference that specifies this level in the document hierarchy.
     * @param accessLevel The accumulated result.
     */
    private void resolve(DocumentReference user,
                         Collection<DocumentReference> groups,
                         Collection<RightsObject> rightsObjects,
                         EntityReference ref,
                         AccessLevel accessLevel)
    {
        AccessLevel currentLevel = new AccessLevel();

        if (ref.getParent() == null && ref.getChild() != null 
            && ref.getChild().getType() == EntityType.WIKI) {
            /*
             * We are moving from virtual wiki to main wiki.
             */
        }

        for (Right right : enabledRights.get(ref.getType())) {
            boolean foundAllow = false;
            for (RightsObject obj : rightsObjects) {
                if (obj.checkRight(right)) {
                    if (obj.getState() == ALLOW) {
                        foundAllow = true;
                    }
                    resolveLevel(right, user, groups, obj, ref, currentLevel);
                }
            }
            if (foundAllow && currentLevel.get(right) == UNDETERMINED) {
                /*
                 * The same behavior as the old implementation. I.e.,
                 * an allow means implicit deny for everyone else.
                 */
                currentLevel.deny(right);

            }
        }
        mergeLevels(currentLevel, accessLevel, ref);
    }


    /**
     * Resolve the given right for the user on the level given by ref.
     * @param right The right to resolve.
     * @param user The user identity.
     * @param groups The groups where the user is a member.
     * @param obj The currently considered rights object.
     * @param ref An entity reference that represents the current level in the document hierarchy.
     * @param accessLevel The accumulated result at the current level.
     */
    private void resolveLevel(Right right,
                              DocumentReference user,
                              Collection<DocumentReference> groups,
                              RightsObject obj,
                              EntityReference ref,
                              AccessLevel accessLevel)
    {
        if (right == PROGRAM && ref.getParent() != null) {
            /*
             * Programming rights only checked on main wiki.
             */
            return;
        }

        if (obj.checkUser(user)) {
            resolveConflict(obj.getState(), right, accessLevel);
        } else {
            for (DocumentReference group : groups) {
                if (obj.checkGroup(group)) {
                    resolveConflict(obj.getState(), right, accessLevel);
                    break;
                }
            }
        }
    }

    /**
     * Resolve conflicting rights within the current level in the document hierarchy.
     * @param state The state to consider setting.
     * @param right The right that is being concerned.
     * @param accessLevel The accumulated result.
     */
    private void resolveConflict(RightState state, Right right, AccessLevel accessLevel)
    {
        if (accessLevel.get(right) == UNDETERMINED) {
            accessLevel.set(right, state);
            return;
        }
        if (state == UNDETERMINED) {
            return;
        }
        if (accessLevel.get(right) != state) {
            accessLevel.set(right, tieResolution.get(right));
        }
    }

    /**
     * Merge the current levels with the result from previous ones.
     * @param currentLevel The access level computed at the current level in the document hierarchy.
     * @param accessLevel The resulting access levels previously computed.
     * @param ref {@link EntityReference} that specifies the current level in the document hierarchy.
     */
    private void mergeLevels(AccessLevel currentLevel,
                             AccessLevel accessLevel,
                             EntityReference ref)
    {
        for (Right right : enabledRights.get(ref.getType())) {
            if (right == PROGRAM && ref.getParent() != null) {
                /*
                 * Programming rights only allowed on main wiki.
                 */
                continue;
            }
            if (accessLevel.get(right) == UNDETERMINED) {
                accessLevel.set(right, currentLevel.get(right));
                continue;
            }
            if (currentLevel.get(right) == UNDETERMINED) {
                continue;
            }
            if (currentLevel.get(right) == ALLOW) {
                if (!smallerWin.get(right)) {
                    accessLevel.allow(right);
                }
            }
        }
    }

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
        return StringUtils.equalsIgnoreCase(user.getName(), RightService.SUPERADMIN_USER);
    }


    /**
     * @param user A user identity.
     * @param entity The entity.
     * @return {@code true} if and only if the entity is a document
     * and the user is the creator of the document.
     */
    private boolean isCreator(DocumentReference user, EntityReference entity)
    {
        if (entity.getType() == EntityType.DOCUMENT) {
            return XWikiUtils.isCreator(user, new DocumentReference(entity));
        }
        return false;
    }

    /**
     * @param user User identity.
     * @param entity An entity in the wiki.
     * @return {@code true} if and only if the given user is the owner
     * of the wiki where the entity is stored.
     */
    private boolean isWikiOwner(DocumentReference user, EntityReference entity)
    {
        EntityReference wiki = entity.getRoot();
        return XWikiUtils.isWikiOwner(user, wiki.getName());
    }
}

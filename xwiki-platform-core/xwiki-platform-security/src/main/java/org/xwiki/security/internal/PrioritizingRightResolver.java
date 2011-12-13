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

import org.xwiki.security.Right;
import static org.xwiki.security.Right.*;
import org.xwiki.security.RightState;
import org.xwiki.security.RightCacheKey;
import static org.xwiki.security.RightState.*;
import org.xwiki.security.AccessLevel;
import org.xwiki.security.RightsObject;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.EnumMap;

/**
 * The default implementation for the right resolver.
 *
 * @version $Id$
 */
@Component("priority")
public class PrioritizingRightResolver extends AbstractRightResolver
{
    /** Priority of rights specified for users. */
    private static final int USER_PRIORITY = Integer.MAX_VALUE;

    /** Priority of rights specified for "all group". */
    private static final int ALL_GROUP_PRIORITY = 0;

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
                accessLevel.allow(right);
            }
            return accessLevel.getExistingInstance();
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

        postProcess(user, entity, accessLevel);

        return accessLevel.getExistingInstance();
    }

    /**
     * Fill out default values, allow additional rights in case of
     * program or admin rights, etc.
     * @param user The user, whose rights are to be determined.
     * @param entity The entity, which the user wants to access.
     * @param accessLevel The accumulated result.
     */
    private void postProcess(DocumentReference user, EntityReference entity, AccessLevel accessLevel)
    {
        for (Right right : Right.values()) {
            if (accessLevel.get(right) == UNDETERMINED) {
                if (!user.getWikiReference().getName().equals(entity.getRoot().getName())) {
                    /*
                     * Deny all by default for users from another wiki.
                     */
                    accessLevel.deny(right);
                } else {
                    /*
                     * Creator is granted delete-rights, by default.
                     */
                    if (right == DELETE && isCreator(user, entity)) {
                        accessLevel.allow(DELETE);
                    } else {
                        accessLevel.set(right, AccessLevel.DEFAULT_ACCESS_LEVEL.get(right));
                    }
                }
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
        Map<Right, Integer> priorities = new EnumMap(Right.class);

        for (Right right : enabledRights.get(ref.getType())) {
            boolean foundAllow = false;
            for (RightsObject obj : rightsObjects) {
                if (obj.checkRight(right)) {
                    resolveLevel(right, user, groups, obj, ref, priorities, currentLevel);
                }
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
     * @param priorities The accumulated priorities of each right in the current level.
     * @param accessLevel The accumulated result at the current level.
     */
    private void resolveLevel(Right right,
                              DocumentReference user,
                              Collection<DocumentReference> groups,
                              RightsObject obj,
                              EntityReference ref,
                              Map<Right, Integer> priorities,
                              AccessLevel accessLevel)
    {
        if (right == PROGRAM && ref.getParent() != null) {
            /*
             * Programming rights only checked on main wiki.
             */
            return;
        }

        if (obj.checkUser(user)) {
            resolveConflict(obj.getState(), right, USER_PRIORITY, priorities, accessLevel);
        } else {
            for (DocumentReference group : groups) {
                if (obj.checkGroup(group)) {
                    resolveConflict(obj.getState(), right, getPriority(group), priorities, accessLevel);
                    break;
                }
            }
        }
    }

    /**
     * Resolve conflicting rights within the current level in the document hierarchy.
     * @param state The state to consider setting.
     * @param right The right that is being concerned.
     * @param priority The priority to use for this particular right match.
     * @param priorities Priority that was used for previous matches.
     * @param accessLevel The accumulated result.
     */
    private void resolveConflict(RightState state,
                                 Right right,
                                 int priority,
                                 Map<Right, Integer> priorities,
                                 AccessLevel accessLevel)
    {
        if (accessLevel.get(right) == UNDETERMINED) {
            accessLevel.set(right, state);
            priorities.put(right, priority);
            return;
        }
        if (state == UNDETERMINED) {
            return;
        }
        if (accessLevel.get(right) != state) {
            if (priority > priorities.get(right)) {
                accessLevel.set(right, state);
                priorities.put(right, priority);
            } else {
                accessLevel.set(right, tieResolution.get(right));
            }
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
     * @param group A group identifier.
     * @return the priority for the group.
     */
    private int getPriority(DocumentReference group)
    {
        if (group.getName().equals("XWikiAllGroup")) {
            return ALL_GROUP_PRIORITY;
        } else {
            return ALL_GROUP_PRIORITY + 1;
        }
    }
}

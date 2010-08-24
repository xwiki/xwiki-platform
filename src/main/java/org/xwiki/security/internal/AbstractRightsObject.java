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

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.UsersClass;

import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;

import org.xwiki.security.Right;
import org.xwiki.security.RightState;
import org.xwiki.security.RightsObject;

import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;

/**
 * Wrapper around xwiki rights objects.
 * @version $Id$
 */
public abstract class AbstractRightsObject implements RightsObject
{
    /** XWiki class for storing rights. */
    static final String LOCAL_RIGHTS_CLASS = "XWiki.XWikiRights";

    /** XWiki class for storing global rights. */
    static final String GLOBAL_RIGHTS_CLASS = "XWiki.XWikiGlobalRights";

    /** Field name in xwiki rights object. */
    private static final String GROUPS_FIELD_NAME = "groups";

    /** Field name in xwiki rights object. */
    private static final String USERS_FIELD_NAME  = "users";

    /** Field name in xwiki rights object. */
    private static final String ALLOW_FIELD_NAME  = "allow";

    /** Wiki delimiter. */
    private static final String WIKI_DELIMITER = ":";

    /** Space delimiter. */
    private static final String SPACE_DELIMITER = ".";

    /** The set of users. */
    private final Set<DocumentReference> users;

    /** The set of groups. */
    private final Set<DocumentReference> groups;

    /** The set of right levels. */
    private final Set<Right> rights;

    /** The state specified by this object. */
    private final RightState state;

    /**
     * Construct a more manageable java object from the corresponding
     * xwiki object.
     * @param obj An xwiki rights object.
     * @param resolver A document reference resolver for user and group pages.
     * @param wikiName The name of the current wiki.
     */
    protected AbstractRightsObject(BaseObject obj, DocumentReferenceResolver resolver, String wikiName)
    {
        state = (obj.getIntValue(ALLOW_FIELD_NAME) == 1) ? RightState.ALLOW : RightState.DENY;
        users = new HashSet();
        groups = new HashSet();
        rights = EnumSet.noneOf(Right.class);

        String levels = obj.getStringValue("levels");
        String[] levelsarray = StringUtils.split(levels, " ,|");
        for (String s : levelsarray) {
            Right right = Right.toRight(s);
            if (right != Right.ILLEGAL) {
                rights.add(right);
            }
        }

        for (String u : UsersClass.getListFromString(obj.getStringValue(USERS_FIELD_NAME))) {
            String user = u;
            DocumentReference ref = resolver.resolve(user, wikiName);
            this.users.add(ref);
        }

        for (String g : GroupsClass.getListFromString(obj.getStringValue(GROUPS_FIELD_NAME))) {
            String group = g;
            DocumentReference ref = resolver.resolve(group, wikiName);
            this.groups.add(ref);
        }
    }

    /** 
     * Constructor used only by test code.
     * @param rights The set of rights.
     * @param state The state of this rights object.
     * @param users The set of users.
     * @param groups The set of groups.
     */
    protected AbstractRightsObject(Set<Right> rights,
                                   RightState state,
                                   Set<DocumentReference> users,
                                   Set<DocumentReference> groups)
    {
        this.users = users;
        this.groups = groups;
        this.rights = rights;
        this.state = state;
    }

    /**
     * Check if the state of this object should be applied for a given right.
     * @param right The righ to check.
     * @return {@code true} if the state should be applied for the right,
     * othewise {@code false}.
     */
    public boolean checkRight(Right right)
    {
        return rights.contains(right);
    }

    /**
     * @return The {@cod RightState} of this object.
     */
    public RightState getState()
    {
        return state;
    }

    /**
     * Check if the state of this object should be applied to a given group.
     * @param group The group to check.
     * @return {@code true} if the state should be applied for group,
     * othewise {@code false}.
     */
    public boolean checkGroup(DocumentReference group)
    {
        return groups.contains(group);
    }

    /**
     * Check if the state of this object should be applied to a given user.
     * @param user The user to check.
     * @return {@code true} if the state should be applied for user,
     * othewise {@code false}.
     */
    public boolean checkUser(DocumentReference user)
    {
        return users.contains(user);
    }

    @Override
    public boolean equals(Object other)
    {
        if (!(other instanceof AbstractRightsObject)) {
            return false;
        }

        return state == ((AbstractRightsObject) other).state
            && rights.equals(((AbstractRightsObject) other).rights)
            && users.equals(((AbstractRightsObject) other).users)
            && groups.equals(((AbstractRightsObject) other).groups);
    }

    @Override
    public int hashCode()
    {
        return state.hashCode() + rights.hashCode() + users.hashCode() + groups.hashCode();
    }

    @Override
    public String toString()
    {
        return "State: " + state
            + "Rights: " + rights
            + "Users: "  + users
            + "Groups: " + groups;
    }
}

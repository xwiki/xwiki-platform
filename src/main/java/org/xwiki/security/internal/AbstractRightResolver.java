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

import org.xwiki.security.RightResolver;
import org.xwiki.security.RightService;
import org.xwiki.security.Right;
import org.xwiki.security.RightState;
import static org.xwiki.security.Right.*;
import static org.xwiki.security.RightState.*;


import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.EntityType;

import java.util.HashMap;
import java.util.Arrays;
import java.util.Map;
import java.util.EnumMap;

import org.xwiki.component.logging.AbstractLogEnabled;
import org.apache.commons.lang.StringUtils;

/**
 * Abstract super class for right resolvers.
 * @version $Id: $
 */
abstract class AbstractRightResolver extends AbstractLogEnabled implements RightResolver
{
    /** Map for resolving conflicting rights within a document hierarchy level. */
    protected final Map<Right, RightState> tieResolution = new EnumMap(Right.class);
    /** Map to resolving configting rights between document hierarchy levels. */
    protected final Map<Right, Boolean> smallerWin = new EnumMap(Right.class);
    /** Additional rights an admin have. */
    protected final Right[] adminImpliedRights   = {LOGIN, VIEW, EDIT, DELETE, REGISTER, COMMENT };
    /** Additional rights a programmer have. */
    protected final Right[] programImpliedRights = {LOGIN, VIEW, EDIT, DELETE, ADMIN, REGISTER, COMMENT };
    /**
     * The enabled rights for a document hierarcy level.  The PROGRAM
     * right should only be enabled for the main wiki, not for wikis
     * in general. 
     */
    protected final Map<EntityType, Iterable<Right>> enabledRights = new HashMap();

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
    protected boolean isSuperAdmin(DocumentReference user)
    {
        return StringUtils.equalsIgnoreCase(user.getName(), RightService.SUPERADMIN_USER);
    }


    /**
     * @param user A user identity.
     * @param entity The entity.
     * @return {@code true} if and only if the entity is a document
     * and the user is the creator of the document.
     */
    protected boolean isCreator(DocumentReference user, EntityReference entity)
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
    protected boolean isWikiOwner(DocumentReference user, EntityReference entity)
    {
        EntityReference wiki = entity.getRoot();
        return XWikiUtils.isWikiOwner(user, wiki.getName());
    }
}
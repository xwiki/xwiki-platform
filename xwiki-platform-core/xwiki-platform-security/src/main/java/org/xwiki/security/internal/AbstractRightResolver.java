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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.RightResolver;
import org.xwiki.security.RightService;

/**
 * Abstract super class for right resolvers.
 * @version $Id$
 */
abstract class AbstractRightResolver implements RightResolver
{
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
        return (entity.getType() == EntityType.DOCUMENT) && XWikiUtils.isCreator(user, new DocumentReference(entity));
    }

    /**
     * @param user User identity.
     * @param entity An entity in the wiki.
     * @return {@code true} if and only if the given user is the owner
     * of the wiki where the entity is stored.
     */
    protected boolean isWikiOwner(DocumentReference user, EntityReference entity)
    {
        WikiReference wiki = (WikiReference) entity.getRoot();
        return XWikiUtils.isWikiOwner(user, wiki);
    }
}

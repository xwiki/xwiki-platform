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
package org.xwiki.user;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Represents the Super Admin user reference, i.e. a virtual user that has all permissions. This class is the single
 * source of truth about the Super Admin user: use {@link #isSuperAdmin(UserReference)} to check whether a resolved
 * {@link UserReference} is the Super Admin user, and {@link #isSuperAdminReference(EntityReference)} when only a
 * document reference is available.
 *
 * @version $Id$
 * @since 12.2
 */
public final class SuperAdminUserReference implements UserReference
{
    /**
     * The name of the Super Admin user in a user reference (e.g. the {@code superadmin} part of
     * {@code xwiki:XWiki.superadmin}). Note that the Super Admin user is recognized whatever the case used for that
     * name, see {@link #isSuperAdminName(String)}.
     *
     * @since 18.9.0RC1
     */
    public static final String SUPERADMIN_USER_NAME = "superadmin";

    /**
     * The name of the space holding the Super Admin user, i.e. the space a user reference must be in to denote the
     * Super Admin user.
     *
     * @since 18.9.0RC1
     */
    public static final String SUPERADMIN_USER_SPACE = "XWiki";

    /**
     * The reference of the Super Admin user inside a wiki, i.e. {@code XWiki.superadmin}. The Super Admin user is a
     * virtual user and has no document, this is the reference it is serialized to and the only reference that denotes
     * it.
     *
     * @since 18.9.0RC1
     */
    public static final LocalDocumentReference SUPERADMIN_LOCAL_REFERENCE =
        new LocalDocumentReference(SUPERADMIN_USER_SPACE, SUPERADMIN_USER_NAME);

    /**
     * The unique instance of this class.
     */
    public static final SuperAdminUserReference INSTANCE = new SuperAdminUserReference();

    private SuperAdminUserReference()
    {
        // Voluntarily empty. We want to have a single instance of this class (hence the private part).
    }

    /**
     * @param userReference the user reference to check. It must be a resolved reference: {@link CurrentUserReference}
     *            is never the Super Admin user, resolve it first with a {@link UserReferenceResolver} if needed
     * @return {@code true} if the passed reference is the Super Admin user reference
     * @since 18.9.0RC1
     */
    public static boolean isSuperAdmin(UserReference userReference)
    {
        return INSTANCE == userReference;
    }

    /**
     * Tells whether a document reference denotes the Super Admin user, i.e. whether it points to
     * {@code XWiki.superadmin} in some wiki. Whenever a {@link UserReference} is available, use
     * {@link #isSuperAdmin(UserReference)} instead.
     *
     * @param userReference the reference of the user document to check, in any wiki
     * @return {@code true} if the passed reference denotes the Super Admin user
     * @since 18.9.0RC1
     */
    public static boolean isSuperAdminReference(EntityReference userReference)
    {
        boolean result = false;
        EntityReference documentReference =
            (userReference == null) ? null : userReference.extractReference(EntityType.DOCUMENT);
        if (documentReference != null && isSuperAdminName(documentReference.getName())) {
            result = isSuperAdminSpace(documentReference.getParent());
        }
        return result;
    }

    /**
     * @param spaceReference the reference of the space holding a user document
     * @return {@code true} when the passed reference is the top level XWiki space of a wiki
     */
    private static boolean isSuperAdminSpace(EntityReference spaceReference)
    {
        boolean result = false;
        // The Super Admin user lives directly in the top level XWiki space of a wiki. A document with the same name
        // in any other space, including a nested space whose last level is named XWiki, is an ordinary document and
        // must not be given the Super Admin permissions.
        if (spaceReference != null && spaceReference.getType() == EntityType.SPACE
            && SUPERADMIN_USER_SPACE.equals(spaceReference.getName()))
        {
            EntityReference parentReference = spaceReference.getParent();
            result = parentReference == null || parentReference.getType() == EntityType.WIKI;
        }
        return result;
    }

    /**
     * Tells whether a user reference name denotes the Super Admin user. Since only the name is checked, this must be
     * used only where the reference is already known to point inside the {@link #SUPERADMIN_USER_SPACE} space;
     * everywhere else use {@link #isSuperAdminReference(EntityReference)} or {@link #isSuperAdmin(UserReference)},
     * which also verify the space.
     *
     * @param userName the name part of a user reference (e.g. the {@code superadmin} part of
     *            {@code xwiki:XWiki.superadmin})
     * @return {@code true} if the passed name is the Super Admin user name, whatever its case
     * @since 18.9.0RC1
     */
    public static boolean isSuperAdminName(String userName)
    {
        return SUPERADMIN_USER_NAME.equalsIgnoreCase(userName);
    }

    @Override
    public boolean isGlobal()
    {
        return true;
    }
}

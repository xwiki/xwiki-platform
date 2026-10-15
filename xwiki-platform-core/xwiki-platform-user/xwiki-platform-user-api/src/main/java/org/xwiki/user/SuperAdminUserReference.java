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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represents the Super Admin user reference, i.e. a virtual user that has all permissions. This class is the single
 * source of truth about the Super Admin user: use {@link #isSuperAdmin(UserReference)} to check whether a
 * {@link UserReference} is the Super Admin user, and {@link #isSuperAdminReference(EntityReference)} when only the
 * reference of a user document is available.
 *
 * @version $Id$
 * @since 12.2
 */
public final class SuperAdminUserReference implements UserReference
{
    /**
     * The name of the Super Admin user in a user reference (e.g. the {@code superadmin} part of
     * {@code xwiki:XWiki.superadmin}). Note that the Super Admin user is recognized whatever the case used for that
     * name.
     *
     * @since 18.10.0RC1
     */
    @Unstable
    public static final String SUPERADMIN_USER_NAME = "superadmin";

    /**
     * The name of the space in which the Super Admin user is serialized as a document reference.
     *
     * @since 18.10.0RC1
     */
    @Unstable
    public static final String SUPERADMIN_USER_SPACE = "XWiki";

    /**
     * The reference the Super Admin user is serialized to inside a wiki, i.e. {@code XWiki.superadmin}. The Super Admin
     * user is a virtual user and has no document.
     *
     * @since 18.10.0RC1
     */
    @Unstable
    public static final LocalDocumentReference SUPERADMIN_LOCAL_REFERENCE =
        new LocalDocumentReference(SUPERADMIN_USER_SPACE, SUPERADMIN_USER_NAME);

    /**
     * The string serialization of {@link #SUPERADMIN_LOCAL_REFERENCE}, i.e. {@code XWiki.superadmin}.
     *
     * @since 18.10.0RC1
     */
    @Unstable
    public static final String SUPERADMIN_LOCAL_REFERENCE_STRING = SUPERADMIN_USER_SPACE + '.' + SUPERADMIN_USER_NAME;

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
     * @since 18.10.0RC1
     */
    @Unstable
    public static boolean isSuperAdmin(UserReference userReference)
    {
        return INSTANCE == userReference;
    }

    /**
     * Tells whether the reference of a user document denotes the Super Admin user, i.e. whether its name is
     * {@link #SUPERADMIN_USER_NAME}, whatever the case. Whenever a {@link UserReference} is available, use
     * {@link #isSuperAdmin(UserReference)} instead.
     *
     * @param userReference the reference of the user document to check
     * @return {@code true} if the passed reference denotes the Super Admin user
     * @since 18.10.0RC1
     */
    @Unstable
    public static boolean isSuperAdminReference(EntityReference userReference)
    {
        return userReference != null && SUPERADMIN_USER_NAME.equalsIgnoreCase(userReference.getName());
    }

    @Override
    public boolean isGlobal()
    {
        return true;
    }

    /**
     * Keep a single instance of this class when it's deserialized, so that comparing a deserialized reference with
     * {@link #INSTANCE} still works.
     *
     * @return the unique instance of this class
     */
    private Object readResolve()
    {
        return INSTANCE;
    }
}

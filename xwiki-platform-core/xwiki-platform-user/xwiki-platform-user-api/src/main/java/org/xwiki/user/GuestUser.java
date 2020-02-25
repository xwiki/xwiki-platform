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

import org.xwiki.stability.Unstable;

/**
 * Represents the Guest user (i.e. a virtual user representing a non-logged in user).
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Unstable
public final class GuestUser implements User
{
    /**
     * Package-level visibility so that it can be used in the User class (located in the same package.
     */
    static final GuestUser INSTANCE = new GuestUser();

    private GuestUser()
    {
        // Voluntarily empty. We want to have a single instance of this class (hence the private part).
    }

    @Override
    public boolean displayHiddenDocuments()
    {
        return false;
    }

    @Override
    public boolean isActive()
    {
        return true;
    }

    @Override
    public String getFirstName()
    {
        return "Guest";
    }

    @Override
    public String getLastName()
    {
        return null;
    }

    @Override
    public String getEmail()
    {
        return null;
    }

    @Override
    public UserType getType()
    {
        return UserType.SIMPLE;
    }

    @Override
    public boolean isEmailChecked()
    {
        return true;
    }

    @Override
    public boolean isGlobal()
    {
        return true;
    }

    @Override
    public Object getProperty(String propertyName)
    {
        return null;
    }

    @Override
    public UserReference getUserReference()
    {
        return UserReference.GUEST_REFERENCE;
    }
}

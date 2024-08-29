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

/**
 * Represents the Super Admin user reference, i.e. a virtual user that is not logged in. If you need to verify
 * if a given UserReference is the Guest user, you should use {@code if (myRef == GuestUserReference.INSTANCE)}.
 *
 * @version $Id$
 * @since 12.2
 */
public final class GuestUserReference implements UserReference
{
    /**
     * The unique instance of this class.
     */
    public static final GuestUserReference INSTANCE = new GuestUserReference();

    private GuestUserReference()
    {
        // Voluntarily empty. We want to have a single instance of this class (hence the private part).
    }

    @Override
    public boolean isGlobal()
    {
        return true;
    }
}

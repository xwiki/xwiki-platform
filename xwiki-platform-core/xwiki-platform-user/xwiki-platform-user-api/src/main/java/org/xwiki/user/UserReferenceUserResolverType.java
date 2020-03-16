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

import java.lang.reflect.ParameterizedType;

import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.stability.Unstable;

/**
 * Represents the {@code UserResolver<UserReference>} {@link java.lang.reflect.Type}.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Unstable
public final class UserReferenceUserResolverType
{
    /**
     * Type instance for {@code UserResolver<UserReference>}.
     */
    public static final ParameterizedType INSTANCE =
        new DefaultParameterizedType(null, UserResolver.class, UserReference.class);

    private UserReferenceUserResolverType()
    {
        // Voluntarily empty. We want to have a single instance of this class (hence the private part).
    }
}

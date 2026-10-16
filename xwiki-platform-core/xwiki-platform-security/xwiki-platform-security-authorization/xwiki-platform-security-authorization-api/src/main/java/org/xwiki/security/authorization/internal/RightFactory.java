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
package org.xwiki.security.authorization.internal;

import java.util.Set;

import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightDescription;

/**
 * Creates new {@link Right} instances for the internal implementation of
 * {@link AuthorizationManager#register(RightDescription, Set)}.
 * <p>
 * The constructors of {@link Right} are not public since a new right must always be registered through the
 * {@link AuthorizationManager}, which also takes care of the implied rights and of the security cache. The
 * {@link Creator} is provided by {@link Right} itself during its static initialization, which gives the internal code
 * access to these constructors without exposing them in the public API.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class RightFactory
{
    /**
     * Creates and registers a new right.
     *
     * @version $Id$
     */
    @FunctionalInterface
    public interface Creator
    {
        /**
         * @param description the description of the right to create
         * @param impliedByRights the already existing rights that imply the new right
         * @return the new right
         */
        Right create(RightDescription description, Set<Right> impliedByRights);
    }

    private static volatile Creator creator;

    private RightFactory()
    {
    }

    /**
     * Set the creator of rights. This method is called by {@link Right} during its static initialization and cannot be
     * called again afterwards.
     *
     * @param rightCreator the creator of rights
     * @throws IllegalStateException if a creator is already set
     */
    public static synchronized void setCreator(Creator rightCreator)
    {
        if (creator != null) {
            throw new IllegalStateException("The right creator is already set.");
        }
        creator = rightCreator;
    }

    /**
     * Create and register a new right.
     *
     * @param description the description of the right to create
     * @param impliedByRights the already existing rights that imply the new right
     * @return the new right
     */
    public static Right createRight(RightDescription description, Set<Right> impliedByRights)
    {
        // The creator is set by the static initialization of Right, which already happened since the implied by
        // rights are Right instances (the internal manager always passes at least the admin and programming rights).
        return creator.create(description, impliedByRights);
    }
}

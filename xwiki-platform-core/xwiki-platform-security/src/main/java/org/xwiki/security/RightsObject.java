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
 *
 */
package org.xwiki.security;

import org.xwiki.model.reference.DocumentReference;

/**
 * Wrapper around xwiki rights objects.
 * @version $Id$
 */
public interface RightsObject
{
    /**
     * Check if the state of this object should be applied for a given right.
     * @param right The righ to check.
     * @return {@code true} if the state should be applied for the right,
     * othewise {@code false}.
     */
    boolean checkRight(Right right);

    /**
     * @return The {@cod RightState} of this object.
     */
    RightState getState();

    /**
     * Check if the state of this object should be applied to a given group.
     * @param group The group to check.
     * @return {@code true} if the state should be applied for group,
     * othewise {@code false}.
     */
    boolean checkGroup(DocumentReference group);

    /**
     * Check if the state of this object should be applied to a given user.
     * @param user The user to check.
     * @return {@code true} if the state should be applied for user,
     * othewise {@code false}.
     */
    boolean checkUser(DocumentReference user);
}

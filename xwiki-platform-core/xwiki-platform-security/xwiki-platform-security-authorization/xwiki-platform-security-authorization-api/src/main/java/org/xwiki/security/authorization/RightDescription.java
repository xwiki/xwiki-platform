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
package org.xwiki.security.authorization;

import java.util.Set;

import org.xwiki.model.EntityType;

/**
 * Describe a {@link Right}, allow adding new Rights, also implemented by the {@link Right} class.
 *
 * @version $Id$
 * @since 4.0M2
 */
public interface RightDescription
{
    /**
     * @return The string representation of this right.
     */
    String getName();

    /**
     * @return The default state, in case no matching right is found
     * at any level. Should be either {@link RuleState#ALLOW} or {@link RuleState#DENY}.
     */
    RuleState getDefaultState();

    /**
     * @return Whether this right should be allowed or denied in case
     * of a tie.
     */
    RuleState getTieResolutionPolicy();

    /**
     * @return Policy on how this right should be overridden by
     * lower levels in the entity reference hierarchy. When true,
     * this right on a document override this right on a wiki.
     */
    boolean getInheritanceOverridePolicy();

    /**
     * @return a set of additional rights implied by this right. Note that this method should
     * return {@code null} instead of an empty set.
     */
    Set<Right> getImpliedRights();

    /**
     * @return a set of entity type for which this right should be enabled. Special type Right.FARM (==null) could
     * be used to target the EntityType.WIKI for the main wiki only (i.e. PROGRAM)
     */
    Set<EntityType> getTargetedEntityType();

    /**
     * Used to check if this right should be allowed when the wiki is in read-only mode.
     * From the native right, only EDIT, DELETE, COMMENT and REGISTER returns false.
     *
     * @return true if this right allow a read-only access to the wiki.
     */
    boolean isReadOnly();
}


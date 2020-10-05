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
package org.xwiki.like.internal;

import java.util.Collections;
import java.util.Set;

import org.xwiki.model.EntityType;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightDescription;
import org.xwiki.security.authorization.RuleState;

/**
 * A programmatic right for Like feature.
 *
 * This right is allowed by default and implies Login and View rights.
 *
 * @version $Id$
 * @since 12.7RC1
 */
public final class LikeRight implements RightDescription
{
    /**
     * Singleton instance for a like right.
     */
    public static final LikeRight INSTANCE = new LikeRight();

    private LikeRight()
    {
    }

    @Override
    public String getName()
    {
        return "Like";
    }

    @Override
    public RuleState getDefaultState()
    {
        return RuleState.ALLOW;
    }

    @Override
    public RuleState getTieResolutionPolicy()
    {
        return RuleState.ALLOW;
    }

    @Override
    public boolean getInheritanceOverridePolicy()
    {
        return false;
    }

    @Override
    public Set<Right> getImpliedRights()
    {
        return null;
    }

    @Override
    public Set<EntityType> getTargetedEntityType()
    {
        return Right.WIKI_SPACE_DOCUMENT;
    }

    @Override
    public boolean isReadOnly()
    {
        return false;
    }
}

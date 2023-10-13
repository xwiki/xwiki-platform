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
package org.xwiki.platform.security.requiredrights;

import org.xwiki.model.EntityType;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a required right for an entity, composed of a {@link Right}, an {@link EntityType} and a boolean optional
 * field.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Unstable
public class RequiredRight
{
    private final Right right;

    private final EntityType entityType;

    private final boolean optional;

    /**
     * @param right the required right
     * @param entityType the level at which the right is required (e.g., document, space, wiki)
     * @param optional whether the right is optional or not, i.e., if the entity also works without the right or
     *     not
     */
    public RequiredRight(Right right, EntityType entityType, boolean optional)
    {
        this.right = right;
        this.entityType = entityType;
        this.optional = optional;
    }

    /**
     * @return the required right
     */
    public Right getRight()
    {
        return this.right;
    }

    /**
     * @return the level at which the right is required (e.g., document, space, wiki)
     */
    public EntityType getEntityType()
    {
        return this.entityType;
    }

    /**
     * @return whether the right is optional or not, i.e., if the entity also works without the right or not
     */
    public boolean isOptional()
    {
        return this.optional;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("right", getRight())
            .append("entityType", getEntityType())
            .append("optional", isOptional())
            .toString();
    }
}

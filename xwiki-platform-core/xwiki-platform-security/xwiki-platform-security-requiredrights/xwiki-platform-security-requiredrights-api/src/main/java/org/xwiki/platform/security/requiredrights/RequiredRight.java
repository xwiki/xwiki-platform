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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represents a required right for an entity, composed of a {@link Right}, an {@link EntityType} and a boolean
 * manualReviewNeeded field.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Unstable
public class RequiredRight implements Serializable
{
    /**
     * Programming right required.
     */
    public static final RequiredRight PROGRAM = new RequiredRight(Right.PROGRAM, null, false);

    /**
     * Programming right might be required, but a manual review is needed to confirm if the right is required.
     */
    public static final RequiredRight MAYBE_PROGRAM = new RequiredRight(Right.PROGRAM, null, true);

    /**
     * Script right required.
     */
    public static final RequiredRight SCRIPT = new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, false);

    /**
     * Script right might be required, but a manual review is needed to confirm if the right is required.
     */
    public static final RequiredRight MAYBE_SCRIPT = new RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, true);

    /**
     * Wiki-level admin right required.
     *
     * @since 15.10RC1
     */
    public static final RequiredRight WIKI_ADMIN = new RequiredRight(Right.ADMIN, EntityType.WIKI, false);

    /**
     * Script right is required, programming right might be required.
     */
    public static final List<RequiredRight> SCRIPT_AND_MAYBE_PROGRAM = List.of(SCRIPT, MAYBE_PROGRAM);

    /**
     * The serialization UID. The value is the value of the first version of this class that didn't have this field.
     */
    @Serial
    private static final long serialVersionUID = 4837678565544747071L;

    private final Right right;

    private final EntityType entityType;

    private final boolean manualReviewNeeded;

    /**
     * @param right the required right
     * @param entityType the level at which the right is required (e.g., document, space, wiki)
     * @param manualReviewNeeded whether a manual review is needed to confirm if the right is required or not
     */
    public RequiredRight(Right right, EntityType entityType, boolean manualReviewNeeded)
    {
        this.right = right;
        this.entityType = entityType;
        this.manualReviewNeeded = manualReviewNeeded;
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
     * @return whether a manual review is needed to confirm if the right is required or not.
     * This could be the case, e.g., when a title contains # or $ characters,
     * which are used in Velocity scripts but could also occur in a title without scripts.
     * Similarly, it is impossible to reliably determine automatically if a script needs programming right or not.
     */
    public boolean isManualReviewNeeded()
    {
        return this.manualReviewNeeded;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("right", getRight())
            .append("entityType", getEntityType())
            .append("manualReviewNeeded", isManualReviewNeeded())
            .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RequiredRight that = (RequiredRight) o;

        return new EqualsBuilder()
            .append(isManualReviewNeeded(), that.isManualReviewNeeded())
            .append(getRight(), that.getRight())
            .append(getEntityType(), that.getEntityType())
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(getRight())
            .append(getEntityType())
            .append(isManualReviewNeeded())
            .toHashCode();
    }
}

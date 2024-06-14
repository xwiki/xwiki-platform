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
package org.xwiki.netflux.internal;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Represents a change to an entity.
 *
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
public class EntityChange implements Comparable<EntityChange>
{
    /**
     * The script level for a given entity change.
     */
    public enum ScriptLevel
    {
        /** The change author is not allowed to execute scripts. */
        NO_SCRIPT,

        /** The change author is allowed to execute safe scripts. */
        SCRIPT,

        /** The change author is allowed to execute any type of script. */
        PROGRAMMING
    }

    /**
     * The entity that has been changed.
     */
    private final EntityReference entityReference;

    /**
     * The author of the change.
     */
    private final UserReference author;

    /**
     * The script level of the author of the change relative to the changed entity (e.g. whether the author of the
     * change is allowed to include scripts in the content of the changed entity).
     */
    private final ScriptLevel scriptLevel;

    /**
     * The timestamp of the change.
     */
    private final long timestamp = new Date().getTime();

    /**
     * Creates a new entity change instance.
     *
     * @param entityReference the changed entity
     * @param author the author of the change
     * @param scriptLevel the script level of the author of the change relative to the changed entity
     */
    public EntityChange(EntityReference entityReference, UserReference author, ScriptLevel scriptLevel)
    {
        this.entityReference = entityReference;
        this.author = author;
        this.scriptLevel = scriptLevel;
    }

    /**
     * @return the reference of the changed entity
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the author of the change
     */
    public UserReference getAuthor()
    {
        return author;
    }

    /**
     * @return the script level of the author of the change relative to the changed entity
     */
    public ScriptLevel getScriptLevel()
    {
        return scriptLevel;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(this.entityReference).append(this.author).append(this.scriptLevel)
            .append(this.timestamp).toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        EntityChange otherChange = (EntityChange) object;
        return new EqualsBuilder().append(this.entityReference, otherChange.entityReference)
            .append(this.author, otherChange.author).append(this.scriptLevel, otherChange.scriptLevel)
            .append(this.timestamp, otherChange.timestamp).isEquals();
    }

    @Override
    public int compareTo(EntityChange other)
    {
        // Sort by script level ascending (less script rights first) and by timestamp descending (most recent first).
        int scriptLevelDifference = this.scriptLevel.compareTo(other.scriptLevel);
        return scriptLevelDifference != 0 ? scriptLevelDifference : Long.compare(other.timestamp, this.timestamp);
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("entity", this.entityReference).append("author", this.author)
            .append("scriptLevel", this.scriptLevel).append("timestamp", this.timestamp).toString();
    }
}

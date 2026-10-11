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
package org.xwiki.yjs.websocket.internal;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Records a change in the script author of a Yjs room.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
public class ScriptAuthorChange implements Comparable<ScriptAuthorChange>
{
    /**
     * The script level associated to a Yjs room.
     */
    public enum ScriptLevel
    {
        /** Don't execute the scripts that are included in the content synchronized by the Yjs collaboration room. */
        NO_SCRIPT,

        /**
         * Execute the scripts that are included in the content synchronized by the Yjs collaboration room if they are
         * safe.
         */
        SCRIPT,

        /** Execute any type of script that are included in the content synchronized by the Yjs collaboration room. */
        PROGRAMMING
    }

    /**
     * The user that is the new script author of the Yjs room after this change.
     */
    private final UserReference author;

    /**
     * The script level of the Yjs room after this change.
     */
    private final ScriptLevel scriptLevel;

    /**
     * The timestamp of the change.
     */
    private final long timestamp;

    /**
     * Create a new script author change.
     *
     * @param author the user that is the new script author of the Yjs room
     * @param scriptLevel the script level of the Yjs room after this change
     */
    public ScriptAuthorChange(UserReference author, ScriptLevel scriptLevel)
    {
        this.author = author;
        this.scriptLevel = scriptLevel;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * @return the user that is the new script author of the Yjs room after this change
     */
    public UserReference getAuthor()
    {
        return this.author;
    }

    /**
     * @return the script level of the Yjs room after this change
     */
    public ScriptLevel getScriptLevel()
    {
        return this.scriptLevel;
    }

    @Override
    public int compareTo(ScriptAuthorChange other)
    {
        // Sort by script level ascending (less script rights first) and by timestamp descending (most recent first).
        int scriptLevelDifference = this.scriptLevel.compareTo(other.scriptLevel);
        return scriptLevelDifference != 0 ? scriptLevelDifference : Long.compare(other.timestamp, this.timestamp);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ScriptAuthorChange other = (ScriptAuthorChange) obj;
        return new EqualsBuilder().append(this.author, other.author).append(this.scriptLevel, other.scriptLevel)
            .append(this.timestamp, other.timestamp).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(this.author).append(this.scriptLevel).append(this.timestamp).toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("author", this.author).append("scriptLevel", this.scriptLevel)
            .append("timestamp", this.timestamp).toString();
    }
}

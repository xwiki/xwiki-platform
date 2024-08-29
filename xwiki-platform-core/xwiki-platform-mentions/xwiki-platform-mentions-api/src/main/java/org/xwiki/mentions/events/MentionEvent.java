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
package org.xwiki.mentions.events;

import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * The mention event.
 *
 * @version $Id$
 * @since 12.5RC1
 */
public class MentionEvent implements TargetableEvent
{
    /**
     * Name of the mention event.
     */
    public static final String EVENT_TYPE = "mentions.mention";

    private final Set<String> targets;

    private final MentionEventParams params;

    /**
     * Default constructor.
     *
     * @param targets Mention target (single user or members of a group).
     * @param params Additional mention parameters.
     */
    public MentionEvent(Set<String> targets, MentionEventParams params)
    {
        this.targets = targets;
        this.params = params;
    }

    @Override
    public Set<String> getTarget()
    {
        return this.targets;
    }

    /**
     *
     * @return Additional mention parameters.
     */
    public MentionEventParams getParams()
    {
        return this.params;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof MentionEvent;
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

        MentionEvent that = (MentionEvent) o;

        return new EqualsBuilder()
                   .append(this.targets, that.targets)
                   .append(this.params, that.params)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.targets)
                   .append(this.params)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("targets", this.getTarget())
                   .append("params", this.getParams())
                   .build();
    }
}

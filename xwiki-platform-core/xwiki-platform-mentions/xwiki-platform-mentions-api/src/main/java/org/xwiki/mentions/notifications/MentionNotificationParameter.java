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
package org.xwiki.mentions.notifications;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Holds a mention notification parameter, composed of the reference of the mentioned actor, the unique anchor
 * identifier of the mention in a page, and the display style of the mention.
 *
 * @version $Id$
 * @since 12.10
 */
public class MentionNotificationParameter implements Serializable
{
    private static final long serialVersionUID = -8933896168459931795L;

    private final String reference;

    private final String anchorId;

    private final DisplayStyle displayStyle;

    /**
     * @param reference the actor reference
     * @param anchorId the anchor identifier of the mention
     * @param displayStyle the mention display style
     */
    public MentionNotificationParameter(String reference, String anchorId, DisplayStyle displayStyle)
    {
        this.reference = reference;
        this.anchorId = anchorId;
        this.displayStyle = displayStyle;
    }

    /**
     * @return the actor reference
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @return the anchor identifier of the mention
     */
    public String getAnchorId()
    {
        return this.anchorId;
    }

    /**
     * @return the mention display style
     */
    public DisplayStyle getDisplayStyle()
    {
        return this.displayStyle;
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

        MentionNotificationParameter that = (MentionNotificationParameter) o;

        return new EqualsBuilder()
            .append(this.reference, that.reference)
            .append(this.anchorId, that.anchorId)
            .append(this.displayStyle, that.displayStyle)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.reference)
            .append(this.anchorId)
            .append(this.displayStyle)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("reference", this.getReference())
            .append("anchorId", this.getAnchorId())
            .append("displayStyle", this.getDisplayStyle())
            .build();
    }
}

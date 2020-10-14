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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

/**
 * Hold a mention notification parameter, composed of the reference of the mentioned actor and the unique anchor
 * identifier of the mention in a page.
 *
 * @since 12.10RC1
 * @version $Id$
 */
@Unstable
public class MentionNotificationParameter
{
    private final String reference;

    private final String anchorId;

    /**
     * @param reference the actor reference
     * @param anchorId the anchor identifier of the mention
     */
    public MentionNotificationParameter(String reference, String anchorId)
    {
        this.reference = reference;
        this.anchorId = anchorId;
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
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.reference)
            .append(this.anchorId)
            .toHashCode();
    }
}

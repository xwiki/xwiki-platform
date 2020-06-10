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
package org.xwiki.platform.mentions.events;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.platform.mentions.MentionLocation;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * The parameters of the mention event.
 * This class is serialized to be retrieved for notifications rendering.
 *
 * @version $Id$
 * @since 12.5RC1
 */
public class MentionEventParams
{
    private String userReference;

    private String documentReference;

    private MentionLocation location = MentionLocation.UNDEFINED;

    private String anchor;

    /**
     *
     * @return The the user doing the mention.
     */
    public String getUserReference()
    {
        return this.userReference;
    }

    /**
     *
     * @param userReference the user doing the mention.
     * @return the current object.
     */
    public MentionEventParams setUserReference(String userReference)
    {
        this.userReference = userReference;
        return this;
    }

    /**
     *
     * @return the document in which then mention occurs.
     */
    public String getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     *
     * @param documentReference the document in which then mention occurs.
     * @return the current object.
     */
    public MentionEventParams setDocumentReference(String documentReference)
    {
        this.documentReference = documentReference;
        return this;
    }

    /**
     *
     * @return the location of the mention.
     */
    public MentionLocation getLocation()
    {
        return this.location;
    }

    /**
     *
     * @param location the location of the mention.
     * @return the current object.
     */
    public MentionEventParams setLocation(MentionLocation location)
    {
        this.location = location;
        return this;
    }

    /**
     * @return the anchor to be used for the link to the mention.
     */
    public String getAnchor()
    {
        return anchor;
    }

    /**
     *
     * @param anchor the anchor to be used to link to the mention.
     * @return the current object.
     */
    public MentionEventParams setAnchor(String anchor)
    {
        this.anchor = anchor;
        return this;
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

        MentionEventParams that = (MentionEventParams) o;

        return new EqualsBuilder()
                   .append(this.userReference, that.userReference)
                   .append(this.documentReference, that.documentReference)
                   .append(this.location, that.location)
                   .append(this.anchor, that.anchor)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.userReference)
                   .append(this.documentReference)
                   .append(this.location)
                   .append(this.anchor)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("userReference", this.getUserReference())
                   .append("documentReference", this.getDocumentReference())
                   .append("location", this.getLocation())
                   .append("anchorId", this.getAnchor())
                   .build();
    }
}

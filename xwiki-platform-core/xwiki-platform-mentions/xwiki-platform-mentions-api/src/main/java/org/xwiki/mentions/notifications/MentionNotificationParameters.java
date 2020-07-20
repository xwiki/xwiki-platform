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
import org.xwiki.mentions.MentionLocation;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Object holding the parameters of the users mentions.
 * @version $Id$
 * @since 12.6RC1
 */
@Unstable
public class MentionNotificationParameters
{
    private final DocumentReference authorReference;

    private final DocumentReference documentReference;

    private final DocumentReference mentionedIdentity;

    private final MentionLocation location;

    private final String anchorId;

    private final XDOM xdom;

    /**
     * @param authorReference the reference of the author of the mention
     * @param documentReference the document in which the mention has been done
     * @param mentionedIdentity the identity of the mentioned user
     * @param location the location of the mention
     * @param anchorId the anchor link to use
     * @param xdom the content xdom
     */
    public MentionNotificationParameters(DocumentReference authorReference, DocumentReference documentReference,
        DocumentReference mentionedIdentity, MentionLocation location, String anchorId, XDOM xdom)
    {
        this.authorReference = authorReference;
        this.documentReference = documentReference;
        this.mentionedIdentity = mentionedIdentity;
        this.location = location;
        this.anchorId = anchorId;
        this.xdom = xdom;
    }

    /**
     *
     * @return the reference of the author of the mention
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     *
     * @return the document in which the mention has been done
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     *
     * @return the identity of the mentioned user
     */
    public DocumentReference getMentionedIdentity()
    {
        return this.mentionedIdentity;
    }

    /**
     *
     * @return the location of the mention
     */
    public MentionLocation getLocation()
    {
        return this.location;
    }

    /**
     *
     * @return the anchor link to use
     */
    public String getAnchorId()
    {
        return this.anchorId;
    }

    /**
     *
     * @return the content xdom
     */
    public XDOM getXdom()
    {
        return this.xdom;
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

        MentionNotificationParameters that = (MentionNotificationParameters) o;

        return new EqualsBuilder()
                   .append(this.authorReference, that.authorReference)
                   .append(this.documentReference, that.documentReference)
                   .append(this.mentionedIdentity, that.mentionedIdentity)
                   .append(this.location, that.location)
                   .append(this.anchorId, that.anchorId)
                   .append(this.xdom, that.xdom)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.authorReference)
                   .append(this.documentReference)
                   .append(this.mentionedIdentity)
                   .append(this.location)
                   .append(this.anchorId)
                   .append(this.xdom)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("authorReference", this.getAuthorReference())
                   .append("documentReference", this.getDocumentReference())
                   .append("mentionedIdentity", this.getMentionedIdentity())
                   .append("location", this.getLocation())
                   .append("anchorId", this.getAnchorId())
                   .append("xdom", this.getXdom())
                   .build();
    }
}

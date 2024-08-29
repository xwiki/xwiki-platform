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
package com.xpn.xwiki.doc;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Represent the relation between a document using a link to an entity, and the used entity.
 * 
 * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
 */
@Deprecated(since = "14.8RC1")
public class XWikiLink
{
    private long id;
    
    private long docId;

    private String link;

    private String fullName;

    private String type;

    private String attachmentName;

    /**
     * Default constructor.
     */
    public XWikiLink()
    {
        this.setDocId(0);
    }

    /**
     * Initializes only the docId.
     *
     * @param docId the value of the docId
     */
    public XWikiLink(long docId)
    {
        this.setDocId(docId);
    }

    /**
     * Initializes the docId as well as the link (the used entity) and the fullName (the reference of the document using
     * the entity). The type is initialized to "document" by default.
     *
     * @param docId the docId of the document using the entity
     * @param link the link of the used entity
     * @param fullName the full name of the document using the entity
     */
    public XWikiLink(long docId, String link, String fullName)
    {
        this.setDocId(docId);
        this.setLink(link);
        this.setFullName(fullName);
        this.setType("document");
    }

    /**
     * Initializes the docId as well as the link (the used entity), the fullName (the reference of the document using
     * the entity), and the type of the linked entity.
     *
     * @param docId the docId of the document using the entity
     * @param link the link of the used entity
     * @param fullName the full name of the document using the entity
     * @param type the type of the used entity
     * @since 14.2RC1
     */
    public XWikiLink(long docId, String link, String fullName, String type)
    {
        this.setDocId(docId);
        this.setLink(link);
        this.setFullName(fullName);
        this.setType(type);
    }

    /**
     * Getter for {@link #id}.
     *
     * @return the synthetic id of this deleted attachment. Uniquely identifies a link
     * @since 14.2RC1
     */
    public long getId()
    {
        return this.id;
    }

    /**
     * Setter for {@link #id}.
     *
     * @param id the synthetic id to set. Used only by hibernate
     */
    // This method is private because it is only used reflexively by Hibernate.
    @SuppressWarnings("java:S1144")
    private void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the id of the document containing the link
     */
    public long getDocId()
    {
        return this.docId;
    }

    public void setDocId(long docId)
    {
        this.docId = docId;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    /**
     * @return the serialized document reference of the link target (compact form without the wiki part if it matches
     *         the current wiki)
     */
    public String getLink()
    {
        return this.link;
    }

    /**
     * @return the serialized reference of the document which contains the link (without the wiki part)
     */
    public String getFullName()
    {
        return this.fullName;
    }

    public void setFullName(String fullName)
    {
        this.fullName = fullName;
    }

    /**
     * @param type the type of the link (e.g., document or attachment)
     * @since 14.2RC1
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return the type of the link (e.g., document or attachment)
     * @since 14.2RC1
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param attachmentName the name of the attachment if the link is an attachment, {@code null} otherwise
     * @since 14.2RC1
     */
    public void setAttachmentName(String attachmentName)
    {
        this.attachmentName = attachmentName;
    }

    /**
     * @return the name of the attachment if the link is an attachment, {@code null} otherwise
     * @since 14.2RC1
     */
    public String getAttachmentName()
    {
        return this.attachmentName;
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

        XWikiLink xWikiLink = (XWikiLink) o;

        return new EqualsBuilder()
            .append(this.docId, xWikiLink.docId)
            .append(this.link, xWikiLink.link)
            .append(this.fullName, xWikiLink.fullName)
            .append(this.type, xWikiLink.type)
            .append(this.attachmentName, xWikiLink.attachmentName)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.docId)
            .append(this.link)
            .append(this.fullName)
            .append(this.type)
            .append(this.attachmentName)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("DocId", getDocId())
            .append("FullName", getFullName())
            .append("Link", getLink())
            .append("Type", getType())
            .append("AttachmentName", getAttachmentName())
            .toString();
    }
}

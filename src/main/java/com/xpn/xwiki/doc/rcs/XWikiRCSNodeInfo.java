/*
 * Copyright 2007, XpertNet SARL, and individual contributors.
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
 *
 */
package com.xpn.xwiki.doc.rcs;

import java.lang.ref.SoftReference;
import java.util.Date;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Contains information about document version.
 * Mutable.
 * @version $Id: $
 */
public class XWikiRCSNodeInfo implements Comparable
{
    /**
     * composite primary id of class. 
     */
    private XWikiRCSNodeId    id;
    /**
     * date of this modification.
     */
    private Date    date      = new Date();
    /**
     * author of modification.
     */
    private String  author    = "XWiki.XWikiGuest";
    /**
     * modification's comment.
     */
    private String  comment   = "";
    /**
     * is this version diff or full version. read-only
     */
    private boolean isDiff     = true;
    /**
     * reference to its XWikiRCSNodeContent.
     */
    private SoftReference contentRef;
    /**
     * default constructor used in Hibernate to load this class.
     */
    public XWikiRCSNodeInfo() { }
    /**
     * @param id - primary key.
     */
    public XWikiRCSNodeInfo(XWikiRCSNodeId id)
    {
        setId((XWikiRCSNodeId) id.clone());
    }
    /**
     * @return primary key.
     */
    public XWikiRCSNodeId getId()
    {
        return id;
    }
    /**
     * @param id - primary key.
     */
    public void setId(XWikiRCSNodeId id)
    {
        this.id = id;
    }
    /**
     * @return date of this modification.
     */
    public Date getDate()
    {
        return date;
    }
    /**
     * @param updateDate - date of this modification.
     */
    public void setDate(Date updateDate)
    {
        this.date = updateDate;
    }
    /**
     * @return get author of modification.
     */
    public String getAuthor()
    {
        return author;
    }
    /**
     * @param updateAuthor - author of modification.
     */
    public void setAuthor(String updateAuthor)
    {
        this.author = updateAuthor;
    }
    /**
     * @return modification's comment.
     */
    public String getComment()
    {
        return comment;
    }
    /**
     * @param comment - modification's comment.
     */
    public void setComment(String comment)
    {
        this.comment = comment;
    }
    /**
     * @return is modification minor.
     */
    public boolean isMinorEdit()
    {
        return id.getVersion().at(1) != 1;
    }
    /**
     * @return is patch or full version.
     */
    public boolean isDiff()
    {
        return isDiff;
    }
    /**
     * @param diff - is patch (true) or full version (false).
     * Should not be used dirrectly.
     * @see XWikiPatch#setDiff(boolean)
     */
    public void setDiff(boolean diff)
    {
        this.isDiff = diff;
    }

    /**
     * @return {@link XWikiRCSNodeContent} for this node.
     * @param context - load with this context. If null then do not load.
     * @throws XWikiException if can't load
     */
    public XWikiRCSNodeContent getContent(XWikiContext context) throws XWikiException
    {
        XWikiRCSNodeContent nodeContent = null;
        if (contentRef != null) {
            nodeContent = (XWikiRCSNodeContent) contentRef.get();
        }
        if (nodeContent != null || context == null) {
            return nodeContent;
        }
        nodeContent = context.getWiki().getVersioningStore()
            .loadRCSNodeContent(context, this.id, true);
        contentRef = new SoftReference(nodeContent);
        return nodeContent;
    }
    /**
     * @param content - {@link XWikiRCSNodeContent} for this node.
     */
    public void setContent(XWikiRCSNodeContent content)
    {
        content.setId(getId());
        contentRef = new SoftReference(content);
        setDiff(content.getPatch().isDiff());
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }
    /**
     * {@inheritDoc}
     */
    public int compareTo(Object arg0)
    {
        final XWikiRCSNodeInfo o = (XWikiRCSNodeInfo) arg0;
        return getId().getVersion().compareTo(o.getId().getVersion());
    }
}

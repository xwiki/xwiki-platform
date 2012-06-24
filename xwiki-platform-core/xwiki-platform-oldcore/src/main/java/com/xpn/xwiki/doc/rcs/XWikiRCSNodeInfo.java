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
package com.xpn.xwiki.doc.rcs;

import java.lang.ref.SoftReference;
import java.util.Date;

import org.suigeneris.jrcs.rcs.Version;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.AbstractSimpleClass;

/**
 * Contains information about document version.
 * Mutable.
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiRCSNodeInfo extends AbstractSimpleClass implements Comparable<XWikiRCSNodeInfo>
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
    private String  author    = XWikiRightService.GUEST_USER_FULLNAME;
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
    private SoftReference<XWikiRCSNodeContent> contentRef;
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
        // For Oracle and other databases, an empty string is equivalent to a NULL and thus
        // we had to remove the NOT-NULL condition on this field. Hence we need to test if it's
        // null here and return an empty string so that all code calling this will not be impacted.
        return author != null ? author : "";
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
        // For Oracle and other databases, an empty string is equivalent to a NULL and thus
        // we had to remove the NOT-NULL condition on this field. Hence we need to test if it's
        // null here and return an empty string so that all code calling this will not be impacted.
        return comment != null ? comment : "";
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
     * Should not be used directly.
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
            .loadRCSNodeContent(this.id, true, context);
        contentRef = new SoftReference<XWikiRCSNodeContent>(nodeContent);
        return nodeContent;
    }
    /**
     * @param content - {@link XWikiRCSNodeContent} for this node.
     */
    public void setContent(XWikiRCSNodeContent content)
    {
        content.setId(getId());
        contentRef = new SoftReference<XWikiRCSNodeContent>(content);
        setDiff(content.getPatch().isDiff());
    }

    /**
     * @return version of this revision.
     */
    public Version getVersion()
    {
        return getId().getVersion();
    }

    @Override
    public int compareTo(XWikiRCSNodeInfo o)
    {
        return getId().getVersion().compareTo(o.getId().getVersion());
    }
}

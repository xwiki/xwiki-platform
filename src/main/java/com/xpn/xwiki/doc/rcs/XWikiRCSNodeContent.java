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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Contains differences between document versions.
 * Mutable.
 * @version $Id: $
 */
public class XWikiRCSNodeContent implements Comparable
{
    /** composite primary id of class. Not null. */
    private XWikiRCSNodeId  id;
    /** Diff or full version of revision. Not null.  */
    private XWikiPatch      patch;
    /**
     * default constructor used in Hibernate to load this class.
     */
    public XWikiRCSNodeContent() { }
    /**
     * @param id - primary key
     */
    public XWikiRCSNodeContent(XWikiRCSNodeId id)
    {
        setId((XWikiRCSNodeId) id.clone());
    }
    /**
     * @return primary key
     */
    public XWikiRCSNodeId getId()
    {
        return id;
    }
    /**
     * @param id = primary key
     */
    public void setId(XWikiRCSNodeId id)
    {
        // mutable, so clone is needed
        this.id = (XWikiRCSNodeId) id.clone();
    }
    /**
     * @return patch for this revision
     * @see XWikiPatch
     */
    public XWikiPatch getPatch()
    {
        return patch;
    }
    /**
     * @param patch - patch for this revision
     * @see XWikiPatch
     */
    public void setPatch(XWikiPatch patch)
    {
        this.patch = patch;
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
        return new ToStringBuilder(this)
            .append("id", getId())
            .toString();
    }
    /**
     * {@inheritDoc}
     */
    public int compareTo(Object arg0)
    {
        final XWikiRCSNodeContent o = (XWikiRCSNodeContent) arg0;
        return getId().getVersion().compareTo(o.getId().getVersion());
    }
}

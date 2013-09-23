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

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.xpn.xwiki.util.AbstractSimpleClass;

/**
 * Contains differences between document versions.
 * Mutable.
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiRCSNodeContent extends AbstractSimpleClass 
    implements Comparable<XWikiRCSNodeContent>
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
    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", getId())
            .toString();
    }
    @Override
    public int compareTo(XWikiRCSNodeContent o)
    {
        return getId().getVersion().compareTo(o.getId().getVersion());
    }
}

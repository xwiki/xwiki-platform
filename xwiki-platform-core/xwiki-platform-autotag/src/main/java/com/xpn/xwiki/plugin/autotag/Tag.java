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
package com.xpn.xwiki.plugin.autotag;

import org.xwiki.xml.XMLUtils;

/**
 * Data structure used by the {@link AutoTagPlugin}, holding information about a particular tag, or a frequent word
 * appearing in a collection of documents.
 *
 * @version $Id$
 * @deprecated the entire Autotag plugin is deprecated, along with its data structures
 */
@Deprecated
public class Tag implements Comparable<Tag>
{
    /** The keyword represented by this object. */
    private String name;

    /** The visual size of this tag in the HTML {@link TagCloud}. */
    private long size;

    /**
     * Default constructor, specifying both the keyword and its target size.
     *
     * @param tagName the keyword that's represented
     * @param tagSize the tag size
     */
    public Tag(String tagName, long tagSize)
    {
        this.size = tagSize;
        this.name = tagName;
    }

    /**
     * Get the HTML markup to represent this tag in a {@link TagCloud}.
     *
     * @return HTML markup
     */
    public String getHtml()
    {
        return "<a class=\"f" + this.size + "\">" + XMLUtils.escapeElementContent(this.name) + "</a> ";
    }

    @Override
    public int compareTo(Tag o)
    {
        return -o.name.compareTo(this.name);
    }
}

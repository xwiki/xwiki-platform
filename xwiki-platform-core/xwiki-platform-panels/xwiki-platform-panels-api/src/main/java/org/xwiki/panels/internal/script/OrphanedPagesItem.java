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
package org.xwiki.panels.internal.script;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Contains the information required to display the orphaned page tree, namely a list of orphaned page references, the
 * next offset, and if their is more orphan pages to be queried.
 *
 * @version $Id$
 * @since 13.1RC1
 * @since 12.10.4
 * @since 12.6.8
 */
public class OrphanedPagesItem
{
    private final List<String> orphanedPages;

    private final int offset;

    private final boolean hasMore;

    /**
     * Default constructor, initializing all the fields of this object.
     *
     * @param orphanedPages the list of orphaned pages
     * @param offset the next offset
     * @param hasMore {@code true} if their is more orphaned pages to retrieve, {@code false} otherwise
     */
    public OrphanedPagesItem(List<String> orphanedPages, int offset, boolean hasMore)
    {
        this.orphanedPages = orphanedPages;
        this.offset = offset;
        this.hasMore = hasMore;
    }

    /**
     * @return a list of orphaned pages
     */
    public List<String> getOrphanedPages()
    {
        return this.orphanedPages;
    }

    /**
     * @return the offset for the next query
     */
    public int getOffset()
    {
        return this.offset;
    }

    /**
     * @return {@code true} if their is more orphaned pages to retrieve, {@code false} otherwise
     */
    public boolean getHasMore()
    {
        return this.hasMore;
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

        OrphanedPagesItem that = (OrphanedPagesItem) o;

        return new EqualsBuilder()
            .append(this.offset, that.offset)
            .append(this.hasMore, that.hasMore)
            .append(this.orphanedPages, that.orphanedPages)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.orphanedPages)
            .append(this.offset)
            .append(this.hasMore)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("orphanedPages", this.orphanedPages)
            .append("offset", this.offset)
            .append("hasMore", this.hasMore)
            .toString();
    }
}

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
package org.xwiki.doc.rights;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since x.y.z
 */
@Unstable
public class XWikiDocumentRequiredRights
{
    private long id;

    private long docId;

    private Right right;

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

    public long getDocId()
    {
        return this.docId;
    }

    public void setDocId(long docId)
    {
        this.docId = docId;
    }

    // @Enumerated(EnumType.STRING) if we want to map the string representation
    public Right getRight()
    {
        return this.right;
    }

    public void setRight(Right right)
    {
        this.right = right;
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

        XWikiDocumentRequiredRights that = (XWikiDocumentRequiredRights) o;

        return new EqualsBuilder()
            .append(this.id, that.id)
            .append(this.docId, that.docId)
            .append(this.right, that.right)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.id)
            .append(this.docId)
            .append(this.right)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("id", this.id)
            .append("docId", this.docId)
            .append("right", this.right)
            .toString();
    }
}

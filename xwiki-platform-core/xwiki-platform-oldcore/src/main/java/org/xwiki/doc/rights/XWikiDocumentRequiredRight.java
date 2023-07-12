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
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Database level object to store a required {@link Right} of a {@link XWikiDocument}.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@Unstable
public class XWikiDocumentRequiredRight
{
    private long id;

    private long docId;

    private Right right;

    /**
     * @return the table id of the required right
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
     * @return the document id of the required right
     */
    public long getDocId()
    {
        return this.docId;
    }

    /**
     * @param docId the document id of the required right
     */
    public void setDocId(long docId)
    {
        this.docId = docId;
    }

    /**
     * @return a required right of a document
     */
    public Right getRight()
    {
        return this.right;
    }

    /**
     * @param right a required right of a document
     */
    public void setRight(Right right)
    {
        this.right = right;
    }

    /**
     * @return the right name, derived from the stored {@link #right} by calling {@link Right#getName()}. {@code null}
     *     in case of {@code null} {@link #right}
     */
    // This method is private because it is only used reflexively by Hibernate.
    @SuppressWarnings("java:S1144")
    private String getRightName()
    {
        if (this.right == null) {
            return null;
        }
        return this.right.getName();
    }

    /**
     * @param rightName the right name, set from the string representation of the right stored in database and
     *     converted to a {@link Right} by calling {@link Right#toRight(String)}
     */
    // This method is private because it is only used reflexively by Hibernate.
    @SuppressWarnings("java:S1144")
    private void setRightName(String rightName)
    {
        this.right = Right.toRight(rightName);
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

        XWikiDocumentRequiredRight that = (XWikiDocumentRequiredRight) o;

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
        return new XWikiToStringBuilder(this)
            .append("id", this.id)
            .append("docId", this.docId)
            .append("right", this.right)
            .toString();
    }
}

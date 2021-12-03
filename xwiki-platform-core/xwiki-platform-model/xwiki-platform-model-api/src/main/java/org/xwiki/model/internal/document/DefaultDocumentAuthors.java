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
package org.xwiki.model.internal.document;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.user.UserReference;

/**
 * Default implementation of {@link DocumentAuthors}.
 *
 * @version $Id$
 * @since 13.10RC1
 */
public class DefaultDocumentAuthors implements DocumentAuthors
{
    private UserReference contentAuthor;
    private UserReference metadataAuthor;
    private UserReference displayedAuthor;
    private UserReference creator;

    /**
     * Default empty constructor.
     */
    public DefaultDocumentAuthors()
    {
    }

    /**
     * Default constructor cloning the given document authors.
     *
     * @param documentAuthors the authors information to be cloned.
     */
    public DefaultDocumentAuthors(DocumentAuthors documentAuthors)
    {
        if (documentAuthors != null) {
            this.contentAuthor = documentAuthors.getContentAuthor();
            this.metadataAuthor = documentAuthors.getMetadataAuthor();
            this.displayedAuthor = documentAuthors.getDisplayedAuthor();
            this.creator = documentAuthors.getCreator();
        }
    }

    /**
     * Specify the author of the content of the document: this author is only responsible to the content, and not to
     * other information of the document such as xobjects.
     *
     * @param contentAuthor the author of the content of the document.
     * @return the current instance for Builder pattern.
     */
    public DefaultDocumentAuthors setContentAuthor(UserReference contentAuthor)
    {
        this.contentAuthor = contentAuthor;
        return this;
    }

    /**
     * Specify the metddata author of the document: this author is not responsible to the content, but responsible to
     * the xobjects and other metadata.
     *
     * @param metdataAuthor the author of the metadata of the document.
     * @return the current instance for builder pattern.
     */
    public DefaultDocumentAuthors setMetadataAuthor(UserReference metdataAuthor)
    {
        this.metadataAuthor = metdataAuthor;
        return this;
    }

    /**
     * Specify the displayed author of the document: this author is only there for display purpose, and might be
     * different from other authors if the document has been saved through a script for example.
     *
     * @param displayedAuthor the author to display.
     * @return the current instance for builder pattern.
     */
    public DefaultDocumentAuthors setDisplayedAuthor(UserReference displayedAuthor)
    {
        this.displayedAuthor = displayedAuthor;
        return this;
    }

    /**
     * Specify the original creator of the document.
     *
     * @param creator the creator of the document.
     * @return the current instance for builder pattern.
     */
    public DefaultDocumentAuthors setCreator(UserReference creator)
    {
        this.creator = creator;
        return this;
    }

    @Override
    public UserReference getContentAuthor()
    {
        return this.contentAuthor;
    }

    @Override
    public UserReference getMetadataAuthor()
    {
        return this.metadataAuthor;
    }

    @Override
    public UserReference getDisplayedAuthor()
    {
        if (this.displayedAuthor == null) {
            return this.getMetadataAuthor();
        } else {
            return this.displayedAuthor;
        }
    }

    @Override
    public UserReference getCreator()
    {
        return creator;
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

        DefaultDocumentAuthors that = (DefaultDocumentAuthors) o;

        return new EqualsBuilder()
            .append(contentAuthor, that.contentAuthor)
            .append(metadataAuthor, that.metadataAuthor)
            .append(displayedAuthor, that.displayedAuthor)
            .append(creator, this.creator)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(contentAuthor)
            .append(metadataAuthor)
            .append(displayedAuthor)
            .append(creator)
            .toHashCode();
    }
}

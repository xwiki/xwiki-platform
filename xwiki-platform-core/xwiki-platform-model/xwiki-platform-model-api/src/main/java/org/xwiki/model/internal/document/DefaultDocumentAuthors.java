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
 * @since 14.0RC1
 */
public class DefaultDocumentAuthors implements DocumentAuthors
{
    private UserReference contentAuthor;
    private UserReference effectiveMetadataAuthor;
    private UserReference originalMetadataAuthor;
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
            this.effectiveMetadataAuthor = documentAuthors.getEffectiveMetadataAuthor();
            this.originalMetadataAuthor = documentAuthors.getOriginalMetadataAuthor();
            this.creator = documentAuthors.getCreator();
        }
    }

    @Override
    public void setContentAuthor(UserReference contentAuthor)
    {
        this.contentAuthor = contentAuthor;
    }

    @Override
    public void setEffectiveMetadataAuthor(UserReference metdataAuthor)
    {
        this.effectiveMetadataAuthor = metdataAuthor;
    }

    @Override
    public void setOriginalMetadataAuthor(UserReference originalMetadataAuthor)
    {
        this.originalMetadataAuthor = originalMetadataAuthor;
    }

    @Override
    public void setCreator(UserReference creator)
    {
        this.creator = creator;
    }

    @Override
    public UserReference getContentAuthor()
    {
        return this.contentAuthor;
    }

    @Override
    public UserReference getEffectiveMetadataAuthor()
    {
        return this.effectiveMetadataAuthor;
    }

    @Override
    public UserReference getOriginalMetadataAuthor()
    {
        if (this.originalMetadataAuthor == null) {
            return this.getEffectiveMetadataAuthor();
        } else {
            return this.originalMetadataAuthor;
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
            .append(effectiveMetadataAuthor, that.effectiveMetadataAuthor)
            .append(originalMetadataAuthor, that.originalMetadataAuthor)
            .append(creator, this.creator)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(contentAuthor)
            .append(effectiveMetadataAuthor)
            .append(originalMetadataAuthor)
            .append(creator)
            .toHashCode();
    }
}

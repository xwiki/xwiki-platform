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

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link DocumentAuthors}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
public class DefaultDocumentAuthors implements DocumentAuthors
{
    private final XWikiDocument documentHolder;
    private UserReference contentAuthor;
    private UserReference effectiveMetadataAuthor;
    private UserReference originalMetadataAuthor;
    private UserReference creator;

    /**
     * Default constructor.
     *
     * @param documentHolder the document which holds those authors.
     */
    public DefaultDocumentAuthors(XWikiDocument documentHolder)
    {
        this.documentHolder = documentHolder;
        this.contentAuthor = GuestUserReference.INSTANCE;
        this.effectiveMetadataAuthor = GuestUserReference.INSTANCE;
        this.originalMetadataAuthor = GuestUserReference.INSTANCE;
        this.creator = GuestUserReference.INSTANCE;
    }

    @Override
    public void copyAuthors(DocumentAuthors documentAuthors)
    {
        // Use setters those that metadata dirty are updated if needed
        this.setContentAuthor(documentAuthors.getContentAuthor());
        this.setEffectiveMetadataAuthor(documentAuthors.getEffectiveMetadataAuthor());
        this.setOriginalMetadataAuthor(documentAuthors.getOriginalMetadataAuthor());
        this.setCreator(documentAuthors.getCreator());
    }

    @Override
    public void setContentAuthor(UserReference contentAuthor)
    {
        if (!Objects.equals(this.contentAuthor, contentAuthor)) {
            this.contentAuthor = contentAuthor;
            this.flagMetadataDirty();
        }
    }

    @Override
    public void setEffectiveMetadataAuthor(UserReference metadataAuthor)
    {
        if (!Objects.equals(this.effectiveMetadataAuthor, metadataAuthor)) {
            this.effectiveMetadataAuthor = metadataAuthor;
            this.flagMetadataDirty();
        }
    }

    @Override
    public void setOriginalMetadataAuthor(UserReference originalMetadataAuthor)
    {

        if (!Objects.equals(this.originalMetadataAuthor, originalMetadataAuthor)) {
            this.originalMetadataAuthor = originalMetadataAuthor;
            this.flagMetadataDirty();
        }
    }

    @Override
    public void setCreator(UserReference creator)
    {
        if (!Objects.equals(this.creator, creator)) {
            this.creator = creator;
            this.flagMetadataDirty();
        }
    }

    private void flagMetadataDirty()
    {
        this.documentHolder.setMetaDataDirty(true);
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
        return this.originalMetadataAuthor;
    }

    @Override
    public UserReference getCreator()
    {
        return creator;
    }

    // We don't check document holder in equals and hashcode to avoid recursive calls.
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
            .append(this.contentAuthor, that.contentAuthor)
            .append(this.effectiveMetadataAuthor, that.effectiveMetadataAuthor)
            .append(this.originalMetadataAuthor, that.originalMetadataAuthor)
            .append(this.creator, that.creator)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 67)
            .append(this.contentAuthor)
            .append(this.effectiveMetadataAuthor)
            .append(this.originalMetadataAuthor)
            .append(this.creator)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("documentHolder", this.documentHolder)
            .append("contentAuthor", this.contentAuthor)
            .append("effectiveMetadataAuthor", this.effectiveMetadataAuthor)
            .append("originalMetadataAuthor", this.originalMetadataAuthor)
            .append("creator", this.creator)
            .toString();
    }
}

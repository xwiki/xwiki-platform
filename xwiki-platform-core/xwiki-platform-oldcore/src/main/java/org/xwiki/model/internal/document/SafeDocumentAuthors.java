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

import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.script.safe.AbstractSafeObject;
import org.xwiki.script.wrap.AbstractWrappingObject;
import org.xwiki.user.UserReference;

/**
 * Safe implementation of {@link DocumentAuthors} that doesn't allow to use any setters. This implementation aims at
 * being used in script services. All setters throw an {@link UnsupportedOperationException}.
 *
 * @version $Id$
 * @since 14.10
 * @since 14.4.7
 */
public class SafeDocumentAuthors extends AbstractWrappingObject<DocumentAuthors> implements DocumentAuthors
{
    /**
     * Default constructor.
     *
     * @param documentAuthors the wrapped authors to use for getters.
     */
    public SafeDocumentAuthors(DocumentAuthors documentAuthors)
    {
        super(documentAuthors);
    }

    @Override
    public UserReference getContentAuthor()
    {
        return getWrapped().getContentAuthor();
    }

    @Override
    public void setContentAuthor(UserReference contentAuthor)
    {
        throw new UnsupportedOperationException(AbstractSafeObject.FORBIDDEN);
    }

    @Override
    public UserReference getEffectiveMetadataAuthor()
    {
        return getWrapped().getEffectiveMetadataAuthor();
    }

    @Override
    public void setEffectiveMetadataAuthor(UserReference metadataAuthor)
    {
        throw new UnsupportedOperationException(AbstractSafeObject.FORBIDDEN);
    }

    @Override
    public UserReference getOriginalMetadataAuthor()
    {
        return getWrapped().getOriginalMetadataAuthor();
    }

    @Override
    public void setOriginalMetadataAuthor(UserReference originalMetadataAuthor)
    {
        throw new UnsupportedOperationException(AbstractSafeObject.FORBIDDEN);
    }

    @Override
    public UserReference getCreator()
    {
        return getWrapped().getCreator();
    }

    @Override
    public void setCreator(UserReference creator)
    {
        throw new UnsupportedOperationException(AbstractSafeObject.FORBIDDEN);
    }

    @Override
    public void copyAuthors(DocumentAuthors documentAuthors)
    {
        throw new UnsupportedOperationException(AbstractSafeObject.FORBIDDEN);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof SafeDocumentAuthors) {
            return Objects.equals(this.getWrapped(), ((SafeDocumentAuthors) obj).getWrapped());
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}

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
package org.xwiki.user.internal.document;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.user.UserReference;

/**
 * Represent a reference to a user stored in a wiki page.
 *
 * Always go through a {@link org.xwiki.user.UserReferenceResolver} to get a
 * {@link DocumentUserReference} object. The main reason is because the resolvers know how to handle Guest and
 * SuperAdmin user references properly.
 *
 * @version $Id$
 */
public class DocumentUserReference implements UserReference
{
    private EntityReferenceProvider entityReferenceProvider;

    private DocumentReference reference;

    /**
     * @param reference the reference to the wiki page storing the user
     * @param entityReferenceProvider the component to check if the current wiki is the main wiki
     */
    public DocumentUserReference(DocumentReference reference, EntityReferenceProvider entityReferenceProvider)
    {
        this.reference = reference;
        this.entityReferenceProvider = entityReferenceProvider;
    }

    /**
     * @return the reference to the document storing the user
     */
    public DocumentReference getReference()
    {
        return this.reference;
    }

    @Override
    public boolean isGlobal()
    {
        return this.entityReferenceProvider.getDefaultReference(EntityType.WIKI).equals(
            getReference().getWikiReference());
    }

    @Override
    public int hashCode()
    {
        return this.reference.hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        DocumentUserReference rhs = (DocumentUserReference) object;
        return new EqualsBuilder()
            .append(getReference(), rhs.getReference())
            .isEquals();
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("reference", getReference());
        return builder.toString();
    }
}

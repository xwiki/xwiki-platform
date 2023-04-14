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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.document.RequiredRights;
import org.xwiki.security.authorization.Right;
import org.xwiki.text.XWikiToStringBuilder;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of the required rights, with no specific restrictions. A read-only implementation exists to be
 * exposed to users with restricted rights, see {@link SafeRequiredRights}.
 *
 * @version $Id$
 * @since 15.3RC1
 */
public class DefaultRequiredRights implements RequiredRights
{
    private final Set<Right> requiredRights;

    private final XWikiDocument document;

    /**
     * Default constructor.
     *
     * @param document the document containing the required rights
     * @param requiredRights the set of required rights of the document (e.g., {@link Right#SCRIPT})
     */
    public DefaultRequiredRights(XWikiDocument document, Set<Right> requiredRights)
    {
        this.document = document;
        if (requiredRights != null) {
            this.requiredRights = new HashSet<>(requiredRights);
        } else {
            this.requiredRights = new HashSet<>();
        }
    }

    @Override
    public Set<Right> getRights()
    {
        return Collections.unmodifiableSet(this.requiredRights);
    }

    @Override
    public boolean has(Right right)
    {
        return getRights().contains(right);
    }

    @Override
    public boolean activated()
    {
        return this.document.getRequiredRightsActivated();
    }

    @Override
    public void setRights(Set<Right> newRights)
    {
        this.requiredRights.clear();
        this.requiredRights.addAll(newRights);
        this.document.setMetaDataDirty(true);
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

        DefaultRequiredRights that = (DefaultRequiredRights) o;

        return new EqualsBuilder()
            .append(this.requiredRights, that.requiredRights)
            .append(this.document, that.document)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37).append(this.requiredRights).append(this.document).toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("requiredRights", this.requiredRights)
            .append("document", this.document.getDocumentReference())
            .toString();
    }
}

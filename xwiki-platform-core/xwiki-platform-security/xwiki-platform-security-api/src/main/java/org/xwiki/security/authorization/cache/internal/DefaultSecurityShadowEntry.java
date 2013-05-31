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

package org.xwiki.security.authorization.cache.internal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.cache.SecurityShadowEntry;

/**
 * Default implementation of the security shadow entry.
 *
 * @version $Id$
 */
public class DefaultSecurityShadowEntry implements SecurityShadowEntry
{
    /** The wiki where the user is shadowed. */
    private SecurityReference wikiReference;

    /** The user/group shadowed by this entry. */
    private UserSecurityReference user;

    /**
     * Build a new shadow entry from given references.
     * @param user The user shadowed by this entry.
     * @param wikiReference The wiki where the user is shadowed.
     */
    DefaultSecurityShadowEntry(UserSecurityReference user, SecurityReference wikiReference) {
        this.user = user;
        this.wikiReference = wikiReference;
    }

    @Override
    public SecurityReference getWikiReference()
    {
        return wikiReference;
    }

    @Override
    public SecurityReference getReference()
    {
        return user;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SecurityShadowEntry)) {
            return false;
        }
        SecurityShadowEntry other = (SecurityShadowEntry) object;

        return this.getReference().equals(other.getReference())
            && this.getWikiReference().equals(other.getWikiReference());
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(this.getWikiReference())
            .append(this.getReference())
            .toHashCode();
    }
}

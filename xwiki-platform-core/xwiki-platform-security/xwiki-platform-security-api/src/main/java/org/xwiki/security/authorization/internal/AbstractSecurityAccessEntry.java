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
package org.xwiki.security.authorization.internal;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.security.authorization.SecurityAccessEntry;

/**
 * This Abstract base class should be used for all SecurityAccessEntry implementing class to ensure
 * proper implementation of equals and hashCode.
 *
 * @version $Id$
 * @since 4.0M2
 */
public abstract class AbstractSecurityAccessEntry implements SecurityAccessEntry
{
    @Override
    public boolean equals(Object object)
    {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SecurityAccessEntry)) {
            return false;
        }
        SecurityAccessEntry other = (SecurityAccessEntry) object;

        return this.getUserReference().equals(other.getUserReference())
            && this.getReference().equals(other.getReference())
            && this.getAccess().equals(other.getAccess());
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(this.getUserReference())
            .append(this.getReference())
            .append(this.getAccess())
            .toHashCode();
    }
}

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
import org.apache.commons.collections.CollectionUtils;
import org.xwiki.security.authorization.SecurityRuleEntry;

/**
 * This Abstract base class should be used for all SecurityRuleEntry implementing class to ensure
 * proper implementation of equals and hashCode.
 *
 * @version $Id$
 * @since 4.0M2 
 */
public abstract class AbstractSecurityRuleEntry implements SecurityRuleEntry
{
    @Override
    public boolean equals(Object object)
    {
        if (object == this) {
            return true;
        }
        if (!(object instanceof SecurityRuleEntry)) {
            return false;
        }
        SecurityRuleEntry other = (SecurityRuleEntry) object;

        return this.getReference().equals(other.getReference())
            && CollectionUtils.isEqualCollection(this.getRules(), other.getRules());
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(this.getReference())
            .append(this.getRules())
            .toHashCode();
    }

    @Override
    public boolean isEmpty()
    {
        return this.getRules().isEmpty();
    }
}

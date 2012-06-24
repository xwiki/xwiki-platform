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
package org.xwiki.security.authorization;

import java.util.Collection;

/**
 * A security entry is the association of an entity and a set of security rules that constitute a elementary
 * declarative information for the security settler to compute the security access of users.
 *
 * @see org.xwiki.security.authorization.internal.AbstractSecurityRuleEntry
 * @version $Id$
 * @since 4.0M2
 */
public interface SecurityRuleEntry extends SecurityEntry
{
    /**
     * @return the collection of security rules defined by this rule entry.
     */
    Collection<SecurityRule> getRules();

    /**
     * @return {@code true} if there is no rule in this entry.
     */
    boolean isEmpty();
}

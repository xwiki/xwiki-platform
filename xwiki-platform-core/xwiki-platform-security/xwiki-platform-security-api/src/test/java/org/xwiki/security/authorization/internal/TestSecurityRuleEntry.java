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

import java.util.Collection;
import java.util.Collections;

import org.xwiki.security.SecurityReference;
import org.xwiki.security.authorization.SecurityRule;

/**
 * This stub implementation of SecurityRuleEntry is only used for testing.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class TestSecurityRuleEntry extends AbstractSecurityRuleEntry
{
    private final SecurityReference reference;

    /** The list of objects. */
    private final Collection<SecurityRule> rules;

    public TestSecurityRuleEntry(SecurityReference reference, Collection<SecurityRule> rules)
    {
        this.reference = reference;
        this.rules = Collections.unmodifiableCollection(rules);
    }

    /**
     * @return all rules available for this entity
     */
    @Override
    public SecurityReference getReference()
    {
        return reference;
    }

    /**
     * @return all rules available for this entity
     */
    @Override
    public Collection<SecurityRule> getRules()
    {
        return rules;
    }
}

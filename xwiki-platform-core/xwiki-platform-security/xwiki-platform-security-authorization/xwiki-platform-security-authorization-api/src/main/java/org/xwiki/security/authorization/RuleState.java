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

/**
 * The state of a particular right as determined by a security rule.
 *
 * @version $Id$
 * @since 4.0M2
 */
public enum RuleState
{
    /** Right state undetermined. */
    UNDETERMINED(0x0),
    /** Right is denied. */
    DENY(0x2),
    /** Right is allowed. */
    ALLOW(0x3);

    /** Value of this rule state. */
    private final int value;

    /** @param value state of a rule. */
    RuleState(int value)
    {
        this.value = value;
    }

    /** @return state of a rule. */
    public int getValue() 
    {
        return value;
    }
}

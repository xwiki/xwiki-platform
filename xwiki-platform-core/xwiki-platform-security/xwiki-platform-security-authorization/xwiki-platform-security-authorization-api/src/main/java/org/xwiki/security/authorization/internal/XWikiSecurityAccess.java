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
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;

/**
 * Default implementation for an {@link org.xwiki.security.authorization.SecurityAccess}.
 *
 * @version $Id$
 * @since 4.0M2
 */
public class XWikiSecurityAccess implements SecurityAccess
{
    /** The default access. */
    private static XWikiSecurityAccess defaultAccess;

    /** The default access size. Check to update defaultAccess if a new Right is added. */
    private static int defaultAccessSize;

    /** Allowed rights. */
    protected RightSet allowed = new RightSet();

    /** Denied rights. */
    protected RightSet denied = new RightSet();

    /**
     * @return the default access, using the default value of all rights.
     */
    public static synchronized XWikiSecurityAccess getDefaultAccess()
    {
        if (defaultAccess == null || Right.size() != defaultAccessSize) {
            defaultAccessSize = Right.size();
            defaultAccess = new XWikiSecurityAccess();
            for (Right right : Right.values()) {
                defaultAccess.set(right, right.getDefaultState());
            }
        }
        return defaultAccess;
    }

    /**
     * Set the state of a right in this access.
     * @param right the right to set.
     * @param state the state to set the right to.
     */
    void set(Right right, RuleState state)
    {
        switch (state) {
            case ALLOW:
                allow(right);
                break;
            case DENY:
                deny(right);
                break;
            case UNDETERMINED:
                clear(right);
                break;
            default:
                break;
        }
    }

    /**
     * Allow this right.
     * @param right the right to allow.
     */
    void allow(Right right)
    {
        allowed.add(right);
        denied.remove(right);
    }

    /**
     * Deny this right.
     * @param right the right to deny.
     */
    void deny(Right right)
    {
        denied.add(right);
        allowed.remove(right);
    }

    /**
     * Clear this right, it will return to the undetermined state.
     * @param right the right to clear.
     */
    void clear(Right right)
    {
        allowed.remove(right);
        denied.remove(right);
    }

    @Override
    public RuleState get(Right right)
    {
        if (denied.contains(right)) {
            return RuleState.DENY;
        }
        if (allowed.contains(right)) {
            return RuleState.ALLOW;
        }
        return RuleState.UNDETERMINED;
    }

    @Override
    public XWikiSecurityAccess clone() throws CloneNotSupportedException
    {
        XWikiSecurityAccess clone = (XWikiSecurityAccess) super.clone();
        clone.allowed = allowed.clone();
        clone.denied = denied.clone();
        return clone;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == this) {
            return true;
        }
        if (!(object instanceof XWikiSecurityAccess)) {
            return false;
        }
        XWikiSecurityAccess other = (XWikiSecurityAccess) object;

        return other.allowed.equals(allowed) && other.denied.equals(denied);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(allowed)
            .append(denied)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        boolean first = true;
        for (Right r : Right.values()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(r).append(": ").append(get(r));
        }
        return b.toString();
    }
}

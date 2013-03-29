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

import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;

/**
 * A fake security rules allowing edit to no one, to in fact deny edit to anyone.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class AllowEditToNoOneRule implements SecurityRule
{
    @Override
    public boolean match(Right right)
    {
        return right == Right.EDIT;
    }

    @Override
    public boolean match(GroupSecurityReference group)
    {
        return false;
    }

    @Override
    public boolean match(UserSecurityReference user)
    {
        return false;
    }

    @Override
    public RuleState getState()
    {
        return RuleState.ALLOW;
    }
}


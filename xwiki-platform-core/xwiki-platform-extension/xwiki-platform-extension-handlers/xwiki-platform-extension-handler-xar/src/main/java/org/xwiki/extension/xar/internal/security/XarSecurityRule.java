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
package org.xwiki.extension.xar.internal.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;

/**
 * Xar document oriented implementation of {@link XarSecurityRule}.
 * 
 * @version $Id$
 * @since 10.5RC1
 */
public class XarSecurityRule implements SecurityRule
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(XarSecurityRule.class);

    private final Right right;

    private final boolean simple;

    private XarSecurityTool securityTool;

    /**
     * @param right the affected right
     * @param simple true if the rule only apply to simple users
     * @param securityTool is used to get information about the user
     */
    public XarSecurityRule(Right right, boolean simple, XarSecurityTool securityTool)
    {
        this.right = right;
        this.simple = simple;
        this.securityTool = securityTool;
    }

    @Override
    public boolean match(Right right)
    {
        return right == this.right;
    }

    @Override
    public boolean match(GroupSecurityReference group)
    {
        return false;
    }

    @Override
    public boolean match(UserSecurityReference user)
    {
        return !this.simple || this.securityTool.isSimpleUser(user.getOriginalDocumentReference());
    }

    @Override
    public RuleState getState()
    {
        return RuleState.DENY;
    }
}

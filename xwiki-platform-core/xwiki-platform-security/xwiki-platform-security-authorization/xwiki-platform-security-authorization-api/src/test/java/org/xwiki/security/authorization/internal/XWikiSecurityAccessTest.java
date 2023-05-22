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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.security.authorization.Right.EDIT;
import static org.xwiki.security.authorization.Right.SCRIPT;
import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.DENY;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Test of {@link XWikiSecurityAccess}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@ComponentTest
class XWikiSecurityAccessTest
{
    private final XWikiSecurityAccess securityAccess = new XWikiSecurityAccess();

    @Test
    void getUndetermined()
    {
        this.securityAccess.configureRequiredRights(Set.of(), true);
        RuleState ruleState = this.securityAccess.get(EDIT, false);
        assertEquals(UNDETERMINED, ruleState);
    }

    @Test
    void getDenied()
    {
        this.securityAccess.configureRequiredRights(Set.of(), true);
        this.securityAccess.deny(EDIT);
        RuleState ruleState = this.securityAccess.get(EDIT, false);
        assertEquals(DENY, ruleState);
    }

    @Test
    void getDeniedRequiredRight()
    {
        this.securityAccess.configureRequiredRights(Set.of(SCRIPT), true);
        this.securityAccess.allow(EDIT);
        RuleState ruleState = this.securityAccess.get(EDIT, false);
        assertEquals(DENY, ruleState);
    }

    @Test
    void getAllowed()
    {
        this.securityAccess.configureRequiredRights(Set.of(), true);
        this.securityAccess.allow(EDIT);
        RuleState ruleState = this.securityAccess.get(EDIT, false);
        assertEquals(ALLOW, ruleState);
    }

    @Test
    void getAllowedRequiredRight()
    {
        this.securityAccess.configureRequiredRights(Set.of(SCRIPT), true);
        this.securityAccess.allow(EDIT);
        this.securityAccess.allow(SCRIPT);
        RuleState ruleState = this.securityAccess.get(EDIT, false);
        assertEquals(ALLOW, ruleState);
    }

    @Test
    void getAllowedRequiredRightSkipped()
    {
        this.securityAccess.configureRequiredRights(Set.of(SCRIPT), true);
        this.securityAccess.allow(EDIT);
        RuleState ruleState = this.securityAccess.get(EDIT, true);
        assertEquals(ALLOW, ruleState);
    }
}

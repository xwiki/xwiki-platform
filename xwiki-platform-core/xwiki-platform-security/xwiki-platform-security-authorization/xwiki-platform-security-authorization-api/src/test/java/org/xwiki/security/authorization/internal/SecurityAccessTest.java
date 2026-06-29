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

import org.junit.jupiter.api.Test;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Tests for asserting the correctness of the {@link SecurityAccess} data structure.
 *
 * @version $Id$
 * @since 4.0M2 
 */
class SecurityAccessTest
{

    /**
     * Assert that the default security access instance defines an access level for each defined right, and that the
     * access level is the same as the defined default for the right.
     */
    private void assertDefaultAccessLevel()
    {
        SecurityAccess access = XWikiSecurityAccess.getDefaultAccess();
        for (Right right : Right.values()) {
            if (access.get(right) != UNDETERMINED) {
                assertEquals(right.getDefaultState(), access.get(right),
                    "Right(" + right.getName() + ")");
            }
        }
    }

    /**
     * Assert that access levels can be cleared and set on a SecurityAccess instance.
     */
    @Test
    void accessLevel() throws Exception
    {
        assertDefaultAccessLevel();

        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();

        assertEquals(RuleState.ALLOW, l.get(Right.VIEW));
        assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        assertEquals(RuleState.ALLOW, l.get(Right.COMMENT));
        assertEquals(RuleState.ALLOW, l.get(Right.LOGIN));
        assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        assertEquals(RuleState.DENY, l.get(Right.DELETE));
        assertEquals(RuleState.DENY, l.get(Right.ADMIN));
        assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.clear(Right.VIEW);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.ADMIN);

        assertEquals(RuleState.UNDETERMINED, l.get(Right.VIEW));
        assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.LOGIN));
        assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        assertEquals(RuleState.DENY, l.get(Right.DELETE));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.ADMIN));
        assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.deny(Right.VIEW);
        l.deny(Right.LOGIN);
        l.allow(Right.ADMIN);
            
        assertEquals(RuleState.DENY, l.get(Right.VIEW));
        assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        assertEquals(RuleState.DENY, l.get(Right.LOGIN));
        assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        assertEquals(RuleState.DENY, l.get(Right.DELETE));
        assertEquals(RuleState.ALLOW, l.get(Right.ADMIN));
        assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.clear(Right.VIEW);
        l.clear(Right.EDIT);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.REGISTER);
        l.clear(Right.DELETE);
        l.clear(Right.ADMIN);
        l.clear(Right.PROGRAM);
        l.clear(Right.ILLEGAL);

        assertEquals(RuleState.UNDETERMINED, l.get(Right.VIEW));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.EDIT));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.LOGIN));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.REGISTER));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.DELETE));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.ADMIN));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.PROGRAM));
        assertEquals(RuleState.UNDETERMINED, l.get(Right.ILLEGAL));

        l.allow(Right.VIEW);
        l.allow(Right.EDIT);
        l.allow(Right.COMMENT);
        l.allow(Right.LOGIN);
        l.allow(Right.REGISTER);
        l.allow(Right.DELETE);
        l.allow(Right.ADMIN);
        l.allow(Right.PROGRAM);
        l.allow(Right.ILLEGAL);

        assertEquals(RuleState.ALLOW, l.get(Right.VIEW));
        assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        assertEquals(RuleState.ALLOW, l.get(Right.COMMENT));
        assertEquals(RuleState.ALLOW, l.get(Right.LOGIN));
        assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        assertEquals(RuleState.ALLOW, l.get(Right.DELETE));
        assertEquals(RuleState.ALLOW, l.get(Right.ADMIN));
        assertEquals(RuleState.ALLOW, l.get(Right.PROGRAM));
        assertEquals(RuleState.ALLOW, l.get(Right.ILLEGAL));

        assertDefaultAccessLevel();
    }

    /**
     * Assert that the clone method works.
     */
    @Test
    void cloneAccess() throws Exception
    {
        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();
        XWikiSecurityAccess k = l.clone();
        assertEquals(l, k);
        assertNotSame(k, l);
        assertDefaultAccessLevel();
    }
}

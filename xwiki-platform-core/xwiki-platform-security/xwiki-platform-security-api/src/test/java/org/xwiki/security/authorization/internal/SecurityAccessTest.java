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

import org.junit.Test;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Tests for asserting the correctness of the {@link SecurityAccess} data structure.
 *
 * @version $Id$
 * @since 4.0M2 
 */
public class SecurityAccessTest
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
                assertThat("Right(" + right.getName() + ")",
                    access.get(right), equalTo(right.getDefaultState()));
            }
        }
    }

    /**
     * Assert that access levels can be cleared and set on a SecurityAccess instance.
     */
    @Test
    public void testAccessLevel() throws Exception
    {
        assertDefaultAccessLevel();

        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();

        assertThat(RuleState.ALLOW, equalTo(l.get(Right.VIEW)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.EDIT)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.COMMENT)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.LOGIN)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.REGISTER)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.DELETE)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.ADMIN)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.PROGRAM)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.ILLEGAL)));

        l.clear(Right.VIEW);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.ADMIN);

        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.VIEW)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.EDIT)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.COMMENT)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.LOGIN)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.REGISTER)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.DELETE)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.ADMIN)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.PROGRAM)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.ILLEGAL)));

        l.deny(Right.VIEW);
        l.deny(Right.LOGIN);
        l.allow(Right.ADMIN);
            
        assertThat(RuleState.DENY, equalTo(l.get(Right.VIEW)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.EDIT)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.COMMENT)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.LOGIN)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.REGISTER)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.DELETE)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.ADMIN)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.PROGRAM)));
        assertThat(RuleState.DENY, equalTo(l.get(Right.ILLEGAL)));

        l.clear(Right.VIEW);
        l.clear(Right.EDIT);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.REGISTER);
        l.clear(Right.DELETE);
        l.clear(Right.ADMIN);
        l.clear(Right.PROGRAM);
        l.clear(Right.ILLEGAL);

        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.VIEW)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.EDIT)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.COMMENT)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.LOGIN)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.REGISTER)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.DELETE)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.ADMIN)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.PROGRAM)));
        assertThat(RuleState.UNDETERMINED, equalTo(l.get(Right.ILLEGAL)));

        l.allow(Right.VIEW);
        l.allow(Right.EDIT);
        l.allow(Right.COMMENT);
        l.allow(Right.LOGIN);
        l.allow(Right.REGISTER);
        l.allow(Right.DELETE);
        l.allow(Right.ADMIN);
        l.allow(Right.PROGRAM);
        l.allow(Right.ILLEGAL);

        assertThat(RuleState.ALLOW, equalTo(l.get(Right.VIEW)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.EDIT)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.COMMENT)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.LOGIN)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.REGISTER)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.DELETE)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.ADMIN)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.PROGRAM)));
        assertThat(RuleState.ALLOW, equalTo(l.get(Right.ILLEGAL)));

        assertDefaultAccessLevel();
    }

    /**
     * Assert that the clone method works.
     */
    @Test
    public void testClone() throws Exception
    {
        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();
        XWikiSecurityAccess k = l.clone();
        assertThat(l, equalTo(k));
        assertThat(l, not(sameInstance(k)));
        assertDefaultAccessLevel();
    }
}

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

import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;

import junit.framework.Assert;
import junit.framework.TestCase;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * Tests for assering the correctness of the {@link SecurityAccess} data structure.
 *
 * @version $Id$
 * @since 4.0M2 
 */
public class SecurityAccessTest extends TestCase
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
    public void testAccessLevel() throws Exception
    {
        assertDefaultAccessLevel();

        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();

        Assert.assertEquals(RuleState.ALLOW, l.get(Right.VIEW));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.COMMENT));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.LOGIN));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        Assert.assertEquals(RuleState.DENY, l.get(Right.DELETE));
        Assert.assertEquals(RuleState.DENY, l.get(Right.ADMIN));
        Assert.assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        Assert.assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.clear(Right.VIEW);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.ADMIN);

        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.VIEW));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.LOGIN));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        Assert.assertEquals(RuleState.DENY, l.get(Right.DELETE));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.ADMIN));
        Assert.assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        Assert.assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.deny(Right.VIEW);
        l.deny(Right.LOGIN);
        l.allow(Right.ADMIN);
            
        Assert.assertEquals(RuleState.DENY, l.get(Right.VIEW));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        Assert.assertEquals(RuleState.DENY, l.get(Right.LOGIN));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        Assert.assertEquals(RuleState.DENY, l.get(Right.DELETE));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.ADMIN));
        Assert.assertEquals(RuleState.DENY, l.get(Right.PROGRAM));
        Assert.assertEquals(RuleState.DENY, l.get(Right.ILLEGAL));

        l.clear(Right.VIEW);
        l.clear(Right.EDIT);
        l.clear(Right.COMMENT);
        l.clear(Right.LOGIN);
        l.clear(Right.REGISTER);
        l.clear(Right.DELETE);
        l.clear(Right.ADMIN);
        l.clear(Right.PROGRAM);
        l.clear(Right.ILLEGAL);

        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.VIEW));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.EDIT));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.COMMENT));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.LOGIN));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.REGISTER));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.DELETE));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.ADMIN));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.PROGRAM));
        Assert.assertEquals(RuleState.UNDETERMINED, l.get(Right.ILLEGAL));

        l.allow(Right.VIEW);
        l.allow(Right.EDIT);
        l.allow(Right.COMMENT);
        l.allow(Right.LOGIN);
        l.allow(Right.REGISTER);
        l.allow(Right.DELETE);
        l.allow(Right.ADMIN);
        l.allow(Right.PROGRAM);
        l.allow(Right.ILLEGAL);

        Assert.assertEquals(RuleState.ALLOW, l.get(Right.VIEW));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.EDIT));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.COMMENT));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.LOGIN));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.REGISTER));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.DELETE));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.ADMIN));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.PROGRAM));
        Assert.assertEquals(RuleState.ALLOW, l.get(Right.ILLEGAL));

        assertDefaultAccessLevel();
    }

    /**
     * Assert that the clone method works.
     */
    public void testClone() throws Exception
    {
        XWikiSecurityAccess l = XWikiSecurityAccess.getDefaultAccess().clone();
        XWikiSecurityAccess k = l.clone();
        Assert.assertEquals(l, k);
        Assert.assertNotSame(l, k);
        assertDefaultAccessLevel();
    }
}

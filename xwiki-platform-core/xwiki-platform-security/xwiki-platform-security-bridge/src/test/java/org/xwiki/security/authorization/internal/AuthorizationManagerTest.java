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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AbstractWikiTestCase;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;

import com.xpn.xwiki.XWikiContext;

public class AuthorizationManagerTest extends AbstractWikiTestCase
{
    private AuthorizationManager authorizationManager;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.authorizationManager = getComponentManager().getInstance(AuthorizationManager.class);
    }

    protected void assertAccessTrue(String message, Right right, DocumentReference userReference,
        EntityReference entityReference, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        Assert.assertTrue(message, this.authorizationManager.hasAccess(right, userReference, entityReference));
    }

    protected void assertAccessFalse(String message, Right right, DocumentReference userReference,
        EntityReference entityReference, XWikiContext ctx) throws Exception
    {
        setContext(ctx);

        Assert.assertFalse(message, this.authorizationManager.hasAccess(right, userReference, entityReference));
    }

    // Tests

    @Test
    public void testGlobalUserInEmptySubWiki() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "emptySubWiki.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki2");

        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.VIEW,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.EDIT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.COMMENT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.REGISTER,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.ADMIN,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
    }

    @Test
    public void testPublicAccess() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "emptySubWiki.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki");

        DocumentReference user = null;
        EntityReference document = new DocumentReference("wiki2", "Space", "Page");

        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.LOGIN, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.VIEW, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.EDIT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.REGISTER, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.COMMENT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.ADMIN, user,
            document, ctx);
    }

    @Test
    public void testPublicAccessOnTopLevel() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "empty.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki");

        DocumentReference user = null;
        EntityReference document = null;

        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.LOGIN, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.VIEW, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.EDIT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.REGISTER, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.COMMENT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.ADMIN, user,
            document, ctx);
    }

    @Test
    public void testRightOnTopLevel() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "emptySubWiki.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki2");

        DocumentReference user = new DocumentReference("wiki", "XWiki", "user");
        EntityReference document = null;

        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.LOGIN, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.VIEW, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.EDIT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.REGISTER, user,
            document, ctx);
        assertAccessTrue("User from global wiki should have the same rights on empty subwiki", Right.COMMENT, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM, user,
            document, ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.ADMIN, user,
            document, ctx);
    }

    // Cache tests

    @Test
    public void testRightOnUserAndDelete() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "usersAndGroups.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki");

        assertAccessTrue("User should have view right", Right.VIEW, new DocumentReference("wiki", "XWiki", "user"),
            new DocumentReference("wiki", "Space", "Page"), ctx);
        assertAccessTrue("User should have view right", Right.VIEW, new DocumentReference("wiki", "XWiki", "user2"),
            new DocumentReference("wiki", "Space", "Page"), ctx);

        testWiki.deleteUser("user", "wiki");

        assertAccessFalse("User should have view right", Right.VIEW, new DocumentReference("wiki", "XWiki", "user"),
            new DocumentReference("wiki", "Space", "Page"), ctx);
        assertAccessTrue("User should have view right", Right.VIEW, new DocumentReference("wiki", "XWiki", "user2"),
            new DocumentReference("wiki", "Space", "Page"), ctx);
    }
}

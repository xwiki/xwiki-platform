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
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
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
        ctx.setWikiId("wiki2");

        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.VIEW,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.EDIT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.COMMENT,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.DELETE,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.REGISTER,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.ADMIN,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
        assertAccessFalse("User from global wiki should have the same rights on empty subwiki", Right.PROGRAM,
            new DocumentReference("wiki", "XWiki", "user"), new DocumentReference("wiki2", "Space", "Page"), ctx);
    }

    @Test
    public void testPublicAccess() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "empty.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        DocumentReference user = null;
        EntityReference document = new DocumentReference("wiki", "Space", "Page");

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
        ctx.setWikiId("wiki");

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
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "empty.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

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
        ctx.setWikiId("wiki");

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

    @Test
    public void testEditAccessToGlobalRightObjectOnEmptyWiki() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "empty.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        DocumentReference user = new DocumentReference("wiki", "XWiki", "user");

        assertAccessFalse("Non-admin should not have edit access to XWikiPreferences in an empty wiki",
            Right.EDIT, user, new DocumentReference("wiki", "XWiki", "XWikiPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to XWiki.WebPreferences in an empty wiki",
            Right.EDIT, user, new DocumentReference("wiki", "XWiki", "WebPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to WebPreferences in any space of an empty wiki",
            Right.EDIT, user, new DocumentReference("wiki", "space", "WebPreferences"), ctx);
    }

    @Test
    public void testEditAccessToGlobalRightObject() throws Exception
    {
        LegacyTestWiki testWiki =
            new LegacyTestWiki(getMockery(), getComponentManager(), "accessToGlobalObjects.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        DocumentReference userA = new DocumentReference("wiki", "XWiki", "userA");
        DocumentReference userB = new DocumentReference("wiki", "XWiki", "userB");
        DocumentReference userA2 = new DocumentReference("wiki2", "XWiki", "userA");
        DocumentReference userB2 = new DocumentReference("wiki2", "XWiki", "userB");

        assertAccessTrue("Admin should have edit access to XWikiPreferences when allowed by the wiki",
            Right.EDIT, userA, new DocumentReference("wiki", "XWiki", "XWikiPreferences"), ctx);
        assertAccessTrue("Admin should have edit access to XWikiPreferences when allowed by the XWiki space",
            Right.EDIT, userA2, new DocumentReference("wiki2", "XWiki", "XWikiPreferences"), ctx);
        assertAccessTrue("Global Admin should have edit access to XWikiPreferences",
            Right.EDIT, userA, new DocumentReference("wiki2", "XWiki", "XWikiPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to XWikiPreferences even when allowed by the document",
            Right.EDIT, userB, new DocumentReference("wiki", "XWiki", "XWikiPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to XWikiPreferences even when allowed by the space",
            Right.EDIT, userB, new DocumentReference("wiki2", "XWiki", "XWikiPreferences"), ctx);

        assertAccessTrue("Admin should have edit access to XWikiPreferences when allowed by the wiki",
            Right.EDIT, userA, new DocumentReference("wiki", "XWiki", "WebPreferences"), ctx);
        assertAccessTrue("Admin should have edit access to XWikiPreferences when allowed by the XWiki space",
            Right.EDIT, userA2, new DocumentReference("wiki2", "XWiki", "WebPreferences"), ctx);
        assertAccessTrue("Global Admin should have edit access to XWikiPreferences",
            Right.EDIT, userA, new DocumentReference("wiki2", "XWiki", "WebPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to XWikiPreferences even when allowed by the document",
            Right.EDIT, userB, new DocumentReference("wiki", "XWiki", "WebPreferences"), ctx);
        assertAccessFalse("Non-admin should not have edit access to XWikiPreferences even when allowed by the space",
            Right.EDIT, userB, new DocumentReference("wiki2", "XWiki", "WebPreferences"), ctx);
    }

    @Test
    public void testMainWikiOwner() throws Exception
    {
        LegacyTestWiki testWiki = new LegacyTestWiki(getMockery(), getComponentManager(), "empty.xml", false);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        assertAccessTrue("Main wiki oner shoudl have Programming Right", Right.PROGRAM,
            new DocumentReference("wiki", "XWiki", "Admin"), null, ctx);
    }

    @Test
    public void testGroupAccessThenUserAccess() throws Exception
    {
        LegacyTestWiki testWiki =
            new LegacyTestWiki(getMockery(), getComponentManager(), "userAndGroupAdmin.xml", false);

        DocumentReference user = new DocumentReference("wiki", "XWiki", "user");
        DocumentReference group = new DocumentReference("wiki", "XWiki", "group");
        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        WikiReference wiki = ctx.getWikiReference();
        SpaceReference space = new SpaceReference("space", ctx.getWikiReference());

        assertAccessTrue("Group should have admin right", Right.ADMIN, group, wiki, ctx);
        assertAccessTrue("Group should have admin right", Right.ADMIN, group, space, ctx);
        assertAccessTrue("User should have admin right", Right.ADMIN, user, wiki, ctx);
        assertAccessTrue("User should have admin right", Right.ADMIN, user, space, ctx);
    }

    @Test
    public void testUserAccessThenGroupAccess() throws Exception
    {
        LegacyTestWiki testWiki =
            new LegacyTestWiki(getMockery(), getComponentManager(), "userAndGroupAdmin.xml", false);

        DocumentReference user = new DocumentReference("wiki", "XWiki", "user");
        DocumentReference group = new DocumentReference("wiki", "XWiki", "group");
        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        WikiReference wiki = ctx.getWikiReference();
        SpaceReference space = new SpaceReference("space", ctx.getWikiReference());

        assertAccessTrue("User should have admin right", Right.ADMIN, user, wiki, ctx);
        assertAccessTrue("User should have admin right", Right.ADMIN, user, space, ctx);
        assertAccessTrue("Group should have admin right", Right.ADMIN, group, wiki, ctx);
        assertAccessTrue("Group should have admin right", Right.ADMIN, group, space, ctx);
    }
}

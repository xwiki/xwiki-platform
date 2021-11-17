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
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AbstractLegacyWikiTestCase;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import org.xwiki.security.internal.XWikiConstants;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl}.
 * 
 * @version $Id$
 */
public class XWikiRightServiceTest extends AbstractLegacyWikiTestCase
{
    @Test
    public void testUserFromAnotherWiki1() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki1.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");

        assertAccessLevelTrue("User from another wiki has right on a local wiki", "view", "wiki:XWiki.user",
            "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki has right on a local wiki", "view", "XWiki.user",
            "wiki2:Space.Page", ctx);
    }

    @Test
    public void testUserFromAnotherWiki2() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki2.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
            "view", "XWiki.user", "wiki2:Space.Page", ctx);

        ctx.setWikiId("wiki2");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            "view", "wiki:XWiki.user", "Space.Page", ctx);

    }

    @Test
    public void testGroupFromAnotherWiki1() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("groupFromAnotherWiki1.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        assertAccessLevelTrue(
            "User group from another wiki does not have right on a local wiki when tested from user wiki", "view",
            "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue(
            "User group from another wiki does not have right on a local wiki when tested from user wiki", "view",
            "XWiki.user", "wiki2:Space.Page", ctx);

        ctx.setWikiId("wiki2");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
            "view", "wiki:XWiki.user", "Space.Page", ctx);

    }

    @Test
    public void testWikiOwnerFromAnotherWiki() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki2.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        assertAccessLevelTrue(
            "Wiki owner from another wiki does not have right on a local wiki when tested from user wiki", "view",
            "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue(
            "Wiki owner from another wiki does not have right on a local wiki when tested from user wiki", "view",
            "XWiki.user", "wiki2:Space.Page", ctx);

        ctx.setWikiId("wiki2");

        assertAccessLevelTrue(
            "Wiki owner from another wiki does not have right on a local wiki when tested from local wiki", "view",
            "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue(
            "Wiki owner from another wiki does not have right on a local wiki when tested from local wiki", "view",
            "wiki:XWiki.user", "Space.Page", ctx);

    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     * 
     * @throws com.xpn.xwiki.XWikiException on error
     */
    @Test
    public void testProgrammingRightsWhenNoContextDocumentIsSet() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("programmingRights.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        setContext(ctx);

        testWiki.setSdoc((String) null);

        // XWiki.Programmer should have PR, as per the global rights.
        testWiki.setUser("XWiki.programmer");
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

        // Guests should not have PR
        testWiki.setUser(XWikiConstants.GUEST_USER_FULLNAME);
        Assert.assertFalse(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertFalse(getCachingImpl().hasProgrammingRights(ctx));

        // superadmin should always have PR
        testWiki.setUser(XWikiConstants.XWIKI_SPACE + '.' + AuthorizationManager.SUPERADMIN_USER);
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

    }

    @Test
    public void testProgrammingRightsWhenCustomSecureDocIsSet() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("programmingRights.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        setContext(ctx);

        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        sdoc.setContentAuthorReference(new DocumentReference(ctx.getMainXWiki(), "XWiki", "superadmin"));
        testWiki.setSdoc(sdoc);

        // XWiki.Programmer should have PR, as per the global rights.
        sdoc.setContentAuthorReference(new DocumentReference(ctx.getMainXWiki(), "XWiki", "programmer"));
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

        // Guests should not have PR
        sdoc.setContentAuthorReference(null);
        Assert.assertFalse(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertFalse(getCachingImpl().hasProgrammingRights(ctx));

        // superadmin should always have PR
        sdoc.setContentAuthorReference(new DocumentReference(ctx.getMainXWiki(), "XWiki", "superadmin"));
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

    }

    @Test
    public void testGuestRightsOnEmptyWiki() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("empty.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki");

        assertAccessLevelTrue("Guest does not have view right on empty wiki.", "view", "wiki:XWiki.XWikiGuest",
            "wiki:Space.Page", ctx);

        assertAccessLevelTrue("Guest does not have edit right on empty wiki.", "edit", "wiki:XWiki.XWikiGuest",
            "wiki:Space.Page", ctx);

        assertAccessLevelFalseExpectedDifference("Guest should not have delete right on empty wiki.", "delete",
            "wiki:XWiki.XWikiGuest", "wiki:Space.Page", ctx);

        assertAccessLevelFalseExpectedDifference("Guest should not have admin right on empty wiki.", "admin",
            "wiki:XWiki.XWikiGuest", "wiki:Space.Page", ctx);

        assertAccessLevelFalse("Guest should not have programming right on empty wiki.", "programming",
            "wiki:XWiki.XWikiGuest", "wiki:Space.Page", ctx);

        testWiki.setUser("wiki:XWiki.XWikiGuest");

        assertWikiAdminRightsFalseExpectedDifference("Guest should not have admin right on empty wiki.", ctx);

        testWiki.setDoc("wiki:Space.Page");

        assertAdminRightsFalseExpectedDifference("Guest should not have admin right on empty wiki.", ctx);
    }

    @Test
    public void testGlobalUserInLocalGroup() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("glocalUserInLocalGroup.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki2");

        assertAccessLevelTrue("User from global wiki should have right on local wiki through local group", "view",
            "wiki:XWiki.user", "wiki2:Space.Page", ctx);
    }

    @Test
    public void testGlobalGroupInLocalGroup() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("glocalGroupInLocalGroup.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setWikiId("wiki2");

        assertAccessLevelTrue("Users from global wiki should have right on local wiki through local group", "view",
            "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("Groups from global wiki should have right on local wiki through local group", "view",
            "wiki:XWiki.group", "wiki2:Space.Page", ctx);
    }

    @Test
    public void testRelativeDocumentReference() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("denieddocument.xml", true);

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setWikiId("wiki");
        testWiki.setDoc("wiki:Space.Page");

        // view

        assertAccessLevelFalse("User has right on the denied document", "view", "wiki:XWiki.user", "wiki:Space.Page",
            ctx);

        assertAccessLevelFalse("User has right on the denied document", "view", "wiki:XWiki.user", "Space.Page", ctx);

        assertAccessLevelFalse("User has right on the denied document", "view", "wiki:XWiki.user", "Page", ctx);

        assertAccessLevelTrue("User does not have right on the document space", "view", "wiki:XWiki.user", "", ctx);

        // edit

        assertAccessLevelFalse("User has right on the denied document", "edit", "XWiki.user", "wiki:Space.Page", ctx);

        assertAccessLevelFalse("User has right on the denied document", "edit", "XWiki.user", "Space.Page", ctx);

        assertAccessLevelFalse("User has right on the denied document", "edit", "XWiki.user", "Page", ctx);

        assertAccessLevelTrue("User does not have right on the document space", "edit", "XWiki.user", "", ctx);
    }
}

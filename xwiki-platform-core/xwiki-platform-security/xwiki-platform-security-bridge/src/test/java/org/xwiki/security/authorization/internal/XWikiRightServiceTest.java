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

import java.util.Collections;

import org.xwiki.security.authorization.AbstractLegacyWikiTestCase;
import org.xwiki.security.authorization.testwikibuilding.LegacyTestWiki;
import org.xwiki.security.authorization.AuthorizationManager;

import com.xpn.xwiki.XWikiContext;

import junit.framework.Assert;
import org.junit.Test;

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
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki1.xml");

        XWikiContext ctx = testWiki.getXWikiContext();
        ctx.setDatabase("wiki");

        // TODO: Here we have a mismatch.

        assertAccessLevelFalseExpectedDifference("User from another wiki has right on a local wiki",
                                                 "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

    }

    @Test
    public void testUserFromAnotherWiki2() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki2.xml");

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setDatabase("wiki");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from user wiki",
                              "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        ctx.setDatabase("wiki2");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
                              "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
                              "view", "wiki:XWiki.user", "Space.Page", ctx);

    }

    @Test
    public void testGroupFromAnotherWiki1() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("groupFromAnotherWiki1.xml");

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setDatabase("wiki");

        assertAccessLevelTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
                              "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User group from another wiki does not have right on a local wiki when tested from user wiki",
                              "view", "XWiki.user", "wiki2:Space.Page", ctx);


        ctx.setDatabase("wiki2");

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
                              "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("User from another wiki does not have right on a local wiki when tested from local wiki",
                              "view", "wiki:XWiki.user", "Space.Page", ctx);

    }

    @Test
    public void testWikiOwnerFromAnotherWiki() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("userFromAnotherWiki2.xml");

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setDatabase("wiki");

        assertAccessLevelTrue("Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
                              "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("Wiki owner from another wiki does not have right on a local wiki when tested from user wiki",
                          "view", "XWiki.user", "wiki2:Space.Page", ctx);

        ctx.setDatabase("wiki2");

        assertAccessLevelTrue("Wiki owner from another wiki does not have right on a local wiki when tested from local wiki",
                          "view", "wiki:XWiki.user", "wiki2:Space.Page", ctx);

        assertAccessLevelTrue("Wiki owner from another wiki does not have right on a local wiki when tested from local wiki",
                          "view", "wiki:XWiki.user", "Space.Page", ctx);

    }

    /**
     * Test that programming rights are checked on the context user when no context document is set.
     * @throws com.xpn.xwiki.XWikiException on error
     */
    @Test
    public void testProgrammingRightsWhenNoContextDocumentIsSet() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("programmingRights.xml");

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setDatabase("wiki");

        setContext(ctx);

        testWiki.setSdoc(null);

        // XWiki.Programmer should have PR, as per the global rights.
        testWiki.setUser("XWiki.programmer");
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

        // Guests should not have PR
        testWiki.setUser(XWikiConstants.GUEST_USER_FULLNAME);
        Assert.assertFalse(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertFalse(getCachingImpl().hasProgrammingRights(ctx));

        // superadmin should always have PR
        testWiki.setUser(XWikiConstants.WIKI_SPACE + '.' + AuthorizationManager.SUPERADMIN_USER);
        Assert.assertTrue(getLegacyImpl().hasProgrammingRights(ctx));
        Assert.assertTrue(getCachingImpl().hasProgrammingRights(ctx));

    }

    @Test
    public void testGuestRightsOnEmptyWiki() throws Exception
    {
        LegacyTestWiki testWiki = newTestWiki("empty.xml");

        XWikiContext ctx = testWiki.getXWikiContext();

        ctx.setDatabase("xwiki");

        assertAccessLevelTrue("Guest does not have view right on empty wiki.",
                              "view", "xwiki:XWiki.XWikiGuest", "xwiki:Space.Page", ctx);
        
        assertAccessLevelTrue("Guest does not have edit right on empty wiki.",
                              "edit", "xwiki:XWiki.XWikiGuest", "xwiki:Space.Page", ctx);
        
        assertAccessLevelTrue("Guest does not have delete right on empty wiki.",
                              "delete", "xwiki:XWiki.XWikiGuest", "xwiki:Space.Page", ctx);
        
        assertAccessLevelTrue("Guest does not have admin right on empty wiki.",
                              "admin", "xwiki:XWiki.XWikiGuest", "xwiki:Space.Page", ctx);
        
        assertAccessLevelTrueExpectedDifference("Guest does not have programming right on empty wiki.",
                              "programming", "xwiki:XWiki.XWikiGuest", "xwiki:Space.Page", ctx);
        
    }
}

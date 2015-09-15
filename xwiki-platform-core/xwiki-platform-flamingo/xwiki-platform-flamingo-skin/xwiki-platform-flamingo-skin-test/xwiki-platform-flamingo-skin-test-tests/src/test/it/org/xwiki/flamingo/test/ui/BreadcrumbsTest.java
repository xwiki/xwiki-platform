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
package org.xwiki.flamingo.test.ui;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;

/**
 * Test Breadcrumbs.
 * 
 * @version $Id$
 * @since 2.7RC1
 */
public class BreadcrumbsTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule adminAuthenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private static final String PARENT_TITLE = "Parent page";

    private static final String CHILD_TITLE = "Child page";

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void verifyBreadcrumbInParentChildMode()
    {
        // Delete the page to reset the rights on it (since the test below modifies them).
        getUtil().deletePage(getTestClassName(), getTestMethodName());
        
        getUtil().setHierarchyMode("parentchild");

        String parentPageName = getTestMethodName() + "ParentPage";
        String parentPageFullName = getTestClassName() + "." + parentPageName;

        getUtil().createPage(getTestClassName(), parentPageName, null, PARENT_TITLE);
        ViewPage vp = getUtil().createPage(getTestClassName(), getTestMethodName(), null, CHILD_TITLE, null,
            parentPageFullName);

        // Verify standard breadcrumb behavior.
        assertTrue(vp.hasBreadcrumbContent(PARENT_TITLE, false));
        assertTrue(vp.hasBreadcrumbContent(CHILD_TITLE, true));
        
        // Remove view rights on the Test.ParentPage page to everyone except superadmin user so that we can verify
        // that the breadcrumb of the child page doesn't display page titles for pages for which you don't have view
        // rights.
        getUtil().addObject(getTestClassName(), parentPageName, "XWiki.XWikiRights",
            "levels", "view",
            "users", "XWiki.superadmin",
            "allow", "1");
        
        // Log out...
        getUtil().forceGuestUser();

        // Verify breadcrumb doesn't display page title for the parent page (since the guest user doesn't have view
        // permission for it).
        vp = getUtil().gotoPage(getTestClassName(), getTestMethodName());
        assertFalse(vp.hasBreadcrumbContent(PARENT_TITLE, false, true));
        assertTrue(vp.hasBreadcrumbContent(CHILD_TITLE, true, true));
        assertTrue(vp.hasBreadcrumbContent(parentPageName, false, false));

        // Set back the default hierarchy mode (but first we need to log back).
        adminAuthenticationRule.authenticate();
        getUtil().setHierarchyMode("reference");
    }
}

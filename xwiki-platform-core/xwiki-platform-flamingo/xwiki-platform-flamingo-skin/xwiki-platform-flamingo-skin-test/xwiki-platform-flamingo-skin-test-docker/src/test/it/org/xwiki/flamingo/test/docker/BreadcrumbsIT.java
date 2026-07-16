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
package org.xwiki.flamingo.test.docker;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.BreadcrumbElement;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Breadcrumbs.
 *
 * @version $Id$
 * @since 2.7RC1
 */
@UITest
class BreadcrumbsIT
{
    private static final String PARENT_TITLE = "Parent page";

    private static final String CHILD_TITLE = "Child page";

    @BeforeEach
    void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void verifyBreadcrumbInParentChildMode(TestUtils setup, TestReference testReference)
    {
        String space = testReference.getLastSpaceReference().getParent().getName();
        String pageName = testReference.getLastSpaceReference().getName();
        String parentPageName = pageName + "ParentPage";
        String parentPageFullName = space + "." + parentPageName;

        // Delete the page to reset the rights on it (since the test below modifies them).
        setup.deletePage(space, pageName);

        setup.setHierarchyMode("parentchild");

        setup.createPage(space, parentPageName, null, PARENT_TITLE);
        ViewPage vp = setup.createPage(space, pageName, null, CHILD_TITLE, null, parentPageFullName);

        // Verify standard breadcrumb behavior.
        assertTrue(vp.hasBreadcrumbContent(PARENT_TITLE, false));
        assertTrue(vp.hasBreadcrumbContent(CHILD_TITLE, true));

        // Remove view rights on the parent page to everyone except superadmin user so that we can verify
        // that the breadcrumb of the child page doesn't display page titles for pages for which you don't have view
        // rights.
        setup.addObject(space, parentPageName, "XWiki.XWikiRights",
            "levels", "view",
            "users", "XWiki.superadmin",
            "allow", "1");

        // Log out...
        setup.forceGuestUser();

        // Verify breadcrumb doesn't display page title for the parent page (since the guest user doesn't have view
        // permission for it).
        vp = setup.gotoPage(space, pageName);
        assertFalse(vp.isAuthenticated());
        assertFalse(vp.hasBreadcrumbContent(PARENT_TITLE, false, true));
        assertTrue(vp.hasBreadcrumbContent(CHILD_TITLE, true, true));
        assertTrue(vp.hasBreadcrumbContent(parentPageName, false, false));

        // Set back the default hierarchy mode (but first we need to log back).
        setup.loginAsSuperAdmin();
        setup.setHierarchyMode("reference");
    }

    @Test
    @Order(2)
    void verifyBreadcrumbInLongHierarchy(TestUtils setup)
    {
        DocumentReference documentReference =
            new DocumentReference("xwiki", Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H"), "WebHome");
        ViewPage page = setup.createPage(documentReference, "Content", "verifyBreadcrumbInLongHierarchy");

        BreadcrumbElement breadcrumb = page.getBreadcrumb();
        // Verify that the breadcrumb is limited
        assertEquals("/A/…/F/G/verifyBreadcrumbInLongHierarchy", breadcrumb.getPathAsString());
        // Verify we can expand it
        breadcrumb.expand();
        assertEquals("/A/B/C/D/E/F/G/verifyBreadcrumbInLongHierarchy", breadcrumb.getPathAsString());
        // Clean
        setup.deletePage(documentReference);
    }
}

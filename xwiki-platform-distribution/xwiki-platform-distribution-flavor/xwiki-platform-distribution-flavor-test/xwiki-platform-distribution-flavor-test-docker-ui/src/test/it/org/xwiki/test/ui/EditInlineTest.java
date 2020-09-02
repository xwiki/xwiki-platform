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
package org.xwiki.test.ui;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test Inline editing.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class EditInlineTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    // Note: We're not testing basic inline editing since this is already covered by the User Profile tests

    @Test
    @Order(1)
    void testEditButtonTriggersInlineEditing(TestUtils testUtils)
    {
        // Go to the Admin user profile page and edit it since editing a user profile page is supposed to go in inline
        // editing by default
        ViewPage vp = testUtils.gotoPage("XWiki", "Admin");
        vp.edit();
        assertTrue(new ViewPage().isInlinePage());
    }

    /* See XE-168 and XWIKI-6992 */
    @Test
    @Order(2)
    void testInlineEditCanChangeTitle(TestUtils testUtils, TestReference testReference)
    {
        String title = RandomStringUtils.randomAlphanumeric(4);
        testUtils.gotoPage(testReference, "edit", "editor=inline&title=" + title);
        InlinePage inlinePage = new InlinePage();
        // Check if the title specified on the request is properly displayed.
        assertEquals(title, inlinePage.getDocumentTitle());
        // Check if the title specified on the request is displayed in the document hierarchy.
        assertTrue(inlinePage.getBreadcrumbContent().contains(title));
        // Save the document and check again the displayed title
        ViewPage viewPage = inlinePage.clickSaveAndView();
        assertEquals(title, viewPage.getDocumentTitle());
    }

    /* See XE-168 */
    @Test
    @Order(3)
    void testInlineEditCanChangeParent(TestUtils testUtils, TestReference testReference)
    {
        // Use the parentchild hierarchy mode to be able to assert the parent.
        testUtils.setHierarchyMode("parentchild");

        testUtils.gotoPage(testReference, "edit", "editor=inline&parent=Main.SomeParent");
        ViewPage vp = new InlinePage().clickSaveAndView();

        // Check the new parent in the breadcrumbs.
        assertTrue(vp.hasBreadcrumbContent("SomeParent", false));

        // Restore the hierarchy mode.
        testUtils.setHierarchyMode("reference");
    }

    /* See XWIKI-2389 */
    @Test
    @Order(4)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testInlineEditPreservesTitle(TestUtils testUtils, TestReference testReference)
    {
        String title = RandomStringUtils.randomAlphanumeric(4);
        testUtils.gotoPage(testReference, "save", "title=" + title);
        ViewPage vp = new ViewPage();
        assertEquals(title, vp.getDocumentTitle());
        InlinePage ip = vp.editInline();
        ViewPage vp2 = ip.clickSaveAndView();
        assertEquals(title, vp2.getDocumentTitle());
    }

    /* See XWIKI-2389 */
    @Test
    @Order(5)
    void testInlineEditPreservesParent(TestUtils testUtils, TestReference testReference)
    {
        // Use the parentchild hierarchy mode to be able to assert the parent.
        testUtils.setHierarchyMode("parentchild");

        testUtils.gotoPage(testReference, "save", "parent=Blog.WebHome");
        ViewPage vp = new ViewPage();
        assertTrue(vp.hasBreadcrumbContent("Blog", false));

        InlinePage ip = vp.editInline();
        ViewPage vp2 = ip.clickSaveAndView();
        assertTrue(vp2.hasBreadcrumbContent("Blog", false));

        // Now try the same in the default hierarchy mode, to make sure the default mode preserves the parent.
        testUtils.setHierarchyMode("reference");

        testUtils.gotoPage(testReference, "edit", "editor=inline");
        InlinePage ip2 = new InlinePage();
        ip2.clickSaveAndView();

        // Switch again to the parentchild hierarchy mode to be able to assert the parent.
        testUtils.setHierarchyMode("parentchild");

        ViewPage vp3 = testUtils.gotoPage(testReference);
        assertTrue(vp3.hasBreadcrumbContent("Blog", false));

        // Restore the hierarchy mode.
        testUtils.setHierarchyMode("reference");
    }

    /* See XWIKI-2199 */
    @Test
    @Order(6)
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    void testInlineEditPreservesTags(TestUtils testUtils, TestReference testReference)
    {
        String tag1 = RandomStringUtils.randomAlphanumeric(4);
        String tag2 = RandomStringUtils.randomAlphanumeric(4);
        testUtils.gotoPage(testReference, "save", "tags=" + tag1 + "%7C" + tag2);
        TaggablePage taggablePage = new TaggablePage();
        assertTrue(taggablePage.hasTag(tag1));
        assertTrue(taggablePage.hasTag(tag2));
        taggablePage.editInline().clickSaveAndView();
        taggablePage = new TaggablePage();
        assertTrue(taggablePage.hasTag(tag1));
        assertTrue(taggablePage.hasTag(tag2));
    }
}

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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Test Inline editing.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class EditInlineTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    // Note: We're not testing basic inline editing since this is already covered by the User Profile tests

    @Test
    public void testEditButtonTriggersInlineEditing()
    {
        // Go to the Admin user profile page and edit it since editing a user profile page is supposed to go in inline
        // editing by default
        ViewPage vp = getUtil().gotoPage("XWiki", "Admin");
        vp.edit();
        Assert.assertTrue(new ViewPage().isInlinePage());
    }

    /* See XE-168 and XWIKI-6992 */
    @Test
    public void testInlineEditCanChangeTitle()
    {
        String title = RandomStringUtils.secure().nextAlphanumeric(4);
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit", "editor=inline&title=" + title);
        InlinePage inlinePage = new InlinePage();
        // Check if the title specified on the request is properly displayed.
        Assert.assertEquals(title, inlinePage.getDocumentTitle());
        // Check if the title specified on the request is displayed in the document hierarchy.
        Assert.assertTrue(inlinePage.getBreadcrumbContent().contains(title));
        // Save the document and check again the displayed title
        ViewPage viewPage = inlinePage.clickSaveAndView();
        Assert.assertEquals(title, viewPage.getDocumentTitle());
    }

    /* See XE-168 */
    @Test
    public void testInlineEditCanChangeParent()
    {
        // Use the parentchild hierarchy mode to be able to assert the parent.
        getUtil().setHierarchyMode("parentchild");

        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit", "editor=inline&parent=Main.SomeParent");
        ViewPage vp = new InlinePage().clickSaveAndView();

        // Check the new parent in the breadcrumbs.
        Assert.assertTrue(vp.hasBreadcrumbContent("SomeParent", false));

        // Restore the hierarchy mode.
        getUtil().setHierarchyMode("reference");
    }

    /* See XWIKI-2389 */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    public void testInlineEditPreservesTitle()
    {
        String title = RandomStringUtils.secure().nextAlphanumeric(4);
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "save", "title=" + title);
        ViewPage vp = new ViewPage();
        Assert.assertEquals(title, vp.getDocumentTitle());
        InlinePage ip = vp.editInline();
        ViewPage vp2 = ip.clickSaveAndView();
        Assert.assertEquals(title, vp2.getDocumentTitle());
    }

    /* See XWIKI-2389 */
    @Test
    public void testInlineEditPreservesParent()
    {
        // Use the parentchild hierarchy mode to be able to assert the parent.
        getUtil().setHierarchyMode("parentchild");

        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "save", "parent=Blog.WebHome");
        ViewPage vp = new ViewPage();
        Assert.assertTrue(vp.hasBreadcrumbContent("Blog", false));

        InlinePage ip = vp.editInline();
        ViewPage vp2 = ip.clickSaveAndView();
        Assert.assertTrue(vp2.hasBreadcrumbContent("Blog", false));

        // Now try the same in the default hierarchy mode, to make sure the default mode preserves the parent.
        getUtil().setHierarchyMode("reference");

        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "edit", "editor=inline");
        InlinePage ip2 = new InlinePage();
        ip2.clickSaveAndView();

        // Switch again to the parentchild hierarchy mode to be able to assert the parent.
        getUtil().setHierarchyMode("parentchild");

        ViewPage vp3 = getUtil().gotoPage(getTestClassName(), getTestMethodName());
        Assert.assertTrue(vp3.hasBreadcrumbContent("Blog", false));

        // Restore the hierarchy mode.
        getUtil().setHierarchyMode("reference");
    }

    /* See XWIKI-2199 */
    @Test
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    public void testInlineEditPreservesTags()
    {
        String tag1 = RandomStringUtils.secure().nextAlphanumeric(4);
        String tag2 = RandomStringUtils.secure().nextAlphanumeric(4);
        getUtil().gotoPage(getTestClassName(), getTestMethodName(), "save", "tags=" + tag1 + "%7C" + tag2);
        TaggablePage taggablePage = new TaggablePage();
        Assert.assertTrue(taggablePage.hasTag(tag1));
        Assert.assertTrue(taggablePage.hasTag(tag2));
        taggablePage.editInline().clickSaveAndView();
        taggablePage = new TaggablePage();
        Assert.assertTrue(taggablePage.hasTag(tag1));
        Assert.assertTrue(taggablePage.hasTag(tag2));
    }
}

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
package org.xwiki.blog.test.ui;

import java.lang.Exception;import java.lang.String;import org.junit.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.blog.test.po.ManageCategoriesPage;

/**
 * Test Blog categories. Tested features: add, rename, delete.
 * 
 * @version $Id$
 * @since 2.3M2
 */
public class BlogCategoriesTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    /**
     * We make sure to have spaces and special chars to ensure categories can be named with any char.
     */
    private static final String CATEGORY = "The \"Do\"s & Don'ts";

    private static final String CATEGORY_RENAME = "New \"categor'y\"";

    private static final String CATEGORY_RENAME_2 =
        "A category with [[//wiki// syntax]] # || onclick=\"alert('fail');return false;\"";

    @Before
    public void setUp() throws Exception
    {
        // clean up
        getUtil().deletePage("Blog", CATEGORY);
        getUtil().deletePage("Blog", CATEGORY_RENAME);
        getUtil().deletePage("Blog", CATEGORY_RENAME_2);
    }

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See http://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See http://jira.xwiki.org/browse/XE-1177")
    })
    public void testCategoryAddRenameRemove()
    {
        categoryAdd(CATEGORY);
        categoryRename(CATEGORY, CATEGORY_RENAME);
        categoryRename(CATEGORY_RENAME, CATEGORY_RENAME_2);
        categoryRemove(CATEGORY_RENAME_2);
    }

    /**
     * Helper method that adds a new category and checks for success
     * 
     * @param name
     */
    private void categoryAdd(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        Assert.assertFalse(categoriesPage.isCategoryPresent(name));

        categoriesPage.clickAddCategory();
        categoriesPage.addCategory(name);
        Assert.assertTrue(categoriesPage.isCategoryPresent(name));
    }

    /**
     * Helper method that renames a category and checks for success
     * 
     * @param fromName source name, must exist
     * @param toName target name, must not exist
     */
    private void categoryRename(String fromName, String toName)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        Assert.assertTrue(categoriesPage.isCategoryPresent(fromName));
        Assert.assertFalse(categoriesPage.isCategoryPresent(toName));

        categoriesPage.renameCategory(fromName, toName);
        Assert.assertFalse(categoriesPage.isCategoryPresent(fromName));
        Assert.assertTrue(categoriesPage.isCategoryPresent(toName));
    }

    /**
     * Helper method that removes a category and checks for success
     * 
     * @param name category name, must exist
     */
    private void categoryRemove(String name)
    {
        ManageCategoriesPage categoriesPage = ManageCategoriesPage.gotoPage();
        Assert.assertTrue(categoriesPage.isCategoryPresent(name));

        categoriesPage.deleteCategory(name);
        Assert.assertFalse(categoriesPage.isCategoryPresent(name));
    }
}

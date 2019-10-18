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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.Keys;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Verify the keyboard shortcuts feature of XWiki.
 * 
 * @version $Id$
 * @since 2.6RC1
 */
public class KeyboardShortcutsTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testKeyboardShortcuts() throws InterruptedException
    {
        getUtil().gotoPage("Sandbox", "WebHome");
        ViewPage viewPage = new ViewPage();

        // Test default edit mode (Wiki for Sandbox.WebHome) key
        EditPage editPage = viewPage.useShortcutKeyForEditing();
        Assert.assertTrue(getUtil().isInWikiEditMode());

        // Test Cancel key
        viewPage = editPage.useShortcutKeyForCancellingEdition();
        Assert.assertTrue(getUtil().isInViewMode());

        // Test Wiki edit key
        viewPage.useShortcutKeyForWikiEditing();
        Assert.assertTrue(getUtil().isInWikiEditMode());

        // Test WYSIWYG edit mode key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForWysiwygEditing();
        Assert.assertTrue(getUtil().isInWYSIWYGEditMode());

        // Test Inline Form edit mode key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForInlineEditing();
        Assert.assertTrue(getUtil().isInInlineEditMode());

        // Test Rights edit mode key on a terminal document
        viewPage = getUtil().gotoPage("Sandbox", "TestPage1");
        viewPage.useShortcutKeyForRightsEditing();
        Assert.assertTrue(getUtil().isInRightsEditMode());

        // Test Rights edit mode key on a non terminal document
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForRightsEditing();
        Assert.assertTrue(getUtil().isInAdminMode());
        AdministrationPage administrationPage = new AdministrationPage();
        Assert.assertTrue(administrationPage.hasSection("PageRights"));

        // Test Object edit mode key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForObjectEditing();
        Assert.assertTrue(getUtil().isInObjectEditMode());

        // Test Class edit mode key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForClassEditing();
        Assert.assertTrue(getUtil().isInClassEditMode());

        // Test Delete key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForPageDeletion();
        Assert.assertTrue(getUtil().isInDeleteMode());

        // Test Rename key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForPageRenaming();
        Assert.assertTrue(getUtil().isInRenameMode());

        // Test View Source key
        viewPage = getUtil().gotoPage("Sandbox", "WebHome");
        viewPage.useShortcutKeyForSourceViewer();
        Assert.assertTrue(getUtil().isInSourceViewMode());
    }
}

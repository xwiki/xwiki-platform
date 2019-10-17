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

        // Test default edit mode (Wiki for Sandbox.WebHome) key
        sendKeys("e");
        new WikiEditPage();
        Assert.assertTrue(getUtil().isInWikiEditMode());

        // Test Cancel key
        getDriver().addPageNotYetReloadedMarker();
        getDriver().createActions().keyDown(Keys.ALT).sendKeys("c").keyUp(Keys.ALT).perform();
        getDriver().waitUntilPageIsReloaded();
        new ViewPage();
        Assert.assertTrue(getUtil().isInViewMode());

        // Test Wiki edit key
        sendKeys("k");
        new ViewPage();
        Assert.assertTrue(getUtil().isInWikiEditMode());

        // Test WYSIWYG edit mode key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("g");
        new ViewPage();
        Assert.assertTrue(getUtil().isInWYSIWYGEditMode());

        // Test Inline Form edit mode key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("f");
        new EditPage();
        Assert.assertTrue(getUtil().isInInlineEditMode());

        // Test Rights edit mode key on a terminal document
        getUtil().gotoPage("Sandbox", "TestPage1");
        sendKeys("r");
        new ViewPage();
        Assert.assertTrue(getUtil().isInRightsEditMode());

        // Test Rights edit mode key on a non terminal document
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("r");
        new ViewPage();
        Assert.assertTrue(getUtil().isInAdminMode());
        AdministrationPage administrationPage = new AdministrationPage();
        Assert.assertTrue(administrationPage.hasSection("PageRights"));

        // Test Object edit mode key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("o");
        new EditPage();
        Assert.assertTrue(getUtil().isInObjectEditMode());

        // Test Class edit mode key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("s");
        new EditPage();
        Assert.assertTrue(getUtil().isInClassEditMode());

        // Test Delete key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys(Keys.DELETE);
        new ViewPage();
        Assert.assertTrue(getUtil().isInDeleteMode());

        // Test Rename key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys(Keys.F2);
        new ViewPage();
        Assert.assertTrue(getUtil().isInRenameMode());

        // Test View Source key
        getUtil().gotoPage("Sandbox", "WebHome");
        sendKeys("d");
        new ViewPage();
        Assert.assertTrue(getUtil().isInSourceViewMode());
    }

    private void sendKeys(CharSequence... keys)
    {
        getDriver().addPageNotYetReloadedMarker();
        getDriver().createActions().sendKeys(keys).perform();
        getDriver().waitUntilPageIsReloaded();
    }
}

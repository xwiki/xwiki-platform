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
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

/**
 * Verify versioning features of documents and attachments.
 * 
 * @version $Id$
 * @since 3.1M2
 */
public class VersionTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    private static final String PAGE_NAME = "HistoryTest";

    private static final String SPACE_NAME = "HistorySpaceTest";

    private static final String TITLE = "Page Title";

    private static final String CONTENT1 = "First version of Content";

    private static final String CONTENT2 = "Second version of Content";

    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testRollbackToFirstVersion() throws Exception
    {
        getUtil().rest().deletePage(SPACE_NAME, PAGE_NAME);

        // Create first version of the page
        ViewPage vp = getUtil().createPage(SPACE_NAME, PAGE_NAME, CONTENT1, TITLE);

        // Adds second version
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.setContent(CONTENT2);
        wikiEditPage.clickSaveAndView();

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify that we can rollback to the first version
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        vp = historyTab.rollbackToVersion("1.1");

        // Rollback doesn't wait...
        // Wait for the comment tab to be selected since we're currently on the history tab and rolling
        // back is going to load a new page and make the focus active on the comments tab.
        vp.waitForDocExtraPaneActive("comments");

        Assert.assertEquals("First version of Content", vp.getContent());

        historyTab = vp.openHistoryDocExtraPane();
        Assert.assertEquals("Rollback to version 1.1", historyTab.getCurrentVersionComment());
        Assert.assertEquals("Administrator", historyTab.getCurrentAuthor());
    }

    /**
     * See XWIKI-8781
     */
    @Test
    @IgnoreBrowsers({
    @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason = "See https://jira.xwiki.org/browse/XE-1146"),
    @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason = "See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testDeleteLatestVersion() throws Exception
    {
        getUtil().rest().deletePage(SPACE_NAME, PAGE_NAME);

        // Create first version of the page
        ViewPage vp = getUtil().createPage(SPACE_NAME, PAGE_NAME, CONTENT1, TITLE);

        // Adds second version
        WikiEditPage wikiEditPage = vp.editWiki();
        wikiEditPage.setContent(CONTENT2);
        wikiEditPage.clickSaveAndView();

        // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
        // view mode) is fixed.
        vp.waitForDocExtraPaneActive("comments");

        // Verify and delete the latest version.
        HistoryPane historyTab = vp.openHistoryDocExtraPane();
        Assert.assertEquals("2.1", historyTab.getCurrentVersion());
        historyTab = historyTab.deleteVersion("2.1");

        // Verify that the current version is now the previous one.
        Assert.assertEquals("1.1", historyTab.getCurrentVersion());
        Assert.assertEquals("Administrator", historyTab.getCurrentAuthor());
    }
}

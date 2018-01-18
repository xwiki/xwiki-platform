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
package org.xwiki.administration.test.ui;

import java.net.URL;

import org.junit.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ImportAdministrationSectionPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.browser.IgnoreBrowser;
import org.xwiki.test.ui.browser.IgnoreBrowsers;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Test the Import XAR feature.
 *
 * @version $Id$
 * @since 2.3M1
 */
public class ImportIT extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private static final String PACKAGE_WITHOUT_HISTORY = "Main.TestPage-no-history.xar";

    private static final String PACKAGE_WITH_HISTORY = "Main.TestPage-with-history.xar";

    private static final String BACKUP_PACKAGE = "Main.TestPage-backup.xar";

    private AdministrationPage adminPage;

    private ImportAdministrationSectionPage sectionPage;

    @Before
    public void setUp() throws Exception
    {
        // Delete Test Page we import from XAR to ensure to start with a predefined state.
        getUtil().rest().deletePage("Main", "TestPage");

        this.adminPage = AdministrationPage.gotoPage();
        this.sectionPage = this.adminPage.clickImportSection();

        // Remove our packages if they're there already, to ensure to start with a predefined state.
        if (this.sectionPage.isPackagePresent(PACKAGE_WITH_HISTORY)) {
            this.sectionPage.deletePackage(PACKAGE_WITH_HISTORY);
        }
        if (this.sectionPage.isPackagePresent(PACKAGE_WITHOUT_HISTORY)) {
            this.sectionPage.deletePackage(PACKAGE_WITHOUT_HISTORY);
        }
        if (this.sectionPage.isPackagePresent(BACKUP_PACKAGE)) {
            this.sectionPage.deletePackage(BACKUP_PACKAGE);
        }
    }

    /**
     * Verify that the Import page doesn't list any package by default in default XE.
     *
     * @since 2.6RC1
     */
    @Test
    public void testImportHasNoPackageByDefault()
    {
        Assert.assertEquals(0, this.sectionPage.getPackageNames().size());
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testImportWithHistory()
    {
        URL fileUrl = this.getClass().getResource("/" + PACKAGE_WITH_HISTORY);

        this.sectionPage.attachPackage(fileUrl);
        this.sectionPage.selectPackage(PACKAGE_WITH_HISTORY);

        this.sectionPage.selectReplaceHistoryOption();
        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        Assert.assertEquals("3.1", history.getCurrentVersion());
        Assert.assertEquals("A third version of the document", history.getCurrentVersionComment());
        Assert.assertTrue(history.hasVersionWithSummary("A new version of the document"));
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testImportWithNewHistoryVersion()
    {
        URL fileUrl = this.getClass().getResource("/" + PACKAGE_WITHOUT_HISTORY);

        this.sectionPage.attachPackage(fileUrl);
        this.sectionPage.selectPackage(PACKAGE_WITHOUT_HISTORY);

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        Assert.assertEquals("1.1", history.getCurrentVersion());
        Assert.assertEquals("Imported from XAR", history.getCurrentVersionComment());
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testImportAsBackup()
    {
        URL fileUrl = this.getClass().getResource("/" + BACKUP_PACKAGE);

        this.sectionPage.attachPackage(fileUrl);
        this.sectionPage.selectPackage(BACKUP_PACKAGE);

        WebElement importAsBackup = getDriver().findElement(By.name("importAsBackup"));
        Assert.assertTrue(importAsBackup.isSelected());

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        Assert.assertEquals("JohnDoe", history.getCurrentAuthor());
    }

    @Test
    @IgnoreBrowsers({
        @IgnoreBrowser(value = "internet.*", version = "8\\.*", reason="See https://jira.xwiki.org/browse/XE-1146"),
        @IgnoreBrowser(value = "internet.*", version = "9\\.*", reason="See https://jira.xwiki.org/browse/XE-1177")
    })
    public void testImportWhenImportAsBackupIsNotSelected()
    {
        URL fileUrl = this.getClass().getResource("/" + BACKUP_PACKAGE);

        this.sectionPage.attachPackage(fileUrl);
        this.sectionPage.selectPackage(BACKUP_PACKAGE);

        WebElement importAsBackup = getDriver().findElement(By.name("importAsBackup"));
        importAsBackup.click();
        Assert.assertFalse(importAsBackup.isSelected());

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        Assert.assertEquals("superadmin", history.getCurrentAuthor());
    }
}

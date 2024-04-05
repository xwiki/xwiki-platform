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

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ImportAdministrationSectionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the Import XAR feature.
 *
 * @version $Id$
 */
@UITest(properties = {
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.packaging.PackagePlugin"})
class XARImportIT
{
    private static final String PACKAGE_WITHOUT_HISTORY = "Main.TestPage-no-history.xar";

    private static final String PACKAGE_WITH_HISTORY = "Main.TestPage-with-history.xar";

    private static final String PACKAGE_WITH_HISTORY13 = "Main.TestPage-with-history-1.3.xar";

    private static final String BACKUP_PACKAGE = "Main.TestPage-backup.xar";

    private static final LocalDocumentReference TESTPAGE = new LocalDocumentReference("Main", "TestPage");

    private static final String ATTACHE_NAME = "testattachment.txt";

    private AdministrationPage adminPage;

    private ImportAdministrationSectionPage sectionPage;

    @BeforeEach
    public void setUp(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Delete Test Page we import from XAR to ensure to start with a predefined state.
        setup.rest().delete(TESTPAGE);

        this.adminPage = AdministrationPage.gotoPage();
        this.sectionPage = this.adminPage.clickImportSection();

        // Remove our packages if they're there already, to ensure to start with a predefined state.
        if (this.sectionPage.isPackagePresent(PACKAGE_WITH_HISTORY)) {
            this.sectionPage.deletePackage(PACKAGE_WITH_HISTORY);
        }
        if (this.sectionPage.isPackagePresent(PACKAGE_WITH_HISTORY13)) {
            this.sectionPage.deletePackage(PACKAGE_WITH_HISTORY13);
        }
        if (this.sectionPage.isPackagePresent(PACKAGE_WITHOUT_HISTORY)) {
            this.sectionPage.deletePackage(PACKAGE_WITHOUT_HISTORY);
        }
        if (this.sectionPage.isPackagePresent(BACKUP_PACKAGE)) {
            this.sectionPage.deletePackage(BACKUP_PACKAGE);
        }
    }

    private File getFileToUpload(TestConfiguration testConfiguration, String filename)
    {
        return new File(testConfiguration.getBrowser().getTestResourcesPath(), "XARImportIT/" + filename);
    }


    private void assertImportWithHistory(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, PACKAGE_WITH_HISTORY);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(PACKAGE_WITH_HISTORY);

        this.sectionPage.selectReplaceHistoryOption();
        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        assertEquals("3.1", history.getCurrentVersion());
        assertEquals("A third version of the document", history.getCurrentVersionComment());
        assertTrue(history.hasVersionWithSummary("A new version of the document"));

        AttachmentsPane attachments = new AttachmentsViewPage().openAttachmentsDocExtraPane();

        assertEquals(1, attachments.getNumberOfAttachments());
        assertEquals("3 bytes", attachments.getSizeOfAttachment(ATTACHE_NAME));
        assertEquals("1.2", attachments.getLatestVersionOfAttachment(ATTACHE_NAME));

        attachments.getAttachmentLink(ATTACHE_NAME).click();
        assertEquals("1.2", setup.getDriver().findElement(By.tagName("html")).getText());
    }

    /**
     * Verify that the Import page doesn't list any package by default in default XE.
     */
    @Test
    void testImportHasNoPackageByDefault()
    {
        assertEquals(0, this.sectionPage.getPackageNames().size());
    }

    @Test
    void testImportWithHistory13(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, PACKAGE_WITH_HISTORY13);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(PACKAGE_WITH_HISTORY13);

        this.sectionPage.selectReplaceHistoryOption();
        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        assertEquals("3.1", history.getCurrentVersion());
        assertEquals("A third version of the document", history.getCurrentVersionComment());
        assertTrue(history.hasVersionWithSummary("A new version of the document"));

        AttachmentsPane attachments = new AttachmentsViewPage().openAttachmentsDocExtraPane();

        assertEquals(1, attachments.getNumberOfAttachments());
        assertEquals("3 bytes", attachments.getSizeOfAttachment(ATTACHE_NAME));
        assertEquals("1.2", attachments.getLatestVersionOfAttachment(ATTACHE_NAME));

        attachments.getAttachmentLink(ATTACHE_NAME).click();
        assertEquals("1.2", setup.getDriver().findElement(By.tagName("html")).getText());
    }

    @Test
    void testImportWithHistoryWhenNoPage(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        assertImportWithHistory(setup, testReference, testConfiguration);
    }

    @Test
    void testImportWithHistoryWhenPage(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        Page page = setup.rest().page(TESTPAGE);
        page.setContent("previous page");
        setup.rest().save(page);
        setup.rest().attachFile(new EntityReference("testattachment.txt", EntityType.ATTACHMENT, TESTPAGE),
            "previous attachment".getBytes(), true);

        assertImportWithHistory(setup, testReference, testConfiguration);
    }

    @Test
    void testImportWithNewHistoryVersion(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, PACKAGE_WITHOUT_HISTORY);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(PACKAGE_WITHOUT_HISTORY);

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        assertEquals("1.1", history.getCurrentVersion());
        assertEquals("Imported from XAR", history.getCurrentVersionComment());
    }

    @Test
    void testImportAsBackup(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, BACKUP_PACKAGE);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(BACKUP_PACKAGE);

        assertTrue(this.sectionPage.isImportAsBackup());

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        assertEquals("JohnDoe", history.getCurrentAuthor());
    }

    @Test
    void testImportWhenImportAsBackupIsNotSelected(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, BACKUP_PACKAGE);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(BACKUP_PACKAGE);

        assertFalse(this.sectionPage.clickImportAsBackup());

        this.sectionPage.importPackage();

        ViewPage importedPage = this.sectionPage.clickImportedPage("Main.TestPage");

        // Since the page by default opens the comments pane, if we instantly click on the history, the two tabs
        // will race for completion. Let's wait for comments first.
        importedPage.openCommentsDocExtraPane();
        HistoryPane history = importedPage.openHistoryDocExtraPane();

        assertEquals("superadmin", history.getCurrentAuthor());
    }
}

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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openqa.selenium.By;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.administration.test.po.ExportAdministrationSectionPage;
import org.xwiki.administration.test.po.ImportAdministrationSectionPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    /**
     * Filename containing HTML that would trigger XSS if the package explorer doesn't escape it.
     * The file is uploaded via REST API to avoid creating a file with special characters on the filesystem.
     * No closing tag is used to avoid a forward slash (/) in the filename, which would be removed by
     * XWikiDocument#setAttachment.
     */
    private static final String XSS_PACKAGE = "<b id=xss-filename>XSSImport.xar";

    private static final LocalDocumentReference IMPORT_PAGE = new LocalDocumentReference("XWiki", "XWikiPreferences");

    private static final LocalDocumentReference TESTPAGE = new LocalDocumentReference("Main", "TestPage");

    private static final String ATTACHE_NAME = "testattachment.txt";

    private static final String ADMINISTRATION_UI = "org.xwiki.platform:xwiki-platform-administration-ui";

    private static final String MAIN_WIKI = "xwiki";

    private static final String XWIKI_SPACE = "XWiki";

    private static final String XWIKI_PREFERENCES = "XWikiPreferences";

    private static final String WEB_HOME = "WebHome";

    private static final String XAR_EXTENSION = ".xar";

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
        if (this.sectionPage.isPackagePresent(XSS_PACKAGE)) {
            this.sectionPage.deletePackage(XSS_PACKAGE);
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

    @Test
    void testImportWithInvalidCSRFToken(TestUtils setup, TestConfiguration testConfiguration)
    {
        File file = getFileToUpload(testConfiguration, PACKAGE_WITHOUT_HISTORY);

        this.sectionPage.attachPackage(file);
        this.sectionPage.selectPackage(PACKAGE_WITHOUT_HISTORY);

        // Corrupt the CSRF token to trigger a CSRF validation failure.
        setup.getDriver().executeJavascript("document.documentElement.dataset.xwikiFormToken = 'invalid'");

        String errorMessage = this.sectionPage.importPackageWithExpectedError();

        assertTrue(errorMessage.contains("CSRF validation failed"),
            "Expected CSRF error, got: " + errorMessage);
    }

    @Test
    void testXSSInPackageExplorer(TestUtils setup) throws Exception
    {
        // Upload the XSS test package directly via REST API using a filename that contains HTML.
        // This avoids creating a file with special characters on the filesystem.
        // All the fields displayed by the package explorer contain unique HTML that would create
        // identifiable DOM elements if injected without escaping.
        byte[] xarBytes;
        try (InputStream is = getClass().getResourceAsStream("/XARImportIT/XSSImport.xar")) {
            assertNotNull(is);
            xarBytes = is.readAllBytes();
        }
        EntityReference attachmentRef = new EntityReference(XSS_PACKAGE, EntityType.ATTACHMENT, IMPORT_PAGE);
        setup.attachFile(attachmentRef, xarBytes, false);

        // Reload the import section to see the newly uploaded package.
        this.sectionPage = ImportAdministrationSectionPage.gotoPage();
        try {
            // Click the package link and wait for the package explorer to load.
            this.sectionPage.selectPackage(XSS_PACKAGE);

            TreeElement packageTree = this.sectionPage.getPackageTree();
            TreeNodeElement spaceNode = packageTree.getTopLevelNodes().get(0);
            spaceNode.open();
            TreeNodeElement docNode = spaceNode.getChildren().get(0);

            assertAll(
                // Positive: verify values ARE displayed as literal text (not parsed as HTML)
                () -> assertEquals(XSS_PACKAGE, this.sectionPage.getPackageFileName(),
                    "Attachment filename not displayed as plain text"),
                () -> assertEquals(Optional.of("<b id=\"xss-name\">XSSName</b>"),
                    this.sectionPage.getPackageName(),
                    "Package name not displayed as plain text"),
                () -> assertEquals(Optional.of("<b id=\"xss-version\">1.0</b>"),
                    this.sectionPage.getPackageVersion(),
                    "Package version not displayed as plain text"),
                () -> assertEquals(Optional.of("<b id=\"xss-author\">XSSAuthor</b>"),
                    this.sectionPage.getPackageAuthor(),
                    "Package author not displayed as plain text"),
                () -> assertEquals(Optional.of("<b id=\"xss-licence\">LGPL</b>"),
                    this.sectionPage.getPackageLicense(),
                    "Package license not displayed as plain text"),
                () -> assertEquals("<b id=\"xss-space\">Main</b>", spaceNode.getLabel(),
                    "Space name not displayed as plain text"),
                () -> assertEquals("<b id=\"xss-doc\">XSSPage</b>", docNode.getLabel(),
                    "Document name not displayed as plain text"),
                // Negative: verify no HTML was injected into the DOM
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-filename")).isEmpty(),
                    "Attachment filename injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-name")).isEmpty(),
                    "Package name injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-version")).isEmpty(),
                    "Package version injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-author")).isEmpty(),
                    "Package author injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-licence")).isEmpty(),
                    "Package licence injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-space")).isEmpty(),
                    "Space name injected as HTML"),
                () -> assertTrue(
                    setup.getDriver().findElementsWithoutWaiting(By.id("xss-doc")).isEmpty(),
                    "Document name injected as HTML")
            );
        } finally {
            // Clean up the XSS test package.
            setup.deleteAttachement(IMPORT_PAGE, XSS_PACKAGE);
        }
    }

    /**
     * Export a nested page whose view right is restricted to the admin group, import it in a subwiki, and verify that
     * the rights are still enforced there.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false, extensions = ADMINISTRATION_UI)
    void exportImportPageWithRights(WikiReference subwiki, TestUtils setup, TestReference testReference)
        throws Exception
    {
        // Nested page without intermediate pages.
        DocumentReference page = new DocumentReference(WEB_HOME,
            new SpaceReference("4", new SpaceReference("3", new SpaceReference("2",
                new SpaceReference("1", testReference.getLastSpaceReference())))));
        DocumentReference importedPage = page.setWikiReference(subwiki);
        DocumentReference controlPage = new DocumentReference(WEB_HOME,
            new SpaceReference("Control", testReference.getLastSpaceReference())).setWikiReference(subwiki);
        String testName = testReference.getLastSpaceReference().getName();
        String userName = testName + "User";
        String packageName = testName + XAR_EXTENSION;

        setup.setCurrentWiki(MAIN_WIKI);
        setup.rest().delete(page);
        setup.rest().delete(importedPage);
        setup.deletePage(XWIKI_SPACE, userName);
        try {
            setup.rest().savePage(page, "Restricted content", "4");
            setup.setRights(page, "XWiki.XWikiAdminGroup", "", "view", true);
            setup.rest().savePage(controlPage, "Control content", "Control");
            setup.createUser(userName, userName, null);

            setup.login(userName, userName);
            assertTrue(setup.gotoPage(page).isForbidden(), "The user should not be allowed to view the page");

            setup.loginAsSuperAdmin();
            byte[] xar = exportXAR(setup, page, Map.of("name", testName, "pages", "xwiki:" + toLocal(page)));

            ImportAdministrationSectionPage importSection = gotoImportSection(setup, subwiki, packageName, xar);
            try {
                importSection.importPackage();

                // The XWikiRights object is imported along with the page.
                org.xwiki.rest.model.jaxb.Object rightsObject =
                    setup.rest().get(new ObjectReference("XWiki.XWikiRights[0]", importedPage));
                assertEquals("XWiki.XWikiAdminGroup", getPropertyValue(rightsObject, "groups"));
                assertEquals("view", getPropertyValue(rightsObject, "levels"));
                assertEquals("Restricted content", setup.gotoPage(importedPage).getContent());

                setup.login(userName, userName);
                assertTrue(setup.gotoPage(importedPage).isForbidden(),
                    "The user should not be allowed to view the imported page");
                // The global user can view the other pages of the subwiki.
                assertEquals("Control content", setup.gotoPage(controlPage).getContent());
            } finally {
                setup.loginAsSuperAdmin();
                setup.deleteAttachement(getImportPage(subwiki), packageName);
            }
        } finally {
            setup.setCurrentWiki(MAIN_WIKI);
            setup.rest().delete(page);
            setup.rest().delete(importedPage);
            setup.rest().delete(controlPage);
            setup.deletePage(XWIKI_SPACE, userName);
        }
    }

    /**
     * Export a page with its history, import it in a subwiki replacing the page history, and verify that the subwiki
     * page has the same versions and version comments as the original.
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false, extensions = ADMINISTRATION_UI)
    void exportImportPageWithHistory(WikiReference subwiki, TestUtils setup, TestReference testReference)
        throws Exception
    {
        DocumentReference page =
            new DocumentReference(WEB_HOME, new SpaceReference("2", testReference.getLastSpaceReference()));
        DocumentReference importedPage = page.setWikiReference(subwiki);
        String testName = testReference.getLastSpaceReference().getName();
        String packageName = testName + XAR_EXTENSION;

        setup.setCurrentWiki(MAIN_WIKI);
        setup.rest().delete(page);
        setup.rest().delete(importedPage);
        try {
            Map<String, String> versions = new LinkedHashMap<>();
            versions.put("Some text\n\nMore text", "Add text");
            versions.put("Some text", "Remove text");
            versions.put("Some text\n\n|=Column 1|=Column 2\n|Cell 1|Cell 2", "Add a table");
            versions.put("Some text\n\n|=Column 1|=Column 2\n|Cell 1|Cell 2\n\n[[Home>>Main.WebHome]]", "Add a link");
            for (Map.Entry<String, String> version : versions.entrySet()) {
                Page restPage = setup.rest().page(page);
                restPage.setContent(version.getKey());
                restPage.setComment(version.getValue());
                setup.rest().save(restPage);
            }
            List<String> history = getHistory(setup, page);
            assertEquals(List.of("4.1 Add a link", "3.1 Add a table", "2.1 Remove text", "1.1 Add text"), history);

            setup.loginAsSuperAdmin();
            byte[] xar = exportXAR(setup, page, Map.of("name", testName, "pages", "xwiki:" + toLocal(page),
                "history", "true"));

            ImportAdministrationSectionPage importSection = gotoImportSection(setup, subwiki, packageName, xar);
            try {
                importSection.selectReplaceHistoryOption();
                importSection.importPackage();

                assertEquals(history, getHistory(setup, importedPage));
                assertEquals(setup.rest().<Page>get(page).getContent(),
                    setup.rest().<Page>get(importedPage).getContent());
                // The history tab displays the imported versions.
                ViewPage importedViewPage = setup.gotoPage(importedPage);
                importedViewPage.openCommentsDocExtraPane();
                HistoryPane historyPane = importedViewPage.openHistoryDocExtraPane();
                assertEquals("4.1", historyPane.getCurrentVersion());
                assertEquals("Add a link", historyPane.getCurrentVersionComment());
            } finally {
                setup.deleteAttachement(getImportPage(subwiki), packageName);
            }
        } finally {
            setup.setCurrentWiki(MAIN_WIKI);
            setup.rest().delete(page);
            setup.rest().delete(importedPage);
        }
    }

    /**
     * Export the pages created in the main wiki, except the ones of the XWiki space, from the Export administration
     * section, and import the pages of the test space in a subwiki (the package also holds the pages created by the
     * other tests, which must not leak in the subwiki).
     */
    @ParameterizedTest
    @WikisSource(mainWiki = false, extensions = ADMINISTRATION_UI)
    void exportImportPartialWiki(WikiReference subwiki, TestUtils setup, TestReference testReference)
        throws Exception
    {
        SpaceReference space = testReference.getLastSpaceReference();
        DocumentReference homePage = testReference;
        DocumentReference classPage =
            new DocumentReference("ItemClass", new SpaceReference("Code", space));
        DocumentReference itemPage = new DocumentReference("Item", space);
        List<DocumentReference> pages = List.of(homePage, classPage, itemPage);
        String className = toLocal(classPage);
        String testName = space.getName();
        String packageName = testName + XAR_EXTENSION;

        setup.loginAsSuperAdmin();
        setup.setCurrentWiki(MAIN_WIKI);
        deletePages(setup, pages, subwiki);
        try {
            // A small application: a class, a page holding an object of this class, and a home page.
            setup.rest().savePage(homePage, "Partial wiki home", "Partial wiki");
            setup.rest().savePage(classPage, "", "Item Class");
            setup.addClassProperty(classPage, "name", "String");
            setup.rest().savePage(itemPage, "Item content", "Item");
            setup.rest().addObject(itemPage, className, "name", "First item");

            ExportAdministrationSectionPage exportSection = ExportAdministrationSectionPage.gotoPage();
            exportSection.setFilter(ExportAdministrationSectionPage.Filter.CREATED_PAGES);
            exportSection.unselectAllChildren(exportSection.getPageTree().getTopLevelNodes().stream()
                .filter(node -> XWIKI_SPACE.equals(node.getLabel())).findFirst()
                .orElseThrow(() -> new AssertionError("No XWiki node in the export tree")));
            exportSection.setTargetXWikiVersion("[12.0,)");
            exportSection.setPackageName(testName);
            Map<String, List<String>> exportParameters = exportSection.export();
            assertEquals(List.of(ExportAdministrationSectionPage.Filter.CREATED_PAGES.getId()),
                exportParameters.get("filter"));

            byte[] xar = exportXAR(setup, new DocumentReference(MAIN_WIKI, XWIKI_SPACE, XWIKI_PREFERENCES),
                exportParameters);
            List<String> entries = getEntryNames(xar);
            assertThat(entries, hasItems(pages.stream().map(XARImportIT::toEntryName).toArray(String[]::new)));
            assertThat(entries, everyItem(not(startsWith(XWIKI_SPACE + "/"))));

            ImportAdministrationSectionPage importSection = gotoImportSection(setup, subwiki, packageName, xar);
            try {
                TreeElement packageTree = importSection.getPackageTree();
                List<String> packageNodes = packageTree.getNodeIDs();
                importSection.selectNoDocuments();
                assertEquals(List.of(), packageTree.getSelectedNodeIDs());
                importSection.selectAllDocuments();
                assertEquals(packageNodes.size(), packageTree.getSelectedNodeIDs().size());
                // Import only the pages of the test space.
                importSection.selectNoDocuments();
                getSpaceNode(packageTree, space).select();
                importSection.importPackage();

                assertEquals("Partial wiki home", setup.gotoPage(homePage.setWikiReference(subwiki)).getContent());
                assertEquals("Item content", setup.gotoPage(itemPage.setWikiReference(subwiki)).getContent());
                DocumentReference importedItemPage = itemPage.setWikiReference(subwiki);
                org.xwiki.rest.model.jaxb.Object itemObject =
                    setup.rest().get(new ObjectReference(className + "[0]", importedItemPage));
                assertEquals("First item", getPropertyValue(itemObject, "name"));
            } finally {
                setup.deleteAttachement(getImportPage(subwiki), packageName);
            }
        } finally {
            setup.setCurrentWiki(MAIN_WIKI);
            deletePages(setup, pages, subwiki);
        }
    }

    /**
     * @return the node of the given space in the package tree, opening its parent space nodes
     */
    private TreeNodeElement getSpaceNode(TreeElement packageTree, SpaceReference space)
    {
        List<TreeNodeElement> nodes = packageTree.getTopLevelNodes();
        TreeNodeElement spaceNode = null;
        for (EntityReference spaceReference : space.getReversedReferenceChain()) {
            if (spaceReference.getType() == EntityType.SPACE) {
                if (spaceNode != null) {
                    nodes = spaceNode.open().getChildren();
                }
                spaceNode = nodes.stream().filter(node -> spaceReference.getName().equals(node.getLabel()))
                    .findFirst().orElseThrow(() -> new AssertionError("No [" + spaceReference + "] package node"));
            }
        }
        return spaceNode;
    }

    private static String toEntryName(DocumentReference reference)
    {
        return toLocal(reference).replace('.', '/') + ".xml";
    }

    private void deletePages(TestUtils setup, List<DocumentReference> pages, WikiReference subwiki) throws Exception
    {
        for (DocumentReference page : pages) {
            setup.rest().delete(page);
            setup.rest().delete(page.setWikiReference(subwiki));
        }
    }

    private static String toLocal(DocumentReference reference)
    {
        List<String> names = new ArrayList<>();
        reference.getSpaceReferences().forEach(spaceReference -> names.add(spaceReference.getName()));
        names.add(reference.getName());
        return String.join(".", names);
    }

    /**
     * Send the export request directly: the browser saves the exported package in its download directory, which the
     * test cannot read. The export action only exports on POST requests, so the parameters are sent as a form.
     */
    private byte[] exportXAR(TestUtils setup, DocumentReference reference, Map<String, ?> parameters)
        throws Exception
    {
        List<NameValuePair> formParameters = new ArrayList<>();
        parameters.forEach((key, value) -> {
            List<?> values = value instanceof List<?> list ? list : List.of(value);
            values.forEach(item -> formParameters.add(new BasicNameValuePair(key, String.valueOf(item))));
        });
        HttpPost request = new HttpPost(
            setup.getCurrentExecutor().getHttpClientBaseURL() + setup.getPath(reference, "export", ""));
        request.setEntity(new UrlEncodedFormEntity(formParameters, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response =
            TestUtils.assertStatusCodes(setup.execute(request), false, HttpStatus.SC_OK)) {
            return EntityUtils.toByteArray(response.getEntity());
        }
    }

    private List<String> getEntryNames(byte[] xar) throws Exception
    {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xar))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                names.add(entry.getName());
            }
        }
        return names;
    }

    private DocumentReference getImportPage(WikiReference wiki)
    {
        return new DocumentReference(wiki.getName(), XWIKI_SPACE, XWIKI_PREFERENCES);
    }

    /**
     * Attach the package to the administration page of the given wiki (as the upload form of the Import section does)
     * and select it in the Import section of this wiki.
     */
    private ImportAdministrationSectionPage gotoImportSection(TestUtils setup, WikiReference wiki, String packageName,
        byte[] xar) throws Exception
    {
        setup.attachFile(new EntityReference(packageName, EntityType.ATTACHMENT, getImportPage(wiki)), xar, false);
        setup.setCurrentWiki(wiki.getName());
        ImportAdministrationSectionPage importSection = ImportAdministrationSectionPage.gotoPage();
        importSection.selectPackage(packageName);
        return importSection;
    }

    private List<String> getHistory(TestUtils setup, DocumentReference page) throws Exception
    {
        History history = setup.rest().get(PageHistoryResource.class, page);
        return history.getHistorySummaries().stream()
            .map(summary -> summary.getVersion() + " " + summary.getComment()).toList();
    }

    private String getPropertyValue(org.xwiki.rest.model.jaxb.Object object, String propertyName)
    {
        return object.getProperties().stream().filter(property -> propertyName.equals(property.getName()))
            .map(Property::getValue).findFirst().orElseGet(() -> fail("No property [" + propertyName + "]"));
    }
}

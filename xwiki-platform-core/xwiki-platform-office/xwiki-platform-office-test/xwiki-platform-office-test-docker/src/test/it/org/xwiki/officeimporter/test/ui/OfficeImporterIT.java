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
package org.xwiki.officeimporter.test.ui;

import java.io.File;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.test.po.OfficeImporterPage;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DeletePageConfirmationPage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the office importer.
 *
 * @version $Id$
 * @since 7.3M1
 */
@UITest(office = true, servletEngine = ServletEngine.TOMCAT,
    forbiddenEngines = {
        // These tests need to have XWiki running inside a Docker container (we chose Tomcat since it's the most
        // used one), because they need LibreOffice to be installed, and we cannot guarantee that it is installed on the
        // host machine.
        ServletEngine.JETTY_STANDALONE
    },
    properties = {
        // Add the FileUploadPlugin which is needed by the test to upload some office files to import
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",
        // Starting or stopping the Office server requires PR (for the current user, on the main wiki reference)
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.OfficeImporterAdmin"
    }
)
class OfficeImporterIT
{
    private TestUtils setup;

    private TestConfiguration testConfiguration;

    @BeforeEach
    public void setUp(TestUtils setup, TestConfiguration testConfiguration)
    {
        this.setup = setup;
        this.testConfiguration = testConfiguration;

        setup.loginAsSuperAdmin();
        // Connect the wiki to the office server if it is not already done
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        administrationPage.clickSection("Content", "Office Server");
        OfficeServerAdministrationSectionPage officeServerAdministrationSectionPage =
            new OfficeServerAdministrationSectionPage();
        if (!"Connected".equals(officeServerAdministrationSectionPage.getServerState())) {
            officeServerAdministrationSectionPage.startServer();
        }
    }

    @Test
    void verifyImport(TestInfo info)
    {
        verifyImports(info);
        verifySplitByHeadings(info);
        verifyChildNamingMethodInputVisibility(info);
    }

    /**
     * A basic test that imports some documents and verify they are correctly imported.
     * <p>
     * TODO: Do a more advanced check about styling and content.
     */
    private void verifyImports(TestInfo info)
    {
        String testName = info.getTestMethod().get().getName();
        // Test word file
        ViewPage resultPage = importFile(testName, "msoffice.97-2003/Test.doc");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage(testName);

        // Test power point file
        resultPage = importFile(testName, "msoffice.97-2003/Test.ppt");
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(testName);

        // Test excel file
        resultPage = importFile(testName, "msoffice.97-2003/Test.xls");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage(testName);

        // Test ODT file
        resultPage = importFile(testName, "ooffice.3.0/Test.odt");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        WikiEditPage wikiEditPage = resultPage.editWiki();
        String regex = "(?<imageName>Test_[\\w]+\\.png)";
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(wikiEditPage.getContent());
        assertTrue(matcher.find());
        String imageName = matcher.group("imageName");
        resultPage = wikiEditPage.clickCancel();
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(4, attachmentsPane.getNumberOfAttachments());
        assertTrue(attachmentsPane.attachmentExistsByFileName(imageName));
        deletePage(testName);

        // Test ODP file
        resultPage = importFile(testName, "ooffice.3.0/Test.odp");
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(testName);

        // Test ODS file
        resultPage = importFile(testName, "ooffice.3.0/Test.ods");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage(testName);

        // Test ODT file with accents
        resultPage = importFile(testName, "ooffice.3.0/Test_accents & é$ù !-_.+();@=.odt");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        wikiEditPage = resultPage.editWiki();
        regex = "(?<imageName>Test_accents & é\\$ù !-_\\.\\+\\(\\);\\\\@=_\\w+\\.png)";
        pattern = Pattern.compile(regex, Pattern.MULTILINE);
        String wikiContent = wikiEditPage.getContent();
        matcher = pattern.matcher(wikiContent);
        assertTrue(matcher.find(), String.format("Pattern [%s] not found in [%s]", regex, wikiContent));
        imageName = matcher.group("imageName");
        resultPage = wikiEditPage.clickCancel();
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals(4, attachmentsPane.getNumberOfAttachments());
        // the \ before the @ needs to be removed as it's not in the filename
        assertTrue(attachmentsPane.attachmentExistsByFileName(imageName.replace("\\@", "@")));
        deletePage(testName);

        // Test power point file with accents
        resultPage = importFile(testName, "msoffice.97-2003/Test_accents & é$ù !-_.+();@=.ppt");
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test_accents & é$ù !-_.+();@=-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test_accents & é$ù !-_.+();@=-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test_accents & é$ù !-_.+();@=-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test_accents & é$ù !-_.+();@=-slide3.jpg"));
        wikiEditPage = resultPage.editWiki();
        assertTrue(wikiEditPage.getContent().contains("[[image:Test_accents & é$ù !-_.+();\\@=-slide0.jpg]]"));
        resultPage = wikiEditPage.clickCancel();
        deletePage(testName);
    }

    public void verifySplitByHeadings(TestInfo info)
    {
        String testName = info.getTestMethod().get().getName();
        ViewPage resultPage = importFile(testName, "ToSplit.odt", true);
        assertTrue(StringUtils.contains(resultPage.getContent(), "Introduction"));

        // See children
        verifyChild(testName, "First Part", "Hello, this is the first part of my story!");
        verifyChild(testName, "Second Part", "This is the second part of my story!");
        verifyChild(testName, "Last Part", "It's finished. Thanks you!");

        // Go back to the parent
        resultPage = this.setup.gotoPage(new DocumentReference("xwiki", getClass().getSimpleName(), testName));
        deletePageWithChildren(resultPage);
    }

    /**
     * Test if the expected child exists at the expected place (as a children of the target page).
     */
    private void verifyChild(String testName, String expectedName, String expectedContent)
    {
        ViewPage child = this.setup.gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), expectedName));
        assertTrue(child.exists());
        assertEquals(expectedName, child.getDocumentTitle());
        assertTrue(StringUtils.contains(child.getContent(), expectedContent));
    }

    /**
     * Depending on if the target page is terminal or not, the "childNamingMethod" input is displayed or not.
     */
    public void verifyChildNamingMethodInputVisibility(TestInfo info)
    {
        DocumentReference testDocument =
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName()),
                info.getTestMethod().get().getName());

        // Cleaning
        this.setup.deletePage(testDocument);

        // 1: create a terminal page
        CreatePagePage createPagePage = this.setup.gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(true);
        createPagePage.clickCreate();
        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        // Test
        assertTrue(officeImporterPage.isChildPagesNamingMethodDisplayed());

        // 2: create a non terminal page
        createPagePage = this.setup.gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(false);
        createPagePage.clickCreate();
        officeImporterPage = new OfficeImporterPage();
        // Test
        assertFalse(officeImporterPage.isChildPagesNamingMethodDisplayed());
    }

    /**
     * Import an office file in the wiki.
     *
     * @param fileName name of the file to import (the file should be located in test /resources/ folder)
     * @return the result page
     */
    private ViewPage importFile(String testName, String fileName)
    {
        return importFile(testName, fileName, false);
    }

    private File getResourceFile(String filename)
    {
        return new File(this.testConfiguration.getBrowser().getTestResourcesPath(), filename);
    }

    /**
     * Import an office file in the wiki.
     *
     * @param fileName name of the file to import (the file should be located in test /resources/ folder)
     * @param splitByHeadings either the option splitByHeadings should be use or not
     * @return the result page
     */
    private ViewPage importFile(String testName, String fileName, boolean splitByHeadings)
    {
        ViewPage page = this.setup.gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), "WebHome"));
        CreatePagePage createPage = page.createPage();
        createPage.setType("office");
        createPage.clickCreate();

        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        File resourceFile = this.getResourceFile(fileName);
        officeImporterPage.setFile(resourceFile);
        officeImporterPage.setFilterStyle(true);
        officeImporterPage.setSplitDocument(splitByHeadings);

        return officeImporterPage.submit();
    }

    /**
     * Delete a page with all its children.
     *
     * @param pageToDelete the page to delete
     */
    private void deletePageWithChildren(ViewPage pageToDelete)
    {
        DeletePageConfirmationPage confirmationPage = pageToDelete.deletePage();
        if (confirmationPage.hasAffectChildrenOption()) {
            confirmationPage.setAffectChildren(true);
        }
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        assertTrue(deletingPage.isSuccess());
    }

    /**
     * Delete the page created by the test.
     */
    private void deletePage(String testName)
    {
        DocumentReference pageToDelete =
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), "WebHome");
        this.setup.deletePage(pageToDelete);
    }
}

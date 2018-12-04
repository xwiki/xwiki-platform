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
package ui;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.test.po.OfficeImporterPage;
import org.xwiki.officeimporter.test.po.OfficeImporterResultPage;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletEngine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for the office importer
 * 
 * @version $Id$
 * @since 7.3M1
 */
@UITest(office = true, servletEngine = ServletEngine.JETTY, verbose = true)
public class OfficeImporterIT
{

    @BeforeEach
    public void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
        // Connect the wiki to the office server if it is not already done
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        administrationPage.clickSection("Content", "Office Server");
        OfficeServerAdministrationSectionPage officeServerAdministrationSectionPage =
            new OfficeServerAdministrationSectionPage();
        if (!"Connected".equals(officeServerAdministrationSectionPage.getServerState())) {
            officeServerAdministrationSectionPage.startServer();
            assertEquals("Connected", officeServerAdministrationSectionPage.getServerState());
        }
    }

    /**
     * Import an office file in the wiki.
     * 
     * @param fileName name of the file to import (the file should be located in test /resources/ folder)
     * @return the result page
     */
    private ViewPage importFile(TestUtils testUtils, String testName, String fileName)
    {
        return importFile(testUtils, testName, fileName, false);
    }

    /**
     * Import an office file in the wiki.
     * 
     * @param fileName name of the file to import (the file should be located in test /resources/ folder)
     * @param splitByHeadings either the option splitByHeadings should be use or not
     * @return the result page
     */
    private ViewPage importFile(TestUtils testUtils, String testName, String fileName, boolean splitByHeadings)
    {
        ViewPage page = testUtils.gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), "WebHome"));
        CreatePagePage createPage = page.createPage();
        createPage.setType("office");
        createPage.clickCreate();

        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        officeImporterPage.setFile(new File(getClass().getResource(fileName).getPath()));
        officeImporterPage.setFilterStyle(true);
        officeImporterPage.setSplitDocument(splitByHeadings);

        OfficeImporterResultPage officeImporterResultPage = officeImporterPage.clickImport();
        assertEquals("Conversion succeeded. You can view the result, or you can Go back to convert another document.",
            officeImporterResultPage.getMessage());
        return officeImporterResultPage.viewResult();
    }

    /**
     * Delete a page with all its children.
     * 
     * @param pageToDelete the page to delete
     */
    private void deletePageWithChildren(ViewPage pageToDelete)
    {
        ConfirmationPage confirmationPage = pageToDelete.delete();
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
    private void deletePage(TestUtils testUtils, String testName)
    {
        DocumentReference pageToDelete =
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), "WebHome");
        testUtils.deletePage(pageToDelete);
    }

    /**
     * A basic test that imports some documents and verify they are correctly imported TODO: do a more advanced check
     * about styling and content
     */
    @Test
    public void testImports(TestUtils testUtils)
    {
        String testName = "testImports";
        // Test word file
        ViewPage resultPage = importFile(testUtils, testName, "/msoffice.97-2003/Test.doc");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage(testUtils, testName);

        // Test power point file
        resultPage = importFile(testUtils, testName, "/msoffice.97-2003/Test.ppt");
        AttachmentsPane attachmentsPane = resultPage.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(testUtils, testName);

        // Test excel file
        resultPage = importFile(testUtils, testName, "/msoffice.97-2003/Test.xls");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage(testUtils, testName);

        // Test ODT file
        resultPage = importFile(testUtils, testName, "/ooffice.3.0/Test.odt");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage(testUtils, testName);

        // Test ODP file
        resultPage = importFile(testUtils, testName, "/ooffice.3.0/Test.odp");
        attachmentsPane = resultPage.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(testUtils, testName);

        // Test ODS file
        resultPage = importFile(testUtils, testName, "/ooffice.3.0/Test.ods");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage(testUtils, testName);
    }

    /**
     * Test if the expected child exists at the expected place (as a children of the target page).
     */
    private void testChild(TestUtils testUtils, String testName, String expectedName, String expectedContent)
    {
        ViewPage child = testUtils.gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName(), testName), expectedName));
        assertTrue(child.exists());
        assertEquals(expectedName, child.getDocumentTitle());
        assertTrue(StringUtils.contains(child.getContent(), expectedContent));
    }

    @Test
    public void testSplitByHeadings(TestUtils testUtils)
    {
        String testName = "testSplitByHeadings";
        ViewPage resultPage = importFile(testUtils, testName, "/ToSplit.odt", true);
        assertTrue(StringUtils.contains(resultPage.getContent(), "Introduction"));

        // See children
        testChild(testUtils, testName, "First Part", "Hello, this is the first part of my story!");
        testChild(testUtils, testName, "Second Part", "This is the second part of my story!");
        testChild(testUtils, testName, "Last Part", "It's finished. Thanks you!");

        // Go back to the parent
        resultPage = testUtils.gotoPage(new DocumentReference("xwiki", getClass().getSimpleName(), testName));
        deletePageWithChildren(resultPage);
    }

    /**
     * Depending on if the target page is terminal or not, the "childNamingMethod" input is displayed or not.
     */
    @Test
    public void testChildNamingMethodInputVisibility(TestUtils testUtils)
    {
        DocumentReference testDocument =
            new DocumentReference("xwiki", Arrays.asList(getClass().getSimpleName()),
                "testChildNamingMethodInputVisibility");

        // Cleaning
        testUtils.deletePage(testDocument);

        // 1: create a terminal page
        CreatePagePage createPagePage = testUtils.gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(true);
        createPagePage.clickCreate();
        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        // Test
        assertTrue(officeImporterPage.isChildPagesNamingMethodDisplayed());

        // 2: create a non terminal page
        createPagePage = testUtils.gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(false);
        createPagePage.clickCreate();
        officeImporterPage = new OfficeImporterPage();
        // Test
        assertFalse(officeImporterPage.isChildPagesNamingMethodDisplayed());
    }
}

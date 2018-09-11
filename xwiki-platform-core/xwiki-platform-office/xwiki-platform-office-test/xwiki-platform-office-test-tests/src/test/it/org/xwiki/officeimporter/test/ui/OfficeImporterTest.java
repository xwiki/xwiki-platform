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

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.test.po.OfficeImporterPage;
import org.xwiki.officeimporter.test.po.OfficeImporterResultPage;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.attachment.test.po.AttachmentsPane;
import org.xwiki.attachment.test.po.PageWithAttachmentPane;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;

/**
 * Functional tests for the office importer
 * 
 * @version $Id$
 * @since 7.3M1
 */
public class OfficeImporterTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Before
    public void setUp()
    {
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
    private ViewPage importFile(String fileName)
    {
        return importFile(fileName, false);
    }

    /**
     * Import an office file in the wiki.
     * 
     * @param fileName name of the file to import (the file should be located in test /resources/ folder)
     * @param splitByHeadings either the option splitByHeadings should be use or not
     * @return the result page
     */
    private ViewPage importFile(String fileName, boolean splitByHeadings)
    {
        ViewPage page = getUtil().gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getTestClassName(), getTestMethodName()), "WebHome"));
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
        deletingPage.waitUntilIsTerminated();
        assertTrue(deletingPage.isSuccess());
    }

    /**
     * Delete the page created by the test.
     */
    private void deletePage()
    {
        DocumentReference pageToDelete =
            new DocumentReference("xwiki", Arrays.asList(getTestClassName(), getTestMethodName()), "WebHome");
        getUtil().deletePage(pageToDelete);
    }

    /**
     * A basic test that imports some documents and verify they are correctly imported TODO: do a more advanced check
     * about styling and content
     */
    @Test
    public void testImports()
    {
        // Test word file
        ViewPage resultPage = importFile("/msoffice.97-2003/Test.doc");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage();

        // Test power point file
        importFile("/msoffice.97-2003/Test.ppt");
        PageWithAttachmentPane page = new PageWithAttachmentPane();
        AttachmentsPane attachmentsPane = page.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage();

        // Test excel file
        resultPage = importFile("/msoffice.97-2003/Test.xls");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage();

        // Test ODT file
        resultPage = importFile("/ooffice.3.0/Test.odt");
        assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage();

        // Test ODP file
        importFile("/ooffice.3.0/Test.odp");
        page = new PageWithAttachmentPane();
        attachmentsPane = page.openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage();

        // Test ODS file
        resultPage = importFile("/ooffice.3.0/Test.ods");
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet1"));
        assertTrue(StringUtils.contains(resultPage.getContent(), "Sheet2"));
        deletePage();
    }

    /**
     * Test if the expected child exists at the expected place (as a children of the target page).
     */
    private void testChild(String expectedName, String expectedContent)
    {
        ViewPage child = getUtil().gotoPage(
            new DocumentReference("xwiki", Arrays.asList(getTestClassName(), getTestMethodName()), expectedName));
        assertTrue(child.exists());
        assertEquals(expectedName, child.getDocumentTitle());
        assertTrue(StringUtils.contains(child.getContent(), expectedContent));
    }

    @Test
    public void testSplitByHeadings()
    {
        ViewPage resultPage = importFile("/ToSplit.odt", true);
        assertTrue(StringUtils.contains(resultPage.getContent(), "Introduction"));

        // See children
        testChild("First Part", "Hello, this is the first part of my story!");
        testChild("Second Part", "This is the second part of my story!");
        testChild("Last Part", "It's finished. Thanks you!");

        // Go back to the parent
        resultPage = getUtil().gotoPage(new DocumentReference("xwiki", getTestClassName(), getTestMethodName()));
        deletePageWithChildren(resultPage);
    }

    /**
     * Depending on if the target page is terminal or not, the "childNamingMethod" input is displayed or not.
     */
    @Test
    public void testChildNamingMethodInputVisibility()
    {
        DocumentReference testDocument =
            new DocumentReference("xwiki", Arrays.asList(getTestClassName()), getTestMethodName());

        // Cleaning
        getUtil().deletePage(testDocument);

        // 1: create a terminal page
        CreatePagePage createPagePage = getUtil().gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(true);
        createPagePage.clickCreate();
        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        // Test
        assertTrue(officeImporterPage.isChildPagesNamingMethodDisplayed());

        // 2: create a non terminal page
        createPagePage = getUtil().gotoPage(testDocument).createPage();
        createPagePage.setType("office");
        createPagePage.setTerminalPage(false);
        createPagePage.clickCreate();
        officeImporterPage = new OfficeImporterPage();
        // Test
        assertFalse(officeImporterPage.isChildPagesNamingMethodDisplayed());
    }
}

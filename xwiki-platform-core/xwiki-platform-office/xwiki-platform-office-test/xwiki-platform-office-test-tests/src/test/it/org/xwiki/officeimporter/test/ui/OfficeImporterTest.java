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
import org.junit.Assert;
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
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Functional test for the office importer
 *  
 * @version $Id$
 * @since 7.2
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
        administrationPage.clickSection("Configuration", "Office Server");
        OfficeServerAdministrationSectionPage officeServerAdministrationSectionPage
                = new OfficeServerAdministrationSectionPage();
        if (!"Connected".equals(officeServerAdministrationSectionPage.getServerState())) {
            officeServerAdministrationSectionPage.startServer();
            Assert.assertEquals("Connected", officeServerAdministrationSectionPage.getServerState());
        }
    }
    
    private ViewPage importFile(String fileName)
    {
        return importFile(fileName, false);
    }

    private ViewPage importFile(String fileName, boolean splitByHeadings)
    {
        ViewPage page = getUtil().gotoPage(new DocumentReference("xwiki", 
                Arrays.asList(getTestClassName(), getTestMethodName()), "WebHome"));
        CreatePagePage createPage = page.createPage();
        createPage.setType("office");
        createPage.clickCreate();

        OfficeImporterPage officeImporterPage = new OfficeImporterPage();
        officeImporterPage.setFile(new File(getClass().getResource(fileName).getPath()));
        officeImporterPage.setFilterStyle(true);
        officeImporterPage.setSplitDocument(splitByHeadings);
        
        OfficeImporterResultPage officeImporterResultPage = officeImporterPage.clickImport();
        Assert.assertEquals(
                "Conversion succeeded. You can view the result, or you can Go back to convert another document.",
                officeImporterResultPage.getMessage());
        return officeImporterResultPage.viewResult();
    }
    
    private void deletePage(ViewPage pageToDelete)
    {
        ConfirmationPage confirmationPage = pageToDelete.delete();
        if (confirmationPage.hasAffectChildrenOption()) {
            confirmationPage.setAffectChildren(true);
        }
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilIsTerminated();
        Assert.assertTrue(deletingPage.isSuccess());
    }

    /**
     * A basic test that imports some documents and verify they are correctly imported
     * TODO: do a more advanced check about styling and content
     */
    @Test
    public void testImports()
    {
        // Test word file
        ViewPage resultPage = importFile("/msoffice.97-2003/Test.doc");
        Assert.assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage(resultPage);

        // Test power point file
        resultPage = importFile("/msoffice.97-2003/Test.ppt");
        AttachmentsPane attachmentsPane = resultPage.openAttachmentsDocExtraPane();
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(resultPage);
        
        // Test excel file
        resultPage = importFile("/msoffice.97-2003/Test.xls");
        Assert.assertTrue(StringUtils.contains(resultPage.getContent(), "Résumé"));
        deletePage(resultPage);

        // Test ODT file
        resultPage = importFile("/ooffice.3.0/Test.odt");
        Assert.assertTrue(StringUtils.contains(resultPage.getContent(), "This is a test document."));
        deletePage(resultPage);

        // Test ODP file
        resultPage = importFile("/ooffice.3.0/Test.odp");
        attachmentsPane = resultPage.openAttachmentsDocExtraPane();
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide0.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide1.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide2.jpg"));
        Assert.assertTrue(attachmentsPane.attachmentExistsByFileName("Test-slide3.jpg"));
        deletePage(resultPage);

        // Test ODS file
        resultPage = importFile("/ooffice.3.0/Test.ods");
        Assert.assertTrue(StringUtils.contains(resultPage.getContent(), "Résumé"));
        deletePage(resultPage);
    }
    
    private void testChild(String expectedName, String expectedContent)
    {
        ViewPage child = getUtil().gotoPage(new DocumentReference("xwiki", 
                Arrays.asList(getTestClassName(), getTestMethodName()), expectedName));
        Assert.assertTrue(child.exists());
        Assert.assertEquals(expectedName, child.getDocumentTitle());
        Assert.assertTrue(StringUtils.contains(child.getContent(), expectedContent));
    }
    
    @Test
    public void testSplitByHeadings()
    {
        ViewPage resultPage = importFile("/ToSplit.odt", true);
        Assert.assertTrue(StringUtils.contains(resultPage.getContent(), "Introduction"));
        
        // See children      
        testChild("First Part", "Hello, this is the first part of my story!");
        testChild("Second Part", "This is the second part of my story!");
        testChild("Last Part", "It's finished. Thanks you!");
        
        // Go back to the parent
        resultPage = getUtil().gotoPage(new DocumentReference("xwiki", getTestClassName(), getTestMethodName()));
        deletePage(resultPage);
    }
}

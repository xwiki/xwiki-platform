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
package org.xwiki.index.test.ui.docker;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.index.test.po.AllDocsLiveData;
import org.xwiki.index.test.po.AllDocsPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.CopyPage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.EditRightsPane;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.editor.RightsEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the AllDocs page.
 *
 * @version $Id$
 * @since 11.4RC1
 */
@UITest
class AllDocsIT
{
    private static final String LOCATION_COLUMN_LABEL = "Location";

    private static final String TYPE_COLUMN_LABEL = "Type";

    private static final String NAME_COLUMN_LABEL = "Name";

    private static final String SIZE_COLUMN_LABEL = "Size";

    private static final String DATE_COLUMN_LABEL = "Date";

    private static final String AUTHOR_COLUMN_LABEL = "Author";

    @Test
    @Order(1)
    void verifyAllDocs(TestUtils setup, TestInfo testInfo, TestReference testReference)
    {
        // Fixture
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());

        // tests
        validateActionsAndGuest(setup);
        validateFilterDoc(testReference);
        validateCopyLink(setup, testInfo, testReference);
        validateRenameLink(setup, testInfo, testReference);
        validateDeleteLink(setup, testReference);
        validateRightLink(setup, testReference);
    }

    /**
     * Test attachment listing, filtering and sorting.
     * <p>
     * This test is against XWiki Enterprise XE-701 https://jira.xwiki.org/browse/XE-701 (fixed in 2.5M1).
     */
    @Test
    @Order(2)
    void attachmentsTabFilteringAndSorting(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create 2 pages with attachments so that this test filter returns only one.
        // Note that we need to be logged in.
        String className = testReference.getLastSpaceReference().getName();
        setup.createPageWithAttachment(className, "Page", null, null, "attachment1.txt",
            new ByteArrayInputStream("attachment content1".getBytes()), TestUtils.SUPER_ADMIN_CREDENTIALS);
        setup.createPageWithAttachment(className, "OtherPage", null, null, "attachment2.txt",
            new ByteArrayInputStream("attachment content2".getBytes()), TestUtils.SUPER_ADMIN_CREDENTIALS);

        AllDocsPage docsPage = AllDocsPage.gotoPage();
        TableLayoutElement liveData = docsPage.clickAttachmentsTab().getTableLayout();

        // Here we test if all the Columns are displayed.
        assertTrue(liveData.hasColumn(TYPE_COLUMN_LABEL), "No Type column found");
        assertTrue(liveData.hasColumn(NAME_COLUMN_LABEL), "No Name column found");
        assertTrue(liveData.hasColumn(LOCATION_COLUMN_LABEL), "No Location column found");
        assertTrue(liveData.hasColumn(SIZE_COLUMN_LABEL), "No Size column found");
        assertTrue(liveData.hasColumn(DATE_COLUMN_LABEL), "No Date column found");
        assertTrue(liveData.hasColumn(AUTHOR_COLUMN_LABEL), "No Author column found");

        String defaultLocationFilter = className + ".";

        liveData.filterColumn(LOCATION_COLUMN_LABEL, defaultLocationFilter);

        assertEquals(2, liveData.countRows());

        // Filter by attachment file name.
        liveData.filterColumn(NAME_COLUMN_LABEL, "t1");
        assertEquals(1, liveData.countRows());
        assertEquals("attachment1.txt", liveData.getCell(NAME_COLUMN_LABEL, 1).getText());

        // Clear the filter.
        liveData.filterColumn(NAME_COLUMN_LABEL, "");

        // Filter by attachment location.
        liveData.filterColumn(LOCATION_COLUMN_LABEL, defaultLocationFilter + "Oth");
        assertEquals(1, liveData.countRows());
        assertEquals(className + "OtherPage", liveData.getCell(LOCATION_COLUMN_LABEL, 1).getText());

        // Reset the filter.
        liveData.filterColumn(LOCATION_COLUMN_LABEL, defaultLocationFilter);

        // Sort by attachment file name. The live table should be already sorted by file name ascending. This will
        // reverse the order.
        assertEquals("attachment2.txt", liveData.getCell(NAME_COLUMN_LABEL, 2).getText());
        liveData.sortBy(NAME_COLUMN_LABEL);
        assertEquals(2, liveData.countRows());
        assertEquals("attachment2.txt", liveData.getCell(NAME_COLUMN_LABEL, 1).getText());

        // Sort by attachment location.
        liveData.sortBy(LOCATION_COLUMN_LABEL);
        assertEquals(className + "Page", liveData.getCell(LOCATION_COLUMN_LABEL, 2).getText());
        liveData.sortBy(LOCATION_COLUMN_LABEL);
        assertEquals(2, liveData.countRows());
        assertEquals(className + "Page", liveData.getCell(LOCATION_COLUMN_LABEL, 1).getText());
    }

    /**
     * Verify that the Action column is displayed only for logged in users.
     */
    private void validateActionsAndGuest(TestUtils setup)
    {
        // Create a test user
        setup.createUserAndLogin("Foobar", "password");
        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        assertTrue(livetable.getTableLayout().hasColumn("Actions"), "No Actions column found");

        // Logs out to be guest to verify that the Action columns is no longer displayed
        setup.forceGuestUser();

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        assertFalse(livetable.getTableLayout().hasColumn("Actions"), "Actions column shouldn't be visible for guests");

        setup.loginAsSuperAdmin();
    }

    /**
     * Verify filtering works by filtering on the document name
     */
    private void validateFilterDoc(TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        // We get one result for the page we've created
        TableLayoutElement tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("Title", testName);

        // We get no result for a page created by user barbaz
        livetable.filterColumn("Last Author", "barbaz");
        assertEquals(0, tableLayout.countRows());
    }

    /**
     * Verify links for Copy / Rename / Delete and Rights link actions.
     */
    private void validateCopyLink(TestUtils setup, TestInfo testInfo, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        String copyPageName = testName + "Copy";
        String testSpace = testInfo.getTestClass().get().getSimpleName();
        DocumentReference copyPageReference = new DocumentReference("xwiki",
            Arrays.asList(testSpace, copyPageName), "WebHome");
        setup.deletePage(copyPageReference);

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        // We get one result for the page we've created
        TableLayoutElement tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("Title", testName);

        // click on the copy action link.
        livetable.clickAction(1, "copy");
        CopyPage copyPage = new CopyPage();
        copyPage.setTargetSpaceName(testSpace);
        copyPage.setTargetPageName(copyPageName);
        CopyOrRenameOrDeleteStatusPage statusPage = copyPage.clickCopyButton();
        statusPage.waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        tableLayout = livetable.getTableLayout();
        assertEquals(2, tableLayout.countRows());
        livetable.filterColumn(LOCATION_COLUMN_LABEL, copyPageName);
        assertEquals(1, tableLayout.countRows());

        setup.deletePage(copyPageReference);
    }

    private void validateRenameLink(TestUtils setup, TestInfo testInfo, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        String renamedPageName = testName + "Renamed";
        String testSpace = testInfo.getTestClass().get().getSimpleName();
        DocumentReference renamedPageReference = new DocumentReference("xwiki",
            Arrays.asList(testSpace, renamedPageName), "WebHome");

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        // We get one result for the page we've created
        TableLayoutElement tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("Title", testName);

        // click on the copy action link.
        livetable.clickAction(1, "rename");
        RenamePage renamePage = new RenamePage();
        renamePage.getDocumentPicker().setName(renamedPageName);

        CopyOrRenameOrDeleteStatusPage statusPage = renamePage.clickRenameButton();
        statusPage.waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        assertEquals(testSpace + renamedPageName, tableLayout.getCell(LOCATION_COLUMN_LABEL, 1).getText());

        setup.deletePage(renamedPageReference);
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());
    }

    private void validateDeleteLink(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        // We get one result for the page we've created
        TableLayoutElement tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("Title", testName);

        // click on the copy action link.
        livetable.clickAction(1, "delete");
        ConfirmationPage confirmationPage = new ConfirmationPage();
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        assertEquals(0, livetable.getTableLayout().countRows());
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());
    }

    private void validateRightLink(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLiveData livetable = page.clickIndexTab();
        livetable.filterColumn(LOCATION_COLUMN_LABEL, testName);
        // We get one result for the page we've created
        TableLayoutElement tableLayout = livetable.getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("Title", testName);

        // click on the copy action link.
        livetable.clickAction(1, "rights");

        RightsEditPage rightsEditPage = new RightsEditPage();
        rightsEditPage.switchToUsers();
        assertEquals(EditRightsPane.State.NONE, rightsEditPage.getGuestRight(EditRightsPane.Right.DELETE));
    }
}

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
package org.xwiki.index.test.ui;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.index.test.po.AllDocsLivetable;
import org.xwiki.index.test.po.AllDocsPage;
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
public class AllDocsIT
{
    @Test
    public void verifyAllDocs(TestUtils setup, TestInfo testInfo, TestReference testReference)
    {
        // Fixture
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());

        // tests
        validateActionsAndGuest(setup);
        validateFilterDoc(setup, testReference);
        validateCopyLink(setup, testInfo,  testReference);
        validateRenameLink(setup, testInfo, testReference);
        validateDeleteLink(setup, testReference);
        validateRightLink(setup, testReference);
    }

    /**
     * Verify that the Action column is displayed only for logged in users.
     */
    private void validateActionsAndGuest(TestUtils setup)
    {
        // Create a test user
        setup.createUserAndLogin("Foobar", "password");
        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLivetable livetable = page.clickIndexTab();
        assertTrue(livetable.hasColumn("Actions"), "No Actions column found");

        // Logs out to be guest to verify that the Action columns is no longer displayed
        setup.forceGuestUser();

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        assertFalse(livetable.hasColumn("Actions"), "Actions column shouldn't be visible for guests");

        setup.loginAsSuperAdmin();
    }

    /**
     * Verify filtering works by filtering on the document name
     */
    private void validateFilterDoc(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();
        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLivetable livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        // We get one result for the page we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testName));

        // We get no result for a page created by user barbaz
        livetable.filterColumn(4, "barbaz");
        assertEquals(0, livetable.getRowCount());
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
        AllDocsLivetable livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        // We get one result for the page we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testName));

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
        livetable.filterColumn(2, testName);
        assertEquals(2, livetable.getRowCount());
        livetable.filterColumn(2, copyPageName);
        assertEquals(1, livetable.getRowCount());

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
        AllDocsLivetable livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        // We get one result for the page we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testName));

        // click on the copy action link.
        livetable.clickAction(1, "rename");
        RenamePage renamePage = new RenamePage();
        renamePage.getDocumentPicker().setName(renamedPageName);

        CopyOrRenameOrDeleteStatusPage statusPage = renamePage.clickRenameButton();
        statusPage.waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());

        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        assertEquals(1, livetable.getRowCount());
        assertEquals(testSpace+renamedPageName, livetable.getCell(1, 2).getText());

        setup.deletePage(renamedPageReference);
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());
    }

    private void validateDeleteLink(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLivetable livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        // We get one result for the page we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testName));

        // click on the copy action link.
        livetable.clickAction(1, "delete");
        ConfirmationPage confirmationPage = new ConfirmationPage();
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        page = AllDocsPage.gotoPage();
        livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        assertEquals(0, livetable.getRowCount());
        setup.createPage(testReference, "", testReference.getLastSpaceReference().getName());
    }

    private void validateRightLink(TestUtils setup, TestReference testReference)
    {
        String testName = testReference.getLastSpaceReference().getName();

        AllDocsPage page = AllDocsPage.gotoPage();
        AllDocsLivetable livetable = page.clickIndexTab();
        livetable.filterColumn(2, testName);
        // We get one result for the page we've created
        assertEquals(1, livetable.getRowCount());
        assertTrue(livetable.hasRow("Title", testName));

        // click on the copy action link.
        livetable.clickAction(1, "rights");

        RightsEditPage rightsEditPage = new RightsEditPage();
        rightsEditPage.switchToUsers();
        assertEquals(EditRightsPane.State.NONE, rightsEditPage.getGuestRight(EditRightsPane.Right.ADMIN));
    }
}

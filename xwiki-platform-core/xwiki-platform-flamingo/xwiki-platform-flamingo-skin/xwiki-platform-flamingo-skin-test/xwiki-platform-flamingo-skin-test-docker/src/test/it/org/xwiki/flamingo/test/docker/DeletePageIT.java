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
package org.xwiki.flamingo.test.docker;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.flamingo.skin.test.po.JobQuestionPane;
import org.xwiki.flamingo.skin.test.po.RestoreStatusPage;
import org.xwiki.flamingo.skin.test.po.UndeletePage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.XWikiWebDriver;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.DeletePageConfirmationPage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.DeletedPageEntry;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests the Delete Page feature.
 *
 * @version $Id$
 * @since 3.0M3
 */
@UITest
class DeletePageIT
{
    private static final DocumentReference REFACTORING_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", Arrays.asList("Refactoring", "Code"), "RefactoringConfiguration");

    private ViewPage viewPage;

    private static final String LOGGED_USERNAME = "superadmin";

    private static final String DOCUMENT_NOT_FOUND = "The requested page could not be found.";

    private static final String DELETE_ACTION = "delete";

    private static final String PAGE_CONTENT = "This page is used for testing delete functionality";

    private static final String PAGE_TITLE = "Page title that will be deleted";

    private static final String DELETE_SUCCESSFUL = "Done.";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        // Create a new Page that will be deleted
        this.viewPage = setup.createPage(testReference, PAGE_CONTENT, PAGE_TITLE);
    }

    @AfterEach
    void tearDown(TestUtils setup) throws Exception
    {
        // we play with multilingual in some tests, ensure to set it back to default values.
        setup.setWikiPreference("multilingual", "false");
        setup.setWikiPreference("default_language", "en");
    }

    @Test
    @Order(1)
    void deleteOkWhenConfirming()
    {
        ConfirmationPage confirmationPage = this.viewPage.deletePage();
        // This tests for regression of XWIKI-1388
        assertNotNull(confirmationPage.getCurrentUser(),
            "The interface should not show the user as logged out while deleting page");
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        // Note: it's better to wait instead of using isSuccess() since there could be some timeframe between
        // the hiding of the progress UI and the display of the success message.
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();

        List<DeletedPageEntry> deletedPagesEntries = deleteOutcome.getDeletedPagesEntries();
        assertEquals(1, deletedPagesEntries.size());
        assertEquals(LOGGED_USERNAME, deletedPagesEntries.get(0).getDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
    }

    /**
     * Verify that we can delete a page without showing the confirmation dialog box and that we can redirect to any page
     * we want when the delete is done.
     */
    @Test
    @Order(2)
    void deletePageCanSkipConfirmationAndDoARedirect(TestUtils setup, TestReference testReference)
    {
        DocumentReference documentReference = new DocumentReference("Whatever", testReference.getLastSpaceReference());
        String pageURL = setup.getURL(documentReference);
        setup.gotoPage(testReference, DELETE_ACTION, "confirm=1&xredirect=" + pageURL);
        ViewPage vp = new ViewPage();
        // Since the page PAGE_VALUE + "Whatever" doesn't exist the View Action will redirect to the Nested Document
        // SPACE_VALUE + "." + PAGE_VALUE + "Whatever + ".WebHome".
        assertEquals(documentReference.toString(),
            String.format("xwiki:%s", vp.getMetaDataValue("space")));
        assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    /**
     * Verify that we can skip the default delete result page and instead redirect to any page we want.
     */
    @Test
    @Order(3)
    void deletePageCanDoRedirect(TestUtils setup, TestReference testReference)
    {
        DocumentReference documentReference = new DocumentReference("Whatever", testReference.getLastSpaceReference());
        String pageURL = setup.getURL(documentReference);
        setup.gotoPage(testReference, DELETE_ACTION, "xredirect=" + pageURL);
        ConfirmationPage confirmation = new ConfirmationPage();
        confirmation.clickYes();
        ViewPage vp = new ViewPage();
        // Since the page PAGE_VALUE + "Whatever" doesn't exist the View Action will redirect to the Nested Document
        // SPACE_VALUE + "." + PAGE_VALUE + "Whatever + ".WebHome".
        assertEquals(documentReference.toString(),
            String.format("xwiki:%s", vp.getMetaDataValue("space")));
        assertEquals("WebHome", vp.getMetaDataValue("page"));
    }

    /**
     * Verify that hitting cancel on the delete confirmation dialog box goes back to the page being deleted.
     */
    @Test
    @Order(4)
    void deletePageGoesToOriginalPageWhenCancelled(TestUtils setup, XWikiWebDriver driver, TestReference testReference)
    {
        this.viewPage.deletePage().clickNo();
        assertEquals(setup.getURL(testReference), driver.getCurrentUrl() + "WebHome");
    }

    @Test
    @Order(5)
    void deletePageIsImpossibleWhenNoDeleteRights()
    {
        // Logs out to be guest and not have the right to delete
        this.viewPage.logout();
        assertFalse(this.viewPage.canDelete());
    }

    /**
     * Verify that when you delete a terminal and a non terminal page sharing the same location, both deleted versions
     * are present in the recycle bin list when you hit the location afterwards.
     *
     * @see: "XWIKI-12563: Cannot restore a terminal page from its location"
     * @since 7.2RC1
     */
    @Test
    @Order(6)
    void deleteTerminalAndNonTerminalPages(TestUtils setup, TestReference reference)
    {
        SpaceReference lastSpaceReference = reference.getLastSpaceReference();
        // We use a slightly different test reference name to ensure the page is not created yet.
        String referenceName = lastSpaceReference.getName() + "1";
        DocumentReference nonTerminalPageRef = new DocumentReference("WebHome",
            new SpaceReference(referenceName, lastSpaceReference.getParent()));
        DocumentReference terminalPageRef = new DocumentReference(referenceName,
            (SpaceReference) lastSpaceReference.getParent());

        // Create the terminal page.
        ViewPage terminalPage = setup.createPage(terminalPageRef, "Content", "Title");
        // Delete it
        terminalPage.deletePage().clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();

        // Look at the recycle bin
        DeletePageOutcomePage deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();
        assertTrue(deletePageOutcomePage.getDeletedPagesEntries().isEmpty());
        assertEquals(1, deletePageOutcomePage.getDeletedTerminalPagesEntries().size());

        // Create the non terminal page.
        ViewPage nonTerminalPage = setup.createPage(nonTerminalPageRef, "Content", "Title");
        // Delete it
        nonTerminalPage.deletePage().clickYes();
        deletingPage.waitUntilFinished();

        // Look at the recycle bin
        deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();

        List<DeletedPageEntry> deletedTerminalPagesEntries = deletePageOutcomePage.getDeletedTerminalPagesEntries();
        List<DeletedPageEntry> deletedPagesEntries = deletePageOutcomePage.getDeletedPagesEntries();

        assertEquals(1, deletedPagesEntries.size());
        assertEquals(1, deletedTerminalPagesEntries.size());

        // Delete both version in the recycle bin
        deletePageOutcomePage = deletedTerminalPagesEntries.get(0).clickDelete();
        deletedPagesEntries = deletePageOutcomePage.getDeletedPagesEntries();
        deletedPagesEntries.get(0).clickDelete();
    }

    /**
     * Test that when you delete a page and you select "affect children", it delete children properly. It also test the
     * opposite. Further, this tests the batch restore.
     *
     * @since 7.2RC1
     */
    @Test
    @Order(7)
    void deleteChildren(TestUtils setup, TestReference parentReference, TestInfo info)
    {
        // Initialize the parent
        ViewPage parentPage = setup.createPage(parentReference, "Content", "Parent");

        // Test 1: Try to delete it to make sure we don't have the "affect children" option yet
        DeletePageConfirmationPage confirmationPage = parentPage.deletePage();
        assertFalse(confirmationPage.hasAffectChildrenOption());

        // Initialize the children pages
        final int nbChildren = 3;
        DocumentReference[] childrenReferences = new DocumentReference[nbChildren];
        assertTrue(info.getTestClass().isPresent());
        assertTrue(info.getTestMethod().isPresent());
        for (int i = 0; i < nbChildren; ++i) {
            childrenReferences[i] = new DocumentReference("xwiki",
                Arrays.asList(info.getTestClass().get().getSimpleName(), info.getTestMethod().get().getName(),
                    "Child_" + (i + 1)), "WebHome");
            setup.createPage(childrenReferences[i], "Content", "Child " + (i + 1));
        }

        // Test 2: when you don't select "affect children", the children are not deleted
        parentPage = setup.gotoPage(parentReference);
        confirmationPage = parentPage.deletePage();
        assertTrue(confirmationPage.hasAffectChildrenOption());
        confirmationPage.setAffectChildren(false);
        confirmationPage.openAffectChildrenPanel();
        TableLayoutElement affectChildrenLiveData = new LiveDataElement("deleteSpaceIndex").getTableLayout();
        assertEquals(nbChildren, affectChildrenLiveData.countRows());
        for (int i = 1; i <= nbChildren; i++) {
            SpaceReference childSpaceReference = childrenReferences[i - 1].getLastSpaceReference();
            affectChildrenLiveData.assertCellWithLink("Title", String.format("Child %d", i),
                setup.getURL(childSpaceReference));
            affectChildrenLiveData.assertCellWithLink("Location", String.format("Child_%d", i),
                setup.getURL(childSpaceReference));
            affectChildrenLiveData.assertRow("Date", hasItem(affectChildrenLiveData.getDatePatternMatcher()));
            affectChildrenLiveData.assertCellWithLink("Last Author", "superadmin",
                setup.getURL(new DocumentReference("xwiki", "XWiki", "superadmin")));
        }

        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        // Check the page have been effectively removed
        ViewPage page = setup.gotoPage(parentReference);
        assertFalse(page.exists());
        // But not the children 
        for (int i = 0; i < nbChildren; ++i) {
            page = setup.gotoPage(childrenReferences[i]);
            assertTrue(page.exists());
        }

        // Test 3: when you select "affect children", the children are deleted too
        parentPage = setup.createPage(parentReference, "Content", "Parent");
        confirmationPage = parentPage.deletePage();
        assertTrue(confirmationPage.hasAffectChildrenOption());
        confirmationPage.setAffectChildren(true);
        deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();
        // Check the page have been effectively removed
        page = setup.gotoPage(parentReference);
        assertFalse(page.exists());
        // And also the children
        for (int i = 0; i < nbChildren; ++i) {
            page = setup.gotoPage(childrenReferences[i]);
            assertFalse(page.exists());
        }

        // Test 4: test batch restore
        setup.gotoPage(parentReference);
        DeletePageOutcomePage outcomePage = new DeletePageOutcomePage();
        outcomePage.clickBatchLink();
        UndeletePage undeletePage = new UndeletePage();

        // Go back and forward again to test clicking the cancel link.
        outcomePage = undeletePage.clickCancel();
        outcomePage.clickBatchLink();
        undeletePage = new UndeletePage();

        assertTrue(undeletePage.hasBatch());
        undeletePage.setBatchIncluded(true);
        undeletePage.toggleBatchPanel();

        assertEquals("superadmin", undeletePage.getPageDeleter());
        String deletedBatchId = undeletePage.getDeletedBatchId();
        try {
            UUID deletedBatchUUID = UUID.fromString(deletedBatchId);
            assertNotNull(deletedBatchUUID);
        } catch (IllegalArgumentException e) {
            fail("Batch id is not a valid UUID: " + deletedBatchId);
        }

        TableLayoutElement liveDataTable = undeletePage.getDeletedBatchLiveData().getTableLayout();
        assertEquals(nbChildren + 1, liveDataTable.countRows());
        liveDataTable.assertRow(UndeletePage.LIVE_DATA_PAGE, "Parent");
        for (int i = 0; i < nbChildren; ++i) {
            liveDataTable.assertRow(UndeletePage.LIVE_DATA_PAGE, "Child " + (i + 1));
        }
        // Assert that we have at least some actions. Testing individual items is not so easy because the action URLs
        // are not what the LD page object expects.
        liveDataTable.assertRow(UndeletePage.LIVE_DATA_ACTIONS, "RestoreDelete");

        // Trigger the actual restore.
        RestoreStatusPage restoreStatusPage = undeletePage.clickRestore();
        restoreStatusPage.waitUntilFinished();
        assertEquals("Done.", restoreStatusPage.getInfoMessage());
        page = restoreStatusPage.gotoRestoredPage();

        // Check the page have been effectively restored.
        assertTrue(page.exists());
        // And also the children have been restored.
        for (int i = 0; i < nbChildren; ++i) {
            page = setup.gotoPage(childrenReferences[i]);
            assertTrue(page.exists());
        }
    }

    /**
     * This test check the behaviour of job questions when deleting a page with a used class - check that the question
     * UI is displayed in that case - check that we can select some files in the question to be deleted.
     * <p>
     * More behaviour of the questions are checked in RenamePageTest
     */
    @Test
    @Order(8)
    void deletePageWithUsedClass(TestUtils setup, TestInfo info)
    {
        // Create 4 pages under the same parent
        // 2 of them are free pages (WebHome and FreePage)
        // 1 of them is a class (ClassPage)
        // the last one contains an object property using the class

        String testClassName = info.getTestClass().get().getSimpleName();
        String testMethodName = info.getTestMethod().get().getName();
        List<String> parentSpaceReference = Arrays.asList(testClassName, testMethodName);
        DocumentReference parentReference = new DocumentReference("xwiki",
            parentSpaceReference,
            "WebHome");
        String space = testClassName + "." + testMethodName;
        String classPageName = "ClassPage";
        String objectPageName = "ObjectPage";
        String freePageName = "FreePage";

        DocumentReference classReference = new DocumentReference("xwiki", parentSpaceReference, classPageName);
        DocumentReference objectReference = new DocumentReference("xwiki", parentSpaceReference, objectPageName);
        DocumentReference freeReference = new DocumentReference("xwiki", parentSpaceReference, freePageName);

        setup.createPage(parentReference, "Some content", "Parent");
        setup.createPage(freeReference, "Some content", freePageName);
        setup.createPage(classReference, "Some content", classPageName);
        setup.createPage(objectReference, "Some content", objectPageName);

        setup.addClassProperty(space, classPageName, "Foo", "String");
        setup.addObject(objectReference, classReference.toString(), Collections.singletonMap("Foo", "Bar"));

        // Try to delete the parent page
        ViewPage parentPage = setup.gotoPage(parentReference);
        DeletePageConfirmationPage confirmationPage = parentPage.deletePage();
        confirmationPage.setAffectChildren(true);
        confirmationPage.confirmDeletePage();

        // At this point we should have the question job UI
        JobQuestionPane jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        assertFalse(jobQuestionPane.isEmpty());

        assertEquals("You are about to delete pages that contain used XClass.", jobQuestionPane.getQuestionTitle());
        TreeElement treeElement = jobQuestionPane.getQuestionTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();

        // there is two main nodes:
        //  1. to represent free pages
        //  2. to represent classes with associated objects
        assertEquals(2, topLevelNodes.size());
        TreeNodeElement freePages = topLevelNodes.get(0);
        assertEquals("Pages that do not contain any used XClass", freePages.getLabel());
        assertFalse(freePages.isSelected());

        freePages = freePages.open().waitForIt();
        List<TreeNodeElement> children = freePages.getChildren();

        // free pages should contain three nodes with alphabetical order:
        //   1. the FreePage obviously
        //   2. the ObjectPage since it does not contain a class
        //   3. the WebHome page
        assertEquals(3, children.size());

        TreeNodeElement freePage = children.get(0);
        assertEquals(freePageName, freePage.getLabel());
        assertEquals(space + "." + freePageName, freePage.getId());
        assertTrue(freePage.isLeaf());

        TreeNodeElement objectPage = children.get(1);
        assertEquals(objectPageName, objectPage.getLabel());
        assertEquals(space + "." + objectPageName, objectPage.getId());
        assertTrue(objectPage.isLeaf());

        TreeNodeElement webHome = children.get(2);

        // label = page title
        assertEquals("Parent", webHome.getLabel());
        assertEquals(space + ".WebHome", webHome.getId());
        assertTrue(webHome.isLeaf());

        TreeNodeElement classPage = topLevelNodes.get(1);
        assertEquals(classPageName, classPage.getLabel());
        assertEquals(space + "." + classPageName, classPage.getId());
        assertFalse(classPage.isSelected());

        classPage = classPage.open().waitForIt();
        children = classPage.getChildren();
        assertEquals(1, children.size());

        assertEquals(objectPageName, children.get(0).getLabel());

        // here it's an object
        assertEquals("object-" + space + "." + objectPageName, children.get(0).getId());
        assertTrue(children.get(0).isLeaf());

        freePage.select();
        CopyOrRenameOrDeleteStatusPage statusPage = jobQuestionPane.confirmQuestion();
        statusPage.waitUntilFinished();

        // check that the old one doesn't exist anymore
        ViewPage viewPage = setup.gotoPage(freeReference);
        assertFalse(viewPage.exists());
    }

    /**
     * This test checks the behaviour of the page deletion form when the user is allowed to choose between sending the
     * page to the recycle bin or delete it permanently.
     * In this test the user chooses to send the page to the recycle bin.
     */
    @Test
    @Order(9)
    void deleteToRecycleBin(TestUtils setup, TestReference testReference)
    {
        // Set the user type to Advanced
        Map<String, Object> userProperties = new HashMap<>();
        userProperties.put("usertype", "Advanced");
        setup.updateObject("XWiki", "superadmin", "XWiki.XWikiUsers", 0, userProperties);

        setup.gotoPage(testReference);
        DeletePageConfirmationPage confirmationPage = this.viewPage.deletePage();
        assertFalse(confirmationPage.isRecycleBinOptionsDisplayed());

        // Set the isRecycleBinSkippingActivated property to true, allowing advanced user to choose whether they want document to be
        // sent to the recycle bin or permanently removed.
        setup.updateObject(REFACTORING_CONFIGURATION_REFERENCE, "Refactoring.Code.RefactoringConfigurationClass", 0,
            "isRecycleBinSkippingActivated", "1");

        setup.gotoPage(testReference);
        confirmationPage = this.viewPage.deletePage();

        assertTrue(confirmationPage.isRecycleBinOptionsDisplayed());
        confirmationPage.selectOptionToRecycleBin();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        List<DeletedPageEntry> deletedPagesEntries = deleteOutcome.getDeletedPagesEntries();
        assertEquals(1, deletedPagesEntries.size());
        assertEquals(LOGGED_USERNAME, deletedPagesEntries.get(0).getDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
    }

    /**
     * This test checks the behaviour of the page deletion form when the user is allowed to choose between sending the
     * page to the recycle bin or delete it permanently.
     * In this test the user chooses to delete the page permanently.
     */
    @Test
    @Order(10)
    void deleteSkipRecycleBin()
    {
        DeletePageConfirmationPage confirmationPage = this.viewPage.deletePage();
        confirmationPage.selectOptionSkipRecycleBin();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
        ViewPage viewPage1 = new ViewPage();
        assertTrue(viewPage1.isNewDocument());
    }

    /**
     * This test is similar than {@link #deletePageWithUsedClass(TestUtils, TestInfo)} except that here we're checking
     * the deletion when deleting only the xclass page alone: by default it should not be selected, and should not
     * be deleted when confirming.
     */
    @Test
    @Order(11)
    void deletePageWithSingleUsedClass(TestUtils setup, TestInfo info)
    {
        // Create 2 pages in two locations:
        // first page is an xclass that we'll try to delete
        // second page will contain an xobject of the created xclass

        String testClassName = info.getTestClass().get().getSimpleName();
        String testMethodName = info.getTestMethod().get().getName();
        String xclassSpace = "XClassSpace";
        String xobjectSpace = "XObjectSpace";
        List<String> xclassSpaceReference = Arrays.asList(testClassName, testMethodName, xclassSpace);
        DocumentReference xclassReference = new DocumentReference("xwiki",
            xclassSpaceReference,
            "WebHome");
        List<String> xobjectSpaceReference = Arrays.asList(testClassName, testMethodName, xobjectSpace);
        DocumentReference xobjectReference = new DocumentReference("xwiki",
            xobjectSpaceReference,
            "WebHome");
        String space = testClassName + "." + testMethodName;
        String classPageName = "ClassPage";
        String objectPageName = "ObjectPage";

        DocumentReference classReference = new DocumentReference("xwiki", xclassSpaceReference, classPageName);
        DocumentReference objectReference = new DocumentReference("xwiki", xobjectSpaceReference, objectPageName);

        setup.createPage(classReference, "XClass page content", classPageName);
        setup.createPage(objectReference, "XObject page content", objectPageName);

        setup.addClassProperty(space, classPageName, "Foo", "String");
        setup.addObject(objectReference, classReference.toString(), Collections.singletonMap("Foo", "Bar"));

        // Try to delete the xclass page
        ViewPage parentPage = setup.gotoPage(classReference);
        DeletePageConfirmationPage confirmationPage = parentPage.deletePage();
        confirmationPage.confirmDeletePage();

        // At this point we should have the question job UI
        JobQuestionPane jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        assertFalse(jobQuestionPane.isEmpty());

        assertEquals("You are about to delete pages that contain used XClass.", jobQuestionPane.getQuestionTitle());
        TreeElement treeElement = jobQuestionPane.getQuestionTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();

        // there is a single node for the xclass:
        //  1. to represent free pages
        //  2. to represent classes with associated objects
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement classPage = topLevelNodes.get(0);
        assertEquals(classPageName, classPage.getLabel());
        assertEquals(String.format("%s.%s.%s", space, xclassSpace, classPageName), classPage.getId());
        assertFalse(classPage.isSelected());

        classPage = classPage.open().waitForIt();
        List<TreeNodeElement> children = classPage.getChildren();
        assertEquals(1, children.size());

        assertEquals(objectPageName, children.get(0).getLabel());

        // here it's an object
        assertEquals(String.format("object-%s.%s.%s", space, xobjectSpace, objectPageName), children.get(0).getId());
        assertTrue(children.get(0).isLeaf());

        CopyOrRenameOrDeleteStatusPage statusPage = jobQuestionPane.confirmQuestion();
        statusPage.waitUntilFinished();

        // check that the class page still exist
        ViewPage viewPage = setup.gotoPage(classReference);
        assertTrue(viewPage.exists());
    }

    /**
     * This is basically the same scenario than before except that we create a translation of the xclass, and we try
     * to delete that one: in theory it should only delete the translation.
     */
    @Test
    @Order(12)
    void deleteTranslationXClass(TestUtils setup, TestInfo info) throws Exception
    {
        // Create 2 pages in two locations:
        // first page is an xclass that we'll translate
        // second page will contain an xobject of the created xclass

        String testClassName = info.getTestClass().get().getSimpleName();
        String testMethodName = info.getTestMethod().get().getName();
        String xclassSpace = "XClassSpace";
        String xobjectSpace = "XObjectSpace";
        List<String> xclassSpaceReference = Arrays.asList(testClassName, testMethodName, xclassSpace);
        DocumentReference xclassReference = new DocumentReference("xwiki",
            xclassSpaceReference,
            "WebHome");
        List<String> xobjectSpaceReference = Arrays.asList(testClassName, testMethodName, xobjectSpace);
        DocumentReference xobjectReference = new DocumentReference("xwiki",
            xobjectSpaceReference,
            "WebHome");
        String space = testClassName + "." + testMethodName;
        String classPageName = "ClassPage";
        String objectPageName = "ObjectPage";

        DocumentReference classReference = new DocumentReference("xwiki", xclassSpaceReference, classPageName);
        DocumentReference objectReference = new DocumentReference("xwiki", xobjectSpaceReference, objectPageName);

        setup.createPage(classReference, "XClass page content", classPageName);
        setup.createPage(objectReference, "XObject page content", objectPageName);

        setup.addClassProperty(space, classPageName, "Foo", "String");
        setup.addObject(objectReference, classReference.toString(), Collections.singletonMap("Foo", "Bar"));

        // switch the wiki to multilingual
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");

        DocumentReference frenchXClassTranslation = new DocumentReference(classReference, Locale.FRENCH);
        setup.createPage(frenchXClassTranslation, "French XClass page content", classPageName);

        DocumentReference rootLocaleXClassReference = new DocumentReference(classReference, Locale.ROOT);

        // Check that accessing the page and its translation displays different content, since we'll use that
        // for checking if translation exists.
        ViewPage viewPage = setup.gotoPage(rootLocaleXClassReference);
        assertEquals("XClass page content", viewPage.getContent());

        viewPage = setup.gotoPage(frenchXClassTranslation);
        assertEquals("French XClass page content", viewPage.getContent());

        // Try to delete the xclass page
        ViewPage parentPage = setup.gotoPage(frenchXClassTranslation);
        DeletePageConfirmationPage confirmationPage = parentPage.deletePage();
        DeletingPage deletingPage = confirmationPage.confirmDeletePage();
        deletingPage.waitUntilFinished();

        // check that the translation does not exist, but the xclass does
        viewPage = setup.gotoPage(rootLocaleXClassReference);
        assertTrue(viewPage.exists());
        assertEquals("XClass page content", viewPage.getContent());

        viewPage = setup.gotoPage(frenchXClassTranslation);
        // We cannot check the deletion of the translation with a viewPage.exists() because
        // the original page is automatically displayed when the translation does not exist.
        assertEquals("XClass page content", viewPage.getContent());

        // We create back the translation
        setup.createPage(frenchXClassTranslation, "French XClass page content", classPageName);

        // This time we try to delete the class reference page (not the translation)
        parentPage = setup.gotoPage(rootLocaleXClassReference);
        confirmationPage = parentPage.deletePage();
        confirmationPage.confirmDeletePage();

        // At this point we should have the question job UI
        JobQuestionPane jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        assertFalse(jobQuestionPane.isEmpty());

        assertEquals("You are about to delete pages that contain used XClass.", jobQuestionPane.getQuestionTitle());
        TreeElement treeElement = jobQuestionPane.getQuestionTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();

        // there is a single node for the xclass:
        // 1. to represent free pages
        // 2. to represent classes with associated objects
        assertEquals(1, topLevelNodes.size());

        TreeNodeElement classPage = topLevelNodes.get(0);
        assertEquals(classPageName, classPage.getLabel());
        assertEquals(String.format("%s.%s.%s", space, xclassSpace, classPageName), classPage.getId());
        assertFalse(classPage.isSelected());

        classPage = classPage.open().waitForIt();
        List<TreeNodeElement> children = classPage.getChildren();
        assertEquals(1, children.size());

        assertEquals(objectPageName, children.get(0).getLabel());

        // here it's an object
        assertEquals(String.format("object-%s.%s.%s", space, xobjectSpace, objectPageName), children.get(0).getId());
        assertTrue(children.get(0).isLeaf());

        // select the class to be deleted
        classPage.select();

        CopyOrRenameOrDeleteStatusPage statusPage = jobQuestionPane.confirmQuestion();
        statusPage.waitUntilFinished();

        // Ensure the xclass has been deleted
        DeletePageOutcomePage deletePageOutcomePage = deletingPage.getDeletePageOutcomePage();
        assertTrue(deletePageOutcomePage.hasTerminalPagesInRecycleBin());

        // Ensure the translation has also been deleted
        setup.gotoPage(frenchXClassTranslation);
        deletePageOutcomePage = new DeletePageOutcomePage();
        assertEquals("Impossible de trouver ce document.", deletePageOutcomePage.getMessage());
    }

    /**
     * Check that when a new target document is selected, the backlinks are updated to the new value and the redirect is
     * working when accessing the old page.
     *
     * @since 14.4.2
     * @since 14.5
     */
    @Test
    @Order(13)
    void deleteWithUpdateLinksAndAutoRedirect(TestUtils testUtils, TestReference reference,
        TestConfiguration testConfiguration) throws Exception
    {
        DocumentReference backlinkDocumentReference = new DocumentReference("xwiki", "Backlink", "WebHome");
        DocumentReference newTargetReference = new DocumentReference("xwiki", "NewTarget", "WebHome");

        testUtils.createPage(reference, PAGE_CONTENT, PAGE_TITLE);
        // Create backlink.
        testUtils.createPage(backlinkDocumentReference,
            String.format("[[Link>>doc:%s]]", testUtils.serializeReference(reference)), "Backlink document");
        testUtils.createPage(newTargetReference, "", "New target");

        // Wait for Solr indexing to complete as backlink information from Solr is needed
        new SolrTestUtils(testUtils, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Delete page and provide a new target, with updateLinks and autoRedirect enabled.
        ViewPage viewPage = testUtils.gotoPage(reference);
        DeletePageConfirmationPage confirmationPage = viewPage.deletePage();
        confirmationPage.setNewBacklinkTarget(testUtils.serializeReference(newTargetReference));
        confirmationPage.setUpdateLinks(true);
        confirmationPage.setAutoRedirect(true);
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());

        // Verify that a redirect was added and the link was updated.
        viewPage = testUtils.gotoPage(reference);
        assertEquals("New target", this.viewPage.getDocumentTitle());
        assertEquals("[[Link>>doc:NewTarget.WebHome]]",
            testUtils.rest().<Page>get(backlinkDocumentReference).getContent());
    }

    /**
     * Check that if a new target is not selected, the backlinks are not altered and no redirect is added.
     *
     * @since 14.4.2
     * @since 14.5
     */
    @Test
    @Order(14)
    void deleteWithoutNewTarget(TestUtils testUtils, TestReference reference, TestConfiguration testConfiguration)
        throws Exception
    {
        DocumentReference backlinkDocReference = new DocumentReference("xwiki", "Backlink", "WebHome");
        String backlinkDocContent = String.format("[[Link>>doc:%s]]", testUtils.serializeReference(reference));

        testUtils.createPage(reference, PAGE_CONTENT, PAGE_TITLE);
        // Create backlink.
        testUtils.createPage(backlinkDocReference, backlinkDocContent, "Backlink document");

        // Wait for Solr indexing to complete as backlink information from Solr is needed
        new SolrTestUtils(testUtils, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Delete page without specifying a new target.
        ViewPage viewPage = testUtils.gotoPage(reference);
        DeletePageConfirmationPage confirmationPage = viewPage.deletePage();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();

        // Verify that there is no redirect and the links were not altered.
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        assertEquals(LOGGED_USERNAME, deleteOutcome.getPageDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
        assertEquals(backlinkDocContent, testUtils.rest().<Page>get(backlinkDocReference).getContent());
    }

    /**
     * Test that when you delete a page and you select "affect children" along with a new target document, only the
     * parent page has the backlinks updated a the redirect added.
     *
     * @since 14.4.2
     * @since 14.5
     */
    @Test
    @Order(15)
    void deleteWithAffectChildrenAndNewTarget(TestUtils testUtils, TestReference parentReference,
        TestConfiguration testConfiguration) throws Exception
    {
        DocumentReference childReference = new DocumentReference("Child", parentReference.getLastSpaceReference());
        String childFullName = testUtils.serializeReference(childReference).split(":")[1];
        DocumentReference backlinkDocReference = new DocumentReference("xwiki", "Backlink", "WebHome");
        DocumentReference newTargetReference = new DocumentReference("xwiki", "NewTarget", "WebHome");

        testUtils.createPage(parentReference, "Content", "Parent");
        testUtils.createPage(childReference, "", "Child");
        testUtils.createPage(newTargetReference, "", "New target");
        // Create backlinks to the parent and the child page.
        String format = "[[Parent>>doc:%s]] [[Child>>doc:%s]]";
        testUtils.createPage(backlinkDocReference,
            String.format(format, testUtils.serializeReference(parentReference), childFullName), "Backlink document");

        // Wait for Solr indexing to complete as backlink information from Solr is needed
        new SolrTestUtils(testUtils, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Delete parent page with affectChildren and newTarget (updateLinks and autoRedirect enabled).
        ViewPage parentPage = testUtils.gotoPage(parentReference);
        DeletePageConfirmationPage confirmationPage = parentPage.deletePage();
        confirmationPage.setAffectChildren(true);
        confirmationPage.setNewBacklinkTarget(testUtils.serializeReference(newTargetReference));
        confirmationPage.setUpdateLinks(true);
        confirmationPage.setAutoRedirect(true);
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();

        // Verify that there is no redirect on the child page and backlink was not altered.
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        String newContent =
            String.format(format, testUtils.serializeReference(newTargetReference).split(":")[1], childFullName);
        assertEquals(newContent, testUtils.rest().<Page>get(backlinkDocReference).getContent());
        parentPage = testUtils.gotoPage(parentReference);
        assertEquals("New target", parentPage.getDocumentTitle());
        ViewPage childPage = testUtils.gotoPage(childReference);
        assertEquals("Child", childPage.getDocumentTitle());
    }
}

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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.flamingo.skin.test.po.JobQuestionPane;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class RenamePageIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Order(1)
    @Test
    void convertNestedPageToTerminalPage(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Note: we use a 3-level-deep nested page since 2 levels wouldn't show problems such as the regression
        // we've had with https://jira.xwiki.org/browse/XWIKI-16170

        // Clean-up: delete the pages that will be used in this test
        DocumentReference reference = new DocumentReference("xwiki", Arrays.asList("1", "2", "3"), "WebHome");
        setup.rest().delete(reference);
        setup.rest().delete(new DocumentReference("xwiki", Arrays.asList("1", "2"), "3"));

        // Create 1.2.3.WebHome
        ViewPage vp = setup.createPage(reference, "", "");

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Go to the Rename page view for 1.2.3.WebHome and check the Terminal checkbox. We also need to uncheck the
        // Auto Redirect checkbox so the page 1.2.3.WebHome will not appear as existing after the Rename operation.
        RenamePage renamePage = vp.rename();
        renamePage.setTerminal(true);
        renamePage.setAutoRedirect(false);
        renamePage.clickRenameButton().waitUntilFinished();

        // Test if 1.2.3.WebHome has been renamed to 1.2.3 (1.2.3.WebHome doesn't exist while 1.2.3 exists)
        assertTrue(setup.pageExists(Arrays.asList("1", "2"), "3"), "Page 1.2.3 doesn't exist!");
        assertFalse(setup.pageExists(Arrays.asList("1", "2", "3"), "WebHome"), "Page 1.2.3.WebHome exists!");
    }

    @Order(2)
    @Test
    void renamePageCheckConfirmationPreserveChildrenUpdateLinksSetAutoRedirect(TestUtils setup,
        TestConfiguration testConfiguration) throws Exception
    {
        // Clean-up: delete the pages that will be used in this test
        setup.rest().deletePage("My", "Page");
        setup.rest().delete(setup.resolveDocumentReference("1.2.WebHome"));
        setup.rest().delete(setup.resolveDocumentReference("1.2.3.WebHome"));
        setup.rest().delete(setup.resolveDocumentReference("A.B.2.WebHome"));
        setup.rest().delete(setup.resolveDocumentReference("A.B.2.3.WebHome"));

        // Create the needed pages
        setup.createPage(Arrays.asList("1", "2"), "WebHome", "", "");
        setup.createPage(Arrays.asList("1", "2", "3"), "WebHome", "", "");
        // We set the content to "[[1.2.WebHome]]" to test the "Update Links" feature
        setup.createPage("My", "Page", "[[1.2.WebHome]]", "");

        // Go to 1.2.WebHome to start the test
        setup.gotoPage(Arrays.asList("1", "2"), "WebHome", "", "");

        ViewPage vp = new ViewPage();

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Go to the Rename page view for 1.2.WebHome.
        RenamePage renamePage = vp.rename();
        // Check the "Preserve Children", "Update Links" & "Auto Redirect" checkboxes.
        renamePage.setPreserveChildren(true);
        renamePage.setUpdateLinks(true);
        renamePage.setAutoRedirect(true);
        // Set the new parent as "A.B"
        renamePage.getDocumentPicker().setParent("A.B");
        CopyOrRenameOrDeleteStatusPage renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();

        // Check successful Rename confirmation
        assertEquals("Done.", renameStatusPage.getInfoMessage());
        // Test the Rename operation: we need to have 2.WebHome and 2.3.WebHome under A.B
        assertTrue(setup.pageExists(Arrays.asList("A", "B", "2"), "WebHome"), "Page A.B.2.WebHome doesn't exist!");
        assertTrue(setup.pageExists(Arrays.asList("A", "B", "2", "3"), "WebHome"),
            "Page A.B.2.3.WebHome doesn't exist!");
        // Test the Auto Redirect: when visiting the original pages you need to be redirected to the new locations
        setup.gotoPage(Arrays.asList("1", "2"), "WebHome", "view", "");
        assertEquals("/A/B/2", vp.getBreadcrumbContent());
        setup.gotoPage(Arrays.asList("1", "2", "3"), "WebHome", "view", "");
        assertEquals("/A/B/2/3", vp.getBreadcrumbContent());
        // Test the Update Links feature: the content of the page needs to point to the new location
        assertEquals("[[A.B.2.WebHome]]", setup.gotoPage("My", "Page").editWiki().getContent());
    }

    /**
     * This test check the behaviour of job questions when renaming a page with a used class - check that the question
     * UI is displayed in that case - check that when two jobs are triggered one is blocked and display the UI - check
     * that we can cancel a job on a question and it unblocks the others - check that we can select some files in the
     * question to be renamed.
     */
    @Order(3)
    @Test
    void renamePageWithUsedClass(TestUtils setup, TestReference testReference)
    {
        // Create 4 pages under the same parent
        // 2 of them are free pages (WebHome and FreePage)
        // 1 of them is a class (ClassPage)
        // the last one contains an object property using the class
        String space =
            testReference.getSpaceReferences().stream().map(SpaceReference::getName).collect(Collectors.joining("."));
        String classPageName = "ClassPage";
        String objectPageName = "ObjectPage";
        String freePageName = "FreePage";

        DocumentReference classReference = new DocumentReference(classPageName, testReference.getLastSpaceReference());
        DocumentReference objectReference =
            new DocumentReference(objectPageName, testReference.getLastSpaceReference());
        DocumentReference freeReference = new DocumentReference(freePageName, testReference.getLastSpaceReference());

        setup.createPage(testReference, "Some content", "Parent");
        setup.createPage(freeReference, "Some content", freePageName);
        setup.createPage(classReference, "Some content", classPageName);
        setup.createPage(objectReference, "Some content", objectPageName);

        setup.addClassProperty(space, classPageName, "Foo", "String");
        setup.addObject(objectReference, classReference.toString(), Collections.singletonMap("Foo", "Bar"));

        // Try to rename the parent page
        ViewPage parentPage = setup.gotoPage(testReference);
        RenamePage renamePage = parentPage.rename();
        renamePage.getDocumentPicker().setParent("Foo");
        renamePage.clickRenameButton();

        // At this point we should have the question job UI
        JobQuestionPane jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        assertFalse(jobQuestionPane.isEmpty());

        assertEquals("You are about to rename pages that contain used XClass.", jobQuestionPane.getQuestionTitle());
        TreeElement treeElement = jobQuestionPane.getQuestionTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();

        // there is two main nodes:
        // 1. to represent free pages
        // 2. to represent classes with associated objects
        assertEquals(2, topLevelNodes.size());
        TreeNodeElement freePages = topLevelNodes.get(0);
        assertEquals("Pages that do not contain any used XClass", freePages.getLabel());
        assertFalse(freePages.isSelected());

        freePages = freePages.open().waitForIt();
        List<TreeNodeElement> children = freePages.getChildren();

        // free pages should contain three nodes with alphabetical order:
        // 1. the FreePage obviously
        // 2. the ObjectPage since it does not contain a class
        // 3. the WebHome page
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

        // current URL with the job ID
        String firstJobUrl = setup.getDriver().getCurrentUrl();

        String newParentName = testReference.getSpaceReferences().get(0).getName() + "newParent";

        // we'll try to rename the same page
        // the new job will be blocked since we did not cancel or confirm the first one
        parentPage = setup.gotoPage(testReference);
        renamePage = parentPage.rename();
        renamePage.getDocumentPicker().setParent(newParentName);
        renamePage.clickRenameButton();
        jobQuestionPane = new JobQuestionPane();
        assertTrue(jobQuestionPane.isBlockedJob());
        String secondJobUrl = setup.getDriver().getCurrentUrl();

        // go back to the first job to cancel it
        setup.getDriver().navigate().to(firstJobUrl);

        jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        jobQuestionPane.cancelQuestion();
        assertTrue(jobQuestionPane.isCanceled());

        // go back to the second job to only rename the freePage
        setup.getDriver().navigate().to(secondJobUrl);
        jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        assertFalse(jobQuestionPane.isBlockedJob());
        treeElement = jobQuestionPane.getQuestionTree();
        freePages = treeElement.getTopLevelNodes().get(0);
        freePages = freePages.open().waitForIt();

        // select the free page
        freePage = freePages.getChildren().get(0);
        assertEquals(freePageName, freePage.getLabel());
        freePage.select();
        CopyOrRenameOrDeleteStatusPage statusPage = jobQuestionPane.confirmQuestion();
        statusPage.waitUntilFinished();

        // check that the old one doesn't exist anymore and only the new one exists
        ViewPage viewPage = setup.gotoPage(freeReference);
        assertFalse(viewPage.exists());

        DocumentReference newFreePage = new DocumentReference("xwiki",
            Arrays.asList(newParentName, testReference.getLastSpaceReference().getName()), freePageName);
        viewPage = setup.gotoPage(newFreePage);
        assertTrue(viewPage.exists());
    }

    /**
     * Test renaming a page whose reference is used in another page content, for links and inside macro content and
     * macro parameters.
     *
     * @since 13.4RC1
     */
    @Order(4)
    @Test
    void renamePageUsedInMacroContentAndParameters(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // FIXME: Using WebHome locations and not terminal page, since it's not properly supported yet.
        // Improve the test whenever XWIKI-18634 is fixed
        // Fixture: we're creating 5 different pages to ensure that the backlinks, rename are working properly
        // and independently in different situations.
        String sourcePageName = "WebHome";
        String sourcePageLocation1 = RenamePageIT.class.getSimpleName() + ".StandardLink";
        String sourcePage1 = sourcePageLocation1 + "." + sourcePageName;

        String sourcePageLocation2 = RenamePageIT.class.getSimpleName() + ".StandardMacroLink";
        String sourcePage2 = sourcePageLocation2 + "." + sourcePageName;

        String sourcePageLocation3 = RenamePageIT.class.getSimpleName() + ".NestedMacroLink";
        String sourcePage3 = sourcePageLocation3 + "." + sourcePageName;

        String sourcePageLocation4 = RenamePageIT.class.getSimpleName() + ".ImageLink";
        String sourcePage4 = sourcePageLocation4 + "." + sourcePageName;

        String sourcePageLocation5 = RenamePageIT.class.getSimpleName() + ".IncludeLink";
        String sourcePage5 = sourcePageLocation5 + "." + sourcePageName;

        String targetPageSubSpace = RenamePageIT.class.getSimpleName() + ".SubSpace";
        String targetPageLastSpace1 = "TargetStandardLink";
        String targetPage1 = targetPageSubSpace + "." + targetPageLastSpace1 + ".WebHome";

        String targetPageLastSpace2 = "TargetStandardMacroLink";
        String targetPage2 = targetPageSubSpace + "." + targetPageLastSpace2 + ".WebHome";

        String targetPageLastSpace3 = "TargetNestedMacroLink";
        String targetPage3 = targetPageSubSpace + "." + targetPageLastSpace3 + ".WebHome";

        String targetPageLastSpace4 = "TargetImageLink";
        String targetPage4 = targetPageSubSpace + "." + targetPageLastSpace4 + ".WebHome";

        String targetPageLastSpace5 = "TargetIncludeLink";
        String targetPage5 = targetPageSubSpace + "." + targetPageLastSpace5 + ".WebHome";

        EntityReference sourcePageReference1 = setup.resolveDocumentReference(sourcePage1);
        EntityReference sourcePageReference2 = setup.resolveDocumentReference(sourcePage2);
        EntityReference sourcePageReference3 = setup.resolveDocumentReference(sourcePage3);
        EntityReference sourcePageReference4 = setup.resolveDocumentReference(sourcePage4);
        EntityReference sourcePageReference5 = setup.resolveDocumentReference(sourcePage5);

        EntityReference targetPageReference1 = setup.resolveDocumentReference(targetPage1);
        EntityReference targetPageReference2 = setup.resolveDocumentReference(targetPage2);
        EntityReference targetPageReference3 = setup.resolveDocumentReference(targetPage3);
        EntityReference targetPageReference4 = setup.resolveDocumentReference(targetPage4);
        EntityReference targetPageReference5 = setup.resolveDocumentReference(targetPage5);

        setup.rest().delete(sourcePageReference1);
        setup.rest().delete(sourcePageReference2);
        setup.rest().delete(sourcePageReference3);
        setup.rest().delete(sourcePageReference4);
        setup.rest().delete(sourcePageReference5);

        setup.rest().delete(targetPageReference1);
        setup.rest().delete(targetPageReference2);
        setup.rest().delete(targetPageReference3);
        setup.rest().delete(targetPageReference4);
        setup.rest().delete(targetPageReference5);

        setup.rest().delete(testReference);

        setup.createPage(sourcePageReference1, "Some content to be linked. number 1");
        setup.createPage(sourcePageReference2, "Some content to be linked in macro. number 2");
        setup.createPage(sourcePageReference3, "Some content to be linked in nested macro. number 3");
        setup.createPage(sourcePageReference4, "A page with image to be linked. number 4");
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        File image = new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/image.gif");
        attachmentsPane.setFileToUpload(image.getAbsolutePath());
        attachmentsPane.waitForUploadToFinish("image.gif");
        setup.createPage(sourcePageReference5, "A page to be included. number 5");

        String testPageContent = "Check out this page: [[type the link label>>doc:%1$s]]\n" + "\n" + "{{warning}}\n"
            + "Withing a macro: Check out this page: [[type the link label>>doc:%2$s]]\n" + "\n" + "{{error}}\n"
            + "And in nested macro: Check out this page: [[type the link label>>doc:%3$s]]\n" + "{{/error}}\n" + "\n"
            + " \n" + "{{/warning}}\n" + "\n" + "Picture: [[image:%4$s@image.gif]]\n" + "Include macro:\n" + "\n"
            + "{{include reference=\"%5$s\"/}}\n\n"
            + "== A section ==\n\n"
            + "First link again: [[type the link label>>doc:%1$s]]\n\n"
            + "{{warning}}\n"
            + "Withing a macro: Check out this page: [[type the link label>>doc:%1$s]]\n"
            + "{{/warning}}\n\n"
            + "Final line.";
        setup.createPage(testReference,
            String.format(testPageContent, sourcePage1, sourcePage2, sourcePage3, sourcePage4, sourcePage5));

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // rename link 1
        ViewPage viewPage = setup.gotoPage(sourcePageReference1);
        RenamePage rename = viewPage.rename();
        DocumentPicker documentPicker = rename.getDocumentPicker();
        documentPicker.setParent(targetPageSubSpace);
        documentPicker.setName(targetPageLastSpace1);
        rename.setTerminal(false); // to be changed too when XWIKI-18634 is fixed.
        rename.clickRenameButton().waitUntilFinished();

        viewPage = setup.gotoPage(testReference);
        WikiEditPage wikiEditPage = viewPage.editWiki();
        assertEquals(String.format(testPageContent, targetPage1, sourcePage2, sourcePage3, sourcePage4, sourcePage5),
            wikiEditPage.getContent());

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // rename link 2
        viewPage = setup.gotoPage(sourcePageReference2);
        rename = viewPage.rename();
        documentPicker = rename.getDocumentPicker();
        documentPicker.setParent(targetPageSubSpace);
        documentPicker.setName(targetPageLastSpace2);
        rename.setTerminal(false); // to be changed too when XWIKI-18634 is fixed.
        rename.clickRenameButton().waitUntilFinished();

        viewPage = setup.gotoPage(testReference);
        wikiEditPage = viewPage.editWiki();
        assertEquals(String.format(testPageContent, targetPage1, targetPage2, sourcePage3, sourcePage4, sourcePage5),
            wikiEditPage.getContent());

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // rename link 3
        viewPage = setup.gotoPage(sourcePageReference3);
        rename = viewPage.rename();
        documentPicker = rename.getDocumentPicker();
        documentPicker.setParent(targetPageSubSpace);
        documentPicker.setName(targetPageLastSpace3);
        rename.setTerminal(false); // to be changed too when XWIKI-18634 is fixed.
        rename.clickRenameButton().waitUntilFinished();

        viewPage = setup.gotoPage(testReference);
        wikiEditPage = viewPage.editWiki();
        assertEquals(String.format(testPageContent, targetPage1, targetPage2, targetPage3, sourcePage4, sourcePage5),
            wikiEditPage.getContent());

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // rename link 4
        viewPage = setup.gotoPage(sourcePageReference4);
        rename = viewPage.rename();
        documentPicker = rename.getDocumentPicker();
        documentPicker.setParent(targetPageSubSpace);
        documentPicker.setName(targetPageLastSpace4);
        rename.setTerminal(false); // to be changed too when XWIKI-18634 is fixed.
        rename.clickRenameButton().waitUntilFinished();

        viewPage = setup.gotoPage(testReference);
        wikiEditPage = viewPage.editWiki();
        assertEquals(String.format(testPageContent, targetPage1, targetPage2, targetPage3, targetPage4, sourcePage5),
            wikiEditPage.getContent());

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // rename link 5
        viewPage = setup.gotoPage(sourcePageReference5);
        rename = viewPage.rename();
        documentPicker = rename.getDocumentPicker();
        documentPicker.setParent(targetPageSubSpace);
        documentPicker.setName(targetPageLastSpace5);
        rename.setTerminal(false); // to be changed too when XWIKI-18634 is fixed.
        rename.clickRenameButton().waitUntilFinished();

        viewPage = setup.gotoPage(testReference);
        wikiEditPage = viewPage.editWiki();
        assertEquals(String.format(testPageContent, targetPage1, targetPage2, targetPage3, targetPage4, targetPage5),
            wikiEditPage.getContent());
    }

    /**
     * Test renaming a page refactor outgoing links in both the default locale and translations.
     */
    @Order(5)
    @Test
    void renamePageRelativeLinkPageAndTranslation(TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        String parent = "RenamePageIT";
        String name = "renamePageRefactorOutgoingLinksInPageAndTranslation";
        LocalDocumentReference reference = new LocalDocumentReference(parent, name);

        // Create a page and a translation with a link
        setup.rest().savePage(reference, "[[OtherPage]]", "");
        setup.rest().savePage(new LocalDocumentReference(reference, Locale.FRENCH), "fr [[OtherPage]]", "");

        assertEquals("[[OtherPage]]", setup.rest().<Page>get(reference).getContent());

        // Wait for the solr indexing to be completed before doing any rename
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Rename the page
        ViewPage vp = setup.gotoPage(reference);
        String newSpace = "New" + parent;
        String newName = "New" + name;
        RenamePage renamePage = vp.rename();
        renamePage.getDocumentPicker().setParent(newSpace);
        renamePage.getDocumentPicker().setName(newName);
        renamePage.clickRenameButton().waitUntilFinished();

        // Make sure the link was refactored in both the page and its translation
        Page newPage = setup.rest().get(new LocalDocumentReference(newSpace, newName));
        assertEquals("[[OtherPage]]", newPage.getContent());

        newPage = setup.rest().get(new LocalDocumentReference(newSpace, newName, Locale.FRENCH));
        assertEquals("fr [[OtherPage]]", newPage.getContent());
    }

    @Order(6)
    @Test
    void renamePageWithObjectAndAttachmentsPreserveHistory(TestUtils testUtils, TestReference testReference)
        throws Exception
    {
        testUtils.rest().savePage(testReference, "Content 0", "Title");
        testUtils.rest().savePage(testReference, "Content 0 1", "Title");
        testUtils.rest().savePage(testReference, "Content 0 1 2", "Title");
        testUtils.rest().savePage(testReference, "Content 0 1 2 3", "Title");
        testUtils.rest().savePage(testReference, "Content 0 1 2 3 4", "Title");
        // add attachment
        AttachmentReference attachmentReference = new AttachmentReference("file.txt", testReference);
        testUtils.rest().attachFile(attachmentReference, "attachment1".getBytes(), true);
        //add object
        Object styleSheetObject = testUtils.rest().object(testReference, "XWiki.StyleSheetExtension");
        styleSheetObject.getProperties().add(testUtils.rest().property("title", "a ssx"));
        testUtils.rest().add(styleSheetObject);

        ViewPage viewPage = testUtils.gotoPage(testReference);
        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals(7, historyPane.getNumberOfVersions());
        assertEquals("7.1", historyPane.getCurrentVersion());

        RenamePage rename = viewPage.rename();
        rename.getDocumentPicker().setTitle("Another Title");
        CopyOrRenameOrDeleteStatusPage statusPage = rename.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());
        viewPage = statusPage.gotoNewPage();
        historyPane = viewPage.openHistoryDocExtraPane();
        assertEquals(7, historyPane.getNumberOfVersions());
        assertEquals("7.2", historyPane.getCurrentVersion());
        assertEquals("Update document after refactoring.", historyPane.getCurrentVersionComment());
    }

    @Order(7)
    @Test
    void renamePageWithRedirectAndBack(TestUtils setup, TestReference testReference) throws Exception
    {
        String sourceName = "Source";
        String targetName = "Target";

        DocumentReference sourceReference =
            new DocumentReference("WebHome", new SpaceReference(sourceName, testReference.getLastSpaceReference()));
        DocumentReference targetReference =
            new DocumentReference("WebHome", new SpaceReference(targetName, testReference.getLastSpaceReference()));

        // Clean-up: delete the pages that will be used in this test
        setup.rest().delete(sourceReference);
        setup.rest().delete(targetReference);

        // Create the needed page
        setup.rest().savePage(sourceReference, "source", sourceName);

        // Rename source to target with redirect
        ViewPage vp = setup.gotoPage(sourceReference);
        RenamePage renamePage = vp.rename();
        renamePage.setAutoRedirect(true);
        renamePage.getDocumentPicker().setTitle(targetName);
        CopyOrRenameOrDeleteStatusPage renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", renameStatusPage.getInfoMessage());
        assertTrue(setup.rest().exists(sourceReference));
        assertTrue(setup.rest().exists(targetReference));

        // Make sure the redirect is in place
        vp = setup.gotoPage(sourceReference);
        assertEquals(setup.serializeReference(targetReference), vp.getMetaDataValue("reference"));

        // Rename back target to source
        renamePage = vp.rename();
        renamePage.getDocumentPicker().setTitle(sourceName);
        renameStatusPage = renamePage.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", renameStatusPage.getInfoMessage());
        assertTrue(setup.rest().exists(sourceReference));
        assertFalse(setup.rest().exists(targetReference));

        // Make sure the source is back to normal
        vp = setup.gotoPage(sourceReference);
        assertEquals(setup.serializeReference(sourceReference), vp.getMetaDataValue("reference"));
    }

    @Order(8)
    @ParameterizedTest
    @ValueSource(strings = { "true", "false" })
    @NullSource
    void renameWithIndexingWaiting(String strategy, TestUtils setup, TestReference testReference,
        TestConfiguration testConfiguration) throws Exception
    {
        // Create two pages that link to each other.
        SpaceReference spaceReference = testReference.getLastSpaceReference();
        SpaceReference sourceSpace = new SpaceReference("Source", spaceReference);
        SpaceReference targetSpace = new SpaceReference("Target", spaceReference);
        DocumentReference sourceReference = new DocumentReference("WebHome", sourceSpace);
        DocumentReference targetReference = new DocumentReference("WebHome", targetSpace);

        SpaceReference otherSpace = new SpaceReference("Other", spaceReference);
        DocumentReference otherReference = new DocumentReference("WebHome", otherSpace);

        setup.rest().delete(sourceReference);
        setup.rest().delete(targetReference);
        setup.rest().delete(otherReference);

        // Wait for an empty queue here to ensure that the deleted page has been removed from the index and links
        // won't be updated just because the page is still in the index.
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        // Make the other page slow to index by sleeping when we're in Solr.
        String slowTitle = "$services.sleep.sleepInSolr(20) Slow Title";

        setup.rest().savePage(sourceReference,
            "Source, see [[%s]].".formatted(setup.serializeLocalReference(otherReference)), "Source");
        String otherFormat = "Other, see [[%s]]";
        String otherContent = otherFormat.formatted(setup.serializeLocalReference(sourceReference));
        setup.rest().savePage(otherReference, otherContent, slowTitle);

        ViewPage viewPage = setup.gotoPage(sourceReference);
        RenamePage renamePage = viewPage.rename();
        renamePage.getDocumentPicker().setTitle("Target");
        CopyOrRenameOrDeleteStatusPage statusPage = renamePage.clickRenameButton();

        int currentTimeout = setup.getDriver().getTimeout();
        try {
            // Increase the timeout as the question pane will only appear after 10 seconds and then we need to wait
            // possibly for another 10 seconds until indexing finished.
            setup.getDriver().setTimeout(20);
            JobQuestionPane jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
            assertEquals("Continue waiting for the link indexing to finish?", jobQuestionPane.getQuestionTitle());
            if (strategy != null) {
                jobQuestionPane.clickButton("qproperty_continueWaiting", strategy);
            }
            statusPage.waitUntilFinished();

            assertEquals("Done.", statusPage.getInfoMessage());
            assertTrue(setup.rest().exists(targetReference));
            assertFalse(setup.rest().exists(sourceReference));

            String updatedOtherContent = setup.gotoPage(otherReference).editWiki().getContent();
            if ("false".equals(strategy)) {
                // Make sure the other page hasn't been changed.
                assertEquals(otherContent, updatedOtherContent);
            } else {
                // Both when the question isn't answered and when the continue waiting button is clicked, the other page
                // should be updated.
                assertEquals(otherFormat.formatted(setup.serializeLocalReference(targetReference)),
                    updatedOtherContent);
            }
        } finally {
            setup.getDriver().setTimeout(currentTimeout);
            // Make sure we delete that strange page again.
            setup.rest().delete(otherReference);
        }
    }

    @Order(9)
    @Test
    void renameWithRelativeLinks(TestUtils testUtils, TestReference testReference, TestConfiguration testConfiguration)
        throws Exception
    {
        testUtils.createPage(testReference, "[[Alice]]\n[[Bob]]\n[[Eve]]", "Test relative links");
        SpaceReference rootSpaceReference = testReference.getLastSpaceReference();
        SpaceReference aliceSpace = new SpaceReference("Alice", rootSpaceReference);
        testUtils.createPage(new DocumentReference("WebHome", aliceSpace), "Alice page",
            "Alice");
        SpaceReference bobSpace = new SpaceReference("Bob", rootSpaceReference);
        testUtils.createPage(new DocumentReference("WebHome", bobSpace), "[[Alice]]",
            "Alice");

        // Wait for an empty queue here to ensure that the deleted page has been removed from the index and links
        // won't be updated just because the page is still in the index.
        new SolrTestUtils(testUtils, testConfiguration.getServletEngine()).waitEmptyQueue();

        ViewPage viewPage = testUtils.gotoPage(testReference);
        RenamePage rename = viewPage.rename();
        rename.getDocumentPicker().setName(rootSpaceReference.getName() + "Foo");
        CopyOrRenameOrDeleteStatusPage statusPage = rename.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());

        WikiEditPage wikiEditPage = statusPage.gotoNewPage().editWiki();
        assertEquals("[[Alice]]\n[[Bob]]\n[[Eve]]", wikiEditPage.getContent());

        SpaceReference newRootSpace =
            new SpaceReference(rootSpaceReference.getName() + "Foo", rootSpaceReference.getParent());
        SpaceReference newBobSpace = new SpaceReference("Bob", newRootSpace);
        wikiEditPage = WikiEditPage.gotoPage(new DocumentReference("WebHome", newBobSpace));
        assertEquals("[[Alice]]", wikiEditPage.getContent());

        SpaceReference newAliceSpace = new SpaceReference("Alice", newRootSpace);
        DocumentReference newAliceReference = new DocumentReference("WebHome", newAliceSpace);
        viewPage = testUtils.gotoPage(newAliceReference);
        rename = viewPage.rename();
        rename.getDocumentPicker().setName("Alice2");
        statusPage = rename.clickRenameButton().waitUntilFinished();
        assertEquals("Done.", statusPage.getInfoMessage());

        SpaceReference Alice2Space = new SpaceReference("Alice2", newRootSpace);
        DocumentReference Alice2Reference = new DocumentReference("WebHome", Alice2Space);
        wikiEditPage = WikiEditPage.gotoPage(new DocumentReference("WebHome", newRootSpace));
        String serializedAlice2Reference = testUtils.serializeLocalReference(Alice2Reference);
        assertEquals(String.format("[[%s]]\n[[Bob]]\n[[Eve]]", serializedAlice2Reference), wikiEditPage.getContent());

        // FIXME: ideally this one should be refactored too, however it's not a regression.
        //wikiEditPage = WikiEditPage.gotoPage(new DocumentReference("WebHome", newBobSpace));
        //assertEquals(String.format("[[%s]]", serializedAlice2Reference), wikiEditPage.getContent());
    }
}

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
package org.xwiki.flamingo.test.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.JobQuestionPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.tree.test.po.TreeElement;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
public class RenamePageIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Order(1)
    @Test
    public void convertNestedPageToTerminalPage(TestUtils setup) throws Exception
    {
        // Note: we use a 3-level-deep nested page  since 2 levels wouldn't show problems such as the regression
        // we've had with https://jira.xwiki.org/browse/XWIKI-16170

        // Clean-up: delete the pages that will be used in this test
        DocumentReference reference = new DocumentReference("xwiki", Arrays.asList("1", "2", "3"), "WebHome");
        setup.rest().delete(reference);
        setup.rest().delete(new DocumentReference("xwiki", Arrays.asList("1", "2"), "3"));

        // Create 1.2.3.WebHome
        ViewPage vp = setup.createPage(reference, "", "");

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
    public void renamePageCheckConfirmationPreserveChildrenUpdateLinksSetAutoRedirect(TestUtils setup) throws Exception
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
        Assert.assertTrue("Page A.B.2.WebHome doesn't exist!", setup.pageExists(Arrays.asList("A", "B", "2"), "WebHome"));
        Assert.assertTrue("Page A.B.2.3.WebHome doesn't exist!",
            setup.pageExists(Arrays.asList("A", "B", "2", "3"), "WebHome"));
        // Test the Auto Redirect: when visiting the original pages you need to be redirected to the new locations
        setup.gotoPage(Arrays.asList("1", "2"), "WebHome", "view", "");
        assertEquals("/A/B/2", vp.getBreadcrumbContent());
        setup.gotoPage(Arrays.asList("1", "2", "3"), "WebHome", "view", "");
        assertEquals("/A/B/2/3", vp.getBreadcrumbContent());
        // Test the Update Links feature: the content of the page needs to point to the new location
        assertEquals("[[A.B.2.WebHome]]", setup.gotoPage("My", "Page").editWiki().getContent());
    }

    /**
     * This test check the behaviour of job questions when renaming a page with a used class
     *   - check that the question UI is displayed in that case
     *   - check that when two jobs are triggered one is blocked and display the UI
     *   - check that we can cancel a job on a question and it unblocks the others
     *   - check that we can select some files in the question to be renamed.
     */
    @Order(3)
    @Test
    public void renamePageWithUsedClass(TestUtils setup, TestReference testReference)
    {
        // Create 4 pages under the same parent
        // 2 of them are free pages (WebHome and FreePage)
        // 1 of them is a class (ClassPage)
        // the last one contains an object property using the class
        String space = testReference.getSpaceReferences()
            .stream().map(SpaceReference::getName).collect(Collectors.joining("."));
        String classPageName = "ClassPage";
        String objectPageName = "ObjectPage";
        String freePageName = "FreePage";

        DocumentReference classReference = new DocumentReference(classPageName, testReference.getLastSpaceReference());
        DocumentReference objectReference = new DocumentReference(objectPageName,
            testReference.getLastSpaceReference());
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
        Assert.assertFalse(jobQuestionPane.isEmpty());

        assertEquals("You are about to rename pages that contain used XClass.", jobQuestionPane.getQuestionTitle());
        TreeElement treeElement = jobQuestionPane.getQuestionTree();
        List<TreeNodeElement> topLevelNodes = treeElement.getTopLevelNodes();

        // there is two main nodes:
        //  1. to represent free pages
        //  2. to represent classes with associated objects
        assertEquals(2, topLevelNodes.size());
        TreeNodeElement freePages = topLevelNodes.get(0);
        assertEquals("Pages that do not contain any used XClass", freePages.getLabel());
        Assert.assertFalse(freePages.isSelected());

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
        Assert.assertTrue(freePage.isLeaf());

        TreeNodeElement objectPage = children.get(1);
        assertEquals(objectPageName, objectPage.getLabel());
        assertEquals(space + "." + objectPageName, objectPage.getId());
        Assert.assertTrue(objectPage.isLeaf());

        TreeNodeElement webHome = children.get(2);

        // label = page title
        assertEquals("Parent", webHome.getLabel());
        assertEquals(space + ".WebHome", webHome.getId());
        Assert.assertTrue(webHome.isLeaf());

        TreeNodeElement classPage = topLevelNodes.get(1);
        assertEquals(classPageName, classPage.getLabel());
        assertEquals(space + "." + classPageName, classPage.getId());
        Assert.assertFalse(classPage.isSelected());

        classPage = classPage.open().waitForIt();
        children = classPage.getChildren();
        assertEquals(1, children.size());

        assertEquals(objectPageName, children.get(0).getLabel());

        // here it's an object
        assertEquals("object-" + space + "." + objectPageName, children.get(0).getId());
        Assert.assertTrue(children.get(0).isLeaf());

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
        Assert.assertTrue(jobQuestionPane.isBlockedJob());
        String secondJobUrl = setup.getDriver().getCurrentUrl();

        // go back to the first job to cancel it
        setup.getDriver().navigate().to(firstJobUrl);

        jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        jobQuestionPane.cancelQuestion();
        Assert.assertTrue(jobQuestionPane.isCanceled());

        // go back to the second job to only rename the freePage
        setup.getDriver().navigate().to(secondJobUrl);
        jobQuestionPane = new JobQuestionPane().waitForQuestionPane();
        Assert.assertFalse(jobQuestionPane.isBlockedJob());
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
        Assert.assertFalse(viewPage.exists());

        DocumentReference newFreePage = new DocumentReference("xwiki",
            Arrays.asList(newParentName, testReference.getLastSpaceReference().getName()),
            freePageName);
        viewPage = setup.gotoPage(newFreePage);
        Assert.assertTrue(viewPage.exists());
    }
}

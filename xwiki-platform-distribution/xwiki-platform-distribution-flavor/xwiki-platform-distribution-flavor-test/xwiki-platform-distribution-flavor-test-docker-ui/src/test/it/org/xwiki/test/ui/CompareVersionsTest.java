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
package org.xwiki.test.ui;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ChangesPane;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.FormContainerElement;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the comparison of document versions.
 * 
 * @version $Id$
 * @since 12.8RC1
 */
@UITest
public class CompareVersionsTest
{
    // @Rule
    // public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, testUtils);

    /**
     * The test page.
     */
    private ViewPage testPage;

    private final static String pageName = "PageWithManyVersions";

    @BeforeAll
    void setUp(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getClass().getSimpleName();
        // Check if the test page exists.
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        if (this.testPage.exists()) {
            // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
            // view mode) is fixed.
            this.testPage.waitForDocExtraPaneActive("comments");
            // NOTE: We use the same page for all tests because tests don't modify the test page. They only compare two
            // versions of the test page.
            return;
        }

        // Since this test also verifies diffs when the parent/child relationship is modified, use this mode (which
        // isn't the default) for testing purpose. Also note that in the "reference" mode there's no diff to display
        // when the document is moved to a different parent since it means changing the identity of the document for
        // now and thus changing it means getting a new document.
        testUtils.setHierarchyMode("parentchild");

        // Create the test page.
        // Version 1.1
        this.testPage = testUtils.createPage(testClassName, pageName, "one\ntwo\nthree", "Test");
        // Change the content and the meta data.
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("content", "one\n**two**\nfour");
        queryMap.put("title", "Compare verSions test");
        queryMap.put("parent", "Sandbox.WebHome");
        queryMap.put("commentinput", "Changed content and meta data.");
        queryMap.put("minorEdit", "true");
        // Version 1.2
        testUtils.gotoPage(testClassName, pageName, "save", queryMap);

        queryMap = new HashMap<>();
        queryMap.put("title", "Compare versions test");
        queryMap.put("commentinput", "Fix typo in title.");
        queryMap.put("minorEdit", "true");
        // Version 1.3
        testUtils.gotoPage(testClassName, pageName, "save", queryMap);

        ViewPage viewPage = testUtils.gotoPage(testClassName, pageName);
        // Add objects.
        ObjectEditPage objectEditPage = viewPage.editObjects();
        FormContainerElement form = objectEditPage.addObject("XWiki.JavaScriptExtension");
        Map<String, String> assignment = new HashMap<>();
        assignment.put("XWiki.JavaScriptExtension_0_name", "JavaScript code");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = bob;\nbob = tmp;");
        assignment.put("XWiki.JavaScriptExtension_0_use", "onDemand");
        form.fillFieldsByName(assignment);
        // Version 1.4
        objectEditPage.clickSaveAndContinue();
        assignment.put("XWiki.JavaScriptExtension_0_name", "Code snippet");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = 2 * bob;\nbob = tmp;");
        form.fillFieldsByName(assignment);
        // Version 1.5
        objectEditPage.clickSaveAndContinue();

        // Create class.
        ClassEditPage classEditPage = objectEditPage.editClass();
        // Version 1.6
        classEditPage.addProperty("age", "Number");
        // Version 1.7
        classEditPage.addProperty("color", "String");
        classEditPage.getNumberClassEditElement("age").setNumberType("integer");
        // Version 1.8
        classEditPage.clickSaveAndContinue();
        // Version 1.9
        classEditPage.deleteProperty("color");
        // Version 1.10
        this.testPage = classEditPage.clickSaveAndView();

        // Add tags.
        TaggablePage taggablePage = new TaggablePage();
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags("foo,bar");
        // Version 1.11
        addTagsPane.add();
        // Version 1.12
        taggablePage.removeTag("foo");

        // Attach files.
        AttachmentsPane attachmentsPane = this.testPage.openAttachmentsDocExtraPane();
        // TODO: Update this code when we (re)add support for uploading multiple files at once.
        // Version 2.1, 3.1, 4.1
        for (String fileName : new String[] {"SmallAttachment.txt", "SmallAttachment2.txt", "SmallAttachment.txt"}) {
            attachmentsPane.setFileToUpload(this.getClass().getResource('/' + fileName).getPath());
            attachmentsPane.waitForUploadToFinish(fileName);
            attachmentsPane.clickHideProgress();
        }
        // Version 5.1
        attachmentsPane.deleteAttachmentByFileByName("SmallAttachment2.txt");

        // Add comments.
        testUtils.createUserAndLogin("Alice", "ecila");
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        CommentsTab commentsTab = this.testPage.openCommentsDocExtraPane();
        // Version 5.2
        commentsTab.postComment("first line\nsecond line", true);
        commentsTab.editCommentByID(0, "first line\nline in between\nsecond line");

        // Version 5.5
        commentsTab.replyToCommentByID(0, "this is a reply");
        commentsTab.deleteCommentByID(1);
    }

    @AfterAll
    void tearDownClass(TestUtils testUtils)
    {
        // Put back the default hierarchy mode
        testUtils.setHierarchyMode("reference");
    }

    @Test
    @Order(1)
    void testCompareVersionScenario(TestUtils testUtils, TestReference testReference)
    {
        testAllChanges(testUtils, testReference);
        testVersionNavigation(testUtils, testReference);
        testUnifiedDiffShowsInlineChanges(testUtils, testReference);
        testNoChanges(testUtils, testReference);
        testDeleteVersions(testUtils, testReference);
    }

    private void testDeleteVersions(TestUtils testUtils, TestReference testReference)
    {
        testUtils.loginAsAdmin();
        String testClassName = testReference.getClass().getSimpleName();
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        HistoryPane historyTab = this.testPage.openHistoryDocExtraPane().showMinorEdits();
        historyTab.deleteRangeVersions("1.3", "5.4");
        String queryString = "viewer=changes&rev1=1.1&rev2=1.2";
        testUtils.gotoPage(testClassName, this.testPage.getMetaDataValue("page"), "view", queryString);
        ChangesPane changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());
        changesPane.clickNextChange();

        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.5", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertFalse(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertFalse(changesPane.hasNextToVersion());
    }

    /**
     * Tests that all changes are displayed.
     */
    private void testAllChanges(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getClass().getSimpleName();
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        HistoryPane historyTab = this.testPage.openHistoryDocExtraPane().showMinorEdits();
        String currentVersion = historyTab.getCurrentVersion();

        // TODO: If the document has many versions, like in this case, the versions are paginated and currently there's
        // no way to compare two versions from two different pagination pages using the UI. Thus we have to build the
        // URL and load the compare page manually. Update the code when we remove this UI limitation.
        // ChangesPane changesPane = historyTab.compare("1.1", currentVersion).getChangesPane();
        String queryString = String.format("viewer=changes&rev1=1.1&rev2=%s", currentVersion);
        testUtils.gotoPage(testClassName, this.testPage.getMetaDataValue("page"), "view", queryString);
        ChangesPane changesPane = new ChangesPane();

        // Version summary.
        String today = new SimpleDateFormat("yyyy/MM/dd").format(new Date());
        assertTrue(changesPane.getFromVersionSummary().startsWith(
            "From version 1.1 >\nedited by Administrator\non " + today));
        assertTrue(changesPane.getToVersionSummary().startsWith(
            "To version < " + currentVersion + "\nedited by Alice\non " + today));
        assertEquals("Change comment: Deleted object", changesPane.getChangeComment());

        // Diff summary.
        DocumentDiffSummary diffSummary = changesPane.getDiffSummary();
        assertThat(Arrays.asList("Page properties", "Attachments", "Objects", "Class properties"),
            containsInAnyOrder(diffSummary.getItems().toArray()));
        assertEquals("(5 modified, 0 added, 0 removed)", diffSummary.getPagePropertiesSummary());
        assertEquals("(0 modified, 1 added, 0 removed)", diffSummary.getAttachmentsSummary());
        assertEquals("(0 modified, 2 added, 0 removed)", diffSummary.getObjectsSummary());
        assertEquals("(0 modified, 1 added, 0 removed)", diffSummary.getClassPropertiesSummary());
        assertEquals(singletonList("SmallAttachment.txt"), diffSummary.toggleAttachmentsDetails().getAddedAttachments());
        assertEquals(Arrays.asList("XWiki.JavaScriptExtension[0]", "XWiki.XWikiComments[0]"), diffSummary
            .toggleObjectsDetails().getAddedObjects());
        assertEquals(singletonList("age"), diffSummary.toggleClassPropertiesDetails().getAddedClassProperties());

        // Diff details.
        assertThat(Arrays.asList("Page properties", "SmallAttachment.txt", "XWiki.JavaScriptExtension[0]",
            "XWiki.XWikiComments[0]", "age"), containsInAnyOrder(changesPane.getChangedEntities().toArray()));

        // Page properties changes.
        EntityDiff pageProperties = changesPane.getEntityDiff("Page properties");
        assertThat(Arrays.asList("Title", "Parent", "Author", "Tags", "Content"),
            containsInAnyOrder(pageProperties.getPropertyNames().toArray()));
        assertDiff(pageProperties.getDiff("Title"), "-<del>T</del>est",
            "+<ins>Compar</ins>e<ins> ver</ins>s<ins>ions </ins>t<ins>est</ins>");
        assertDiff(pageProperties.getDiff("Parent"), "+Sandbox.WebHome");
        assertDiff(pageProperties.getDiff("Author"), "-XWiki.A<del>dm</del>i<del>n</del>",
            "+XWiki.A<ins>l</ins>i<ins>ce</ins>");
        assertDiff(pageProperties.getDiff("Tags"), "+bar");
        assertDiff(pageProperties.getDiff("Content"), "@@ -1,3 +1,3 @@", " one", "-two",
            "-<del>th</del>r<del>ee</del>", "+<ins>**</ins>two<ins>**</ins>", "+<ins>fou</ins>r");

        // Attachment changes.
        EntityDiff attachmentDiff = changesPane.getEntityDiff("SmallAttachment.txt");
        assertThat(Arrays.asList("Author", "Size", "Content"),
            containsInAnyOrder(attachmentDiff.getPropertyNames().toArray()));
        assertDiff(attachmentDiff.getDiff("Author"), "+XWiki.Admin");
        assertDiff(attachmentDiff.getDiff("Size"), "+27 bytes");
        assertDiff(attachmentDiff.getDiff("Content"), "+This is a small attachment.");

        // Object changes.
        EntityDiff jsxDiff = changesPane.getEntityDiff("XWiki.JavaScriptExtension[0]");
        assertThat(Arrays.asList("Caching policy", "Name", "Use this extension", "Code"),
            containsInAnyOrder(jsxDiff.getPropertyNames().toArray()));
        assertDiff(jsxDiff.getDiff("Caching policy"), "+long");
        assertDiff(jsxDiff.getDiff("Name"), "+Code snippet");
        assertDiff(jsxDiff.getDiff("Use this extension"), "+onDemand");
        assertDiff(jsxDiff.getDiff("Code"), "+var tmp = alice;", "+alice = 2 * bob;", "+bob = tmp;");

        // Comment changes.
        EntityDiff commentDiff = changesPane.getEntityDiff("XWiki.XWikiComments[0]");
        assertThat(Arrays.asList("Author", "Date", "Comment"),
            containsInAnyOrder(commentDiff.getPropertyNames().toArray()));
        assertDiff(commentDiff.getDiff("Author"), "+XWiki.Alice");
        assertEquals(2, commentDiff.getDiff("Date").size());
        assertDiff(commentDiff.getDiff("Comment"), "+first line", "+line in between", "+second line");

        // Class property changes.
        EntityDiff ageDiff = changesPane.getEntityDiff("age");
        assertThat(Arrays.asList("Name", "Number", "Pretty Name", "Size", "Number Type"),
            containsInAnyOrder(ageDiff.getPropertyNames().toArray()));
        assertDiff(ageDiff.getDiff("Name"), "+age");
        assertDiff(ageDiff.getDiff("Number"), "+1");
        assertDiff(ageDiff.getDiff("Pretty Name"), "+age");
        assertDiff(ageDiff.getDiff("Size"), "+30");
        assertDiff(ageDiff.getDiff("Number Type"), "+integer");
    }

    /**
     * Tests that a message is displayed when there are no changes.
     */
    private void testNoChanges(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getClass().getSimpleName();
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        HistoryPane historyTab = this.testPage.openHistoryDocExtraPane();
        String currentVersion = historyTab.getCurrentVersion();
        assertTrue(historyTab.compare(currentVersion, currentVersion).getChangesPane().hasNoChanges());
    }

    /**
     * Tests that the unified diff (for multi-line text) shows the inline changes.
     */
    private void testUnifiedDiffShowsInlineChanges(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getClass().getSimpleName();
        this.testPage = testUtils.gotoPage(testClassName, pageName);
        ChangesPane changesPane =
            this.testPage.openHistoryDocExtraPane().showMinorEdits().compare("1.4", "1.5").getChangesPane();
        EntityDiff jsxDiff = changesPane.getEntityDiff("XWiki.JavaScriptExtension[0]");
        assertDiff(jsxDiff.getDiff("Code"), "@@ -1,3 +1,3 @@", " var tmp = alice;", "-alice = bob;",
            "+alice = <ins>2 * </ins>bob;", " bob = tmp;");
    }

    /**
     * Tests navigation between versions.
     */
    private void testVersionNavigation(TestUtils testUtils, TestReference testReference)
    {
        String testClassName = testReference.getClass().getSimpleName();
        String queryString = "viewer=changes&rev1=1.2&rev2=5.4";
        testUtils.gotoPage(testClassName, this.testPage.getMetaDataValue("page"), "view", queryString);
        ChangesPane changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.4", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.1", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.3", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        assertEquals("1.4", changesPane.getFromVersion());
        assertEquals("1.3", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals("1.3", changesPane.getFromVersion());
        assertEquals("1.2", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        queryString = "viewer=changes&rev1=1.9&rev2=2.1";
        testUtils.gotoPage(testClassName, this.testPage.getMetaDataValue("page"), "view", queryString);
        changesPane = new ChangesPane();
        assertEquals("1.9", changesPane.getFromVersion());
        assertEquals("2.1", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals("2.1", changesPane.getFromVersion());
        assertEquals("3.1", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        queryString = "viewer=changes&rev1=1.2&rev2=5.4";
        testUtils.gotoPage(testClassName, this.testPage.getMetaDataValue("page"), "view", queryString);
        changesPane.clickNextToVersion();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.5", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertFalse(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertFalse(changesPane.hasNextToVersion());

        changesPane.clickPreviousToVersion();
        changesPane = new ChangesPane();
        assertEquals("1.2", changesPane.getFromVersion());
        assertEquals("5.4", changesPane.getToVersion());
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());
    }

    private void assertDiff(List<String> actualLines, String... expectedLines)
    {
        if (expectedLines.length > 0 && !expectedLines[0].startsWith("@@")) {
            assertEquals(Arrays.asList(expectedLines), actualLines.subList(1, actualLines.size()));
        } else {
            assertEquals(Arrays.asList(expectedLines), actualLines);
        }
    }
}

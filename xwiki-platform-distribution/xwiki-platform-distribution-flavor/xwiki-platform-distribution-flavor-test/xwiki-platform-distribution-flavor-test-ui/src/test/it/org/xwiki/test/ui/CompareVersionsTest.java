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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.attachment.test.po.AttachmentsPane;
import org.xwiki.attachment.test.po.PageWithAttachmentPane;
import org.xwiki.tag.test.po.AddTagsPane;
import org.xwiki.tag.test.po.TaggablePage;
import org.xwiki.test.ui.po.ChangesPane;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.FormElement;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

/**
 * Tests the comparison of document versions.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class CompareVersionsTest extends AbstractTest
{
    @Rule
    public AdminAuthenticationRule adminAuthenticationRule = new AdminAuthenticationRule(true, getUtil());

    /**
     * The test page.
     */
    private ViewPage testPage;

    @Before
    public void setUp() throws Exception
    {
        String pageName = "PageWithManyVersions";

        // Check if the test page exists.
        testPage = getUtil().gotoPage(getTestClassName(), pageName);
        if (testPage.exists()) {
            // TODO: Remove when XWIKI-6688 (Possible race condition when clicking on a tab at the bottom of a page in
            // view mode) is fixed.
            testPage.waitForDocExtraPaneActive("comments");
            // NOTE: We use the same page for all tests because tests don't modify the test page. They only compare two
            // versions of the test page.
            return;
        }

        // Since this test also verifies diffs when the parent/child relationship is modified, use this mode (which
        // isn't the default) for testing purpose. Also note that in the "reference" mode there's no diff to display
        // when the document is moved to a different parent since it means changing the identity of the document for
        // now and thus changing it means getting a new document.
        getUtil().setHierarchyMode("parentchild");
        getDriver().navigate().refresh();

        // Create the test page.
        testPage = getUtil().createPage(getTestClassName(), pageName, "one\ntwo\nthree", "Test");

        // Change the content and the meta data.
        WikiEditPage wikiEditPage = testPage.editWiki();
        wikiEditPage.setContent("one\n**two**\nfour");
        wikiEditPage.setTitle("Compare verSions test");
        wikiEditPage.setParent("Sandbox.WebHome");
        wikiEditPage.setEditComment("Changed content and meta data.");
        wikiEditPage.clickSaveAndContinue();
        wikiEditPage.setTitle("Compare versions test");
        wikiEditPage.setMinorEdit(true);
        wikiEditPage.setEditComment("Fix typo in title.");
        wikiEditPage.clickSaveAndContinue();

        // Add objects.
        ObjectEditPage objectEditPage = wikiEditPage.editObjects();
        FormElement form = objectEditPage.addObject("XWiki.JavaScriptExtension");
        Map<String, String> assignment = new HashMap<String, String>();
        assignment.put("XWiki.JavaScriptExtension_0_name", "JavaScript code");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = bob;\nbob = tmp;");
        assignment.put("XWiki.JavaScriptExtension_0_use", "onDemand");
        form.fillFieldsByName(assignment);
        objectEditPage.clickSaveAndContinue();
        assignment.put("XWiki.JavaScriptExtension_0_name", "Code snippet");
        assignment.put("XWiki.JavaScriptExtension_0_code", "var tmp = alice;\nalice = 2 * bob;\nbob = tmp;");
        form.fillFieldsByName(assignment);
        objectEditPage.clickSaveAndContinue();

        // Create class.
        ClassEditPage classEditPage = objectEditPage.editClass();
        classEditPage.addProperty("age", "Number");
        classEditPage.addProperty("color", "String");
        classEditPage.getNumberClassEditElement("age").setNumberType("integer");
        classEditPage.clickSaveAndContinue();
        classEditPage.deleteProperty("color");
        testPage = classEditPage.clickSaveAndView();

        // Add tags.
        TaggablePage taggablePage = new TaggablePage();
        AddTagsPane addTagsPane = taggablePage.addTags();
        addTagsPane.setTags("foo,bar");
        addTagsPane.add();
        taggablePage.removeTag("foo");

        PageWithAttachmentPane pageWithAttachmentPane = new PageWithAttachmentPane();
        // Attach files.
        AttachmentsPane attachmentsPane = pageWithAttachmentPane.openAttachmentsDocExtraPane();
        // TODO: Update this code when we (re)add support for uploading multiple files at once.
        for (String fileName : new String[] {"SmallAttachment.txt", "SmallAttachment2.txt", "SmallAttachment.txt"}) {
            attachmentsPane.setFileToUpload(this.getClass().getResource('/' + fileName).getPath());
            attachmentsPane.waitForUploadToFinish(fileName);
            attachmentsPane.clickHideProgress();
        }
        attachmentsPane.deleteAttachmentByFileByName("SmallAttachment2.txt");

        // Add comments.
        getUtil().createUserAndLogin("Alice", "ecila");
        testPage = getUtil().gotoPage(getTestClassName(), pageName);
        CommentsTab commentsTab = testPage.openCommentsDocExtraPane();
        commentsTab.postComment("first line\nsecond line", true);
        commentsTab.editCommentByID(0, "first line\nline in between\nsecond line");
        commentsTab.replyToCommentByID(0, "this is a reply");
        commentsTab.deleteCommentByID(1);
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        // Put back the default hierarchy mode
        getUtil().setHierarchyMode("reference");
        getDriver().navigate().refresh();
    }

    /**
     * Tests that all changes are displayed.
     */
    @Test
    public void testAllChanges()
    {
        HistoryPane historyTab = testPage.openHistoryDocExtraPane().showMinorEdits();
        String currentVersion = historyTab.getCurrentVersion();

        // TODO: If the document has many versions, like in this case, the versions are paginated and currently there's
        // no way to compare two versions from two different pagination pages using the UI. Thus we have to build the
        // URL and load the compare page manually. Update the code when we remove this UI limitation.
        // ChangesPane changesPane = historyTab.compare("1.1", currentVersion).getChangesPane();
        String queryString = String.format("viewer=changes&rev1=1.1&rev2=%s", currentVersion);
        getUtil().gotoPage(getTestClassName(), testPage.getMetaDataValue("page"), "view", queryString);
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
        assertEquals(Arrays.asList("SmallAttachment.txt"), diffSummary.toggleAttachmentsDetails().getAddedAttachments());
        assertEquals(Arrays.asList("XWiki.JavaScriptExtension[0]", "XWiki.XWikiComments[0]"), diffSummary
            .toggleObjectsDetails().getAddedObjects());
        assertEquals(Arrays.asList("age"), diffSummary.toggleClassPropertiesDetails().getAddedClassProperties());

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

        testVersionNavigation();
    }

    /**
     * Tests that a message is displayed when there are no changes.
     */
    @Test
    public void testNoChanges()
    {
        HistoryPane historyTab = testPage.openHistoryDocExtraPane();
        String currentVersion = historyTab.getCurrentVersion();
        assertTrue(historyTab.compare(currentVersion, currentVersion).getChangesPane().hasNoChanges());
    }

    /**
     * Tests that the unified diff (for multi-line text) shows the inline changes.
     */
    @Test
    public void testUnifiedDiffShowsInlineChanges()
    {
        ChangesPane changesPane =
            testPage.openHistoryDocExtraPane().showMinorEdits().compare("2.2", "2.3").getChangesPane();
        EntityDiff jsxDiff = changesPane.getEntityDiff("XWiki.JavaScriptExtension[0]");
        assertDiff(jsxDiff.getDiff("Code"), "@@ -1,3 +1,3 @@", " var tmp = alice;", "-alice = bob;",
            "+alice = <ins>2 * </ins>bob;", " bob = tmp;");
    }

    /**
     * Tests navigation between versions.
     */
    private void testVersionNavigation()
    {
        String queryString = "viewer=changes&rev1=1.2&rev2=6.4";
        getUtil().gotoPage(getTestClassName(), testPage.getMetaDataValue("page"), "view", queryString);
        ChangesPane changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.2");
        assertEquals(changesPane.getToVersion(), "6.4");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.1");
        assertEquals(changesPane.getToVersion(), "1.2");
        assertFalse(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertFalse(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.2");
        assertEquals(changesPane.getToVersion(), "1.3");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousFromVersion();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.1");
        assertEquals(changesPane.getToVersion(), "1.3");
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
        assertEquals(changesPane.getFromVersion(), "1.3");
        assertEquals(changesPane.getToVersion(), "1.3");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextFromVersion();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "2.1");
        assertEquals(changesPane.getToVersion(), "1.3");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickPreviousChange();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.3");
        assertEquals(changesPane.getToVersion(), "1.2");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        changesPane.clickNextChange();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "2.1");
        assertEquals(changesPane.getToVersion(), "1.3");
        assertTrue(changesPane.hasPreviousChange());
        assertTrue(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertTrue(changesPane.hasNextToVersion());

        queryString = "viewer=changes&rev1=1.2&rev2=6.4";
        getUtil().gotoPage(getTestClassName(), testPage.getMetaDataValue("page"), "view", queryString);
        changesPane.clickNextToVersion();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.2");
        assertEquals(changesPane.getToVersion(), "6.5");
        assertTrue(changesPane.hasPreviousChange());
        assertFalse(changesPane.hasNextChange());
        assertTrue(changesPane.hasPreviousFromVersion());
        assertTrue(changesPane.hasNextFromVersion());
        assertTrue(changesPane.hasPreviousToVersion());
        assertFalse(changesPane.hasNextToVersion());

        changesPane.clickPreviousToVersion();
        changesPane = new ChangesPane();
        assertEquals(changesPane.getFromVersion(), "1.2");
        assertEquals(changesPane.getToVersion(), "6.4");
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

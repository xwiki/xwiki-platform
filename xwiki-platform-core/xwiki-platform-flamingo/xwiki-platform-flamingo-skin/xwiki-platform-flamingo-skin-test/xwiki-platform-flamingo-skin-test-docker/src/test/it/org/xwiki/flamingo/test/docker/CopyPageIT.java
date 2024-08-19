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
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.index.tree.test.po.DocumentPickerModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestLocalReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.CopyOverwritePromptPage;
import org.xwiki.test.ui.po.CopyPage;
import org.xwiki.test.ui.po.DocumentPicker;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@UITest
class CopyPageIT
{
    private static final String PAGE_CONTENT = "This page is used for copying purposes";

    private static final String OVERWRITTEN_PAGE_CONTENT = "This page is used for overwritten copy purposes";

    private static final String COPY_SUCCESSFUL = "Done.";

    private static final String OVERWRITE_PROMPT1 = "Warning:\nThe page ";

    private static final String OVERWRITE_PROMPT2 =
        " already exists. Are you sure you want to overwrite it (all its content would be lost)?";

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    private List<String> getLocation(WikiReference wiki, String space, String page)
    {
        return wiki.getName().equals("xwiki") ? Arrays.asList("", space, page)
            : Arrays.asList("", wiki.getName(), space, page);
    }

    @ParameterizedTest
    @WikisSource(extensions = {"org.xwiki.platform:xwiki-platform-index-tree-macro"})
    void copyPage(WikiReference wiki, TestLocalReference testReference, TestUtils setup,
        TestConfiguration testConfiguration) throws Exception
    {
        // Make sure to be on the right wiki
        setup.gotoPage(new DocumentReference(testReference, wiki));

        String sourceSpaceName = testReference.getParent().getParent().getName();
        String sourcePageName = testReference.getParent().getName();
        String targetSpaceName = sourceSpaceName + "Copy";
        String targetPageName = sourcePageName + "Copy";

        // Delete page that may already exist
        setup.rest().deletePage(sourceSpaceName, sourcePageName);
        setup.rest().deletePage(targetSpaceName, targetPageName);

        // Create a new page that will be copied.
        ViewPage viewPage = setup.createPage(sourceSpaceName, sourcePageName, PAGE_CONTENT, sourcePageName);

        // Add an attachment to verify that it's version is not incremented in the target document (XWIKI-8157).
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        File image = new File(testConfiguration.getBrowser().getTestResourcesPath(), "AttachmentIT/image.gif");
        attachmentsPane.setFileToUpload(image.getAbsolutePath());
        attachmentsPane.waitForUploadToFinish("image.gif");
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("image.gif"));

        // Click on Copy from the Page top menu.
        viewPage.copy();
        CopyPage copyPage = new CopyPage();

        // Check the source document
        assertEquals(getLocation(wiki, sourceSpaceName, sourcePageName), copyPage.getSourceLocation().getPath());
        assertEquals(sourceSpaceName, copyPage.getSourceSpaceName());
        assertEquals(sourcePageName, copyPage.getSourcePageName());

        // Check the default target document.
        DocumentPicker documentPicker = copyPage.getDocumentPicker();
        assertEquals(sourcePageName, documentPicker.getTitle());
        assertEquals(getLocation(wiki, sourceSpaceName, sourcePageName), documentPicker.getLocation().getPath());
        assertEquals(sourceSpaceName, copyPage.getTargetSpaceName());
        assertEquals(sourcePageName, copyPage.getTargetPageName());

        // Fill the target destination the page to be copied to.
        documentPicker.setTitle(targetPageName);
        documentPicker.waitForName(targetPageName);
        documentPicker.waitForLocation(getLocation(wiki, sourceSpaceName, targetPageName));
        // The target page name is updated based on the entered title.
        assertEquals(targetPageName, copyPage.getTargetPageName());
        // Select a new parent document.
        documentPicker.toggleLocationAdvancedEdit().setParent(targetSpaceName);
        documentPicker.waitForLocation(getLocation(wiki, targetSpaceName, targetPageName));

        // Click copy button
        CopyOrRenameOrDeleteStatusPage copyStatusPage = copyPage.clickCopyButton().waitUntilFinished();

        // Check successful copy confirmation
        assertEquals(COPY_SUCCESSFUL, copyStatusPage.getInfoMessage());
        viewPage = copyStatusPage.gotoNewPage();

        assertEquals(getLocation(wiki, targetSpaceName, targetPageName), viewPage.getBreadcrumb().getPath());
        // Verify that the copied title is modified to be the new page name since it was set to be the page name
        // originally.
        assertEquals(targetPageName, viewPage.getDocumentTitle());
        assertEquals(PAGE_CONTENT, viewPage.getContent());

        // Verify the attachment version is the same (XWIKI-8157).
        // FIXME: Remove the following wait when XWIKI-6688 is fixed.
        viewPage.waitForDocExtraPaneActive("comments");
        attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertEquals("1.1", attachmentsPane.getLatestVersionOfAttachment("image.gif"));
    }

    @ParameterizedTest
    @WikisSource(extensions = {"org.xwiki.platform:xwiki-platform-index-tree-macro"})
    void copyOverwritePage(WikiReference wiki, TestLocalReference testReference, TestUtils setup) throws Exception
    {
        // Make sure to be on the right wiki
        setup.gotoPage(new DocumentReference(testReference, wiki));

        String sourceSpaceName = testReference.getParent().getParent().getName();
        String sourcePageName = testReference.getParent().getName();
        String targetSpaceName = sourceSpaceName + "Copy";
        String targetPageName = sourcePageName + "Copy";

        // Delete page that may already exist
        setup.rest().deletePage(sourceSpaceName, sourcePageName);
        setup.rest().deletePage(targetSpaceName, targetPageName);

        // Create a new page that will be overwritten.
        setup.rest().savePage(new LocalDocumentReference(targetSpaceName, targetPageName), OVERWRITTEN_PAGE_CONTENT,
            targetPageName);

        // Create a new page that will be copied.
        ViewPage viewPage = setup.createPage(sourceSpaceName, sourcePageName, PAGE_CONTENT, sourcePageName);

        // Click on Copy from the Page top menu.
        viewPage.copy();
        CopyPage copyPage = new CopyPage();

        // We are copying a terminal page so the checkbox should be checked.
        assertTrue(copyPage.isTerminal());

        // Fill the target destination the page to be copied to.
        DocumentPicker documentPicker = copyPage.getDocumentPicker();
        documentPicker.browseDocuments();
        DocumentPickerModal documentPickerModal = new DocumentPickerModal();
        documentPickerModal.waitForDocumentSelected(sourceSpaceName, "WebHome").selectDocument(targetSpaceName,
            "WebHome");
        documentPicker.setTitle(targetPageName);
        documentPicker.waitForName(targetPageName);
        documentPicker.waitForLocation(getLocation(wiki, targetSpaceName, targetPageName));
        assertEquals(targetSpaceName, copyPage.getTargetSpaceName());
        assertEquals(targetPageName, copyPage.getTargetPageName());

        // Click copy button
        CopyOverwritePromptPage copyOverwritePrompt = copyPage.clickCopyButtonExpectingOverwritePrompt();

        // Check overwrite warning
        assertEquals(OVERWRITE_PROMPT1 + targetSpaceName + '.' + targetPageName + OVERWRITE_PROMPT2,
            copyOverwritePrompt.getWarningMessage());

        // Cancel the copy
        viewPage = copyOverwritePrompt.clickCancelButton();

        // Verify that we have been sent back to the original page
        assertEquals(sourcePageName, viewPage.getDocumentTitle());

        // Click on Copy from the Page top menu.
        viewPage.copy();
        copyPage = new CopyPage();

        // Fill the target destination the page to be copied to.
        copyPage.getDocumentPicker().toggleLocationAdvancedEdit().setParent(targetSpaceName).setName(targetPageName);

        // Click copy button
        copyOverwritePrompt = copyPage.clickCopyButtonExpectingOverwritePrompt();

        // Click change target
        copyOverwritePrompt.clickChangeTargetButton();
        copyPage = new CopyPage();

        // Check the source document
        assertEquals(getLocation(wiki, sourceSpaceName, sourcePageName), copyPage.getSourceLocation().getPath());
        assertEquals(sourceSpaceName, copyPage.getSourceSpaceName());
        assertEquals(sourcePageName, copyPage.getSourcePageName());

        // Check the default target document.
        documentPicker = copyPage.getDocumentPicker();
        assertEquals(sourcePageName, documentPicker.getTitle());
        assertEquals(getLocation(wiki, sourceSpaceName, sourcePageName), documentPicker.getLocation().getPath());
        assertEquals(sourceSpaceName, copyPage.getTargetSpaceName());
        assertEquals(sourcePageName, copyPage.getTargetPageName());

        // Fill the target destination the page to be copied to.
        documentPicker.setTitle("My copy").toggleLocationAdvancedEdit().setParent(targetSpaceName)
            .setName(targetPageName);

        // Copy and confirm overwrite
        copyOverwritePrompt = copyPage.clickCopyButtonExpectingOverwritePrompt();
        CopyOrRenameOrDeleteStatusPage copyStatusPage = copyOverwritePrompt.clickCopyButton().waitUntilFinished();

        // Check successful copy confirmation
        assertEquals(COPY_SUCCESSFUL, copyStatusPage.getInfoMessage());
        viewPage = copyStatusPage.gotoNewPage();

        // Verify that the title of the copy has been updated (independent from the name of the copy).
        assertEquals("My copy", viewPage.getDocumentTitle());
        assertEquals(PAGE_CONTENT, viewPage.getContent());
    }
}

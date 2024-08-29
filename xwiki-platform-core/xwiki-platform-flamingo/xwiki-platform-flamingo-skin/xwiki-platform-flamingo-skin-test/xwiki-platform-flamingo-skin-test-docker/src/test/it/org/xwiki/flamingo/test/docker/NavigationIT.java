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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.flamingo.skin.test.po.AttachmentsViewPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CommentsTab;
import org.xwiki.test.ui.po.HistoryPane;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests related to navigation in the wiki.
 * 
 * @since 11.10
 * @version $Id$
 */
@UITest
public class NavigationIT
{
    @BeforeAll
    public void beforeAll(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    @BeforeEach
    public void setup(TestUtils testUtils, TestReference testReference)
    {
        testUtils.deletePage(testReference);
        testUtils.createPage(testReference, "Some dumb content", "Just a title");
    }

    @Test
    @Order(1)
    public void navigateWithKeyboardShortcuts(TestUtils testUtils, TestReference testReference)
    {
        DocumentReference terminalPageReference = new DocumentReference("Foo", testReference.getLastSpaceReference());
        testUtils.deletePage(terminalPageReference);
        testUtils.createPage(terminalPageReference, "Terminal page content", "Terminal page title");

        testUtils.gotoPage(testReference);
        ViewPage viewPage = new ViewPage();

        // Test default edit mode (Wiki for Sandbox.WebHome) key
        EditPage editPage = viewPage.useShortcutKeyForEditing();
        assertTrue(testUtils.isInWikiEditMode());

        // Test Cancel key
        viewPage = editPage.useShortcutKeyForCancellingEdition();
        assertTrue(testUtils.isInViewMode());

        // Test Wiki edit key
        viewPage.useShortcutKeyForWikiEditing();
        assertTrue(testUtils.isInWikiEditMode());

        // Test WYSIWYG edit mode key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForWysiwygEditing();
        assertTrue(testUtils.isInWYSIWYGEditMode());

        // Test Inline Form edit mode key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForInlineEditing();
        assertTrue(testUtils.isInInlineEditMode());

        // Test Rights edit mode key on a terminal document
        viewPage = testUtils.gotoPage(terminalPageReference);
        viewPage.useShortcutKeyForRightsEditing();
        assertTrue(testUtils.isInRightsEditMode());

        // Test Rights edit mode key on a non terminal document
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForRightsEditing();
        assertTrue(testUtils.isInAdminMode());
        AdministrationPage administrationPage = new AdministrationPage();
        assertTrue(administrationPage.hasSection("PageRights"));

        // Test Object edit mode key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForObjectEditing();
        assertTrue(testUtils.isInObjectEditMode());

        // Test Class edit mode key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForClassEditing();
        assertTrue(testUtils.isInClassEditMode());

        // Test Delete key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForPageDeletion();
        assertTrue(testUtils.isInDeleteMode());

        // Test Rename key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForPageRenaming();
        assertTrue(testUtils.isInRenameMode());

        // Test View Source key
        viewPage = testUtils.gotoPage(testReference);
        viewPage.useShortcutKeyForSourceViewer();
        assertTrue(testUtils.isInSourceViewMode());

        // Test all panes
        viewPage = testUtils.gotoPage(testReference);
        AttachmentsPane attachmentsPane = new AttachmentsViewPage().useShortcutKeyForAttachmentPane();
        assertTrue(attachmentsPane.isOpened());

        HistoryPane historyPane = viewPage.useShortcutKeyForHistoryPane();
        assertTrue(historyPane.isOpened());

        CommentsTab commentsTab = viewPage.useShortcutKeyForCommentPane();
        assertTrue(commentsTab.isOpened());

        InformationPane informationPane = viewPage.useShortcutKeyForInformationPane();
        assertTrue(informationPane.isOpened());
    }

    /**
     * Test document extras presence after a click on the corresponding tabs.
     */
    @Test
    @Order(2)
    public void docExtraLoadingFromTabClicks(TestUtils testUtils, TestReference testReference)
    {
        ViewPage viewPage = testUtils.gotoPage(testReference);

        AttachmentsPane attachmentsPane = new AttachmentsViewPage().openAttachmentsDocExtraPane();
        assertTrue(attachmentsPane.isOpened());

        HistoryPane historyPane = viewPage.openHistoryDocExtraPane();
        assertTrue(historyPane.isOpened());

        InformationPane informationPane = viewPage.openInformationDocExtraPane();
        assertTrue(informationPane.isOpened());

        CommentsTab commentsTab = viewPage.openCommentsDocExtraPane();
        assertTrue(commentsTab.isOpened());
    }

    /**
     * Test document extra presence when the user arrives from an URL with anchor. This test also verify that the
     * browser scrolls to the bottom of the page.
     */
    @Order(3)
    @Test
    public void docExtraLoadingFromURLAnchor(TestUtils testUtils, TestReference testReference)
    {
        LocalDocumentReference otherPageReference = new LocalDocumentReference("Main", "ThisPageDoesNotExist");
        List<String> docExtraPanes = Arrays.asList("attachments", "history", "information", "comments");

        ViewPage viewPage;
        for (String docExtraPane : docExtraPanes) {
            // We have to load a different page first since opening the same page with a new anchor doesn't call
            // our functions (on purpose).
            testUtils.gotoPage(otherPageReference);
            testUtils.gotoPage(testReference, "view", null, StringUtils.capitalize(docExtraPane));
            viewPage = new ViewPage();
            viewPage.waitForDocExtraPaneActive(docExtraPane);
        }
    }

    /**
     * Ensure that the base bin URL redirect to the main page of the wiki.
     */
    @Order(4)
    @Test
    public void simpleBinUrlDoesNotThrowException(TestUtils testUtils)
    {
        testUtils.gotoPage(testUtils.getBaseBinURL());
        ViewPage viewPage = new ViewPage();
        assertEquals("XWiki - Main - Main", viewPage.getPageTitle());
    }
}

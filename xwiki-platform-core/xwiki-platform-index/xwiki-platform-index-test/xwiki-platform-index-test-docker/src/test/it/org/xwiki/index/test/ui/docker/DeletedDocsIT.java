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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.index.test.po.DeletedDocsPage;
import org.xwiki.index.test.po.RestoreDocumentConfirmationModal;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DeletePageConfirmationPage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for Deleted Pages tab from Page Index (XWiki.AllDocs).
 *
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@UITest
class DeletedDocsIT
{
    @BeforeEach
    void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void restoreOriginalDocumentAfterDeletionAndRecreation(TestUtils testUtils, TestReference reference)
    {
        // Delete and create a new version of a document.
        ViewPage viewPage = testUtils.createPage(reference, "original", "Original page");
        DeletePageConfirmationPage confirmationPage = viewPage.deletePage();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals("Done.", deletingPage.getInfoMessage());
        testUtils.createPage(reference, "new", "New page");

        // Go to deleted pages and try to restore the original document. Filter the table first since the module
        // tests delete other pages, which could push this one to another page of the table.
        DeletedDocsPage deletedDocs = DeletedDocsPage.gotoPage();
        String docName = testUtils.serializeLocalReference(reference);
        deletedDocs.filterByDocName(docName);
        RestoreDocumentConfirmationModal confirmationModal = deletedDocs.tryReplaceDoc(docName);
        assertTrue(confirmationModal.isDisplayed());
        viewPage = confirmationModal.clickReplace();

        assertEquals("Original page", viewPage.getDocumentTitle());
        assertEquals("original", viewPage.getContent());
    }

    @Test
    @Order(2)
    void restoreDeletedPages(TestUtils setup, TestReference reference)
    {
        DocumentReference terminalReference = new DocumentReference("Terminal", reference.getLastSpaceReference());
        setup.deletePage(reference, true);
        setup.createPage(reference, "nested content", "Nested page");
        setup.createPage(terminalReference, "terminal content", "Terminal page");
        setup.deletePage(terminalReference);
        setup.deletePage(reference);

        // Restore both pages from the Deleted Pages tab. No page exists at their location so the plain "Restore"
        // action is displayed, and it redirects to the restored page.
        String nestedName = setup.serializeLocalReference(reference);
        String terminalName = setup.serializeLocalReference(terminalReference);
        String spaceName = setup.serializeLocalReference(reference.getLastSpaceReference());
        DeletedDocsPage deletedDocs = DeletedDocsPage.gotoPage();
        deletedDocs.filterByDocName(spaceName);
        assertTrue(deletedDocs.hasDoc(nestedName));
        assertTrue(deletedDocs.hasDoc(terminalName));
        ViewPage viewPage = deletedDocs.restoreDoc(nestedName);
        assertEquals("Nested page", viewPage.getDocumentTitle());
        assertEquals("nested content", viewPage.getContent());

        deletedDocs = DeletedDocsPage.gotoPage();
        deletedDocs.filterByDocName(spaceName);
        assertFalse(deletedDocs.hasDoc(nestedName));
        viewPage = deletedDocs.restoreDoc(terminalName);
        assertEquals("Terminal page", viewPage.getDocumentTitle());
        assertEquals("terminal content", viewPage.getContent());

        // Both pages are back at their original location and are no longer listed as deleted.
        deletedDocs = DeletedDocsPage.gotoPage();
        deletedDocs.filterByDocName(spaceName);
        assertFalse(deletedDocs.hasDoc(nestedName));
        assertFalse(deletedDocs.hasDoc(terminalName));
        assertEquals("nested content", setup.gotoPage(reference).getContent());
        assertEquals("terminal content", setup.gotoPage(terminalReference).getContent());
    }
}

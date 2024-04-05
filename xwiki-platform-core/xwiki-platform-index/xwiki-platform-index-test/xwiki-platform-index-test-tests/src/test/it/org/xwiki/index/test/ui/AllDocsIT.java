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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.index.test.po.AllDocsPage;
import org.xwiki.index.tree.test.po.DocumentTreeElement;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.tree.test.po.TreeNodeElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests for the AllDocs page.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class AllDocsIT extends AbstractTest
{
    @Before
    public void setUp() throws Exception
    {
        getUtil().loginAsSuperAdmin();
        getUtil().deleteSpace(getTestClassName());
    }

    @Test
    public void recycleBinTab() throws Exception
    {
        // Create a document and delete it
        getUtil().createPageWithAttachment(Arrays.asList(getTestClassName(), "DeleteableSpace"), "document",
            null, "Document", null, null, "file.txt",
            getClass().getResourceAsStream("/file.txt"), TestUtils.SUPER_ADMIN_CREDENTIALS);

        getUtil().loginAsSuperAdmin();
        getUtil().deleteSpace(getTestClassName());
        getUtil().gotoPage(AllDocsPage.getURL());

        AllDocsPage page = new AllDocsPage();
        assertTrue("Deleted documents tab is not visible to Admin", page.hasDeletedDocsTab());
        assertTrue("Deleted attachments tab is not visible to Admin", page.hasDeletedAttachmentsTab());

        // Logs out to be guest to verify that Deleted attachments/documents are not visible to guests
        getUtil().forceGuestUser();

        assertFalse("Deleted documents shouldn't be visible to guests", page.hasDeletedDocsTab());
        assertFalse("Deleted attachments shouldn't be visible to guests", page.hasDeletedAttachmentsTab());
    }

    /**
     * Validate the tree view.
     */
    @Test
    public void treeViewTab() throws Exception
    {
        // Create a tree structure.
        String spaceName = getTestClassName();
        getUtil().createPage(spaceName, "WebHome", null, null);
        getUtil().createPageWithAttachment(Arrays.asList(spaceName, "A", "B"), "C", null, "Child Page", null, null,
            "file.txt", getClass().getResourceAsStream("/file.txt"), TestUtils.SUPER_ADMIN_CREDENTIALS);

        DocumentTreeElement tree = AllDocsPage.gotoPage().clickTreeTab();

        TreeNodeElement root = tree.getDocumentNode(spaceName, "WebHome");
        assertEquals(spaceName, root.getLabel());

        assertFalse(tree.hasDocument(spaceName, "A", "WebHome"));
        root.open().waitForIt();
        TreeNodeElement alice = tree.getDocumentNode(spaceName, "A", "WebHome");
        assertEquals("A", alice.getLabel());

        assertFalse(tree.hasDocument(spaceName, "A", "B", "WebHome"));
        alice.open().waitForIt();
        TreeNodeElement bob = tree.getDocumentNode(spaceName, "A", "B", "WebHome");
        assertEquals("B", bob.getLabel());

        assertFalse(tree.hasDocument(spaceName, "A", "B", "C"));
        bob.open().waitForIt();
        TreeNodeElement child = tree.getDocumentNode(spaceName, "A", "B", "C");
        assertEquals("Child Page", child.getLabel());

        assertFalse(tree.hasAttachment(spaceName, "A", "B", "C", "file.txt"));
        // Open the Attachments node.
        child.open().waitForIt().getChildren().get(0).open().waitForIt();
        TreeNodeElement file = tree.getAttachmentNode(spaceName, "A", "B", "C", "file.txt");
        assertEquals("file.txt", file.getLabel());
    }

    /**
     * @see "XWIKI-5187: XWiki Explorer doesn't support very well spaces and pages with special characters in their
     * names"
     */
    @Test
    public void treeViewTabWithSpecialCharactersInEntityNames()
    {
        // Create a tree structure.
        String spaceName = getTestMethodName() + ".0";
        getUtil().createPage(Arrays.asList(spaceName, "Level.1", "Level{[(2)]}", "Level@3"), "End", null, null);

        DocumentTreeElement tree = AllDocsPage.gotoPage().clickTreeTab();
        tree.openToDocument(spaceName, "Level.1", "Level{[(2)]}", "Level@3", "End");

        assertTrue(tree.hasDocument(spaceName, "Level.1", "WebHome"));
        assertTrue(tree.hasDocument(spaceName, "Level.1", "Level{[(2)]}", "WebHome"));
        assertTrue(tree.hasDocument(spaceName, "Level.1", "Level{[(2)]}", "Level@3", "WebHome"));
    }

}

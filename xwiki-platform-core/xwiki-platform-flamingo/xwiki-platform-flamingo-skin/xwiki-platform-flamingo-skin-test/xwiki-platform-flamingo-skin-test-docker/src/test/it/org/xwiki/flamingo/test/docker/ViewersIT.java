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

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.ChildrenPage;
import org.xwiki.flamingo.skin.test.po.PinnedChildPagesTab;
import org.xwiki.flamingo.skin.test.po.SiblingsPage;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xwiki.security.internal.XWikiConstants.GUEST_USER;
import static org.xwiki.security.internal.XWikiConstants.XWIKI_SPACE;

/**
 * Test of the pages accessible from the viewers section of the pages hamburger menu. For instance, children or sibling
 * pages.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@UITest
class ViewersIT
{
    private static final String EDIT_USER = "ViewersITUser";

    /**
     * Creates a page with two children {@code ChildA} and {@code ChildB}. Then, visit the siblings listing of {@code
     * ChildB} and assert the content of the siblings Live Data as Guest. Finally, repeat the operation as superadmin to
     * validate that administration actions are also displayed.
     */
    @Test
    @Order(1)
    void siblings(TestUtils testUtils, TestReference testReference)
    {
        DocumentReference childADocumentReference =
            new DocumentReference("ChildA", testReference.getLastSpaceReference());
        DocumentReference childBDocumentReference =
            new DocumentReference("ChildB", testReference.getLastSpaceReference());

        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference, true);
        testUtils.forceGuestUser();

        testUtils.createPage(testReference, "");
        testUtils.createPage(childADocumentReference, "", "ChildA");
        testUtils.createPage(childBDocumentReference, "", "ChildB");

        TableLayoutElement guestTableLayoutElement = SiblingsPage.clickOnSiblingsMenu().getLiveData().getTableLayout();
        assertEquals(1, guestTableLayoutElement.countRows());
        guestTableLayoutElement
            .assertCellWithLink(SiblingsPage.LIVE_DATA_TITLE, "ChildA", testUtils.getURL(childADocumentReference));
        guestTableLayoutElement
            .assertRow(SiblingsPage.LIVE_DATA_DATE, hasItem(guestTableLayoutElement.getDatePatternMatcher()));
        guestTableLayoutElement.assertCellWithLink(SiblingsPage.LIVE_DATA_LAST_AUTHOR, GUEST_USER,
            testUtils.getURL(new DocumentReference("xwiki", XWIKI_SPACE, GUEST_USER)));
        guestTableLayoutElement.assertCellWithLink(SiblingsPage.LIVE_DATA_ACTIONS, "Copy",
            testUtils.getURL(childADocumentReference, "view", "xpage=copy"));

        // Visit the siblings page as superadmin to verify the administration actions too.
        testUtils.loginAsSuperAdmin();

        TableLayoutElement adminTableLayoutElement =
            SiblingsPage.goToPage(childBDocumentReference).getLiveData().getTableLayout();
        assertEquals(1, adminTableLayoutElement.countRows());
        adminTableLayoutElement
            .assertCellWithLink(SiblingsPage.LIVE_DATA_TITLE, "ChildA", testUtils.getURL(childADocumentReference));
        adminTableLayoutElement.assertCellWithLink(SiblingsPage.LIVE_DATA_ACTIONS, "Copy",
            testUtils.getURL(childADocumentReference, "view", "xpage=copy"));
        adminTableLayoutElement.assertCellWithLink(SiblingsPage.LIVE_DATA_ACTIONS, "Rename",
            testUtils.getURL(childADocumentReference, "view", "xpage=rename&step=1"));
        adminTableLayoutElement.assertCellWithDeleteAction(SiblingsPage.LIVE_DATA_ACTIONS, childADocumentReference);
    }

    /**
     * Creates a page with two children {@code ChildA} and {@code ChildB}. Then, visit the children listing of the
     * parent page and assert the content of the children Live Data as Guest. Finally, repeat the operation as
     * superadmin to validate that administration actions are also displayed.
     */
    @Test
    @Order(2)
    void children(TestUtils testUtils, TestReference testReference) throws Exception
    {
        DocumentReference childADocumentReference =
            new DocumentReference("ChildA", testReference.getLastSpaceReference());
        DocumentReference childBDocumentReference =
            new DocumentReference("ChildB", testReference.getLastSpaceReference());

        testUtils.loginAsSuperAdmin();
        testUtils.deletePage(testReference, true);
        testUtils.createPage(testReference, "");
        testUtils.createPage(childADocumentReference, "", "ChildA");
        testUtils.createPage(childBDocumentReference, "", "ChildB");
        testUtils.createUser(EDIT_USER, EDIT_USER, null);
        testUtils.loginAsSuperAdmin();
        testUtils.setRightsOnSpace(testReference.getLastSpaceReference(), null, EDIT_USER, "edit,delete", true);

        testUtils.forceGuestUser();
        testUtils.gotoPage(testReference);

        ChildrenPage childrenPage = ChildrenPage.clickOnChildrenMenu();
        assertFalse(childrenPage.hasTabs());
        TableLayoutElement guestTableLayoutElement = childrenPage.getLiveData().getTableLayout();
        assertEquals(2, guestTableLayoutElement.countRows());
        guestTableLayoutElement
            .assertCellWithLink(ChildrenPage.LIVE_DATA_TITLE, "ChildA", testUtils.getURL(childADocumentReference));
        guestTableLayoutElement
            .assertCellWithLink(ChildrenPage.LIVE_DATA_TITLE, "ChildB", testUtils.getURL(childBDocumentReference));
        guestTableLayoutElement
            .assertRow(ChildrenPage.LIVE_DATA_DATE, hasItem(guestTableLayoutElement.getDatePatternMatcher()));
        guestTableLayoutElement.assertCellWithLink(ChildrenPage.LIVE_DATA_LAST_AUTHOR, "superadmin",
            testUtils.getURL(new DocumentReference("xwiki", XWIKI_SPACE, "superadmin")));
        guestTableLayoutElement.assertCellWithLink(ChildrenPage.LIVE_DATA_ACTIONS, "Copy",
            testUtils.getURL(childADocumentReference, "view", "xpage=copy"));

        // Visit the children page as superadmin to verify the administration actions too.
        testUtils.loginAsSuperAdmin();
        childrenPage = ChildrenPage.goToPage(testReference);
        assertTrue(childrenPage.hasTabs());

        TableLayoutElement adminTableLayoutElement =
            ChildrenPage.goToPage(testReference).getLiveData().getTableLayout();
        // WebPreferences is visible by superadmin
        assertEquals(3, adminTableLayoutElement.countRows());
        adminTableLayoutElement
            .assertCellWithLink(ChildrenPage.LIVE_DATA_TITLE, "ChildA", testUtils.getURL(childADocumentReference));
        adminTableLayoutElement
            .assertCellWithLink(ChildrenPage.LIVE_DATA_TITLE, "ChildB", testUtils.getURL(childBDocumentReference));
        adminTableLayoutElement.assertCellWithLink(ChildrenPage.LIVE_DATA_ACTIONS, "Copy",
            testUtils.getURL(childADocumentReference, "view", "xpage=copy"));
        adminTableLayoutElement.assertCellWithLink(ChildrenPage.LIVE_DATA_ACTIONS, "Rename",
            testUtils.getURL(childADocumentReference, "view", "xpage=rename&step=1"));
        // Removes the form_token parameter from the expected URL because it is not found in the URL of the delete 
        // action.
        adminTableLayoutElement.assertCellWithDeleteAction(ChildrenPage.LIVE_DATA_ACTIONS, childADocumentReference);

        // test using pinned child pages with a simple user with edit rights
        testUtils.login(EDIT_USER, EDIT_USER);
        childrenPage = ChildrenPage.goToPage(testReference);
        assertTrue(childrenPage.hasTabs());

        PinnedChildPagesTab pinnedChildPagesTab = childrenPage.openPinnedChildPagesTab();
        assertEquals(List.of("ChildA", "ChildB"),
            pinnedChildPagesTab.getNavigationTree().getTopLevelPages());
        assertFalse(pinnedChildPagesTab.isPinned("ChildA"));
        assertFalse(pinnedChildPagesTab.isPinned("ChildB"));

        pinnedChildPagesTab.dragBefore("ChildB", "ChildA");
        assertTrue(pinnedChildPagesTab.isPinned("ChildB"));
        pinnedChildPagesTab.save();

        childrenPage = ChildrenPage.goToPage(testReference);
        pinnedChildPagesTab = childrenPage.openPinnedChildPagesTab();
        assertEquals(List.of("ChildB", "ChildA"), pinnedChildPagesTab.getNavigationTree().getTopLevelPages());
        assertTrue(pinnedChildPagesTab.isPinned("ChildB"));
        assertFalse(pinnedChildPagesTab.isPinned("ChildA"));

        pinnedChildPagesTab.unpinPage("ChildB");
        pinnedChildPagesTab.save();

        childrenPage = ChildrenPage.goToPage(testReference);
        pinnedChildPagesTab = childrenPage.openPinnedChildPagesTab();
        assertEquals(List.of("ChildA", "ChildB"), pinnedChildPagesTab.getNavigationTree().getTopLevelPages());
        assertFalse(pinnedChildPagesTab.isPinned("ChildA"));
        assertFalse(pinnedChildPagesTab.isPinned("ChildB"));

        pinnedChildPagesTab.pinPage("ChildB");
        assertTrue(pinnedChildPagesTab.isPinned("ChildA"));
        assertTrue(pinnedChildPagesTab.isPinned("ChildB"));
        pinnedChildPagesTab.save();

        testUtils.loginAsSuperAdmin();
        new SolrTestUtils(testUtils).waitEmptyQueue();
        childrenPage = ChildrenPage.goToPage(testReference);
        pinnedChildPagesTab = childrenPage.openPinnedChildPagesTab();
        assertEquals(List.of("ChildA", "ChildB", "WebPreferences"),
            pinnedChildPagesTab.getNavigationTree().getTopLevelPages());
        assertTrue(pinnedChildPagesTab.isPinned("ChildA"));
        assertTrue(pinnedChildPagesTab.isPinned("ChildB"));
        assertFalse(pinnedChildPagesTab.isPinned("WebPreferences"));

        testUtils.login(EDIT_USER, EDIT_USER);
        // Check that pinned page are preserved after a move
        RenamePage renamePage = testUtils.gotoPage(testReference).rename();
        renamePage.setPreserveChildren(true);
        renamePage.getDocumentPicker().setTitle("ChildrenRenamed");
        CopyOrRenameOrDeleteStatusPage statusPage = renamePage.clickRenameButton().waitUntilFinished();
        statusPage.gotoNewPage();
        childrenPage = ChildrenPage.clickOnChildrenMenu();

        pinnedChildPagesTab = childrenPage.openPinnedChildPagesTab();
        assertEquals(List.of("ChildA", "ChildB"), pinnedChildPagesTab.getNavigationTree().getTopLevelPages());
        assertTrue(pinnedChildPagesTab.isPinned("ChildA"));
        assertTrue(pinnedChildPagesTab.isPinned("ChildB"));
    }
}

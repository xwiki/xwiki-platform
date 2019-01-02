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

import static org.junit.Assert.*;

import org.junit.Rule;
import java.util.Arrays;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.CopyOrRenameOrDeleteStatusPage;
import org.xwiki.test.ui.po.RenamePage;
import org.xwiki.test.ui.po.ViewPage;

public class RenamePageTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private static final String RENAME_SUCCESSFUL = "Done.";

    // Convert a nested page to a terminal page
    @Test
    public void convertNestedPageToTerminalPage() throws Exception
    {
        // Clean-up: delete the pages that will be used in this test
        getUtil().rest().deletePage("1", "2");
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2.WebHome"));

        // Create 1.2.WebHome
        getUtil().createPage(Arrays.asList("1", "2"), "WebHome", "", "");

        // Go to 1.2.WebHome to start the test
        getUtil().gotoPage(Arrays.asList("1", "2"), "WebHome", "", "");

        ViewPage vp = new ViewPage();

        // Go to the Rename page view for 1.2.WebHome and check the Terminal checkbox. We also need to uncheck the Auto
        // Redirect checkbox so the page 1.2.WebHome will not appear as existing after the Rename operation.
        RenamePage renamePage = vp.rename();
        renamePage.setTerminal(true);
        renamePage.setAutoRedirect(false);
        renamePage.clickRenameButton().waitUntilFinished();

        // Test if 1.2.WebHome has been renamed to 1.2 (1.2.WebHome doesn't exist while 1.2 exists)
        assertTrue("Page 1.2 doesn't exist!", getUtil().pageExists(Arrays.asList("1"), "2"));
        assertFalse("Page 1.2.WebHome exists!", getUtil().pageExists(Arrays.asList("1", "2"), "WebHome"));
    }

    // Rename a page with children, update the backlinks and test the Auto Redirect feature
    @Test
    public void renamePageCheckConfirmationPreserveChildrenUpdateLinksSetAutoRedirect() throws Exception
    {
        // Clean-up: delete the pages that will be used in this test
        getUtil().rest().deletePage("My", "Page");
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("1.2.3.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("A.B.2.WebHome"));
        getUtil().rest().delete(getUtil().resolveDocumentReference("A.B.2.3.WebHome"));

        // Create the needed pages
        getUtil().createPage(Arrays.asList("1", "2"), "WebHome", "", "");
        getUtil().createPage(Arrays.asList("1", "2", "3"), "WebHome", "", "");
        // We set the content to "[[1.2.WebHome]]" to test the "Update Links" feature
        getUtil().createPage("My", "Page", "[[1.2.WebHome]]", "");

        // Go to 1.2.WebHome to start the test
        getUtil().gotoPage(Arrays.asList("1", "2"), "WebHome", "", "");

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
        assertEquals(RENAME_SUCCESSFUL, renameStatusPage.getInfoMessage());
        // Test the Rename operation: we need to have 2.WebHome and 2.3.WebHome under A.B
        assertTrue("Page A.B.2.WebHome doesn't exist!", getUtil().pageExists(Arrays.asList("A", "B", "2"), "WebHome"));
        assertTrue("Page A.B.2.3.WebHome doesn't exist!",
            getUtil().pageExists(Arrays.asList("A", "B", "2", "3"), "WebHome"));
        // Test the Auto Redirect: when visiting the original pages you need to be redirected to the new locations
        getUtil().gotoPage(Arrays.asList("1", "2"), "WebHome", "view", "");
        assertEquals("/A/B/2", vp.getBreadcrumbContent());
        getUtil().gotoPage(Arrays.asList("1", "2", "3"), "WebHome", "view", "");
        assertEquals("/A/B/2/3", vp.getBreadcrumbContent());
        // Test the Update Links feature: the content of the page needs to point to the new location
        assertEquals("[[A.B.2.WebHome]]", getUtil().gotoPage("My", "Page").editWiki().getContent());
    }
}

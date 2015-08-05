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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.CreateSpacePage;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.Assert.*;

/**
 * Tests basic page and space creation.
 *
 * @version $Id$
 * @since 7.2M2
 */
public class CreatePageAndSpaceTest extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void createSpace()
    {
        // Test 1:  Test Space creation when on an existing page (i.e. the space creation UI will open to ask for the
        //          space name.

        // Clean up before the test starts
        String pageName = getTestMethodName();
        String spaceName = getTestClassName();
        getUtil().deleteSpace(spaceName);
        getUtil().deletePage(spaceName, pageName);

        // Create the page that's supposed to exist.
        getUtil().createPage(spaceName, pageName, "Dummy", "Dummy Title");

        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it.
        getUtil().gotoPage(getUtil().getURL("create", new String[] {spaceName, pageName}, "tocreate=space"));
        CreateSpacePage csp = new CreateSpacePage();
        EditPage editSpaceWebhomePage = csp.createSpace(spaceName);

        assertEquals(spaceName, editSpaceWebhomePage.getMetaDataValue("space"));
        assertEquals("WebHome", editSpaceWebhomePage.getMetaDataValue("page"));
        // The parent is the current document from where the space was created.
        assertEquals(spaceName + "." + pageName, editSpaceWebhomePage.getParent());
        // and the title the name of the space
        assertEquals(spaceName, editSpaceWebhomePage.getDocumentTitle());

        // Test 2:  Test Space creation when on an non-existing page (i.e. the create action will lead to editing the
        //          current document - No space name will be asked).

        // Since the Flamingo skin no longer supports creating a space from the UI, trigger the Space creation UI
        // by using directly the direct action URL for it. This time on a non-exsiting page.
        getUtil().gotoPage(getUtil().getURL("create", new String[] {spaceName, "NonExistingPage"}, "tocreate=space"));
        EditPage editPage = new EditPage();

        assertEquals(spaceName, editPage.getMetaDataValue("space"));
        assertEquals("NonExistingPage", editPage.getMetaDataValue("page"));
        // The default parent is the home page of the current wiki (XWIKI-7572).
        assertEquals("Main.WebHome", editPage.getParent());
        // and the title the name of the space
        assertEquals("NonExistingPage", editPage.getDocumentTitle());
    }
}

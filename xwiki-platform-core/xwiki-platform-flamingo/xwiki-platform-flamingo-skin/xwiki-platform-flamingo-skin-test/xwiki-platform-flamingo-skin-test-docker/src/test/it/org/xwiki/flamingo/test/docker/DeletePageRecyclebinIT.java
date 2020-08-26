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

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.flamingo.skin.test.po.DeleteConfirmationPage;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ConfirmationPage;
import org.xwiki.test.ui.po.DeletePageOutcomePage;
import org.xwiki.test.ui.po.DeletingPage;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the Delete Page feature.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@UITest(properties = { "xwikiPropertiesAdditionalProperties=refactoring.recyclebin.skip=true" })
public class DeletePageRecyclebinIT
{
    private ViewPage viewPage;

    private static final String LOGGED_USERNAME = "superadmin";

    private static final String DOCUMENT_NOT_FOUND = "The requested page could not be found.";

    private static final String SPACE_VALUE = "Test";

    private static final String PAGE_VALUE = "DeletePageTest";

    private static final String PAGE_CONTENT = "This page is used for testing delete functionality";

    private static final String PAGE_TITLE = "Page title that will be deleted";

    private static final String DELETE_SUCCESSFUL = "Done.";


    @BeforeEach
    public void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();

        // set the user type to Advanced
        final HashMap<String, Object> properties = new HashMap<>();
        properties.put("usertype", "Advanced");
        setup.updateObject("XWiki", "superadmin", "XWiki.XWikiUsers", 0, properties);

        // Create a new Page that will be deleted
        this.viewPage = setup.createPage(SPACE_VALUE, PAGE_VALUE, PAGE_CONTENT, PAGE_TITLE);
    }

    @Test
    @Order(1)
    public void deleteToRecyclebin() {
        ConfirmationPage confirmationPage = this.viewPage.delete();
        new DeleteConfirmationPage().selectOptionToRecycleBin();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        assertEquals(LOGGED_USERNAME, deleteOutcome.getPageDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
    }

    @Test
    @Order(2)
    public void deleteSkipRecyclebin() {
        ConfirmationPage confirmationPage = this.viewPage.delete();
        new DeleteConfirmationPage().selectOptionSkipRecycleBin();
        confirmationPage.clickYes();
        DeletingPage deletingPage = new DeletingPage();
        deletingPage.waitUntilFinished();
        assertEquals(DELETE_SUCCESSFUL, deletingPage.getInfoMessage());
        DeletePageOutcomePage deleteOutcome = deletingPage.getDeletePageOutcomePage();
        assertEquals(LOGGED_USERNAME, deleteOutcome.getPageDeleter());
        assertEquals(DOCUMENT_NOT_FOUND, deleteOutcome.getMessage());
    }
}

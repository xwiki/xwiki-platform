/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it;

import com.xpn.xwiki.it.framework.AbstractXWikiTestCase;
import com.xpn.xwiki.it.framework.AlbatrossSkinExecutor;
import com.xpn.xwiki.it.framework.XWikiTestSuite;
import junit.framework.Test;
import junit.framework.AssertionFailedError;

/**
 * Verify deletion of pages.
 *
 * @version $Id: $
 */
public class DeletePageTest extends AbstractXWikiTestCase
{
    public static Test suite()
    {
        XWikiTestSuite suite = new XWikiTestSuite("Verify deletion of pages");
        suite.addTestSuite(DeletePageTest.class, AlbatrossSkinExecutor.class);
        return suite;
    }

    public void testDeleteOkWhenConfirming()
    {
        logInAndCreatePageToBeDeleted("DeleteTest");
        clickDeletePage();
        clickLinkWithLocator("//input[@value='yes']");

        assertTextPresent("The document has been deleted.");
    }

    /**
     * Verify that we can delete a page without showing the confirmation dialog box and that we
     * can redirect to any page we want when the delete is done.
     */
    public void testDeletePageCanSkipConfirmationAndDoARedirect()
    {
        logInAndCreatePageToBeDeleted("DeleteTest");
        open("/xwiki/bin/delete/Test/DeleteTest?confirm=1&xredirect=/xwiki/bin/view/Main/");
        assertPage("Main", "WebHome");
    }

    /**
     * Verify that we can skip the default delete result page and instead redirect to any page we
     * want.
     */
    public void testDeletePageCanDoRedirect()
    {
        logInAndCreatePageToBeDeleted("DeleteTest");
        open("/xwiki/bin/delete/Test/DeleteTest?xredirect=/xwiki/bin/view/Main/");
        clickLinkWithLocator("//input[@value='yes']");
        assertPage("Main", "WebHome");
    }

    /**
     * Verify that hitting cancel on the delete confirmation dialog box goes back to the page being
     * deleted.
     */
    public void testDeletePageGoesToOriginalPageWhenCancelled()
    {
        logInAndCreatePageToBeDeleted("DeleteTestNoDelete");
        // Note: We call the page with a unique name as we're not going to delete it and it should
        // not interefere with others tests. We could always remove the DeleteTest page before any
        // test but it would take longer.
        open("/xwiki/bin/delete/Test/DeleteTestNoDelete");
        clickLinkWithLocator("//input[@value='no']");
        assertPage("Test", "DeleteTestNoDelete");
    }

    public void testDeletePageIsImpossibleWhenNoDeleteRights()
    {
        open("/xwiki/bin/view/Main/");

        // Ensure the user isn't logged in
        if (isAuthenticated()) {
            logout();
        }

        // Note: Ideally we should have tested for the non existence of the Delete button element.
        // However, in order to isolate skin implementation from the test this would have required
        // adding a new isDeleteButtonPresent() method to the Skin Executor API. This would have
        // been a bit awkward as testing for the Delete button is only going to happen in this
        // test case and thus there's no need to share it with all the oher tests. This is why I
        // have chosen to reuse the existing clickDeletePage() method and test for an exception.
        try {
            clickDeletePage();
            fail("Should have failed here as the Delete button shouldn't be present");
        } catch (AssertionFailedError expected) {
            assertTrue(expected.getMessage().endsWith("isn't present."));
        }
    }

    private void logInAndCreatePageToBeDeleted(String pageName)
    {
        loginAsAdmin();

        open("/xwiki/bin/edit/Test/" + pageName + "?editor=wiki");
        setFieldValue("content", "some content");
        clickEditSaveAndView();
    }
}

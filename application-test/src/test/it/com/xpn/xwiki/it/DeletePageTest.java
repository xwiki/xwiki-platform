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
        logInAndCreatePageToBeDeleted();
        clickDeletePage();
        clickLinkWithLocator("//input[@value='yes']");

        assertTextPresent("The document has been deleted.");
    }

    /**
     * Verify that we can skip the delete result page if we pass a xredirect parameter to a page
     * we want to be redirected to. Note that the confirm=1 parameter is also required as
     * otherwise the redirect will have no effect. This is possibly a bug.
     */
    public void testDeletePageCanSkipConfirmationAndDoARedirect()
    {
        logInAndCreatePageToBeDeleted();
        open("/xwiki/bin/delete/Test/DeleteTest?confirm=1&xredirect=/xwiki/bin/view/Main/");
        assertPage("Main", "WebHome");
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

    private void logInAndCreatePageToBeDeleted()
    {
        loginAsAdmin();

        open("/xwiki/bin/edit/Test/DeleteTest?editor=wiki");
        setFieldValue("content", "some content");
        clickEditSaveAndView();
    }
}

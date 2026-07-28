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
package org.xwiki.edit.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the document lock taken by the edit action.
 *
 * @version $Id$
 */
@UITest
class DocumentLockIT
{
    private static final String USER_NAME = "DocumentLockUser";

    private static final String PASSWORD = "password";

    private static final String CONTENT = "Some content";

    /**
     * The lock is released by a beacon request sent when the edit page is left. Verify that this request reaches the
     * server even when the page is left because the test framework switches the authenticated user, otherwise the next
     * user editing that page gets a "currently locked by" warning (see
     * {@link org.xwiki.test.ui.po.editor.ForceEditLockPage}) instead of the editor.
     */
    @Test
    void releaseLockWhenSwitchingUser(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.createPage(testReference, CONTENT, "Document lock test");
        setup.createUser(USER_NAME, PASSWORD, null);

        // Take a lock on the page as the test user. The wiki editor is used explicitly so that the test doesn't depend
        // on which editor is the preferred one.
        setup.login(USER_NAME, PASSWORD);
        assertEquals(CONTENT, WikiEditPage.gotoPage(testReference).getContent(),
            "Expected the wiki editor of the page locked by " + USER_NAME);

        // Switch the user, which is when the lock of the page being left has to be released.
        setup.loginAsSuperAdmin();

        // Edit the same page as superadmin: we get the editor and not the lock warning page since the lock was
        // released above. If the request releasing the lock didn't reach the server then there is no editor on this
        // page, only a warning with a button to force the edit.
        assertEquals(CONTENT, WikiEditPage.gotoPage(testReference).getContent(),
            "Expected the wiki editor of the page whose lock was released");
    }
}

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
package org.xwiki.administration.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.ForgotUsernameCompletePage;
import org.xwiki.administration.test.po.ForgotUsernamePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the ForgotUsername UI.
 *
 * @version $Id$
 * @since 11.10RC1
 */
@UITest
public class ForgotUsernameIT
{
    @Test
    public void retrieveUsername(TestUtils testUtils, TestReference testReference)
    {
        String user = "realuser";
        String userMail = "realuser@host.org";

        // We need to login as superadmin to set the user email.
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(user, "realuserpwd", testUtils.getURLToNonExistentPage(), "email", userMail);

        testUtils.forceGuestUser();
        ForgotUsernamePage forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail(userMail);
        ForgotUsernameCompletePage forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertFalse(forgotUsernameCompletePage.isAccountNotFound());
        assertTrue(forgotUsernameCompletePage.isUsernameRetrieved(user));

        // Bypass the check that prevents to reload the current page
        testUtils.gotoPage(testUtils.getURLToNonExistentPage());

        // test that bad mail results in no results
        forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail("bad_mail@evil.com");
        forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertTrue(forgotUsernameCompletePage.isAccountNotFound());
        assertFalse(forgotUsernameCompletePage.isUsernameRetrieved(user));

        // Bypass the check that prevents to reload the current page
        testUtils.gotoPage(testUtils.getURLToNonExistentPage());

        // XWIKI-4920 test that the email is properly escaped
        forgotUsernamePage = ForgotUsernamePage.gotoPage();
        forgotUsernamePage.setEmail("a' synta\\'x error");
        forgotUsernameCompletePage = forgotUsernamePage.clickRetrieveUsername();
        assertTrue(forgotUsernameCompletePage.isAccountNotFound());
        assertFalse(forgotUsernameCompletePage.isUsernameRetrieved(user));
    }
}

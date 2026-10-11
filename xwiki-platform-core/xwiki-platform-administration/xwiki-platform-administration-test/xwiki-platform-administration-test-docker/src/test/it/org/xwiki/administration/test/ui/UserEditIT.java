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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.EditUserModal;
import org.xwiki.administration.test.po.UsersAdministrationSectionPage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.security.requiredrights.test.po.RequiredRightsPreEditCheckElement;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for editing users via the admin user edit modal, including the edit confirmation flow.
 *
 * @version $Id$
 */
@UITest(properties = {
    // Add the RightsManagerPlugin needed by the test
    "xwikiCfgPlugins=com.xpn.xwiki.plugin.rightsmanager.RightsManagerPlugin",
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus,
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-19932
        "org.xwiki.platform:xwiki-platform-notifications-filters-default"
    }
)
class UserEditIT
{
    private static final String USER_PASSWORD = "testPassword";

    @BeforeEach
    void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    /**
     * Tests that a user can be edited via the admin user edit modal.
     */
    @Test
    void editUser(TestUtils setup, TestReference testReference)
    {
        String userName = testReference.getLastSpaceReference().getName();
        setup.deletePage("XWiki", userName);
        setup.createUser(userName, USER_PASSWORD, "");

        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        // Make sure the user is displayed even if there are many users.
        usersPage.getUsersLiveData().getTableLayout().filterColumn("User", userName);

        int rowNumber = usersPage.getRowNumberByUsername(userName);

        EditUserModal editUserModal = usersPage.clickEditUser(rowNumber);
        editUserModal.waitUntilReady();

        String newFirstName = "John";
        editUserModal.setFirstName(newFirstName);
        editUserModal.clickSave();

        usersPage.waitForNotificationSuccessMessage("Saved");
        usersPage.getUsersLiveData().getTableLayout().waitUntilReady();

        // Reopen the modal to verify the first name was saved.
        rowNumber = usersPage.getRowNumberByUsername(userName);
        editUserModal = usersPage.clickEditUser(rowNumber);
        editUserModal.waitUntilReady();
        assertEquals(newFirstName, editUserModal.getFirstName());
        editUserModal.close();
    }

    /**
     * Tests that editing a user profile that contains required rights warnings shows the edit confirmation modal.
     * The test verifies that cancelling the modal does not open the edit form, and confirming it does.
     */
    @Test
    void editUserWithRequiredRightsWarning(TestUtils setup, TestReference testReference) throws Exception
    {
        String userName = testReference.getLastSpaceReference().getName();
        LocalDocumentReference userProfileReference = new LocalDocumentReference("XWiki", userName);
        setup.rest().delete(userProfileReference);

        // Create and log in as the test user and save the user profile page with a velocity macro in the content.
        // This makes the test user the content author, which triggers the required rights warning
        // when an admin (who has more rights) edits the profile.
        setup.createUserAndLogin(userName, USER_PASSWORD);
        setup.rest().savePage(userProfileReference, "{{velocity}}$xcontext.user{{/velocity}}", "");
        setup.loginAsSuperAdmin();

        UsersAdministrationSectionPage usersPage = UsersAdministrationSectionPage.gotoPage();
        // Make sure the user is displayed even if there are many users.
        usersPage.getUsersLiveData().getTableLayout().filterColumn("User", userName);
        int rowNumber = usersPage.getRowNumberByUsername(userName);

        // Clicking edit should trigger the required rights confirmation flow, showing a warning modal.
        EditUserModal modal = usersPage.clickEditUser(rowNumber);
        modal.waitUntilEditConfirmationWarningIsDisplayed();

        RequiredRightsPreEditCheckElement editChecks = new RequiredRightsPreEditCheckElement();
        editChecks.toggleDetails();
        assertEquals(1, editChecks.count());
        assertEquals("A [velocity] scripting macro requires script rights and might require programming right "
            + "depending on the called methods.", editChecks.getSummary(0));
        modal.clickCancel();

        // Click edit again: the warning modal should reappear.
        modal = usersPage.clickEditUser(rowNumber);
        modal.waitUntilEditConfirmationWarningIsDisplayed();
        editChecks.toggleDetails();
        assertEquals(1, editChecks.count());

        // Confirm to open the edit modal.
        modal.forceEditConfirmationWarning(true);
        modal.close();

        // Clicking edit again shouldn't show the warning again, the forcing should be remembered.
        modal = usersPage.clickEditUser(rowNumber);
        modal.waitUntilReady();
        modal.close();
    }
}


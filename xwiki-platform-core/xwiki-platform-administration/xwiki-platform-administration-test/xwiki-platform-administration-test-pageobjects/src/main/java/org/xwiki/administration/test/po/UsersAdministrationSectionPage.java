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
package org.xwiki.administration.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the actions possible on the Users Administration Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class UsersAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String USER_ACTION_XPATH_FORMAT =
        "//table[@id = 'userstable']//td[contains(@class, 'name') and normalize-space(.) = '%s']"
            + "/following-sibling::td[contains(@class, 'actions')]/a[contains(@class, 'action%s')]";

    public static final String ADMINISTRATION_SECTION_ID = "Users";

    @FindBy(css = ".btn[data-target='#createUserModal']")
    private WebElement createUserButton;

    /**
     * The live table listing the users.
     */
    private final LiveTableElement usersLiveTable;

    /**
     * @since 4.2M1
     */
    public static UsersAdministrationSectionPage gotoPage()
    {
        AdministrationSectionPage.gotoPage(ADMINISTRATION_SECTION_ID);
        return new UsersAdministrationSectionPage();
    }

    public UsersAdministrationSectionPage()
    {
        super(ADMINISTRATION_SECTION_ID);
        this.usersLiveTable = new LiveTableElement("userstable");
    }

    public RegistrationModal clickAddNewUser()
    {
        this.createUserButton.click();
        return new RegistrationModal().waitUntilPageIsLoaded();
    }

    /**
     * @return the live table that list the users
     * @since 4.3.1
     */
    public LiveTableElement getUsersLiveTable()
    {
        return this.usersLiveTable;
    }

    @Override
    public UsersAdministrationSectionPage waitUntilPageIsLoaded()
    {
        this.usersLiveTable.waitUntilReady();
        return this;
    }

    public ConfirmationModal clickDeleteUser(String userName)
    {
        getDriver().findElementWithoutWaiting(By.xpath(String.format(USER_ACTION_XPATH_FORMAT, userName, "delete")))
            .click();
        return new ConfirmationModal(By.id("deleteUserModal"));
    }

    public UsersAdministrationSectionPage deleteUser(String userName)
    {
        clickDeleteUser(userName).clickOk();
        // The live table is refreshed.
        this.usersLiveTable.waitUntilReady();
        return this;
    }

    public boolean canDeleteUser(String userName)
    {
        return !getDriver()
            .findElementsWithoutWaiting(By.xpath(String.format(USER_ACTION_XPATH_FORMAT, userName, "delete")))
            .isEmpty();
    }

    public RegistrationModal clickEditUser(String userName)
    {
        getDriver().findElementWithoutWaiting(By.xpath(String.format(USER_ACTION_XPATH_FORMAT, userName, "edit")))
            .click();
        return new RegistrationModal().waitUntilPageIsLoaded();
    }
}

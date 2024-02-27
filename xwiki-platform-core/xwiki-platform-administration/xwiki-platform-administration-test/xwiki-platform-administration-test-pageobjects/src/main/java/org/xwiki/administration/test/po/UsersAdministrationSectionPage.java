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
import org.xwiki.livedata.test.po.LiveDataElement;

/**
 * Represents the actions possible on the Users Administration Page.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class UsersAdministrationSectionPage extends AdministrationSectionPage
{
    private static final String ADMINISTRATION_SECTION_ID = "Users";

    @FindBy(css = ".btn[data-target='#createUserModal']")
    private WebElement createUserButton;

    /**
     * The live table listing the users.
     */
    private final LiveDataElement usersLiveData;

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
        this.usersLiveData = new LiveDataElement("userstable");
    }

    public RegistrationModal clickAddNewUser()
    {
        this.createUserButton.click();
        return new RegistrationModal();
    }

    /**
     * @return the live table that list the users
     * @since 4.3.1
     */
    public LiveDataElement getUsersLiveData()
    {
        return this.usersLiveData;
    }

    public DeleteUserConfirmationModal clickDeleteUser(int rowNumber)
    {
        getUsersLiveData().getTableLayout().clickAction(rowNumber, "delete");
        return new DeleteUserConfirmationModal();
    }

    public boolean canDeleteUser(int rowNumber)
    {
        return getUsersLiveData().getTableLayout().hasAction(rowNumber, "delete");
    }

    public UsersAdministrationSectionPage disableUser(int rowNumber)
    {

        getUsersLiveData().getTableLayout().clickAction(rowNumber, "disable");
        this.waitForNotificationSuccessMessage("User account disabled");
        this.usersLiveData.getTableLayout().waitUntilReady();
        return this;
    }

    public boolean isUserDisabled(int rowNumber)
    {
        return !getUsersLiveData()
            .getTableLayout()
            .findElementsInRow(rowNumber, By.cssSelector("td[data-title='User'] div.user.disabled")).isEmpty();
    }

    public boolean canDisableUser(int rowNumber)
    {
        return getUsersLiveData().getTableLayout().hasAction(rowNumber, "disable");
    }

    public boolean canEnableUser(int rowNumber)
    {
        return getUsersLiveData().getTableLayout().hasAction(rowNumber, "enable");
    }

    public boolean canEditUser(int rowNumber)
    {
        return getUsersLiveData().getTableLayout().hasAction(rowNumber, "edit");
    }
}

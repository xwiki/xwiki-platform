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

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Represents the actions possible on the Users Administration Page.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class UsersAdministrationSectionPage extends AdministrationSectionPage
{
    public static final String ADMINISTRATION_SECTION_ID = "Users";

    @FindBy(id = "addNewUser")
    private WebElement addNewUserButton;

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
        usersLiveTable = new LiveTableElement("userstable");
    }

    public LightBoxRegistrationPage clickAddNewUser()
    {
        this.addNewUserButton.click();
        return new LightBoxRegistrationPage();
    }

    /**
     * @return the live table that list the users
     * @since 4.3.1
     */
    public LiveTableElement getUsersLiveTable()
    {
        return usersLiveTable;
    }
}

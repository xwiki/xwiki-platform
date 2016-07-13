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
import org.xwiki.test.ui.po.BasePage;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Page Object for the Administration/Groups page.
 * 
 * @version $Id$
 * @since 8.1M2
 */
public class GroupsPage extends BasePage
{
    LiveTableElement groupsLiveTable = new LiveTableElement("groupstable");

    @FindBy(id = "addNewGroup")
    private WebElement addGroupButton;

    @FindBy(css = "#addnewgroup .button.create")
    private WebElement createGroupButton;

    public void clickAddNewGroupButton()
    {
        this.addGroupButton.click();
    }

    public void clickCreateGroupButton()
    {
        this.createGroupButton.click();
    }

    /**
     * Method to create a new Group.
     */
    public GroupsPage addNewGroup(String groupName)
    {
        clickAddNewGroupButton();

        // TODO: create PO for the popup
        getDriver().waitUntilElementIsVisible(By.id("newgroupi"));
        getDriver().findElementWithoutWaiting(By.id("newgroupi")).sendKeys(groupName);
        clickCreateGroupButton();
        getDriver().waitUntilElementDisappears(By.id("lb"));

        GroupsPage groupsPage = new GroupsPage();
        groupsPage.waitUntilPageIsLoaded();
        return groupsPage;
    }

    /**
     * Method to go to the Administration/Groups page.
     */
    public static GroupsPage gotoPage()
    {
        AdministrationPage.gotoPage().clickSection("Users & Groups", "Groups");
        GroupsPage groupsPage = new GroupsPage();
        groupsPage.waitUntilPageIsLoaded();
        return groupsPage;
    }

    /**
     * Method that overrides waitUntilPageIsLoaded() and waits also for the Groups livetable to load.
     * 
     * @see org.xwiki.test.ui.po.BasePage#waitUntilPageIsLoaded()
     */
    @Override
    public BasePage waitUntilPageIsLoaded()
    {
        super.waitUntilPageIsLoaded();

        groupsLiveTable.waitUntilReady();

        return this;
    }

    public LiveTableElement getGroupsTable()
    {
        return groupsLiveTable;
    }

    public void filterGroups(String group)
    {
        groupsLiveTable.filterColumn("name", group);
    }
}

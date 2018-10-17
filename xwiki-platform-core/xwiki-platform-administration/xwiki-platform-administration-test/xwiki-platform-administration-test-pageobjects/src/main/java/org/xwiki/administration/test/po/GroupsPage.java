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
import org.xwiki.test.ui.po.ConfirmationModal;
import org.xwiki.test.ui.po.LiveTableElement;

/**
 * Page Object for the Administration/Groups page.
 *
 * @version $Id$
 * @since 8.1M2
 */
public class GroupsPage extends BasePage
{
    private static final String GROUP_ACTION_XPATH_FORMAT =
        "//table[@id = 'groupstable']//td[contains(@class, 'name') and normalize-space(.) = '%s']"
            + "/following-sibling::td[contains(@class, 'actions')]/a[contains(@class, 'action%s')]";

    private LiveTableElement groupsLiveTable = new LiveTableElement("groupstable");

    @FindBy(css = ".btn[data-target='#createGroupModal']")
    private WebElement createGroupButton;

    /**
     * Method to create a new Group.
     */
    public GroupsPage addNewGroup(String groupName)
    {
        clickCreateGroup().createGroup(groupName);

        // The live table is refreshed.
        this.groupsLiveTable.waitUntilReady();

        return this;
    }

    public CreateGroupModal clickCreateGroup()
    {
        this.createGroupButton.click();
        return new CreateGroupModal();
    }

    /**
     * Method to go to the Administration/Groups page.
     */
    public static GroupsPage gotoPage()
    {
        AdministrationPage.gotoPage().clickSection("Users & Rights", "Groups");
        GroupsPage groupsPage = new GroupsPage();
        return groupsPage.waitUntilPageIsLoaded();
    }

    /**
     * Method that overrides waitUntilPageIsLoaded() and waits also for the groups live table to load.
     *
     * @see org.xwiki.test.ui.po.BasePage#waitUntilPageIsLoaded()
     */
    @Override
    public GroupsPage waitUntilPageIsLoaded()
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
        groupsLiveTable.filterColumn("xwiki-livetable-groupstable-filter-1", group);
    }

    public ConfirmationModal clickDeleteGroup(String groupName)
    {
        getDriver().findElementWithoutWaiting(By.xpath(String.format(GROUP_ACTION_XPATH_FORMAT, groupName, "delete")))
            .click();
        return new ConfirmationModal(By.id("deleteGroupModal"));
    }

    public GroupsPage deleteGroup(String groupName)
    {
        clickDeleteGroup(groupName).clickOk();
        // The live table is refreshed.
        this.groupsLiveTable.waitUntilReady();
        return this;
    }

    public boolean canDeleteGroup(String groupName)
    {
        return !getDriver()
            .findElementsWithoutWaiting(By.xpath(String.format(GROUP_ACTION_XPATH_FORMAT, groupName, "delete")))
            .isEmpty();
    }

    public EditGroupModal clickEditGroup(String groupName)
    {
        getDriver().findElementWithoutWaiting(By.xpath(String.format(GROUP_ACTION_XPATH_FORMAT, groupName, "edit")))
            .click();
        return new EditGroupModal().waitUntilReady();
    }

    public String getMemberCount(String groupName)
    {
        String xpath = "//table[@id = 'groupstable']//td[contains(@class, 'name') and normalize-space(.) = '"
            + groupName + "']/following-sibling::td[contains(@class, 'members')]";
        return getDriver().findElementWithoutWaiting(By.xpath(xpath)).getText();
    }
}

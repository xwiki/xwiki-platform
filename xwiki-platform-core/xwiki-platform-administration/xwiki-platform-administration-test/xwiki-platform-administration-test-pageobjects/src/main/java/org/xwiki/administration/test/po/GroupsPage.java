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

import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.BasePage;

/**
 * Page Object for the Administration/Groups page.
 *
 * @version $Id$
 * @since 8.1M2
 */
public class GroupsPage extends BasePage
{
    private final LiveDataElement groupsLiveData = new LiveDataElement("groupstable");

    @FindBy(css = ".btn[data-target='#createGroupModal']")
    private WebElement createGroupButton;

    private TableLayoutElement tableLayout;

    /**
     * Method to create a new Group.
     */
    public GroupsPage addNewGroup(String groupName)
    {
        clickCreateGroup().createGroup(groupName);
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
        return groupsPage;
    }

    public TableLayoutElement getGroupsTable()
    {
        if (this.tableLayout == null) {
            this.tableLayout = this.groupsLiveData.getTableLayout();
        }
        return this.tableLayout;
    }

    public DeleteGroupConfirmationModal clickDeleteGroup(String groupName)
    {
        int rowNumber = getRowNumberByGroupName(groupName);
        getGroupsTable().clickAction(rowNumber, "delete");
        return new DeleteGroupConfirmationModal();
    }

    public GroupsPage deleteGroup(String groupName)
    {
        clickDeleteGroup(groupName).clickOk();
        return this;
    }

    public EditGroupModal clickEditGroup(String groupName)
    {
        int rowNumber = getRowNumberByGroupName(groupName);
        getGroupsTable().clickAction(rowNumber, "edit");

        return new EditGroupModal().waitUntilReady();
    }

    public String getMemberCount(String groupName)
    {
        int rowNumber = getRowNumberByGroupName(groupName);
        return getGroupsTable().getCell("Members", rowNumber).getText();
    }

    private int getRowNumberByGroupName(String groupName)
    {
        String entryIndex = this.groupsLiveData.getTableLayout()
            .getRows()
            .stream()
            .filter(it -> {
                String rowGroupName = it.findElement(By.cssSelector("[data-title='Group Name']")).getText();
                return Objects.equals(rowGroupName, groupName);
            })
            .findFirst()
            .map(it -> it.getAttribute("data-livedata-entry-index"))
            .get();
        return Integer.parseInt(entryIndex) + 1;
    }
}

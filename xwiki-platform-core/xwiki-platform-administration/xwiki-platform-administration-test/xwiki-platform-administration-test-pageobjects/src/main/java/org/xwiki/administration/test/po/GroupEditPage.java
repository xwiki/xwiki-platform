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

import java.util.List;
import java.util.Objects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Page Object for a Group page in Edit mode.
 *
 * @version $Id$
 * @since 8.1M2
 */
public class GroupEditPage extends InlinePage
{
    private final LiveDataElement membersLiveData = new LiveDataElement("groupusers");

    @FindBy(id = "addMembers")
    private WebElement addMemberButton;

    @FindBy(id = "groupInput")
    private WebElement groupInput;

    @FindBy(id = "userInput")
    private WebElement userInput;

    private TableLayoutElement tableLayout;

    public void clickAddMemberButton()
    {
        this.addMemberButton.click();
    }

    /**
     * Method to add a new member (user or group) to a Group.
     */
    public GroupEditPage addMemberToGroup(String member, boolean isUser)
    {
        return addMembersToGroup(isUser ? this.userInput : this.groupInput, member);
    }

    public GroupEditPage addUsers(String... users)
    {
        return addMembersToGroup(this.userInput, users);
    }

    public GroupEditPage addGroups(String... groups)
    {
        return addMembersToGroup(this.groupInput, groups);
    }

    private GroupEditPage addMembersToGroup(WebElement input, String... members)
    {
        SuggestInputElement picker = new SuggestInputElement(input);
        for (String member : members) {
            picker.sendKeys(member).waitForSuggestions().selectByIndex(0);
        }
        picker.hideSuggestions();
        clickAddMemberButton();
        waitForNotificationSuccessMessage("Members successfully added");
        return this;
    }

    public GroupEditPage removeMembers(String... members)
    {
        for (String member : members) {
            int index = 1;
            for (WebElement row : getMembersTable().getRows()) {
                if (Objects.equals(getRowUserName(row), member)) {
                    getMembersTable().clickAction(index, "delete");
                    // Wait for the confirmation message before moving to the next member.
                    waitForNotificationSuccessMessage("Member successfully removed from group");
                    break;
                }
                index++;
            }
        }
        return this;
    }

    public void filterMembers(String member)
    {
        this.membersLiveData.getTableLayout().filterColumn("Member", member);
    }

    /**
     * Method to go to a Group page in Edit mode.
     */
    public static GroupEditPage gotoPage(DocumentReference groupReference)
    {
        getUtil().gotoPage(groupReference, "edit");
        return new GroupEditPage();
    }

    public TableLayoutElement getMembersTable()
    {
        if (this.tableLayout == null) {
            this.tableLayout = this.membersLiveData.getTableLayout();
        }
        return this.tableLayout;
    }

    private static String getRowUserName(WebElement row)
    {
        List<WebElement> elements =
            row.findElements(By.cssSelector("[data-livedata-property-id=\"member\"] .user-name"));
        String rowUserName;
        if (elements.isEmpty()) {
            elements =
                row.findElements(By.cssSelector("[data-livedata-property-id=\"member\"] .group-name"));
            if (elements.isEmpty()) {
                rowUserName = null;
            } else {
                rowUserName = elements.get(0).getText();
            }
        } else {
            rowUserName = elements.get(0).getText();
        }
        return rowUserName;
    }
}

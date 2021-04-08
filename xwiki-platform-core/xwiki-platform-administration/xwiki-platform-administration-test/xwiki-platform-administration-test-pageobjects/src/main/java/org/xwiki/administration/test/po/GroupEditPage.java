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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Page Object for a Group page in Edit mode.
 *
 * @version $Id$
 * @since 8.1M2
 */
public class GroupEditPage extends InlinePage
{
    private static final String MEMBER_ACTION_XPATH_FORMAT =
        "//table[@id = 'groupusers']//td[contains(@class, 'member') and normalize-space(.) = '%s']"
            + "/following-sibling::td[contains(@class, 'actions')]/a[contains(@class, 'action%s')]";

    LiveTableElement membersLiveTable = new LiveTableElement("groupusers");

    @FindBy(id = "addMembers")
    private WebElement addMemberButton;

    @FindBy(id = "groupInput")
    private WebElement groupInput;

    @FindBy(id = "userInput")
    private WebElement userInput;

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
            getDriver().findElementWithoutWaiting(By.xpath(String.format(MEMBER_ACTION_XPATH_FORMAT, member, "delete")))
                .click();
            waitForNotificationSuccessMessage("Done");
        }
        return this;
    }

    public void filterMembers(String member)
    {
        membersLiveTable.filterColumn("xwiki-livetable-groupusers-filter-1", member);
    }

    /**
     * Method to go to a Group page in Edit mode.
     */
    public static GroupEditPage gotoPage(DocumentReference groupReference)
    {
        getUtil().gotoPage(groupReference, "edit");
        return new GroupEditPage();
    }

    public LiveTableElement getMembersTable()
    {
        return membersLiveTable;
    }
}

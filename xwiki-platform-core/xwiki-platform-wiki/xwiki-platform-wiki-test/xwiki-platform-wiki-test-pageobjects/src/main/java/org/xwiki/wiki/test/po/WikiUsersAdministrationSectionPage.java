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
package org.xwiki.wiki.test.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.LiveTableElement;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Represents the Users administration section of a subwiki, where the members of the wiki are managed (provided by
 * the {@code WikiManager.WikiUsers} page).
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
public class WikiUsersAdministrationSectionPage extends ViewPage
{
    private static final By MEMBER_NAME = By.cssSelector("#groupusers-display .user-name");

    @FindBy(id = "userInput")
    private WebElement userInput;

    @FindBy(id = "addMembers")
    private WebElement addMembersButton;

    private final LiveTableElement membersTable = new LiveTableElement("groupusers");

    /**
     * Opens the Users administration section of the given subwiki.
     *
     * @param wikiId the identifier of the subwiki
     * @return the page object of the section
     */
    public static WikiUsersAdministrationSectionPage gotoPage(String wikiId)
    {
        getUtil().gotoPage(new DocumentReference(wikiId, "XWiki", "XWikiPreferences"), "admin",
            "editor=globaladmin&section=Users");
        WikiUsersAdministrationSectionPage page = new WikiUsersAdministrationSectionPage();
        page.membersTable.waitUntilReady();
        return page;
    }

    /**
     * Pick the given global users in the "Add or invite users to wiki" form and add them as members of the wiki.
     *
     * @param users the names of the users to add (e.g. {@code U1})
     * @return this page object
     */
    public WikiUsersAdministrationSectionPage addMembers(String... users)
    {
        SuggestInputElement picker = new SuggestInputElement(this.userInput);
        for (String user : users) {
            picker.clear().sendKeys(user).waitForSuggestions().selectByVisibleText(user);
        }
        picker.hideSuggestions();
        this.addMembersButton.click();
        waitForNotificationSuccessMessage("Members successfully added");
        this.membersTable.waitUntilReady();
        return this;
    }

    /**
     * Click the Delete action of the given member in the Current Members table.
     *
     * @param user the name of the member to remove, as displayed in the table
     * @return this page object
     */
    public WikiUsersAdministrationSectionPage removeMember(String user)
    {
        WebElement memberName = getDriver().findElementsWithoutWaiting(MEMBER_NAME).stream()
            .filter(element -> user.equals(element.getText())).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("[" + user + "] is not a member of the wiki"));
        memberName.findElement(By.xpath("ancestor::tr[1]//a[contains(@class, 'actiondelete')]")).click();
        waitForNotificationSuccessMessage("Done");
        getDriver().waitUntilCondition(driver -> !getMembers().contains(user));
        return this;
    }

    /**
     * @return the names of the members listed in the Current Members table
     */
    public List<String> getMembers()
    {
        return getDriver().findElementsWithoutWaiting(MEMBER_NAME).stream().map(WebElement::getText).toList();
    }
}

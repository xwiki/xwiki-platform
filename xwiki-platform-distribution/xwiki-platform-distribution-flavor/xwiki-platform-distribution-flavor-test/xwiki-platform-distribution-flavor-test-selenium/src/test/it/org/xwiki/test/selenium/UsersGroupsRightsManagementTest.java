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
package org.xwiki.test.selenium;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.administration.test.po.AdministrationMenu;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;
import org.xwiki.test.ui.po.SuggestInputElement;

import static org.junit.Assert.*;

/**
 * Verify the Users, Groups and Rights Management features of XWiki.
 *
 * @version $Id$
 */
public class UsersGroupsRightsManagementTest extends AbstractXWikiTestCase
{
    private AdministrationMenu administrationMenu = new AdministrationMenu();

    /**
     * <ul>
     * <li>Validate group creation.</li>
     * <li>Validate groups administration print "0" members for empty group.</li>
     * <li>Validate group deletion.</li>
     * <li>Validate rights automatically cleaned from deleted groups.</li>
     * </ul>
     */
    @Test
    public void testCreateAndDeleteGroup()
    {
        // Make sure there's no XWikiNewGroup before we try to create it
        deleteGroup("XWikiNewGroup", true);
        createGroup("XWikiNewGroup");

        // Validate XWIKI-1903: Empty group shows 1 member.
        assertEquals("Group XWikiNewGroup which is empty print more than 0 members", 0,
            getGroupMembersCount("XWikiNewGroup"));

        // Give "view" global right to XWikiNewGroup on wiki
        openGlobalRightsPage();
        clickGroupsRadioButton();
        clickViewRightsCheckbox("XWikiNewGroup", "allow");

        // Give "comment" page right to XWikiNewGroup on Test.TestCreateAndDeleteGroup page
        createPage("Test", "TestCreateAndDeleteGroup", "whatever");
        clickEditPageAccessRights();
        clickGroupsRadioButton();
        clickCommentRightsCheckbox("XWikiNewGroup", "allow");

        // Delete the newly created group and see if rights are cleaned
        deleteGroup("XWikiNewGroup", false);

        // Validate XWIKI-2304: When a user or a group is removed it's not removed from rights objects
        open("XWiki", "XWikiPreferences", "edit", "editor=object");
        assertTextNotPresent("XWikiNewGroup");
        open("Test", "TestCreateAndDeleteGroup", "edit", "editor=object");
        assertTextNotPresent("XWikiNewGroup");
    }

    /**
     * Validate that administration show error when trying to create an existing group.
     */
    @Test
    public void testCreateGroupWhenGroupAlreadyExists()
    {
        open("XWiki", "testCreateGroupWhenGroupAlreadyExists", "edit", "editor=wiki");
        clickEditSaveAndView();
        openGroupsPage();
        clickLinkWithText("Add group", false);
        waitForLightbox("Create new group".toUpperCase());
        setFieldValue("newgroupi", "testCreateGroupWhenGroupAlreadyExists");
        getSelenium().click("//input[@value='Create group']");
        // We need to wait till the alert appears since when the user clicks on the "Create Group" button there's
        // an Ajax call made to the server.
        waitForAlert();
        assertEquals("testCreateGroupWhenGroupAlreadyExists cannot be used for the "
            + "group name, as another page with this name already exists.", getSelenium().getAlert());
    }

    /**
     * <ul>
     * <li>Validate user creation.</li>
     * <li>Validate user deletion.</li>
     * <li>Validate groups automatically cleaned from deleted users.</li>
     * </ul>
     */
    @Test
    public void testCreateAndDeleteUser()
    {
        // Make sure there's no XWikiNewUser user before we try to create it
        deleteUser("XWikiNewUser", true);
        createUser("XWikiNewUser", "XWikiNewUser");

        // Verify that new users are automatically added to the XWikiAllGroup group.
        open("XWiki", "XWikiAllGroup");
        waitForGroupUsersLiveTable();
        assertTextPresent("XWiki.XWikiNewUser");

        // Delete the newly created user and see if groups are cleaned
        deleteUser("XWikiNewUser", false);

        // Verify that when a user is removed he's removed from the groups he belongs to.
        open("XWiki", "XWikiAllGroup");
        waitForGroupUsersLiveTable();
        assertTextNotPresent("XWiki.XWikiNewUser");
    }

    /**
     * Test that the Ajax registration tool accepts non-ASCII symbols.
     */
    @Test
    public void testCreateNonAsciiUser()
    {
        // Make sure there's no AccentUser user before we try to create it
        deleteUser("AccentUser", true);

        // Use ISO-8859-1 symbols to make sure that the test works both in ISO-8859-1 and UTF8
        createUser("AccentUser", "AccentUser", "a\u00e9b", "c\u00e0d");

        // Verify that the user is present in the table
        assertTextPresent("AccentUser");
        // Verify that the correct symbols appear
        assertTextPresent("a\u00e9b");
        assertTextPresent("c\u00e0d");
    }

    /**
     * Validate group rights. Validate XWIKI-2375: Group and user access rights problem with a name which includes space
     * characters
     */
    @Test
    public void testGroupRights()
    {
        String username = "TestUser";
        // Voluntarily put a space in the group name.
        String groupname = "Test Group";

        // Make sure there's no "TestUser" user and no "Test Group" user before we try to create it
        deleteUser(username, true);
        deleteGroup(groupname, true);

        // Create a new user, a new group, make the user part of that group and create a new page
        createUser(username, username);
        createGroup(groupname);
        addUserToGroup(username, groupname);
        createPage("Test", "TestGroupRights", "Some content");

        // Deny view rights to the group on the newly created page
        open("Test", "TestGroupRights", "edit", "editor=rights");
        clickGroupsRadioButton();
        // Click a first time to allow view and a second time to deny it.
        clickViewRightsCheckbox(groupname, "allow");
        clickViewRightsCheckbox(groupname, "deny1");

        // Make sure that Admins can still view the page
        open("Test", "TestGroupRights");
        assertTextPresent("Some content");

        // And ensure that the newly created user cannot view it
        login(username, username, false);
        open("Test", "TestGroupRights");
        assertTextPresent("not allowed");

        // Cleanup
        loginAsAdmin();
        deleteUser(username, false);
        deleteGroup(groupname, false);
    }

    /**
     * Test adding a group to a group. Specifically, assert that the group is added as a member itself, not adding all
     * its members one by one.
     */
    @Test
    public void testAddGroupToGroup()
    {
        String group = "GroupWithGroup";
        createGroup(group);
        openGroupsPage();
        String xpath = "//tbody/tr[td/a='" + group + "']/td[3]/img[@title='Edit']";
        System.out.println("XPATH: " + xpath);
        waitForCondition("selenium.isElementPresent(\"" + xpath + "\")");
        getSelenium().click("//tbody/tr[td/a=\"" + group + "\"]/td[3]/img[@title=\"Edit\"]");
        waitForLightbox("SUBGROUPS TO ADD");
        setSuggestInputValue("groupInput", "XWikiAllGroup");
        clickLinkWithLocator("addMembers", false);
        String xpathPrefix = "//div[@id='lb-content']/div/div/table/tbody/tr/td/table/tbody/tr";
        String adminGroupXPath =
            xpathPrefix + "/td[contains(@class, 'member')]/a[@href='/xwiki/bin/view/XWiki/XWikiAllGroup']";
        // this xpath expression is fragile, but we have to start as up as the lightbox does, because
        // the same table with same ids and classes is already displayed in the Preferences page
        // (that is, the list of existing groups).
        waitForCondition("selenium.isElementPresent(\"" + adminGroupXPath + "\")");
        // Now assert that XWiki.Admin, member of XWikiAdminGroup is not added as a member of our created group
        assertElementNotPresent(xpathPrefix + "/td[contains(@class, 'member')]/a[@href='/xwiki/bin/view/XWiki/Admin']");
        clickLinkWithLocator("lb-close");

        // Now same test, but from the group document UI in inline mode
        clickLinkWithText(group);
        clickEditPageInlineForm();
        setSuggestInputValue("groupInput", "XWikiAdminGroup");
        clickLinkWithLocator("addMembers", false);
        waitForTextContains("id=groupusers", "XWiki.XWikiAdminGroup");

        // cleanup
        deleteGroup(group, false);
    }

    /**
     * Validate adding a member to a group via the administration.
     */
    @Test
    public void testAddUserToGroup()
    {
        // Make sure there's no XWikiNewUser user before we try to create it
        deleteUser("XWikiTestUser", true);
        createUser("XWikiTestUser", "XWikiTestUser");

        addUserToGroup("XWikiTestUser", "XWikiAdminGroup");

        deleteUser("XWikiTestUser", true);
    }

    /**
     * Validate member filtering on group sheet.
     */
    @Test
    public void testFilteringOnGroupSheet()
    {
        openGroupsPage();
        String rowXPath = "//td[contains(@class, 'member')]/a[@href='/xwiki/bin/view/XWiki/Admin']";
        this.clickLinkWithText("XWikiAdminGroup");
        this.waitForCondition("selenium.isElementPresent(\"" + rowXPath + "\")");

        this.getSelenium().focus("member");
        this.getSelenium().typeKeys("member", "zzz");
        this.waitForCondition("!selenium.isElementPresent(\"" + rowXPath + "\")");

        this.getSelenium().focus("member");
        // Type Backspace 3 times to delete the previous text.
        this.getSelenium().typeKeys("member", "\b\b\bAd");
        this.waitForCondition("selenium.isElementPresent(\"" + rowXPath + "\")");
    }

    // Helper methods

    private void createGroup(String groupname)
    {
        openGroupsPage();
        clickLinkWithText("Add group", false);
        waitForLightbox("Create new group".toUpperCase());
        setFieldValue("newgroupi", groupname);
        clickLinkWithXPath("//input[@value='Create group']", true);
        waitForTextContains("id=groupstable", groupname);
    }

    /**
     * @param deleteOnlyIfExists if true then only delete the group if it exists
     */
    private void deleteGroup(String groupname, boolean deleteOnlyIfExists)
    {
        if (!deleteOnlyIfExists || (deleteOnlyIfExists && isExistingPage("XWiki", groupname))) {
            openGroupsPage();
            getSelenium().chooseOkOnNextConfirmation();
            clickLinkWithLocator("//tbody/tr[td/a='" + groupname + "']//img[@title='Delete']", false);
            waitForConfirmation();
            assertEquals("The group XWiki." + groupname + " will be deleted. Are you sure you want to proceed?",
                getSelenium().getConfirmation());
            // Wait till the group has been deleted.
            waitForCondition("!selenium.isElementPresent('//tbody/tr[td/a=\"" + groupname + "\"]')");
        }
    }

    private void createUser(String login, String pwd)
    {
        createUser(login, pwd, "New", "User");
    }

    private void createUser(String login, String pwd, String fname, String lname)
    {
        openUsersPage();
        clickLinkWithText("Add user", false);
        waitForElement("//input[@id='register_first_name']");
        setFieldValue("register_first_name", fname);
        setFieldValue("register_last_name", lname);
        setFieldValue("xwikiname", login);
        setFieldValue("register_password", pwd);
        setFieldValue("register2_password", pwd);
        setFieldValue("register_email", "new.user@xwiki.org");
        getSelenium().click("//input[@value='Save']");
        // Wait till the user is displayed.
        waitForTextContains("id=userstable", login);
    }

    private void deleteUser(String login, boolean deleteOnlyIfExists)
    {
        if (!deleteOnlyIfExists || (deleteOnlyIfExists && isExistingPage("XWiki", login))) {
            openUsersPage();
            clickLinkWithLocator("//tbody/tr[td/a='" + login + "']//img[@title='Delete']", false);
            waitForConfirmation();
            assertEquals("The user XWiki." + login + " will be deleted and removed from all groups he belongs to. "
                + "Are you sure you want to proceed?", getSelenium().getConfirmation());
            // Wait till the user has been deleted.
            waitForCondition("!selenium.isElementPresent('//tbody/tr[td/a=\"" + login + "\"]')");
        }
    }

    private void addUserToGroup(String user, String group)
    {
        openGroupsPage();
        String xpath = "//tbody/tr[td/a='" + group + "']/td[3]/img[@title='Edit']";
        waitForCondition("selenium.isElementPresent(\"" + xpath + "\")");
        getSelenium().click(xpath);
        waitForLightbox("USERS TO ADD");
        setSuggestInputValue("userInput", user);
        clickLinkWithLocator("addMembers", false);

        String xpathPrefix = "//div[@id='lb-content']/div/div/table/tbody/tr/td/table/tbody/tr";
        String newGroupMemberXPath =
            xpathPrefix + "/td[contains(@class, 'member')]/a[@href='/xwiki/bin/view/XWiki/" + user + "']";
        // this xpath expression is fragile, but we have to start as up as the lightbox does, because
        // the same table with same ids and classes is already displayed in the Preferences page
        // (that is, the list of existing groups).
        waitForCondition("selenium.isElementPresent(\"" + newGroupMemberXPath + "\")");

        // Close the group edit lightbox
        clickLinkWithLocator("lb-close");
        open("XWiki", group);
        waitForGroupUsersLiveTable();
        assertTextPresent(user);
    }

    private void setSuggestInputValue(String id, String value)
    {
        SuggestInputElement suggester = new SuggestInputElement(getDriver().findElementWithoutWaiting(By.id(id)));
        suggester.clearSelectedSuggestions().sendKeys(value).waitForSuggestions().sendKeys(Keys.ENTER)
            .hideSuggestions();
    }

    private void waitForLightbox(String lightboxName)
    {
        waitForBodyContains(lightboxName);
    }

    private void clickGroupsRadioButton()
    {
        clickLinkWithXPath("//input[@name='uorg' and @value='groups']", false);
    }

    private void openGlobalRightsPage()
    {
        openAdministrationPage();
        administrationMenu.expandCategoryWithName("Users & Rights").getSectionByName("Users & Rights", "Rights")
            .click();
    }

    private void openGroupsPage()
    {
        openAdministrationPage();
        administrationMenu.expandCategoryWithName("Users & Rights").getSectionByName("Users & Rights", "Groups")
            .click();
        waitForLiveTable("groupstable");
    }

    private void openUsersPage()
    {
        // Note: We could have used the following command instead:
        // open("XWiki", "XWikiUsers", "admin", "editor=users")
        // However we haven't done it since we also want to verify that clicking on the "Users" tab works.
        openAdministrationPage();
        administrationMenu.expandCategoryWithName("Users & Rights").getSectionByName("Users & Rights", "Users").click();
        waitForLiveTable("userstable");
    }

    /**
     * @return the number of members in the passed group. Should only be executed when on the Global Rights page.
     */
    private int getGroupMembersCount(String groupname)
    {
        return Integer.parseInt(getSelenium().getText("//tbody/tr[td/a=\"" + groupname + "\"]/td[2]"));
    }

    /**
     * @param actionToVerify the action that the click is supposed to have done. Valid values are "allow", "deny1" or
     *            "none".
     */
    private void clickViewRightsCheckbox(String groupOrUserName, String actionToVerify)
    {
        clickRightsCheckbox(groupOrUserName, actionToVerify, 2);
    }

    /**
     * @param actionToVerify the action that the click is supposed to have done. Valid values are "allow", "deny1" or
     *            "none".
     */
    private void clickCommentRightsCheckbox(String groupOrUserName, String actionToVerify)
    {
        clickRightsCheckbox(groupOrUserName, actionToVerify, 3);
    }

    private void clickRightsCheckbox(String groupOrUserName, String actionToVerify, int positionInTd)
    {
        String xpath = "//tbody/tr[td/a='" + groupOrUserName + "']/td[" + positionInTd + "]/img";
        clickLinkWithXPath(xpath, false);
        // Wait till it has been clicked since this can take some time.
        waitForCondition("selenium.isElementPresent(\"" + xpath + "[contains(@src, '" + actionToVerify + ".png')]\")");
    }

    /**
     * Waits for the live table that lists group users to load.
     */
    private void waitForGroupUsersLiveTable()
    {
        waitForLiveTable("groupusers");
    }
}

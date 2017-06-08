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
package org.xwiki.test.ui.appwithinminutes;

import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.appwithinminutes.test.po.UserClassFieldEditPane;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.editor.UserPicker;
import org.xwiki.test.ui.po.editor.UserPicker.UserElement;
import org.xwiki.xclass.test.po.ClassSheetPage;

/**
 * Special class editor tests that address only the User class field type.
 * 
 * @version $Id$
 * @since 4.5
 */
public class UserClassFieldTest extends AbstractClassEditorTest
{
    @BeforeClass
    public static void setUpClass() throws Exception
    {
        // Create 2 users.
        getUtil().createUserAndLogin("tmortagne", "tmortagne", "first_name", "Thomas", "last_name", "Mortagne",
            "avatar", "tmortagne.png");
        getUtil().attachFile("XWiki", "tmortagne", "tmortagne.png",
            UserClassFieldTest.class.getResourceAsStream("/appwithinminutes/tmortagne.png"), false);
        getUtil().createUserAndLogin("Enygma2002", "Enygma2002", "first_name", "Eduard", "last_name", "Moraru",
            "avatar", "Enygma2002.png");
        getUtil().attachFile("XWiki", "Enygma2002", "Enygma2002.png",
            UserClassFieldTest.class.getResourceAsStream("/appwithinminutes/Enygma2002.png"), false);
    }

    @Test
    public void testSuggestions()
    {
        UserPicker userPicker = new UserClassFieldEditPane(editor.addField("User").getName()).getUserPicker();

        // The suggestions should be case-insensitive. Match the last name.
        List<UserElement> suggestions = userPicker.sendKeys("mOr").waitForSuggestions().getSuggestions();
        Assert.assertEquals(2, suggestions.size());
        assertUserElement(suggestions.get(0), "Eduard Moraru", "Enygma2002");
        assertUserElement(suggestions.get(1), "Thomas Mortagne");

        // Match the first name.
        suggestions = userPicker.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, "As").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserElement(suggestions.get(0), "Thomas Mortagne");

        // Match the alias.
        suggestions = userPicker.sendKeys(Keys.BACK_SPACE, "20").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserElement(suggestions.get(0), "Eduard Moraru", "Enygma2002");

        // The guest user shouldn't be suggested.
        suggestions = userPicker.clear().sendKeys("guest").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("User not found", suggestions.get(0).getText());

        // Default administrator user should be suggested.
        suggestions = userPicker.clear().sendKeys("admin").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserElement(suggestions.get(0), "Administrator", "Admin", "noavatar.png");

        // "a" should bring many suggestions. Also, a single letter should bring suggestions.
        Assert.assertTrue(userPicker.clear().sendKeys("a").waitForSuggestions().getSuggestions().size() > 2);

        // An empty text input shouldn't bring any suggestions.
        try {
            userPicker.sendKeys(Keys.BACK_SPACE).waitForSuggestions();
            Assert.fail();
        } catch (Exception e) {
        }

        // We should be able to close the list of suggestions using the escape key.
        userPicker.sendKeys("mor").waitForSuggestions().sendKeys(Keys.ESCAPE).waitForSuggestionsToFadeOut();

        // The list of suggestions should close itself after a while.
        userPicker.moveMouseOver().sendKeys(Keys.BACK_SPACE).waitForSuggestions().waitForSuggestionsToDisappear();

        // The list of suggestions should stay open if the mouse is over it.
        userPicker.sendKeys(Keys.BACK_SPACE).waitForSuggestions().getSuggestions().get(0).moveMouseOver();
        try {
            userPicker.waitForSuggestionsToDisappear();
            Assert.fail();
        } catch (Exception e) {
        }

        // .. and the list of suggestions should fade out when the mouse is moved out.
        userPicker.moveMouseOver().waitForSuggestionsToFadeOut();
    }

    /**
     * Asserts the given user matches the expectations.
     * 
     * @param user the user to assert
     * @param name the expected name
     * @param extra extra user fields (alias, image, etc.)
     */
    private void assertUserElement(UserElement user, String name, String... extra)
    {
        String alias;
        if (extra.length > 0) {
            alias = extra[0];
        } else {
            String[] parts = name.split("\\s+");
            if (parts.length > 1) {
                alias = parts[0].toLowerCase().charAt(0) + parts[1].toLowerCase();
            } else {
                alias = name;
            }
        }

        String image = alias + ".png?";
        if (extra.length > 1) {
            image = extra[1];
        }

        Assert.assertEquals(name, user.getName());
        Assert.assertEquals(alias, user.getAlias());
        WebElement avatar = user.getAvatar();
        Assert.assertEquals(name, avatar.getAttribute("alt"));
        Assert.assertTrue(avatar.getAttribute("src").contains("/" + image));
    }

    @Test
    public void testSingleSelection()
    {
        UserPicker userPicker = new UserClassFieldEditPane(editor.addField("User").getName()).getUserPicker();

        // Use the keyboard.
        userPicker.sendKeys("mor").waitForSuggestions().sendKeys(Keys.ARROW_DOWN, Keys.ARROW_DOWN, Keys.ENTER);
        List<UserElement> selectedUsers = userPicker.waitForSuggestionsToFadeOut().getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals("", userPicker.getValue());
        // The link to clear the list of selected users should be displayed if at least 2 users are selected.
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());

        // Use the mouse. Since we have single selection by default, the previously selected user should be replaced.
        userPicker.sendKeys("mor").waitForSuggestions().select("Enygma2002");
        selectedUsers = userPicker.waitForSuggestionsToFadeOut().getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Eduard Moraru", "Enygma2002");
        Assert.assertEquals("", userPicker.getValue());
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());

        // Delete the selected user.
        selectedUsers.get(0).delete();
        Assert.assertEquals(0, userPicker.getAcceptedSuggestions().size());
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());

        // When there is only one suggestion, Enter key should select it.
        userPicker.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        selectedUsers = userPicker.waitForSuggestionsToFadeOut().getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Administrator", "Admin", "noavatar.png");
        Assert.assertEquals("", userPicker.getValue());
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());
    }

    @Test
    public void testMultipleSelection()
    {
        UserClassFieldEditPane userField = new UserClassFieldEditPane(editor.addField("User").getName());
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();
        UserPicker userPicker = userField.getUserPicker();

        // Select 2 users.
        userPicker.sendKeys("tmortagne").waitForSuggestions().sendKeys(Keys.ENTER).waitForSuggestionsToFadeOut();
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());
        userPicker.sendKeys("2002").waitForSuggestions().select("Enygma").waitForSuggestionsToFadeOut();
        Assert.assertTrue(userPicker.getClearSelectionLink().isDisplayed());
        List<UserElement> selectedUsers = userPicker.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Thomas Mortagne");
        assertUserElement(selectedUsers.get(1), "Eduard Moraru", "Enygma2002");
        Assert.assertEquals("", userPicker.getValue());

        // Delete the first selected user.
        selectedUsers.get(0).delete();
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());

        // Select another user.
        userPicker.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER).waitForSuggestionsToFadeOut();
        selectedUsers = userPicker.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Eduard Moraru", "Enygma2002");
        assertUserElement(selectedUsers.get(1), "Administrator", "Admin", "noavatar.png");
        Assert.assertEquals("", userPicker.getValue());

        // Change the order of the selected users.
        selectedUsers.get(1).moveBefore(selectedUsers.get(0));
        assertUserElement(userPicker.getAcceptedSuggestions().get(0), "Administrator", "Admin", "noavatar.png");

        // Clear the list of selected users.
        userPicker.getClearSelectionLink().click();
        Assert.assertFalse(userPicker.getClearSelectionLink().isDisplayed());
        Assert.assertEquals(0, userPicker.getAcceptedSuggestions().size());
    }

    @Test
    public void testSaveAndInitialSelection()
    {
        UserPicker userPicker = new UserClassFieldEditPane(editor.addField("User").getName()).getUserPicker();
        userPicker.sendKeys("thomas").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView().edit();

        UserClassFieldEditPane userField = new UserClassFieldEditPane("user1");
        userPicker = userField.getUserPicker().waitToLoad();
        List<UserElement> selectedUsers = userPicker.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals("", userPicker.getValue());

        // Enable multiple selection.
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();

        // Re-take the user picker because the display has been reloaded.
        userPicker = userField.getUserPicker();

        // Select one more user.
        userPicker.waitToLoad().sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new UserClassFieldEditPane("user1").getUserPicker().waitToLoad();
        selectedUsers = userPicker.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Thomas Mortagne");
        assertUserElement(selectedUsers.get(1), "Administrator", "Admin", "noavatar.png");
        Assert.assertEquals("", userPicker.getValue());

        // We should be able to input free text also.
        userPicker.sendKeys("foobar").waitForSuggestions().sendKeys(Keys.ESCAPE).waitForSuggestionsToFadeOut();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new UserClassFieldEditPane("user1").getUserPicker().waitToLoad();
        selectedUsers = userPicker.getAcceptedSuggestions();
        Assert.assertEquals(3, selectedUsers.size());
        assertUserElement(selectedUsers.get(2), "foobar", "foobar", "noavatar.png");
        Assert.assertEquals("", userPicker.getValue());

        // Delete the fake user.
        selectedUsers.get(2).delete();
        Assert.assertEquals(2, userPicker.getAcceptedSuggestions().size());

        // Delete all selected users.
        userPicker.getClearSelectionLink().click();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new UserClassFieldEditPane("user1").getUserPicker().waitToLoad();
        Assert.assertEquals(0, userPicker.getAcceptedSuggestions().size());
        Assert.assertEquals("", userPicker.getValue());
    }

    @Test
    public void testApplicationEntry()
    {
        // Create the application class.
        UserPicker userPicker = new UserClassFieldEditPane(editor.addField("User").getName()).getUserPicker();
        userPicker.sendKeys("thomas").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView();

        // Create the application entry.
        ClassSheetPage classSheetPage = new ClassSheetPage();
        InlinePage entryEditPage = classSheetPage.createNewDocument(getTestClassName(), getTestMethodName() + "Entry");

        // Assert the initial value.
        String id = getTestClassName() + "." + getTestMethodName() + "_0_user1";
        userPicker = new UserPicker(getDriver().findElement(By.id(id)));
        List<UserElement> selectedUsers = userPicker.waitToLoad().getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserElement(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals("", userPicker.getValue());

        // Change the selected user.
        userPicker.sendKeys("eduard").waitForSuggestions().sendKeys(Keys.ENTER).waitForSuggestionsToFadeOut();
        // We wait for the page to load because Selenium doesn't do it all the time when Save & View is clicked.
        entryEditPage.clickSaveAndView().waitUntilPageIsLoaded();

        // Assert the view mode.
        List<WebElement> users = getDriver().findElements(By.className("user"));
        Assert.assertEquals(1, users.size());
        assertUserElement(new UserElement(users.get(0)), "Eduard Moraru", "Enygma2002");
    }
}

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.xwiki.appwithinminutes.test.po.SuggestClassFieldEditPane;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;
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
        SuggestInputElement userPicker = new SuggestClassFieldEditPane(editor.addField("User").getName()).getPicker();

        // The suggestions should be case-insensitive. Match the last name.
        List<SuggestionElement> suggestions = userPicker.sendKeys("mOr").waitForSuggestions().getSuggestions();
        Assert.assertEquals(2, suggestions.size());
        assertUserSuggestion(suggestions.get(0), "Thomas Mortagne");
        assertUserSuggestion(suggestions.get(1), "Eduard Moraru", "Enygma2002");

        // Match the first name.
        suggestions = userPicker.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, "As").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserSuggestion(suggestions.get(0), "Thomas Mortagne");

        // Match the alias.
        suggestions = userPicker.sendKeys(Keys.BACK_SPACE, "20").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserSuggestion(suggestions.get(0), "Eduard Moraru", "Enygma2002");

        // The guest user shouldn't be suggested.
        suggestions = userPicker.clear().sendKeys("guest").waitForSuggestions().getSuggestions();
        Assert.assertTrue(suggestions.isEmpty());

        // Default administrator user should be suggested.
        suggestions = userPicker.clear().sendKeys("admin").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        assertUserSuggestion(suggestions.get(0), "Administrator", "Admin", "fa-user");

        // "a" should bring many suggestions. Also, a single letter should bring suggestions.
        Assert.assertTrue(userPicker.clear().sendKeys("a").waitForSuggestions().getSuggestions().size() > 2);

        // An empty text input brings a default list of suggestions. There should be at least 3 users (the 2 users we
        // created plus the default administrator).
        Assert.assertTrue(userPicker.sendKeys(Keys.BACK_SPACE).waitForSuggestions().getSuggestions().size() > 2);

        // We should be able to close the list of suggestions using the escape key.
        Assert.assertTrue(
            userPicker.sendKeys("mor").waitForSuggestions().sendKeys(Keys.ESCAPE).getSuggestions().isEmpty());
    }

    /**
     * Asserts the given user matches the expectations.
     * 
     * @param user the user to assert
     * @param name the expected name
     * @param extra extra user fields (alias, image, etc.)
     */
    private void assertUserSuggestion(SuggestionElement user, String name, String... extra)
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

        Assert.assertEquals(name, user.getLabel());
        
        String value = user.getValue();
        if (value.startsWith("XWiki.")) {
            Assert.assertEquals("XWiki." + alias, value);
        } else {
            Assert.assertEquals(alias, value);
        }

        if (image != null) {
            Assert.assertTrue(user.getIcon().contains(image));
        } else {
            try {
                user.getIcon();
                Assert.fail();
            } catch (NoSuchElementException e) {
            }
        }
    }

    @Test
    public void testSingleSelection()
    {
        SuggestInputElement userPicker = new SuggestClassFieldEditPane(editor.addField("User").getName()).getPicker();

        // Use the keyboard.
        userPicker.sendKeys("mor").waitForSuggestions().sendKeys(Keys.ARROW_DOWN, Keys.ENTER);
        List<SuggestionElement> selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Eduard Moraru", "Enygma2002");
        Assert.assertEquals(Collections.singletonList("XWiki.Enygma2002"), userPicker.getValues());

        // Use the mouse. Since we have single selection by default, the previously selected user should be replaced.
        selectedUsers = userPicker.click().selectByVisibleText("Thomas Mortagne").getSelectedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals(Collections.singletonList("XWiki.tmortagne"), userPicker.getValues());

        // Delete the selected user.
        selectedUsers.get(0).delete();
        Assert.assertEquals(0, userPicker.getSelectedSuggestions().size());
        Assert.assertEquals(Collections.singletonList(""), userPicker.getValues());

        // When there is only one suggestion, Enter key should select it.
        userPicker.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Administrator", "Admin", "fa-user");
        Assert.assertEquals(Collections.singletonList("XWiki.Admin"), userPicker.getValues());
    }

    @Test
    public void testMultipleSelection()
    {
        SuggestClassFieldEditPane userField = new SuggestClassFieldEditPane(editor.addField("User").getName());
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();
        SuggestInputElement userPicker = userField.getPicker();

        // Select 2 users.
        userPicker.sendKeys("tmortagne").waitForSuggestions().sendKeys(Keys.ENTER);
        userPicker.sendKeys("2002").waitForSuggestions().selectByValue("XWiki.Enygma2002");
        List<SuggestionElement> selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Thomas Mortagne");
        assertUserSuggestion(selectedUsers.get(1), "Eduard Moraru", "Enygma2002");
        Assert.assertEquals(Arrays.asList("XWiki.tmortagne", "XWiki.Enygma2002"), userPicker.getValues());

        // Delete the first selected user.
        selectedUsers.get(0).delete();

        // Select another user.
        userPicker.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Administrator", "Admin", "fa-user");
        assertUserSuggestion(selectedUsers.get(1), "Eduard Moraru", "Enygma2002");
        Assert.assertEquals(Arrays.asList("XWiki.Admin", "XWiki.Enygma2002"), userPicker.getValues());

        // Clear the list of selected users.
        userPicker.clearSelectedSuggestions();
        Assert.assertEquals(0, userPicker.getSelectedSuggestions().size());
        Assert.assertTrue(userPicker.getValues().isEmpty());
    }

    @Test
    public void testSaveAndInitialSelection()
    {
        SuggestInputElement userPicker = new SuggestClassFieldEditPane(editor.addField("User").getName()).getPicker();
        userPicker.sendKeys("thomas").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView().edit();

        SuggestClassFieldEditPane userField = new SuggestClassFieldEditPane("user1");
        userPicker = userField.getPicker();
        List<SuggestionElement> selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals(Collections.singletonList("XWiki.tmortagne"), userPicker.getValues());

        // Enable multiple selection.
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();

        // Re-take the user picker because the display has been reloaded.
        userPicker = userField.getPicker();

        // Select one more user.
        userPicker.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new SuggestClassFieldEditPane("user1").getPicker();
        selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Thomas Mortagne");
        assertUserSuggestion(selectedUsers.get(1), "Administrator", "Admin", "fa-user");
        Assert.assertEquals(Arrays.asList("XWiki.tmortagne", "XWiki.Admin"), userPicker.getValues());

        // We should be able to input free text also.
        userPicker.sendKeys("foobar").waitForSuggestions().selectTypedText();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new SuggestClassFieldEditPane("user1").getPicker();
        selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(3, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(2), "foobar", "foobar", null);
        Assert.assertEquals(Arrays.asList("XWiki.tmortagne", "XWiki.Admin", "foobar"), userPicker.getValues());

        // Delete the fake user.
        selectedUsers.get(2).delete();
        Assert.assertEquals(2, userPicker.getSelectedSuggestions().size());

        // Delete all selected users.
        userPicker.clearSelectedSuggestions();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        userPicker = new SuggestClassFieldEditPane("user1").getPicker();
        Assert.assertEquals(0, userPicker.getSelectedSuggestions().size());
        Assert.assertTrue(userPicker.getValues().isEmpty());
    }

    @Test
    public void testApplicationEntry()
    {
        // Create the application class.
        SuggestInputElement userPicker = new SuggestClassFieldEditPane(editor.addField("User").getName()).getPicker();
        userPicker.sendKeys("thomas").waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView();

        // Create the application entry.
        ClassSheetPage classSheetPage = new ClassSheetPage();
        InlinePage entryEditPage = classSheetPage.createNewDocument(getTestClassName(), getTestMethodName() + "Entry");

        // Assert the initial value.
        String id = getTestClassName() + "." + getTestMethodName() + "_0_user1";
        userPicker = new SuggestInputElement(getDriver().findElement(By.id(id)));
        List<SuggestionElement> selectedUsers = userPicker.getSelectedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        assertUserSuggestion(selectedUsers.get(0), "Thomas Mortagne");
        Assert.assertEquals(Collections.singletonList("XWiki.tmortagne"), userPicker.getValues());

        // Change the selected user.
        userPicker.clearSelectedSuggestions().sendKeys("eduard").waitForSuggestions().sendKeys(Keys.ENTER);
        // We wait for the page to load because Selenium doesn't do it all the time when Save & View is clicked.
        entryEditPage.clickSaveAndView().waitUntilPageIsLoaded();

        // Assert the view mode.
        List<WebElement> users = getDriver().findElements(By.className("user"));
        Assert.assertEquals(1, users.size());
        Assert.assertEquals("Eduard Moraru", users.get(0).getText());
        Assert.assertTrue(
            users.get(0).findElement(By.className("user-avatar")).getAttribute("src").contains("/Enygma2002.png?"));
    }
}

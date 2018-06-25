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
import org.xwiki.test.ui.po.SelectizeElement;
import org.xwiki.test.ui.po.SelectizeElement.SelectizeElementItem;
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
        SelectizeElement selectize = new UserClassFieldEditPane(editor.addField("User").getName()).getSelectize();

        // The suggestions should be case-insensitive. Match the last name.
        List<SelectizeElementItem> suggestions = selectize.sendKeys("mOr").waitForSuggestions().getSuggestions();
        Assert.assertEquals(2, suggestions.size());
        Assert.assertEquals("Thomas Mortagne", suggestions.get(0).getLabel());
        Assert.assertEquals("Eduard Moraru", suggestions.get(1).getLabel());

        // Match the first name.
        suggestions = selectize.clear().sendKeys("As").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("Thomas Mortagne", suggestions.get(0).getLabel());

        // Match the alias.
        suggestions = selectize.clear().sendKeys("20").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("Eduard Moraru", suggestions.get(0).getLabel());

        // The guest user shouldn't be suggested.
        suggestions = selectize.clear().sendKeys("guest").waitForSuggestions().getSuggestions();
        Assert.assertEquals(0, suggestions.size());

        // Default administrator user should be suggested.
        suggestions = selectize.clear().sendKeys("admin").waitForSuggestions().getSuggestions();
        Assert.assertEquals(1, suggestions.size());
        Assert.assertEquals("Administrator", suggestions.get(0).getLabel());

        // "a" should bring many suggestions. Also, a single letter should bring suggestions.
        suggestions = selectize.clear().sendKeys("a").waitForSuggestions().getSuggestions();
        Assert.assertTrue(suggestions.size() > 2);
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
        SelectizeElement selectize = new UserClassFieldEditPane(editor.addField("User").getName()).getSelectize();

        selectize.sendKeys("mor").waitForSuggestions().sendKeys(Keys.ENTER);
        List<SelectizeElementItem> selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        Assert.assertEquals("Thomas Mortagne", selectedUsers.get(0).getLabel());
        Assert.assertEquals("", selectize.getValue());

        selectize.deleteItems().sendKeys("mor").waitForSuggestions().select("XWiki.Enygma2002");
        selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        Assert.assertEquals("Eduard Moraru", selectedUsers.get(0).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Delete the selected user.
        selectedUsers.get(0).delete();
        Assert.assertEquals(0, selectize.getAcceptedSuggestions().size());

        // When there is only one suggestion, Enter key should select it.
        selectize.sendKeys("admin").waitForSuggestions().sendKeys(Keys.ENTER);
        selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        Assert.assertEquals("Administrator", selectedUsers.get(0).getLabel());
        Assert.assertEquals("", selectize.getValue());
    }

    @Test
    public void testMultipleSelection()
    {
        UserClassFieldEditPane userField = new UserClassFieldEditPane(editor.addField("User").getName());
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();
        SelectizeElement selectize = userField.getSelectize();

        // Select 2 users.
        selectize.sendKeys("tmortagne").waitForSuggestions().selectFirst();
        selectize.sendKeys("2002").waitForSuggestions().select("XWiki.Enygma2002");
        List<SelectizeElementItem> selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        Assert.assertEquals("Thomas Mortagne", selectedUsers.get(0).getLabel());
        Assert.assertEquals("Eduard Moraru", selectedUsers.get(1).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Delete the first selected user.
        selectedUsers.get(0).delete();
        Assert.assertTrue(selectize.getAcceptedSuggestions().stream().noneMatch(
            item -> "XWiki.tmortagne".equals(item.getLabel())));

        // Select another user.
        selectize.sendKeys("admin").waitForSuggestions().selectFirst();
        selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        Assert.assertEquals("Administrator", selectedUsers.get(0).getLabel());
        Assert.assertEquals("Eduard Moraru", selectedUsers.get(1).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Clear the list of selected users.
        selectize.deleteItems();
        Assert.assertEquals(0, selectize.getAcceptedSuggestions().size());
    }

    @Test
    public void testSaveAndInitialSelection()
    {
        SelectizeElement selectize = new UserClassFieldEditPane(editor.addField("User").getName()).getSelectize();
        selectize.sendKeys("thomas").waitForSuggestions().selectFirst();
        editor.clickSaveAndView().edit();

        UserClassFieldEditPane userField = new UserClassFieldEditPane("user1");
        selectize = userField.getSelectize();
        List<SelectizeElementItem> selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        Assert.assertEquals("Thomas Mortagne", selectedUsers.get(0).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Enable multiple selection.
        userField.openConfigPanel();
        userField.setMultipleSelect(true);
        userField.closeConfigPanel();

        // Re-take the user picker because the display has been reloaded.
        selectize = userField.getSelectize();

        // Select one more user.
        selectize.sendKeys("admin").waitForSuggestions().selectFirst();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        selectize = new UserClassFieldEditPane("user1").getSelectize();
        selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(2, selectedUsers.size());
        Assert.assertEquals("Thomas Mortagne", selectedUsers.get(0).getLabel());
        Assert.assertEquals("Administrator", selectedUsers.get(1).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // We should be able to input free text also.
        selectize.sendKeys("foobar").waitForSuggestions().selectCreate();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        selectize = new UserClassFieldEditPane("user1").getSelectize();
        selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(3, selectedUsers.size());
        Assert.assertEquals("foobar", selectedUsers.get(2).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Delete the fake user.
        selectedUsers.get(2).delete();
        Assert.assertEquals(2, selectedUsers.size());

        // Delete all selected users.
        selectize.deleteItems();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        selectize = new UserClassFieldEditPane("user1").getSelectize();
        Assert.assertEquals(0, selectize.getAcceptedSuggestions().size());
        Assert.assertEquals("", selectize.getValue());
    }

    @Test
    public void testApplicationEntry()
    {
        // Create the application class.
        SelectizeElement selectize = new UserClassFieldEditPane(editor.addField("User").getName()).getSelectize();
        selectize.sendKeys("thomas").waitForSuggestions().selectFirst();
        editor.clickSaveAndView();

        // Create the application entry.
        ClassSheetPage classSheetPage = new ClassSheetPage();
        InlinePage entryEditPage = classSheetPage.createNewDocument(getTestClassName(), getTestMethodName() + "Entry");

        // Assert the initial value.
        String id = getTestClassName() + "." + getTestMethodName() + "_0_user1";
        selectize = new SelectizeElement(getDriver().findElement(By.id(id)));
        List<SelectizeElementItem> selectedUsers = selectize.getAcceptedSuggestions();
        Assert.assertEquals(1, selectedUsers.size());
        Assert.assertEquals("Thomas Mortagne", selectedUsers.get(0).getLabel());
        Assert.assertEquals("", selectize.getValue());

        // Change the selected user.
        selectize.deleteItems();
        selectize.sendKeys("eduard").waitForSuggestions().selectFirst();
        // We wait for the page to load because Selenium doesn't do it all the time when Save & View is clicked.
        entryEditPage.clickSaveAndView().waitUntilPageIsLoaded();

        // Assert the view mode.
        List<WebElement> users = getDriver().findElements(By.className("user"));
        Assert.assertEquals(1, users.size());
        assertUserElement(new UserElement(users.get(0)), "Eduard Moraru", "Enygma2002");
    }
}

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
package org.xwiki.appwithinminutes.test.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.SuggestClassFieldEditPane;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InlinePage;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;
import org.xwiki.xclass.test.po.ClassSheetPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Special class editor tests that address only the Page class field type.
 *
 * @version $Id$
 * @since 10.6
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class PageClassFieldIT
{
    /**
     * The fixture pages live in a dedicated flat space so that the page picker suggestion hints (which display the
     * space path) are deterministic and the page values are simple {@code space.page} references.
     */
    private static final String SPACE_NAME = "PageClassFieldIT";

    private static final String PAGE1 = "pageclassfieldpage1";

    private static final String PAGE2 = "pageclassfieldpage2";

    private static final String TEST_HOME = "pageclassfieldtesthome";

    @BeforeAll
    void beforeAll(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Create the fixture pages used to feed the page picker suggestions. They are shared by all the tests, so we
        // create them only once.
        SpaceReference fixtureSpace = new SpaceReference("xwiki", SPACE_NAME);
        setup.deleteSpace(fixtureSpace);
        setup.rest().savePage(new DocumentReference(PAGE1, fixtureSpace), "Content", SPACE_NAME + " Page 1");
        setup.rest().savePage(new DocumentReference(PAGE2, fixtureSpace), "Content", SPACE_NAME + " Page 2");
        setup.rest().savePage(new DocumentReference(TEST_HOME, new SpaceReference("space", fixtureSpace)),
            "Content", SPACE_NAME + " TestHome");
    }

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deleteSpace(testReference.getLastSpaceReference());
    }

    @Test
    @Order(1)
    void suggestions(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        SuggestInputElement pagePicker = new SuggestClassFieldEditPane(editor.addField("Page").getName()).getPicker();

        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();

        List<SuggestionElement> suggestions =
            pagePicker.sendKeys(SPACE_NAME, " pag").waitForSuggestions().getSuggestions();
        assertEquals(2, suggestions.size());
        assertEquals(SPACE_NAME + " Page 1", suggestions.get(0).getLabel());
        assertEquals(SPACE_NAME, suggestions.get(0).getHint());
        assertEquals(SPACE_NAME + " Page 2", suggestions.get(1).getLabel());
        assertEquals(SPACE_NAME, suggestions.get(1).getHint());

        suggestions = pagePicker.sendKeys(" 1").waitForSuggestions().getSuggestions();
        assertEquals(1, suggestions.size());
        assertEquals(SPACE_NAME + " Page 1", suggestions.get(0).getLabel());

        suggestions = pagePicker.clear().sendKeys(SPACE_NAME).waitForSuggestions().getSuggestions();
        assertEquals(3, suggestions.size());
        assertEquals(SPACE_NAME + " Page 1", suggestions.get(0).getLabel());
        assertEquals(SPACE_NAME, suggestions.get(0).getHint());
        assertEquals(SPACE_NAME + " Page 2", suggestions.get(1).getLabel());
        assertEquals(SPACE_NAME, suggestions.get(1).getHint());
        assertEquals(SPACE_NAME + " TestHome", suggestions.get(2).getLabel());
        assertEquals(SPACE_NAME + " / space", suggestions.get(2).getHint());
    }

    @Test
    @Order(2)
    void singleSelection(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        SuggestInputElement pagePicker = new SuggestClassFieldEditPane(editor.addField("Page").getName()).getPicker();

        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();

        // Use the keyboard.
        List<SuggestionElement> selectedPages = pagePicker.sendKeys(SPACE_NAME).waitForSuggestions()
            .sendKeys(Keys.ENTER).getSelectedSuggestions();
        assertEquals(1, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 1", selectedPages.get(0).getLabel());
        assertEquals(Collections.singletonList(SPACE_NAME + "." + PAGE1), pagePicker.getValues());

        // Use the mouse. Since we have single selection by default, the previously selected page should be replaced.
        selectedPages = pagePicker.click().selectByVisibleText(SPACE_NAME + " Page 2").getSelectedSuggestions();
        assertEquals(1, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 2", selectedPages.get(0).getLabel());
        assertEquals(Collections.singletonList(SPACE_NAME + "." + PAGE2), pagePicker.getValues());

        // Delete the selected page.
        selectedPages.get(0).delete();
        assertEquals(0, pagePicker.getSelectedSuggestions().size());
        assertEquals(Collections.singletonList(""), pagePicker.getValues());
    }

    @Test
    @Order(3)
    void multipleSelection(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        SuggestClassFieldEditPane pageField = new SuggestClassFieldEditPane(editor.addField("Page").getName());
        pageField.openConfigPanel();
        pageField.setMultipleSelect(true);
        pageField.closeConfigPanel();
        SuggestInputElement pagePicker = pageField.getPicker();

        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();

        // Select 2 pages.
        List<SuggestionElement> selectedPages = pagePicker.sendKeys(SPACE_NAME).waitForSuggestions()
            .sendKeys(Keys.ENTER).sendKeys(Keys.ENTER).getSelectedSuggestions();
        assertEquals(2, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 1", selectedPages.get(0).getLabel());
        assertEquals(SPACE_NAME + " Page 2", selectedPages.get(1).getLabel());
        assertEquals(Arrays.asList(SPACE_NAME + "." + PAGE1, SPACE_NAME + "." + PAGE2), pagePicker.getValues());

        // Delete the first selected page.
        selectedPages.get(0).delete();

        // Select another page.
        pagePicker.sendKeys(SPACE_NAME, " testhome").waitForSuggestions().sendKeys(Keys.ENTER);
        selectedPages = pagePicker.getSelectedSuggestions();
        assertEquals(2, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 2", selectedPages.get(0).getLabel());
        assertEquals(SPACE_NAME + " TestHome", selectedPages.get(1).getLabel());
        assertEquals(Arrays.asList(SPACE_NAME + "." + PAGE2, SPACE_NAME + ".space." + TEST_HOME),
            pagePicker.getValues());

        // Clear the list of selected pages.
        pagePicker.clearSelectedSuggestions();
        assertEquals(0, pagePicker.getSelectedSuggestions().size());
        assertTrue(pagePicker.getValues().isEmpty());
    }

    @Test
    @Order(4)
    void saveAndInitialSelection(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        SuggestInputElement pagePicker = new SuggestClassFieldEditPane(editor.addField("Page").getName()).getPicker();
        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();
        pagePicker.sendKeys(SPACE_NAME).waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView().edit();

        SuggestClassFieldEditPane pageField = new SuggestClassFieldEditPane("page1");
        pagePicker = pageField.getPicker();
        List<SuggestionElement> selectedPages = pagePicker.getSelectedSuggestions();
        assertEquals(1, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 1", selectedPages.get(0).getLabel());
        assertEquals(Collections.singletonList(SPACE_NAME + "." + PAGE1), pagePicker.getValues());

        // Enable multiple selection.
        pageField.openConfigPanel();
        pageField.setMultipleSelect(true);
        pageField.closeConfigPanel();

        // Re-take the page picker because the display has been reloaded.
        pagePicker = pageField.getPicker();

        // Select one more page.
        pagePicker.click().waitForSuggestions().sendKeys(SPACE_NAME).waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        pagePicker = new SuggestClassFieldEditPane("page1").getPicker();
        selectedPages = pagePicker.getSelectedSuggestions();
        assertEquals(2, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 1", selectedPages.get(0).getLabel());
        assertEquals(SPACE_NAME + " Page 2", selectedPages.get(1).getLabel());
        assertEquals(Arrays.asList(SPACE_NAME + "." + PAGE1, SPACE_NAME + "." + PAGE2), pagePicker.getValues());

        // We should be able to input free text also.
        pagePicker.click().waitForSuggestions().sendKeys("foobar").waitForSuggestions().selectTypedText();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        pagePicker = new SuggestClassFieldEditPane("page1").getPicker();
        selectedPages = pagePicker.getSelectedSuggestions();
        assertEquals(3, selectedPages.size());
        assertEquals("foobar", selectedPages.get(2).getLabel());
        assertEquals(Arrays.asList(SPACE_NAME + "." + PAGE1, SPACE_NAME + "." + PAGE2, "foobar"),
            pagePicker.getValues());

        // Delete the fake page.
        selectedPages.get(2).delete();
        assertEquals(2, pagePicker.getSelectedSuggestions().size());

        // Delete all selected pages.
        pagePicker.clearSelectedSuggestions();
        editor.clickSaveAndContinue();
        editor.clickCancel().edit();

        pagePicker = new SuggestClassFieldEditPane("page1").getPicker();
        assertEquals(0, pagePicker.getSelectedSuggestions().size());
        assertTrue(pagePicker.getValues().isEmpty());
    }

    @Test
    @Order(5)
    void applicationEntry(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        // Create the application class.
        SuggestInputElement pagePicker = new SuggestClassFieldEditPane(editor.addField("Page").getName()).getPicker();
        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();
        pagePicker.sendKeys(SPACE_NAME).waitForSuggestions().sendKeys(Keys.ENTER);
        editor.clickSaveAndView();

        // Create the application entry.
        ClassSheetPage classSheetPage = new ClassSheetPage();
        String location = setup.serializeLocalReference(testReference.getLastSpaceReference());
        InlinePage entryEditPage = classSheetPage.createNewDocument(location, "Entry");

        // Assert the initial value.
        String className = setup.serializeReference(
            new DocumentReference("Class", testReference.getLastSpaceReference()).getLocalDocumentReference());
        String id = className + "_0_page1";
        pagePicker = new SuggestInputElement(setup.getDriver().findElement(By.id(id)));
        List<SuggestionElement> selectedPages = pagePicker.getSelectedSuggestions();
        assertEquals(1, selectedPages.size());
        assertEquals(SPACE_NAME + " Page 1", selectedPages.get(0).getLabel());
        assertEquals(Collections.singletonList(SPACE_NAME + "." + PAGE1), pagePicker.getValues());

        // Make sure the picker is ready.
        pagePicker.click().waitForSuggestions();
        pagePicker.sendKeys(SPACE_NAME).waitForSuggestions().sendKeys(Keys.ENTER);
        // Change the selected page.
        pagePicker.clearSelectedSuggestions().sendKeys(SPACE_NAME, " testh").waitForSuggestions().sendKeys(Keys.ENTER);
        entryEditPage.clickSaveAndView();

        // Assert the view mode.
        List<WebElement> pages = setup.getDriver().findElements(By.cssSelector("#xwikicontent dd a"));
        assertEquals(1, pages.size());
        assertEquals(SPACE_NAME + " TestHome", pages.get(0).getText());
    }

    private ApplicationClassEditPage goToEditor(TestReference testReference)
    {
        return ApplicationClassEditPage
            .goToEditor(new DocumentReference("Class", testReference.getLastSpaceReference()));
    }
}

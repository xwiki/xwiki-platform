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
package org.xwiki.flamingo.test.docker;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Space Picker.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@UITest
class SpacePickerIT
{
    private static final String PICKER_ID = "spacePickerTest";

    private static final String PICKER_TEMPLATE =
        "{{velocity}}{{html}}#spacePicker({'id': '%s', 'multiple': true}){{/html}}{{/velocity}}";

    private static final String WEB_HOME = "WebHome";

    /**
     * A space is displayed with the title of the page backing it, so it must also be possible to find it by that title.
     */
    @Test
    @Order(1)
    void suggestsSpaceByTheTitleOfItsHomePage(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        // The name of the space doesn't contain the searched text, so that we know the suggestion was matched on the
        // title of its home page.
        SpaceReference spaceReference = new SpaceReference("Container", reference.getLastSpaceReference());
        String title = reference.getLastSpaceReference().getName() + "TitleOnly";
        setup.rest().savePage(new DocumentReference(WEB_HOME, spaceReference), "", title);

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions = picker.sendKeys(title).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(1, suggestions.size());
        assertEquals(title, suggestions.get(0).getLabel());

        // The value is the reference of the space, not the reference of the page backing it.
        picker.selectByVisibleText(title);
        assertEquals(List.of(setup.serializeLocalReference(spaceReference)), picker.getValues());
    }

    /**
     * Searching for pages can't match the name of a space, because the page backing a space is always named WebHome,
     * which is why the picker also searches for spaces. See XWIKI-23834.
     */
    @Test
    @Order(2)
    void suggestsSpaceByItsName(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String spaceName = reference.getLastSpaceReference().getName() + "NameOnly";
        SpaceReference spaceReference = new SpaceReference(spaceName, reference.getLastSpaceReference());
        // The title of the home page doesn't contain the name of the space, so that we know the suggestion was matched
        // on the name of the space.
        setup.rest().savePage(new DocumentReference(WEB_HOME, spaceReference), "", "Unrelated Title");

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions = picker.sendKeys(spaceName).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(List.of(setup.serializeLocalReference(spaceReference)),
            suggestions.stream().map(SuggestionElement::getValue).toList());
    }

    /**
     * Only the pages backing a space are spaces, so the terminal pages must not be suggested.
     */
    @Test
    @Order(3)
    void doesNotSuggestTerminalPages(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String searchedText = reference.getLastSpaceReference().getName() + "Terminal";
        // A terminal page is not a space, so it must not be suggested, even though its title matches.
        setup.rest().savePage(new DocumentReference(searchedText, reference.getLastSpaceReference()), "", searchedText);
        // A nested page is a space, so it must be suggested.
        SpaceReference spaceReference =
            new SpaceReference(searchedText + "Nested", reference.getLastSpaceReference());
        setup.rest().savePage(new DocumentReference(WEB_HOME, spaceReference), "", searchedText + "Nested");

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions =
            picker.sendKeys(searchedText).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(List.of(setup.serializeLocalReference(spaceReference)),
            suggestions.stream().map(SuggestionElement::getValue).toList());
    }

    /**
     * The hierarchy of a space is displayed as a hint, so that two spaces with the same name can be told apart.
     */
    @Test
    @Order(4)
    void showsTheHierarchyOfTheSuggestedSpace(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String parentTitle = reference.getLastSpaceReference().getName() + "ParentTitle";
        SpaceReference parentReference = new SpaceReference("Parent", reference.getLastSpaceReference());
        setup.rest().savePage(new DocumentReference(WEB_HOME, parentReference), "", parentTitle);
        String childTitle = reference.getLastSpaceReference().getName() + "ChildTitle";
        SpaceReference childReference = new SpaceReference("Child", parentReference);
        setup.rest().savePage(new DocumentReference(WEB_HOME, childReference), "", childTitle);

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions = picker.sendKeys(childTitle).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(1, suggestions.size());
        assertEquals(childTitle, suggestions.get(0).getLabel());
        // The hint is the hierarchy of the space, without the space itself.
        String hint = suggestions.get(0).getHint();
        assertTrue(hint.contains(parentTitle), "The hint [%s] doesn't contain the parent [%s].".formatted(hint,
            parentTitle));
        assertFalse(hint.contains(childTitle), "The hint [%s] contains the space itself.".formatted(hint));
    }

    /**
     * No suggestion should be displayed, and no error raised, when nothing matches the searched text.
     */
    @Test
    @Order(5)
    void suggestsNothingWhenNoMatch(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String searchedText = reference.getLastSpaceReference().getName() + "NoMatch";

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions =
            picker.sendKeys(searchedText).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(0, suggestions.size());
    }

    /**
     * Verify that spaces with unicode names/titles can be found regardless of case and diacritics, just like pages.
     * See {@code PagePickerIT#searchCaseInsensitiveUnicode}.
     */
    @ParameterizedTest
    @ValueSource(strings = { "ähm", "töst", "école", "hôtelière" })
    @Order(6)
    void searchCaseInsensitiveUnicode(String searchText, TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String title = "École hôtelière";
        SpaceReference spaceReference = new SpaceReference("ÄhmTöst", reference.getLastSpaceReference());
        setup.rest().savePage(new DocumentReference(WEB_HOME, spaceReference), "", title);

        SuggestInputElement picker = displayPicker(setup, reference);

        List<SuggestionElement> suggestions = picker.sendKeys(searchText).waitForNonTypedSuggestions().getSuggestions();
        assertEquals(1, suggestions.size(), "Didn't find anything searching for %s".formatted(searchText));
        assertEquals(title, suggestions.get(0).getLabel());
    }

    /**
     * Displays the picker on the test page and returns it, ready to be used.
     */
    private SuggestInputElement displayPicker(TestUtils setup, TestReference reference)
    {
        setup.createPage(reference, String.format(PICKER_TEMPLATE, PICKER_ID),
            reference.getLastSpaceReference().getName());
        return new SuggestInputElement(setup.getDriver().findElementWithoutWaiting(By.id(PICKER_ID)));
    }
}

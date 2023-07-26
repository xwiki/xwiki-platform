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
package org.xwiki.user.test.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Functional tests for the user picker.
 *
 * @version $Id$
 * @since 14.10.12
 * @since 15.5RC1
 */
@UITest(properties = {
    // Use the user address as qualifier when displaying users in compact mode.
    "xwikiPropertiesAdditionalProperties=user.display.qualifierProperty=address"})
public class UserPickerIT
{
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        setup.createUser("mflorea", "pass", "", "first_name", "Marius", "last_name", "Florea", "address",
            "Iasi, Romania");
        setup.createUser("vmassol", "pass", "", "first_name", "Vincent", "last_name", "Massol");
    }

    @Test
    @Order(1)
    public void userQualifierProperty(TestUtils setup, TestReference testReference) throws Exception
    {
        SuggestInputElement userPicker =
            createUserPicker(setup, testReference, true, Collections.singletonMap("id", "testAuthor"));

        List<SuggestionElement> suggestions = userPicker.sendKeys("ma").waitForSuggestions().getSuggestions();
        assertEquals(2, suggestions.size());
        assertEquals("Iasi, Romania", suggestions.get(0).getHint());
        assertFalse(suggestions.get(1).hasHint());

        userPicker.selectByVisibleText("Marius Florea").selectByValue("XWiki.vmassol");
        suggestions = userPicker.getSelectedSuggestions();
        assertEquals(2, suggestions.size());
        // The qualifier property is displayed as tool tip (on hover) instead of hint once the user is selected (because
        // the text input height is limited).
        assertFalse(suggestions.get(0).hasHint());
        assertEquals("Iasi, Romania", suggestions.get(0).getTooltip());
        assertFalse(suggestions.get(1).hasHint());
        assertEquals("", suggestions.get(1).getTooltip());
    }

    private SuggestInputElement createUserPicker(TestUtils setup, TestReference testReference, boolean multiSelect,
        Map<String, Object> parameters) throws JsonProcessingException
    {
        StringBuilder content = new StringBuilder();
        content.append("{{velocity}}\n");
        content.append("{{html}}\n");
        content.append("#userPicker(").append(multiSelect).append(", ")
            .append(this.objectMapper.writeValueAsString(parameters)).append(")\n");
        content.append("{{/html}}\n");
        content.append("{{/velocity}}");
        setup.createPage(testReference, content.toString());
        return new SuggestInputElement(
            setup.getDriver().findElementWithoutWaiting(By.id(Objects.toString(parameters.get("id"), null))));
    }
}

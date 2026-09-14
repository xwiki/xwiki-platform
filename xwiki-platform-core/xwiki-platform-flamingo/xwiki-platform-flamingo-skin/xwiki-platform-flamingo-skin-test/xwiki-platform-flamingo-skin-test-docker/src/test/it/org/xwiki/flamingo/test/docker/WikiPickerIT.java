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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.SuggestInputElement.SuggestionElement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Functional tests for the Wiki Picker.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@UITest
class WikiPickerIT
{
    private static final String PICKER_ID = "wikiPickerTest";

    private static final String PICKER_TEMPLATE =
        "{{velocity}}{{html}}#wikiPicker({'id': '%s', 'multiple': true}){{/html}}{{/velocity}}";

    /**
     * The main wiki must be suggested, and selecting it must round-trip to the same value being saved in the macro
     * call.
     */
    @Test
    void suggestsAndSelectsTheMainWiki(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.createPage(reference, String.format(PICKER_TEMPLATE, PICKER_ID),
            reference.getLastSpaceReference().getName());

        SuggestInputElement picker =
            new SuggestInputElement(setup.getDriver().findElementWithoutWaiting(By.id(PICKER_ID)));

        List<SuggestionElement> suggestions = picker.sendKeys("xwiki").waitForNonTypedSuggestions().getSuggestions();
        assertTrue(suggestions.stream().anyMatch(suggestion -> "xwiki".equals(suggestion.getValue())),
            "The main wiki should be suggested.");

        picker.selectByValue("xwiki");
        assertEquals(List.of("xwiki"), picker.getValues());
    }
}

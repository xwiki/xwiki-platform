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
import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests for the Page Picker.
 *
 * @version $Id$
 */
@UITest
class PagePickerIT
{
    private static final String PICKER_ID = "pagePickerTest";

    /**
     * See XWIKI-16078: Selected (and deleted) users/subgroups are not displayed properly in drop-down when editing a
     * group
     */
    @Test
    @Order(1)
    void unselectingAPageDoesNotRemoveItFromSuggestions(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        String pageName = reference.getLastSpaceReference().getName();
        setup.createPage(reference, String.format(
                "{{velocity}}{{html}}#pagePicker({'id': '%s', 'multiple': true}){{/html}}{{/velocity}}", PICKER_ID),
            pageName);

        SuggestInputElement pagePicker =
            new SuggestInputElement(setup.getDriver().findElementWithoutWaiting(By.id(PICKER_ID)));

        // Make sure the picker is ready. TODO: remove once XWIKI-19056 is closed.
        pagePicker.click().waitForSuggestions();

        pagePicker.sendKeys(pageName.substring(0, 3)).waitForSuggestions().selectByVisibleText(pageName);
        pagePicker.clearSelectedSuggestions().sendKeys(pageName.substring(0, 3)).waitForSuggestions()
            .selectByVisibleText(pageName);
    }

    /**
     * Verify that spaces can be selected even if they contain many children.
     */
    @Test
    @Order(2)
    void selectSpaceWithManyChildren(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();

        String childName = "Child";
        for (int i = 0; i < 11; i++) {
            DocumentReference childReference = new DocumentReference(childName + i,
                reference.getLastSpaceReference());
            setup.createPage(childReference, "Test page " + i, "Child page " + i);
        }

        String pageName = reference.getLastSpaceReference().getName();
        setup.createPage(reference,
            String.format("{{velocity}}{{html}}#pagePicker({'id': '%s'}){{/html}}{{/velocity}}", PICKER_ID),
            pageName);

        SuggestInputElement pagePicker =
            new SuggestInputElement(setup.getDriver().findElementWithoutWaiting(By.id(PICKER_ID)));

        // Search for the space and ensure that we get it (and just that space, not any of the children).
        List<SuggestInputElement.SuggestionElement> suggestions =
            pagePicker.sendKeys(pageName).waitForSuggestions().getSuggestions();
        assertEquals(1, suggestions.size());
        assertEquals(pageName, suggestions.get(0).getLabel());
        // Just to be sure that searching for the children also works, search and select the first child.
        pagePicker.clear().sendKeys(childName).waitForSuggestions()
            .selectByVisibleText("Child page 0");
    }
}

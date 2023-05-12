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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;

/**
 * Functional tests for the Page Picker.
 *
 * @version $Id$
 */
@UITest
class PagePickerIT
{
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
        setup.createPage(reference,
            "{{velocity}}{{html}}#pagePicker({'id': 'pagePickerTest', 'multiple': true}){{/html}}{{/velocity}}",
            pageName);

        SuggestInputElement pagePicker =
            new SuggestInputElement(setup.getDriver().findElementWithoutWaiting(By.id("pagePickerTest")));

        // Make sure the picker is ready. TODO: remove once XWIKI-19056 is closed.
        pagePicker.click().waitForSuggestions();

        pagePicker.sendKeys(pageName.substring(0, 3)).waitForSuggestions().selectByVisibleText(pageName);
        pagePicker.clearSelectedSuggestions().sendKeys(pageName.substring(0, 3)).waitForSuggestions()
            .selectByVisibleText(pageName);
    }
}

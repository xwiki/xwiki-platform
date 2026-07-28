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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.xclass.test.po.ClassSheetPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage.goToEditor;

/**
 * Special class editor tests that address only the Number class field type.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@UITest(properties = {
    // Exclude the AppWithinMinutes.ClassEditSheet and AppWithinMinutes.DynamicMessageTool from the PR checker since
    // they use the groovy macro which requires PR rights.
    // TODO: Should be removed once XWIKI-20529 is closed.
    // Exclude AppWithinMinutes.LiveTableEditSheet because it calls com.xpn.xwiki.api.Document.saveWithProgrammingRights
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:AppWithinMinutes\\.(ClassEditSheet|DynamicMessageTool|LiveTableEditSheet)"
})
class NumberClassFieldIT
{
    private static final String FIELD_NAME = "number1";

    private static final String INVALID_FORMAT_MESSAGE = "is not a valid number of type \"long\"";

    @BeforeEach
    void setUp(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();
        setup.deleteSpace(testReference.getLastSpaceReference());
    }

    /**
     * The Number field is rendered as an HTML5 number input, so the browser marks non-numeric input as invalid
     * (browsers don't all sanitize the typed text itself, e.g. Firefox keeps "aaa" visible, but they do flag it).
     */
    @Test
    @Order(1)
    void browserRejectsInvalidInput(TestUtils setup, TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference.getLastSpaceReference());
        editor.addField("Number");
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();
        EntryEditPage entryEditPage = new EntryEditPage();

        entryEditPage.setValue(FIELD_NAME, "aaa");

        boolean isValid = (Boolean) setup.getDriver()
            .executeScript("return document.querySelector('input[id$=\"_0_" + FIELD_NAME + "\"]').checkValidity();");
        assertFalse(isValid);
    }

    /**
     * A value like "99999999999999999999" (overflows a long) is a valid HTML5 number but not a valid long, so it
     * still reaches the server. When editing an existing entry, the failure must show a
     * translated message rather than a generic error.
     */
    @Test
    @Order(2)
    void saveAndContinueShowsFriendlyError(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference.getLastSpaceReference());
        editor.addField("Number");
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();
        EntryEditPage entryEditPage = new EntryEditPage();
        entryEditPage.setValue(FIELD_NAME, "42");
        entryEditPage.clickSaveAndContinue();

        entryEditPage.setValue(FIELD_NAME, "99999999999999999999");
        entryEditPage.clickSaveAndContinue(false);

        entryEditPage.waitForNotificationErrorMessage(INVALID_FORMAT_MESSAGE);
    }
}

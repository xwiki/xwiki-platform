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
 * @since 18.8.0RC1
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
     * Checks the successive validation layers of a Number field of the default {@code long} type, on a single
     * fixture. Non-numeric input is rejected by the browser, which renders the field as an HTML5 number input and
     * either refuses to insert the value or marks the field invalid. A decimal value like "3.5" is a valid HTML5
     * number but mismatches the {@code step="1"} constraint, so the browser must mark the field invalid; both
     * signs are checked since the field has no {@code min} attribute, so the step check is evaluated relative to a
     * base of 0. A value like "99999999999999999999" is a valid HTML5 number that overflows the {@code long} type.
     * The browser can't be asked to catch it, since no {@code min}/{@code max} can express the {@code long} range
     * without disabling the step check, so it reaches the server and must be reported with a friendly message
     * rather than a generic error.
     */
    @Test
    void numberFieldValidation(TestReference testReference)
    {
        EntryEditPage entryEditPage = addNumberFieldAndGoToEntry(testReference);

        entryEditPage.setValue(FIELD_NAME, "aaa");

        assertFalse("aaa".equals(entryEditPage.getValue(FIELD_NAME)) && entryEditPage.isFieldValid(FIELD_NAME),
            "The non-numeric value was kept and accepted as valid");

        entryEditPage.setValue(FIELD_NAME, "3.5");

        assertFalse(entryEditPage.isFieldValid(FIELD_NAME), "The positive decimal value was accepted as valid");

        entryEditPage.setValue(FIELD_NAME, "-3.5");

        assertFalse(entryEditPage.isFieldValid(FIELD_NAME), "The negative decimal value was accepted as valid");

        // A value inside the long range still saves, now that the range is no longer expressed as min and max.
        entryEditPage.setValue(FIELD_NAME, "42");
        entryEditPage.clickSaveAndContinue();

        entryEditPage.setValue(FIELD_NAME, "99999999999999999999");
        entryEditPage.clickSaveAndContinue(false);

        entryEditPage.waitForNotificationErrorMessage(INVALID_FORMAT_MESSAGE);
    }

    private EntryEditPage addNumberFieldAndGoToEntry(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference.getLastSpaceReference());
        editor.addField("Number");
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();
        return new EntryEditPage();
    }
}

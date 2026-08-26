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

import org.junit.jupiter.api.Test;
import org.xwiki.appwithinminutes.test.po.ApplicationClassEditPage;
import org.xwiki.appwithinminutes.test.po.EntryEditPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
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

    /**
     * The Number field is rendered as an HTML5 number input, so the browser rejects non-numeric input, either by
     * refusing to insert it or by marking the field invalid, before the entry can be saved.
     */
    @Test
    void browserRejectsInvalidInput(TestReference testReference)
    {
        EntryEditPage entryEditPage = addNumberFieldAndGoToEntry(testReference);

        entryEditPage.setValue(FIELD_NAME, "aaa");

        assertFalse("aaa".equals(entryEditPage.getValue(FIELD_NAME)) && entryEditPage.isFieldValid(FIELD_NAME),
            "The invalid value was kept and accepted as valid");
    }

    /**
     * A value like "99999999999999999999" is a valid HTML5 number but overflows the default {@code long} type, so
     * the {@code max} constraint set on the input must reject it before the entry can be saved. A decimal value
     * like "3.5" is also a valid HTML5 number but mismatches the {@code step="1"} constraint of the default
     * {@code long} type, so it must be rejected too, regardless of the sign of the {@code min} attribute.
     */
    @Test
    void browserRejectsOutOfRangeAndDecimalInput(TestReference testReference)
    {
        EntryEditPage entryEditPage = addNumberFieldAndGoToEntry(testReference);

        entryEditPage.setValue(FIELD_NAME, "99999999999999999999");

        assertFalse(entryEditPage.isFieldValid(FIELD_NAME), "The out-of-range value was accepted as valid");

        entryEditPage.setValue(FIELD_NAME, "3.5");

        assertFalse(entryEditPage.isFieldValid(FIELD_NAME), "The decimal value was accepted as valid");
    }

    private EntryEditPage addNumberFieldAndGoToEntry(TestReference testReference)
    {
        ApplicationClassEditPage editor = goToEditor(testReference);
        editor.addField("Number");
        editor.clickSaveAndView();
        new ClassSheetPage().clickTemplateLink().edit();
        return new EntryEditPage();
    }
}

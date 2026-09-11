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
package org.xwiki.livedata.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Tests of the Live Data macro in the in-place WYSIWYG editor.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@UITest(
    extraJARs = {
        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index"
    },
    resolveExtraJARs = true
)
class LiveDataInplaceEditIT
{
    private static final String NAME_COLUMN = "name";

    private static final String NAME_LYNDA = "Lynda";

    private static final String LIVE_DATA_ID = "test";

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    /**
     * A Live Data displayed in the in-place editor must still be displayed after a round trip through the Source mode.
     * The editor re-inserts the content it edits, which used to display the Live Data a second time and replace the
     * table by an empty element.
     */
    @Test
    void liveDataIsStillDisplayedAfterSourceRoundTrip(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        // In-place editing is used only when the default editor is the WYSIWYG one.
        setup.setWikiPreference("editor", "Wysiwyg");

        DocumentReference classReference = new DocumentReference("EntryClass", testReference.getLastSpaceReference());
        DocumentReference entryReference = new DocumentReference("Entry", testReference.getLastSpaceReference());
        setup.rest().delete(testReference);
        setup.rest().delete(classReference);
        setup.rest().delete(entryReference);

        // Define the XClass of the Live Data entries and create the single entry, before the page holding the macro:
        // the Live Data has to be displayable as soon as that page is created, since creating it displays it. Adding
        // a class property has no REST API, so it goes through the browser.
        setup.addClassProperty(classReference, NAME_COLUMN, "String");
        String className = setup.serializeReference(classReference.getLocalDocumentReference());
        setup.rest().addObject(entryReference, className, NAME_COLUMN, NAME_LYNDA);

        // The text around the macro gives the editor a place to put the caret that is not the Live Data itself.
        String content = """
            before

            {{liveData
              id="%s"
              properties="%s"
              source="liveTable"
              sourceParameters="translationPrefix=&className=%s"
            /}}

            after
            """.formatted(LIVE_DATA_ID, NAME_COLUMN, className);
        setup.createPage(testReference, content, "Live Data in the in-place editor");
        assertLiveDataIsDisplayed();

        // Entering the in-place editor moves the displayed content into the editable area.
        InplaceEditablePage editablePage = new InplaceEditablePage().editInplace();
        assertLiveDataIsDisplayed();

        // Switch to Source and back: the editor replaces the content it edits by the freshly rendered one.
        CKEditor editor = new CKEditor("content");
        // Focus the rich text area to get the floating tool bar.
        editor.getRichTextArea().click();
        editor.getToolBar().toggleSourceMode();
        editor.getToolBar().toggleSourceMode();
        assertLiveDataIsDisplayed();

        editablePage.cancel();
        assertLiveDataIsDisplayed();
    }

    /**
     * Asserts that the Live Data of the test page is displayed with its single entry. The page objects are created
     * anew on each call because the editor replaces the Live Data element in the DOM.
     */
    private void assertLiveDataIsDisplayed()
    {
        TableLayoutElement tableLayout = new LiveDataElement(LIVE_DATA_ID).getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
    }
}

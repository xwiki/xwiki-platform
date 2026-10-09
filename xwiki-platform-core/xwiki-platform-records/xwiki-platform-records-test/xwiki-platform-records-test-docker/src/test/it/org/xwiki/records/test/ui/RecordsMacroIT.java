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
package org.xwiki.records.test.ui;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.ui.AbstractCKEditorIT;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.records.test.po.RecordsMacroEditModal;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the Records macro dialog in CKEditor.
 *
 * @version $Id$
 */
@UITest
class RecordsMacroIT extends AbstractCKEditorIT
{
    private static final String LIVE_DATA_ID = "records";

    private static final String STATUS = "status";

    private static final String GENRE = "genre";

    private static final String FILTER_SORT_TAB = "Filter & sort";

    private static final String TITLE_COLUMN = "Title";

    private WYSIWYGEditPage editPage;

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    /**
     * The filters picked in the dialog are saved as one {@code &}-separated value, so that they all apply, and a stored
     * value is shown back as one item per constraint.
     */
    @Test
    @Order(1)
    void filters(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        DocumentReference classReference = new DocumentReference("BookClass", testReference.getLastSpaceReference());
        setup.rest().delete(classReference);
        // Adding a class property has no REST API, so it goes through the browser.
        setup.addClassProperty(classReference, STATUS, "String");
        setup.addClassProperty(classReference, GENRE, "String");
        String className = setup.serializeReference(classReference.getLocalDocumentReference());
        // Only the first entry matches both filters, while each of the others matches one of them.
        createEntry(setup, testReference, className, "Published fiction", "published", "fiction");
        createEntry(setup, testReference, className, "Published essay", "published", "essay");
        createEntry(setup, testReference, className, "Draft fiction", "draft", "fiction");

        edit(setup, testReference, """
            {{records class="%s" id="%s"/}}
            """.formatted(className, LIVE_DATA_ID));
        RecordsMacroEditModal macroEditModal = editMacro();
        SuggestInputElement filtersPicker = macroEditModal.getFiltersPicker();
        filtersPicker.sendKeys("status=published").selectTypedText();
        // The suggestions stay open for the next filter and cover the dialog buttons.
        filtersPicker.sendKeys("genre=fiction").selectTypedText().hideSuggestions();
        assertEquals("status=published&genre=fiction", macroEditModal.getFilters());
        macroEditModal.clickSubmit();
        this.textArea.waitForContentRefresh();
        assertSourceContains("filters=\"status=published&genre=fiction\"");

        this.editPage.clickSaveAndView();
        TableLayoutElement tableLayout = new LiveDataElement(LIVE_DATA_ID).getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(1);
        tableLayout.assertRow(TITLE_COLUMN, "Published fiction");

        // Reopening the dialog shows one item per constraint.
        this.editPage = WYSIWYGEditPage.gotoPage(testReference);
        loadEditor();
        macroEditModal = editMacro();
        assertEquals(List.of("status=published", "genre=fiction"), getItems(macroEditModal.getFiltersPicker()));
        macroEditModal.clickCancel();

        // A field named twice is two constraints on that field, and removing one keeps the other alone.
        setSource("""
            before
            
            {{records class="%s" filters="status=published&status=draft" id="%s"/}}
            
            after"""
            .formatted(className, LIVE_DATA_ID));
        macroEditModal = editMacro();
        filtersPicker = macroEditModal.getFiltersPicker();
        assertEquals(List.of("status=published", "status=draft"), getItems(filtersPicker));
        filtersPicker.getSelectedSuggestions().get(1).delete();
        filtersPicker.hideSuggestions();
        assertEquals("status=published", macroEditModal.getFilters());
        macroEditModal.clickSubmit();
        this.textArea.waitForContentRefresh();
        assertSourceContains("filters=\"status=published\"");
    }

    private void createEntry(TestUtils setup, TestReference testReference, String className, String title,
        String status, String genre) throws Exception
    {
        DocumentReference entryReference = new DocumentReference(title, testReference.getLastSpaceReference());
        setup.rest().delete(entryReference);
        setup.createPage(entryReference, "", title);
        setup.addObject(entryReference, className, STATUS, status, GENRE, genre);
    }

    /**
     * Opens the test page in the WYSIWYG editor with the given macro call between two paragraphs, which give the editor
     * somewhere to put the caret that is not the macro itself.
     */
    private void edit(TestUtils setup, TestReference testReference, String macroCall)
    {
        setup.deletePage(testReference, true);
        this.editPage = WYSIWYGEditPage.gotoPage(testReference);
        loadEditor();
        setSource("before\n\n%s\n\nafter".formatted(macroCall));
    }

    private void loadEditor()
    {
        this.editor = new CKEditor("content").waitToLoad();
        this.textArea = this.editor.getRichTextArea();
    }

    /**
     * Selects the macro, which is the only one, opens its dialog and shows the filters.
     */
    private RecordsMacroEditModal editMacro()
    {
        // The caret goes to the paragraph after the macro, from where moving left selects the macro.
        this.textArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.END));
        this.textArea.sendKeys(Keys.HOME, Keys.LEFT);
        this.textArea.waitUntilWidgetSelected();
        this.textArea.sendKeys(Keys.ENTER);
        return new RecordsMacroEditModal().waitUntilReady().openTab(FILTER_SORT_TAB);
    }

    private List<String> getItems(SuggestInputElement picker)
    {
        return picker.getSelectedSuggestions().stream().map(SuggestInputElement.SuggestionElement::getValue).toList();
    }
}

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

import java.util.Collections;
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
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.records.test.po.RecordsMacroEditModal;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    private static final String FILTER_SORT_TAB = "filterSort";

    private static final String EDITING_TAB = "editing";

    private static final String COLUMNS_TAB = "columns";

    private static final String TITLE_STRATEGY = "Use user-submitted title as page name";

    private static final String TEMPLATE_PROVIDER_STRATEGY =
        "Use template provider naming logic (creation restrictions override the new entries location)";

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
            {{records class="%s" id="%s"/}}""".formatted(className, LIVE_DATA_ID));
        RecordsMacroEditModal macroEditModal = editMacro();
        SuggestInputElement filtersPicker = macroEditModal.getFiltersPicker();
        filtersPicker.sendKeys("status=published").selectTypedText();
        // The suggestions stay open for the next filter and cover the dialog buttons.
        filtersPicker.sendKeys("genre=fiction").selectTypedText().hideSuggestions();
        assertEquals("status=published&genre=fiction", macroEditModal.getFilters());
        macroEditModal.clickSubmit();
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
        assertSourceContains("filters=\"status=published\"");
    }

    /**
     * Entries can only be created in edit mode, so the creation strategy can only be picked while the edit mode is
     * allowed. A strategy that is no longer installed turns creation off with a warning, and is kept when the dialog
     * opens. A strategy needs the columns it names the new pages after. The entries created from the table go to the
     * location picked in the dialog.
     */
    @Test
    @Order(2)
    void createEntries(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        DocumentReference classReference = new DocumentReference("TaskClass", testReference.getLastSpaceReference());
        setup.rest().delete(classReference);
        setup.addClassProperty(classReference, STATUS, "String");
        String className = setup.serializeReference(classReference.getLocalDocumentReference());
        SpaceReference location = new SpaceReference("Tasks", testReference.getLastSpaceReference());
        setup.createPage(new DocumentReference("WebHome", location), "", "Tasks");

        // A strategy that is not installed turns creation off, and the readers are told why. This table is on a page of
        // its own because edit() deletes the test page with its children, the location among them, when it exists.
        DocumentReference unknownStrategyPage = new DocumentReference("UnknownStrategy",
            testReference.getLastSpaceReference());
        ViewPage viewPage = setup.createPage(unknownStrategyPage, """
            {{records class="%s" id="%s" properties="%s" allowEditMode="true" creationStrategy="missing"/}}"""
            .formatted(className, LIVE_DATA_ID, STATUS));
        assertThat(viewPage.getContent(), containsString(
            "Entries cannot be created from this table because the creation strategy missing is not installed."));

        edit(setup, testReference, """
            {{records class="%s" id="%s" properties="%s" creationStrategy="missing"/}}"""
            .formatted(className, LIVE_DATA_ID, STATUS));
        RecordsMacroEditModal macroEditModal = editMacro().openTab(EDITING_TAB);
        // The saved strategy is disabled while the edit mode is off, and given back once it is allowed.
        assertEquals("missing", macroEditModal.getCreationStrategy());
        assertFalse(macroEditModal.isCreationStrategyEnabled());
        macroEditModal.setAllowEditMode(true);
        assertTrue(macroEditModal.isCreationStrategyEnabled());
        assertEquals("missing", macroEditModal.getCreationStrategy());
        assertFalse(macroEditModal.isCreationStrategyAvailable(TITLE_STRATEGY));
        assertFalse(macroEditModal.isCreationStrategyAvailable(TEMPLATE_PROVIDER_STRATEGY));
        // Each strategy needing the title has its own line in the warning.
        String missingTitle = "\"%s\" cannot be used because the table does not display Page Title.";
        assertThat(macroEditModal.getCreationStrategyWarning(), containsString(missingTitle.formatted(TITLE_STRATEGY)));
        assertThat(macroEditModal.getCreationStrategyWarning(),
            containsString(missingTitle.formatted(TEMPLATE_PROVIDER_STRATEGY)));
        macroEditModal.openTab(COLUMNS_TAB).getColumnsPicker().sendKeys("Page Title").waitForSuggestions()
            .selectByValue("doc.title").hideSuggestions();
        macroEditModal.openTab(EDITING_TAB);
        assertTrue(macroEditModal.isCreationStrategyAvailable(TITLE_STRATEGY));
        assertEquals("", macroEditModal.getCreationStrategyWarning());
        macroEditModal.setCreationStrategy(TITLE_STRATEGY);
        macroEditModal.setNewEntriesLocation(location);
        String savedLocation = macroEditModal.getNewEntriesLocation();
        macroEditModal.clickSubmit();
        assertSourceContains("creationStrategy=\"title\"");
        assertSourceContains("allowEditMode=\"true\"");
        assertSourceContains("properties=\"status,doc.title\"");
        assertSourceContains("newEntriesLocation=\"%s\"".formatted(savedLocation));

        // The picked location is shown again when the dialog is reopened.
        macroEditModal = editMacro().openTab(EDITING_TAB);
        assertEquals(savedLocation, macroEditModal.getNewEntriesLocation());
        macroEditModal.clickCancel();

        this.editPage.clickSaveAndView();
        LiveDataElement liveData = new LiveDataElement(LIVE_DATA_ID);
        TableLayoutElement tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(0);
        liveData.toggleEditMode();
        tableLayout.startNewEntry();
        tableLayout.createNewEntryFromDocumentProperty(TITLE_COLUMN, "First task");
        assertEquals(1, tableLayout.getTotalEntries());
        assertTrue(setup.rest().exists(new DocumentReference("First task", location)));
    }

    /**
     * The entries created from the table start from the template of the template provider named after the object
     * type, unless another choice is picked in the dialog.
     */
    @Test
    @Order(3)
    void createEntriesFromATemplate(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        SpaceReference space = testReference.getLastSpaceReference();
        DocumentReference classReference = new DocumentReference("NoteClass", space);
        DocumentReference templateReference = new DocumentReference("NoteTemplate", space);
        DocumentReference templateProviderReference = new DocumentReference("NoteTemplateProvider", space);
        DocumentReference urgentTemplateReference = new DocumentReference("UrgentNoteTemplate", space);
        DocumentReference urgentTemplateProviderReference = new DocumentReference("UrgentNoteTemplateProvider", space);
        // Removes what a previous run left, since the notes would be listed and the templates get a second object.
        for (String name : List.of("First note", "Second note", "Third note")) {
            setup.rest().delete(new DocumentReference(name, space));
        }
        for (DocumentReference reference : List.of(classReference, templateReference, templateProviderReference,
            urgentTemplateReference, urgentTemplateProviderReference)) {
            setup.rest().delete(reference);
        }
        setup.addClassProperty(classReference, STATUS, "String");
        String className = setup.serializeReference(classReference.getLocalDocumentReference());
        // The live table source leaves this template out of the entries, since it is named after the object type.
        setup.addObject(templateReference, className, STATUS, "todo");
        setup.addObject(templateProviderReference, "XWiki.TemplateProviderClass", "template",
            setup.serializeReference(templateReference.getLocalDocumentReference()));
        // A provider not named after the object type, which only the dropdown can pick, by its name. Its template is
        // listed as an entry, since only the template named after the object type is left out.
        setup.addObject(urgentTemplateReference, className, STATUS, "urgent");
        setup.addObject(urgentTemplateProviderReference, "XWiki.TemplateProviderClass", "name", "Urgent note",
            "template", setup.serializeReference(urgentTemplateReference.getLocalDocumentReference()));
        String macroCall = "{{records class=\"%s\" id=\"%s\" properties=\"doc.title,%s\" allowEditMode=\"true\" "
            + "creationStrategy=\"title\"/}}";

        // The paragraphs around the macro give the editor somewhere to put the caret, as edit() does.
        setup.createPage(testReference,
            "before\n\n%s\n\nafter".formatted(macroCall.formatted(className, LIVE_DATA_ID, STATUS)), "");
        LiveDataElement liveData = new LiveDataElement(LIVE_DATA_ID);
        TableLayoutElement tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(1);
        liveData.toggleEditMode();
        tableLayout.startNewEntry();
        tableLayout.createNewEntryFromDocumentProperty(TITLE_COLUMN, "First note");
        assertThat(tableLayout.getColumnValues(STATUS), hasItem("todo"));

        // The page is not recreated, since deleting it would also delete the object type and the template.
        this.editPage = WYSIWYGEditPage.gotoPage(testReference);
        loadEditor();
        editMacro().openTab(EDITING_TAB).setTemplateProvider("No template").clickSubmit();
        assertSourceContains("templateProvider=\"XWiki.NoTemplateProvider\"");
        this.editPage.clickSaveAndView();
        liveData = new LiveDataElement(LIVE_DATA_ID);
        tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(2);
        liveData.toggleEditMode();
        tableLayout.startNewEntry();
        tableLayout.createNewEntryFromDocumentProperty(TITLE_COLUMN, "Second note");
        assertEquals(3, tableLayout.getTotalEntries());
        // The column also lists the cell of the filter row, so the entries are told apart by their status.
        assertEquals(1, Collections.frequency(tableLayout.getColumnValues(STATUS), "todo"));

        this.editPage = WYSIWYGEditPage.gotoPage(testReference);
        loadEditor();
        editMacro().openTab(EDITING_TAB).setTemplateProvider("Urgent note").clickSubmit();
        assertSourceContains("templateProvider=\"%s\"".formatted(
            setup.serializeReference(urgentTemplateProviderReference.getLocalDocumentReference())));
        this.editPage.clickSaveAndView();
        liveData = new LiveDataElement(LIVE_DATA_ID);
        tableLayout = liveData.getTableLayout();
        tableLayout.waitUntilRowCountEqualsTo(3);
        liveData.toggleEditMode();
        tableLayout.startNewEntry();
        tableLayout.createNewEntryFromDocumentProperty(TITLE_COLUMN, "Third note");
        assertEquals(4, tableLayout.getTotalEntries());
        assertEquals(2, Collections.frequency(tableLayout.getColumnValues(STATUS), "urgent"));
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
        return new RecordsMacroEditModal(this::submitAndWaitForContentRefresh).waitUntilReady()
            .openTab(FILTER_SORT_TAB);
    }

    /**
     * Submits the macro dialog and waits for the editor to render the updated macro.
     */
    private void submitAndWaitForContentRefresh(Runnable submit)
    {
        submit.run();
        this.textArea.waitForContentRefresh();
    }

    private List<String> getItems(SuggestInputElement picker)
    {
        return picker.getSelectedSuggestions().stream().map(SuggestInputElement.SuggestionElement::getValue).toList();
    }
}

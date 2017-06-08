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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import com.thoughtworks.selenium.Wait;

import static org.junit.Assert.*;

/**
 * Integration tests for macro support inside the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class MacroTest extends AbstractWysiwygTestCase
{
    public static final String MENU_MACRO = "Macro";

    public static final String MENU_REFRESH = "Refresh";

    public static final String MENU_COLLAPSE = "Collapse";

    public static final String MENU_COLLAPSE_ALL = "Collapse All";

    public static final String MENU_EXPAND = "Expand";

    public static final String MENU_EXPAND_ALL = "Expand All";

    public static final String MENU_EDIT = "Edit Macro Properties...";

    public static final String MENU_INSERT = "Insert Macro...";

    public static final String MACRO_CATEGORY_SELECTOR = "//select[@title='Select a macro category']";

    public static final String MACRO_LIVE_FILTER_SELECTOR = "//input[@title = 'Type to filter']";

    public static final String MACRO_SELECTOR_LIST = "//div[contains(@class, 'xListBox')]";

    /**
     * Tests that after deleting the last character before a macro the caret remains before the macro and not inside the
     * macro.
     */
    @Test
    public void testDeleteCharacterBeforeMacro()
    {
        switchToSource();
        setSourceText("a{{html}}b{{/html}}");
        switchToWysiwyg();
        typeDelete();
        typeText("x");
        switchToSource();
        assertSourceText("x{{html}}b{{/html}}");
    }

    /**
     * Tests that by holding the Delete key down before a macro the caret doesn't get inside the macro, but, instead,
     * the macro is deleted.
     */
    @Test
    public void testHoldDeleteKeyBeforeMacro()
    {
        switchToSource();
        setSourceText("bc{{html}}def{{/html}}g");
        switchToWysiwyg();
        // Place the caret between "b" and "c".
        moveCaret("document.body.firstChild.firstChild", 1);
        typeDelete(2, true);
        typeText("x");
        switchToSource();
        assertSourceText("bxg");
    }

    /**
     * Tests that after deleting with Backspace a text selection ending before a macro the caret remains before the
     * macro and not inside the macro.
     */
    @Test
    public void testSelectCharacterBeforeMacroAndPressBackspace()
    {
        switchToSource();
        setSourceText("g{{html}}h{{/html}}");
        switchToWysiwyg();
        // Select the character preceding the macro.
        selectNode("document.body.firstChild.firstChild");
        typeBackspace();
        typeText("x");
        switchToSource();
        assertSourceText("x{{html}}h{{/html}}");
    }

    /**
     * Tests that if we select the text before a macro and insert a symbol instead of it then the symbol is inserted
     * before the macro and not inside the macro.
     */
    @Test
    public void testSelectCharacterBeforeMacroAndInsertSymbol()
    {
        switchToSource();
        setSourceText("i{{html}}j{{/html}}");
        switchToWysiwyg();
        // Select the character preceding the macro.
        selectNode("document.body.firstChild.firstChild");
        clickSymbolButton();
        getSelenium().click("//div[@title='copyright sign']");
        typeText("x");
        switchToSource();
        assertSourceText("\u00A9x{{html}}j{{/html}}");
    }

    /**
     * Tests that a macro can be deleted by pressing Delete key when the caret is placed before that macro.
     */
    @Test
    public void testPressDeleteJustBeforeMacro()
    {
        switchToSource();
        setSourceText("j{{html}}k{{/html}}l");
        switchToWysiwyg();
        // Move the caret just before the macro.
        moveCaret("document.body.firstChild.firstChild", 1);
        typeDelete();
        typeText("x");
        switchToSource();
        assertSourceText("jxl");
    }

    /**
     * Tests that after deleting the last character after a macro the caret remains after the macro and not inside the
     * macro.
     */
    @Test
    public void testDeleteCharacterAfterMacro()
    {
        switchToSource();
        setSourceText("a{{html}}b{{/html}}c");
        switchToWysiwyg();
        // Move the caret at the end.
        moveCaret("document.body.firstChild.lastChild", 1);
        typeBackspace();
        typeText("x");
        switchToSource();
        assertSourceText("a{{html}}b{{/html}}x");
    }

    /**
     * Tests that by holding the Backspace key down after a macro the caret doesn't get inside the macro, but, instead,
     * the macro is deleted.
     */
    @Test
    public void testHoldBackspaceKeyAfterMacro()
    {
        switchToSource();
        setSourceText("c{{html}}def{{/html}}g");
        switchToWysiwyg();
        // Move the caret at the end.
        moveCaret("document.body.firstChild.lastChild", 1);
        typeBackspace(2, true);
        typeText("x");
        switchToSource();
        assertSourceText("cx");
    }

    /**
     * Tests that after deleting with Delete key a text selection starting after a macro the caret remains after the
     * macro and not inside the macro.
     */
    @Test
    public void testSelectCharacterAfterMacroAndPressDelete()
    {
        switchToSource();
        setSourceText("g{{html}}h{{/html}}i");
        switchToWysiwyg();
        // Select the character following the macro.
        selectNode("document.body.firstChild.lastChild");
        typeDelete();
        typeText("x");
        switchToSource();
        assertSourceText("g{{html}}h{{/html}}x");
    }

    /**
     * Tests that if we select the text after a macro and insert a symbol instead of it then the symbol is inserted
     * after the macro and not inside the macro.
     */
    @Test
    public void testSelectCharacterAfterMacroAndInsertSymbol()
    {
        switchToSource();
        setSourceText("i{{html}}j{{/html}}k");
        switchToWysiwyg();
        // Select the character following the macro.
        selectNode("document.body.firstChild.lastChild");
        clickSymbolButton();
        getSelenium().click("//div[@title='copyright sign']");
        typeText("x");
        switchToSource();
        assertSourceText("i{{html}}j{{/html}}\u00A9x");
    }

    /**
     * Tests that a macro can be deleted by pressing Backspace key when the caret is placed after that macro.
     */
    @Test
    public void testPressBackspaceJustAfterMacro()
    {
        switchToSource();
        setSourceText("k{{html}}l{{/html}}m");
        switchToWysiwyg();
        // Move the caret at the end.
        moveCaret("document.body.firstChild.lastChild", 0);
        typeBackspace();
        typeText("x");
        switchToSource();
        assertSourceText("kxm");
    }

    /**
     * Tests that Undo/Redo operations don't affect the macros present in the edited document.
     */
    @Test
    public void testUndoRedoWhenMacrosArePresent()
    {
        switchToSource();
        setSourceText("{{html}}pq{{/html}}");
        switchToWysiwyg();
        // We have to manually place the caret to be sure it is before the macro. The caret is before the macro when the
        // browser window is focused but inside the macro when the tests run in background.
        moveCaret("document.body", 0);
        typeText("uv");
        clickUndoButton();
        clickRedoButton();
        switchToSource();
        assertSourceText("uv{{html}}pq{{/html}}");
    }

    /**
     * Clicks on a macro and deletes it.
     */
    @Test
    public void testSelectAndDeleteMacro()
    {
        switchToSource();
        setSourceText("{{html}}<p>foo</p>{{/html}}\n\nbar");
        switchToWysiwyg();
        selectRichTextAreaFrame();
        try {
            getSelenium().click(getMacroLocator(0));
        } finally {
            selectTopFrame();
        }
        typeBackspace();
        switchToSource();
        assertSourceText("bar");
    }

    /**
     * @see XWIKI-3221: New lines inside code macro are lost when saving
     */
    @Test
    public void testWhiteSpacesInsideCodeMacroArePreserved()
    {
        String wikiText = "{{code}}\nfunction foo() {\n    alert('bar');\n}\n{{/code}}";
        switchToSource();
        setSourceText(wikiText);
        switchToWysiwyg();
        switchToSource();
        assertSourceText(wikiText);
    }

    /**
     * Test if the user can collapse and expand all the macros using the macro menu and if this menu is synchronized
     * with the current state of the rich text area.
     */
    @Test
    public void testCollapseAndExpandAllMacros()
    {
        setContent("no macro");

        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_REFRESH));
        // If there's no macro present then the Collapse/Expand menu items must be disabled.
        assertFalse(isMenuEnabled(MENU_COLLAPSE_ALL));
        assertFalse(isMenuEnabled(MENU_EXPAND_ALL));
        closeMenuContaining(MENU_REFRESH);

        switchToSource();
        setSourceText("k\n\n{{html}}l{{/html}}\n\nm\n\n{{code}}n{{/code}}\n\no");
        switchToWysiwyg();
        clickMenu(MENU_MACRO);
        // By default all macros are expanded.
        assertTrue(isMenuEnabled(MENU_COLLAPSE_ALL));
        assertFalse(isMenuEnabled(MENU_EXPAND_ALL));

        // Let's collapse all macros.
        clickMenu(MENU_COLLAPSE_ALL);

        clickMenu(MENU_MACRO);
        // Now all macros should be collapsed.
        assertFalse(isMenuEnabled(MENU_COLLAPSE_ALL));
        assertTrue(isMenuEnabled(MENU_EXPAND_ALL));

        // Let's expand all macros.
        clickMenu(MENU_EXPAND_ALL);

        clickMenu(MENU_MACRO);
        // Now all macros should be expanded.
        assertTrue(isMenuEnabled(MENU_COLLAPSE_ALL));
        assertFalse(isMenuEnabled(MENU_EXPAND_ALL));
        closeMenuContaining(MENU_REFRESH);

        // Let's collapse the first macro by selecting it first and then using the shortcut key.
        selectMacro(0);
        collapseMacrosUsingShortcutKey();
        // Finally unselect the macro.
        clearMacroSelection();

        // Now let's check the menu. Both Collapse All and Expand All menu items should be enabled.
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_COLLAPSE_ALL));
        assertTrue(isMenuEnabled(MENU_EXPAND_ALL));
    }

    /**
     * Test if the user can collapse and expand the selected macros using the macro menu and if this menu is
     * synchronized with the current state of the rich text area.
     */
    @Test
    public void testCollapseAndExpandSelectedMacros()
    {
        switchToSource();
        setSourceText("o\n\n{{html}}n{{/html}}\n\nm\n\n{{code}}l{{/code}}\n\nk");
        switchToWysiwyg();

        // If no macro is selected then Expand and Collapse menu entries shouldn't be present.
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_REFRESH));
        assertFalse(isMenuEnabled(MENU_COLLAPSE));
        assertFalse(isMenuEnabled(MENU_EXPAND));
        closeMenuContaining(MENU_REFRESH);

        // Select the first macro and collapse it (by default macros should be expanded).
        selectMacro(0);
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_COLLAPSE));
        assertFalse(isMenuEnabled(MENU_EXPAND));
        clickMenu(MENU_COLLAPSE);

        // Now expand it back.
        clickMenu(MENU_MACRO);
        assertFalse(isMenuEnabled(MENU_COLLAPSE));
        assertTrue(isMenuEnabled(MENU_EXPAND));
        clickMenu(MENU_EXPAND);

        // Let's select the second macro too.
        getSelenium().controlKeyDown();
        selectMacro(1);
        getSelenium().controlKeyUp();

        // Collapse both selected macros.
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_COLLAPSE));
        assertFalse(isMenuEnabled(MENU_EXPAND));
        clickMenu(MENU_COLLAPSE);

        // Let's check if the menu reports them as collapsed.
        clickMenu(MENU_MACRO);
        assertFalse(isMenuEnabled(MENU_COLLAPSE));
        assertTrue(isMenuEnabled(MENU_EXPAND));

        // Expand both.
        clickMenu(MENU_EXPAND);

        // Let's check if the menu reports them as expanded.
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_COLLAPSE));
        assertFalse(isMenuEnabled(MENU_EXPAND));
        closeMenuContaining(MENU_COLLAPSE);
    }

    /**
     * Test if the user can select a macro by clicking it and then toggle between collapsed and expanded states using
     * the space key.
     */
    @Test
    public void testClickToSelectMacroAndToggleCollapse()
    {
        // Let's use a macro without definition.
        switchToSource();
        setSourceText("{{foo}}bar{{/foo}}");
        switchToWysiwyg();

        // By default macros are expanded. Let's check this.
        // Note: We have to select the rich text area frame because the visibility of an element is evaluated relative
        // to the current window.
        selectRichTextAreaFrame();
        try {
            assertFalse(getSelenium().isVisible(getMacroPlaceHolderLocator(0)));
            assertTrue(getSelenium().isVisible(getMacroOutputLocator(0)));
        } finally {
            selectTopFrame();
        }

        // Select the macro.
        selectMacro(0);

        // Let's collapse the selected macro and check its state.
        toggleMacroCollapsedState();
        selectRichTextAreaFrame();
        try {
            assertTrue(getSelenium().isVisible(getMacroPlaceHolderLocator(0)));
            assertFalse(getSelenium().isVisible(getMacroOutputLocator(0)));
        } finally {
            selectTopFrame();
        }

        // Let's expand the selected macro and check its state.
        toggleMacroCollapsedState();
        selectRichTextAreaFrame();
        try {
            assertFalse(getSelenium().isVisible(getMacroPlaceHolderLocator(0)));
            assertTrue(getSelenium().isVisible(getMacroOutputLocator(0)));
        } finally {
            selectTopFrame();
        }
    }

    /**
     * Tests the refresh feature when there's no macro present in the edited document.
     */
    @Test
    public void testRefreshContentWithoutMacros()
    {
        String text = "a b";
        typeText(text);
        assertEquals(text, getRichTextArea().getText());

        // If no macros are present then the refresh shoudn't affect too much the edited content.
        refreshMacros();
        assertEquals(text, getRichTextArea().getText());
    }

    /**
     * Tests that the user can refresh all the macros from the edited document by using the Refresh menu.
     */
    @Test
    public void testRefreshMacros()
    {
        switchToSource();
        setSourceText("{{box}}p{{/box}}\n\n{{code}}q{{/code}}");
        switchToWysiwyg();

        // Collapse the second macro.
        selectRichTextAreaFrame();
        try {
            assertFalse(getSelenium().isVisible(getMacroPlaceHolderLocator(1)));
            assertTrue(getSelenium().isVisible(getMacroOutputLocator(1)));
        } finally {
            selectTopFrame();
        }
        selectMacro(1);
        toggleMacroCollapsedState();
        selectRichTextAreaFrame();
        try {
            assertTrue(getSelenium().isVisible(getMacroPlaceHolderLocator(1)));
            assertFalse(getSelenium().isVisible(getMacroOutputLocator(1)));
        } finally {
            selectTopFrame();
        }

        // Unselect the macro.
        clearMacroSelection();

        // Refresh the content
        refreshMacros();

        // Check if the second macro is expanded.
        selectRichTextAreaFrame();
        try {
            assertFalse(getSelenium().isVisible(getMacroPlaceHolderLocator(1)));
            assertTrue(getSelenium().isVisible(getMacroOutputLocator(1)));
        } finally {
            selectTopFrame();
        }
    }

    /**
     * Tests that the user can refresh the Table Of Contents macro after adding more headers.
     */
    @Test
    public void testRefreshTocMacro()
    {
        switchToSource();
        setSourceText("{{toc start=\"1\"/}}\n\n= Title 1\n\n== Title 2");
        switchToWysiwyg();

        // We should have two list items in the edited document.
        String listItemCountExpression = "return document.getElementsByTagName('li').length";
        assertEquals(2L, getRichTextArea().executeScript(listItemCountExpression));

        // Place the caret after the second heading and insert a new one.
        moveCaret("document.getElementsByTagName('h2')[0].firstChild.firstChild", 7);
        // Wait for the macro to be unselected.
        waitForSelectedMacroCount(0);
        typeEnter();
        typeText("Title 3");
        applyStyleTitle3();

        // Refresh the content and the TOC macro.
        refreshMacros();

        // We should have three list items in the edited document now.
        assertEquals(3L, getRichTextArea().executeScript(listItemCountExpression));
    }

    /**
     * Tests the edit macro feature by editing a HTML macro instance and changing its content and a parameter.
     */
    @Test
    public void testEditHTMLMacro()
    {
        switchToSource();
        setSourceText("{{html}}white{{/html}}");
        switchToWysiwyg();
        editMacro(0);

        // Change the content of the HTML macro.
        setFieldValue("pd-content-input", "black");

        // Set the Wiki parameter to true.
        getSelenium().select("pd-wiki-input", "yes");

        // Apply changes.
        applyMacroChanges();

        // Test if our changes have been applied.
        switchToSource();
        assertSourceText("{{html wiki=\"true\"}}\nblack\n{{/html}}");
        switchToWysiwyg();

        // Edit again, this time using the default value for the Wiki parameter.
        editMacro(0);

        // Set the Wiki parameter to its default value, false.
        assertEquals("true", getSelenium().getValue("pd-wiki-input"));
        getSelenium().select("pd-wiki-input", "no");

        // Apply changes.
        applyMacroChanges();

        // Test if our changes have been applied. This time the Wiki parameter is missing from the output because it has
        // the default value.
        switchToSource();
        assertSourceText("{{html}}\nblack\n{{/html}}");
    }

    /**
     * Tests if the edit macro feature doesn't fail when the user inputs special characters like {@code "} (used for
     * wrapping parameter values) or {@code |-|} (used to separate the macro name, parameter list and macro content in
     * macro serialization).
     * 
     * @see XWIKI-3270: Quotes inside macro parameter values need to be escaped
     */
    @Test
    public void testEditMacroWithSpecialCharactersInParameterValues()
    {
        switchToSource();
        setSourceText("{{box title =  \"1~\"2|-|3=~~~\"4~~\" }}=~\"|-|~~{{/box}}");
        switchToWysiwyg();
        editMacro(0);

        // Check if the content of the macro has the right value.
        assertEquals("=~\"|-|~~", getSelenium().getValue("pd-content-input"));

        // Check if the title parameter has the right value (it should be the first text input).
        assertEquals("1\"2|-|3=~\"4~", getSelenium().getValue("pd-title-input"));

        // Change the title parameter.
        setFieldValue("pd-title-input", "a\"b|-|c=~\"d~");
        applyMacroChanges();

        switchToSource();
        assertSourceText("{{box title=\"a~\"b|-|c=~~~\"d~~\"}}\n=~\"|-|~~\n{{/box}}");
    }

    /**
     * Tests if the edit macro feature doesn't fail when the user tries to edit an unregistered macro (a macro who's
     * descriptor can't be found).
     */
    @Test
    public void testEditUnregisteredMacro()
    {
        switchToSource();
        setSourceText("{{foo}}bar{{/foo}}");
        switchToWysiwyg();
        editMacro(0);

        // Check if the dialog shows the error message
        assertTrue(getSelenium().isVisible("//div[@class = 'xDialogBody']/div[contains(@class, 'errormessage')]"));
    }

    /**
     * Tests that macro edits can be undone.
     */
    @Test
    public void testUndoMacroEdit()
    {
        switchToSource();
        setSourceText("{{velocity}}$xcontext.user{{/velocity}}");
        switchToWysiwyg();

        // First edit.
        editMacro(0);
        setFieldValue("pd-content-input", "$datetool.date");
        applyMacroChanges();

        // Second edit.
        editMacro(0);
        setFieldValue("pd-content-input", "$xwiki.version");
        applyMacroChanges();

        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        clickUndoButton(2);
        waitForPushButton(TOOLBAR_BUTTON_REDO_TITLE, true);
        clickRedoButton();
        switchToSource();
        assertSourceText("{{velocity}}\n$datetool.date\n{{/velocity}}");
    }

    /**
     * Tests the basic insert macro scenario, using the code macro.
     */
    @Test
    public void testInsertCodeMacro()
    {
        insertMacro("Code");

        setFieldValue("pd-content-input", "function f(x) {\n  return x;\n}");
        applyMacroChanges();

        editMacro(0);
        setFieldValue("pd-title-input", "Identity function");
        applyMacroChanges();

        switchToSource();
        assertSourceText("{{code title=\"Identity function\"}}\nfunction f(x) {\n  return x;\n}\n{{/code}}");
    }

    /**
     * Tests if the ToC macro can be inserted in an empty paragraph without receiving the "Not an inline macro" error
     * message.
     * 
     * @see XWIKI-3551: Cannot insert standalone macros
     */
    @Test
    public void testInsertTOCMacro()
    {
        // Create two headings to be able to detect if the ToC macro has the right output.
        typeText("Title 1");
        applyStyleTitle1();

        typeEnter();
        typeText("Title 2");
        applyStyleTitle2();

        // Let's insert a ToC macro between the two headings.
        // First, place the caret at the end of first heading.
        moveCaret("document.body.getElementsByTagName('h1')[0].firstChild", 7);
        // Get out of the heading.
        typeEnter();
        // Insert the ToC macro
        insertMacro("Table Of Contents");
        // Make sure the ToC starts with level 2 headings.
        setFieldValue("pd-start-input", "2");
        applyMacroChanges();

        // Check the output of the ToC macro.
        assertEquals(1L, getRichTextArea().executeScript("return document.getElementsByTagName('li').length"));

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("= Title 1 =\n\n\n{{toc start=\"2\"/}}\n\n\n== Title 2 ==");
    }

    /**
     * @see XWIKI-4048: Automatically add empty new line before/after macros when inserting standalone macros
     */
    @Test
    public void testInsertStandAloneMacroInline()
    {
        switchToSource();
        setSourceText("= Heading =\n\nparagraph");
        switchToWysiwyg();

        // Place the caret inside the paragraph.
        moveCaret("document.body.lastChild.firstChild", 4);

        // Insert the ToC macro
        insertMacro("Table Of Contents");
        applyMacroChanges();

        // Check the output of the ToC macro.
        assertEquals(1L, getRichTextArea().executeScript("return document.getElementsByTagName('li').length"));

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("= Heading =\n\npara\n\n{{toc/}}\n\ngraph");
    }

    /**
     * Inserts a HTML macro, whose output contains block-level elements, in the middle of a paragraph's text and tests
     * if the macro can be fixed by separating it in an empty paragraph.
     * 
     * @see XWIKI-3551: Cannot insert standalone macros
     */
    @Test
    public void testInsertHTMLMacroWithBlockContentInANotEmptyParagraph()
    {
        // Create a paragraph with some text inside.
        typeText("beforeafter");
        applyStyleTitle1();
        applyStylePlainText();

        // Place the caret in the middle of the paragraph.
        moveCaret("document.body.firstChild.firstChild", 6);

        // Insert the HTML macro.
        insertMacro("HTML");
        // Make the macro render a list, which is forbidden inside a paragraph.
        setFieldValue("pd-content-input", "<ul><li>xwiki</li></ul>");
        applyMacroChanges();

        // At this point the macro should render an error message instead of the list.
        String listItemCountExpression = "return document.getElementsByTagName('li').length";
        assertEquals(0L, getRichTextArea().executeScript(listItemCountExpression));

        // Let's fix the macro by separating it in an empty paragraph.
        // Move the caret before the macro and press Enter to move it into a new paragraph.
        moveCaret("document.body.firstChild.firstChild", 6);
        typeEnter();
        // Move the caret after the macro and press Enter to move the following text in a new paragraph.
        moveCaret("document.body.lastChild.lastChild", 0);
        typeEnter();

        // Now the macro should be in an empty paragraph.
        // Let's refresh the content to see if the macro was fixed.
        refreshMacros();
        // Check the output of the HTML macro.
        assertEquals(1L, getRichTextArea().executeScript(listItemCountExpression));

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("before\n\n{{html}}\n<ul><li>xwiki</li></ul>\n{{/html}}\n\nafter");
    }

    /**
     * @see XWIKI-3570: Code macro fails to escape properly in GWT editor
     */
    @Test
    public void testInsertCodeMacroWithXMLComments()
    {
        // Insert the Code macro.
        insertMacro("Code");
        // Set the language parameter to XML.
        setFieldValue("pd-language-input", "xml");
        // Set the content. Include XML comments in the content.
        setFieldValue("pd-content-input",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- this is a test -->\n<test>123</test>");
        applyMacroChanges();

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("{{code language=\"xml\"}}\n<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!-- this is a test -->\n<test>123</test>\n{{/code}}");
        switchToWysiwyg();

        // Edit the inserted macro.
        editMacro(0);
        assertEquals("xml", getSelenium().getValue("pd-language-input"));
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!-- this is a test -->\n<test>123</test>",
            getSelenium().getValue("pd-content-input"));
        closeDialog();
    }

    /**
     * @see XWIKI-3581: WYSIWYG editor treats macro parameter names as case sensitive
     */
    @Test
    public void testDetectMacroParameterNamesIgnoringCase()
    {
        switchToSource();
        setSourceText("{{box CSSClaSS=\"foo\"}}bar{{/box}}");
        switchToWysiwyg();
        // Edit the Box macro.
        editMacro(0);
        // See if the CSSClaSS parameter was correctly detected.
        assertEquals("foo", getSelenium().getValue("pd-cssClass-input"));
        // Change the value of the CSSClaSS parameter.
        setFieldValue("pd-cssClass-input", "xyz");
        applyMacroChanges();
        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("{{box CSSClaSS=\"xyz\"}}\nbar\n{{/box}}");
    }

    /**
     * @see XWIKI-3735: Differentiate macros with empty content from macros without content.
     */
    @Test
    public void testDifferentiateMacrosWithEmptyContentFromMacrosWithoutContent()
    {
        StringBuffer macros = new StringBuffer();
        macros.append("{{code/}}");
        macros.append("{{code}}{{/code}}");
        macros.append("{{code title=\"1|-|2\"/}}");
        macros.append("{{code title=\"1|-|2\"}}{{/code}}");

        // Insert the macros.
        switchToSource();
        setSourceText(macros.toString());
        switchToWysiwyg();
        // See if the macro syntax is left unchanged when the macros are not edited.
        switchToSource();
        assertSourceText(macros.toString());
        switchToWysiwyg();

        // Edit the first macro (the one without content and without arguments).
        editMacro(0);
        setFieldValue("pd-content-input", "|-|");
        applyMacroChanges();
        switchToSource();
        assertSourceText("{{code}}|-|{{/code}}{{code}}{{/code}}{{code title=\"1|-|2\"/}}{{code title=\"1|-|2\"}}{{/code}}");
        switchToWysiwyg();

        // Edit the second macro (the one with empty content but without arguments).
        editMacro(1);
        setFieldValue("pd-title-input", "|-|");
        setFieldValue("pd-content-input", "|-|");
        applyMacroChanges();

        // Edit the third macro (the one without content but with arguments).
        editMacro(2);
        setFieldValue("pd-title-input", "");
        setFieldValue("pd-content-input", "|-|");
        applyMacroChanges();

        // Edit the forth macro (the one with empty content and with arguments).
        editMacro(3);
        setFieldValue("pd-content-input", "|-|");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{code}}|-|{{/code}}{{code title=\"|-|\"}}|-|{{/code}}"
            + "{{code}}|-|{{/code}}{{code title=\"1|-|2\"}}|-|{{/code}}");
    }

    /**
     * @see XWIKI-4085: Content duplicated if i have a macro (toc, id..) in an html macro.
     */
    @Test
    public void testNestedMacrosAreNotDuplicated()
    {
        StringBuilder content = new StringBuilder();
        content.append("{{html wiki=\"true\"}}\n\n");
        content.append("= Hello title 1 =\n\n");
        content.append("{{toc start=\"2\"/}}\n\n");
        content.append("= Hello title 2 =\n\n");
        content.append("{{/html}}");
        switchToSource();
        setSourceText(content.toString());
        switchToWysiwyg();

        // Check if only one macro was detected (which should be the top level macro).
        assertEquals(1, getMacroCount());

        // Check if the top level macro was correctly detected.
        deleteMacro(0);
        switchToSource();
        assertSourceText("");

        // Reset the initial content.
        setSourceText(content.toString());
        switchToWysiwyg();

        // Check if the nested macro is duplicated.
        switchToSource();
        assertSourceText(content.toString());
    }

    /**
     * @see XWIKI-4155: Use double click or Enter to select the macro to insert.
     */
    @Test
    public void testDoubleClickToSelectMacroToInsert()
    {
        openSelectMacroDialog();

        // We have to wait for the specified macro to be displayed on the dialog because the loading indicator is
        // removed just before the list of macros is displayed and the Selenium click command can interfere.
        waitForMacroListItem("Info Message", true);
        // Each double click event should be preceded by a click event.
        getSelenium().click(getMacroListItemLocator("Info Message"));
        // Fire the double click event.
        getSelenium().doubleClick(getMacroListItemLocator("Info Message"));
        waitForDialogToLoad();

        // Fill the macro content.
        setFieldValue("pd-content-input", "x");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{info}}\nx\n{{/info}}");
    }

    /**
     * @see XWIKI-4155: Use double click or Enter to select the macro to insert.
     */
    @Test
    public void testPressEnterToSelectMacroToInsert()
    {
        openSelectMacroDialog();

        // We have to wait for the specified macro to be displayed on the dialog because the loading indicator is
        // removed just before the list of macros is displayed and the Selenium click command can interfere.
        waitForMacroListItem("HTML", true);
        // Select a macro.
        getSelenium().click(getMacroListItemLocator("HTML"));
        // Press Enter to choose the selected macro.
        getSelenium().typeKeys("//div[@class = 'xListBox']", "\\13");
        waitForDialogToLoad();

        // Fill the macro content.
        setFieldValue("pd-content-input", "a");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{html}}\na\n{{/html}}");
    }

    /**
     * @see XWIKI-4137: Pop up the "Edit macro properties" dialog when double-clicking on a macro block.
     */
    @Test
    public void testDoubleClickToEditMacro()
    {
        // Insert two macros.
        switchToSource();
        setSourceText("{{error}}x{{/error}}{{info}}y{{/info}}");
        switchToWysiwyg();

        // Double click to edit the second macro.
        selectRichTextAreaFrame();
        try {
            String infoMacroLocator = getMacroLocator(1);
            waitForElement(infoMacroLocator);
            getSelenium().doubleClick(infoMacroLocator);
        } finally {
            selectTopFrame();
        }
        waitForDialogToLoad();

        // Fill the macro content.
        setFieldValue("pd-content-input", "z");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{error}}x{{/error}}{{info}}z{{/info}}");
    }

    /**
     * Sometimes when you double click a macro the browser selects the macro container (i.e. the DOM selection wraps the
     * element that contains the macro output). Test if the macro edit box is still triggered.
     */
    @Test
    public void testDoubleClickToEditMacroWhenDOMSelectionWrapsTheMacroContainer()
    {
        // Insert a macro.
        switchToSource();
        setSourceText("before {{error}}currently{{/error}} after");
        switchToWysiwyg();

        // Double click to edit the macro.
        selectRichTextAreaFrame();
        try {
            // Fire the double click event on the macro.
            getSelenium().doubleClick(getMacroLocator(0));
        } finally {
            selectTopFrame();
        }
        waitForDialogToLoad();

        // Fill the macro content.
        setFieldValue("pd-content-input", "now");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("before {{error}}now{{/error}} after");
    }

    /**
     * @see XWIKI-6121: Double clicking on text makes macro edit box appear
     */
    @Test
    public void testDoubleClickOutsideSelectedMacro()
    {
        // Insert a macro.
        switchToSource();
        // Put the outside text before the macro to overcome an issue in Selenium's MouseMoveAction.
        // See http://code.google.com/p/selenium/issues/detail?id=4863 (Move mouse action fails inside an iframe if the
        // x/y coordinates of the target element on the screen are greater than the width/height of the iframe)
        setSourceText("(% id=\"outside\" %)after\n\n{{info}}before{{/info}}");
        switchToWysiwyg();

        // Select the macro.
        selectMacro(0);
        waitForSelectedMacroCount(1);

        // Double click on the paragraph outside of the macro output.
        selectRichTextAreaFrame();
        try {
            getSelenium().doubleClick("document.getElementById('outside')");
        } finally {
            selectTopFrame();
        }

        try {
            waitForDialogToLoad();
            fail("The macro edit box shouldn't be triggered by double clicking outside of the macro output.");
        } catch (TimeoutException e) {
            // Expected.
        }
    }

    /**
     * @see XWIKI-3437: List macros by category/library
     */
    @Test
    public void testSelectMacroFromCategory()
    {
        openSelectMacroDialog();

        // "All Macros" category should be selected by default.
        assertEquals("All Macros", getSelectedMacroCategory());

        // Make sure the "Code" and "Velocity" macros are present in "All Macros" category.
        waitForMacroListItem("Code", true);
        waitForMacroListItem("Velocity", true);

        // "Velocity" shouldn't be in the "Formatting" category.
        selectMacroCategory("Formatting");
        waitForMacroListItem("Code", true);
        waitForMacroListItem("Velocity", false);

        // "Code" shouldn't be in the "Development" category.
        selectMacroCategory("Development");
        waitForMacroListItem("Velocity", true);
        waitForMacroListItem("Code", false);

        // Select the "Velocity" macro.
        getSelenium().click(getMacroListItemLocator("Velocity"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();

        // Set the content field.
        setFieldValue("pd-content-input", "$xwiki.version");
        applyMacroChanges();

        // Open the "Select Macro" dialog again to see if its state was preserved.
        openSelectMacroDialog();
        assertEquals("Development", getSelectedMacroCategory());
        closeDialog();

        // Check the result.
        switchToSource();
        assertSourceText("{{velocity}}\n$xwiki.version\n{{/velocity}}");
    }

    /**
     * @see XWIKI-4206: Add the ability to search in the list of macros
     */
    @Test
    public void testFilterMacrosFromCategory()
    {
        openSelectMacroDialog();

        // Make sure the current category is "All Macros".
        selectMacroCategory("All Macros");

        // Check if "Velocity", "Footnote" and "Error Message" macros are present.
        waitForMacroListItem("Velocity", true);
        waitForMacroListItem("Footnote", true);
        waitForMacroListItem("Error Message", true);

        // Check if the filter can make a difference.
        int expectedMacroCountAfterFilterAllMacros = getMacroListItemCount("note");
        assertTrue(expectedMacroCountAfterFilterAllMacros < getMacroListItemCount());

        // Filter the macros.
        filterMacrosContaining("note");

        // Check what macros are present.
        waitForMacroListItem("Velocity", false);
        waitForMacroListItem("Error Message", true);

        // Check the number of macros.
        assertEquals(expectedMacroCountAfterFilterAllMacros, getMacroListItemCount());

        // Check if the filter is preserved when switching the category.
        // Select the category of the "Footnote" macro.
        selectMacroCategory("Content");
        waitForMacroListItem("Footnote", true);
        waitForMacroListItem("Error Message", false);
        // Select the category of the "Error Message" macro.
        selectMacroCategory("Formatting");
        waitForMacroListItem("Error Message", true);
        waitForMacroListItem("Footnote", false);

        // Save the current macro list item count to be able to check if the filter state is preserved.
        int previousMacroListItemCount = getMacroListItemCount();

        // Select the "Error Message" macro.
        getSelenium().click(getMacroListItemLocator("Error Message"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();

        // Set the content field.
        setFieldValue("pd-content-input", "test");
        applyMacroChanges();

        // Open the "Select Macro" dialog again to see if the filter was preserved.
        openSelectMacroDialog();
        waitForMacroListItem("Error Message", true);
        assertEquals("note", getMacroFilterValue());
        assertEquals(previousMacroListItemCount, getMacroListItemCount());
        assertEquals(previousMacroListItemCount, getMacroListItemCount("note"));
        closeDialog();

        // Check the result.
        switchToSource();
        assertSourceText("{{error}}\ntest\n{{/error}}");
    }

    /**
     * @see XWIKI-3434: Use the dialog wizard for insert macro UI
     */
    @Test
    public void testReturnToSelectMacroStep()
    {
        openSelectMacroDialog();
        waitForMacroListItem("Velocity", true);

        // Filter the macros.
        int scriptMacroCount = getMacroListItemCount("script");
        selectMacroCategory("Development");
        filterMacrosContaining("script");
        waitForMacroListItemCount(scriptMacroCount);

        // Select the "Groovy" macro.
        getSelenium().click(getMacroListItemLocator("Groovy"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();

        // Return to the "Select Macro" step.
        getSelenium().click("//div[@class = 'xDialogContent']//button[text() = 'Previous']");
        waitForDialogToLoad();

        // Check if the state of the "Select Macro" dialog was preserved.
        assertEquals("Development", getSelectedMacroCategory());
        assertEquals("script", getMacroFilterValue());

        // Select a different macro.
        waitForMacroListItem("Velocity", true);
        getSelenium().click(getMacroListItemLocator("Velocity"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();

        // Set the content field.
        setFieldValue("pd-content-input", "$xcontext.user");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{velocity}}\n$xcontext.user\n{{/velocity}}");
    }

    /**
     * Tests that the user can't move to the Edit Macro step without selection a macro first.
     */
    @Test
    public void testValidateSelectMacroStep()
    {
        openSelectMacroDialog();
        // Wait for the list of macros to be filled.
        waitForMacroListItem("Velocity", true);
        // Make sure no macro is selected.
        assertFalse(isMacroListItemSelected());
        // The validation message should be hidden.
        assertFieldErrorIsNotPresent();
        // Try to move to the next step.
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        // Check if the validation message is visible.
        assertFieldErrorIsPresent("Please select a macro from the list below.", MACRO_SELECTOR_LIST);

        // The validation message should be hidden when we change the macro category.
        selectMacroCategory("Navigation");
        // Wait for the list of macros to be updated (Velocity macro shouldn't be present in the updated list).
        waitForMacroListItem("Velocity", false);
        // Make sure no macro is selected.
        assertFalse(isMacroListItemSelected());
        // The validation message should be hidden.
        assertFieldErrorIsNotPresent();
        // Try to move to the next step.
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        // Check if the validation message is visible.
        assertFieldErrorIsPresent("Please select a macro from the list below.", MACRO_SELECTOR_LIST);

        // The validation message should be hidden when we filter the macros.
        filterMacrosContaining("anchor");
        // Wait for the list of macros to be filtered. The ToC macro should be filtered out.
        waitForMacroListItem("Table Of Contents", false);
        // Make sure no macro is selected.
        assertFalse(isMacroListItemSelected());
        // The validation message should be hidden.
        assertFieldErrorIsNotPresent();
        // Try to move to the next step.
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        // Check if the validation message is visible.
        assertFieldErrorIsPresent("Please select a macro from the list below.", MACRO_SELECTOR_LIST);

        // The validation message should be hidden when we cancel the dialog.
        closeDialog();
        openSelectMacroDialog();
        assertFieldErrorIsNotPresent();
        // Make sure no macro is selected.
        assertFalse(isMacroListItemSelected());
        // Try to move to the next step.
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        // Check if the validation message is visible.
        assertFieldErrorIsPresent("Please select a macro from the list below.", MACRO_SELECTOR_LIST);

        // Finally select a macro.
        getSelenium().click(getMacroListItemLocator("Id"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();

        // The validation message shouldn't be visible (we moved to the next step).
        assertFieldErrorIsNotPresent();

        // Set the name field.
        setFieldValue("pd-name-input", "foo");
        applyMacroChanges();

        // Check the result.
        switchToSource();
        assertSourceText("{{id name=\"foo\"/}}");
    }

    /**
     * Tests if the user can select from the previously inserted macros.
     */
    @Test
    public void testSelectFromPreviouslyInsertedMacros()
    {
        openSelectMacroDialog();

        // The "Previously Inserted Macros" category should be initially empty.
        selectMacroCategory("Previously Inserted Macros");
        waitForMacroListItemCount(0);

        // Insert a macro to see if it appears under the "Previously Inserted Macros" category.
        selectMacroCategory("All Macros");
        waitForMacroListItem("HTML", true);
        getSelenium().click(getMacroListItemLocator("HTML"));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();
        setFieldValue("pd-content-input", "xwiki");
        applyMacroChanges();

        // Check if the inserted macro is listed under the "Previously Inserted Macros" category.
        openSelectMacroDialog();
        selectMacroCategory("Previously Inserted Macros");
        // Wait for the macro list to be updated.
        waitForMacroListItemCount(1);
        // Velocity macro shouldn't be present among the previously used macros.
        assertFalse(getDriver().hasElementWithoutWaiting(By.xpath(getMacroListItemLocator("Velocity"))));
        assertTrue(getDriver().hasElementWithoutWaiting(By.xpath(getMacroListItemLocator("HTML"))));

        // Close the dialog and check the result.
        closeDialog();
        switchToSource();
        assertSourceText("{{html}}\nxwiki\n{{/html}}");
    }

    /**
     * @see XWIKI-4415: Context document not set when refreshing macros.
     */
    @Test
    public void testRefreshContextSensitiveVelocity()
    {
        switchToSource();
        setSourceText("{{velocity}}$doc.fullName{{/velocity}}");
        switchToWysiwyg();
        String expected = getRichTextArea().getText();
        refreshMacrosUsingShortcutKey();
        assertEquals(expected, getRichTextArea().getText());
    }

    /**
     * @see XWIKI-4541: Links are removed when a macro is collapsed and the editor looses focus.
     */
    @Test
    public void testLinksArePreservedWhenMacroIsCollapsed()
    {
        switchToSource();
        setSourceText("before\n\n{{velocity}}abc [[123>>http://www.xwiki.org]] xyz{{/velocity}}\n\nafter");
        switchToWysiwyg();
        // Make sure the rich text area is focused. We need to do this to be sure the blur event has any effect.
        focusRichTextArea();
        // Check if the link is present before collapsing the macro.
        String linkCountExpression = "return document.getElementsByTagName('a').length";
        assertEquals(1L, getRichTextArea().executeScript(linkCountExpression));
        // Select the macro.
        selectMacro(0);
        // Collapse the macro.
        toggleMacroCollapsedState();
        // Blur and focus again the rich text area.
        blurRichTextArea();
        focusRichTextArea();
        // Expand the macro.
        toggleMacroCollapsedState();
        // The link should have been preserved.
        assertEquals(1L, getRichTextArea().executeScript(linkCountExpression));
    }

    /**
     * @see XWIKI-4613: Macros that output STYLE tags inside the HTML body generate wiki syntax garbage.
     */
    @Test
    public void testHTMLMacroWithStyleTag()
    {
        switchToSource();
        StringBuilder sourceText = new StringBuilder();
        sourceText.append("{{html clean=\"false\"}}\n");
        sourceText.append("<style type=\"text/css\">\n");
        sourceText.append(".test {\n");
        sourceText.append("  color: red;\n");
        sourceText.append("}\n");
        sourceText.append("</style>\n");
        sourceText.append("<div class=\"test\">This is a test.</div>\n");
        sourceText.append("{{/html}}");
        setSourceText(sourceText.toString());
        switchToWysiwyg();
        // Force re-rendering.
        refreshMacrosUsingShortcutKey();
        // Check the result.
        switchToSource();
        assertSourceText(sourceText.toString());
    }

    /**
     * @see XWIKI-4856: Charset errors on macro insertion
     */
    @Test
    public void testMacroWithUnicodeCharacters()
    {
        // Insert a macro with Unicode characters.
        switchToSource();
        String sourceText = "before {{info}}\u0103\u0219\u021B\u00E2\u00EE\u00E9\u00E8{{/info}} after";
        setSourceText(sourceText);
        switchToWysiwyg();
        // Do a round-trip to the server to re-parse and re-render the content.
        refreshMacros();
        // Check if the content is affected by undo operation.
        typeText("1 2");
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        clickUndoButton(2);
        // Check the result.
        switchToSource();
        assertSourceText("1" + sourceText);
    }

    /**
     * @see XWIKI-4946: Default values for the required macro parameters should be send to the server by the WYSIWYG
     */
    @Test
    public void testDefaultValuesForMandatoryParametersAreSent()
    {
        open(this.getClass().getSimpleName(), getTestMethodName(), "edit", "editor=object");
        if (!isElementPresent("xclass_XWiki.WikiMacroClass")) {
            // Create the macro.
            getSelenium().select("classname", "WikiMacroClass");
            clickEditAddObject();
            setFieldValue("XWiki.WikiMacroClass_0_id", "now");
            setFieldValue("XWiki.WikiMacroClass_0_name", "Now");
            getSelenium().select("XWiki.WikiMacroClass_0_contentType", "No content");
            setFieldValue("XWiki.WikiMacroClass_0_code", "{{velocity}}$datetool.date{{/velocity}}");
            clickEditSaveAndContinue();
            // Create the mandatory parameter.
            getSelenium().select("classname", "WikiMacroParameterClass");
            clickEditAddObject();
            setFieldValue("XWiki.WikiMacroParameterClass_0_name", "format");
            getSelenium().select("XWiki.WikiMacroParameterClass_0_mandatory", "Yes");
            setFieldValue("XWiki.WikiMacroParameterClass_0_defaultValue", "yyyy.MM.dd");
            clickEditSaveAndContinue();
        }
        open(this.getClass().getSimpleName(), getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();
        // Insert the macro we just created.
        insertMacro("Now");
        applyMacroChanges();
        // Check the result.
        switchToSource();
        assertSourceText("{{now format=\"yyyy.MM.dd\"/}}");
    }

    /**
     * @see XWIKI-5013: HTML code visible when inserting velocity macro displaying a property
     */
    @Test
    public void testInsertVelocityMacroDisplayingAProperty()
    {
        insertMacro("Velocity");
        setFieldValue("pd-content-input", "$xwiki.getDocument(\"XWiki.Admin\").display(\"comment\")");
        applyMacroChanges();
        // Check the displayed text.
        // Trim the white-space that is coming from the macro selection boundary and which is not visible by the user.
        // There's also some white-space generate by the elements that wrap or follow the text area.
        assertEquals("Admin is the default Wiki Admin.", getRichTextArea().getText().trim());
    }

    /**
     * Tests that a macro can be inserted by clicking the associated tool bar button.
     */
    @Test
    public void testInsertMacroFromToolBar()
    {
        // The tool bar button for inserting the velocity macro has been added in WysiwygTestSetup.
        clickInsertMacroButton("velocity");
        waitForDialogToLoad();
        setFieldValue("pd-content-input", "$xwiki.version");
        applyMacroChanges();
        // Check if the macro was correctly inserted.
        switchToSource();
        assertSourceText("{{velocity}}\n$xwiki.version\n{{/velocity}}");
    }

    /**
     * @see XWIKI-4461: Cannot have a cursor around macro blocks in Wysiwyg
     * @see XWIKI-3335: Cannot move the caret with the right arrow key after a macro ending the document
     */
    @Test
    public void testTypeAroundMacro()
    {
        switchToSource();
        setSourceText("{{info}}one{{/info}}{{warning}}two{{/warning}}{{error}}three{{/error}}"
            + "\n\n{{info}}four{{/info}}\n\n{{warning}}five{{/warning}}");
        switchToWysiwyg();

        // Place the caret at the start of the first macro.
        moveCaret(getMacroOutputLocator(0) + ".firstChild.firstChild", 0);
        typeText("1");

        // Place the caret at the end of the first macro.
        moveCaret(getMacroOutputLocator(0) + ".firstChild.firstChild", 3);
        typeText(" ");
        // Check that Space didn't toggle the macro collapsed state.
        // Note: We have to select the rich text area frame because the visibility of an element is evaluated relative
        // to the current window.
        selectRichTextAreaFrame();
        try {
            assertFalse(getSelenium().isVisible(getMacroPlaceHolderLocator(0)));
            assertTrue(getSelenium().isVisible(getMacroOutputLocator(0)));
        } finally {
            selectTopFrame();
        }

        // Place the caret at the start of the third macro.
        moveCaret(getMacroOutputLocator(2) + ".firstChild.firstChild", 0);
        typeText("3");

        // Place the caret at the end of the third macro.
        moveCaret(getMacroOutputLocator(2) + ".firstChild.firstChild", 5);
        typeEnter();
        try {
            waitForDialogToLoad();
            fail("The macro edit box shouldn't be triggered by pressing Enter at the end of the output.");
        } catch (TimeoutException e) {
            // Expected.
        }
        typeText("2");

        // Place the caret at the start of the last macro.
        moveCaret(getMacroOutputLocator(4) + ".firstChild.firstChild", 0);
        typeEnter();
        typeText("4");

        // Place the caret at the end of the last macro.
        moveCaret(getMacroOutputLocator(4) + ".firstChild.firstChild", 4);
        typeEnter();
        typeText("5");

        switchToSource();
        assertSourceText("1{{info}}one{{/info}} {{warning}}two{{/warning}}3{{error}}three{{/error}}\n\n2"
            + "\n\n{{info}}\nfour\n{{/info}}\n\n4\n\n{{warning}}\nfive\n{{/warning}}\n\n5");
    }

    /**
     * @see XWIKI-7743: Wrong editor width when returning from full screen edit after editing/adding a macro
     */
    @Test
    public void testEditorWidthIsRestoredAfterFullScreenEdit()
    {
        clickEditInFullScreen();
        refreshMacros();
        clickExitFullScreen();
        // The width of the rich text area should be reset after exiting the full screen edit.
        assertEquals("", getSelenium().getEval("window.document.getElementsByTagName('iframe')[0].style.width"));
    }

    /**
     * @param index the index of a macro inside the edited document
     * @return a {@link String} representing a DOM locator for the specified macro
     */
    public String getMacroLocator(int index)
    {
        return "document.getElementsByClassName('macro')[" + index + "]";
    }

    /**
     * @return the number of macros detected in the edited document
     */
    public long getMacroCount()
    {
        return (Long) getRichTextArea().executeScript("return document.getElementsByClassName('macro').length");
    }

    /**
     * @return the number of selected macros in the edited document
     */
    public long getSelectedMacroCount()
    {
        return (Long) getRichTextArea()
            .executeScript("return document.getElementsByClassName('macro-selected').length");
    }

    /**
     * Wait for the specified number of macros to be selected.
     * 
     * @param count the number of selected macros to wait for
     */
    public void waitForSelectedMacroCount(final int count)
    {
        new Wait()
        {
            public boolean until()
            {
                return count == getSelectedMacroCount();
            }
        }.wait("The specified number of selected macros, " + count + ", hasn't been reached in a decent amount of time");
    }

    /**
     * The macro place holder is shown when the macro is collapsed. In this state the output of the macro is hidden.
     * 
     * @param index the index of a macro inside the edited document
     * @return a {@link String} representing a DOM locator for the place holder of the specified macro, relative to the
     *         edited document
     */
    public String getMacroPlaceHolderLocator(int index)
    {
        return "document.getElementsByClassName('macro-placeholder')[" + index + "]";
    }

    /**
     * The output of a macro is shown when the macro is expanded. In this state the macro place holder is hidden.
     * 
     * @param index the index of a macro inside the edited document
     * @return a {@link String} representing a DOM locator for the output of the specified macro, relative to the edited
     *         document
     */
    public String getMacroOutputLocator(int index)
    {
        return "document.getElementsByClassName('macro-output')[" + index + "]";
    }

    /**
     * Selects the macro with the specified index in the edited document.
     * 
     * @param index the index of the macro to be selected
     */
    public void selectMacro(int index)
    {
        String locator = getMacroLocator(index);
        selectRichTextAreaFrame();
        try {
            getSelenium().mouseOver(locator);
            getSelenium().mouseMove(locator);
            getSelenium().mouseDown(locator);
            getSelenium().mouseUp(locator);
            getSelenium().click(locator);
            getSelenium().mouseMove(locator);
            getSelenium().mouseOut(locator);
        } finally {
            selectTopFrame();
        }
        // Select the macro container to be sure that the shortcut keys (Enter, Space) work. The click usually places
        // the caret at the start of the macro content when typing is allowed.
        selectNodeContents(locator);
    }

    /**
     * Collapses the currently selected macro or, if none is selected, all the macros using the shortcut key.
     */
    public void collapseMacrosUsingShortcutKey()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "c"));
    }

    /**
     * Expands the currently selected macro or, if none is selected, all the macros using the shortcut key.
     */
    public void expandMacrosUsingShortcutKey()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "e"));
    }

    /**
     * Toggles the collapsed state of the currently selected macro.
     */
    public void toggleMacroCollapsedState()
    {
        getRichTextArea().sendKeys(Keys.SPACE);
    }

    /**
     * Opens the edit macro dialog to edit the specified macro.
     * 
     * @param index the index of the macro to be edited
     */
    public void editMacro(int index)
    {
        selectMacro(index);
        clickMenu(MENU_MACRO);
        clickMenu(MENU_EDIT);
        waitForDialogToLoad();
    }

    /**
     * Deletes the specified macro by selecting it and then pressing the Delete key.
     * 
     * @param index the index of the macro to be deleted
     */
    public void deleteMacro(int index)
    {
        selectMacro(index);
        typeDelete();
    }

    /**
     * Opens the insert macro dialog, chooses the specified macro and then opens the edit macro dialog to fill the
     * parameters of the selected macro.
     * 
     * @param macroName the name of the macro to insert
     */
    public void insertMacro(String macroName)
    {
        openSelectMacroDialog();

        // We have to wait for the specified macro to be displayed on the dialog because the loading indicator is
        // removed just before the list of macros is displayed and the Selenium click command can interfere.
        waitForMacroListItem(macroName, true);
        getSelenium().click(getMacroListItemLocator(macroName));
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Select']");
        waitForDialogToLoad();
    }

    /**
     * Applies the changes from the edit macro dialog.<br/>
     * NOTE: This method can be called after both edit and insert macro actions.
     */
    public void applyMacroChanges()
    {
        // The label of the finish button is "Apply" when we edit a macro and "Insert Macro" when we insert a macro.
        getSelenium().click("//div[@class = 'xDialogFooter']/button[text() = 'Apply' or text() = 'Insert Macro']");
        waitForEditorToLoad();
    }

    /**
     * Refreshes the macros present in the edited document using the menu.
     */
    public void refreshMacros()
    {
        clickMenu(MENU_MACRO);
        clickMenu(MENU_REFRESH);
        waitForEditorToLoad();
    }

    /**
     * Refreshes the macros present in the edited document using the shortcut key.
     */
    public void refreshMacrosUsingShortcutKey()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "r"));
        waitForEditorToLoad();
    }

    /**
     * Selects the specified macro category.
     * <p>
     * NOTE: This method doesn't wait for the category to be loaded!
     * 
     * @param category the name of the macro category to select
     */
    public void selectMacroCategory(String category)
    {
        getSelenium().select(MACRO_CATEGORY_SELECTOR, category);
    }

    /**
     * @return the selected macro category
     */
    public String getSelectedMacroCategory()
    {
        return getSelenium().getSelectedLabel(MACRO_CATEGORY_SELECTOR);
    }

    /**
     * Waits for the specified macro to be displayed or hidden on the "Select Macro" dialog.
     * 
     * @param macroName the name of a macro
     * @param present {@code true} to wait for the specified macro to be present, {@code false} to wait for it to be
     *            hidden
     */
    public void waitForMacroListItem(final String macroName, final boolean present)
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver input)
            {
                return present == MacroTest.this.getDriver().hasElementWithoutWaiting(
                    By.xpath(getMacroListItemLocator(macroName)));
            }
        });
    }

    /**
     * Waits for the specified number of macros to be listed on the "Select Macro" dialog.
     * 
     * @param count the expected number of macros currently displayed on the "Select Macro" dialog
     */
    public void waitForMacroListItemCount(final int count)
    {
        new Wait()
        {
            public boolean until()
            {
                return count == getMacroListItemCount();
            }
        }.wait("The number of listed macros doesn't match.");
    }

    /**
     * @param macroName a macro name
     * @return the selector for the specified macro in the list of macros from the "Select Macro" dialog
     */
    public String getMacroListItemLocator(String macroName)
    {
        return "//div[contains(@class, 'xListBox')]//div[contains(@class, 'xMacroLabel') and text() = '" + macroName
            + "']";
    }

    /**
     * @return the number of macro list items on the "Select Macro" dialog.
     */
    public int getMacroListItemCount()
    {
        return getSelenium().getXpathCount("//div[contains(@class, 'xListItem xMacro')]").intValue();
    }

    /**
     * @param filter a text used to filter the macro list items
     * @return the number of macro list items containing the specified text
     */
    public int getMacroListItemCount(String filter)
    {
        return getSelenium().getXpathCount(
            "//div[contains(@class, 'xListItem xMacro') and contains(., '" + filter + "')]").intValue();
    }

    /**
     * Sets the value of the live filter to the given string.
     * 
     * @param filter the value to set to the live macro filter
     */
    public void filterMacrosContaining(String filter)
    {
        getSelenium().typeKeys(MACRO_LIVE_FILTER_SELECTOR, filter);
    }

    /**
     * @return the value of the live macro filter
     */
    public String getMacroFilterValue()
    {
        return getSelenium().getValue(MACRO_LIVE_FILTER_SELECTOR);
    }

    /**
     * Opens the "Select Macro" dialog.
     */
    public void openSelectMacroDialog()
    {
        clickMenu(MENU_MACRO);
        assertTrue(isMenuEnabled(MENU_INSERT));
        clickMenu(MENU_INSERT);
        waitForDialogToLoad();
    }

    /**
     * @return {@code true} if there is a macro list item selected in the list of macros from the "Select Macro" dialog,
     *         {@code false} otherwise
     */
    public boolean isMacroListItemSelected()
    {
        return isElementPresent("//div[contains(@class, 'xMacro') and contains(@class, 'xListItem-selected')]");
    }

    /**
     * Clears the macro selection. No macro should be selected after calling this method.
     */
    public void clearMacroSelection()
    {
        moveCaret("document.body", 0);
        triggerToolbarUpdate();
        waitForSelectedMacroCount(0);
    }

    /**
     * Clicks the tool bar button corresponding to the specified macro.
     * 
     * @param macroId a macro identifier
     */
    public void clickInsertMacroButton(String macroId)
    {
        pushToolBarButton(String.format("Insert %s macro", macroId));
    }
}

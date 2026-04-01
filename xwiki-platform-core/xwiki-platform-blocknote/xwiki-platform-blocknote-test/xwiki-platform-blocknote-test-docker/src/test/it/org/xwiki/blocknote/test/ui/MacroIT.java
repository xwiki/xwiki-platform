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
package org.xwiki.blocknote.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.blocknote.test.po.BlockNoteEditor;
import org.xwiki.blocknote.test.po.BlockNoteRichTextArea;
import org.xwiki.edit.test.po.InplaceEditablePage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WikiEditPage;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;
import org.xwiki.wysiwyg.test.po.MacroDialogSelectModal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify macro related features of the BlockNote editor integration.
 *
 * @version $Id$
 * @since 18.3.0RC1
 */
@UITest(
    properties = {
        // The Image Wizard needs this to be able to upload images.
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin"
    }
)
class MacroIT extends AbstractBlockNoteIT
{
    @Test
    @Order(1)
    void editMacro(TestUtils setup, TestReference testReference)
    {
        // Start fresh.
        setup.deletePage(testReference);
        setup.createPage(testReference, """
            first {{success cSSclaSS="one" tiTle="inline"}}done{{/success}} line

            {{info TItle="My title"}}
            My content.
            {{/info}}

            end""");

        InplaceEditablePage page = new InplaceEditablePage().editInplace();

        BlockNoteEditor editor = new BlockNoteEditor("content");
        BlockNoteRichTextArea textArea = editor.getRichTextArea();

        // Edit the inline macro.
        MacroDialogEditModal macroEditModal = textArea.doubleClickMacro(0);
        assertEquals("Success Message", macroEditModal.getMacroName());
        assertEquals("inline", macroEditModal.getMacroParameter("title"));
        assertEquals("one", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("done", macroEditModal.getMacroContent());

        macroEditModal.setMacroParameter("title", "Inline title").setMacroParameter("cssClass", "two")
            .setMacroContent("Done!").clickSubmit();

        // Edit the block macro.
        macroEditModal = textArea.doubleClickMacro(1);
        assertEquals("Info Message", macroEditModal.getMacroName());
        assertEquals("My title", macroEditModal.getMacroParameter("title"));
        assertEquals("My content.", macroEditModal.getMacroContent());

        macroEditModal.setMacroParameter("title", "New title").setMacroParameter("cssClass", "test")
            .setMacroContent("New content.").clickSubmit();

        // Edit again the inline macro.
        macroEditModal = textArea.doubleClickMacro(0);
        assertEquals("Success Message", macroEditModal.getMacroName());
        assertEquals("Inline title", macroEditModal.getMacroParameter("title"));
        assertEquals("two", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Done!", macroEditModal.getMacroContent());

        macroEditModal.setMacroParameter("title", "My Inline Title").setMacroParameter("cssClass", "three")
            .clickSubmit();
        
        // Edit again the block macro.
        macroEditModal = textArea.doubleClickMacro(1);
        assertEquals("Info Message", macroEditModal.getMacroName());
        assertEquals("New title", macroEditModal.getMacroParameter("title"));
        assertEquals("test", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("New content.", macroEditModal.getMacroContent());

        macroEditModal.setMacroParameter("cssClass", "newTest").setMacroContent("Some information.").clickSubmit();

        page.save();
        WikiEditPage wikiEditor = page.editWiki();
        assertEquals("""
            (% style="color:default;background-color:default;text-align:left" %)
            first {{success cssClass="three" title="My Inline Title"}}Done!{{/success}} line

            {{info cssClass="newTest" title="New title"}}
            Some information.
            {{/info}}

            (% style="color:default;background-color:default;text-align:left" %)
            end

            (% style="color:default;background-color:default;text-align:left" %)""", wikiEditor.getContent());

        // Edit the page again to verify that we can also change the macro, not just its parameters.
        wikiEditor.clickCancel();
        page.editInplace();

        editor = new BlockNoteEditor("content");
        textArea = editor.getRichTextArea();

        // Change the inline macro from success to error.
        macroEditModal = textArea.doubleClickMacro(0);
        assertEquals("Success Message", macroEditModal.getMacroName());
        assertEquals("My Inline Title", macroEditModal.getMacroParameter("title"));
        assertEquals("three", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Done!", macroEditModal.getMacroContent());

        MacroDialogSelectModal macroSelectModal = macroEditModal.clickChangeMacro();
        macroSelectModal.filterByText("error", 1).getFirstMacro().orElseThrow().click();
        macroEditModal = macroSelectModal.clickSelect();
        assertEquals("Error Message", macroEditModal.getMacroName());
        assertEquals("My Inline Title", macroEditModal.getMacroParameter("title"));
        // Values of parameters that are not editable in-place seem to be lost when changing the macro. This is not
        // specific to BlockNote.
        assertEquals("", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Done!", macroEditModal.getMacroContent());
        macroEditModal.setMacroParameter("cssClass", "four").setMacroContent("Failed!").clickSubmit();

        // Change the block macro from info to warning.
        macroEditModal = textArea.doubleClickMacro(1);
        assertEquals("Info Message", macroEditModal.getMacroName());
        assertEquals("New title", macroEditModal.getMacroParameter("title"));
        assertEquals("newTest", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Some information.", macroEditModal.getMacroContent());

        macroSelectModal = macroEditModal.clickChangeMacro();
        macroSelectModal.filterByText("warning", 1).getFirstMacro().orElseThrow().click();
        macroEditModal = macroSelectModal.clickSelect();
        assertEquals("Warning Message", macroEditModal.getMacroName());
        assertEquals("New title", macroEditModal.getMacroParameter("title"));
        // Values of parameters that are not editable in-place seem to be lost when changing the macro. This is not
        // specific to BlockNote.
        assertEquals("", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Some information.", macroEditModal.getMacroContent());
        macroEditModal.setMacroParameter("cssClass", "new-test").setMacroContent("Some warning!").clickSubmit();

        // Edit again the inline macro.
        macroEditModal = textArea.doubleClickMacro(0);
        assertEquals("Error Message", macroEditModal.getMacroName());
        assertEquals("My Inline Title", macroEditModal.getMacroParameter("title"));
        assertEquals("four", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Failed!", macroEditModal.getMacroContent());
        macroEditModal.clickCancel();

        // Edit again the block macro.
        macroEditModal = textArea.doubleClickMacro(1);
        assertEquals("Warning Message", macroEditModal.getMacroName());
        assertEquals("New title", macroEditModal.getMacroParameter("title"));
        assertEquals("new-test", macroEditModal.getMacroParameter("cssClass"));
        assertEquals("Some warning!", macroEditModal.getMacroContent());
        macroEditModal.clickCancel();

        page.save();
        wikiEditor = page.editWiki();
        assertEquals("""
            (% style="color:default;background-color:default;text-align:left" %)
            first {{error cssClass="four" title="My Inline Title"}}Failed!{{/error}} line

            {{warning cssClass="new-test" title="New title"}}
            Some warning!
            {{/warning}}

            (% style="color:default;background-color:default;text-align:left" %)
            end

            (% style="color:default;background-color:default;text-align:left" %)""", wikiEditor.getContent());
    }
}

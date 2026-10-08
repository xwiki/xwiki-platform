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
package org.xwiki.ckeditor.test.ui;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditorConfigurationPane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.MessageBoxElement;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests how rendering macros are integrated in CKEditor.
 * 
 * @version $Id$
 */
@UITest(
    properties = {
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib. See https://jira.xwiki.org/browse/XWIKI-8271
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",

        // The macro service uses the extension index script service to get the list of uninstalled macros (from
        // extensions) which expects an implementation of the extension index. The extension index script service is a
        // core extension so we need to make the extension index also core.
        "org.xwiki.platform:xwiki-platform-extension-index",
        // Solr search is used to get suggestions for the link quick action.
        "org.xwiki.platform:xwiki-platform-search-solr-query"
    },
    resolveExtraJARs = true
)
class MacroIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup, TestConfiguration testConfiguration)
    {
        setup.loginAsSuperAdmin();
        CKEditorConfigurationPane.open().setLoadJavaScriptSkinExtensions(true).clickSave();

        createAndLoginStandardUser(setup);
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    @AfterAll
    void afterAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        CKEditorConfigurationPane.open().setLoadJavaScriptSkinExtensions(false).clickSave();
    }

    @Test
    @Order(1)
    void children(TestUtils setup, TestReference testReference)
    {
        // Create a child page.
        LocalDocumentReference childReference =
            new LocalDocumentReference("Child", testReference.getLastSpaceReference());
        setup.createPage(childReference, "Child page content", "Child page title");

        // Use the Children Macro.
        edit(setup, testReference, false);
        setSource("before\n\n{{children/}}\n\nafter");
        textArea.waitUntilContentContains("Child page title");

        // Verify that the macro output is properly protected (not converted to wiki syntax).
        textArea.sendKeys(" end");
        assertSourceEquals("before\n\n{{children/}}\n\nafter end");
    }

    @Test
    @Order(2)
    void macroContent(TestUtils setup, TestReference testReference)
    {
        WYSIWYGEditPage editPage = edit(setup, testReference, true);

        setSource("""
            {{box}}
            Inline {{box}}<param></param>{{/box}}.
            {{/box}}""");

        this.textArea.waitUntilContentContains("Inline");

        ViewPage viewPage = editPage.clickSaveAndView();
        assertThat(viewPage.getContent(), containsString("<param></param>"));
    }

    @Test
    @Order(3)
    void macroPlaceholder(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference, true);
        setSource("before\n\n{{id name='test'/}}\n\nafter");
        assertEquals("before\nmacro:id\nafter", this.textArea.getText());

        // The macro name is displayed next to the move handle, but only while the macro is hovered.
        assertNull(this.textArea.getMacroNameNextToMoveHandle(0));
        this.textArea.hoverMacro(0);
        assertEquals("id", this.textArea.getMacroNameNextToMoveHandle(0));

        this.textArea.sendKeys(Keys.HOME, Keys.LEFT);
        this.textArea.waitUntilWidgetSelected();
        this.textArea.sendKeys(Keys.ENTER);
        MacroDialogEditModal macroEditModal = new MacroDialogEditModal().waitUntilReady();
        assertEquals("test", macroEditModal.getMacroParameter("name"));
        macroEditModal.setMacroParameter("name", "foo").clickSubmit();
        this.textArea.waitForContentRefresh();

        assertEquals("before\nmacro:id\nafter", this.textArea.getText());
        assertSourceEquals("before\n\n{{id name=\"foo\"/}}\n\nafter");
    }

    @Test
    @Order(4)
    void editInlineParametersWithTheMacroEditModal(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference, true);
        setSource("""
            before

            {{info title="Parent Info"}}
            **one**

            {{success title="Child Success"}}
            __two__
            {{/success}}

            //three//
            {{/info}}

            after""");

        // Change the title parameter and the macro content inline.
        this.textArea.sendKeys(Keys.PAGE_UP, Keys.DOWN, Keys.HOME, "The ", Keys.DOWN, Keys.END, ".1");

        // Edit the outer macro and assert the parameter values.
        MacroDialogEditModal macroEditModal = this.editor.getBalloonToolBar().editMacro();
        assertEquals("The Parent Info", macroEditModal.getMacroParameter("title"));
        assertEquals("""
            **one.1**

            {{success title="Child Success"}}
            __two__
            {{/success}}

            //three//""", macroEditModal.getMacroContent());

        macroEditModal.setMacroParameter("title", "Modified Parent Info");
        macroEditModal.setMacroContent("""
            **one.1**

            {{success title="Child Success"}}
            --two--
            {{/success}}

            //three//""").clickSubmit();

        this.textArea.waitForContentRefresh();

        // Modify again the tile parameter and the macro content inline.
        this.textArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.RIGHT));
        this.textArea.sendKeys("Final");
        this.textArea.sendKeys(Keys.PAGE_UP, Keys.UP, Keys.DOWN, Keys.DOWN, Keys.HOME, "zero ");

        assertSourceEquals("""
            before

            {{info title="Final Parent Info"}}
            **zero one.1**

            {{success title="Child Success"}}
            --two--
            {{/success}}

            //three//
            {{/info}}

            after""");
    }

    @Test
    @Order(5)
    void insertInfoBoxFromInsertMenu(TestUtils setup, TestReference testReference)
    {
        WYSIWYGEditPage editPage = edit(setup, testReference, true);

        // The Info Box is inserted directly, with a default message, and the caret is placed at the start of it.
        this.editor.getToolBar().insertInfoBox();
        assertThat(this.textArea.getText(), containsString("Type your information message here."));
        this.textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        this.textArea.sendKeys("This is an info macro!");
        assertThat(this.textArea.getText(), containsString("This is an info macro!"));

        // The editor keeps an empty paragraph after the inserted macro, so that the user can type after it.
        assertSourceEquals("{{info}}\nThis is an info macro!\n{{/info}}\n\n ", true);

        List<MessageBoxElement> messageBoxes = editPage.clickSaveAndView().getMessageBoxes();
        assertEquals(1, messageBoxes.size());
        MessageBoxElement infoBox = messageBoxes.get(0);
        assertEquals(MessageBoxElement.Type.INFO, infoBox.getType());
        assertEquals("This is an info macro!", infoBox.getText());
    }

    @Test
    @Order(6)
    void inlineEditMacroInsertedFromOtherMacros(TestUtils setup, TestReference testReference)
    {
        WYSIWYGEditPage editPage = edit(setup, testReference, true);
        this.textArea.sendKeys("before", Keys.ENTER);

        MacroDialogEditModal macroEditModal =
            this.editor.getToolBar().insertOtherMacro().filterByText("Info Message", 1).clickSelect();
        macroEditModal.setMacroParameter("title", "This is a title!");
        macroEditModal.setMacroContent("This is an info macro!");
        macroEditModal.clickSubmit();
        this.textArea.waitForContentRefresh();
        this.textArea.waitUntilTextContains("This is an info macro!");

        // Edit the title parameter and then the macro content in-line, moving the caret from the paragraph placed
        // before the macro.
        this.textArea.sendKeys(Keys.PAGE_UP, Keys.UP, Keys.DOWN, Keys.END, " More title.", Keys.DOWN, Keys.END,
            " More content.");

        // Format the last word of the macro content.
        this.textArea.sendKeys(Keys.LEFT, Keys.chord(Keys.CONTROL, Keys.SHIFT, Keys.LEFT));
        this.editor.getToolBar().bold().italic().underline();

        assertSourceEquals("""
            before

            {{info title="This is a title! More title."}}
            This is an info macro! More __//**content**//__.
            {{/info}}""");

        List<MessageBoxElement> messageBoxes = editPage.clickSaveAndView().getMessageBoxes();
        assertEquals(1, messageBoxes.size());
        MessageBoxElement infoBox = messageBoxes.get(0);
        assertEquals(MessageBoxElement.Type.INFO, infoBox.getType());
        assertEquals("This is a title! More title.\nThis is an info macro! More content.", infoBox.getText());
        assertTrue(infoBox.isBold("content"));
        assertTrue(infoBox.isItalic("content"));
        assertTrue(infoBox.isUnderlined("content"));
    }

    @Test
    @Order(7)
    void insertDisplayMacroFromOtherMacros(TestUtils setup, TestReference testReference) throws Exception
    {
        LocalDocumentReference displayedReference =
            new LocalDocumentReference("Displayed", testReference.getLastSpaceReference());
        setup.createPage(displayedReference, "Displayed page content", "Displayed page title");
        // The page picker of the macro reference parameter uses the search to get its suggestions.
        waitForSolrIndexing(setup);

        WYSIWYGEditPage editPage = edit(setup, testReference, true);

        MacroDialogEditModal macroEditModal =
            this.editor.getToolBar().insertOtherMacro().filterByText("Display other pages", 1).clickSelect();
        macroEditModal.getMacroParameterSuggestInput("reference").sendKeys("Displayed").selectByIndex(0);
        macroEditModal.clickSubmit();
        this.textArea.waitUntilTextContains("Displayed page content");

        assertSourceEquals(
            String.format("{{display reference=\"%s\"/}}", setup.serializeLocalReference(displayedReference)));

        ViewPage viewPage = editPage.clickSaveAndView();
        assertEquals("Displayed page content", viewPage.getContent());
    }
}

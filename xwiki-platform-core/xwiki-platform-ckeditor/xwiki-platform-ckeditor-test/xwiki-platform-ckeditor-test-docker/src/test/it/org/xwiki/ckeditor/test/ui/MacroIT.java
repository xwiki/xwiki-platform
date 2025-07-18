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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditorConfigurationPane;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

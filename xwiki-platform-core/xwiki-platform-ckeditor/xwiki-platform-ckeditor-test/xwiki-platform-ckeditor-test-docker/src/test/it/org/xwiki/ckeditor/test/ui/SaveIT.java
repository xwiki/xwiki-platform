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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.panels.test.po.DocumentInformationPanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.DocumentSyntaxPicker;
import org.xwiki.test.ui.po.DocumentSyntaxPicker.SyntaxConversionConfirmationModal;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the Save plugin.
 *
 * @version $Id$
 * @since 1.13
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
class SaveIT extends AbstractCKEditorIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    @Test
    @Order(1)
    void save(TestUtils setup, TestReference testReference)
    {
        WYSIWYGEditPage editPage = edit(setup, testReference);
        textArea.clear();
        textArea.sendKeys("xyz");
        editPage.clickSaveAndView().editWYSIWYG();
        assertEquals("<p>xyz</p>", editor.waitToLoad().getRichTextArea().getContent());
    }

    @Test
    @Order(2)
    void saveAfterSyntaxChange(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "[[label>>#target]]", "", "xwiki/2.0");
        // Wait for the WYSIWYG edit page to load.
        WYSIWYGEditPage editPage = edit(setup, testReference, false);

        // Type some text to verify that it isn't lost when we change the syntax.
        // First move the cursor to the start before entering the text to ensure it is outside the link, being
        // outside the link is the initial state in Chrome but not in Firefox.
        textArea.sendKeys(Keys.HOME, "test ");

        DocumentSyntaxPicker documentSyntaxPicker = new DocumentInformationPanel().getSyntaxPicker();
        assertEquals("xwiki/2.0", documentSyntaxPicker.getSelectedSyntax());

        SyntaxConversionConfirmationModal confirmationModal = documentSyntaxPicker.selectSyntaxById("xwiki/2.1");
        assertTrue(confirmationModal.getMessage()
            .contains("from the previous XWiki 2.0 syntax to the selected XWiki 2.1 syntax?"));
        confirmationModal.confirmSyntaxConversion();

        assertEquals("test [[label>>||anchor=\"target\"]]", editPage.clickSaveAndView().editWiki().getContent());
        assertEquals("xwiki/2.1", new DocumentInformationPanel().getSyntaxPicker().getSelectedSyntax());
    }
}

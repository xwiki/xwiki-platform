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
package org.xwiki.administration.test.ui;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.EditingAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.EditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate the Editing section of the Administration application, and in particular that changing the wiki-level
 * default editor controls which editor is used when editing a page.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@UITest
class EditingIT
{
    private String initialDefaultEditor;

    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Remember the configured default editor so that we can restore it after the test.
        this.initialDefaultEditor = EditingAdministrationSectionPage.gotoPage().getDefaultEditor();
    }

    @AfterAll
    void afterAll(TestUtils setup)
    {
        // Restore the default editor to its initial value so that other tests are not affected.
        setup.loginAsSuperAdmin();
        EditingAdministrationSectionPage.gotoPage().setDefaultEditor(this.initialDefaultEditor);
    }

    /**
     * Validate that changing the default editor in the Editing administration section controls the editor used when
     * editing a page. See XWIKI-18672.
     */
    @Test
    @Order(1)
    void changeDefaultEditor(TestUtils setup, TestReference testReference)
    {
        // Create the page used to verify which editor is loaded by default.
        setup.createPage(testReference, "");

        // Set the default editor to Text (i.e. the Wiki editor) and verify that the Wiki editor is loaded instead.
        EditingAdministrationSectionPage editingSection = EditingAdministrationSectionPage.gotoPage();
        editingSection.setDefaultEditor("Text");
        assertEquals("Text", editingSection.getDefaultEditor());

        setup.gotoPage(testReference, "edit");
        assertEquals(EditPage.Editor.WIKI, new EditPage().getEditor());

        // Set the default editor to WYSIWYG and verify that the WYSIWYG editor is loaded when editing the page.
        editingSection = EditingAdministrationSectionPage.gotoPage();
        editingSection.setDefaultEditor("Wysiwyg");
        assertEquals("Wysiwyg", editingSection.getDefaultEditor());

        setup.gotoPage(testReference, "edit");
        assertEquals(EditPage.Editor.WYSIWYG, new EditPage().getEditor());
    }
}

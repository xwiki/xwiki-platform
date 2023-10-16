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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.ckeditor.test.po.CKEditorConfigurationPane;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Tests how rendering macros are integrated in CKEditor.
 * 
 * @version $Id$
 */
@UITest
class MacroIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        CKEditorConfigurationPane.open().setLoadJavaScriptSkinExtensions(true).clickSave();

        createAndLoginStandardUser(setup);
    }

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference);
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
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
}

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
package org.xwiki.requiredrights.test.ui;

import org.junit.jupiter.api.Test;
import org.xwiki.requiredrights.test.po.RequiredRightsEditPanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.InformationPane;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test creating a page with required rights.
 *
 * @version $Id$
 * @since 15.6RC1
 */
@UITest
class RequiredRightsIT
{
    @Test
    void requiredRights(TestUtils setup, TestReference testReference)
    {
        // Need to do the test with admin as superadmin always has all the rights.
        setup.createAdminUser();
        setup.loginAsAdmin();
        // Cleanup in case of re-run.
        setup.deletePage(testReference);
        // Create a page with a macro requiring the Script right (velocity macro).
        String content = "content";
        ViewPage page = setup.createPage(testReference, "{{velocity}}" + content + "{{/velocity}}");
        assertEquals(content, page.getContent());
        WikiEditPage wikiEditPage = page.editWiki();
        new RequiredRightsEditPanel().select(1);
        PreviewEditPage previewEditPage = wikiEditPage.clickPreview();
        assertTrue(previewEditPage.getContent().contains(String.format("Failed to execute the [velocity] macro. "
                + "Cause: [The execution of the [velocity] script macro is not allowed in [%s]. "
                + "Check the rights of its last author, the required rights of the document, or the parameters if it's "
                + "rendered from another script.]",
            setup.serializeReference(testReference))));
        previewEditPage.clickBackToEdit();
        new RequiredRightsEditPanel().select(1);
        previewEditPage = wikiEditPage.clickPreview();
        assertEquals(content, previewEditPage.getContent());
    }

    @Test
    void requiredRightsInplace(TestUtils setup, TestReference testReference)
    {
        // Need to do the test with admin as superadmin always has all the rights.
        setup.createAdminUser();
        setup.loginAsAdmin();
        // Cleanup in case of re-run.
        setup.deletePage(testReference);
        // Create a page with a macro requiring the Script right (velocity macro).
        String content = "content";
        ViewPage page = setup.createPage(testReference, "{{velocity}}" + content + "{{/velocity}}");
        assertEquals(content, page.getContent());
        InformationPane informationPane = page.openInformationDocExtraPane();
        informationPane.editRequiredRights()
            .setValue("1")
            .clickSave();
        assertTrue(page.getContent().contains(String.format("Failed to execute the [velocity] macro. "
                + "Cause: [The execution of the [velocity] script macro is not allowed in [%s]. "
                + "Check the rights of its last author, the required rights of the document, or the parameters if it's "
                + "rendered from another script.]",
            setup.serializeReference(testReference))));
        informationPane.editRequiredRights()
            .setValue("2")
            .clickSave();
        setup.gotoPage(testReference);
        assertEquals(content, page.getContent());
    }
}

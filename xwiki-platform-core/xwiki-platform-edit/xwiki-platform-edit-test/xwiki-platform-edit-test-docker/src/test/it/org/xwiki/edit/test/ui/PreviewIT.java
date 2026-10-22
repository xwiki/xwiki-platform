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
package org.xwiki.edit.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.PreviewEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the preview of a page being edited.
 *
 * @version $Id$
 */
@UITest(properties = {
    // The previewed page needs Programming Right, which the functional tests deny to wiki content by default, to
    // verify that the preview is rendered with the rights of the user who edits the page.
    "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:.*PreviewIT\\..*"
})
class PreviewIT
{
    @Test
    @Order(1)
    void previewNewPage(TestUtils setup, TestReference testReference)
    {
        setup.loginAsSuperAdmin();

        WikiEditPage wikiEditPage = WikiEditPage.gotoPage(testReference);
        wikiEditPage.setTitle("Preview Title");
        wikiEditPage.setContent("{{velocity}}$xwiki.hasAccessLevel('programming')"
            + " $tdoc.author $tdoc.contentAuthor $tdoc.creator{{/velocity}}");
        PreviewEditPage previewPage = wikiEditPage.clickPreview();

        // The preview is rendered with the rights of the user who edits the page.
        assertEquals("true XWiki.superadmin XWiki.superadmin XWiki.superadmin", previewPage.getContent());
        // The preview displays the page title.
        assertEquals("Preview Title", new ViewPage().getDocumentTitle());
    }
}

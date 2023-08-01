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

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of the CKEditor Link Plugin.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.3
 */
@UITest(extraJARs = {
    "org.xwiki.platform:xwiki-platform-search-solr-query"
})
class LinkPluginIT extends AbstractCKEditorIT
{
    @Test
    void insertLinks(TestUtils setup, TestReference testReference, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(testReference);
        // Create a sub-page with an attachment, to have something to link to.
        uploadAttachment(setup, new DocumentReference("subPage", testReference.getLastSpaceReference()), "image.gif");

        // Wait for SOLR indexing to complete as the link search is based on solr indexation.
        waitForSolrIndexing(setup, testConfiguration);

        ViewPage page = setup.gotoPage(testReference);
        WYSIWYGEditPage wysiwygEditPage = page.editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        String spaceName = testReference.getLastSpaceReference().getParent().getName();
        editor.getToolBar().insertOrEditLink()
            .setResourceValue("subPage")
            .selectPageItem(String.format("%s / insertLinks", spaceName), "subPage")
            .submit();

        editor.getRichTextArea().sendKeys(Keys.RIGHT, Keys.ENTER);

        editor.getToolBar().insertOrEditLink()
            .setResourceType("attach")
            .setResourceValue("image")
            .selectPageItem(String.format("%s / insertLinks / subPage", spaceName), "image.gif")
            .submit();

        ViewPage savedPage = wysiwygEditPage.clickSaveAndView();

        // Verify that the content matches what we did using CKEditor.
        assertEquals("[[type the link label>>doc:subPage]]\n"
            + "\n"
            + "[[type the link label>>attach:subPage@image.gif]]", savedPage.editWiki().getContent());
    }

    private ViewPage uploadAttachment(TestUtils setup, DocumentReference testReference, String attachmentName)
        throws Exception
    {
        ViewPage newPage = setup.createPage(testReference, "", "");
        setup.attachFile(testReference, attachmentName,
            getClass().getResourceAsStream("/ResourcePicker/" + attachmentName), false);
        return newPage;
    }
}

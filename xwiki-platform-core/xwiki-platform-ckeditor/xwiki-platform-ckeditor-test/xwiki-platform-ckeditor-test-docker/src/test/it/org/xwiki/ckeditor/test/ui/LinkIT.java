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
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.LinkDialog;
import org.xwiki.ckeditor.test.po.LinkPickerModal;
import org.xwiki.ckeditor.test.po.LinkTreeElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of the CKEditor Link Plugin.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.3
 */
@UITest(properties = {
    "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml"
    }, extraJARs = {
    "org.xwiki.platform:xwiki-platform-search-solr-query",
    "org.xwiki.platform:xwiki-platform-notifications-filters-default"
})
class LinkIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Wait for Solr indexing to complete as the link search is based on Solr indexation.
        setup.loginAsSuperAdmin();
        waitForSolrIndexing(setup, testConfiguration);

        createAndLoginStandardUser(setup);
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
    }

    @Test
    @Order(1)
    void insertLinks(TestUtils setup, TestReference testReference) throws Exception
    {
        // Create a sub-page with an attachment, to have something to link to.
        String attachmentName = "image.gif";
        setup.createPage(testReference, "", "");
        DocumentReference subPage = new DocumentReference("subPage", testReference.getLastSpaceReference());
        setup.createPage(subPage, "", "");
        setup.attachFile(subPage, attachmentName,
            getClass().getResourceAsStream("/ResourcePicker/" + attachmentName), false);

        edit(setup, testReference, false);

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

        // Verify that the content matches what we did using CKEditor.
        assertSourceEquals("[[type the link label>>doc:subPage]]\n\n[[type the link label>>attach:subPage@image.gif]]");
    }

    @Test
    @Order(2)
    void useLinkShortcutWhenTargetPageNameHasSpecialCharacters(TestUtils setup, TestReference testReference)
    {
        // Create the link target page with special characters in its name.
        LocalDocumentReference childPageReference =
            new LocalDocumentReference("\"Quote\" and 'Apostrophe'", testReference.getLastSpaceReference());
        setup.createPage(childPageReference, "", "");

        edit(setup, testReference);

        textArea.sendKeys("[quo");
        AutocompleteDropdown linkDropDown = new AutocompleteDropdown();
        linkDropDown.waitForItemSelected("[quo", childPageReference.getName());
        textArea.sendKeys(Keys.ENTER);
        linkDropDown.waitForItemSubmitted();

        assertSourceEquals(String.format("[[%1$s>>%1$s]] ", childPageReference.getName()));
    }

    @Test
    @Order(3)
    void createLinkForNotExistingPage(TestUtils setup, TestReference testReference)
    {
        setup.createPage(testReference, "", "createLinkForNotExistingPage");
        edit(setup, testReference, false);
        editor.getToolBar()
            .insertOrEditLink()
            .setResourceValue("Foo.Bar.Buz.Test")
            .createLinkOfNewPage(true)
            .submit();
        editor.getRichTextArea().sendKeys(Keys.RIGHT, Keys.ENTER);
        editor.getToolBar()
            .insertOrEditLink()
            .setResourceValue("Fa.Fi.Foo")
            .createLinkOfNewPage(false)
            .submit();
        editor.getRichTextArea().sendKeys(Keys.RIGHT, Keys.ENTER);
        LinkDialog linkDialog = editor.getToolBar().insertOrEditLink();
        LinkPickerModal linkPickerModal = linkDialog.openDocumentPicker();
        LinkTreeElement tree = linkPickerModal.getTree();
        tree.waitForIt();
        assertTrue(tree.hasNewPageCreation(testReference));
        tree.createNode(testReference, "SubPage");
        SpaceReference testReferenceLastSpace = new SpaceReference("SubPage", testReference.getLastSpaceReference());
        DocumentReference subPage = new DocumentReference("WebHome", testReferenceLastSpace);
        tree.createNode(subPage, "Another");
        testReferenceLastSpace = new SpaceReference("Another", testReferenceLastSpace);
        tree.getNode(new DocumentReference("WebHome", testReferenceLastSpace)).select();
        linkPickerModal.select();
        linkDialog.submit();
        // Verify that the content matches what we did using CKEditor.
        assertSourceEquals("[[type the link label>>doc:Foo.Bar.Buz.Test]]\n"
            + "\n"
            + "[[type the link label>>doc:.Fa\\.Fi\\.Foo.WebHome]]\n"
            + "\n"
            + "[[type the link label>>doc:.SubPage.Another.WebHome]]");
    }
}

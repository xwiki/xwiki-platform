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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoAlertPresentException;
import org.xwiki.ckeditor.test.po.CKEditor;
import org.xwiki.ckeditor.test.po.CKEditorDialog;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.RichTextAreaElement;
import org.xwiki.ckeditor.test.po.image.ImageDialogSelectModal;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;


/**
 * All functional tests for Quick Actions.
 *
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
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
public class QuickActionsIT 
{

    /**
     * Test string that will be inserted to check formatting.
     */
    private static final String TEST_TEXT = "Hello, world!";

    @BeforeAll
    void setUp(TestUtils setup)
    {
        // Run the tests as a normal user. We make the user advanced only to enable the Edit drop down menu.
        createAndLoginStandardUser(setup);
    }

    @BeforeEach
    void cleanUp(TestUtils setup, TestReference testReference)
    {
        setup.deletePage(testReference);
    }

    @AfterEach
    void exitEditMode(TestUtils setup, TestReference testReference)
    {
        if (setup.isInWYSIWYGEditMode() || setup.isInWikiEditMode()) {
            // We pass the action because we don't want to wait for view mode to be loaded.
            setup.gotoPage(testReference, "view");
            try {
                // Confirm the page leave (discard unsaved changes) if we are asked for.
                setup.getDriver().switchTo().alert().accept();
            } catch (NoAlertPresentException e) {
                // Do nothing.
            }
        }
    }

    private static void createAndLoginStandardUser(TestUtils setup)
    {
        setup.createUserAndLogin("alice", "pa$$word", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    @Test
    @Order(1)
    void heading1AndParagraph(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Switch to another style
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("hea");
        qa.waitForItemSelected("Heading 1");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<h1>" + TEST_TEXT + "<br></h1>"));

        // Switch back to paragraph
        textArea.sendKeys("/");
        qa = new AutocompleteDropdown();
        textArea.sendKeys("parag");
        qa.waitForItemSelected("Paragraph");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        Assertions.assertTrue(textArea.getContent().contains("<p>" + TEST_TEXT + "<br></p>"));
    }

    @Test
    @Order(2)
    void heading2(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("hea");
        qa.waitForItemSelected("Heading 1");
        textArea.sendKeys(Keys.DOWN);
        qa.waitForItemSelected("Heading 2");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<h2>" + TEST_TEXT + "<br></h2>"));
    }

    @Test
    @Order(3)
    void heading3(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("hea");
        qa.waitForItemSelected("Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN);
        qa.waitForItemSelected("Heading 3");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<h3>" + TEST_TEXT + "<br></h3>"));
    }

    @Test
    @Order(4)
    void bulletedList(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("bu");
        qa.waitForItemSelected("Bulleted List");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<ul><li>" + TEST_TEXT + "<br></li></ul>"));
    }

    @Test
    @Order(5)
    void numberedList(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("nu");
        qa.waitForItemSelected("Numbered List");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<ol><li>" + TEST_TEXT + "<br></li></ol>"));
    }

    @Test
    @Order(6)
    void table(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("tab");
        qa.waitForItemSelected("Table");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Click OK on the table insertion dialog
        new CKEditorDialog().submitDialog();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        textArea.getContent().contains("<tbody><tr><td>" + TEST_TEXT + "<br></td><td><br></td></tr>"
            + "<tr><td><br></td><td><br></td></tr>"
            + "<tr><td><br></td><td><br></td></tr></tbody></table>");
    }

    @Test
    @Order(7)
    void blockQuote(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("quo");
        qa.waitForItemSelected("Quote");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        Assertions.assertTrue(textArea.getContent().contains("<blockquote><p>" + TEST_TEXT + "<br></p></blockquote>"));
    }

    @Test
    @Order(8)
    void infoBox(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("inf");
        qa.waitForItemSelected("Info Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{info}}\n"
                + "Type your information message here.\n"
                + "{{/info}}"));
    }

    @Test
    @Order(9)
    void successBox(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("suc");
        qa.waitForItemSelected("Success Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{success}}\n"
                + "Type your success message here.\n"
                + "{{/success}}"));
    }
    @Test
    @Order(10)
    void warningBox(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("war");
        qa.waitForItemSelected("Warning Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{warning}}\n"
                + "Type your warning message here.\n"
                + "{{/warning}}"));
    }

    @Test
    @Order(11)
    void errorBox(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("err");
        qa.waitForItemSelected("Error Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{error}}\n"
                + "Type your error message here.\n"
                + "{{/error}}"));
    }

    @Test
    @Order(12)
    void divider(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("div");
        qa.waitForItemSelected("Divider");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        Assertions.assertTrue(textArea.getContent().contains("<hr>"));
    }

    @Test
    @Order(13)
    void link(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("lin");
        qa.waitForItemSelected("Link");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea.sendKeys("ali");
        AutocompleteDropdown link = new AutocompleteDropdown();
        link.waitForItemSelected("alice");
        Assertions.assertTrue(textArea.getText().contains("["));
    }

    @Test
    @Order(14)
    void image(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("im");
        qa.waitForItemSelected("Image");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        new ImageDialogSelectModal().waitUntilReady().clickCancel();
    }

    @Test
    @Order(15)
    void mention(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("men");
        qa.waitForItemSelected("Mention");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        AutocompleteDropdown mention = new AutocompleteDropdown();
        mention.waitForItemSelected("alice");
        Assertions.assertTrue(textArea.getText().contains("@"));
    }

    @Test
    @Order(16)
    void emoji(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("emo");
        qa.waitForItemSelected("Emoji");
        textArea.sendKeys(Keys.ENTER);

        AutocompleteDropdown emoji = new AutocompleteDropdown();
        emoji.waitForItemSelected("🛩");
        Assertions.assertTrue(textArea.getText().contains(":sm"));
    }

    @Test
    @Order(17)
    void include(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("inc");
        qa.waitForItemSelected("Include Page");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Empty form
        new MacroDialogEditModal().waitUntilReady().clickSubmit();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{include/}}"));
    }

    @Test
    @Order(18)
    void code(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("cod");
        qa.waitForItemSelected("Code Snippet");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Empty form
        new MacroDialogEditModal().waitUntilReady().clickSubmit();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{code language=\"none\"}}{{/code}}"));
    }

    @Test
    @Order(19)
    void toc(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("toc");
        qa.waitForItemSelected("Table of Contents");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        textArea.waitUntilContentEditable();

        WikiEditPage wikiEditPage = editPage.clickSaveAndView().editWiki();
        Assertions.assertTrue(wikiEditPage.getContent().contains("{{toc/}}"));
    }

    @Test
    @Order(20)
    void find(TestUtils setup, TestReference testReference) throws Exception
    {
        WYSIWYGEditPage editPage = setup.gotoPage(testReference).editWYSIWYG();
        CKEditor editor = new CKEditor("content").waitToLoad();

        RichTextAreaElement textArea = editor.getRichTextArea();

        // Run the action
        textArea.sendKeys("/");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        textArea.sendKeys("fin");
        qa.waitForItemSelected("Find and Replace");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Click close on the Find and Replace dialog
        new CKEditorDialog().cancelDialog();
    }
}

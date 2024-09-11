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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.ckeditor.test.po.AutocompleteDropdown;
import org.xwiki.ckeditor.test.po.CKEditorDialog;
import org.xwiki.ckeditor.test.po.MacroDialogEditModal;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.WYSIWYGEditPage;

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
class QuickActionsIT extends AbstractCKEditorIT
{
    /**
     * Test string that will be inserted to check formatting.
     */
    private static final String TEST_TEXT = "Hello, world!";

    private WYSIWYGEditPage editPage;

    @BeforeAll
    void beforeAll(TestUtils setup, TestConfiguration testConfiguration) throws Exception
    {
        // Wait for Solr indexing to complete as the link search is based on Solr indexation.
        setup.loginAsSuperAdmin();
        setup.setWikiPreference("iconTheme",  "IconThemes.Silk");
        waitForSolrIndexing(setup, testConfiguration);

        createAndLoginStandardUser(setup);
    }

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        this.editPage = edit(setup, testReference);
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
    }

    @Test
    @Order(1)
    void heading1AndParagraph()
    {
        // Switch to another style
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("= " + TEST_TEXT + " =");

        // The rich text area is recreated when switching back to WYSIWYG mode.
        textArea = editor.getRichTextArea();

        // Switch back to paragraph
        textArea.sendKeys("/parag");
        qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/parag", "Paragraph");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        assertSourceEquals(TEST_TEXT);
    }

    @Test
    @Order(2)
    void heading2()
    {
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.DOWN);
        qa.waitForItemSelected("/hea", "Heading 2");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("== " + TEST_TEXT + " ==");
    }

    @Test
    @Order(3)
    void heading3()
    {
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN);
        qa.waitForItemSelected("/hea", "Heading 3");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("=== " + TEST_TEXT + " ===");
    }

    @Test
    @Order(4)
    void heading4()
    {
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN, Keys.DOWN);
        qa.waitForItemSelected("/hea", "Heading 4");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("==== " + TEST_TEXT + " ====");
    }
    
    @Test
    @Order(5)
    void heading5()
    {
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN, Keys.DOWN, Keys.DOWN);
        qa.waitForItemSelected("/hea", "Heading 5");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("===== " + TEST_TEXT + " =====");
    }

    @Test
    @Order(6)
    void heading6()
    {
        textArea.sendKeys("/hea");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/hea", "Heading 1");
        textArea.sendKeys(Keys.DOWN, Keys.DOWN, Keys.DOWN, Keys.DOWN, Keys.DOWN);
        qa.waitForItemSelected("/hea", "Heading 6");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("====== " + TEST_TEXT + " ======");
    }
    
    @Test
    @Order(7)
    void bulletedList()
    {
        textArea.sendKeys("/bu");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/bu", "Bulleted List");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("* " + TEST_TEXT);
    }

    @Test
    @Order(8)
    void numberedList()
    {
        textArea.sendKeys("/nu");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/nu", "Numbered List");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("1. " + TEST_TEXT);
    }

    @Test
    @Order(9)
    void table()
    {
        textArea.sendKeys("/tab");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/tab", "Table");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Click OK on the table insertion dialog
        new CKEditorDialog().submit();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals("|" + TEST_TEXT + "| \n| | \n| | \n\n ");
    }

    @Test
    @Order(10)
    void blockQuote()
    {
        textArea.sendKeys("/quo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/quo", "Quote");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Write some text
        textArea.sendKeys(TEST_TEXT);

        assertSourceEquals(">" + TEST_TEXT);
    }

    @Test
    @Order(11)
    void infoBox()
    {
        textArea.sendKeys("/inf");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/inf", "Info Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        // Delete the default message text.
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        textArea.sendKeys("my info message");

        assertSourceEquals("{{info}}\nmy info message\n{{/info}}");
    }

    @Test
    @Order(12)
    void successBox()
    {
        textArea.sendKeys("/suc");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/suc", "Success Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        // Delete the default message text.
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        textArea.sendKeys("my success message");

        assertSourceEquals("{{success}}\nmy success message\n{{/success}}");
    }

    @Test
    @Order(13)
    void warningBox()
    {
        textArea.sendKeys("/war");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/war", "Warning Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        // Delete the default message text.
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        textArea.sendKeys("my warning message");

        assertSourceEquals("{{warning}}\nmy warning message\n{{/warning}}");
    }

    @Test
    @Order(14)
    void errorBox()
    {
        textArea.sendKeys("/err");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/err", "Error Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();
        // Delete the default message text.
        textArea.sendKeys(Keys.chord(Keys.SHIFT, Keys.END), Keys.BACK_SPACE);
        textArea.sendKeys("my error message");

        assertSourceEquals("{{error}}\nmy error message\n{{/error}}");
    }

    @Test
    @Order(15)
    void divider()
    {
        textArea.sendKeys("/div");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/div", "Divider");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        assertSourceEquals("----\n\n ");
    }

    @Test
    @Order(16)
    void link()
    {
        textArea.sendKeys("/lin");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/lin", "Link");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        AutocompleteDropdown link = new AutocompleteDropdown().waitForItemSelected("[", "Upload Attachment");
        textArea.sendKeys("ali");
        link.waitForItemSelected("[ali", "alice");
        textArea.sendKeys(Keys.ENTER);
        link.waitForItemSubmitted();

        assertSourceEquals("[[alice>>XWiki.alice]] ");
    }

    @Test
    @Order(17)
    void image()
    {
        textArea.sendKeys("/im");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/im", "Image");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();
        
        // Wait for the image insertion dropdown to show
        new AutocompleteDropdown();
    }

    @Test
    @Order(18)
    void mention()
    {
        textArea.sendKeys("/men");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/men", "Mention");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        AutocompleteDropdown mention = new AutocompleteDropdown();
        textArea.sendKeys("al");
        mention.waitForItemSelected("@al", "alice");
        textArea.sendKeys(Keys.ENTER);
        mention.waitForItemSubmitted();
        // The mention is inserted as a macro, which means the rich text area is made read-only while the entire content
        // is re-rendered (refreshed).
        textArea.waitUntilContentEditable();

        assertSourceContains("{{mention reference=\"XWiki.alice\"");
    }

    @Test
    @Order(19)
    void emoji()
    {
        textArea.sendKeys("/emo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/emo", "Emoji");
        textArea.sendKeys(Keys.ENTER);

        AutocompleteDropdown emoji = new AutocompleteDropdown().waitForItemSelected(":sm", "🛩");
        textArea.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, "cat");
        emoji.waitForItemSelected(":cat", "🐈");
        textArea.sendKeys(Keys.ENTER);

        assertSourceEquals("🐈");
    }

    @Test
    @Order(20)
    void include()
    {
        textArea.sendKeys("/inc");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/inc", "Include Page");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Empty form
        new MacroDialogEditModal().waitUntilReady().clickSubmit();

        textArea = editor.getRichTextArea();

        assertSourceEquals("{{include/}}");
    }

    @Test
    @Order(21)
    void code()
    {
        textArea.sendKeys("/cod");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/cod", "Code Snippet");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Empty form
        new MacroDialogEditModal().waitUntilReady().clickSubmit();

        textArea = editor.getRichTextArea();

        assertSourceEquals("{{code language=\"none\"}}{{/code}}");
    }

    @Test
    @Order(22)
    void toc()
    {
        textArea.sendKeys("/toc");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/toc", "Table of Contents");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        textArea = editor.getRichTextArea();

        assertSourceEquals("{{toc/}}\n");
    }

    @Test
    @Order(23)
    void find()
    {
        textArea.sendKeys("/fin");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/fin", "Find and Replace");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Click close on the Find and Replace dialog
        new CKEditorDialog().cancel();
    }
    
    @Test
    @Order(24)
    void emojiClickTriggersDropDown(TestUtils setup)
    {
        textArea.sendKeys("/emo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/emo", "Emoji");

        // Click on the emoji Quick Action (instead of pressing Enter).
        qa.getSelectedItem().click();
        // The previous click leaves the mouse near the caret (where we type) and thus when the Emoji dropdown is shown
        // the mouse may hover one of the suggested Emojis, changing the default selection. We need to move the mouse
        // away so that we can verify the default selection.
        setup.getDriver().createActions().moveToElement(this.editPage.getSaveAndViewButton()).perform();

        AutocompleteDropdown emoji = new AutocompleteDropdown();
        textArea.sendKeys(Keys.BACK_SPACE, Keys.BACK_SPACE, "cat");
        emoji.waitForItemSelected(":cat", "🐈");
        textArea.sendKeys(Keys.ENTER);

        assertSourceEquals("🐈");
    }

    @Test
    @Order(25)
    void icon()
    {
        textArea.sendKeys("/icon");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/icon", "Icon");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // Search and insert the wiki icon.
        textArea.sendKeys("wiki");
        AutocompleteDropdown icon = new AutocompleteDropdown().waitForItemSelected("icon::wiki", "wiki");
        textArea.sendKeys(Keys.ENTER);
        icon.waitForItemSubmitted();

        // We wait for the editor to update because the icon quick action is using a macro.
        textArea = editor.getRichTextArea();

        assertSourceEquals("{{displayIcon name=\"wiki\"/}} ");
    }

    @Test
    @Order(26)
    void infoBoxInline()
    {
        // Type some text in a paragraph.
        textArea.sendKeys("before after");
        // Place the caret between the typed words.
        textArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT, Keys.LEFT, Keys.RIGHT));
        textArea.sendKeys(" /inf");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/inf", "Info Box");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // The content is reloaded after a macro is inserted.
        textArea = editor.getRichTextArea();

        // Note that we didn't modify the default info message because inline macros are not editable in-place.
        assertSourceEquals("before {{info}}Type your information message here.{{/info}} after");
    }

    @Test
    @Order(27)
    void velocityInline()
    {
        // Type some text in a paragraph.
        textArea.sendKeys("one two");
        // Place the caret between the typed words.
        textArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.LEFT, Keys.LEFT, Keys.RIGHT));
        textArea.sendKeys(" /velo");
        AutocompleteDropdown qa = new AutocompleteDropdown();
        qa.waitForItemSelected("/velo", "Velocity");
        textArea.sendKeys(Keys.ENTER);
        qa.waitForItemSubmitted();

        // The content is reloaded after a macro is inserted.
        textArea = editor.getRichTextArea();

        // Note that we didn't modify the default info message because inline macros are not editable in-place.
        assertSourceEquals("one {{velocity}} {{/velocity}} two");
    }
}

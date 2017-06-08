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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.openqa.selenium.Keys;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import static org.junit.Assert.*;

public class StandardFeaturesTest extends AbstractWysiwygTestCase
{
    @Test
    public void testEmptyWysiwyg()
    {
        switchToSource();
        assertSourceText("");
    }

    @Test
    public void testTypingAndDeletion()
    {
        String text = "az";
        typeText(text);
        assertContent(text);
        typeBackspace(text.length());
        testEmptyWysiwyg();
    }

    @Test
    public void testBold()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickBoldButton();
        assertContent("<h5><strong>x</strong></h5>");
    }

    @Test
    public void testItalics()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickItalicsButton();
        assertContent("<h5><em>x</em></h5>");
    }

    @Test
    public void testUnderline()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickUnderlineButton();
        assertContent("<h5><ins>x</ins></h5>");
    }

    @Test
    public void testStrikethrough()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickStrikethroughButton();
        assertContent("<h5><del>x</del></h5>");
    }

    @Test
    public void testSubscript()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickSubscriptButton();
        assertContent("<h5><sub>x</sub></h5>");
    }

    @Test
    public void testSuperscript()
    {
        typeText("x");
        applyStyleTitle5();
        selectAllContent();
        clickSuperscriptButton();
        assertContent("<h5><sup>x</sup></h5>");
    }

    @Test
    public void testUnorderedList()
    {
        // Create a list with 3 items
        typeTextThenEnter("a");
        typeTextThenEnter("b");
        // We press Enter here to be sure there's no bogus BR after the typed text.
        typeTextThenEnter("c");
        // Delete the empty line which was created only to avoid bogus BRs. See XWIKI-2732.
        typeBackspace();
        selectAllContent();
        clickUnorderedListButton();
        assertContent("<ul><li>a</li><li>b</li><li>c</li></ul>");

        // Undo
        clickUnorderedListButton();
        assertContent("a<br>b<br>c");

        // Create a list with 1 item and delete it
        resetContent();
        typeText("a");
        selectAllContent();
        clickUnorderedListButton();
        typeBackspace(2);
        testEmptyWysiwyg();
    }

    @Test
    public void testOrderedList()
    {
        // Create a list with 3 items
        typeTextThenEnter("a");
        typeTextThenEnter("b");
        // We press Enter here to be sure there's no bogus BR after the typed text.
        typeTextThenEnter("c");
        // Delete the empty line which was created only to avoid bogus BRs. See XWIKI-2732.
        typeBackspace();
        selectAllContent();
        clickOrderedListButton();
        assertContent("<ol><li>a</li><li>b</li><li>c</li></ol>");

        // Undo
        clickOrderedListButton();
        assertContent("a<br>b<br>c");

        // Create a list with 1 item and delete it
        resetContent();
        typeText("a");
        selectAllContent();
        clickOrderedListButton();
        typeBackspace(2);
        testEmptyWysiwyg();
    }

    @Test
    public void testStyle()
    {
        typeText("x");
        selectAllContent();

        applyStyleTitle1();
        assertContent("<h1>x</h1>");

        applyStyleTitle2();
        assertContent("<h2>x</h2>");

        applyStyleTitle3();
        assertContent("<h3>x</h3>");

        applyStyleTitle4();
        assertContent("<h4>x</h4>");

        applyStyleTitle5();
        assertContent("<h5>x</h5>");

        applyStyleTitle6();
        assertContent("<h6>x</h6>");

        applyStylePlainText();
        assertContent("<p>x</p>");
    }

    /**
     * @see XWIKI-2949: A separator (HR) inserted at the beginning of a document is badly displayed and difficult to
     *      remove
     */
    @Test
    public void testHR()
    {
        clickHRButton();
        // Create a heading and then delete it just to remove the bogus BR at the end.
        applyStyleTitle1();
        typeBackspace();
        // We don't switch to source because we want to see if the Backspace works.
        assertContent("<hr>");

        // Strange but we need to type Backspace twice although there's nothing else besides the horizontal ruler.
        typeBackspace(2);
        testEmptyWysiwyg();
        switchToWysiwyg();

        typeText("az");
        applyStyleTitle1();
        // Type Enter then Backspace to remove the bogus BR at the end.
        typeEnter();
        typeBackspace();
        // Since the left arrow key doesn't move the caret we have to use the Range API instead.
        moveCaret("document.body.firstChild.firstChild", 1);
        clickHRButton();
        assertContent("<h1>a</h1><hr><h1>z</h1>");
    }

    /**
     * @see XWIKI-3012: Exception when opening a WYSIWYG dialog in FF2.0
     * @see XWIKI-2992: Place the caret after the inserted symbol
     * @see XWIKI-3682: Trademark symbol is not displayed correctly.
     */
    @Test
    public void testInsertSymbol()
    {
        clickSymbolButton();
        getSelenium().click("//div[@title='copyright sign']");
        clickSymbolButton();
        closeDialog();
        clickSymbolButton();
        getSelenium().click("//div[@title='registered sign']");
        clickSymbolButton();
        getSelenium().click("//div[@title='trade mark sign']");
        switchToSource();
        assertSourceText("\u00A9\u00AE\u2122");
    }

    /**
     * The rich text area should remain focused and the text shouldn't be changed.
     * 
     * @see XWIKI-3043: Prevent tab from moving focus from the new WYSIWYG editor
     */
    @Test
    public void testTabDefault()
    {
        typeText("a");
        typeTab();
        typeText("b");
        typeShiftTab();
        typeText("c");
        assertContent("a&nbsp;&nbsp;&nbsp; bc");
    }

    /**
     * The list item should be indented or outdented depending on the Shift key.
     * 
     * @see XWIKI-3043: Prevent tab from moving focus from the new WYSIWYG editor
     */
    @Test
    public void testTabInListItem()
    {
        typeText("x");
        typeShiftEnter();
        // "y" (lower case only) is misinterpreted.
        // See http://jira.openqa.org/browse/SIDE-309
        // See http://jira.openqa.org/browse/SRC-385
        typeText("Y");
        selectAllContent();
        clickUnorderedListButton();
        // Since the left arrow key doesn't move the caret we have to use the Range API instead.
        moveCaret("document.body.firstChild.childNodes[1].firstChild", 0);
        typeTab();
        assertContent("<ul><li>x<ul><li>Y</li></ul></li></ul>");
        typeShiftTab();
        assertContent("<ul><li>x</li><li>Y</li></ul>");
    }

    /**
     * @see XWIKI-2735: Clicking on the space between two lines hides the cursor
     */
    @Test
    public void testEmptyLinesAreEditable()
    {
        switchToSource();
        setSourceText("a\n\n\n\nb");
        switchToWysiwyg();
        // TODO: Since neither the down arrow key nor the click doesn't seem to move the caret we have to find another
        // way of placing the caret on the empty lines, without using the Range API.
        // For the moment let's just test if the empty lines contain a BR.
        assertContent("<p>a</p><p><br></p><p><br></p><p>b</p>");
    }

    /**
     * @see XWIKI-2732: Unwanted BR tags
     */
    @Test
    public void testUnwantedBRsAreRemoved()
    {
        typeText("a");
        typeShiftEnter();
        typeText("b");
        typeShiftEnter();
        switchToSource();
        assertSourceText("a\nb\\\\");
    }

    /**
     * @see XWIKI-3138: WYSIWYG 2.0 Preview Error
     */
    @Test
    public void testPreview()
    {
        typeText("x");
        selectAllContent();
        clickBoldButton();
        clickEditPreview();
        clickBackToEdit();
        switchToSource();
        assertSourceText("**x**");
    }

    /**
     * @see XWIKI-2993: Insert horizontal line on a selection of unordered list.
     */
    @Test
    public void testInsertHRInPlaceOfASelectedList()
    {
        typeTextThenEnter("a");
        typeText("z");
        selectAllContent();
        clickUnorderedListButton();
        clickHRButton();
        switchToSource();
        assertSourceText("----");
    }

    /**
     * @see XWIKI-3053: When a HR is inserted at the beginning of a paragraph an extra empty paragraph is generated
     *      before that HR
     */
    @Test
    public void testInsertHRInsideParagraph()
    {
        // "y" (lower case only) is misinterpreted.
        // See http://jira.openqa.org/browse/SIDE-309
        // See http://jira.openqa.org/browse/SRC-385
        typeText("xY");
        applyStyleTitle1();
        applyStylePlainText();

        // Insert HR at the end of the paragraph.
        clickHRButton();

        // Move the caret between x and Y.
        moveCaret("document.body.firstChild.firstChild", 1);

        // Insert HR in the middle of the paragraph.
        clickHRButton();

        // Move the caret before x.
        moveCaret("document.body.firstChild.firstChild", 0);

        // Insert HR at the beginning of the paragraph.
        clickHRButton();

        // We have to assert the HTML content because the arrow keys don't move the caret so we can't test if the user
        // can edit the generated empty paragraphs. The fact that they contain a BR proves this.
        assertContent("<p><br></p><hr><p>x</p><hr><p>Y</p><hr><p><br></p>");
    }

    /**
     * @see XWIKI-3191: New lines at the end of list items are not preserved by the wysiwyg
     */
    @Test
    public void testNewLinesAtTheEndOfListItemsArePreserved()
    {
        String sourceText = "* \\\\\n** \\\\\n*** test1";
        switchToSource();
        setSourceText(sourceText);
        switchToWysiwyg();
        switchToSource();
        assertSourceText(sourceText);
    }

    /**
     * @see XWIKI-3194: Cannot remove just one text style when using the style attribute instead of formatting tags
     */
    @Test
    public void testRemoveBoldStyleWhenTheStyleAttributeIsUsed()
    {
        switchToSource();
        setSourceText("hello (% style=\"font-family: monospace; font-weight: bold;\" %)vincent(%%) world");
        switchToWysiwyg();

        // Select the word in bold.
        selectNodeContents("document.body.firstChild.childNodes[1]");
        waitForBoldDetected(true);

        // Remove the bold style.
        clickBoldButton();
        waitForBoldDetected(false);

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("hello (% style=\"font-family: monospace; font-weight: normal;\" %)vincent(%%) world");
    }

    /**
     * @see XWIKI-2997: Cannot un-bold a text with style Title 1
     */
    @Test
    public void testRemoveBoldStyleWithinHeading()
    {
        // Insert a heading and make sure it has bold style.
        switchToSource();
        setSourceText("(% style=\"font-weight: bold;\" %)\n= Title 1 =");
        switchToWysiwyg();

        // Select a part of the heading.
        select("document.body.firstChild.firstChild.firstChild", 3, "document.body.firstChild.firstChild.firstChild", 5);
        waitForBoldDetected(true);

        // Remove the bold style.
        clickBoldButton();
        waitForBoldDetected(false);

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("(% style=\"font-weight: bold;\" %)\n= Tit(% style=\"font-weight: normal;\" %)le(%%) 1 =");
    }

    /**
     * @see XWIKI-3111: A link to an email address can be removed by removing the underline style
     */
    @Test
    public void testRemoveUnderlineStyleFromALink()
    {
        // Insert a link to an email address.
        switchToSource();
        // Links are not underlined anymore in Colibri skin so we apply the underline style to the link label.
        setSourceText("[[__foo__>>mailto:x@y.z||title=\"bar\"]]");
        switchToWysiwyg();

        // Select the text of the link.
        selectNode("document.body.getElementsByTagName('a')[0]");
        waitForUnderlineDetected(true);

        // Remove the underline style.
        clickUnderlineButton();
        waitForUnderlineDetected(false);

        // Check the XWiki syntax.
        switchToSource();
        assertSourceText("[[foo>>mailto:x@y.z||title=\"bar\"]]");
    }

    /**
     * Tests if the state of the tool bar buttons is updated immediately after the editor finished loading.
     */
    @Test
    public void testToolBarIsUpdatedOnLoad()
    {
        clickEditPageInWikiSyntaxEditor();
        setFieldValue("content", "**__abc__**");
        clickEditPageInWysiwyg();
        waitForEditorToLoad();
        waitForBoldDetected(true);
        waitForUnderlineDetected(true);

        switchToSource();
        setSourceText("**abc**");
        switchToWysiwyg();
        waitForBoldDetected(true);
        waitForUnderlineDetected(false);
    }

    /**
     * @see XWIKI-2669: New WYSIWYG editor doesn't work when special characters are entered by the user.
     */
    @Test
    public void testHTMLSpecialChars()
    {
        typeText("<\"'&#'\">");
        switchToSource();
        assertSourceText("<\"'&#'\">");

        // Change the source to force a conversion.
        setSourceText("<'\"&#\"'>");
        switchToWysiwyg();
        assertContent("<p>&lt;'\"&amp;#\"'&gt;</p>");

        applyStyleTitle1();
        switchToSource();
        assertSourceText("= <'\"&#\"'> =");
    }

    /**
     * @see XWIKI-4033: When saving after section edit entire page is overwritten.
     */
    @Test
    public void testSectionEditing()
    {
        // Save the current location to be able to get back to it later.
        String location = getSelenium().getLocation();

        // Create two sections.
        switchToSource();
        setSourceText("= s1 =\n\nabc\n\n= s2 =\n\nxyz");
        clickEditSaveAndView();

        // Edit the second section.
        open(location + (location.indexOf('?') < 0 ? "?" : "") + "&section=2");
        waitForEditorToLoad();
        typeDelete(2);
        typeText("c2");
        switchToSource();
        assertSourceText("= c2 =\n\nxyz");
        clickEditSaveAndView();

        // Check the content of the page.
        open(location);
        waitForEditorToLoad();
        switchToSource();
        assertSourceText("= s1 =\n\nabc\n\n= c2 =\n\nxyz");
    }

    /**
     * @see XWIKI-4335: Typing ">" + text in wysiwyg returns a quote
     */
    @Test
    public void testQuoteSyntaxIsEscaped()
    {
        typeText("> 1");
        switchToSource();
        switchToWysiwyg();
        assertEquals("> 1", getRichTextArea().getText());
    }

    /**
     * @see XWIKI: Problems removing italics from a definition.
     */
    @Test
    public void testRemoveItalicsFromDefinition()
    {
        switchToSource();
        // Make sure the definitions are displayed with italics to avoid depending on the current skin.
        setSourceText("(% style=\"font-style: italic;\" %)\n(((\n; term1\n: definition1\n:; term2\n:: definition2\n)))");
        switchToWysiwyg();
        selectNodeContents("document.getElementsByTagName('dd')[0].firstChild");
        clickItalicsButton();
        switchToSource();
        assertSourceText("(% style=\"font-style: italic;\" %)\n(((\n; term1\n"
            + ": (% style=\"font-style: normal;\" %)definition1(%%)\n:; term2\n:: definition2\n)))");
    }

    /**
     * @see XWIKI-4364: Verbatim blocks suffer corruption when previewed using the GWT editor
     */
    @Test
    public void testMultiLineVerbatimBlock()
    {
        switchToSource();
        String multiLineVerbatimBlock = "{{{a verbatim block\nwhich is multiline}}}";
        setSourceText(multiLineVerbatimBlock);
        switchToWysiwyg();
        switchToSource();
        assertSourceText(multiLineVerbatimBlock);
    }

    /**
     * @see XWIKI-4399: ((( ))) looses class or style definitions when you edit in WYSIWYG.
     */
    @Test
    public void testGroupStyleIsPreserved()
    {
        switchToSource();
        String styledGroup = "(% class=\"abc\" style=\"color: red;\" %)\n(((\ntext\n)))";
        setSourceText(styledGroup);
        switchToWysiwyg();
        switchToSource();
        assertSourceText(styledGroup);
    }

    /**
     * @see XWIKI-4529: XWiki velocity variables are undefined when the edited content is rendered.
     */
    @Test
    public void testXWikiVarsAreDefined()
    {
        switchToSource();
        setSourceText("{{velocity}}#if($hasEdit)1#{else}2#end{{/velocity}}");
        switchToWysiwyg();
        assertEquals("1", getRichTextArea().getText());
    }

    /**
     * @see XWIKI-4665: Pressing Meta+G (Jump to page) in the WYSIWYG editor displays the popup inside the rich text
     *      area.
     */
    @Test
    public void testJavaScriptExtensionsAreNotIncludedInEditMode()
    {
        // Type some text to be sure the conversion is triggered when switching to source.
        typeText("x");
        // Type Ctrl+G to open the "Jump to page" dialog.
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "g"));
        // Switch to source and check the result.
        switchToSource();
        assertSourceText("x");
        // Now check that the "Jump to page" feature works indeed.
        String jumpToPageTitleXPath = "//div[@class = 'xdialog-title' and starts-with(., 'Go to:')]";
        assertElementNotPresent(jumpToPageTitleXPath);
        getSourceTextArea().sendKeys(Keys.chord(Keys.CONTROL, "g"));
        assertElementPresent(jumpToPageTitleXPath);

        // Ctrl+G is a shortcut for "Find again" on Firefox and it opens the find toolbar at the bottom of the page.
        // This toolbar can influence the way Selenium/WebDriver computes the position of the buttons on the page
        // leading to cases where clicking on a button has no action because the click is performed at a different
        // location or because Selenium thinks the location is outside of the page.
        // Close the "Jump to page" dialog, switch back to rich text area and close the find toolbar.
        getSourceTextArea().sendKeys(Keys.ESCAPE);
        switchToWysiwyg();
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "g"), Keys.ESCAPE);
    }

    /**
     * @see XWIKI-4346: Cannot use the WYSIWYG editor in small screen after you have deleted all text in full screen
     * @see XWIKI-6003: Entering and exiting fullscreen mode resets the scroll offset and the cursor position or the
     *      current selection
     */
    @Test
    public void testEditInFullScreen()
    {
        typeText("abc");
        // Select 'b' to check if the selection is preserved when we switch to full screen editing.
        select("document.body.firstChild", 1, "document.body.firstChild", 2);
        clickEditInFullScreen();
        typeText("1");
        clickExitFullScreen();
        typeText("2");
        switchToSource();
        assertSourceText("a12c");
    }

    /**
     * @see XWIKI-5036: WYSIWYG editor doesn't display when creating a document named #"&§-_\
     */
    @Test
    public void testEditPageWithSpecialSymbolsInName()
    {
        startCreatePage("Main", "#\"&\u00A7-_\\");
        waitForEditorToLoad();
        typeText("qzr");
        clickEditPreview();
        assertTextPresent("qzr");
    }

    /**
     * @see XWIKI-5250: Merge nested custom styles like font and color before save
     */
    @Test
    public void testNestedCustomInlineStyles()
    {
        setContent("<p>ab<span style=\"color: red\">cd<span style=\"background-color: yellow\">ef</span>gh</span>"
            + "ij<span style=\"font-family: monospace\">kl<span style=\"font-size: 24pt\">mn</span>op</span>rs</p>");
        switchToSource();
        assertEquals("ab(% style=\"color: red\" %)cd(% style=\"color: red; "
            + "background-color: yellow\" %)ef(% style=\"color: red\" %)gh(%%)"
            + "ij(% style=\"font-family: monospace\" %)kl(% style=\"font-family: monospace;"
            + " font-size: 24pt\" %)mn(% style=\"font-family: monospace\" %)op(%%)rs", getSourceText());
    }
}

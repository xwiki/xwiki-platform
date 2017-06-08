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

import java.util.Date;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import com.thoughtworks.selenium.Wait;

import static org.junit.Assert.*;

public class TabsTest extends AbstractWysiwygTestCase
{
    @Test
    public void testMultipleClicksOnTheSameTab()
    {
        setContent("<strong>foo</strong>");
        switchToWysiwyg(false);
        switchToWysiwyg();
        switchToSource();
        assertSourceText("**foo**");
    }

    /**
     * Tests that XWIKI-3834 remains fixed.
     */
    @Test
    public void testMultipleSwitches()
    {
        StringBuffer content = new StringBuffer();
        // We put quite a lot of content so that the conversion is not immediate.
        content.append("<strong>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor "
            + "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation "
            + "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in "
            + "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
            + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        content.append("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt "
            + "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco "
            + "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in "
            + "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
            + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        content.append("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt "
            + "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco "
            + "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in "
            + "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
            + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.");
        content.append("Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt "
            + "ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco "
            + "laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in "
            + "voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non "
            + "proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</strong>");
        setContent(content.toString());

        // We go back and forth multiple times to be sure that it is not a matter of chance.
        switchToSource(false);
        switchToWysiwyg(false);
        switchToSource(false);
        switchToWysiwyg(false);
        switchToSource();

        assertFalse(getSourceText().contains("strong"));
    }

    /**
     * @see XWIKI-4079: Links are lost when switching to Source in the WYSIWYG editor.
     */
    @Test
    public void testLinksAreNotLostWhenSwitchingToSourceTab()
    {
        String content = "Visit [[XWiki>>http://www.xwiki.org]] and our [[blog>>Blog.WebHome]].";
        switchToSource();
        setSourceText(content);
        switchToWysiwyg();
        switchToSource();
        assertSourceText(content);
    }

    /**
     * @see XWIKI-3965: Relative images are not displayed when switching from Source tab to Wysiwyg tab.
     */
    @Test
    public void testContextDocumentIsPreserved()
    {
        // Uploading an image to the current document is difficult. Instead we use a context sensitive velocity script.
        clickEditPageInWikiSyntaxEditor();
        setFieldValue("content", "{{velocity}}$doc.fullName{{/velocity}}");
        clickEditPageInWysiwyg();
        waitForEditorToLoad();
        String expected = getRichTextArea().getText();
        switchToSource();
        switchToWysiwyg();
        assertEquals(expected, getRichTextArea().getText());
    }

    /**
     * Switches to source tab while the rich text area is still loading. The source text must remain unchanged.
     */
    @Test
    public void testSwitchToSourceWhileWysiwygIsLoading()
    {
        switchToSource();
        StringBuffer sourceText = new StringBuffer();
        sourceText.append("{{code language=\"java\"}}\n");
        sourceText.append("public interface Command {\n");
        sourceText.append("  boolean execute(String parameter);\n");
        sourceText.append("}\n");
        sourceText.append("{{/code}}");
        setSourceText(sourceText.toString());
        // Set the cursor position before "language" to see if it is preserved.
        getSourceTextArea().sendKeys(Keys.HOME, Keys.PAGE_UP, Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT),
            Keys.ARROW_RIGHT);
        // Switch to WYSIWYG tab but don't wait for the rich text area to load.
        switchToWysiwyg(false);
        // Switch back to source tab.
        switchToSource();
        getSourceTextArea().sendKeys("x");
        // Check the source text. We don't assert the cursor position directly because it isn't available when the test
        // is run in background.
        assertSourceText(sourceText.substring(0, 7) + "x" + sourceText.substring(7));
    }

    /**
     * Switches to source tab and waits for the conversion. The cursor should be placed at the start.
     * 
     * @see XWIKI-4392: Place the caret at the beginning of the content when swtching to WYSIWYG Source editor.
     */
    @Test
    public void testSwitchToSourceWithHTMLChangesAndWait()
    {
        typeText("1");
        applyStyleTitle1();
        switchToSource();
        getSourceTextArea().sendKeys("x");
        assertSourceText("x= 1 =");
    }

    /**
     * Switches to source tab without changing the HTML. The source selection should be preserved.
     */
    @Test
    public void testSwitchToSourceWithoutHTMLChanges()
    {
        switchToSource();
        String sourceText = "one **two** three";
        setSourceText(sourceText);
        // Move the caret between the first two *.
        getSourceTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.ARROW_LEFT, Keys.ARROW_LEFT), Keys.ARROW_LEFT);
        switchToWysiwyg();
        // Switch back to source tab without changing the HTML.
        switchToSource();
        getSourceTextArea().sendKeys("x");
        // Check the source text.
        assertSourceText(sourceText.substring(0, 5) + "x" + sourceText.substring(5));
    }

    /**
     * Switches to source tab but doesn't wait for the result. Comes back to source tab when the result is received.
     */
    @Test
    public void testSwitchToSourceWithHTMLChangesAndDontWait()
    {
        setContent("<!--startmacro:code|-|language=\"java\"|-|public class Apple extends Fruit {\n"
            + "  public String getColor() {\n    return Colors.RED;\n  }\n}--><!--stopmacro-->");
        // Switch to source tab but don't wait for the conversion result.
        switchToSource(false);
        // Switch back to WYSIWYG tab.
        switchToWysiwyg();
        // Wait for the conversion result on the WYSIWYG tab.
        new Wait()
        {
            public boolean until()
            {
                return getSourceText().length() > 1;
            }
        }.wait("Conversion takes too long!");
        // Switch to source tab without waiting. The source text was already received.
        switchToSource(false);
        assertTrue(getSourceTextArea().isEnabled());
        getSourceTextArea().sendKeys("x");
        assertSourceText("x{{code language=\"java\"}}\npublic class Apple extends Fruit {\n"
            + "  public String getColor() {\n    return Colors.RED;\n  }\n}\n{{/code}}");
    }

    /**
     * Test if the editor switches back to WYSIWYG tab when the conversion from HTML to source text fails.
     */
    @Test
    public void testSwitchToSourceFailure()
    {
        // Put some bogus content in the rich text area. This content should cause a conversion exception.
        String content = "<span class=\"wikiexternallink\"><a href=\"mailto:x@y.z\">xyz</a></span><!--stopwikilink-->";
        setContent(content);
        // Try to switch to source. Don't wait because the plain text area will remain disabled.
        switchToSource(false);
        // The editor switches back to WYSIWYG tab if it catches a conversion exception.
        waitForElement("//div[@class = 'gwt-TabBarItem gwt-TabBarItem-selected']/div[. = 'WYSIWYG']");
        // The HTML mustn't change.
        assertContent(content);
        // Let's see if we can correct the mistake.
        setContent("<p>Should be fine now.</p>");
        switchToSource();
        assertSourceText("Should be fine now.");
    }

    /**
     * Tests if the switch to source action can be canceled.
     */
    @Test
    public void testCancelSwitchToSource()
    {
        // Put some content in the rich text area.
        setContent("<h1>Heading</h1><p>paragraph</p><ul><li>list</li></ul><table><tr><td>cell</td></tr></table>");
        // Place the caret inside the heading.
        moveCaret("document.body.firstChild.firstChild", 3);
        // Switch to source but don't wait till the conversion is done.
        switchToSource(false);
        // Switch back to rich text area, before receiving the source text.
        switchToWysiwyg();
        // Change the rich text.
        typeText("X");
        // Switch to source again, this time with a different rich text. Wait for the conversion to end.
        switchToSource();
        // Check the result.
        assertSourceText("= HeaXding =\n\nparagraph\n\n* list\n\n|cell");
    }

    /**
     * @see XWIKI-4517: NullPointerException thrown when switching to Source tab before the rich text area has finished
     *      loading
     */
    @Test
    public void testSwitchToSourceBeforeWysiwygLoad()
    {
        // Switch to source and put some content that takes time to render.
        switchToSource();
        String sourceText =
            "{{code}}long x = 1L;{{/code}}\n\n{{html}}" + new Date().getTime()
                + "{{/html}}\n\n{{velocity}}$doc.name{{/velocity}}";
        setSourceText(sourceText);
        clickEditSaveAndContinue();
        // Reload the page to be able to switch to source tab before the rich text area finishes loading.
        getSelenium().runScript("window.location.reload(true)");
        // Switch to source tab as soon as the source tab is available.
        waitForElement("//div[@class = 'gwt-TabBarItem']/div[. = 'Source']");
        switchToSource();
        // Verify the source text.
        assertSourceText(sourceText);
        // NOTE: If the WYSIWYG editor is loaded when the page loads then the source text area is unusable until after
        // the load event fires which happens after all the external resource including in-line frames are completely
        // loaded. So even though we can switch to source tab before the rich text area finishes loading, we might not
        // be able to use the plain text area. As a consequence we can't write a more elaborate test. The solution is to
        // load the editor after the host page finishes loading.
    }

    /**
     * Switches to WYSIWYG tab while the source text area is still loading (i.e. waiting for a HTML to source conversion
     * result). The rich text must remain unchanged.
     */
    @Test
    public void testSwitchToWysiwygWhileSourceIsLoading()
    {
        String content =
            "before <!--startmacro:code|-|language=\"java\"|-|\\npublic class Apple extends Fruit {\n"
                + "  public String getColor() {\n    return Colors.RED;\n  }\n}\n--><!--stopmacro--> after";
        setContent(content);
        // Select some text to see if the selection is preserved.
        select("document.body.firstChild", 3, "document.body.firstChild", 6);
        // Switch to source but don't wait for the conversion result.
        switchToSource(false);
        // Switch back to WYSIWYG tab before the conversion result is received.
        switchToWysiwyg();
        // Overwrite the selected text.
        typeText("#");
        // Check the result.
        assertContent(content.substring(0, 3) + "#" + content.substring(6));
    }

    /**
     * Switches to WYSIWYG tab without changing the source text. The DOM selection should be preserved.
     */
    @Test
    public void testSwitchToWysiwygWithoutSourceChanges()
    {
        setContent("<em>alice</em> and <strong>bob</strong>");
        select("document.body.firstChild.firstChild", 3, "document.body.lastChild.firstChild", 1);
        // Switch to source and wait for the conversion result.
        switchToSource();
        assertSourceText("//alice// and **bob**");
        // Switch back to WYSIWYG tab without modifying the source text.
        switchToWysiwyg();
        // Test is the content and the selection were preserved.
        pushToolBarButton("Clear Formatting");
        assertContent("<em>ali</em>ce and b<strong>ob</strong>");
    }

    /**
     * Switches to WYSIWYG tab and waits for the conversion. The caret should be placed at the start.
     */
    @Test
    public void testSwitchToWysiwygWithSourceChangesAndWait()
    {
        switchToSource();
        setSourceText("**X**Wiki");
        switchToWysiwyg();
        typeText("+");
        assertContent("<p><strong>+X</strong>Wiki</p>");
    }

    /**
     * Switches to WYSIWYG tab but doesn't wait for the rich text area to finish loading. Comes back to WYSIWYG tab when
     * the rich text area is loaded.
     */
    @Test
    public void testSwitchToWysiwygWithHTMLChangesAndDontWait()
    {
        switchToSource();
        String sourceText = "before {{code language=\"java\"}}private static final long x = 1L;{{/code}}";
        setSourceText(sourceText);
        // Switch to WYSIWYG tab but don't wait for the rich text area to load.
        switchToWysiwyg(false);
        // Switch back to source tab and wait there for the rich text area to load.
        switchToSource();
        getDriver().waitUntilElementDisappears(By.xpath("//div[@class = 'xRichTextEditor']//div[@class = 'loading']"));
        // Switch again to WYSIWYG tab to check the result.
        switchToWysiwyg();
        typeText("#");
        switchToSource();
        assertSourceText("#" + sourceText);
    }

    /**
     * Tests if the switch to WYSIWYG tab action can be canceled.
     */
    @Test
    public void testCancelSwitchToWysiwyg()
    {
        // Switch to source tab and insert some content that takes time to render. A code macro is perfect for this.
        switchToSource();
        StringBuilder sourceText = new StringBuilder();
        sourceText.append("{{code language=\"java\"}}\n");
        sourceText.append("public final class Apple extends Fruit {\n");
        sourceText.append("  public String getColor() {\n");
        sourceText.append("    return \"red\";\n");
        sourceText.append("  }\n");
        sourceText.append("}\n");
        sourceText.append("{{/code}}");
        setSourceText(sourceText.toString());
        // Place the caret before "Apple".
        getSourceTextArea().sendKeys(Keys.HOME, Keys.PAGE_UP, Keys.ARROW_DOWN,
            Keys.chord(Keys.CONTROL, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT), Keys.ARROW_RIGHT);
        // Switch to rich text but don't wait till the rich text area finishes loading.
        switchToWysiwyg(false);
        // Switch back to source before the rich text area is reloaded.
        switchToSource();
        // Change the content.
        getSourceTextArea().sendKeys("X");
        // Switch to WYSIWYG tab again, this time with a different source text. Wait for the rich text area to load.
        switchToWysiwyg();
        // Check the result.
        switchToSource();
        getSourceTextArea().sendKeys("Y");
        assertSourceText(sourceText.substring(0, 44) + "XY" + sourceText.substring(44));
    }

    /**
     * Switches to source tab, changes the source text then switches back to WYSIWYG tab and undoes the change.
     */
    @Test
    public void testUndoSourceChange()
    {
        typeText("1");
        applyStyleTitle1();
        switchToSource();
        // Change the source text.
        getSourceTextArea().sendKeys(Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, Keys.ARROW_RIGHT, "2");
        // Switch to WYSIWYG tab and undo the change.
        switchToWysiwyg();
        // The tool bar is not updated right away. We have to wait for the undo push button to become enabled.
        waitForPushButton(TOOLBAR_BUTTON_UNDO_TITLE, true);
        clickUndoButton();
        // Check the result.
        switchToSource();
        // NOTE: This is not the right result since the heading style was removed also. This needs to be fixed.
        assertSourceText("1");
    }
}

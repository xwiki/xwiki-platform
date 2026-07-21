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
package org.xwiki.blocknote.test.po;

import org.jspecify.annotations.NonNull;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.wysiwyg.test.po.MacroDialogEditModal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfNestedElementsLocatedBy;

/**
 * Represents the BlockNote rich text area.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
public class BlockNoteRichTextArea extends BaseElement
{
    @NonNull
    private WebElement container;

    /**
     * Create a new instance that can be used to interact with the specified BlockNote rich text area instance.
     *
     * @param container the rich text area container element
     */
    public BlockNoteRichTextArea(@NonNull WebElement container)
    {
        this.container = container;
        waitUntilContentEditable();
    }

    /**
     * Waits for the rich text area to be editable.
     *
     * @return this rich text area instance
     */
    public BlockNoteRichTextArea waitUntilContentEditable()
    {
        getDriver().waitUntilCondition(ExpectedConditions.domAttributeToBe(this.container, "contenteditable", "true"));
        return this;
    }

    /**
     * @return the inner text of the rich text area
     */
    public String getText()
    {
        return this.container.getText();
    }

    /**
     * @return the HTML content of the rich text area
     * @since 18.4.0RC1
     */
    public String getContent()
    {
        return this.container.getDomProperty("innerHTML");
    }

    /**
     * Clears the content of the rich text area.
     *
     * @return this rich text area instance
     */
    public BlockNoteRichTextArea clear()
    {
        this.container.clear();
        return this;
    }

    /**
     * Clicks on the rich text area.
     *
     * @return this rich text area instance
     */
    public BlockNoteRichTextArea click()
    {
        // Click on the top left corner of the rich text area to place the caret at the beginning of the content.
        getDriver().createActions().moveToElement(this.container, 0, 0).click().perform();
        return this;
    }

    /**
     * Simulate typing in the rich text area.
     * 
     * @param keysToSend the sequence of keys to by typed
     * @return this rich text area instance
     */
    public BlockNoteRichTextArea sendKeys(CharSequence... keysToSend)
    {
        if (keysToSend.length > 0) {
            getActiveElement().sendKeys(keysToSend);
        }
        return this;
    }

    /**
     * Appends a new paragraph at the end of the content and types the given keys in it.
     * <p>
     * Since BlockNote 0.51 the trailing block (the empty area at the bottom of the editor) is no longer a real,
     * editable paragraph but a non-editable decoration widget that inserts a new paragraph (and places the caret in it)
     * only when clicked. As a consequence we can't simply move the caret to the end of the content (e.g. with
     * {@code PAGE_DOWN}) and type, because the typed text would have no editable target and would be dropped. We have
     * to click the trailing block first to materialize a real paragraph.
     * <p>
     * An alternative way to insert an end-of-content new line is to move the caret to the end of the last line, then
     * press {@code ENTER}.
     *
     * @param keysToSend the sequence of keys to type in the new paragraph
     * @return this blocknote rich text area instance
     * @since 18.6.0RC1
     */
    public BlockNoteRichTextArea appendParagraph(CharSequence... keysToSend)
    {
        By trailingBlock = By.cssSelector(".bn-trailing-block");
        getDriver().waitUntilCondition(visibilityOfNestedElementsLocatedBy(this.container, trailingBlock));
        getDriver().createActions().click(this.container.findElement(trailingBlock)).perform();
        return sendKeys(keysToSend);
    }

    /**
     * @return the element that has the focus in the rich text area
     */
    private WebElement getActiveElement()
    {
        // Chrome expects us to send the keys directly to the nested editable where we want to type. If we send the
        // keys to the root editable then they are inserted directly in the root editable (e.g. when editing a macro
        // inline the characters we type will be inserted outside the macro content nested editable).
        WebElement activeElement = getDriver().switchTo().activeElement();
        boolean rootEditableElementIsOrContainsActiveElement = (boolean) getDriver()
            .executeScript("return arguments[0].contains(arguments[1])", this.container, activeElement);
        if (!rootEditableElementIsOrContainsActiveElement) {
            activeElement = this.container;
        }
        // Firefox ignores the sent keys if the target element is not focused.
        focus(activeElement);
        return activeElement;
    }

    private void focus(WebElement element)
    {
        getDriver().executeScript("arguments[0].focus()", element);
    }

    /**
     * Clicks on the image with the specified index in the rich text area.
     *
     * @param index the index of the image to click, starting from 0
     * @return this rich text area instance
     * @since 18.3.0RC1
     */
    public BlockNoteRichTextArea clickImage(int index)
    {
        // The image might not be loaded yet, so wait until it's clickable before clicking on it.
        WebElement image = getImage(index);
        getDriver().waitUntilCondition(ExpectedConditions.elementToBeClickable(image));
        image.click();
        return this;
    }

    /**
     * Returns the image with the specified index in the rich text area.
     *
     * @param index the index of the image to return, starting from 0
     * @return the image element
     * @since 18.6.0
     */
    public WebElement getImage(int index)
    {
        return this.container.findElements(By.tagName("img")).get(index);
    }

    /**
     * Double clicks on the macro with the specified index in the rich text area to open the macro edit modal.
     * 
     * @param index the index of the macro to double click, starting from 0
     * @return the opened macro edit modal
     * @since 18.3.0RC1
     */
    public MacroDialogEditModal doubleClickMacro(int index)
    {
        // The double click event listener is registered on the macro output wrapper which is the first child of the
        // block content.
        WebElement macro = this.container.findElements(By.cssSelector("""
            .bn-block-content[data-content-type="xwikiMacroBlock"] > :first-child,
            .bn-inline-content-section[data-inline-content-type="xwikiInlineMacro"] > :first-child"""))
            .get(index);
        getDriver().createActions().doubleClick(macro).perform();
        return new MacroDialogEditModal().waitUntilReady();
    }

    /**
     * Waits until the rich text area contains the specified plain text.
     * 
     * @param textFragment the text fragment to wait for
     * @return this rich text area instance
     * @since 18.4.0RC1
     */
    public BlockNoteRichTextArea waitUntilTextContains(String textFragment)
    {
        try {
            getDriver().waitUntilCondition(driver -> getText().contains(textFragment));
        } catch (Exception e) {
            String text = getText();
            assertTrue(text.contains(textFragment), "Unexpected content: " + text);
        }
        return this;
    }

    /**
     * Waits until the rich text area text is exactly the specified plain text.
     *
     * @param text the expected text, optionally including the position of the user cursors using the "%s" placeholder
     * @param cursors the list of users whose cursor position is displayed inside the edited content
     * @return this rich text area instance
     * @since 18.4.0RC1
     */
    public BlockNoteRichTextArea waitUntilTextIs(String text, String... cursors)
    {
        String expectedText = getExpectedText(text, cursors);
        try {
            getDriver().waitUntilCondition(driver -> expectedText.equals(getText()));
        } catch (Exception e) {
            assertEquals(expectedText, getText());
        }
        return this;
    }

    private String getExpectedText(String expectedText, String... cursors)
    {
        Object[] args = new Object[cursors.length];
        for (int i = 0; i < cursors.length; i++) {
            args[i] = "\u2060%n%s%n\u2060".formatted(cursors[i]);
        }
        return expectedText.formatted(args);
    }

    /**
     * Asserts that the rich text area text is exactly the specified plain text.
     *
     * @param expectedText the expected text, optionally including the position of the user cursors using the "%s"
     *            placeholder
     * @param cursors the list of users whose cursor position is displayed inside the edited content
     * @since 18.4.0RC1
     */
    public void assertTextIs(String expectedText, String... cursors)
    {
        assertEquals(getExpectedText(expectedText, cursors), getText());
    }

    /**
     * Waits until the rich text area is focused. This is especially needed when switching between browser tabs because:
     * <ul>
     * <li>when a browser tab becomes inactive its active element gets blurred (loses the focus)</li>
     * <li>when a browser tab becomes active its active element gets back the focus; however, this doesn't always happen
     * instantly; moreover, besides the focus, the selection (caret position) also needs to be restored; if you try to
     * send keys to the active element right after activating the tab they might be ignored</li>
     * </ul>
     * 
     * @return this rich text area instance
     * @since 18.4.0RC1
     */
    public BlockNoteRichTextArea waitUntilFocused()
    {
        // Wait for the rich text area to be focused for two consecutive ticks.
        String script = """
            const richTextArea = arguments[0];
            const selectionContainer = window.getSelection()?.getRangeAt(0)?.commonAncestorContainer;
            const focused = document.visibilityState === 'visible'
              && document.hasFocus()
              && richTextArea.contains(document.activeElement)
              && richTextArea.contains(selectionContainer);
            if (focused) {
              richTextArea.__focusCount = (richTextArea.__focusCount || 0) + 1;
              if (richTextArea.__focusCount > 1) {
                delete richTextArea.__focusCount;
                return true;
              } else {
                return false;
              }
            } else {
              delete richTextArea.__focusCount;
              return false;
            }
            """;
        getDriver().waitUntilCondition(driver -> (boolean) getDriver().executeScript(script, this.container));
        return this;
    }
}

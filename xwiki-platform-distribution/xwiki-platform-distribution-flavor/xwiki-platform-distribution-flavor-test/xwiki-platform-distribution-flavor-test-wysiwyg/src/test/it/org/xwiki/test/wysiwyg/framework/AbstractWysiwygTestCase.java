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
package org.xwiki.test.wysiwyg.framework;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.xwiki.test.selenium.framework.AbstractXWikiTestCase;

import com.thoughtworks.selenium.Wait;

import static org.junit.Assert.*;

/**
 * All XWiki WYSIWYG tests must extend this class.
 * 
 * @version $Id$
 */
public class AbstractWysiwygTestCase extends AbstractXWikiTestCase
{
    private static final String WYSIWYG_LOCATOR_FOR_WYSIWYG_TAB = "//div[@role='tab'][@tabIndex=0]/div[.='WYSIWYG']";

    private static final String WYSIWYG_LOCATOR_FOR_SOURCE_TAB = "//div[@role='tab'][@tabIndex=0]/div[.='Source']";

    /**
     * The title of the indent tool bar button. This title is used in XPath locators to access the indent button.
     */
    public static final String TOOLBAR_BUTTON_INDENT_TITLE = "Increase Indent";

    /**
     * The title of the outdent tool bar button. This title is used in XPath locators to access the outdent button.
     */
    public static final String TOOLBAR_BUTTON_OUTDENT_TITLE = "Decrease Indent";

    /**
     * The title of the undo tool bar button.
     */
    public static final String TOOLBAR_BUTTON_UNDO_TITLE = "Undo (Ctrl+Z)";

    /**
     * The title of the redo tool bar button.
     */
    public static final String TOOLBAR_BUTTON_REDO_TITLE = "Redo (Ctrl+Y)";

    /**
     * The locator for the tool bar list box used to change the style of the current selection.
     */
    public static final String TOOLBAR_SELECT_STYLE = "//select[@title=\"Apply Style\"]";

    /**
     * Locates a menu item by its label.
     */
    public static final String MENU_ITEM_BY_LABEL =
        "//td[contains(@class, 'gwt-MenuItem')]/div[@class = 'gwt-MenuItemLabel' and . = '%s']";

    /**
     * Use this small interval when the operation you are waiting for doesn't execute instantly but pretty fast anyway.
     */
    public static final long SMALL_WAIT_INTERVAL = 50L;

    @Override
    public void setUp()
    {
        super.setUp();

        login();
        open(this.getClass().getSimpleName(), getTestMethodName(), "edit", "editor=wysiwyg");
        waitForEditorToLoad();
    }

    /**
     * Logs in with the default user for this test case.
     */
    protected void login()
    {
        // Nothing here. Use the default login in the WYSIWYG test setup.
    }

    /**
     * @return the rich text area element
     */
    protected RichTextAreaElement getRichTextArea()
    {
        WebDriver driver = getDriver();
        return new RichTextAreaElement(driver, driver.findElement(By.className("gwt-RichTextArea")));
    }

    /**
     * @return the source text area
     */
    protected WebElement getSourceTextArea()
    {
        return getDriver().findElement(By.className("xPlainTextEditor"));
    }

    /**
     * Sets the content of the rich text area.
     * 
     * @param html the new content of the rich text area
     */
    public void setContent(String html)
    {
        getRichTextArea().setContent(html);
    }

    /**
     * Resets the content of the rich text area by selecting all the text like CTRL+A and deleting it using Backspace.
     */
    public void resetContent()
    {
        // We try to mimic as much as possible the user behavior.
        // First, we select all the content.
        selectAllContent();
        // Delete the selected content.
        typeBackspace();
        // We select again all the content. In Firefox, the selection will include the annoying br tag. Further typing
        // will overwrite it. See XWIKI-2732.
        selectAllContent();
    }

    public void selectAllContent()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, "a"));
    }

    public void typeText(String text)
    {
        getRichTextArea().sendKeys(text);
    }

    public void typeTextThenEnter(String text)
    {
        getRichTextArea().sendKeys(text, Keys.RETURN);
    }

    /**
     * Presses the specified key for the given number of times in WYSIWYG rich text editor.
     * 
     * @param key the key to be pressed
     * @param count the number of times to press the specified key
     * @param hold {@code false} if the key should be released after each key press, {@code true} if it should be hold
     *            down and released just at the end
     */
    public void sendKey(Keys key, int count, boolean hold)
    {
        Keys[] sequence = new Keys[count];
        Arrays.fill(sequence, key);
        if (hold) {
            getRichTextArea().sendKeys(Keys.chord(sequence));
        } else {
            getRichTextArea().sendKeys(sequence);
        }
    }

    public void typeEnter()
    {
        typeEnter(1);
    }

    public void typeEnter(int nb)
    {
        sendKey(Keys.RETURN, nb, false);
    }

    public void typeShiftEnter()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.SHIFT, Keys.RETURN));
    }

    public void typeControlEnter()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.CONTROL, Keys.RETURN));
    }

    public void typeMetaEnter()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.META, Keys.RETURN));
    }

    public void typeBackspace()
    {
        typeBackspace(1);
    }

    public void typeBackspace(int count)
    {
        typeBackspace(count, false);
    }

    public void typeBackspace(int count, boolean hold)
    {
        sendKey(Keys.BACK_SPACE, count, hold);
    }

    public void typeLeftArrow()
    {
        getRichTextArea().sendKeys(Keys.ARROW_LEFT);
    }

    public void typeUpArrow()
    {
        getRichTextArea().sendKeys(Keys.ARROW_UP);
    }

    public void typeRightArrow()
    {
        getRichTextArea().sendKeys(Keys.ARROW_RIGHT);
    }

    public void typeDownArrow()
    {
        getRichTextArea().sendKeys(Keys.ARROW_DOWN);
    }

    public void typeDelete()
    {
        typeDelete(1);
    }

    public void typeDelete(int count)
    {
        typeDelete(count, false);
    }

    public void typeDelete(int count, boolean hold)
    {
        sendKey(Keys.DELETE, count, hold);
    }

    public void typeTab()
    {
        typeTab(1);
    }

    public void typeTab(int count)
    {
        sendKey(Keys.TAB, count, false);
    }

    public void typeShiftTab()
    {
        getRichTextArea().sendKeys(Keys.chord(Keys.SHIFT, Keys.TAB));
    }

    public void typeShiftTab(int count)
    {
        for (int i = 0; i < count; i++) {
            typeShiftTab();
        }
    }

    public void clickUnorderedListButton()
    {
        pushToolBarButton("Bullets On/Off");
    }

    public void clickOrderedListButton()
    {
        pushToolBarButton("Numbering On/Off");
    }

    public void clickIndentButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_INDENT_TITLE);
    }

    public boolean isIndentButtonEnabled()
    {
        return isPushButtonEnabled(TOOLBAR_BUTTON_INDENT_TITLE);
    }

    public void clickOutdentButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_OUTDENT_TITLE);
    }

    public boolean isOutdentButtonEnabled()
    {
        return isPushButtonEnabled(TOOLBAR_BUTTON_OUTDENT_TITLE);
    }

    public void clickBoldButton()
    {
        pushToolBarButton("Bold (Ctrl+B)");
    }

    public void clickItalicsButton()
    {
        pushToolBarButton("Italic (Ctrl+I)");
    }

    public void clickUnderlineButton()
    {
        pushToolBarButton("Underline (Ctrl+U)");
    }

    public void clickStrikethroughButton()
    {
        pushToolBarButton("Strikethrough");
    }

    public void clickHRButton()
    {
        pushToolBarButton("Insert Horizontal Ruler");
    }

    public void clickSubscriptButton()
    {
        pushToolBarButton("Subscript");
    }

    public void clickSuperscriptButton()
    {
        pushToolBarButton("Superscript");
    }

    public void clickUndoButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_UNDO_TITLE);
    }

    public void clickUndoButton(int count)
    {
        for (int i = 0; i < count; i++) {
            clickUndoButton();
        }
    }

    public void clickRedoButton()
    {
        pushToolBarButton(TOOLBAR_BUTTON_REDO_TITLE);
    }

    public void clickRedoButton(int count)
    {
        for (int i = 0; i < count; i++) {
            clickRedoButton();
        }
    }

    public void clickSymbolButton()
    {
        pushToolBarButton("Insert Custom Character");
    }

    public void clickOfficeImporterButton()
    {
        pushToolBarButton("Import Office Content");
    }

    public void clickBackToEdit()
    {
        submit("//input[@type = 'submit' and @value = 'Back To Edit']");
        waitForEditorToLoad();
    }

    public void applyStyle(final String style)
    {
        // Wait until the given style is not selected (because the tool bar might not be updated).
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isEditable(TOOLBAR_SELECT_STYLE)
                    && (!getSelenium().isSomethingSelected(TOOLBAR_SELECT_STYLE) || !style.equals(getSelenium()
                        .getSelectedLabel(TOOLBAR_SELECT_STYLE)));
            }
        }.wait("The specified style, '" + style + "', is already applied!");
        getSelenium().select(TOOLBAR_SELECT_STYLE, style);
    }

    /**
     * Waits for the specified style to be detected.
     * 
     * @param style the expected style
     */
    public void waitForStyleDetected(final String style)
    {
        new Wait()
        {
            public boolean until()
            {
                return getSelenium().isSomethingSelected(TOOLBAR_SELECT_STYLE)
                    && style.equals(getSelenium().getSelectedLabel(TOOLBAR_SELECT_STYLE));
            }
        }.wait("The specified style, '" + style + "', wasn't detected!");
    }

    public void applyStylePlainText()
    {
        applyStyle("Plain text");
    }

    public void applyStyleTitle1()
    {
        applyStyle("Title 1");
    }

    public void applyStyleTitle2()
    {
        applyStyle("Title 2");
    }

    public void applyStyleTitle3()
    {
        applyStyle("Title 3");
    }

    public void applyStyleTitle4()
    {
        applyStyle("Title 4");
    }

    public void applyStyleTitle5()
    {
        applyStyle("Title 5");
    }

    public void applyStyleTitle6()
    {
        applyStyle("Title 6");
    }

    public void pushButton(String locator)
    {
        // Can't use : selenium.click(locator);
        // A GWT PushButton is not a standard HTML <input type="submit" ...> or a <button ...>
        // rather it is a styled button constructed from DIV and other HTML tags.
        // Source :
        // http://www.blackpepper.co.uk/black-pepper-blog/Simulating-clicks-on-GWT-push-buttons-with-Selenium-RC.html
        getSelenium().mouseOver(locator);
        getSelenium().mouseDown(locator);
        getSelenium().mouseUp(locator);
        getSelenium().mouseOut(locator);
    }

    /**
     * Pushes the tool bar button with the specified title.
     * 
     * @param title the title of the tool bar button to be pushed
     */
    public void pushToolBarButton(String title)
    {
        pushButton("//div[@title='" + title + "']");
    }

    public void clickButtonWithText(String buttonText)
    {
        getSelenium().click("//button[. = \"" + buttonText + "\"]");
    }

    /**
     * Clicks on the menu item with the specified label.
     * 
     * @param menuLabel a {@link String} representing the label of a menu item
     */
    public void clickMenu(String menuLabel)
    {
        String selector = String.format(MENU_ITEM_BY_LABEL, menuLabel);
        // We select the menu item first.
        getSelenium().mouseOver(selector);
        // And then we click on it.
        getSelenium().click(selector);
    }

    /**
     * Waits for the specified menu to be present.
     * 
     * @param menuLabel the menu label
     */
    public void waitForMenu(String menuLabel)
    {
        waitForElement(String.format(MENU_ITEM_BY_LABEL, menuLabel));
    }

    /**
     * Closes the menu containing the specified menu item by pressing the escape key.
     * 
     * @param menuLabel a menu item from the menu to be closed
     */
    public void closeMenuContaining(String menuLabel)
    {
        getSelenium().typeKeys(String.format(MENU_ITEM_BY_LABEL, menuLabel), "\\27");
    }

    /**
     * Switch the WYSIWYG editor by clicking on the "WYSIWYG" tab item and waits for the rich text area to be
     * initialized.
     */
    public void switchToWysiwyg()
    {
        switchToWysiwyg(true);
    }

    /**
     * Switch the WYSIWYG editor by clicking on the "WYSIWYG" tab item.
     * 
     * @param wait {@code true} to wait for the rich text area to be initialized, {@code false} otherwise
     */
    public void switchToWysiwyg(boolean wait)
    {
        ensureElementIsNotCoveredByFloatingMenu(By.xpath(WYSIWYG_LOCATOR_FOR_WYSIWYG_TAB));
        getSelenium().click(WYSIWYG_LOCATOR_FOR_WYSIWYG_TAB);
        if (wait) {
            final String enabledToolBarButtonXPath =
                "//div[contains(@class, 'gwt-ToggleButton') and not(contains(@class, '-disabled'))]";
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver input)
                {
                    // When switching between tabs, it sometimes takes longer for toggle buttons to become enabled. We
                    // need to wait for that before we can properly use the WYSIWYG.
                    return !getSourceTextArea().isEnabled()
                        && getDriver().hasElementWithoutWaiting(By.xpath(enabledToolBarButtonXPath));
                }
            });
        }
    }

    /**
     * Switch the Source editor by clicking on the "Source" tab item and waits for the plain text area to be
     * initialized.
     */
    public void switchToSource()
    {
        switchToSource(true);
    }

    /**
     * Switch the Source editor by clicking on the "Source" tab item.
     * 
     * @param wait {@code true} to wait for the plain text area to be initialized, {@code false} otherwise
     */
    public void switchToSource(boolean wait)
    {
        ensureElementIsNotCoveredByFloatingMenu(By.xpath(WYSIWYG_LOCATOR_FOR_SOURCE_TAB));
        getSelenium().click(WYSIWYG_LOCATOR_FOR_SOURCE_TAB);
        if (wait) {
            getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver input)
                {
                    return getDriver().findElementWithoutWaiting(By.className("xPlainTextEditor")).isEnabled();
                }
            });
            // Focus the source text area.
            getSourceTextArea().sendKeys("");
        }
    }

    /**
     * Types the specified text in the input specified by its title.
     * 
     * @param inputTitle the {@code title} attribute of the {@code} input element to type in
     * @param text the text to type in the input
     */
    public void typeInInput(String inputTitle, String text)
    {
        getSelenium().type("//input[@title=\"" + inputTitle + "\"]", text);
    }

    /**
     * @param inputTitle the title of the input whose value to return.
     * @return the value of an input specified by its title.
     */
    public String getInputValue(String inputTitle)
    {
        return getSelenium().getValue("//input[@title=\"" + inputTitle + "\"]");
    }

    public boolean isPushButtonEnabled(String pushButtonTitle)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[@title='" + pushButtonTitle + "' and @class='gwt-PushButton gwt-PushButton-up']"));
    }

    /**
     * @param toggleButtonTitle the tool tip of a toggle button from the WYSIWYG tool bar
     * @return {@code true} if the specified toggle button is enabled, {@code false} otherwise
     */
    public boolean isToggleButtonEnabled(String toggleButtonTitle)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[@title='" + toggleButtonTitle
                + "' and contains(@class, 'gwt-ToggleButton') and not(contains(@class, '-disabled'))]"));
    }

    /**
     * Waits for the specified push button to have the specified state i.e. enabled or disabled.
     * 
     * @param pushButtonTitle identifies the button to wait for
     * @param enabled {@code true} to wait for the specified button to become enabled, {@code false} to wait for it to
     *            become disabled
     */
    public void waitForPushButton(final String pushButtonTitle, final boolean enabled)
    {
        new Wait()
        {
            public boolean until()
            {
                return enabled == isPushButtonEnabled(pushButtonTitle);
            }
        }.wait(pushButtonTitle + " button is not " + (enabled ? "enabled" : "disabled") + "!");
    }

    /**
     * Waits for the specified toggle button be enabled or disabled, based on the given state parameter.
     * 
     * @param toggleButtonTitle identifies the button to wait for
     * @param enabled {@code true} to wait for the specified toggle button to become enabled, {@code false} to wait for
     *            it to become disabled
     */
    public void waitForToggleButton(final String toggleButtonTitle, final boolean enabled)
    {
        new Wait()
        {
            public boolean until()
            {
                return enabled == isToggleButtonEnabled(toggleButtonTitle);
            }
        }.wait(toggleButtonTitle + " button is not " + (enabled ? "enabled" : "disabled") + "!");
    }

    /**
     * Waits until the specified toggle button has the given state. This method is useful to wait until a toggle button
     * from the tool bar is updated.
     * 
     * @param toggleButtonTitle the tool tip of a toggle button
     * @param down {@code true} to wait until the specified toggle button is down, {@code false} to wait until it is up
     */
    public void waitForToggleButtonState(final String toggleButtonTitle, final boolean down)
    {
        new Wait()
        {
            public boolean until()
            {
                return down == isToggleButtonDown(toggleButtonTitle);
            }
        }.wait("The state of the '" + toggleButtonTitle + "' toggle button didn't change!");
    }

    /**
     * @param toggleButtonTitle the tool tip of a toggle button
     * @return {@code true} if the specified toggle button is down, {@code false} otherwise
     */
    public boolean isToggleButtonDown(String toggleButtonTitle)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//div[@title='" + toggleButtonTitle + "' and @class='gwt-ToggleButton gwt-ToggleButton-down']"));
    }

    /**
     * Checks if a menu item is enabled or disabled. Menu items have {@code gwt-MenuItem} CSS class. Disabled menu items
     * have and additional {@code gwt-MenuItem-disabled} CSS class.
     * 
     * @param menuLabel a {@link String} representing the label of a menu item
     * @return {@code true} if the menu with the specified label is enabled, {@code false} otherwise
     */
    public boolean isMenuEnabled(String menuLabel)
    {
        return getDriver().hasElementWithoutWaiting(
            By.xpath("//td[contains(@class, 'gwt-MenuItem') and not(contains(@class, 'gwt-MenuItem-disabled'))]"
                + "/div[@class = 'gwt-MenuItemLabel' and . = '" + menuLabel + "']"));
    }

    /**
     * Asserts that the rich text area has the expected inner HTML.
     * 
     * @param expectedHTML the expected inner HTML of the rich text area
     */
    public void assertContent(String expectedHTML)
    {
        assertEquals(expectedHTML, getRichTextArea().getContent());
    }

    /**
     * Places the caret in the specified container, at the specified offset.
     * 
     * @param containerJSLocator the JavaScript code used to access the container node
     * @param offset the offset within the container node
     */
    public void moveCaret(String containerJSLocator, int offset)
    {
        StringBuilder script = new StringBuilder();
        script.append("var range = document.createRange();\n");
        script.append("range.setStart(");
        script.append(containerJSLocator);
        script.append(", ");
        script.append(offset);
        script.append(");\n");
        script.append("range.collapse(true);\n");
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(range);");
        getRichTextArea().executeScript(script.toString());
        triggerToolbarUpdate();
    }

    /**
     * Selects the content between the specified points in the DOM tree.
     * 
     * @param startContainerJSLocator the node containing the start of the selection
     * @param startOffset the offset within the start container where the selection starts
     * @param endContainerJSLocator the node containing the end of the selection
     * @param endOffset the offset within the end container where the selection ends
     */
    public void select(String startContainerJSLocator, int startOffset, String endContainerJSLocator, int endOffset)
    {
        StringBuilder script = new StringBuilder();
        script.append("var range = document.createRange();\n");
        script.append("range.setStart(");
        script.append(startContainerJSLocator);
        script.append(", ");
        script.append(startOffset);
        script.append(");\n");
        script.append("range.setEnd(");
        script.append(endContainerJSLocator);
        script.append(", ");
        script.append(endOffset);
        script.append(");\n");
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(range);\n");
        getRichTextArea().executeScript(script.toString());
        triggerToolbarUpdate();
    }

    /**
     * Selects the specified DOM node.
     * 
     * @param jsLocator a JavaScript locator for the node to be selected
     */
    public void selectNode(String jsLocator)
    {
        StringBuilder script = new StringBuilder();
        script.append("var range = document.createRange();\n");
        script.append("range.selectNode(");
        script.append(jsLocator);
        script.append(");\n");
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(range);\n");
        getRichTextArea().executeScript(script.toString());
        triggerToolbarUpdate();
    }

    /**
     * Selects the contents of the specified DOM node.
     * 
     * @param jsLocator a JavaScript locator for the node whose content are to be selected
     */
    public void selectNodeContents(String jsLocator)
    {
        StringBuilder script = new StringBuilder();
        script.append("var range = document.createRange();\n");
        script.append("range.selectNodeContents(");
        script.append(jsLocator);
        script.append(");\n");
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(range);\n");
        getRichTextArea().executeScript(script.toString());
        triggerToolbarUpdate();
    }

    /**
     * Triggers the wysiwyg toolbar update by typing a key. To be used after programatically setting the selection (with
     * {@link AbstractWysiwygTestCase#select(String, int, String, int)} or
     * {@link AbstractWysiwygTestCase#moveCaret(String, int)}): it will not influence the selection but it will cause
     * the toolbar to update according to the new selection.
     */
    protected void triggerToolbarUpdate()
    {
        getRichTextArea().sendKeys(Keys.SHIFT);
    }

    /**
     * Wait for a WYSIWYG dialog to close. The test checks for a {@code div} element with {@code xDialogBox} value of
     * {@code class} to not be present.
     */
    public void waitForDialogToClose()
    {
        getDriver().waitUntilElementDisappears(By.className("xDialogBox"));
    }

    /**
     * Waits until a WYSIWYG modal dialog is fully loaded. While loading, the body of the dialog has the {@code loading}
     * CSS class besides the {@code xDialogBody} one.
     */
    public void waitForDialogToLoad()
    {
        getDriver().waitUntilElementIsVisible(
            By.xpath("//div[contains(@class, 'xDialogBody') and not(contains(@class, 'loading'))]"));
    }

    /**
     * Close the dialog by clicking the close icon in the top right.
     */
    public void closeDialog()
    {
        getSelenium().click("//img[contains(@class, \"gwt-Image\") and contains(@class, \"xDialogCloseIcon\")]");
        waitForDialogToClose();
    }

    /**
     * Waits until the WYSIWYG editor detects the bold style on the current selection. The bold style is detected when
     * the associated tool bar button is updated. The update is delayed to increase the typing speed.
     */
    public void waitForBoldDetected(boolean down)
    {
        waitForToggleButtonState("Bold (Ctrl+B)", down);
    }

    /**
     * Waits until the WYSIWYG editor detects the underline style on the current selection. The underline style is
     * detected when the associated tool bar button is updated. The update is delayed to increase the typing speed.
     */
    public void waitForUnderlineDetected(boolean down)
    {
        waitForToggleButtonState("Underline (Ctrl+U)", down);
    }

    /**
     * Asserts that the specified error message exists, and the element passed through its XPath locator is marked as in
     * error.
     * 
     * @param errorMessage the expected error message
     * @param fieldXPathLocator the XPath locator of the field which is in error
     */
    public void assertFieldErrorIsPresent(String errorMessage, String fieldXPathLocator)
    {
        // test that the error field is present through this method because the isVisible stops at first encouter of the
        // matching element and fails if it's not visible. However, multiple matching elements might exist and we're
        // interested in at least one of them visible
        assertTrue(getSelenium().getXpathCount(
            "//*[contains(@class, \"xErrorMsg\") and . = '" + errorMessage + "' and @style='']").intValue() > 0);
        assertTrue(getDriver().hasElementWithoutWaiting(
            By.xpath(fieldXPathLocator + "[contains(@class, 'xErrorField')]")));
    }

    /**
     * Asserts that the specified error message does not exist and that the field passed through the XPath locator is
     * not in error. Note that this function checks that the passed field is present, but without an error marker.
     * 
     * @param errorMessage the error message
     * @param fieldXPathLocator the XPath locator of the field to check that it's not in error
     */
    public void assertFieldErrorIsNotPresent(String errorMessage, String fieldXPathLocator)
    {
        assertFalse(getSelenium().isVisible("//*[contains(@class, \"xErrorMsg\") and . = \"" + errorMessage + "\"]"));
        assertTrue(getDriver().hasElementWithoutWaiting(
            By.xpath(fieldXPathLocator + "[not(contains(@class, 'xFieldError'))]")));
    }

    /**
     * Asserts that no error message or field marked as in error is present.
     */
    public void assertFieldErrorIsNotPresent()
    {
        // no error is visible
        assertFalse(getSelenium().isVisible("//*[contains(@class, \"xErrorMsg\")]"));
        // no field with error markers should be present
        assertFalse(getDriver().hasElementWithoutWaiting(By.className("xFieldError")));
    }

    /**
     * Focuses the rich text area.
     * <p>
     * NOTE: The initial range CAN differ when the browser window is focused from when it isn't! Make sure you place the
     * caret where you want it to be at the beginning of you test and after switching back to WYSIWYG editor.
     */
    protected void focusRichTextArea()
    {
        getRichTextArea().sendKeys("");
    }

    /**
     * Simulates a blur event on the rich text area. We don't use the blur method because it fails to notify our
     * listeners when the browser window is not focused, preventing us from running the tests in background.
     */
    protected void blurRichTextArea()
    {
        getDriver().findElement(By.id("xwikidoctitleinput")).sendKeys("");
    }

    /**
     * Inserts a table in place of the current selection or at the caret position, using the default table settings.
     */
    protected void insertTable()
    {
        openInsertTableDialog();
        getSelenium().click("//button[text()=\"Insert Table\"]");
    }

    /**
     * Opens the insert table dialog.
     */
    protected void openInsertTableDialog()
    {
        clickMenu("Table");
        clickMenu("Insert Table...");
        waitForDialogToLoad();
    }

    /**
     * @return the text from the source text area
     */
    protected String getSourceText()
    {
        return getSourceTextArea().getAttribute("value");
    }

    /**
     * Sets the value of the source text area.
     * 
     * @param sourceText the new value for the source text area
     */
    protected void setSourceText(String sourceText)
    {
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].value = arguments[1]", getSourceTextArea(),
            sourceText);
    }

    /**
     * Asserts that the source text area has the given value.
     * 
     * @param expectedSourceText the expected value of the source text area
     */
    protected void assertSourceText(String expectedSourceText)
    {
        assertEquals(expectedSourceText, getSourceText());
    }

    /**
     * Waits for the WYSIWYG editor to load.
     */
    protected void waitForEditorToLoad()
    {
        final String sourceTabSelected = "//div[@class = 'gwt-TabBarItem gwt-TabBarItem-selected']/div[. = 'Source']";
        final String richTextAreaLoader = "//div[@class = 'xRichTextEditor']//div[@class = 'loading']";
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver input)
            {
                // Either the source tab is present and selected and the plain text area can be edited or the rich text
                // area is not loading (with or without tabs).
                return (getDriver().hasElementWithoutWaiting(By.xpath(sourceTabSelected)) && getSourceTextArea()
                    .isEnabled())
                    || (getDriver().hasElementWithoutWaiting(By.className("xRichTextEditor")) && !getDriver()
                        .hasElementWithoutWaiting(By.xpath(richTextAreaLoader)));
            }
        });
    }

    /**
     * Switches to full screen editing mode.
     */
    protected void clickEditInFullScreen()
    {
        getSelenium().click("//img[@title = 'Maximize']");
        waitForElement("//div[@class = 'fullScreenWrapper']");
    }

    /**
     * Exists full screen editing mode.
     */
    protected void clickExitFullScreen()
    {
        getDriver().findElementByXPath("//input[@value = 'Exit Full Screen']").click();
        getDriver().waitUntilElementDisappears(By.className("fullScreenWrapper"));
    }

    /**
     * Creates a new space with the specified name.
     * 
     * @param spaceName the name of the new space to create
     */
    public void createSpace(String spaceName)
    {
        getSelenium().runScript("window.scrollTo(0, 0)");
        getSelenium().click("//div[@id='tmCreate']//button[contains(@class, 'dropdown-toggle')]");
        clickLinkWithLocator("tmCreateSpace");
        getSelenium().type("name", spaceName);
        clickLinkWithLocator("//input[@value='Create']");
        clickEditSaveAndView();
    }

    /**
     * Begin creating a page in the specified space, with the specified name.
     * <p>
     * NOTE: We don't use the save action URL because it requires special characters in space and page name to be
     * escaped. We use instead the create action URL.
     * 
     * @param spaceName the name of the space where to create the page
     * @param pageName the name of the page to create
     * @see #createPage(String, String, String)
     */
    public void startCreatePage(String spaceName, String pageName)
    {
        String queryString = "templateprovider=";
        try {
            queryString += "&space=" + URLEncoder.encode(spaceName, "UTF-8");
            queryString += "&page=" + URLEncoder.encode(pageName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Shouldn't happen.
        }
        getSelenium().open(this.getUrl("Main", "WebHome", "create", queryString));
    }

    /**
     * Creates a page in the specified space, with the specified name.
     * <p>
     * NOTE: We overwrite the method from the base class because it creates the new page using the save action URL which
     * requires special characters in space and page name to be escaped. We use instead the create action URL.
     * 
     * @param spaceName the name of the space where to create the page
     * @param pageName the name of the page to create
     * @param content the content of the new page
     * @see AbstractXWikiTestCase#createPage(String, String, String)
     */
    public void createPage(String spaceName, String pageName, String content)
    {
        startCreatePage(spaceName, pageName);

        String location = getSelenium().getLocation();
        if (location.endsWith("?xpage=docalreadyexists")) {
            open(location.substring(0, location.length() - 23));
            clickEditPageInWysiwyg();
        }
        waitForEditorToLoad();
        switchToSource();
        setSourceText(content);
        clickEditSaveAndView();
    }

    /**
     * Selects the rich text area frame. Selectors are relative to the edited document after calling this method.
     */
    public void selectRichTextAreaFrame()
    {
        WebDriver driver = getDriver();
        driver.switchTo().frame(driver.findElement(By.className("gwt-RichTextArea"))).switchTo().activeElement();
    }

    /**
     * Selects the top frame.
     */
    public void selectTopFrame()
    {
        getSelenium().selectFrame("relative=top");
    }
}

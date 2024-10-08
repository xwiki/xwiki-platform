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
package org.xwiki.ckeditor.test.po;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.xwiki.stability.Unstable;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the editing area of a WYSIWYG editor.
 *
 * @version $Id$
 * @since 1.19
 */
@Unstable
public class RichTextAreaElement extends BaseElement
{
    /**
     * Provides information about the content of the rich text area.
     *
     * @since 16.1.0RC1
     * @since 15.10.7
     */
    public class RichTextAreaContent
    {
        /**
         * @return the list of images included in the rich text area
         */
        public List<WebElement> getImages()
        {

            return getRootEditableElement(false).findElements(By.tagName("img"));
        }
    }

    /**
     * The element that defines the rich text area.
     */
    private final WebElement container;

    private final boolean isFrame;

    private final RichTextAreaContent content = new RichTextAreaContent();

    /**
     * Creates a new rich text area element.
     * 
     * @param container the element that defines the rich text area
     * @param wait whether to wait or not for the content to be editable
     */
    public RichTextAreaElement(WebElement container, boolean wait)
    {
        this.container = container;
        this.isFrame = "iframe".equals(container.getTagName());

        if (wait) {
            this.waitUntilContentEditable();
        }
    }

    /**
     * @return the inner text of the rich text area
     */
    public String getText()
    {
        try {
            return getRootEditableElement().getText();
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Clears the content of the rich text area.
     */
    public void clear()
    {
        try {
            getRootEditableElement().clear();
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Clicks on the rich text area.
     */
    public void click()
    {
        try {
            getRootEditableElement().click();
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Clicks somewhere on the edited content.
     * 
     * @param contentSelector specifies an element from the edited content to click on
     * @since 15.10.11
     * @since 16.4.1
     * @since 16.6.0RC1
     */
    public void click(By contentSelector)
    {
        try {
            WebElement rootEditableElement = getRootEditableElement();
            getDriver().findElementWithoutWaiting(rootEditableElement, contentSelector).click();
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    protected void maybeSwitchToEditedContent()
    {
        if (this.isFrame) {
            getDriver().switchTo().frame(this.container);
        }
    }

    protected void maybeSwitchToDefaultContent()
    {
        if (this.isFrame) {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * Simulate typing in the rich text area.
     * 
     * @param keysToSend the sequence of keys to by typed
     */
    public void sendKeys(CharSequence... keysToSend)
    {
        if (keysToSend.length > 0) {
            try {
                getActiveElement().sendKeys(keysToSend);
            } finally {
                maybeSwitchToDefaultContent();
            }
        }
    }

    /**
     * Executes the given script in the context of the rich text area.
     * 
     * @param script the script to be executed
     * @param arguments the script arguments
     * @return the result of the script execution
     * @see JavascriptExecutor#executeScript(String, Object...)
     */
    public Object executeScript(String script, Object... arguments)
    {
        return getFromEditedContent(() -> getDriver().executeScript(script, arguments));
    }

    /**
     * @return the HTML content of the rich text area
     */
    public String getContent()
    {
        try {
            return getRootEditableElement().getDomProperty("innerHTML");
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Sets the HTML content of the rich text area.
     * 
     * @param content the new HTML content
     */
    public void setContent(String content)
    {
        try {
            getDriver().executeScript("arguments[0].innerHTML = arguments[1];", getRootEditableElement(), content);
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Waits until the content of the rich text area contains the given HTML fragment.
     * 
     * @param html the HTML fragment to wait for
     */
    public void waitUntilContentContains(String html)
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return StringUtils.contains(getContent(), html);
            } catch (StaleElementReferenceException e) {
                // The edited content can be reloaded (which includes the root editable in standalone mode) while we're
                // waiting, for instance because a macro was inserted or updated as a result of a remote change.
                return false;
            }
        });
    }

    /**
     * Waits until the rich text area contains the specified plain text.
     * 
     * @param textFragment the text fragment to wait for
     * @since 16.0
     * @since 15.10.6
     */
    public void waitUntilTextContains(String textFragment)
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return StringUtils.contains(getText(), textFragment);
            } catch (StaleElementReferenceException e) {
                // The edited content can be reloaded (which includes the root editable in standalone mode) while we're
                // waiting, for instance because a macro was inserted or updated as a result of a remote change.
                return false;
            }
        });
    }

    /**
     * @return the HTML element that has the focus in the Rich editor
     */
    private WebElement getActiveElement()
    {
        WebElement rootEditableElement = getRootEditableElement();
        String browserName = getDriver().getCapabilities().getBrowserName().toLowerCase();
        if (browserName.contains("firefox")) {
            // Firefox works best if we send the keys to the root editable element, but we need to focus it first,
            // otherwise the keys are ignored. If we send the keys to a nested editable then we can't navigate outside
            // of it using the arrow keys (as if the editing scope is set to that nested editable).
            focus(rootEditableElement);
            return rootEditableElement;
        } else {
            // Chrome expects us to send the keys directly to the nested editable where we want to type. If we send the
            // keys to the root editable then they are inserted directly in the root editable (e.g. when editing a macro
            // inline the characters we type will be inserted outside the macro content nested editable).
            WebElement activeElement = getDriver().switchTo().activeElement();
            boolean rootEditableElementIsOrContainsActiveElement = (boolean) getDriver()
                .executeScript("return arguments[0].contains(arguments[1])", rootEditableElement, activeElement);
            return rootEditableElementIsOrContainsActiveElement ? activeElement : rootEditableElement;
        }
    }

    /**
     * @return the top most editable element in the rich text area (that includes all the editable content, including
     *         nested editable areas)
     */
    private WebElement getRootEditableElement()
    {
        return getRootEditableElement(true);
    }

    /**
     * @return the top most editable element in the rich text area (that includes all the editable content, including
     *         nested editable areas)
     * @param switchToFrame {@code true} if the driver should switch to the frame containing the rich text area
     */
    private WebElement getRootEditableElement(boolean switchToFrame)
    {
        if (this.isFrame) {
            if (switchToFrame) {
                getDriver().switchTo().frame(this.container);
            }
            return getDriver().findElementWithoutWaitingWithoutScrolling(By.tagName("body"));
        } else {
            return this.container;
        }
    }

    /**
     * Waits until the content is editable.
     *
     * @since 15.5.1
     * @since 15.6RC1
     */
    public void waitUntilContentEditable()
    {
        getDriver().waitUntilCondition(driver -> getFromEditedContent(
            () -> getRootEditableElement(false).getAttribute("contenteditable").equals("true")));
    }

    /**
     * @param placeholder the expected placeholder text, {@code null} if no placeholder is expected
     * @return this rich text area element
     */
    public RichTextAreaElement waitForPlaceholder(String placeholder)
    {
        try {
            WebElement rootEditableElement = getRootEditableElement();
            getDriver().waitUntilCondition(
                driver -> Objects.equals(placeholder, rootEditableElement.getAttribute("data-cke-editorplaceholder")));
        } finally {
            maybeSwitchToDefaultContent();
        }

        return this;
    }

    protected <T> T getFromEditedContent(Supplier<T> supplier)
    {
        try {
            maybeSwitchToEditedContent();
            try {
                return supplier.get();
            } catch (StaleElementReferenceException e) {
                // Try again in case the edited content has been updated.
                return supplier.get();
            }
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    /**
     * Executes some code in the context of the rich text area content window.
     * 
     * @param verifier the code that verifies the content of the rich text area
     * @since 16.1.0RC1
     * @since 15.10.7
     */
    public void verifyContent(Consumer<RichTextAreaContent> verifier)
    {
        getFromEditedContent(() -> {
            verifier.accept(this.content);
            return null;
        });
    }

    /**
     * Wait until the macros present in the rich text area are rendered. Whenever a macro is insered or updated the
     * entire content is reloaded in order for the macros to be rendered server-side.
     *
     * @since 16.3.0RC1
     */
    public void waitUntilMacrosAreRendered()
    {
        getDriver().waitUntilCondition(driver -> {
            return getFromEditedContent(() -> {
                WebElement root = getRootEditableElement(false);
                if (root.getAttribute("contenteditable").equals("true")) {
                    return getDriver()
                        .findElementsWithoutWaiting(root, By.cssSelector(".macro[data-widget='xwiki-macro']")).stream()
                        .allMatch(macroWidget -> "1".equals(macroWidget.getAttribute("data-cke-widget-upcasted")));
                }
                return false;
            });
        });
    }

    /**
     * Sends the save & continue shortcut key to the text area and waits for the success notification message.
     *
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void sendSaveShortcutKey()
    {
        sendKeys(Keys.chord(Keys.ALT, Keys.SHIFT, "s"));

        // The code that waits for the save confirmation clicks on the success notification message to hide it and this
        // steals the focus from the rich text area. Unfortunately this makes Chrome lose the caret position: the next
        // sendKeys will insert the text at the end of the edited content. To avoid this, we backup the active element
        // and restore it after the save confirmation.
        WebElement activeElement;
        try {
            activeElement = getActiveElement();
        } finally {
            maybeSwitchToDefaultContent();
        }

        // This steals the focus from the rich text area because it clicks on the success notification message in order
        // to hide it.
        waitForNotificationSuccessMessage("Saved");

        // Restore the focus to the previously active element. We cannot simply focus the rich text area because the
        // previously active element might have been a nested editable area (e.g. from an inline editable macro or from
        // the image caption).
        maybeSwitchToEditedContent();
        try {
            focus(activeElement);
        } finally {
            maybeSwitchToDefaultContent();
        }
    }

    private void focus(WebElement element)
    {
        getDriver().executeScript("arguments[0].focus()", element);
    }
}

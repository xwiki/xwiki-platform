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

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.FileDetector;
import org.openqa.selenium.remote.LocalFileDetector;
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
     * The CKEditor instance that owns this rich text area.
     */
    private CKEditor editor;

    /**
     * The element that defines the rich text area.
     */
    private final WebElement container;

    private final boolean isFrame;

    private final RichTextAreaContent content = new RichTextAreaContent();

    /**
     * Holds the last known value of the refresh counter, used to detect when the content is refreshed.
     */
    private String cachedRefreshCounter;

    /**
     * Creates a new rich text area element.
     * 
     * @param editor the CKEditor instance that owns this rich text area
     * @param wait whether to wait or not for the content to be editable
     */
    public RichTextAreaElement(CKEditor editor, boolean wait)
    {
        this.editor = editor;
        this.container = editor.getContentContainer();
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
        this.cachedRefreshCounter = getRefreshCounter();
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
     * Wait for the edited content to be refreshed. Whenever a macro is inserted or updated the entire edited content is
     * reloaded in order for the macros to be rendered server-side.
     *
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void waitForContentRefresh()
    {
        waitForContentRefresh(null);
    }

    /**
     * Wait for the edited content to be refreshed. Whenever a macro is inserted or updated the entire edited content is
     * reloaded in order for the macros to be rendered server-side. This method waits:
     * <ul>
     * <li>either until the refresh counter has the expected value (this is useful when the content is refreshed
     * multiple times, at very short intervals)</li>
     * <li>or until its value is different from the cached value.</li>
     * </ul>
     *
     * @param expectedRefreshCounter the expected value of the refresh counter, or {@code null} if the refresh counter
     *            should be checked against the cached value
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void waitForContentRefresh(String expectedRefreshCounter)
    {
        getDriver().waitUntilCondition(driver -> {
            String refreshCounter = getRefreshCounter();
            // Either wait until the refresh counter has the expected value...
            if ((expectedRefreshCounter != null && Objects.equals(expectedRefreshCounter, refreshCounter))
                // ...or until its value is different from the cached value.
                || (expectedRefreshCounter == null && !Objects.equals(cachedRefreshCounter, refreshCounter))) {
                cachedRefreshCounter = refreshCounter;
                return true;
            }
            return false;
        });
    }

    /**
     * @return the current value of the refresh counter, read from the edited content.
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public String getRefreshCounter()
    {
        return getFromEditedContent(() -> getRootEditableElement(false).getAttribute("data-xwiki-refresh-counter"));
    }

    /**
     * Sends the Save &amp; Continue shortcut key to the text area and waits for the success notification message.
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

    /**
     * Simulates the user dropping a file on the rich text area.
     * 
     * @param filePath the path to the file to be dropped; the file must be located in the test resources directory
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     * @throws URISyntaxException if the specified path is not a valid (relative) URI
     */
    public void dropFile(String filePath) throws URISyntaxException
    {
        dropFile(filePath, true);
    }

    /**
     * Simulates the user dropping a file on the rich text area.
     * 
     * @param filePath the path to the file to be dropped; the file must be located in the test resources directory
     * @param wait {@code true} if the method should wait for the success notification message to be displayed
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     * @throws URISyntaxException if the specified path is not a valid (relative) URI
     */
    public void dropFile(String filePath, boolean wait) throws URISyntaxException
    {
        dropFile(loadFile(filePath));
        // Wait for the upload widget to be inserted and selected.
        waitUntilWidgetSelected();
        if (wait) {
            // Wait for the upload to finish.
            waitForOwnNotificationSuccessMessage("File successfully uploaded.");
        }
    }

    private WebElement loadFile(String filePath) throws URISyntaxException
    {
        FileDetector originalFileDetector = getDriver().getFileDetector();
        try {
            getDriver().setFileDetector(new LocalFileDetector());

            // Create the file input element.
            StringBuilder script = new StringBuilder();
            script.append("const fileInput = document.createElement('input');\n");
            script.append("fileInput.type = 'file';\n");
            script.append("document.body.append(fileInput);\n");
            script.append("return fileInput;\n");
            WebElement fileInput = (WebElement) getDriver().executeScript(script.toString());

            // Set the file input value.
            String absolutePath = Paths.get(getClass().getResource(filePath).toURI()).toFile().getAbsolutePath();
            fileInput.sendKeys(absolutePath);

            return fileInput;
        } finally {
            getDriver().setFileDetector(originalFileDetector);
        }
    }

    private void dropFile(WebElement fileInput)
    {
        StringBuilder script = new StringBuilder();
        script.append("const fileInput = arguments[0];\n");
        script.append("const editorName = arguments[1];\n");

        script.append("const file = fileInput.files[0];\n");
        script.append("const dataTransfer = new DataTransfer();\n");
        script.append("dataTransfer.items.add(file);\n");
        script.append("const dropEvent = new DragEvent('drop', {\n");
        // For the standalone edit mode the drop listener is added on the inner (iframe) document but we have to trigger
        // the drop event on a lower level (because CKEditor expects the event target to be an element not the DOM
        // document) so we need the drop event to bubble up.
        script.append("  bubbles: true,\n");
        script.append("  cancelable: true,\n");
        script.append("  dataTransfer\n");
        script.append("});\n");

        script.append("const editor = CKEDITOR.instances[editorName];\n");
        script.append("const dropTarget = CKEDITOR.plugins.clipboard.getDropTarget(editor);\n");
        // Register a drop event listener on the drop target with high priority in order to intercept the event data and
        // set the test range. The CKEditor drop handler will then use this test range to insert the dropped content.
        script.append("const handler = editor.editable().attachListener(dropTarget, 'drop', function(event) {\n");
        script.append("  handler.removeListener();\n");
        script.append("  event.data.testRange = editor.getSelection().getRanges()[0];\n");
        script.append("  return event.data;\n");
        script.append("}, null, null, 0);\n");

        script.append("editor.editable().$.dispatchEvent(dropEvent);\n");

        getDriver().executeScript(script.toString(), fileInput, this.editor.getName());
    }

    /**
     * Waits for a success notification message to be displayed for this rich text area element.
     *
     * @param message the notification message to wait for
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void waitForOwnNotificationSuccessMessage(String message)
    {
        waitForOwnNotificationMessage("success", message);
    }

    private void waitForOwnNotificationMessage(String level, String message)
    {
        String notificationMessageLocator =
            String.format("//div[contains(@class,'cke_notification_%s') and contains(., '%s')]", level, message);
        getDriver().waitUntilElementIsVisible(By.xpath(notificationMessageLocator));
        // In order to improve test speed, clicking on the notification will make it disappear. This also ensures that
        // this method always waits for the last notification message of the specified level.
        try {
            String notificationCloseLocator = notificationMessageLocator + "/a[@class = 'cke_notification_close']";
            getDriver().findElementWithoutWaiting(By.xpath(notificationCloseLocator)).click();
        } catch (WebDriverException e) {
            // The notification message may disappear before we get to click on it and thus we ignore in case there's
            // an error.
        }
    }

    /**
     * Waits until there are no upload widgets in the rich text area (upload widgets are replaced with the actual
     * content when the upload is finished).
     *
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void waitForUploadsToFinish()
    {
        getDriver().waitUntilCondition(driver -> {
            try {
                return !StringUtils.containsAny(getContent(), "cke_widget_uploadfile", "cke_widget_uploadimage");
            } catch (StaleElementReferenceException e) {
                // The edited content can be reloaded (which includes the root editable in standalone mode) while we're
                // waiting, for instance because a macro was inserted or updated as a result of a remote change.
                return false;
            }
        });
    }

    /**
     * Wait until the rich text area contains a selected widget.
     *
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    public void waitUntilWidgetSelected()
    {
        waitUntilContentContains("cke_widget_selected");
    }
}

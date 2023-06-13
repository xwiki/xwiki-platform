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

import org.openqa.selenium.JavascriptExecutor;
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
     * The in-line frame element.
     */
    private final WebElement iframe;

    /**
     * Creates a new rich text area element.
     * 
     * @param iframe the in-line frame used by the rich text area
     */
    public RichTextAreaElement(WebElement iframe)
    {
        this.iframe = iframe;
    }

    /**
     * @return the inner text of the rich text area
     */
    public String getText()
    {
        try {
            return getActiveElement().getText();
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * Clears the content of the rich text area.
     */
    public void clear()
    {
        try {
            getActiveElement().clear();
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * Clicks on the rich text area.
     */
    public void click()
    {
        try {
            getActiveElement().click();
        } finally {
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
                WebElement activeElement = getActiveElement();
                // On Firefox we have to click on the editing area before typing, otherwise the typed keys are ignored.
                // Unfortunately this can change the selection (caret position) within the editing area, but
                // since we don't expose any API to access the selection we're safe for now. Ideally we should save the
                // selection before clicking and then restore it after click, but it won't be easy to represent the
                // selection because Selenium doesn't provide an API to represent text nodes and the selection is often
                // inside a text node. We'd have to represent the selection relative to the parent element (using
                // WebElement) but that moves us away from the standard DOM selection API.
                // Also note that for some reason, using the Actions API (e.g. moving the mouse at a given offset within
                // the editing area before clicking) doesn't have the same result.
                activeElement.click();
                activeElement.sendKeys(keysToSend);
            } finally {
                getDriver().switchTo().defaultContent();
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
        try {
            getDriver().switchTo().frame(this.iframe);
            return ((JavascriptExecutor) getDriver()).executeScript(script.toString(), arguments);
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * @return the HTML content of the rich text area
     */
    public String getContent()
    {
        return (String) executeScript("return document.body.innerHTML");
    }

    /**
     * Sets the HTML content of the rich text area.
     * 
     * @param content the new HTML content
     */
    public void setContent(String content)
    {
        executeScript("document.body.innerHTML = arguments[0];", content);
    }

    /**
     * @return the HTML element that has the focus in the Rich editor
     */
    public WebElement getActiveElement()
    {
        getDriver().switchTo().frame(this.iframe);
        return getDriver().switchTo().activeElement();
    }
}

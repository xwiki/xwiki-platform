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
package org.xwiki.test.ui.po.editor.wysiwyg;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Models the rich text area of the WYSIWYG content editor.
 * 
 * @version $Id$
 * @since 3.2M3
 */
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
        String windowHandle = getDriver().getWindowHandle();
        try {
            return getActiveElement().getText();
        } finally {
            getDriver().switchTo().window(windowHandle);
        }
    }

    /**
     * Clears the content of the rich text area.
     */
    public void clear()
    {
        String windowHandle = getDriver().getWindowHandle();
        try {
            getActiveElement().clear();
        } finally {
            getDriver().switchTo().window(windowHandle);
        }
    }

    /**
     * Clicks on the rich text area.
     */
    public void click()
    {
        String windowHandle = getDriver().getWindowHandle();
        try {
            getActiveElement().click();
        } finally {
            getDriver().switchTo().window(windowHandle);
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
            String windowHandle = getDriver().getWindowHandle();
            try {
                getActiveElement().sendKeys(keysToSend);
            } finally {
                getDriver().switchTo().window(windowHandle);
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
        String windowHandle = getDriver().getWindowHandle();
        try {
            getActiveElement();
            return ((JavascriptExecutor) getDriver()).executeScript(script.toString(), arguments);
        } finally {
            getDriver().switchTo().window(windowHandle);
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
     * Sets the HTML content of the rich text area
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
    private WebElement getActiveElement()
    {
        // Switch to the iframe containing the rich text area content.
        //
        // Note: ATM this line doesn't work with Ghostdriver 1.0.3/PhantomJS 1.9, see
        // https://github.com/detro/ghostdriver/issues/226
        // Current workaround: WebDriver frameDriver = getDriver().switchTo().frame(1);
        WebDriver frameDriver = getDriver().switchTo().frame(iframe);

        // Select the locator allowing us to find the current active element (element with the focus).
        WebDriver.TargetLocator locator = frameDriver.switchTo();

        // Return the active element
        return locator.activeElement();
    }
}

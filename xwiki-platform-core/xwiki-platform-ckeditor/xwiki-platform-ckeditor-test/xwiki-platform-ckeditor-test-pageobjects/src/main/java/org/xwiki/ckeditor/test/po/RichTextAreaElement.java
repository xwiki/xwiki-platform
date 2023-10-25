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

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
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
    protected final WebElement iframe;

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

                // Calling sendKeys doesn't focus the contentEditable element on Firefox so the typed keys are simply
                // ignored. We need to focus the element ourselves before sending the keys.
                getDriver().executeScript("arguments[0].focus()", activeElement);

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
            return getDriver().executeScript(script, arguments);
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
     * Waits until the content of the rich text area contains the given HTML fragment.
     * 
     * @param html the HTML fragment to wait for
     */
    public void waitUntilContentContains(String html)
    {
        new WebDriverWait(getDriver(), Duration.ofSeconds(getDriver().getTimeout()))
            .until((ExpectedCondition<Boolean>) d -> StringUtils.contains(getContent(), html));
    }

    /**
     * @return the HTML element that has the focus in the Rich editor
     */
    private WebElement getActiveElement()
    {
        getDriver().switchTo().frame(this.iframe);
        return getDriver().switchTo().activeElement();
    }

    /**
     * Waits until the content is editable.
     *
     * @since 15.5.1
     * @since 15.6RC1
     */
    public void waitUntilContentEditable()
    {
        try {
            getDriver().switchTo().frame(this.iframe);
            getDriver().waitUntilElementHasAttributeValue(By.className("cke_editable"), "contenteditable", "true");
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }

    /**
     * @return the scroll top position of the rich text area
     */
    public int getScrollTop()
    {
        try {
            getDriver().switchTo().frame(this.iframe);
            return Integer
                .parseInt(getDriver().findElementWithoutWaiting(By.tagName("html")).getDomProperty("scrollTop"));
        } finally {
            getDriver().switchTo().defaultContent();
        }
    }
}

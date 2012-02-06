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

import org.openqa.selenium.By;
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
        getDriver().switchTo().frame(iframe);

        String content = getDriver().findElement(By.id("body")).getText();

        getDriver().switchTo().window(windowHandle);
        return content;
    }

    /**
     * Clears the content of the rich text area.
     */
    public void clear()
    {
        String windowHandle = getDriver().getWindowHandle();
        getDriver().switchTo().frame(iframe);

        executeJavascript("document.body.innerHTML = ''");

        getDriver().switchTo().window(windowHandle);
    }

    /**
     * Simulate typing in the rich text area.
     * 
     * @param keysToSend the sequence of keys to by typed
     */
    public void sendKeys(CharSequence... keysToSend)
    {
        if (keysToSend.length == 0) {
            return;
        }

        String windowHandle = getDriver().getWindowHandle();
        getDriver().switchTo().frame(iframe);

        // FIXME: The following JavaScript code is a temporary workaround for
        // http://code.google.com/p/selenium/issues/detail?id=2331 (sendKeys doesn't work on BODY element with
        // contentEditable=true).
        StringBuilder script = new StringBuilder();

        // Save the selection.
        script.append("window.__savedRange = window.getSelection().getRangeAt(0);\n");

        // Make the BODY element read-only.
        script.append("document.body.contentEditable = false;\n");

        // Wrap all content with a contentEditable DIV.
        script.append("var div = document.createElement('div');\n");
        // Make sure the DIV is visible (this is for the case when the rich text area is empty).
        script.append("div.style.minHeight = '20px';");
        script.append("document.body.appendChild(div);\n");
        script.append("var contentRange = document.createRange();\n");
        script.append("contentRange.setStartBefore(document.body.firstChild);\n");
        script.append("contentRange.setEndBefore(div);\n");
        script.append("div.appendChild(contentRange.extractContents());\n");
        script.append("div.contentEditable = true;\n");

        // Restore the selection.
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(window.__savedRange);\n");
        script.append("window.__savedRange = undefined;\n");
        executeJavascript(script.toString());

        getDriver().findElement(By.xpath("//body/div")).sendKeys(keysToSend);

        // Save the selection.
        script.delete(0, script.length());
        script.append("window.__savedRange = window.getSelection().getRangeAt(0);\n");

        // Unwrap the content.
        script.append("var contentRange = document.createRange();\n");
        script.append("contentRange.selectNodeContents(document.body.firstChild);\n");
        script.append("document.body.appendChild(contentRange.extractContents());\n");
        script.append("document.body.removeChild(document.body.firstChild);\n");

        // Restore contentEditable on the BODY element.
        script.append("document.body.contentEditable = true;\n");

        // Restore the selection.
        script.append("window.getSelection().removeAllRanges();\n");
        script.append("window.getSelection().addRange(window.__savedRange);\n");
        script.append("window.__savedRange = undefined;\n");
        executeJavascript(script.toString());

        getDriver().switchTo().window(windowHandle);
    }
}

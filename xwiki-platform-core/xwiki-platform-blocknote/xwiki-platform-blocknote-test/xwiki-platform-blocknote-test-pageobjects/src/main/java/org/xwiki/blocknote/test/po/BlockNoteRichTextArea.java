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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.xwiki.test.ui.po.BaseElement;

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
     * Clears the content of the rich text area.
     */
    public void clear()
    {
        this.container.clear();
    }

    /**
     * Clicks on the rich text area.
     */
    public void click()
    {
        this.container.click();
    }

    /**
     * Simulate typing in the rich text area.
     * 
     * @param keysToSend the sequence of keys to by typed
     */
    public void sendKeys(CharSequence... keysToSend)
    {
        if (keysToSend.length > 0) {
            getActiveElement().sendKeys(keysToSend);
        }
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
}

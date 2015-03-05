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
package org.xwiki.test.ui.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Represents the form used to add, edit or reply to a comment.
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class CommentForm extends BaseElement
{
    /**
     * The locator to the form.
     */
    private By containerLocator;

    /**
     * Creates a new form instance.
     * 
     * @param containerLocator the locator to the form
     */
    public CommentForm(By containerLocator)
    {
        this.containerLocator = containerLocator;
    }

    private WebElement getContainer()
    {
        return getDriver().findElement(this.containerLocator);
    }

    /**
     * @return the field used to input the comment content
     */
    public WebElement getContentField()
    {
        return getContainer().findElement(By.tagName("textarea"));
    }

    /**
     * Clicks on the preview button and waits for the preview to be ready.
     * 
     * @return the element that wraps the content preview
     */
    public WebElement clickPreview()
    {
        getContainer().findElement(By.xpath(".//input[@type = 'button' and @value = 'Preview']")).click();
        By previewLocator = By.className("commentPreview");
        getDriver().waitUntilElementIsVisible(previewLocator);
        return getContainer().findElement(previewLocator);
    }

    /**
     * Clicks on the back button to cancel the preview and show the content text area
     */
    public void clickBack()
    {
        getContainer().findElement(By.xpath(".//input[@type = 'button' and @value = 'Back']")).click();
    }

    /**
     * Clicks on the submit button and waits for the operation to take place. The effect depends on the actual form. It
     * could add a new comment, a reply to an existing comment or update an existing comment.
     */
    public void clickSubmit()
    {
        clickSubmit(true);
    }

    /**
     * Clicks on the submit button and optionally waits for the operation to take place. The effect depends on the
     * actual form. It could add a new comment, a reply to an existing comment or update an existing comment.
     * <p>
     * Note: Use this method when JavaScript is disabled and the submit is not done asynchronously.
     * 
     * @param wait {@code true} to wait for the success notification, {@code false} otherwise
     */
    public void clickSubmit(boolean wait)
    {
        getContainer().findElement(By.xpath(".//input[@type = 'submit']")).click();
        if (wait) {
            // The submit is done asynchronously so we have to wait for the success notification.
            waitForNotificationSuccessMessage("Comment posted");
        }
    }

    /**
     * Clicks on the cancel button.
     */
    public void clickCancel()
    {
        getContainer().findElement(By.className("cancel")).click();
    }
}

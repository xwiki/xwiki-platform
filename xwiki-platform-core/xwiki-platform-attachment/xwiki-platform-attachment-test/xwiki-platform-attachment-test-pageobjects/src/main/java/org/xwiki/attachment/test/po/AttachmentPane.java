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
package org.xwiki.attachment.test.po;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.flamingo.skin.test.po.AttachmentsPane;
import org.xwiki.test.ui.po.SuggestInputElement;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page object for the move attachment form.
 *
 * @version $Id$
 * @since 14.0RC1
 */
public class AttachmentPane extends ViewPage
{
    /**
     * Click on the move button for the given attachment.
     *
     * @param attachmentName the name of the attachment to move
     * @return the move attachment form page object
     */
    public static AttachmentPane moveAttachment(String attachmentName)
    {
        AttachmentsPane attachmentsPane = new AttachmentsPane();
        if (!attachmentsPane.attachmentExistsByFileName(attachmentName)) {
            throw new RuntimeException("Attachment " + attachmentName + " not found in the attachments pane.");
        }

        attachmentsPane.getAttachmentMoveElement(attachmentName).click();

        return new AttachmentPane();
    }

    /**
     * @param name the new name of the attachment after the move
     */
    public void setName(String name)
    {
        WebElement nameField = getDriver().findElementWithoutWaiting(By.id("targetAttachmentNameTitle"));
        nameField.clear();
        nameField.sendKeys(name);
    }

    /**
     * @param redirect when {@code true}, a redirection object will be persisted in the source page
     */
    public void setRedirect(boolean redirect)
    {
        WebElement redirectCheckbox =
            getDriver().findElementWithoutWaiting(By.cssSelector("input[type='checkbox'][name='autoRedirect']"));
        boolean selected = redirectCheckbox.isSelected();
        if ((selected && !redirect) || (!selected && redirect)) {
            redirectCheckbox.click();
        }
    }

    /**
     * @param location the new location of the attachment (for instance, "xwiki:Space.Page")
     */
    public void setLocation(String location)
    {
        WebElement element = getDriver().findElementWithoutWaiting(By.cssSelector("#targetLocation"));
        SuggestInputElement suggestInputElement = new SuggestInputElement(element);
        suggestInputElement.clearSelectedSuggestions().sendKeys(location).selectTypedText();
    }

    /**
     * Click on the submit button for the form.
     */
    public void submit()
    {
        getDriver().findElementWithoutWaiting(By.cssSelector(".buttons input")).click();
    }

    /**
     * Wait for the move job to finish successfully.
     */
    public void waitForJobDone()
    {
        getDriver().waitUntilCondition(
            driver -> !getDriver().findElementsWithoutWaiting(By.cssSelector(".box.successmessage")).isEmpty());
    }
}

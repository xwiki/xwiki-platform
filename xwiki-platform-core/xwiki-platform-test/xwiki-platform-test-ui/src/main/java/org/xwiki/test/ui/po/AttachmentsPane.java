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

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;

/**
 * Represents the actions possible on the Attachment Pane at the bottom of a page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class AttachmentsPane extends BaseElement
{
    @FindBy(id = "Attachmentspane")
    private WebElement pane;

    @FindBy(xpath = "//input[@value='Add another file']")
    private WebElement addAnotherFile;

    private ConfirmationModal confirmDelete;

    public boolean isOpened()
    {
        return getDriver().findElementWithoutWaiting(By.id("attachmentscontent")).isDisplayed();
    }

    /**
     * Fills the URL with the specified file path.
     * 
     * @param filePath the path to the file to upload in URL form (the file *must* exist in the target directory).
     */
    public void setFileToUpload(final String filePath)
    {
        final List<WebElement> inputs = this.pane.findElements(By.className("uploadFileInput"));
        WebElement input = inputs.get(inputs.size() - 1);
        // Clean the field before setting the value in case of successive uploads.
        input.clear();
        input.sendKeys(filePath);
    }

    public void waitForUploadToFinish(String fileName)
    {
        waitForNotificationSuccessMessage("Attachment uploaded: " + fileName);
    }

    public void clickHideProgress()
    {
        this.pane.findElement(By.xpath("//a[text()='Hide upload status']")).click();
    }

    /**
     * Adds another input field for attaching a file.
     */
    public void addAnotherFile()
    {
        this.addAnotherFile.click();
    }

    public void clickAttachFiles()
    {
        this.pane
            .findElement(By.xpath(
                "//form[@id='AddAttachment']//input[@class='button' and @type='submit' and " + "@value='Attach']"))
            .click();
    }

    /**
     * Return the {@code a} tag of an attachment link according to its name.
     *
     * @param attachmentName the name of the attachment (for instance {@code "my_doc.txt"})
     * @return the {@link WebElement} of the {@code a} tag with the requested name
     * @since 3.2M3
     */
    public WebElement getAttachmentLink(String attachmentName)
    {
        By attachementLinkSelector = By.xpath(
            String.format("//div[@id='_attachments']//a[@title = 'Download this attachment' and contains(@href, '%s')]",
                attachmentName));
        // Make sure that the element is visible and can be clicked before returning it to prevent interacting too 
        // early with the attachment link.
        getDriver().waitUntilCondition(elementToBeClickable(attachementLinkSelector));
        return getDriver().findElementWithoutWaiting(attachementLinkSelector);
    }

    /**
     * Deletes the corresponding file name.
     * 
     * @param attachmentName the name of the attachment to be deleted
     */
    public void deleteAttachmentByFileByName(String attachmentName)
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.id("deleteAttachment"));
        getDriver().findElement(By.xpath("//div[@id='attachmentscontent']//a[text()='" + attachmentName
            + "']/../../div[contains(@class, 'xwikibuttonlinks')]/a[contains(@class,'deletelink')]")).click();
        this.confirmDelete.clickOk();
        getDriver().waitUntilElementDisappears(
            By.xpath("//div[@id='attachmentscontent']//a[text()='" + attachmentName + "']"));
        getDriver().waitUntilElementIsVisible(By.xpath("//div[@id='Attachmentspane']"));
    }

    /**
     * Deletes the first attachment.
     */
    public void deleteFirstAttachment()
    {
        String tmp = getDriver()
            .findElement(By.xpath("//div[@id='_attachments']/*[1]/div[@class='information']/span[@class='name']"))
            .getText();
        getDriver()
            .findElement(By
                .xpath("//div[@id='attachmentscontent']//a[text()='" + tmp + "']/../../span[2]/a[@class='deletelink']"))
            .click();

        getDriver().findElement(By.xpath("//*[@class='xdialog-modal-container']//input[@value='Yes']")).click();
        getDriver().waitUntilElementDisappears(
            By.xpath("//*[@class='xdialog-modal-container']/*[contains(@class, 'xdialog-box-confirmation')]"));
        getDriver().waitUntilElementDisappears(
            By.xpath("//div[@id='attachmentscontent']//a[text()='" + tmp + "']/../../span[2]/a[@class='deletelink']"));
    }

    /**
     * @return the number of attachments in this document.
     */
    public int getNumberOfAttachments()
    {
        By countLocator = By.cssSelector("#Attachmentstab .itemCount");
        return Integer.parseInt(getDriver().findElement(countLocator).getText().replaceAll("[()]", ""));
    }

    /**
     * Deletes ALL the attached files
     */
    public void deleteAllAttachments()
    {
        while (this.getNumberOfAttachments() > 0) {
            this.deleteFirstAttachment();
        }
    }

    public String getUploaderOfAttachment(String attachmentName)
    {
        return getDriver().findElement(By.xpath("//div[@id='attachmentscontent']//a[text()='" + attachmentName
            + "']/../../div[@class='meta']/span[@class='publisher']/span[@class='wikilink']")).getText();
    }

    /**
     * Return the version number for the requested attachment.
     *
     * @param attachmentName the name of the attachment
     * @return the version number displayed for the attachment
     */
    public String getLatestVersionOfAttachment(String attachmentName)
    {
        return getAttachmentVersionElement(attachmentName).getText();
    }

    /**
     * Click on the attachment history link for a given attachment.
     *
     * @param attachmentName the name of the attachment (e.g., "myfile.txt")
     * @return the attachment history page object
     * @since 14.2RC1
     */
    public AttachmentHistoryPage goToAttachmentHistory(String attachmentName)
    {
        getAttachmentVersionElement(attachmentName).click();
        return new AttachmentHistoryPage();
    }

    public String getSizeOfAttachment(String attachmentName)
    {
        return getDriver().findElement(By.xpath("//div[@id='attachmentscontent']//a[text()='" + attachmentName
            + "']/../../div[@class='meta']/span[@class='size']")).getText().replaceAll("[()]", "");

    }

    public String getDateOfLastUpload(String attachmentName)
    {
        return getDriver().findElement(By.xpath("//div[@id='attachmentscontent']//a[text()='" + attachmentName
            + "']/../../div[@class='meta']/span[@class='date']")).getText().replaceFirst("on", "");
    }

    public boolean attachmentExistsByFileName(String attachmentName)
    {
        try {
            getDriver()
                .findElement(By.xpath("//a[@title = 'Download this attachment' and . = '" + attachmentName + "']"));
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    private WebElement getAttachmentVersionElement(String attachmentName)
    {
        return getDriver()
            .findElement(By.xpath(
                "//div[@id='attachmentscontent']//a[text()= '" + attachmentName + "']/../../span[@class='version']/a"));
    }
}

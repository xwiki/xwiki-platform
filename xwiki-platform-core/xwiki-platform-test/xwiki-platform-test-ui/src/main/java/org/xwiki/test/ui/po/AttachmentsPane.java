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

import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedCondition;

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

    private LiveTableElement attachmentsLivetable;

    private String livetableId;

    private ConfirmationModal confirmDelete;

    /*
     * List of attachments livetable column labels.
     */
    private static final List<String> LIVETABLE_COLUMNS =
        Arrays.asList("mimeType", "filename", "filesize", "date", "author");

    /**
     * Default constructor.
     *
     * @since 14.6RC1
     */
    public AttachmentsPane()
    {
        this.livetableId = "docAttachments";
        this.attachmentsLivetable = new LiveTableElement(this.livetableId);
    }

    /**
     * @param livetableId the id of the attachments livetable
     * @since 14.6RC1
     */
    public AttachmentsPane(String livetableId)
    {
        this.livetableId = livetableId;
        this.attachmentsLivetable = new LiveTableElement(this.livetableId);
    }

    public boolean isOpened()
    {
        return this.attachmentsLivetable.isReady();
    }

    /**
     * Wait for the attachments livetable to be ready.
     *
     * @since 14.6RC1
     */
    public void waitForAttachmentsLivetable()
    {
        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return attachmentsLivetable.isReady();
            }
        });
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
     * Get the row index of an attachment knowing the filename.
     *
     * @param attachmentName the name of the attachment
     * @return the row index of the searched attachment
     * @since 14.6RC1
     */
    public int getRowIndexByAttachmentName(String attachmentName)
    {
        return this.attachmentsLivetable.getRowNumberForElement(By.xpath("//a[text()='" + attachmentName + "']"));
    }

    /**
     * Get the filename of an attachment knowing the index in the attachments livetable.
     *
     * @param positionNumber the index of the attachment in the livetable
     * @return the filename of the attachment
     * @since 14.6RC1
     */
    public String getAttachmentNameByPosition(int positionNumber)
    {
        return this.attachmentsLivetable.getCell(positionNumber, 2).findElement(By.className("name")).getText();
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
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 2)
            .findElement(By.className("name")).findElement(By.tagName("a"));
    }

    /**
     * Deletes the corresponding file name.
     * 
     * @param attachmentName the name of the attachment to be deleted
     */
    public void deleteAttachmentByFileByName(String attachmentName)
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.xpath(".//table[@id='" + this.livetableId
            + "']/parent::div/following-sibling::div[contains(@class, 'deleteAttachment')]"));
        WebElement deleteButton = this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 6)
            .findElement(By.className("actiondelete"));
        // This is needed since Selenium deleteButton.click() fails on smaller windows, even if the element is
        // clickable (visible and enabled). The failure was reproduced on org.xwiki.test.ui.CompareVersionsTest and
        // might be resolved after moving the test to docker.
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", deleteButton);
        this.confirmDelete.clickOk();

        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    return !confirmDelete.isDisplayed() && !deleteButton.isDisplayed();
                } catch (StaleElementReferenceException e) {
                    return true;
                }
            }
        });
    }

    /**
     * Deletes the first attachment.
     */
    public void deleteFirstAttachment()
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.xpath(".//table[@id='" + this.livetableId
            + "']/parent::div/following-sibling::div[contains(@class, 'deleteAttachment')]"));
        WebElement deleteButton = this.attachmentsLivetable.getCell(1, 6).findElement(By.className("actiondelete"));
        deleteButton.click();
        this.confirmDelete.clickOk();

        getDriver().waitUntilCondition(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try {
                    return !confirmDelete.isDisplayed() && !deleteButton.isDisplayed();
                } catch (StaleElementReferenceException e) {
                    return true;
                }
            }
        });

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
     * Get the number of displayed attachments, regardless of the total number.
     *
     * @return the number of attachments displayed.
     * @since 14.6RC1
     */
    public int getNumberOfAttachmentsDisplayed()
    {
        return this.attachmentsLivetable.getRowCount();
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
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 5).getText();
    }

    /**
     * Return the version number for the requested attachment.
     *
     * @param attachmentName the name of the attachment
     * @return the version number displayed for the attachment
     */
    public String getLatestVersionOfAttachment(String attachmentName)
    {
        return this.getAttachmentVersionElement(attachmentName).getText();
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
        this.getAttachmentVersionElement(attachmentName).click();
        return new AttachmentHistoryPage();
    }

    public String getSizeOfAttachment(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 3).getText();

    }

    public String getDateOfLastUpload(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 4).getText();
    }

    /**
     * Check if the attachment exists for the currently displayed rows.
     *
     * @param attachmentName the name of the searched attachment
     * @return whether the attachment exists or not in the list of currently displayed attachments
     * @since 14.6RC1
     */
    public boolean attachmentIsDisplayedByFileName(String attachmentName)
    {
        try {
            getDriver().findElement(By
                .xpath("//tbody[@id='" + this.livetableId + "-display']//tr[td//a[text()='" + attachmentName + "']]"));
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }

    /**
     * Check if the attachment exists, regardless of the page where it is displayed.
     *
     * @param attachmentName the name of the searched attachment
     * @return whether the attachments exists or not in the list of all attachments
     */
    public boolean attachmentExistsByFileName(String attachmentName)
    {
        this.filterColumn("filename", attachmentName);
        boolean attachmentExists = this.attachmentIsDisplayedByFileName(attachmentName);
        this.filterColumn("filename", "");

        return attachmentExists;
    }

    /**
     * @param attachmentName the name of the targeted attachment
     * @return the button to trigger the attachment move action
     * @since 14.6RC1
     */
    public WebElement getAttachmentMoveElement(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 6)
            .findElement(By.className("move-attachment"));
    }

    private WebElement getAttachmentVersionElement(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowIndexByAttachmentName(attachmentName), 2)
            .findElement(By.className("version"));
    }

    /**
     * Set the value in the filter of a column.
     *
     * @param columnLabel the label of the column to filter
     * @param filterValue the value to filter by
     * @since 14.6RC1
     */
    public void filterColumn(String columnLabel, String filterValue)
    {
        int index = LIVETABLE_COLUMNS.indexOf(columnLabel) + 1;
        this.attachmentsLivetable.filterColumn("xwiki-livetable-" + this.livetableId + "-filter-" + index, filterValue);
    }
}

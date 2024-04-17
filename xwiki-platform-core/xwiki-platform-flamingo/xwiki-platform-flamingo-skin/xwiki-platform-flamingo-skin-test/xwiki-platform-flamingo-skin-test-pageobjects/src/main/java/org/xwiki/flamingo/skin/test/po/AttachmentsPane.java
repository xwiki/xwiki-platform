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
package org.xwiki.flamingo.skin.test.po;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.test.ui.po.AttachmentHistoryPage;
import org.xwiki.test.ui.po.BaseElement;
import org.xwiki.test.ui.po.ConfirmationModal;

import static org.xwiki.livedata.test.po.TableLayoutElement.FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS;

/**
 * Represents the actions possible on the Attachment Pane at the bottom of a page.
 *
 * @version $Id$
 * @since 3.2M3
 */
public class AttachmentsPane extends BaseElement
{
    /*
     * List of attachments liveData column labels.
     */
    private static final List<String> LIVEDATA_COLUMNS =
        Arrays.asList("Type", "Name", "File size", "Date", "Posted by", "Actions");

    private static final String CLASS_NAME = "name";

    private static final String ACTION_DELETE = "actiondelete";

    @FindBy(id = "Attachmentspane")
    private WebElement pane;

    @FindBy(xpath = "//input[@value='Add another file']")
    private WebElement addAnotherFile;

    private LiveDataElement attachmentsLiveData;

    private TableLayoutElement attachmentsTableLayoutElement;

    private String liveDataId;

    private ConfirmationModal confirmDelete;

    /**
     * Default constructor.
     *
     * @since 14.6RC1
     */
    public AttachmentsPane()
    {
        this.liveDataId = "docAttachments";
        this.attachmentsLiveData = new LiveDataElement(this.liveDataId);
        this.attachmentsTableLayoutElement = this.attachmentsLiveData.getTableLayout();
    }

    /**
     * @param liveDataId the id of the attachments liveData
     * @since 14.6RC1
     */
    public AttachmentsPane(String liveDataId)
    {
        this.liveDataId = liveDataId;
        this.attachmentsLiveData = new LiveDataElement(this.liveDataId);
        this.attachmentsTableLayoutElement = this.attachmentsLiveData.getTableLayout();
    }

    /**
     * @return {@code true} if the attachments pane is fully loaded and ready to use, {@code false} otherwise
     */
    public boolean isOpened()
    {
        return this.attachmentsLiveData.isReady();
    }

    /**
     * Wait for the attachments liveData to be ready.
     *
     * @since 14.6RC1
     */
    public void waitForAttachmentsLiveData()
    {
        this.attachmentsTableLayoutElement.waitUntilReady();
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

    /**
     * Fills the upload comment.
     *
     * @param comment the upload comment.
     * @since 16.3.0RC1
     */
    public void setUploadComment(final String comment)
    {
        final List<WebElement> inputs = this.pane.findElements(By.id("xwikiuploadcomment"));
        WebElement input = inputs.get(inputs.size() - 1);
        // Clean the field before setting the value in case of successive uploads.
        input.clear();
        input.sendKeys(comment);
    }

    /**
     * Fills the URL with the specified file paths.
     *
     * @param filePaths a list of paths to the files to upload in URL form (the files *must* exist in the target
     *     directory).
     * @since 14.10.2
     * @since 15.0RC1
     */
    public void setFilesToUpload(List<String> filePaths)
    {
        setFileToUpload(StringUtils.join(filePaths, System.lineSeparator()));
    }

    /**
     * Wait for the upload of a specific file to be finished.
     *
     * @param fileName the name of the attachment
     */
    public void waitForUploadToFinish(String fileName)
    {
        waitForNotificationSuccessMessage("Attachment uploaded: " + fileName);
    }

    /**
     * Close the progress displayed after uploading one or multiple files.
     */
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

    /**
     * Open a file picker dialog.
     */
    public void clickAttachFiles()
    {
        this.pane
            .findElement(By.xpath(
                "//form[@id='AddAttachment']//input[@class='button' and @type='submit' and " + "@value='Attach']"))
            .click();
    }

    /**
     * Get the row index of an attachment knowing the filename, starting with position 1.
     *
     * @param attachmentName the name of the attachment
     * @return the 1-based row index of the searched attachment, or 0 if it doesn't exist
     * @since 14.6RC1
     */
    public int getRowIndexByAttachmentName(String attachmentName)
    {
        return this.attachmentsTableLayoutElement.getRowIndexForElement(
            By.xpath("//a[text()='" + attachmentName + "']"));
    }

    /**
     * Get the filename of an attachment knowing the index in the attachments liveData.
     *
     * @param index the index of the attachment in the list of liveData displayed entries
     * @return the filename of the attachment
     * @since 14.6RC1
     */
    public String getAttachmentNameByIndex(int index)
    {
        return this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(1), index)
            .findElement(By.className(CLASS_NAME)).getText();
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
        return this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(1),
                getRowIndexByAttachmentName(attachmentName)).findElement(By.className(CLASS_NAME))
            .findElement(By.tagName("a"));
    }

    /**
     * Deletes the corresponding file name.
     *
     * @param attachmentName the name of the attachment to be deleted
     */
    public void deleteAttachmentByFileByName(String attachmentName)
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.xpath(
            ".//div[@id='" + this.liveDataId + "']/following-sibling::div[contains(@class, 'deleteAttachment')]"));
        WebElement deleteButton = this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(5),
            getRowIndexByAttachmentName(attachmentName)).findElement(By.className(ACTION_DELETE));
        // This is needed since Selenium deleteButton.click() fails on smaller windows, even if the element is
        // clickable (visible and enabled). The failure was reproduced on org.xwiki.test.ui.CompareVersionsTest and
        // might be resolved after moving the test to docker.
        ((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", deleteButton);
        this.confirmDelete.clickOk();

        getDriver().waitUntilCondition(driver -> {
            try {
                return !confirmDelete.isDisplayed() && !deleteButton.isDisplayed();
            } catch (StaleElementReferenceException e) {
                return true;
            }
        });
    }

    /**
     * Deletes the first attachment.
     */
    public void deleteFirstAttachment()
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.xpath(".//table[@id='" + this.liveDataId
            + "']/parent::div/following-sibling::div[contains(@class, 'deleteAttachment')]"));
        WebElement deleteButton = this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(5), 1)
            .findElement(By.className(ACTION_DELETE));
        deleteButton.click();
        this.confirmDelete.clickOk();

        getDriver().waitUntilCondition(driver -> {
            try {
                return !confirmDelete.isDisplayed() && !deleteButton.isDisplayed();
            } catch (StaleElementReferenceException e) {
                return true;
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
        return this.attachmentsTableLayoutElement.countRows();
    }

    /**
     * Deletes ALL the attached files.
     */
    public void deleteAllAttachments()
    {
        while (this.getNumberOfAttachments() > 0) {
            this.deleteFirstAttachment();
        }
    }

    /**
     * @param attachmentName the name of the attachment
     * @return the user that uploaded the attachment
     */
    public String getUploaderOfAttachment(String attachmentName)
    {
        return this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(4),
            getRowIndexByAttachmentName(attachmentName)).getText();
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

    /**
     * @param attachmentName the name of the attachment
     * @return the size of the attachment
     */
    public String getSizeOfAttachment(String attachmentName)
    {
        return this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(2),
            getRowIndexByAttachmentName(attachmentName)).getText();
    }

    /**
     * @param attachmentName the name of the attachment
     * @return the date when the attachment was uploaded / modified last time
     */
    public String getDateOfLastUpload(String attachmentName)
    {
        return this.attachmentsTableLayoutElement.getCell(LIVEDATA_COLUMNS.get(3),
            getRowIndexByAttachmentName(attachmentName)).getText();
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
        // The returned index is 1-based.
        return getRowIndexByAttachmentName(attachmentName) != 0;
    }

    /**
     * Check if the attachment exists, regardless of the page where it is displayed.
     *
     * @param attachmentName the name of the searched attachment
     * @return whether the attachments exists or not in the list of all attachments
     */
    public boolean attachmentExistsByFileName(String attachmentName)
    {
        this.filterColumn(2, attachmentName);
        boolean attachmentExists = this.attachmentIsDisplayedByFileName(attachmentName);
        this.filterColumn(2, " ");

        return attachmentExists;
    }

    /**
     * @param attachmentName the name of the targeted attachment
     * @return the button to trigger the attachment move action
     * @since 14.6RC1
     */
    public WebElement getAttachmentMoveElement(String attachmentName)
    {
        return this.attachmentsTableLayoutElement
            .getCell(LIVEDATA_COLUMNS.get(5), getRowIndexByAttachmentName(attachmentName))
            .findElement(By.className("move-attachment"));
    }

    private WebElement getAttachmentVersionElement(String attachmentName)
    {
        return this.attachmentsTableLayoutElement
            .getCell(LIVEDATA_COLUMNS.get(1), getRowIndexByAttachmentName(attachmentName))
            .findElement(By.className("version"));
    }

    /**
     * Set the value in the filter of a column and wait for the results to be displayed.
     *
     * @param columnIndex the 1-based index of the column to filter
     * @param filterValue the value to filter by
     * @since 14.6RC1
     */
    public void filterColumn(int columnIndex, String filterValue)
    {
        this.attachmentsTableLayoutElement.filterColumn(LIVEDATA_COLUMNS.get(columnIndex - 1), filterValue, true,
            Map.of(FILTER_COLUMN_SELECTIZE_WAIT_FOR_SUGGESTIONS, true));
    }
}

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

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.openqa.selenium.By;
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

    private ConfirmationModal confirmDelete;

    AttachmentsPane()
    {
        this.attachmentsLivetable = new LiveTableElement("docAttachments");
    }

    public boolean isOpened()
    {
        return this.attachmentsLivetable.isReady();
    }

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

    public int getRowNumberByAttachmentName(String attachmentName)
    {
        return this.attachmentsLivetable.getRowNumberForElement(By.xpath("//a[text()='" + attachmentName + "']"));
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
        return this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 2)
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
        this.confirmDelete = new ConfirmationModal(By.id("deleteAttachment"));
        WebElement deleteButton = this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 6)
            .findElement(By.className("actiondelete"));
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
     * Deletes the first attachment.
     */
    public void deleteFirstAttachment()
    {
        // We initialize before so we can remove the animation before the modal is shown
        this.confirmDelete = new ConfirmationModal(By.id("deleteAttachment"));
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
        return this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 5).getText();
    }

    /**
     * Return the version number for the requested attachment.
     *
     * @param attachmentName the name of the attachment
     * @return the version number displayed in the attachment URL
     */
    public String getLatestVersionOfAttachment(String attachmentName) throws Exception
    {
        URL url = new URL(getAttachmentLink(attachmentName).getAttribute("href"));
        List<NameValuePair> params =
            new ArrayList<NameValuePair>(URLEncodedUtils.parse(url.getQuery(), Charset.forName("UTF-8")));

        return params.stream().filter(p -> p.getName().equals("rev")).findFirst().get().getValue();
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
        this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 6)
            .findElement(By.className("history")).click();
        return new AttachmentHistoryPage();
    }

    public String getSizeOfAttachment(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 3).getText();

    }

    public String getDateOfLastUpload(String attachmentName)
    {
        return this.attachmentsLivetable.getCell(getRowNumberByAttachmentName(attachmentName), 4).getText();
    }

    public boolean attachmentExistsByFileName(String attachmentName)
    {
        try {
            getDriver().findElement(
                By.xpath("//tbody[@id='docAttachments-display']//tr[td//a[text()='" + attachmentName + "']]"));
        } catch (NoSuchElementException e) {
            return false;
        }
        return true;
    }
}

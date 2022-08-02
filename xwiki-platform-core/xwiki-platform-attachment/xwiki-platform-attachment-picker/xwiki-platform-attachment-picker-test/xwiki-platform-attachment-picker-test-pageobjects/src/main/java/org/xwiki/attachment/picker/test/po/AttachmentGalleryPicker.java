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
package org.xwiki.attachment.picker.test.po;

import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Page object for an attachment picker.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class AttachmentGalleryPicker extends BaseElement
{
    private WebElement attachmentGalleryPickerElement;

    /**
     * Default constructor.
     *
     * @param id the id of the attachment picker
     */
    public AttachmentGalleryPicker(String id)
    {
        this.attachmentGalleryPickerElement = getDriver().findElement(By.id(id));
    }

    /**
     * Wait until the picker is ready.
     *
     * @return the current page object
     */
    public AttachmentGalleryPicker waitUntilReady()
    {
        // The picker is ready when the loading class is not present.
        WebElement attachmentPickerResults =
            this.attachmentGalleryPickerElement.findElement(By.className("attachmentPickerResults"));
        getDriver().waitUntilCondition(driver -> !attachmentPickerResults.getAttribute("class").contains("loading"));
        return this;
    }

    /**
     * @return the list of the titles of the attachments
     */
    public List<String> getAttachmentTitles()
    {
        return getAllAttachments().stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    /**
     * Define the search query of the attachment picker.
     *
     * @param searchQuery the search query, for instance the name of an attachment
     * @return the current page object
     */
    public AttachmentGalleryPicker setSearch(String searchQuery)
    {
        WebElement input =
            this.attachmentGalleryPickerElement.findElement(By.cssSelector(".attachmentPickerSearch input"));
        input.clear();
        input.sendKeys(searchQuery);
        return this;
    }

    /**
     * Wait until the expected number of attachments is displayed in the picker.
     *
     * @param expectedCount the expected number of attachments
     */
    public void waitUntilAttachmentsCount(int expectedCount)
    {
        // Waits for the search to be finished before counting the number of results.
        waitUntilReady();
        getDriver().waitUntilCondition(driver -> getAllAttachments().size() == expectedCount);
    }

    /**
     * Wait until the not result warning message is displayed.
     */
    public void waitNoResultMessageDisplayed()
    {
        getDriver().waitUntilElementIsVisible(this.attachmentGalleryPickerElement,
            By.className("attachmentPickerNoResults"));
    }

    private List<WebElement> getAllAttachments()
    {
        return this.attachmentGalleryPickerElement.findElements(By.className("attachmentTitle"));
    }
}

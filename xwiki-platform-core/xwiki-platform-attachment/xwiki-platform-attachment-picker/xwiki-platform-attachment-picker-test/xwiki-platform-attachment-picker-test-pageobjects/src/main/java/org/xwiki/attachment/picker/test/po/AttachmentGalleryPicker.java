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
import java.util.Optional;
import java.util.function.IntPredicate;
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
        WebElement attachmentPickerResults = getAttachmentPickerResults();
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
        WebElement input = getSearchBlock()
            .findElement(By.cssSelector("input"));
        input.clear();
        input.sendKeys(searchQuery);
        return this;
    }

    /**
     * Wait until the expected number of attachments is displayed in the picker.
     *
     * @param predicate a boolean predicate taking the current attachments count in parameter
     */
    public void waitUntilAttachmentsCount(IntPredicate predicate)
    {
        // Waits for the search to be finished before counting the number of results.
        waitUntilReady();
        getDriver().waitUntilCondition(driver -> predicate.test(getAllAttachments().size()));
    }

    /**
     * Wait until the not result warning message is displayed.
     */
    public void waitNoResultMessageDisplayed()
    {
        getDriver().waitUntilElementIsVisible(this.attachmentGalleryPickerElement,
            By.className("attachmentPickerNoResults"));
    }

    /**
     * Click on the "All Pages" search button to return search results from the whole farm.
     *
     * @return the current page object
     * @since 14.10.1
     * @since 15.0RC1
     */
    public AttachmentGalleryPicker toggleAllPages()
    {
        getSearchBlock().findElement(By.cssSelector("label:nth-child(2)")).click();
        return this;
    }

    /**
     * Click on the "Current Page" search button to return search results from the current page only.
     *
     * @return the current page object
     * @since 14.10.1
     * @since 15.0RC1
     */
    public AttachmentGalleryPicker toggleCurrentPage()
    {
        getSearchBlock().findElement(By.cssSelector("label:nth-child(1)")).click();
        return this;
    }

    /**
     * Click on an attachment. This will either select or un-select it according to it's initial state.
     *
     * @param attachmentName the name of the attachment to click on
     * @return the current page object
     * @since 14.10.1
     * @since 14.5RC1
     */
    public AttachmentGalleryPicker clickAttachment(String attachmentName)
    {
        getAttachmentPickerResults()
            .findElement(By.cssSelector(String.format(".attachmentGroup a[title='%s']", attachmentName)))
            .click();
        return this;
    }

    /**
     * Returns the name of the currently selected attachment, wrapped in an {@link Optional}, or
     * {@link Optional#empty()} if no attachment is currently selected.
     *
     * @return the currently selected attachment name, wrapped in an {@link Optional}, or {@link Optional#empty()} if no
     *     attachment is currently selected
     * @since 14.10.1
     * @since 14.5RC1
     */
    public Optional<String> getSelectedAttachment()
    {
        List<WebElement> selectedElements =
            getAttachmentPickerResults().findElements(By.cssSelector(".attachmentGroup.selected"));
        if (selectedElements.size() > 1) {
            throw new RuntimeException(
                String.format("Too many attachments selected at the same time. Expected [0] or [1] and found [%d]",
                    selectedElements.size()));
        }

        if (selectedElements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(selectedElements.get(0).findElement(By.tagName("a")).getAttribute("title"));
    }

    /**
     * @return {@code true} if the global selection warning message is displayed, {@code false} otherwise
     * @since 14.10.1
     * @since 14.5RC1
     */
    public boolean isGlobalSelectionWarningDisplayed()
    {
        return this.attachmentGalleryPickerElement.findElement(By.cssSelector(".attachmentPickerGlobalSelection"))
            .isDisplayed();
    }

    private List<WebElement> getAllAttachments()
    {
        return this.attachmentGalleryPickerElement.findElements(By.className("attachmentTitle"));
    }

    private WebElement getSearchBlock()
    {
        return this.attachmentGalleryPickerElement.findElement(By.cssSelector(".attachmentPickerSearch"));
    }

    private WebElement getAttachmentPickerResults()
    {
        return this.attachmentGalleryPickerElement.findElement(By.className("attachmentPickerResults"));
    }
}

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
public class AttachmentPicker extends BaseElement
{
    private final WebElement attachmentPicker;

    public AttachmentPicker(String id)
    {
        this.attachmentPicker = getDriver().findElement(By.id(id));
    }

    public AttachmentPicker waitUntilReady()
    {

        getDriver().waitUntilCondition(driver -> !this.attachmentPicker.getAttribute("class").contains("loading"));
        return this;
    }

    public List<String> getAttachmentTitles()
    {
        return getAllAttachments().stream()
            .map(WebElement::getText)
            .collect(Collectors.toList());
    }

    public AttachmentPicker setSearch(String image)
    {
        WebElement input = this.attachmentPicker.findElement(By.cssSelector(".attachmentPickerSearch input"));
        input.clear();
        input.sendKeys(image);
        return this;
    }

    public void waitUntilAttachmentsCount(int expectedCount)
    {
        getDriver().waitUntilCondition(driver -> getAllAttachments().size() == expectedCount);
    }

    public boolean isNoResultMessageDisplayed()
    {
        return this.attachmentPicker.findElement(By.className("attachmentPickerNoResults")).isDisplayed();
    }

    private List<WebElement> getAllAttachments()
    {
        return this.attachmentPicker.findElements(By.className("attachmentTitle"));
    }
}

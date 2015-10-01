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
package org.xwiki.test.ui.po.diff;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Represents the summary displayed when comparing two versions of a document.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
public class DocumentDiffSummary extends BaseElement
{
    private static final String PAGE_PROPERTIES = "Page properties";

    private static final String ATTACHMENTS = "Attachments";

    private static final String OBJECTS = "Objects";

    private static final String CLASS_PROPERTIES = "Class properties";

    private static final String ACTION_CHANGE = "change";

    private static final String ACTION_INSERT = "insert";

    private static final String ACTION_DELETE = "delete";

    /**
     * The element that wraps and container the summary.
     */
    private WebElement container;

    /**
     * Creates a new instance for the specified summary.
     * 
     * @param container the element that wraps and contains the summary
     */
    public DocumentDiffSummary(WebElement container)
    {
        this.container = container;
    }

    public List<String> getItems()
    {
        List<WebElement> items =
            getDriver().findElementsWithoutWaiting(this.container, By.xpath(".//div[@class = 'diff-summary-item']/a"));
        List<String> labels = new ArrayList<>();
        for (WebElement item : items) {
            labels.add(item.getText());
        }
        return labels;
    }

    public String getPagePropertiesSummary()
    {
        return getSummaryItemHint(PAGE_PROPERTIES);
    }

    public String getAttachmentsSummary()
    {
        return getSummaryItemHint(ATTACHMENTS);
    }

    public DocumentDiffSummary toggleAttachmentsDetails()
    {
        return toggleDetails(ATTACHMENTS);
    }

    public List<String> getModifiedAttachments()
    {
        return getSummaryItemDetails(ATTACHMENTS, ACTION_CHANGE);
    }

    public List<String> getAddedAttachments()
    {
        return getSummaryItemDetails(ATTACHMENTS, ACTION_INSERT);
    }

    public List<String> getRemovedAttachments()
    {
        return getSummaryItemDetails(ATTACHMENTS, ACTION_DELETE);
    }

    public String getObjectsSummary()
    {
        return getSummaryItemHint(OBJECTS);
    }

    public DocumentDiffSummary toggleObjectsDetails()
    {
        return toggleDetails(OBJECTS);
    }

    public List<String> getModifiedObjects()
    {
        return getSummaryItemDetails(OBJECTS, ACTION_CHANGE);
    }

    public List<String> getAddedObjects()
    {
        return getSummaryItemDetails(OBJECTS, ACTION_INSERT);
    }

    public List<String> getRemovedObjects()
    {
        return getSummaryItemDetails(OBJECTS, ACTION_DELETE);
    }

    public String getClassPropertiesSummary()
    {
        return getSummaryItemHint(CLASS_PROPERTIES);
    }

    public DocumentDiffSummary toggleClassPropertiesDetails()
    {
        return toggleDetails(CLASS_PROPERTIES);
    }

    public List<String> getModifiedClassProperties()
    {
        return getSummaryItemDetails(CLASS_PROPERTIES, ACTION_CHANGE);
    }

    public List<String> getAddedClassProperties()
    {
        return getSummaryItemDetails(CLASS_PROPERTIES, ACTION_INSERT);
    }

    public List<String> getRemovedClassProperties()
    {
        return getSummaryItemDetails(CLASS_PROPERTIES, ACTION_DELETE);
    }

    private String getSummaryItemHint(String label)
    {
        return this.container.findElement(
            By.xpath(".//span[@class = 'diff-summary-item-hint' and preceding-sibling::a[. = '" + label + "']]"))
            .getText();
    }

    private List<String> getSummaryItemDetails(String label, String action)
    {
        List<WebElement> elements =
            getDriver().findElementsWithoutWaiting(
                this.container,
                By.xpath(".//li[@class = 'diff-summary-item' and child::span[@class = 'diff-icon diff-icon-" + action
                    + "'] and parent::ul/preceding-sibling::div[1]/a[. = '" + label + "']]"));
        List<String> labels = new ArrayList<>();
        for (WebElement element : elements) {
            labels.add(element.getText().trim());
        }
        return labels;
    }

    private DocumentDiffSummary toggleDetails(String label)
    {
        this.container.findElement(By.xpath(".//div[@class = 'diff-summary-item']/a[. = '" + label + "']")).click();
        return this;
    }
}

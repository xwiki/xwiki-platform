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

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.diff.DocumentDiffSummary;
import org.xwiki.test.ui.po.diff.EntityDiff;

/**
 * Displays the differences between two versions of a document.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ChangesPane extends BaseElement
{
    private final static String previousChangeSelector = "#changes-info-boxes > a.changes-arrow-left";
    private final static String nextChangeSelector = "#changes-info-boxes > a.changes-arrow-right";
    private final static String previousFromVersionSelector = "#changes-info-box-from .changes-arrow:first-child";
    private final static String nextFromVersionSelector = "#changes-info-box-from .changes-arrow:last-child";
    private final static String previousToVersionSelector = "#changes-info-box-to .changes-arrow:first-child";
    private final static String nextToVersionSelector = "#changes-info-box-to .changes-arrow:last-child";

    /**
     * The element that wraps all the changes.
     */
    @FindBy(id = "changescontent")
    private WebElement container;

    /**
     * The summary of the older version
     */
    @FindBy(id = "changes-info-box-from")
    private WebElement fromVersionSummary;

    /**
     * The summary of the newer version.
     */
    @FindBy(id = "changes-info-box-to")
    private WebElement toVersionSummary;

    /**
     * The comment for the to version.
     */
    @FindBy(id = "changes-info-comment")
    private WebElement changeComment;

    @FindBy(className = "diff-summary")
    private WebElement diffSummary;

    @FindBy(css = "#changes-info-box-to .changes-version a:not(.changes-arrow)")
    private WebElement toVersionElement;

    @FindBy(css = "#changes-info-box-from .changes-version a:not(.changes-arrow)")
    private WebElement fromVersionElement;

    @FindBy(css = previousChangeSelector)
    private WebElement previousChangeElement;

    @FindBy(css = nextChangeSelector)
    private WebElement nextChangeElement;

    @FindBy(css = previousFromVersionSelector)
    private WebElement previousFromVersionElement;

    @FindBy(css = nextFromVersionSelector)
    private WebElement nextFromVersionElement;

    @FindBy(css = previousToVersionSelector)
    private WebElement previousToVersionElement;

    @FindBy(css = nextToVersionSelector)
    private WebElement nextToVersionElement;

    /**
     * @return the summary of the from version
     */
    public String getFromVersionSummary()
    {
        return fromVersionSummary.getText();
    }

    /**
     * @return the summary of the to version
     */
    public String getToVersionSummary()
    {
        return toVersionSummary.getText();
    }

    /**
     * @return the comment of the to version
     */
    public String getChangeComment()
    {
        return changeComment.getText();
    }

    /**
     * @return {@code true} if the "No changes" message is displayed and there are no diffs displayed, {@code false}
     *         otherwise
     */
    public boolean hasNoChanges()
    {
        return getDriver().findElementsWithoutWaiting(container,
            By.xpath("div[@class = 'infomessage' and . = 'No changes']")).size() > 0
            && getChangedEntities().isEmpty();
    }

    /**
     * @return the summary for the displayed changes
     */
    public DocumentDiffSummary getDiffSummary()
    {
        return new DocumentDiffSummary(this.diffSummary);
    }

    /**
     * @return the names (labels) for the entities that have been modified (have modified properties)
     */
    public List<String> getChangedEntities()
    {
        List<WebElement> elements = getDriver().findElementsWithoutWaiting(By.xpath("//dl[@class = 'diff-group']/dt"));
        List<String> labels = new ArrayList<>();
        for (WebElement element : elements) {
            labels.add(element.getText().trim());
        }
        return labels;
    }

    /**
     * @param label the entity label
     * @return the changes for the specified entity
     */
    public EntityDiff getEntityDiff(String label)
    {
        return new EntityDiff(this.container.findElement(By
            .xpath("//dd[parent::dl[@class = 'diff-group'] and preceding-sibling::dt[normalize-space(.) = '" + label
                + "']]")));
    }

    /**
     * Click the previous change button
     */
    public void clickPreviousChange()
    {
        previousChangeElement.click();
    }

    /**
     * Click the next change button
     */
    public void clickNextChange()
    {
        nextChangeElement.click();
    }

    /**
     * Click the previous change button of the original document
     */
    public void clickPreviousFromVersion()
    {
        previousFromVersionElement.click();
    }

    /**
     * Click the next change button of the original document
     */
    public void clickNextFromVersion()
    {
        nextFromVersionElement.click();
    }

    /**
     * Click the previous change button of the new document
     */
    public void clickPreviousToVersion()
    {
        previousToVersionElement.click();
    }

    /**
     * Click the next change button of the new document
     */
    public void clickNextToVersion()
    {
        nextToVersionElement.click();
    }

    /**
     * @return the presence of the previous change button
     */
    public boolean hasPreviousChange()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(previousChangeSelector));
    }

    /**
     * @return the presence of the next change button
     */
    public boolean hasNextChange()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(nextChangeSelector));
    }

    /**
     * @return the presence of the previous change button of the original document
     */
    public boolean hasPreviousFromVersion()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(previousFromVersionSelector));
    }

    /**
     * @return the presence of the next change button of the original document
     */
    public boolean hasNextFromVersion()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(nextFromVersionSelector));
    }

    /**
     * @return the presence of the previous change button of the new document
     */
    public boolean hasPreviousToVersion()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(previousToVersionSelector));
    }

    /**
     * @return the presence of the next change button of the new document
     */
    public boolean hasNextToVersion()
    {
        return getDriver().hasElementWithoutWaiting(By.cssSelector(nextToVersionSelector));
    }

    /**
     * @return the original document version
     */

    public String getFromVersion()
    {
        return fromVersionElement.getText();
    }

    /**
     * @return the new document version
     */
    public String getToVersion()
    {
        return toVersionElement.getText();
    }
}

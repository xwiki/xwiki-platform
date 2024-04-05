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

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.xwiki.test.ui.po.diff.RawChanges;
import org.xwiki.test.ui.po.diff.RenderedChanges;

/**
 * Displays the differences between two versions of a document.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class ChangesPane extends BaseElement
{
    private final static String previousChangeSelector = "#changes-info-boxes > #changes-arrows-box > a.changes-arrow-left";
    private final static String nextChangeSelector = "#changes-info-boxes > #changes-arrows-box > a.changes-arrow-right";
    private final static String previousFromVersionSelector = "#changes-info-box-from .changes-arrow:first-child";
    private final static String nextFromVersionSelector = "#changes-info-box-from .changes-arrow:last-child";
    private final static String previousToVersionSelector = "#changes-info-box-to .changes-arrow:first-child";
    private final static String nextToVersionSelector = "#changes-info-box-to .changes-arrow:last-child";

    private static final String CLASS_ATTRIBUTE = "class";

    @FindBy(id = "rawChanges")
    private WebElement rawChangesContainer;

    @FindBy(id = "renderedChanges")
    private WebElement renderedChangesContainer;

    @FindBy(css = "[data-hint=\"raw\"]")
    private WebElement rawChangesButton;

    @FindBy(css = "[data-hint=\"rendered\"]")
    private WebElement renderedChangesButton;

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
    @FindBy(css = "#changes-info-box-to .changes-info-comment")
    private WebElement changeComment;

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
     * @return the raw changes tab
     * @since 14.10.15
     * @since 15.5.1
     * @since 15.6
     */
    public RawChanges getRawChanges()
    {
        // Click on the raw changes tab.
        this.rawChangesButton.click();
        // Wait until the raw changes are loaded.
        waitUntilTabIsReady(this.rawChangesContainer);
        return new RawChanges(this.rawChangesContainer);
    }

    /**
     * @return the rendered changes tab
     * @since 14.10.15
     * @since 15.5.1
     * @since 15.6
     */
    public RenderedChanges getRenderedChanges()
    {
        // Click on the rendered changes tab.
        this.renderedChangesButton.click();
        // Wait until the rendered changes are loaded.
        waitUntilTabIsReady(this.renderedChangesContainer);
        return new RenderedChanges(this.renderedChangesContainer);
    }

    private void waitUntilTabIsReady(WebElement tab)
    {
        getDriver().waitUntilCondition(
            driver -> tab.getAttribute(CLASS_ATTRIBUTE).contains("active")
                && !tab.getAttribute(CLASS_ATTRIBUTE).contains("loading"));
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
